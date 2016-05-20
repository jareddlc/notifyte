package com.solderbyte.notifyte;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "Notifyte:Activity";

    // states
    private boolean bleEnabled = false;

    // UI elements
    private static Button button_bluetooth = null;
    private static Button button_scan = null;
    private static Button button_connect = null;
    private static TextView text_device = null;

    private static FloatingActionButton fab = null;

    // dialog
    private ProgressDialog progress_scan = null;
    private ProgressDialog progress_connecting = null;

    // device
    private static CharSequence entry = null;
    private static CharSequence value = null;

    // global
    private static SavedPreferences preferences = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // saved preferences
        preferences = new SavedPreferences();
        preferences.init(this.getApplicationContext());

        // UI listeners
        this.setupUIListeners();
        this.restoreUI();

        // check notification access
        this.checkNotificationAccess();

        // start service
        Intent serviceIntent = new Intent(this, NotifyteService.class);
        this.startService(serviceIntent);

        // register receivers
        this.registerReceiver(serviceStopReceiver, new IntentFilter(Intents.INTENT_SERVICE_STOP));
        this.registerReceiver(bluetoothLeReceiver, new IntentFilter(Intents.INTENT_BLUETOOTH));
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy");
        // unregister receivers
        this.unregisterReceiver(serviceStopReceiver);
        this.unregisterReceiver(bluetoothLeReceiver);

        super.onDestroy();
    }

    private void setupUIListeners() {
        button_bluetooth = (Button) findViewById(R.id.button_bluetooth);
        button_scan = (Button) findViewById(R.id.button_scan);
        button_connect = (Button) findViewById(R.id.button_connect);
        text_device = (TextView) findViewById(R.id.text_device);
        text_device.setText(getString(R.string.text_device) + " " + getString(R.string.text_device_none));
        fab = (FloatingActionButton) findViewById(R.id.fab);

        button_bluetooth.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(LOG_TAG, "button_bluetooth clicked");
                Intent i = new Intent(Intents.INTENT_UI);
                i.putExtra(Intents.INTENT_EXTRA_MSG, Intents.INTENT_ACTION_BLUETOOTH_ENABLE);
                MainActivity.this.sendBroadcast(i);
            }
        });

        button_scan.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Log.d(LOG_TAG, "button_scan clicked");
                Intent i = new Intent(Intents.INTENT_UI);
                i.putExtra(Intents.INTENT_EXTRA_MSG, Intents.INTENT_ACTION_BLUETOOTH_SCAN);
                MainActivity.this.sendBroadcast(i);
                Log.d(LOG_TAG, "showProgressScan");
                showProgressScan();
            }
        });

        button_connect.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Log.d(LOG_TAG, "button_connect clicked");
                if (value != null) {
                    Intent i = new Intent(Intents.INTENT_UI);
                    i.putExtra(Intents.INTENT_EXTRA_MSG, Intents.INTENT_ACTION_BLUETOOTH_CONNECT);
                    i.putExtra(Intents.INTENT_EXTRA_DATA, value);
                    MainActivity.this.sendBroadcast(i);
                }
            }
        });

        fab.setOnClickListener(new FloatingActionButton.OnClickListener() {
            public void onClick(View v) {
                Log.d(LOG_TAG, "fab clicked");
            }
        });
    }

    private void restoreUI() {
        String name = preferences.getString(preferences.DEVICE_NAME);
        String addr = preferences.getString(preferences.DEVICE_ADDR);

        if(!name.equals(preferences.STRING_DEFAULT)) {
            entry = name;
            if(text_device != null) {
                text_device.setText(getString(R.string.text_device) + " " + entry);
            }
        }
        if(!addr.equals(preferences.STRING_DEFAULT)) {
            value = addr;
        }
    }

    private void checkNotificationAccess() {
        ContentResolver contentResolver = this.getContentResolver();
        String notificationListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        String packageName = this.getPackageName();

        if(notificationListeners == null || !notificationListeners.contains(packageName)){
            Log.d(LOG_TAG, "NotifyteNotification Access Disabled");
            this.showNotificationAccess();
        }
        else {
            Log.d(LOG_TAG, "NotifyteNotification Access Enabled");
        }
    }

    private void showNotificationAccess() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.dialog_access_title);
        dialog.setMessage(R.string.dialog_access_message);
        dialog.setCancelable(false);

        dialog.setPositiveButton(R.string.dialog_access_open, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {
                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            }
        });
        dialog.setNegativeButton(R.string.dialog_access_close, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int index) {
            }
        });


        AlertDialog alert = dialog.create();
        alert.show();
    }

    private void showProgressConnecting() {
        progress_connecting = new ProgressDialog(MainActivity.this);
        progress_connecting.setMessage(getString(R.string.progress_dialog_connecting));
        progress_connecting.setCancelable(false);
        progress_connecting.show();
    }

    private void closeProgressConnecting() {
        if(progress_connecting != null) {
            progress_connecting.dismiss();
        }
    }

    private void showProgressScan() {
        Log.d(LOG_TAG, "showProgressScan()");
        progress_scan = new ProgressDialog(MainActivity.this);
        progress_scan.setMessage(getString(R.string.progress_dialog_scan));
        progress_scan.setCancelable(false);
        progress_scan.show();
        Log.d(LOG_TAG, "showProgressScan show");
    }

    private void closeProgressScan() {
        if(progress_scan != null) {
            progress_scan.dismiss();
        }
    }

    private void showDialogScan(final CharSequence[] entries, final CharSequence[] values) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.dialog_scan_title);
        builder.setItems(entries, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int index) {
                entry = entries[index];
                value = values[index];

                preferences.saveString(preferences.DEVICE_NAME, entry.toString());
                preferences.saveString(preferences.DEVICE_ADDR, value.toString());

                Log.d(LOG_TAG, "Device selected: " + entry + ":" + value);
                if(text_device != null) {
                    text_device.setText(getString(R.string.text_device) + " " + entry);
                }
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private BroadcastReceiver serviceStopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "serviceStopReceiver");
            MainActivity.this.unregisterReceiver(bluetoothLeReceiver);
            MainActivity.this.unregisterReceiver(serviceStopReceiver);
            MainActivity.this.finish();
        }
    };

    private BroadcastReceiver bluetoothLeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "bluetoothLeReceiver");

            String message = intent.getStringExtra(Intents.INTENT_EXTRA_MSG);
            Log.d(LOG_TAG, message);

            if(message.equals(Intents.INTENT_BLUETOOTH_ENABLED)) {
                bleEnabled = true;
            }
            if(message.equals(Intents.INTENT_BLUETOOTH_DISABLED)) {
                bleEnabled = false;
            }
            if(message.equals(Intents.INTENT_BLUETOOTH_CONNECTING)) {
                boolean connecting = intent.getBooleanExtra(Intents.INTENT_EXTRA_DATA, false);
                if(connecting) {
                    MainActivity.this.showProgressConnecting();
                }
                else {
                    MainActivity.this.closeProgressConnecting();
                }
            }
            if(message.equals(Intents.INTENT_BLUETOOTH_CONNECTED)) {
                Log.d(LOG_TAG, "connected to device");
            }
            if(message.equals(Intents.INTENT_BLUETOOTH_DISCONNECTED)) {
                Log.d(LOG_TAG, "disconnected to device");
            }
            if(message.equals(Intents.INTENT_BLUETOOTH_CONNECTED_DESKTOP)) {
                Log.d(LOG_TAG, "connected to notifyte desktop app");
            }
            if(message.equals(Intents.INTENT_BLUETOOTH_SCAN_STOPPED)) {
                MainActivity.this.closeProgressScan();
            }
            if(message.equals(Intents.INTENT_BLUETOOTH_DEVICES_LIST)) {
                CharSequence[] entries = intent.getCharSequenceArrayExtra(Intents.INTENT_BLUETOOTH_ENTRIES);
                CharSequence[] values = intent.getCharSequenceArrayExtra(Intents.INTENT_BLUETOOTH_VALUES);

                MainActivity.this.showDialogScan(entries, values);
            }
            if(message.equals(Intents.INTENT_BLUETOOTH_DEVICE)) {
                String device = intent.getStringExtra(Intents.INTENT_EXTRA_DATA);
                Log.d(LOG_TAG, "device: " + device);
            }

            if(button_bluetooth != null) {
                if(bleEnabled == true) {
                    button_bluetooth.setText(R.string.button_bluetooth_off);
                }
                else {
                    button_bluetooth.setText(R.string.button_bluetooth_on);
                }
            }
        }
    };
}
