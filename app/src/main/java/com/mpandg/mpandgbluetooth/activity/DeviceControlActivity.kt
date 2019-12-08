package com.mpandg.mpandgbluetooth.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.InputFilter
import android.text.InputType
import android.text.method.ScrollingMovementMethod
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.preference.PreferenceManager
import com.mpandg.mpandgbluetooth.R
import com.mpandg.mpandgbluetooth.Utils
import com.mpandg.mpandgbluetooth.Utils.InputFilterHex
import com.mpandg.mpandgbluetooth.bluetooth.DeviceConnector
import com.mpandg.mpandgbluetooth.bluetooth.DeviceListActivity
import com.mpandg.mpandgbluetooth.model.DeviceData
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*

class DeviceControlActivity : BaseActivity() {
    private var logTextView: TextView? = null
    private var commandEditText: EditText? = null
    // Application settings
    private var hexMode = false
    private var needClean = false
    private var showTimings = false
    private var showDirection = false
    private var commandEnding: String? = null
    private var deviceName: String? = null
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        PreferenceManager.setDefaultValues(this, R.xml.settings_activity, false)
        if (mHandler == null) mHandler = BluetoothResponseHandler(this) else mHandler?.setTarget(this)
        MSG_NOT_CONNECTED = getString(R.string.msg_not_connected)
        MSG_CONNECTING = getString(R.string.msg_connecting)
        MSG_CONNECTED = getString(R.string.msg_connected)
        setContentView(R.layout.activity_main)
        if (isConnected && state != null) {
            setDeviceName(state.getString(DEVICE_NAME))
        } else supportActionBar?.subtitle = MSG_NOT_CONNECTED
        logTextView = findViewById<View>(R.id.log_textview) as TextView
        logTextView?.movementMethod = ScrollingMovementMethod()
        if (state != null) logTextView?.text = state.getString(LOG)
        commandEditText = findViewById<View>(R.id.command_edittext) as EditText
        // soft-keyboard send button
        commandEditText?.setOnEditorActionListener(OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendCommand(null)
                return@OnEditorActionListener true
            }
            false
        })
        // hardware Enter button
        commandEditText?.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_ENTER -> {
                        sendCommand(null)
                        return@OnKeyListener true
                    }
                    else -> {
                    }
                }
            }
            false
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(DEVICE_NAME, deviceName)
        if (logTextView != null) {
            val log = logTextView?.text.toString()
            outState.putString(LOG, log)
        }
    }

    /**
     * check the connection.
     */
    private val isConnected: Boolean
        get() = connector != null && connector?.state == DeviceConnector.STATE_CONNECTED

    /**
     * disconnect.
     */
    private fun stopConnection() {
        if (connector != null) {
            connector?.stop()
            connector = null
            deviceName = null
        }
    }

    /**
     * The list of devices to connect.
     */
    private fun startDeviceListActivity() {
        stopConnection()
        val serverIntent = Intent(this, DeviceListActivity::class.java)
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE)
    }

    /**
     * handling the "Search" button.
     */
    override fun onSearchRequested(): Boolean {
        if (super.isAdapterReady) startDeviceListActivity()
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.device_control_activity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_search -> {
                if (super.isAdapterReady) {
                    if (isConnected) stopConnection() else startDeviceListActivity()
                } else {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                }
                true
            }
            R.id.menu_clear -> {
                if (logTextView != null) logTextView?.text = ""
                true
            }
            R.id.menu_send -> {
                if (logTextView != null) {
                    val msg = logTextView?.text.toString()
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "text/plain"
                    intent.putExtra(Intent.EXTRA_TEXT, msg)
                    startActivity(Intent.createChooser(intent, getString(R.string.menu_send)))
                }
                true
            }
            R.id.menu_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
        // hex mode
        val mode = Utils.getPreference(this, getString(R.string.pref_commands_mode))
        hexMode = mode == "HEX"
        if (hexMode) {
            commandEditText?.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            commandEditText?.filters = arrayOf<InputFilter>(InputFilterHex())
        } else {
            commandEditText?.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            commandEditText?.filters = arrayOf()
        }
        // End of line.
        commandEnding = getCommandEnding()
        // display format of the log command
        showTimings = Utils.getBooleanPreference(this, getString(R.string.pref_log_timing))
        showDirection = Utils.getBooleanPreference(this, getString(R.string.pref_log_direction))
        needClean = Utils.getBooleanPreference(this, getString(R.string.pref_need_clean))
    }

    /**
     * get command endings.
     */
    private fun getCommandEnding(): String {
        var result = Utils.getPreference(this, getString(R.string.pref_commands_ending))
        result = when (result) {
            "\\r\\n" -> "\r\n"
            "\\n" -> "\n"
            "\\r" -> "\r"
            else -> ""
        }
        return result
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CONNECT_DEVICE ->  // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    val address = data?.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS)
                    val device = btAdapter?.getRemoteDevice(address)
                    device?.let {
                        if (super.isAdapterReady && connector == null) setupConnector(device)
                    } ?: Utils.log("BT device not available")
                }
            REQUEST_ENABLE_BT -> {
                // When the request to enable Bluetooth returns
                super.pendingRequestEnableBt = false
                if (resultCode != Activity.RESULT_OK) {
                    Utils.log("BT not enabled")
                }
            }
        }
    }

    /**
     * Establishing a connection with the device.
     */
    private fun setupConnector(connectedDevice: BluetoothDevice) {
        stopConnection()
        try {
            val emptyName = getString(R.string.empty_device_name)
            val data = DeviceData(
                    device = connectedDevice,
                    emptyName = emptyName,
                    name = null,
                    address = null,
                    deviceClass = null,
                    majorDeviceClass = null)
            connector = mHandler?.let { DeviceConnector(data, it) }
            connector?.connect()
        } catch (e: IllegalArgumentException) {
            Utils.log("setupConnector failed: " + e.message)
        }
    }

    /**
     * Sending device command
     */
    fun sendCommand(@Suppress("UNUSED_PARAMETER") view: View?) {
        if (commandEditText != null) {
            val commandString = commandEditText?.text.toString()
                        if (commandString.isEmpty()) return

//            //Addition of commands in hex
//            if (hexMode && (commandString.length() % 2 == 1)) {
//                commandString = "0" + commandString;
//                commandEditText.setText(commandString);
//            }
//            byte[] command = (hexMode ? Utils.toHex(commandString) : commandString.getBytes());
//            if (command_ending != null) command = Utils.concat(command, command_ending.getBytes());

            if (isConnected) {
                connector?.write(commandString.toByteArray())
                appendLog(commandString, hexMode, true, needClean)
            }
        }
    }

    /**
     * Adding a response to the log
     *
     * @param message  - Text to display
     * @param outgoing - destination.
     */
    fun appendLog(message: String?, hexMode: Boolean, outgoing: Boolean, clean: Boolean) {
        val msg = StringBuilder()
        if (showTimings) msg.append("[").append(timeFormat.format(Date())).append("]")
        if (showDirection) {
            val arrow = if (outgoing) " << " else " >> "
            msg.append(arrow)
        } else msg.append(" ")
        msg.append(if (hexMode) Utils.printHex(message) else message)
        if (outgoing) msg.append('\n')
        logTextView?.let {
            it.append(msg)
            val scrollAmount = it.layout?.getLineTop(it.lineCount)?.minus(it.height)
            scrollAmount?.let { sa -> if (sa > 0) it.scrollTo(0, sa) else it.scrollTo(0, 0) }
        }
        if (clean) commandEditText?.setText("")
    }

    fun setDeviceName(deviceName: String?) {
        this.deviceName = deviceName
        supportActionBar?.subtitle = deviceName
    }

    /**
     * process recieved data from the bluetooth-stream.
     */
    private class BluetoothResponseHandler(activity: DeviceControlActivity) : Handler() {
        private var mActivity: WeakReference<DeviceControlActivity>
        fun setTarget(target: DeviceControlActivity) {
            mActivity.clear()
            mActivity = WeakReference(target)
        }

        override fun handleMessage(msg: Message) {
            val activity = mActivity.get()
            if (activity != null) {
                when (msg.what) {
                    MESSAGE_STATE_CHANGE -> {
                        Utils.log("MESSAGE_STATE_CHANGE: " + msg.arg1)
                        val bar = activity.supportActionBar
                        when (msg.arg1) {
                            DeviceConnector.STATE_CONNECTED -> {
                                assert(bar != null)
                                bar?.subtitle = MSG_CONNECTED
                            }
                            DeviceConnector.STATE_CONNECTING -> {
                                assert(bar != null)
                                bar?.subtitle = MSG_CONNECTING
                            }
                            DeviceConnector.STATE_NONE -> {
                                assert(bar != null)
                                bar?.subtitle = MSG_NOT_CONNECTED
                            }
                        }
                    }
                    MESSAGE_READ -> {
                        val readMessage = msg.obj as String
                        activity.appendLog(readMessage, hexMode = false, outgoing = false, clean = activity.needClean)
                    }
                    MESSAGE_DEVICE_NAME -> activity.setDeviceName(msg.obj as String)
                    MESSAGE_WRITE -> {
                    }
                    MESSAGE_TOAST -> {
                    }
                }
            }
        }

        init {
            mActivity = WeakReference(activity)
        }
    }

    companion object {
        private const val DEVICE_NAME = "DEVICE_NAME"
        private const val LOG = "LOG"
        @SuppressLint("SimpleDateFormat")
        private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS")
        private var MSG_NOT_CONNECTED: String? = null
        private var MSG_CONNECTING: String? = null
        private var MSG_CONNECTED: String? = null
        private var connector: DeviceConnector? = null
        private var mHandler: BluetoothResponseHandler? = null
    }
}