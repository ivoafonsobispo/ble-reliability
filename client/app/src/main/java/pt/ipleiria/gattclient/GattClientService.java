package pt.ipleiria.gattclient;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.HashMap;

public class GattClientService extends Service {
    private boolean scanning = false;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothAdapter mBluetoothAdapter;
    private HashMap<String, BluetoothDevice> availableDevices;
    private GattCallbackInterface mInterface = null;
    private static boolean mBound = false;

    public interface GattCallbackInterface {
        void onGattDeviceFound(BluetoothDevice device, String id);
        void onGattServerConnected(BluetoothGatt bluetoothGatt);
        void onGattServerDisconnected(BluetoothGatt bluetoothGatt);
    }

    // Binder given to clients
    public class BLEServiceBinder extends Binder {
        GattClientService getService() {
            return GattClientService.this;
        }
    }

    private final IBinder mBinder = new BLEServiceBinder();

    @Override
    public IBinder onBind(Intent intent) {
        mBound = true;
        initializeBluetooth();
        availableDevices = new HashMap<>();
        return mBinder;
    }

    private void initializeBluetooth() {
        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager != null) {
            mBluetoothAdapter = mBluetoothManager.getAdapter();
            if (mBluetoothAdapter == null) {
                Log.i("GattClientService", "Bluetooth NOT SUPPORTED");
            } else if (mBluetoothAdapter.isEnabled()) {
                Log.i("GattClientService", "Bluetooth ENABLED");
            } else {
                Log.i("GattClientService", "Bluetooth DISABLED");
            }
        }
    }

    public void scanLeDevice(final boolean enable) {
        scanning = false;
        if (mBound && mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            if (enable) {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
                scanning = true;
            } else {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }
    }

    public void setInterface(GattCallbackInterface bleInterface) {
        mInterface = bleInterface;
    }

    public boolean isScanning() {
        return scanning;
    }

    public boolean isBluetoothEnabled() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    public void connectToDevice(String id) {
        BluetoothDevice device = availableDevices.get(id);
        if (device != null) {
            if (mBluetoothGatt != null) mBluetoothGatt.disconnect();
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        }
    }

    public void clearAvailableDevices() {
        availableDevices.clear();
    }

    private final BluetoothAdapter.LeScanCallback mLeScanCallback = (device, rssi, scanRecord) -> {
        if (availableDevices.get(device.getAddress()) == null) {
            availableDevices.put(device.getAddress(), device);
            mInterface.onGattDeviceFound(device, device.getAddress());
        }
    };

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mInterface.onGattServerConnected(mBluetoothGatt);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mInterface.onGattServerDisconnected(mBluetoothGatt);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            // Handle characteristic read operation
        }

        // Implement other callback methods as needed
    };
}