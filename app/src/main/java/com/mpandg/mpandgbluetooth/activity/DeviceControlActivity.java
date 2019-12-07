package com.mpandg.mpandgbluetooth.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import androidx.appcompat.app.ActionBar;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.mpandg.mpandgbluetooth.R;
import com.mpandg.mpandgbluetooth.Utils;
import com.mpandg.mpandgbluetooth.bluetooth.DeviceConnector;
import com.mpandg.mpandgbluetooth.bluetooth.DeviceListActivity;
import com.mpandg.mpandgbluetooth.model.DeviceData;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class DeviceControlActivity extends BaseActivity {
    private static final String DEVICE_NAME = "DEVICE_NAME";
    private static final String LOG = "LOG";

    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    private static String MSG_NOT_CONNECTED;
    private static String MSG_CONNECTING;
    private static String MSG_CONNECTED;

    private static DeviceConnector connector;
    private static BluetoothResponseHandler mHandler;

    private TextView logTextView;
    private EditText commandEditText;

    // Настройки приложения
    private boolean hexMode, needClean;
    private boolean show_timings, show_direction;
    private String command_ending;
    private String deviceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.settings_activity, false);

        if (mHandler == null) mHandler = new BluetoothResponseHandler(this);
        else mHandler.setTarget(this);

        MSG_NOT_CONNECTED = getString(R.string.msg_not_connected);
        MSG_CONNECTING = getString(R.string.msg_connecting);
        MSG_CONNECTED = getString(R.string.msg_connected);

        setContentView(R.layout.activity_main);
        if (isConnected() && (savedInstanceState != null)) {
            setDeviceName(savedInstanceState.getString(DEVICE_NAME));
        } else //noinspection ConstantConditions
            getSupportActionBar().setSubtitle(MSG_NOT_CONNECTED);

        this.logTextView = (TextView) findViewById(R.id.log_textview);
        this.logTextView.setMovementMethod(new ScrollingMovementMethod());
        if (savedInstanceState != null)
            logTextView.setText(savedInstanceState.getString(LOG));

        this.commandEditText = (EditText) findViewById(R.id.command_edittext);
        // soft-keyboard send button
        this.commandEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendCommand(null);
                    return true;
                }
                return false;
            }
        });
        // hardware Enter button
        this.commandEditText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_ENTER:
                            sendCommand(null);
                            return true;
                        default:
                            break;
                    }
                }
                return false;
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(DEVICE_NAME, deviceName);
        if (logTextView != null) {
            final String log = logTextView.getText().toString();
            outState.putString(LOG, log);
        }
    }


    /**
     * check the connection.
     */
    private boolean isConnected() {
        return (connector != null) && (connector.getState() == DeviceConnector.STATE_CONNECTED);
    }


    /**
     * disconnect.
     */
    private void stopConnection() {
        if (connector != null) {
            connector.stop();
            connector = null;
            deviceName = null;
        }
    }


    /**
     * The list of devices to connect.
     */
    private void startDeviceListActivity() {
        stopConnection();
        Intent serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }


    /**
     * handling the "Search" button.
     */
    @Override
    public boolean onSearchRequested() {
        if (super.isAdapterReady()) startDeviceListActivity();
        return false;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.device_control_activity, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_search:
                if (super.isAdapterReady()) {
                    if (isConnected()) stopConnection();
                    else startDeviceListActivity();
                } else {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
                return true;

            case R.id.menu_clear:
                if (logTextView != null) logTextView.setText("");
                return true;

            case R.id.menu_send:
                if (logTextView != null) {
                    final String msg = logTextView.getText().toString();
                    final Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, msg);
                    startActivity(Intent.createChooser(intent, getString(R.string.menu_send)));
                }
                return true;

            case R.id.menu_settings:
                final Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onStart() {
        super.onStart();

        // hex mode
        final String mode = Utils.getPrefence(this, getString(R.string.pref_commands_mode));
        this.hexMode = mode.equals("HEX");
        if (hexMode) {
            commandEditText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
            commandEditText.setFilters(new InputFilter[]{new Utils.InputFilterHex()});
        } else {
            commandEditText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            commandEditText.setFilters(new InputFilter[]{});
        }

        // End of line.
        this.command_ending = getCommandEnding();

        // display format of the log command
        this.show_timings = Utils.getBooleanPrefence(this, getString(R.string.pref_log_timing));
        this.show_direction = Utils.getBooleanPrefence(this, getString(R.string.pref_log_direction));
        this.needClean = Utils.getBooleanPrefence(this, getString(R.string.pref_need_clean));
    }


    /**
     * get command endings.
     */
    private String getCommandEnding() {
        String result = Utils.getPrefence(this, getString(R.string.pref_commands_ending));
        switch (result) {
            case "\\r\\n":
                result = "\r\n";
                break;
            case "\\n":
                result = "\n";
                break;
            case "\\r":
                result = "\r";
                break;
            default:
                result = "";
                break;
        }
        return result;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    BluetoothDevice device = btAdapter.getRemoteDevice(address);
                    if (super.isAdapterReady() && (connector == null)) setupConnector(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                super.pendingRequestEnableBt = false;
                if (resultCode != Activity.RESULT_OK) {
                    Utils.log("BT not enabled");
                }
                break;
        }
    }


    /**
     * Establishing a connection with the device.
     */
    private void setupConnector(BluetoothDevice connectedDevice) {
        stopConnection();
        try {
            String emptyName = getString(R.string.empty_device_name);
            DeviceData data = new DeviceData(connectedDevice, emptyName);
            connector = new DeviceConnector(data, mHandler);
            connector.connect();
        } catch (IllegalArgumentException e) {
            Utils.log("setupConnector failed: " + e.getMessage());
        }
    }


    /**
     * Sending device command
     */
    public void sendCommand(View view) {
        if (commandEditText != null) {
            String commandString = commandEditText.getText().toString();
//            if (commandString.isEmpty()) return;
//
//            // Дополнение команд в hex
//            if (hexMode && (commandString.length() % 2 == 1)) {
//                commandString = "0" + commandString;
//                commandEditText.setText(commandString);
//            }
//            byte[] command = (hexMode ? Utils.toHex(commandString) : commandString.getBytes());
//            if (command_ending != null) command = Utils.concat(command, command_ending.getBytes());
//            if (isConnected()) {
//                connector.write(command);
//                appendLog(commandString, hexMode, true, needClean);
//            }

            if (isConnected()) {
                connector.write(commandString.getBytes());
                appendLog(commandString, hexMode, true, needClean);
            }
        }
    }


    /**
     * Adding a response to the log
     *
     * @param message  - Text to display
     * @param outgoing - destination.
     */
    void appendLog(String message, boolean hexMode, boolean outgoing, boolean clean) {

        StringBuilder msg = new StringBuilder();
        if (show_timings) msg.append("[").append(timeFormat.format(new Date())).append("]");
        if (show_direction) {
            final String arrow = (outgoing ? " << " : " >> ");
            msg.append(arrow);
        } else msg.append(" ");

        msg.append(hexMode ? Utils.printHex(message) : message);
        if (outgoing) msg.append('\n');
        logTextView.append(msg);

        final int scrollAmount = logTextView.getLayout().getLineTop(logTextView.getLineCount()) - logTextView.getHeight();
        if (scrollAmount > 0)
            logTextView.scrollTo(0, scrollAmount);
        else logTextView.scrollTo(0, 0);

        if (clean) commandEditText.setText("");
    }


    void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
        //noinspection ConstantConditions
        getSupportActionBar().setSubtitle(deviceName);
    }

    /**
     * process recieved data from the bluetooth-stream.
     */
    private static class BluetoothResponseHandler extends Handler {
        private WeakReference<DeviceControlActivity> mActivity;

        public BluetoothResponseHandler(DeviceControlActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        public void setTarget(DeviceControlActivity target) {
            mActivity.clear();
            mActivity = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            DeviceControlActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case MESSAGE_STATE_CHANGE:

                        Utils.log("MESSAGE_STATE_CHANGE: " + msg.arg1);
                        final ActionBar bar = activity.getSupportActionBar();
                        switch (msg.arg1) {
                            case DeviceConnector.STATE_CONNECTED:
                                assert bar != null;
                                bar.setSubtitle(MSG_CONNECTED);
                                break;
                            case DeviceConnector.STATE_CONNECTING:
                                assert bar != null;
                                bar.setSubtitle(MSG_CONNECTING);
                                break;
                            case DeviceConnector.STATE_NONE:
                                assert bar != null;
                                bar.setSubtitle(MSG_NOT_CONNECTED);
                                break;
                        }
                        break;

                    case MESSAGE_READ:
                        final String readMessage = (String) msg.obj;
                        if (readMessage != null) {
                            activity.appendLog(readMessage, false, false, activity.needClean);
                        }
                        break;

                    case MESSAGE_DEVICE_NAME:
                        activity.setDeviceName((String) msg.obj);
                        break;

                    case MESSAGE_WRITE:
                        // stub
                        break;

                    case MESSAGE_TOAST:
                        // stub
                        break;
                }
            }
        }
    }
}