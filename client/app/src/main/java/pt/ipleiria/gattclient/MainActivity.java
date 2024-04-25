package pt.ipleiria.gattclient;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


public class MainActivity extends Activity {
    ExpandableListAdapter mExpandableListAdapter;
    ExpandableListView mExpandableListView;
    List<String> mExpandableListGroups;
    HashMap<String, List<String>> mExpandableListChildMap;
    TextView textViewLog;
    Button scanButton, clearButton; // Buttons
    final String GROUP_DEVICE = "BLE Device Addresses";
    private GattClientService mGattClientService = null;
    private boolean mBound = false;
    Button sendDataButton;
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("00000000-0000-1000-8000-00805f9b34fb");
    private BluetoothGatt bluetoothGatt;

    private final GattClientService.GattCallbackInterface mBLEInterface = new GattClientService.GattCallbackInterface() {
        @Override
        public void onGattDeviceFound(final BluetoothDevice device, final String id) {
            Log.i("GattCallbackInterface", "Found Device:\n\tADDR : " + device.getAddress());
            runOnUiThread(() -> onDeviceFound(device, id));
        }

        @Override
        public void onGattServerConnected(final BluetoothGatt bluetoothGatt) {
            Log.i("GattCallbackInterface", "Connected to Device:\n\tADDR : " + bluetoothGatt.getDevice().getAddress());
            runOnUiThread(() -> onDeviceConnected(bluetoothGatt));
        }

        @Override
        public void onGattServerDisconnected(final BluetoothGatt bluetoothGatt) {
            Log.i("GattCallbackInterface", "Disconnected from Device:\n\tADDR : " + bluetoothGatt.getDevice().getAddress());
            runOnUiThread(() -> onDeviceDisconnected(bluetoothGatt));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check and request necessary permissions
        if (!checkPermissions()) {
            requestLocationPermission();
            return;
        }

        // Initialize UI components
        initializeUI();

        // Bind to GattClientService
        bindToGattClientService();

        // Find and set click listener for the send data button
        sendDataButton = findViewById(R.id.buttonSendData);
        sendDataButton.setOnClickListener(v -> sendData());
    }

    private void initializeUI() {
        textViewLog = findViewById(R.id.textViewLog);
        textViewLog.setMovementMethod(new ScrollingMovementMethod());

        scanButton = findViewById(R.id.buttonScan);
        scanButton.setOnClickListener(v -> toggleScan());

        clearButton = findViewById(R.id.buttonClear);
        clearButton.setOnClickListener(v -> clearData());

        // Initialize expandable list view
        mExpandableListGroups = new ArrayList<>();
        mExpandableListChildMap = new HashMap<>();
        mExpandableListGroups.add(GROUP_DEVICE);
        mExpandableListChildMap.put(GROUP_DEVICE, new ArrayList<>());
        mExpandableListAdapter = new ExpandableListAdapter(this, mExpandableListGroups, mExpandableListChildMap);
        mExpandableListView = findViewById(R.id.expandableListView);
        mExpandableListView.setAdapter(mExpandableListAdapter);
        for (int i = 0; i < mExpandableListGroups.size(); i++)
            mExpandableListView.expandGroup(i);
        mExpandableListView.setOnChildClickListener((parent, v, groupPosition, listPosition, id) -> {
            handleListItemClick(groupPosition, listPosition);
            return false;
        });
    }

    private void bindToGattClientService() {
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Intent intent = new Intent(this, GattClientService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        } else {
            Toast.makeText(this, "Bluetooth LE is not supported", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
    }

    private void toggleScan() {
        if (mGattClientService != null) {
            mGattClientService.scanLeDevice(!mGattClientService.isScanning());
            if (!mGattClientService.isBluetoothEnabled()) {
                Log.i("MainActivity", "Bluetooth Not Enabled");
                Toast.makeText(this, "Bluetooth Not Enabled!", Toast.LENGTH_SHORT).show();
            }
            scanButton.setText(mGattClientService.isScanning() ? "Stop Scan" : "Start Scan");
        }
    }

    private void clearData() {
        if (mGattClientService != null) mGattClientService.clearAvailableDevices();
        for (String s : mExpandableListGroups) {
            Objects.requireNonNull(mExpandableListChildMap.get(s)).clear();
        }
        mExpandableListAdapter.notifyDataSetChanged();
        if (textViewLog.getEditableText() != null) textViewLog.getEditableText().clear();
        textViewLog.bringPointIntoView(0);
    }

    private void handleListItemClick(int groupPosition, int listPosition) {
        String group = mExpandableListGroups.get(groupPosition);
        if (mGattClientService != null && group != null) {
            String data = Objects.requireNonNull(mExpandableListChildMap.get(group)).get(listPosition);
            Toast.makeText(this, group + ":" + data, Toast.LENGTH_SHORT).show();
            Log.i("ExpandableListView", group + ":" + data);
            if (group.equals(GROUP_DEVICE)) {
                textViewLog.append("\nTrying to Connect to Device Addr:" + data + "...\n");
                mGattClientService.connectToDevice(data);
            }
        } else {
            Log.e("ExpandableListView", "OnClicked Child: GATT SERVICE NOT CONNECTED TO ACTIVITY!");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    // Other lifecycle methods like onStart(), onResume(), onPause(), onStop() can remain unchanged.

    // Check if location permission is granted
    public boolean checkPermissions() {
        int res = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    // Service connection to GattClientService
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i("ServiceConnection", "GattClientService Connected to activity");
            mGattClientService = ((GattClientService.BLEServiceBinder) service).getService();
            mGattClientService.setInterface(mBLEInterface);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            Log.i("ServiceConnection", "GattClientService Disconnected from activity");
        }
    };

    // Callback for device found event
    private void onDeviceFound(final BluetoothDevice device, String id) {
        Log.i("MainActivity", "Found Device ADDR:" + id);
        List<String> childList = mExpandableListChildMap.get(GROUP_DEVICE);
        if (childList != null && device != null) {
            childList.add(device.getAddress());
            mExpandableListAdapter.notifyDataSetChanged();
        }
    }

    // Callback for device connected event
    private void onDeviceConnected(final BluetoothGatt bluetoothGatt) {
        if (bluetoothGatt != null) {
            textViewLog.append("Connected to GATT Server on device:\n   " + bluetoothGatt.getDevice().getAddress() + "\n");
            this.bluetoothGatt = bluetoothGatt;
        }
    }

    // Callback for device disconnected event
    private void onDeviceDisconnected(final BluetoothGatt bluetoothGatt) {
        if (bluetoothGatt != null) {
            textViewLog.append("Disconnected from device:\n   " + bluetoothGatt.getDevice().getAddress() + "\n");
            mExpandableListAdapter.notifyDataSetChanged();
        }
    }

    // Send Value 1 to GATT Server
    private void sendData() {
        if (bluetoothGatt != null) {
            textViewLog.append(System.currentTimeMillis() + " - Sent Data: 1\n");

            BluetoothGattCharacteristic characteristic = bluetoothGatt.getService(CHARACTERISTIC_UUID)
                    .getCharacteristic(CHARACTERISTIC_UUID);
            characteristic.setValue(1, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            bluetoothGatt.writeCharacteristic(characteristic);

        }
    }
}