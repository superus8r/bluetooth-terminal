package com.mpandg.mpandgbluetooth.activity

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mpandg.mpandgbluetooth.R
import com.mpandg.mpandgbluetooth.Utils

/**
 * Common base class. Initialize BT-adapter
 * Created by sash0k on 09.12.13.
 */
abstract class BaseActivity : AppCompatActivity() {
    @JvmField
    var btAdapter: BluetoothAdapter? = null
    // do not resend request to enable Bluetooth
// if there is a request already in progress
// See: https://code.google.com/p/android/issues/detail?id=24931#c1
    @JvmField
    var pendingRequestEnableBt = false

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        supportActionBar!!.setHomeButtonEnabled(false)
        if (state != null) {
            pendingRequestEnableBt = state.getBoolean(SAVED_PENDING_REQUEST_ENABLE_BT)
        }
        btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter == null) {
            val noBluetooth = getString(R.string.no_bt_support)
            showAlertDialog(noBluetooth)
            Utils.log(noBluetooth)
        }
    }

    public override fun onStart() {
        super.onStart()
        if (btAdapter == null) return
        if (!btAdapter!!.isEnabled && !pendingRequestEnableBt) {
            pendingRequestEnableBt = true
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
        }
    }

    @Synchronized
    public override fun onResume() {
        super.onResume()
    }

    @Synchronized
    public override fun onPause() {
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(SAVED_PENDING_REQUEST_ENABLE_BT, pendingRequestEnableBt)
    }

    /**
     * Check adapter
     *
     * @return - true, when ready for use
     */
    val isAdapterReady: Boolean
        get() = btAdapter != null && btAdapter!!.isEnabled

    /**
     * It shows a warning dialog.
     * When reconfiguration will be lost.
     *
     * @param message - message
     */
    private fun showAlertDialog(message: String?) {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(getString(R.string.app_name))
        alertDialogBuilder.setMessage(message)
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    companion object {
        // Intent request codes
        const val REQUEST_CONNECT_DEVICE = 1
        const val REQUEST_ENABLE_BT = 2
        // Message types sent from the DeviceConnector Handler
        const val MESSAGE_STATE_CHANGE = 1
        const val MESSAGE_READ = 2
        const val MESSAGE_WRITE = 3
        const val MESSAGE_DEVICE_NAME = 4
        const val MESSAGE_TOAST = 5
        private const val SAVED_PENDING_REQUEST_ENABLE_BT = "PENDING_REQUEST_ENABLE_BT"
    }
}