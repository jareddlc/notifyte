package com.solderbyte.notifyte;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class NotifyteService extends Service {
    private static final String LOG_TAG = "Notifyte:Service";

    // notification bar
    private int notificationId = 882;
    private Notification notification = null;

    // bluetooth
    private BluetoothLeService bluetoothLeService = null;

    // states
    private boolean bleEnabled = false;
    private boolean isConnected = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand: " + flags + ":" + startId);
        // register receivers
        this.registerReceiver(serviceStopReceiver, new IntentFilter(Intents.INTENT_SERVICE_STOP));
        this.registerReceiver(notificationReceiver, new IntentFilter(Intents.INTENT_NOTIFICATION));
        this.registerReceiver(bluetoothLeReceiver, new IntentFilter(Intents.INTENT_BLUETOOTH));
        this.registerReceiver(uiReceiver, new IntentFilter(Intents.INTENT_UI));

        // start service
        this.createNotification(false);
        this.startBluetoothService();
        this.startNotificationService();

        this.startForeground(notificationId, notification);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy");
        unregisterReceiver(serviceStopReceiver);
        super.onDestroy();
    }

    public void createNotification(boolean connected) {
        Log.d(LOG_TAG, "createNotification: " + connected);
        Intent stopService =  new Intent(Intents.INTENT_SERVICE_STOP);
        Intent startActivity = new Intent(this, MainActivity.class);
        PendingIntent startIntent = PendingIntent.getActivity(this, 0, startActivity, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stopIntent = PendingIntent.getBroadcast(this, 0, stopService, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(this);
        nBuilder.setSmallIcon(R.mipmap.icon_launcher);
        nBuilder.setContentTitle(getString(R.string.notification_title));
        if(connected || isConnected) {
            nBuilder.setContentText(getString(R.string.notification_connected));
        }
        else {
            nBuilder.setContentText(getString(R.string.notification_disconnected));
        }
        nBuilder.setContentIntent(startIntent);
        nBuilder.setAutoCancel(true);
        nBuilder.setOngoing(true);
        nBuilder.addAction(R.mipmap.icon_shutdown, getString(R.string.notification_close), stopIntent);
        if(connected) {
            Intent cIntent = new Intent(Intents.INTENT_BLUETOOTH);
            cIntent.putExtra(Intents.INTENT_EXTRA_MSG, Intents.INTENT_ACTION_BLUETOOTH_DISCONNECT);
            PendingIntent pConnect = PendingIntent.getBroadcast(this, 0, cIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            nBuilder.addAction(R.mipmap.icon_bluetooth_disconnected, getString(R.string.notification_disconnect), pConnect);
        }
        else {
            Intent cIntent = new Intent(Intents.INTENT_BLUETOOTH);
            cIntent.putExtra(Intents.INTENT_EXTRA_MSG, Intents.INTENT_ACTION_BLUETOOTH_CONNECT);
            PendingIntent pConnect = PendingIntent.getBroadcast(this, 0, cIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            nBuilder.addAction(R.mipmap.icon_bluetooth_connected, getString(R.string.notification_connect), pConnect);
        }

        // Sets an ID for the notification
        NotificationManager nManager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
        notification = nBuilder.build();
        nManager.notify(notificationId, notification);
    }

    public void clearNotification() {
        NotificationManager nManager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
        nManager.cancel(notificationId);
    }

    public void startBluetoothService() {
        Log.d(LOG_TAG, "startBluetoothService");
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        this.bindService(gattServiceIntent, bluetoothServiceConnection, Context.BIND_AUTO_CREATE);
    }

    protected ServiceConnection bluetoothServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d(LOG_TAG, "Bluetooth onServiceConnected");
            bluetoothLeService = ((BluetoothLeService.LocalBinder)service).getService();
            if(!bluetoothLeService.initialize()) {
                Log.e(LOG_TAG, "Unable to initialize BluetoothLE");
            }
            //sendServiceStarted();
            //sendUIPreferences();

            // auto connectBle
            //bluetoothLeService.connectRfcomm();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(LOG_TAG, "Bluetooth onServiceDisconnected");
            bluetoothLeService = null;
        }
    };

    public void startNotificationService() {
        Log.d(LOG_TAG, "startNotificationService");
        Intent notificationIntent = new Intent(this, NotificationService.class);
        this.startService(notificationIntent);
    }

    public void writeBluetooth(byte[] bytes) {
        if(bluetoothLeService != null || bytes != null) {
            bluetoothLeService.writeBle(bytes);
        }
        else {
            Log.w(LOG_TAG, "bluetoothLeService is null or bytes is null");
        }
    }

    private BroadcastReceiver serviceStopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "serviceStopReceiver");
            // clear states
            bleEnabled = false;

            // unregister receivers
            NotifyteService.this.unregisterReceiver(notificationReceiver);
            NotifyteService.this.unregisterReceiver(bluetoothLeReceiver);
            NotifyteService.this.unregisterReceiver(uiReceiver);

            // unbind services
            NotifyteService.this.unbindService(bluetoothServiceConnection);

            // clear other
            NotifyteService.this.clearNotification();
            NotifyteService.this.stopForeground(true);
            NotifyteService.this.stopSelf();
        }
    };

    private BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "notificationReceiver");
            final String appName = intent.getStringExtra(Intents.INTENT_NOTIFICATION_APP_NAME);
            final String packageName = intent.getStringExtra(Intents.INTENT_NOTIFICATION_PACKAGE_NAME);
            final String name = intent.getStringExtra(Intents.INTENT_NOTIFICATION_NAME);
            final String contact = intent.getStringExtra(Intents.INTENT_NOTIFICATION_CONTACT);
            final String group = intent.getStringExtra(Intents.INTENT_NOTIFICATION_GROUP);
            final String message = intent.getStringExtra(Intents.INTENT_NOTIFICATION_MESSAGE);
            final boolean reply = intent.getBooleanExtra(Intents.INTENT_NOTIFICATION_REPLY, false);
            final long created = intent.getLongExtra(Intents.INTENT_NOTIFICATION_CREATED, 0);
            final int id = intent.getIntExtra(Intents.INTENT_NOTIFICATION_ID, 0);


            JSONObject json = new JSONObject();
            try {
                json.put(Intents.JSON_APP_NAME, appName);
                json.put(Intents.JSON_PACKAGE_NAME, packageName);
                json.put(Intents.JSON_NAME, name);
                json.put(Intents.JSON_CONTACT, contact);
                json.put(Intents.JSON_GROUP, group);
                json.put(Intents.JSON_MESSAGE, message);
                json.put(Intents.JSON_REPLY, reply);
                json.put(Intents.JSON_CREATED, created);
                json.put(Intents.JSON_ID, id);
            }
            catch(JSONException e) {
                Log.e(LOG_TAG, "Error: creating JSON" + e);
                e.printStackTrace();
            }

            byte[] bytes = new byte[0];
            try {
                bytes = json.toString().getBytes("utf-8");
            }
            catch(UnsupportedEncodingException e) {
                Log.e(LOG_TAG, "Error: converting JSON to byte[]" + e);
                e.printStackTrace();
            }
            Log.d(LOG_TAG, "Sending notification: " + json.toString());
            NotifyteService.this.writeBluetooth(bytes);
        }
    };

    private BroadcastReceiver bluetoothLeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "bluetoothLeReceiver");

            String message = intent.getStringExtra(Intents.INTENT_EXTRA_MSG);
            Log.d(LOG_TAG, message);

            if(message.equals(Intents.INTENT_BLUETOOTH_ENABLED)) {
                Log.d(LOG_TAG, "Bluetooth enabled");
                bleEnabled = true;
            }
            if(message.equals(Intents.INTENT_BLUETOOTH_DISABLED)) {
                Log.d(LOG_TAG, "Bluetooth disabled");
                bleEnabled = false;
            }
            if(message.equals(Intents.INTENT_BLUETOOTH_SCAN_STOPPED)) {
                Log.d(LOG_TAG, "Bluetooth scanning stopped");
            }
            if(message.equals(Intents.INTENT_BLUETOOTH_CONNECTED)) {
                Log.d(LOG_TAG, "connected to device");
                NotifyteService.this.createNotification(true);
            }
            if(message.equals(Intents.INTENT_BLUETOOTH_DISCONNECTED)) {
                Log.d(LOG_TAG, "disconnected to device");
                isConnected = false;
                NotifyteService.this.createNotification(false);
            }
            if(message.equals(Intents.INTENT_BLUETOOTH_CONNECTED_DESKTOP)) {
                isConnected = true;
                Log.d(LOG_TAG, "connected to notifyte desktop app");
            }
        }
    };

    private BroadcastReceiver uiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "uiReceiver");

            String message = intent.getStringExtra(Intents.INTENT_EXTRA_MSG);
            Log.d(LOG_TAG, message);

            if(message.equals(Intents.INTENT_ACTION_BLUETOOTH_SCAN)) {
                if(bluetoothLeService != null) {
                    bluetoothLeService.scan();
                }
            }
            if(message.equals(Intents.INTENT_ACTION_BLUETOOTH_ENABLE)) {
                Log.d(LOG_TAG, "bluetooth enable");
            }
            if(message.equals(Intents.INTENT_ACTION_BLUETOOTH_CONNECT)) {
                String device = intent.getStringExtra(Intents.INTENT_EXTRA_DATA);
                bluetoothLeService.setAddress(device);
                bluetoothLeService.connectBle(device);
                bluetoothLeService.setReconnect(true);
                Log.d(LOG_TAG, "Connecting to: " + device);
            }
        }
    };
}


