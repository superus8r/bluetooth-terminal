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

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import com.mpandg.mpandgbluetooth.R
import java.util.*

/**
 * This Activity appears as a dialog. It lists already paired devices,
 * and it can scan for devices nearby. When the user selects a device,
 * its MAC address is returned to the caller as the result of this activity.
 */
class DeviceListActivity : Activity() {
    private var mBtAdapter: BluetoothAdapter? = null
    private var mNewDevicesArrayAdapter: ArrayAdapter<String>? = null
    private val mNewDevicesSet: MutableSet<String> = HashSet()
    private val mPairedDevicesSet: MutableSet<String> = HashSet()
    private var newDevicesListView: ListView? = null
    private var scanButton: Button? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_list)
        // Set default result to CANCELED, in case the user backs out
        setResult(RESULT_CANCELED)
        // Initialize the button to perform device discovery
        scanButton = findViewById<View>(R.id.button_scan) as Button
        scanButton?.setOnClickListener { v ->
            doDiscovery()
            v.isEnabled = false
        }
        val pairedDevicesAdapter = ArrayAdapter<String>(this, R.layout.device_name)
        mNewDevicesArrayAdapter = ArrayAdapter(this, R.layout.device_name)
        val pairedListView = findViewById<View>(R.id.paired_devices) as ListView
        pairedListView.adapter = pairedDevicesAdapter
        pairedListView.onItemClickListener = mDeviceClickListener
        newDevicesListView = findViewById<View>(R.id.new_devices) as ListView
        newDevicesListView?.adapter = mNewDevicesArrayAdapter
        newDevicesListView?.onItemClickListener = mDeviceClickListener
        var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(mReceiver, filter)
        filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(mReceiver, filter)
        mBtAdapter = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices = mBtAdapter?.bondedDevices
        if (pairedDevices != null && pairedDevices.isNotEmpty()) {
            pairedListView.isEnabled = true
            findViewById<View>(R.id.title_paired_devices).visibility = View.VISIBLE
            pairedDevices.forEach { device ->
                val address = device.address
                mPairedDevicesSet.add(address)
                pairedDevicesAdapter.add(device.name + '\n' + address)
            }
        } else {
            pairedListView.isEnabled = false
            val noDevices = resources.getText(R.string.none_paired).toString()
            pairedDevicesAdapter.add(noDevices)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mBtAdapter != null) {
            mBtAdapter?.cancelDiscovery()
        }
        unregisterReceiver(mReceiver)
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private fun doDiscovery() {
        mNewDevicesArrayAdapter?.clear()
        mNewDevicesSet.clear()
        setTitle(R.string.search_message)
        findViewById<View>(R.id.title_new_devices).visibility = View.VISIBLE
        if (mBtAdapter?.isDiscovering == true) mBtAdapter?.cancelDiscovery()
        mBtAdapter?.startDiscovery()
    }

    private val mDeviceClickListener = OnItemClickListener { _, v, _, _ ->
        // Cancel discovery because it's costly and we're about to connect
        mBtAdapter?.cancelDiscovery()
        // Get the device MAC address, which is the last 17 chars in the View
        val info = (v as TextView).text
        if (info != null) { // this is not so cool...
            val address: CharSequence = info.toString().substring(info.length - 17)
            val intent = Intent()
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address)
            setResult(RESULT_OK, intent)
            finish()
        }
    }
    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (device != null) {
                    val address = device.address
                    if (!mNewDevicesSet.contains(address) && !mPairedDevicesSet.contains(address)) {
                        newDevicesListView?.isEnabled = true
                        mNewDevicesSet.add(address)
                        var name = device.name
                        if (name == null || name.isEmpty()) name = getString(R.string.empty_device_name)
                        mNewDevicesArrayAdapter?.add(name + '\n' + device.address)
                    }
                } else {
                    Log.e(TAG, "Could not get parcelable extra from device: " + BluetoothDevice.EXTRA_DEVICE)
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                setTitle(R.string.select_device)
                if (mNewDevicesSet.isEmpty()) {
                    val noDevices = resources.getText(R.string.none_found).toString()
                    mNewDevicesArrayAdapter?.add(noDevices)
                    newDevicesListView?.isEnabled = false
                }
                scanButton?.isEnabled = true
            }
        }
    }

    companion object {
        private const val TAG = "DeviceListActivity"
        const val EXTRA_DEVICE_ADDRESS = "device_address"
    }
}