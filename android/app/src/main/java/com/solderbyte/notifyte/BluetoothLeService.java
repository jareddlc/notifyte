package com.solderbyte.notifyte;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

public class BluetoothLeService extends Service {
    private static final String LOG_TAG = "Notifyte:BluetoothLe";

    // states
    public static boolean isEnabled = false;
    public static boolean isConnected = false;
    public static boolean isScanning = false;
    public static boolean isRemaining = false;
    public static boolean isReceiving = false;
    public static boolean isReconnecting = false;
    public static boolean canReconnect = false;
    public static volatile boolean isThreadRunning = false;

    // bluetooth
    private static BluetoothAdapter bluetoothAdapter;
    private static BluetoothManager bluetoothManager;
    private static BluetoothGatt bluetoothGatt;
    private static BluetoothDevice bluetoothDevice;
    private static BluetoothSocket bluetoothSocket;
    private static BluetoothServerSocket bluetoothServerSocket;
    private static BluetoothGattCharacteristic bluetoothCharacteristic;
    private static BluetoothGattDescriptor bluetoothDescriptor;
    public static InputStream inputStream;
    public static OutputStream outputStream;

    // bluetooth states
    private String bluetoothAddress;
    private static Set<BluetoothDevice> pairedDevices;
    private static Set<BluetoothDevice> scannedDevices;

    // threads
    private static BluetoothThread bluetoothThread;
    private static ConnectBluetoothThread connectThread;
    private static EnableBluetoothThread enableThread;
    private static ReconnectBluetoothThread reconnectThread;

    // globals
    private Context context;
    private final static long SCAN_PERIOD = 7000;
    private final static int BYTE_MAX = 512;
    private final static int BYTE_MTU = 20;
    private byte[] BYTE_BUFFER = null;
    ArrayList BYTE_RX_BUFFER = new ArrayList();
    private static int RECONNECT_ATTEMPTS = 0;
    private static long RECCONECT_TIME = 0;
    public final static String EXTRA_DATA = "EXTRA_DATA";
    public final static String ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE";
    public final static String ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED";
    private static final UUID NOTIFYTE_MOBILE_UUID = UUID.fromString("0000fffa-0000-1000-8000-00805f9b34fb");
    private static final UUID NOTIFYTE_DESKTOP_UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    private static final UUID NOTIFYTE_CHAR_UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
    private static final UUID NOTIFYTE_DESC_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // gatt states
    public static HashMap<Integer, String> gattStatus = new HashMap<Integer, String>() {{
        put(0, "GATT_SUCCESS");
        put(2, "GATT_READ_NOT_PERMITTED");
        put(3, "GATT_WRITE_NOT_PERMITTED");
        put(5, "GATT_INSUFFICIENT_AUTHENTICATION");
        put(6, "GATT_REQUEST_NOT_SUPPORTED");
        put(7, "GATT_INVALID_OFFSET");
        put(13, "GATT_INVALID_ATTRIBUTE_LENGTH");
        put(15, "GATT_INSUFFICIENT_ENCRYPTION");
        put(143, "GATT_CONNECTION_CONGESTED");
        put(257, "GATT_FAILURE");
    }};
    public static HashMap<Integer, String> gattState = new HashMap<Integer, String>() {{
        put(0, "STATE_DISCONNECTED");
        put(1, "STATE_CONNECTING");
        put(2, "STATE_CONNECTED");
        put(3, "STATE_DISCONNECTING");
    }};
    public static HashMap<Integer, String> gattServiceType = new HashMap<Integer, String>() {{
        put(0, "SERVICE_TYPE_PRIMARY");
        put(1, "SERVICE_TYPE_SECONDARY");
    }};
    public static HashMap<Integer, String> gattCharacteristicPermission = new HashMap<Integer, String>() {{
        put(1, "PERMISSION_READ");
        put(2, "PERMISSION_READ_ENCRYPTED");
        put(4, "PERMISSION_READ_ENCRYPTED_MITM");
        put(16, "PERMISSION_WRITE");
        put(32, "PERMISSION_WRITE_ENCRYPTED");
        put(64, "PERMISSION_WRITE_ENCRYPTED_MITM");
        put(128, "PERMISSION_WRITE_SIGNED");
        put(256, "PERMISSION_WRITE_SIGNED_MITM");
    }};
    public static HashMap<Integer, String> gattCharacteristicProperty = new HashMap<Integer, String>() {{
        put(1, "PROPERTY_BROADCAST");
        put(2, "PROPERTY_READ");
        put(4, "PROPERTY_WRITE_NO_RESPONSE");
        put(8, "PROPERTY_WRITE");
        put(16, "PROPERTY_NOTIFY");
        put(32, "PROPERTY_INDICATE");
        put(64, "PROPERTY_SIGNED_WRITE");
        put(128, "PROPERTY_EXTENDED_PROPS");
    }};
    public static HashMap<Integer, String> gattCharacteristicWriteType = new HashMap<Integer, String>() {{
        put(1, "WRITE_TYPE_NO_RESPONSE");
        put(2, "WRITE_TYPE_DEFAULT");
        put(4, "WRITE_TYPE_SIGNED");
    }};

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(LOG_TAG, "onConnectionStateChange: " + status + ":" + gattStatus.get(status) + ":" + gattState.get(newState));

            if(newState == BluetoothProfile.STATE_CONNECTED) {
                isConnected = true;
                BluetoothLeService.this.stopReconnectBle();
                Log.d(LOG_TAG, "Connected");
                BluetoothLeService.this.broadcastUpdate(ACTION_GATT_CONNECTED);
                if(context != null) {
                    Intent msg = new Intent(Intents.INTENT_BLUETOOTH);
                    msg.putExtra(Intents.INTENT_EXTRA_MSG, Intents.INTENT_BLUETOOTH_CONNECTED);
                    context.sendBroadcast(msg);
                }
                // attempts to discover services after successful connection.
                bluetoothGatt.discoverServices();
            }
            else if(newState == BluetoothProfile.STATE_DISCONNECTED) {
                isConnected = false;
                Log.d(LOG_TAG, "Disconnected");
                BluetoothLeService.this.broadcastUpdate(ACTION_GATT_DISCONNECTED);
                if(context != null) {
                    Intent msg = new Intent(Intents.INTENT_BLUETOOTH);
                    msg.putExtra(Intents.INTENT_EXTRA_MSG, Intents.INTENT_BLUETOOTH_DISCONNECTED);
                    context.sendBroadcast(msg);
                }
                if(!isReconnecting) {
                    BluetoothLeService.this.reconnectBle();
                }
            }
            Intent msg = new Intent(Intents.INTENT_BLUETOOTH);
            msg.putExtra(Intents.INTENT_EXTRA_MSG, Intents.INTENT_BLUETOOTH_CONNECTING);
            msg.putExtra(Intents.INTENT_EXTRA_DATA, false);
            context.sendBroadcast(msg);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(LOG_TAG, "onServicesDiscovered: " + gattStatus.get(status));
            if(status == BluetoothGatt.GATT_SUCCESS) {

                // loops through available GATT Services.
                for(BluetoothGattService gattService : gatt.getServices()) {
                    String uuid = gattService.getUuid().toString();
                    String type = gattServiceType.get(gattService.getType());

                    Log.d(LOG_TAG, "gattService: " + gattService);
                    Log.d(LOG_TAG, "gattService type: " + type);
                    Log.d(LOG_TAG, "gattService uuid: " + uuid);
                    // get characteristic when UUID matches
                    if(uuid.equals(BluetoothLeService.NOTIFYTE_DESKTOP_UUID.toString())) {
                        Log.d(LOG_TAG, "Service Found: Notifyte Desktop App");

                        bluetoothCharacteristic = gattService.getCharacteristic(BluetoothLeService.NOTIFYTE_CHAR_UUID);
                        gatt.setCharacteristicNotification(bluetoothCharacteristic, true);
                        Log.d(LOG_TAG, "bluetoothCharacteristic: " + bluetoothCharacteristic);

                        bluetoothDescriptor = bluetoothCharacteristic.getDescriptor(BluetoothLeService.NOTIFYTE_DESC_UUID);
                        bluetoothDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(bluetoothDescriptor);

                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            boolean mtu = gatt.requestMtu(BluetoothLeService.BYTE_MAX);
                            Log.d(LOG_TAG, "requestMtu: " + mtu + " : " + BluetoothLeService.BYTE_MAX);
                        }
                        else {
                            Log.d(LOG_TAG, "requestMtu: " + 20);
                        }

                        Log.d(LOG_TAG, "bluetoothDescriptor: " + bluetoothDescriptor);

                        Intent msg = new Intent(Intents.INTENT_BLUETOOTH);
                        msg.putExtra(Intents.INTENT_EXTRA_MSG, Intents.INTENT_BLUETOOTH_CONNECTED_DESKTOP);
                        context.sendBroadcast(msg);
                    }

                    for(BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
                        String cUuid = gattCharacteristic.getUuid().toString();
                        int cInstanceId = gattCharacteristic.getInstanceId();
                        int cPermissions = gattCharacteristic.getPermissions();
                        int cProperties = gattCharacteristic.getProperties();
                        byte[] cValue = gattCharacteristic.getValue();
                        int cWriteType = gattCharacteristic.getWriteType();

                        // Log.d(LOG_TAG, "gattCharacteristic cUuid: " + cUuid);
                        // Log.d(LOG_TAG, "gattCharacteristic cInstanceId: " + cInstanceId);
                        // Log.d(LOG_TAG, "gattCharacteristic cPermissions: " + cPermissions + ":" + gattCharacteristicPermission.get(cPermissions));
                        // Log.d(LOG_TAG, "gattCharacteristic cProperties: " + cProperties + ":"  + gattCharacteristicProperty.get(cProperties));
                        // Log.d(LOG_TAG, "gattCharacteristic cValue: " + cValue);
                        // Log.d(LOG_TAG, "gattCharacteristic cWriteType: " + cWriteType + ":"  +  gattCharacteristicWriteType.get(cWriteType));
                    }
                }
                BluetoothLeService.this.broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            }
            else {
                Log.d(LOG_TAG, "onServicesDiscovered: none");
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(LOG_TAG, "onCharacteristicRead: " + characteristic + ":" + gattStatus.get(status));
            if(status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothLeService.this.broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(LOG_TAG, "onCharacteristicWrite: " + characteristic + ":" + gattStatus.get(status));
            //BluetoothLeService.this.broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            if(isRemaining) {
                Log.d(LOG_TAG, "onCharacteristicWrite: isRemaining");
                isRemaining = false;
                BluetoothLeService.this.writeBle(BYTE_BUFFER);
            }
            if(gattStatus.get(status).equals("GATT_INVALID_ATTRIBUTE_LENGTH")) {

            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(LOG_TAG, "onCharacteristicChanged: " + characteristic);
            BluetoothLeService.this.broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        Log.d(LOG_TAG, "broadcastUpdate: " + action);

        final Intent intent = new Intent(action);
        this.sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        Log.d(LOG_TAG, "broadcastUpdate: " + characteristic);
        final Intent intent = new Intent(action);
        // for all other profiles, writes the data formatted in HEX.
        final byte[] data = characteristic.getValue();

        if(isReceiving) {
            Log.d(LOG_TAG, "adding to buffer");
            BYTE_RX_BUFFER.add(data);
        }
        else {
            Log.d(LOG_TAG, "creating new buffer");
            BYTE_RX_BUFFER = new ArrayList();
            BYTE_RX_BUFFER.add(data);
        }

        if(data.length == BYTE_MTU) {
            Log.d(LOG_TAG, "isReceiving");
            isReceiving = true;
        }
        else {
            Log.d(LOG_TAG, "isNotReceiving");
            isReceiving = false;
        }

        if(data != null && data.length > 0) {
            String str = new String(data);
            intent.putExtra(EXTRA_DATA, new String(data));
            if(str.contains("\0")) {
                isReceiving = false;

                String message = new String();
                for(int i = 0; i < BYTE_RX_BUFFER.size(); i++) {
                    byte[] b = (byte[]) BYTE_RX_BUFFER.get(i);
                    String temp = new String(b);
                    message += temp;
                }
                BYTE_RX_BUFFER = new ArrayList();
                Log.d(LOG_TAG, "End of message: " + message);

                Intent msg = new Intent(Intents.INTENT_BLUETOOTH);
                msg.putExtra(Intents.INTENT_EXTRA_MSG, Intents.INTENT_BLUETOOTH_NOTIFICATION);
                msg.putExtra(Intents.INTENT_EXTRA_DATA, message);
                context.sendBroadcast(msg);
            }
        }
        this.sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            Log.d(LOG_TAG, "getService");
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind");
        return iBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(LOG_TAG, "onUnbind");
        this.stopSelf();
        this.close();
        return super.onUnbind(intent);
    }

    private final IBinder iBinder = new LocalBinder();

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if(bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(LOG_TAG, "bluetoothAdapter is null or bluetoothGatt is null");
            return;
        }
        Log.d(LOG_TAG, "readCharacteristic");
        bluetoothGatt.readCharacteristic(characteristic);
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if(bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(LOG_TAG, "bluetoothAdapter is null or bluetoothGatt is null");
            return;
        }
        Log.d(LOG_TAG, "setCharacteristicNotification: " + enabled);
        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if(bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(LOG_TAG, "bluetoothAdapter is null or bluetoothGatt is null");
            return;
        }
        Log.d(LOG_TAG, "writeCharacteristic");
        bluetoothGatt.writeCharacteristic(characteristic);
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if(bluetoothGatt == null) {
            Log.w(LOG_TAG, "bluetoothGatt is null");
            return null;
        }
        return bluetoothGatt.getServices();
    }

    public boolean initialize() {
        Log.d(LOG_TAG, "initialize");
        context = BluetoothLeService.this.getApplicationContext();
        scannedDevices = new LinkedHashSet<BluetoothDevice>();

        if(bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) BluetoothLeService.this.getSystemService(Context.BLUETOOTH_SERVICE);
            if(bluetoothManager == null) {
                Log.e(LOG_TAG, "initialize failed. bluetoothManager is null");
                return false;
            }
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        if(bluetoothAdapter == null) {
            Log.e(LOG_TAG, "initialize failed. bluetoothAdapter is null");
            return false;
        }

        if(bluetoothAdapter.isEnabled()) {
            isEnabled = true;
            Log.d(LOG_TAG, "bluetoothAdapter is enabled");
            Intent msg = new Intent(Intents.INTENT_BLUETOOTH);
            msg.putExtra(Intents.INTENT_EXTRA_MSG, Intents.INTENT_BLUETOOTH_ENABLED);
            context.sendBroadcast(msg);
        }
        else {
            isEnabled = false;
            Log.d(LOG_TAG, "bluetoothAdapter is not enabled");
            Intent msg = new Intent(Intents.INTENT_BLUETOOTH);
            msg.putExtra(Intents.INTENT_EXTRA_MSG, Intents.INTENT_BLUETOOTH_DISABLED);
            context.sendBroadcast(msg);
        }
        return true;
    }

    public boolean connectBle(final String address) {
        Log.d(LOG_TAG, "connectBle: " + address);
        if(bluetoothAdapter == null || address == null) {
            Log.d(LOG_TAG, "bluetoothAdapter is null or address is null");
            return false;
        }

        if(bluetoothAddress != null && address.equals(bluetoothAddress) && bluetoothGatt != null) {
            if(bluetoothGatt.connect()) {
                this.forceConnectBle();
                return true;
            }
            else {
                Log.d(LOG_TAG, "could not force reconnect");
                return false;
            }
        }

        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if(device == null) {
            Log.d(LOG_TAG, "BluetoothDevice is null");
            return false;
        }
        // auto connectBle to the device
        Log.d(LOG_TAG, "connecting...");
        bluetoothGatt = device.connectGatt(this, false, mGattCallback);
        bluetoothAddress = address;
        Intent msg = new Intent(Intents.INTENT_BLUETOOTH);
        msg.putExtra(Intents.INTENT_EXTRA_MSG, Intents.INTENT_BLUETOOTH_CONNECTING);
        msg.putExtra(Intents.INTENT_EXTRA_DATA, true);
        context.sendBroadcast(msg);
        return true;
    }

    public void disconnectBle() {
        Log.d(LOG_TAG, "disconnectBle");
        if(bluetoothGatt == null) {
            Log.d(LOG_TAG, "bluetoothAdapter is null or bluetoothGatt is null");
            return;
        }
        bluetoothGatt.disconnect();
    }

    public void forceConnectBle() {
        Log.d(LOG_TAG, "forceConnectBle");
        if(bluetoothAddress != null) {
            this.disconnectBle();
            Log.d(LOG_TAG, "force connecting...");
            final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(bluetoothAddress);
            bluetoothGatt = device.connectGatt(this, false, mGattCallback);
            Intent msg = new Intent(Intents.INTENT_BLUETOOTH);
            msg.putExtra(Intents.INTENT_EXTRA_MSG, Intents.INTENT_BLUETOOTH_CONNECTING);
            msg.putExtra(Intents.INTENT_EXTRA_DATA, true);
            context.sendBroadcast(msg);
        }
        else {
            Log.d(LOG_TAG, "bluetoothAddress is null");
        }
    }

    public void writeBle(byte[] bytes) {
        Log.d(LOG_TAG, "writeBle: " + bytes.length);
        if(isEnabled) {
            if(isConnected) {
                if(bluetoothCharacteristic != null) {

                    if(bytes.length > BYTE_MAX) {
                        byte[] bytesToSend = Arrays.copyOfRange(bytes, 0, BYTE_MAX);
                        BYTE_BUFFER = Arrays.copyOfRange(bytes, BYTE_MAX, bytes.length);
                        isRemaining = true;
                        bytes = bytesToSend;
                    }

                    if(bluetoothCharacteristic.setValue(bytes)) {
                        this.writeCharacteristic(bluetoothCharacteristic);
                        this.setCharacteristicNotification(bluetoothCharacteristic, true);
                    }
                    else {
                        Log.d(LOG_TAG, "bluetoothCharacteristic could not be set");
                    }
                }
                else {
                    Log.d(LOG_TAG, "bluetoothCharacteristic is null");
                }
            }
            else {
                Log.d(LOG_TAG, "Bluetooth not connected");
            }
        }
        else {
            Log.d(LOG_TAG, "Bluetooth not enabled");
        }
    }

    public void setReconnect(boolean value) {
        canReconnect = value;
    }

    public void reconnectBle() {
        Log.d(LOG_TAG, "reconnectBle");
        if(canReconnect) {
            isReconnecting = true;
            reconnectThread = new ReconnectBluetoothThread();
            reconnectThread.start();
        }
    }

    public void stopReconnectBle() {
        Log.d(LOG_TAG, "stopReconnectBle");
        RECONNECT_ATTEMPTS = 0;
        isReconnecting = false;
        if(reconnectThread != null) {
            reconnectThread.close();
            reconnectThread = null;
        }
    }

    public void close() {
        Log.d(LOG_TAG, "close");
        if(bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        if(connectThread != null) {
            connectThread.close();
            connectThread = null;
        }
    }

    public void enableBluetooth() {
        Log.d(LOG_TAG, "enableBluetooth");
        if(!bluetoothAdapter.isEnabled()) {
            enableThread = new EnableBluetoothThread();
            enableThread.start();
        }
        else {
            Log.d(LOG_TAG, "BluetoothAdapter is enabled");
        }
    }

    public void disableBluetooth() {
        Log.d(LOG_TAG, "disableBluetooth");
        bluetoothAdapter.disable();
        isEnabled = false;
    }

    public void connectRfcomm() {
        Log.d(LOG_TAG, "connectRfcomm");
        if(!bluetoothAdapter.isEnabled()) {
            Log.d(LOG_TAG, "bluetoothAdapter is not enabled");
            return;
        }
        if(bluetoothDevice != null) {
            connectThread = new ConnectBluetoothThread();
            connectThread.start();
        }
        else {
            Log.d(LOG_TAG, "bluetoothDevice is null");
        }
    }

    public void disconnectRfcomm() {
        Log.d(LOG_TAG, "disconnectRfcomm");
        if(bluetoothDevice != null && isConnected) {
            connectThread.close();
        }
        else {
            Log.d(LOG_TAG, "bluetoothDevice is null or not connected");
        }
    }

    public void writeRfComm(byte[] bytes) {
        Log.d(LOG_TAG, "writeRfComm");
        if(bluetoothThread != null) {
            bluetoothThread.write(bytes);
        }
        else {
            Log.d(LOG_TAG, "bluetoothThread is null");
        }
    }

    public void setAddress(String address) {
        Log.d(LOG_TAG, "setAddress: " + address);
        bluetoothAddress = address;
        setBluetoothDevice(bluetoothAddress);
    }

    public static void setBluetoothDevice(String address) {
        Log.d(LOG_TAG, "setBluetoothDevice: " + address);
        // loop through devices
        if(pairedDevices != null) {
            Log.d(LOG_TAG, "setting from paired devices");
            for(BluetoothDevice device : pairedDevices) {
                if(device.getAddress().equals(address)) {
                    Log.d(LOG_TAG, "Set paired device: " + device.getName() + ":" + device.getAddress());
                    bluetoothDevice = device;
                    return;
                }
            }
        }
        if(scannedDevices.size() > 0) {
            Log.d(LOG_TAG, "setting from scanned devices");
            for(BluetoothDevice device : scannedDevices) {
                if(device.getAddress().equals(address)) {
                    Log.d(LOG_TAG, "Set scanned device: " + device.getName() + ":" + device.getAddress());
                    bluetoothDevice = device;
                    return;
                }
            }
        }

        if(pairedDevices == null && scannedDevices.size() <= 0) {
            Log.d(LOG_TAG, "pairedDevices and scannedDevices are null or empty");
        }
    }

    public void setEntries() {
        Log.d(LOG_TAG, "setEntries");
        if(isEnabled) {
            List<CharSequence> entries = new ArrayList<CharSequence>();
            List<CharSequence> values = new ArrayList<CharSequence>();
            pairedDevices = bluetoothAdapter.getBondedDevices();
            // loop through paired devices
            if(pairedDevices.size() > 0) {
                for(BluetoothDevice device : pairedDevices) {
                    String deviceName = device.getName();
                    String deviceAddr = device.getAddress();
                    Log.d(LOG_TAG, "Paired Device: " + deviceName + ":" + deviceAddr);
                    if(deviceName != null && !deviceName.isEmpty() && deviceAddr != null && !deviceAddr.isEmpty()) {
                        entries.add(deviceName);
                        values.add(deviceAddr);
                    }
                }
            }
            else {
                Log.d(LOG_TAG, "No pairedDevices");
            }
            // loop trough scanned devices
            if(scannedDevices.size() > 0) {
                for(BluetoothDevice device : scannedDevices) {
                    // make sure we dont add duplicates
                    if(!entries.contains(device.getName())) {
                        String deviceName = device.getName();
                        String deviceAddr = device.getAddress();
                        Log.d(LOG_TAG, "Scanned Device: " + deviceName + ":" + deviceAddr);
                        if(deviceName != null && !deviceName.isEmpty() && deviceAddr != null && !deviceAddr.isEmpty()) {
                            entries.add(deviceName);
                            values.add(deviceAddr);
                        }
                    }
                }
            }
            else {
                Log.d(LOG_TAG, "No scannedDevices");
            }

            CharSequence[] pairedEntries = entries.toArray(new CharSequence[entries.size()]);
            CharSequence[] pairedEntryValues = values.toArray(new CharSequence[values.size()]);

            Intent msg = new Intent(Intents.INTENT_BLUETOOTH);
            msg.putExtra(Intents.INTENT_EXTRA_MSG, Intents.INTENT_BLUETOOTH_DEVICES_LIST);
            msg.putExtra(Intents.INTENT_BLUETOOTH_ENTRIES, pairedEntries);
            msg.putExtra(Intents.INTENT_BLUETOOTH_VALUES, pairedEntryValues);
            context.sendBroadcast(msg);
        }
        else {
            Log.d(LOG_TAG, "Bluetooth is not enabled");
        }
    }

    @SuppressLint("NewApi")
    public void scan() {
        Log.d(LOG_TAG, "scan");
        if(isEnabled) {
            if(context != null) {
                if(!isScanning) {
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        Log.d(LOG_TAG, "scanning with startLeScan: " + SCAN_PERIOD + "ms");
                        new android.os.Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                isScanning = false;
                                bluetoothAdapter.stopLeScan(mLeScanCallback);
                                Log.d(LOG_TAG, "scanned stopped");

                                Intent msg = new Intent(Intents.INTENT_BLUETOOTH);
                                msg.putExtra(Intents.INTENT_EXTRA_MSG, Intents.INTENT_BLUETOOTH_SCAN_STOPPED);
                                context.sendBroadcast(msg);

                                BluetoothLeService.this.setEntries();
                            }
                        }, SCAN_PERIOD);

                        isScanning = true;
                        bluetoothAdapter.startLeScan(mLeScanCallback);
                    }
                    else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Log.d(LOG_TAG, "scanning with startScan: " + SCAN_PERIOD + "ms");
                        new android.os.Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                isScanning = false;
                                BluetoothLeScanner mBluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                                mBluetoothLeScanner.stopScan(mScanCallback);
                                Log.d(LOG_TAG, "scanned stopped");

                                Intent msg = new Intent(Intents.INTENT_BLUETOOTH);
                                msg.putExtra(Intents.INTENT_EXTRA_MSG, Intents.INTENT_BLUETOOTH_SCAN_STOPPED);
                                context.sendBroadcast(msg);
                                BluetoothLeService.this.setEntries();
                            }
                        }, SCAN_PERIOD);

                        isScanning = true;
                        BluetoothLeScanner mBluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                        mBluetoothLeScanner.startScan(mScanCallback);
                    }
                    else {
                        Log.d(LOG_TAG, "scanning with startDiscovery: " + SCAN_PERIOD + "ms");
                        // Bluetooth Classic
                        new android.os.Handler().postDelayed(new Runnable() {
                            public void run() {
                                isScanning = false;
                                bluetoothAdapter.cancelDiscovery();
                                Log.d(LOG_TAG, "scanned stopped");

                                Intent msg = new Intent(Intents.INTENT_BLUETOOTH);
                                msg.putExtra(Intents.INTENT_EXTRA_MSG, Intents.INTENT_BLUETOOTH_SCAN_STOPPED);
                                context.sendBroadcast(msg);
                                BluetoothLeService.this.setEntries();
                                BluetoothLeService.this.unregisterReceiver(scanReceiver);
                            }
                        }, SCAN_PERIOD);


                        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                        this.registerReceiver(scanReceiver, filter);
                        isScanning = true;
                        bluetoothAdapter.startDiscovery();
                    }
                }
                else {
                    Log.d(LOG_TAG, "currently scanning");
                }
            }
            else{
                Log.d(LOG_TAG, "Handler is null");
            }
        }
        else {
            Log.d(LOG_TAG, "Bluetooth is not enabled");
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.d(LOG_TAG, "onLeScan: " + rssi);
            if(scannedDevices.add(device)) {
                Log.d(LOG_TAG, device.getName() + " : " + device.getAddress() + " : " + device.getType() + " : " + device.getBondState());
                Intent msg = new Intent(Intents.INTENT_BLUETOOTH);
                msg.putExtra(Intents.INTENT_EXTRA_MSG, Intents.INTENT_BLUETOOTH_DEVICE);
                msg.putExtra(Intents.INTENT_EXTRA_DATA, device.getName() + "," + device.getAddress());
                context.sendBroadcast(msg);
            }
        }
    };

    @SuppressLint("NewApi")
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d(LOG_TAG, "onScanResult: " + callbackType);
            BluetoothDevice device = result.getDevice();
            if(scannedDevices.add(device)) {
                Log.d(LOG_TAG, device.getName() + " : " + device.getAddress() + " : " + device.getType() + " : " + device.getBondState());
                Intent msg = new Intent(Intents.INTENT_BLUETOOTH);
                msg.putExtra(Intents.INTENT_EXTRA_MSG, Intents.INTENT_BLUETOOTH_DEVICE);
                msg.putExtra(Intents.INTENT_EXTRA_DATA, device.getName() + "," + device.getAddress());
                context.sendBroadcast(msg);
            }
        }
    };

    private final BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "scanReceiver");
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(scannedDevices.add(device)) {
                    Log.d(LOG_TAG, device.getName() + " : " + device.getAddress() + " : " + device.getType() + " : " + device.getBondState());
                    Intent msg = new Intent(Intents.INTENT_BLUETOOTH);
                    msg.putExtra(Intents.INTENT_EXTRA_MSG, Intents.INTENT_BLUETOOTH_DEVICE);
                    msg.putExtra(Intents.INTENT_EXTRA_DATA, device.getName() + "," + device.getAddress());
                    context.sendBroadcast(msg);
                }
            }
        }
    };

    private class EnableBluetoothThread extends Thread {
        public void run() {
            boolean bluetoothEnabled = true;
            long timeStart = Calendar.getInstance().getTimeInMillis();
            Log.d(LOG_TAG, "EnableBluetoothThread: " + timeStart);

            bluetoothAdapter.enable();
            while(!bluetoothAdapter.isEnabled()) {
                try {
                    long timeDiff =  Calendar.getInstance().getTimeInMillis() - timeStart;
                    if(timeDiff >= 5000) {
                        bluetoothEnabled = false;
                        break;
                    }
                    Thread.sleep(100L);
                }
                catch (InterruptedException ie) {
                    // unexpected interruption while enabling bluetooth
                    Thread.currentThread().interrupt(); // restore interrupted flag
                    return;
                }
            }
            if(bluetoothEnabled) {
                isEnabled = true;
                Intent msg = new Intent(Intents.INTENT_BLUETOOTH);
                msg.putExtra(Intents.INTENT_EXTRA_MSG, Intents.INTENT_BLUETOOTH_ENABLED);
                context.sendBroadcast(msg);
                Log.d(LOG_TAG, "Enabled");
            }
            else {
                isEnabled = false;
                Intent msg = new Intent(Intents.INTENT_BLUETOOTH);
                msg.putExtra(Intents.INTENT_EXTRA_MSG, Intents.INTENT_BLUETOOTH_ENABLED_FAILED);
                context.sendBroadcast(msg);
                Log.d(LOG_TAG, "Timed out");
            }
        }
    }

    private class ConnectBluetoothThread extends Thread {
        public ConnectBluetoothThread() {
            Log.d(LOG_TAG, "ConnectBluetoothThread");

            // get a BluetoothSocket to connectBle with the given BluetoothDevice
            try {
                Log.d(LOG_TAG, "try ConnectBluetoothThread: " + bluetoothDevice.getName() + " with UUID: " + NOTIFYTE_MOBILE_UUID.toString());
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(NOTIFYTE_MOBILE_UUID);
            }
            catch(Exception e) {
                Log.e(LOG_TAG, "Error: bluetoothDevice.createRfcommSocketToServiceRecord()", e);
            }
        }

        public void run() {
            Log.d(LOG_TAG, "Running ConnectBluetoothThread");
            // Cancel discovery because it will slow down the connection
            bluetoothAdapter.cancelDiscovery();

            try {
                // connectBle the device through the socket. This will block until it succeeds or throws an exception
                bluetoothSocket.connect();
                isConnected = true;
                if(context != null) {
                    Intent msg = new Intent(Intents.INTENT_BLUETOOTH);
                    msg.putExtra(Intents.INTENT_EXTRA_MSG, Intents.INTENT_BLUETOOTH_RFCOMM_CONNECTED);
                    context.sendBroadcast(msg);
                }
            }
            catch(IOException connectException) {
                Log.e(LOG_TAG, "Error: bluetoothSocket.connectBle()", connectException);
                try {
                    bluetoothSocket.close();
                    if(context != null) {
                        Intent msg = new Intent(Intents.INTENT_BLUETOOTH);
                        msg.putExtra(Intents.INTENT_EXTRA_MSG, Intents.INTENT_BLUETOOTH_RFCOMM_CONNECTED_FAILED);
                        context.sendBroadcast(msg);
                    }
                }
                catch(IOException closeException) {
                    Log.e(LOG_TAG, "Error: BluetoothSocket.close()", closeException);
                }
                return;
            }
            Log.d(LOG_TAG, "ConnectBluetoothThread connected");
            // Manage the connection
            bluetoothThread = new BluetoothThread();
            bluetoothThread.start();
        }

        public void close() {
            if(bluetoothThread != null) {
                isThreadRunning = false;
                bluetoothThread.close();
                isConnected = false;
                Log.d(LOG_TAG, "ConnectBluetoothThread close");
                if(context != null) {
                    Intent msg = new Intent(Intents.INTENT_BLUETOOTH);
                    msg.putExtra(Intents.INTENT_EXTRA_MSG, Intents.INTENT_BLUETOOTH_RFCOMM_DISCONNECTED);
                    context.sendBroadcast(msg);
                }
            }
        }
    }

    public class BluetoothThread extends Thread {
        public BluetoothThread() {
            Log.d(LOG_TAG, "BluetoothThread");

            // get the input and output streams
            try {
                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();
                isThreadRunning = true;
            }
            catch(IOException e) {
                close();
                Log.e(LOG_TAG, "Error: bluetoothSocket.getInputStream()/socket.getOutputStream()", e);
            }
        }

        public void run() {
            Log.d(LOG_TAG, "Running BluetoothThread");
            ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];

            // listen to the InputStream
            while(isThreadRunning) {
                try {
                    int bytes = inputStream.read(buffer);
                    byteArray.write(buffer, 0, bytes);
                    Log.d(LOG_TAG, "Received: " + byteArray);
                    try {
                        Intent msg = new Intent(Intents.INTENT_BLUETOOTH);
                        msg.putExtra(Intents.INTENT_EXTRA_MSG, Intents.INTENT_BLUETOOTH_DATA);
                        msg.putExtra(Intents.INTENT_BLUETOOTH_DATA, byteArray.toByteArray());
                        context.sendBroadcast(msg);
                    }
                    catch(Exception e) {
                        Log.e(LOG_TAG, "Error: mHandler.obtainMessage()", e);
                    }

                    byteArray.reset();
                }
                catch(IOException e) {
                    if(isThreadRunning) {
                        Log.e(LOG_TAG, "Error: inputStream.read()", e);
                        close();
                        bluetoothThread.close();
                        if(connectThread != null) {
                            connectThread.close();
                            connectThread = null;
                        }
                    }
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
                byteArray.write(bytes, 0, bytes.length);
                Log.d(LOG_TAG, "Sending: " + byteArray);
                outputStream.write(bytes);
                outputStream.flush();
            }
            catch(IOException e) {
                Log.e(LOG_TAG, "Error: outputStream.write()", e);
            }
        }

        public void close() {
            try {
                inputStream.close();
            }
            catch(IOException e) {
                Log.e(LOG_TAG, "Error: inputStream.close()", e);
            }
            try {
                outputStream.close();
            }
            catch(IOException e) {
                Log.e(LOG_TAG, "Error: outputStream.close()", e);
            }
            try {
                bluetoothSocket.close();
            }
            catch(IOException e) {
                Log.e(LOG_TAG, "Error: bluetoothSocket.close()", e);
            }
        }
    }

    @SuppressWarnings("unused")
    private class ServerThread extends Thread {
        public ServerThread() {
            Log.d(LOG_TAG, "ServerThread");

            try {
                Log.d(LOG_TAG, "try ServerThread with UUID: " + NOTIFYTE_MOBILE_UUID);
                bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("SessionManagerSecure", NOTIFYTE_MOBILE_UUID);
            }
            catch(IOException e) {
                Log.e(LOG_TAG, "Error bluetoothAdapter.listenUsingRfcommWithServiceRecord()");
                e.printStackTrace();
            }
        }

        public void run() {
            Log.d(LOG_TAG, "Running ServerThread");

            try {
                bluetoothServerSocket.accept();
                Log.d(LOG_TAG, "bluetoothServerSocket.accept() success");
            }
            catch(IOException e) {
                Log.e(LOG_TAG, "Error bluetoothServerSocket.accept()");
                e.printStackTrace();
            }
        }

        public void close() {
            try {
                bluetoothServerSocket.close();
            }
            catch(IOException e) {
                Log.e(LOG_TAG, "Error: mmSocket.close()", e);
            }
        }
    }

    private class ReconnectBluetoothThread extends Thread {
        public void run() {
            long timeStart = Calendar.getInstance().getTimeInMillis();
            Log.d(LOG_TAG, "Reconnecting Bluetooth: " + timeStart);

            while(isReconnecting) {
                long lastAttempt = Calendar.getInstance().getTimeInMillis() - RECCONECT_TIME;

                if(allowedReconnect(lastAttempt / 1000)) {
                    Log.d(LOG_TAG, "Attempting to reconnect: " +  RECONNECT_ATTEMPTS);
                    RECCONECT_TIME = Calendar.getInstance().getTimeInMillis();
                    BluetoothLeService.this.disconnectBle();
                    BluetoothLeService.this.connectBle(bluetoothAddress);
                }
            }
        }

        public void close() {
            isReconnecting = false;
        }
    }

    public boolean allowedReconnect(long elapsed) {
        boolean allowedReconnect = false;

        if(elapsed > 30 && RECONNECT_ATTEMPTS < 4) {
            // every 30 secs (2 min)
            RECONNECT_ATTEMPTS += 1;
            allowedReconnect = true;
        }
        else if(elapsed > 60 && RECONNECT_ATTEMPTS < 9) {
            // every 1 min (5 mins)
            RECONNECT_ATTEMPTS += 1;
            allowedReconnect = true;
        }
        else if(elapsed > 300 && RECONNECT_ATTEMPTS < 21) {
            // every 5 min (1 hr)
            RECONNECT_ATTEMPTS += 1;
            allowedReconnect = true;
        }

        if(RECONNECT_ATTEMPTS > 21) {
            RECONNECT_ATTEMPTS = 0;
            Log.d(LOG_TAG, "reconnects attempts exceeded. giving up");
            BluetoothLeService.this.disconnectBle();
            BluetoothLeService.this.stopReconnectBle();
            allowedReconnect = false;
        }

        return allowedReconnect;
    }
}
