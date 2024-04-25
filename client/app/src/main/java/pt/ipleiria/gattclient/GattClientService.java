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

/**
 * Created by 138 on 1/9/2017.
 */

public class GattClientService extends Service {
    private boolean scanning = false;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothAdapter mBluetoothAdapter; //deviceListAdapter to scan for le devices
    private static boolean mBound = false;
    private HashMap<String, BluetoothDevice> availableDevices;
    public interface GattCallbackInterface{
        void onGattDeviceFound(final BluetoothDevice device, final String id);
        void onGattServerConnected(final BluetoothGatt bluetoothGatt);
        void onGattServerDisconnected(final BluetoothGatt bluetoothGatt);
    }
    GattCallbackInterface mInterface = null;
    // Binder given to clients

    private final IBinder mBinder = new BLEServiceBinder();
    // Random number generator

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class BLEServiceBinder extends Binder {
        GattClientService getService() {
            // Return this instance of LocalService so clients can call public methods
            return GattClientService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        mBound = true;
        // Initializes Bluetooth deviceListAdapter.
        //bluetooth system service management class
        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE); //get service
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if(mBluetoothAdapter == null){
            Log.i("GattClientService", "Bluetooth NOT SUPPORTED");
        }
        else if(mBluetoothAdapter.isEnabled()) {
            Log.i("GattClientService", "Bluetooth ENABLED");
        }
        else {
            Log.i("GattClientService", "Bluetooth DISABLED");
        }
        availableDevices = new HashMap<>();
        return mBinder;
    }
    //user methods//
    public void scanLeDevice(final boolean enable) {
        scanning = false;
        if(mBound && mBluetoothAdapter != null && mBluetoothAdapter.isEnabled())//if enabled
        {
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

    public boolean isScanning() {return scanning;}

    public boolean isBluetoothEnabled() {return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();}

    public void connectToDevice(String id){
        BluetoothDevice d = availableDevices.get(id);
        if(d != null)
        {
            if(mBluetoothGatt != null )mBluetoothGatt.disconnect(); //disconnect if already connected
            mBluetoothGatt = d.connectGatt(this,false,mGattCallback);
            if(mBluetoothGatt != null ) {
                mBluetoothGatt.connect();
            }
            else
                Log.i("GattClientService", "Device returned null Gatt Object");

        }
    }


    public void clearAvailableDevices(){
        availableDevices.clear();

    }
    // Device scan callback.

    private final BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    if(availableDevices.get(device.getAddress()) == null)
                    {
                        availableDevices.put(device.getAddress(), device);
                        mInterface.onGattDeviceFound(device, device.getAddress());
                    }
                }
            };

    //gatt callback
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback(){
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
        int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("GattClientService:", "Connected to GATT server.");
                mInterface.onGattServerConnected(mBluetoothGatt);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("GattClientService:", "Disconnected to Gatt Server on Device: " + mBluetoothGatt.getDevice());
                mInterface.onGattServerDisconnected(mBluetoothGatt);
            }
        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("GattClientService:", "Read GattCharacteristic:" + characteristic);
            }else{
                Log.i("GattClientService:", "Failed to Read GattCharacteristic: STATUS:" +status);
            }
        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic
                characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("GattClientService:", "Wrote GattCharacteristic:" + characteristic);
            }else{
                Log.i("GattClientService:", "Failed to Wrote GattCharacteristic: STATUS:" +status);
            }
        }
        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                     int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("GattClientService:", "Wrote GattDescriptor" + descriptor);
            }else{
                Log.i("GattClientService:", "Failed to Wrote GattDescriptor: STATUS:" +status);
            }
        }
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                      int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("GattClientService:", "Wrote GattDescriptor:" + descriptor);
            }else{
                Log.i("GattClientService:", "Failed to Wrote GattDescriptor: STATUS:" +status);
            }
        }
    };
}
