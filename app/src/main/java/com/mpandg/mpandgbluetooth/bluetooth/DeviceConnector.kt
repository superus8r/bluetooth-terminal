/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mpandg.mpandgbluetooth.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.os.Handler
import com.mpandg.mpandgbluetooth.activity.BaseActivity
import com.mpandg.mpandgbluetooth.bluetooth.BluetoothUtils.createRfcommSocket
import com.mpandg.mpandgbluetooth.model.DeviceData
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class DeviceConnector(deviceData: DeviceData, private val mHandler: Handler) {
    private var mState: Int
    private val btAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val connectedDevice: BluetoothDevice
    private var mConnectThread: ConnectThread? = null
    private var mConnectedThread: ConnectedThread? = null
    private val deviceName: String
    /**
     * Device connection request.
     */
    @Synchronized
    fun connect() {
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread = null
            }
        }
        if (mConnectedThread != null) {
            mConnectedThread?.cancel()
            mConnectedThread = null
        }
        // Start the thread to connect with the given device
        mConnectThread = ConnectThread(connectedDevice)
        mConnectThread?.start()
        state = STATE_CONNECTING
    }

    /**
     * closing a connection.
     */
    @Synchronized
    fun stop() {
        if (mConnectThread != null) {
            mConnectThread = null
        }
        if (mConnectedThread != null) {
            mConnectedThread?.cancel()
            mConnectedThread = null
        }
        state = STATE_NONE
    }

    /**
     * Get device status.
     */
    /**
     * set the terminal state.
     *
     */
    @get:Synchronized
    @set:Synchronized
    var state: Int
        get() = mState
        private set(state) {
            mState = state
            mHandler.obtainMessage(BaseActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget()
        }

    @Synchronized
    private fun connected(socket: BluetoothSocket) {
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread = null
        }
        if (mConnectedThread != null) {
            mConnectedThread?.cancel()
            mConnectedThread = null
        }
        state = STATE_CONNECTED
        // Send the name of the connected device back to the UI Activity
        val msg = mHandler.obtainMessage(BaseActivity.MESSAGE_DEVICE_NAME, deviceName)
        mHandler.sendMessage(msg)
        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = ConnectedThread(socket)
        mConnectedThread?.start()
    }

    fun write(data: ByteArray) {
        var r: ConnectedThread?
        // Synchronize a copy of the ConnectedThread
        synchronized(this) {
            if (mState != STATE_CONNECTED) return
            r = mConnectedThread
        }
        // Perform the write asynchronously
        if (data.size == 1) r?.write(data[0]) else r?.writeData(data)
    }

    private fun connectionFailed() {
        // Send a failure message back to the Activity
        val msg = mHandler.obtainMessage(BaseActivity.MESSAGE_TOAST)
        val bundle = Bundle()
        msg.data = bundle
        mHandler.sendMessage(msg)
        state = STATE_NONE
    }

    private fun connectionLost() { // Send a failure message back to the Activity
        val msg = mHandler.obtainMessage(BaseActivity.MESSAGE_TOAST)
        val bundle = Bundle()
        msg.data = bundle
        mHandler.sendMessage(msg)
        state = STATE_NONE
    }

    /**
     * connect to bluetooth device
     */
    private inner class ConnectThread internal constructor(device: BluetoothDevice?) : Thread() {
        private val mmSocket: BluetoothSocket? = device?.let { createRfcommSocket(device) }
        /**
         * basic method for device connection.
         * if the connection was successful, the control will be transferred to the other thread.
         */
        override fun run() {
            btAdapter.cancelDiscovery()
            if (mmSocket == null) {
                connectionFailed()
                return
            }
            // Make a connection to the BluetoothSocket
            try { // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect()
            } catch (e: IOException) { // Close the socket
                try {
                    mmSocket.close()
                } catch (e2: IOException) {
                }
                connectionFailed()
                return
            }
            // Reset the ConnectThread because we're done
            synchronized(this@DeviceConnector) { mConnectThread = null }
            // Start the connected thread
            connected(mmSocket)
        }

    }

    /**
     * communicating with bluetooth device.
     */
    private inner class ConnectedThread internal constructor(socket: BluetoothSocket) : Thread() {
        private val mmSocket: BluetoothSocket = socket
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?
        /**
         * The main method is waiting for commands from the incoming stream.
         */
        override fun run() {
            val buffer = ByteArray(512)
            var bytes: Int
            val readMessage = StringBuilder()
            while (true) {
                try { // read the incoming data from stream and append in a response line.
                    bytes = mmInStream?.read(buffer) ?: 0
                    val doneReading = String(buffer, 0, bytes)
                    readMessage.append(doneReading)
                    // indicate end of the message and return an answer to main stream.
                    if (doneReading.contains("\n")) {
                        mHandler.obtainMessage(BaseActivity.MESSAGE_READ, bytes, -1, readMessage.toString()).sendToTarget()
                        readMessage.setLength(0)
                    }
                } catch (e: IOException) {
                    connectionLost()
                    break
                }
            }
        }

        /**
         * write data to device.
         */
        fun writeData(chunk: ByteArray?) {
            try {
                mmOutStream?.write(chunk ?: byteArrayOf())
                mmOutStream?.flush()
                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(BaseActivity.MESSAGE_WRITE, -1, -1, chunk).sendToTarget()
            } catch (e: IOException) {
            }
        }

        /**
         * write bytes.
         */
        fun write(command: Byte) {
            val buffer = ByteArray(1)
            buffer[0] = command
            try {
                mmOutStream?.write(buffer)
                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(BaseActivity.MESSAGE_WRITE, -1, -1, buffer).sendToTarget()
            } catch (e: IOException) {
            }
        }

        /**
         * cancel - close the socket.
         */
        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
            }
        }

        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.inputStream
                tmpOut = socket.outputStream
            } catch (e: IOException) {
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut
        }
    }

    companion object {
        // Constants that indicate the current connection state
        const val STATE_NONE = 0 // we're doing nothing
        const val STATE_CONNECTING = 1 // now initiating an outgoing connection
        const val STATE_CONNECTED = 2 // now connected to a remote device
    }

    init {
        connectedDevice = btAdapter.getRemoteDevice(deviceData.address)
        deviceName = deviceData.name ?: deviceData.address ?: throw Error("device name not available!")
        mState = STATE_NONE
    }
}