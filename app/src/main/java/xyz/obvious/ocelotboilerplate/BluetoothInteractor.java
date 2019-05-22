/*
 * Copyright (c) 2019 4iiii Innovations Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use,copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package xyz.obvious.ocelotboilerplate;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import androidx.fragment.app.FragmentActivity;

/**
 * This class performs the communication with the OS implementation of Bluetooth protocol.
 * The current implementation only handles the communication with a single Bluetooth device and passes
 * all the events and data to the listener interfaces injected into this object.
 */
class BluetoothInteractor
{
    /**
     * A static string used to tag log messages that this class writes when in debug mode
     */
    private static String LOG_TAG = BluetoothInteractor.class.getSimpleName();
    /**
     * The default period to remain scanning for Bluetooth low energy devices
     */
    private static int DEFAULT_SCAN_PERIOD = 30000;

    /**
     * The current instance of the BluetoothInteractor
     */
    private static BluetoothInteractor _instance = null;

    /**
     * The Intent request code for enabling the Bluetooth on the phone
     */
    static int ENABLE_REQUEST_CODE = 4321;

    /**
     * The OS Bluetooth manager used to communicate with the Bluetooth adapter on the phone
     */
    private BluetoothManager _bluetoothManager = null;
    /**
     * The default Bluetooth adapter being used on the phone
     */
    private BluetoothAdapter _defaultBluetoothAdapter;

    /**
     * The current GATT device that has been connected to using this interactor
     */
    private BluetoothGatt _deviceGatt = null;

    /**
     * The callback that receives the Bluetooth events from the OS BluetoothManager
     */
    private BluetoothInteractorGattCallback _gattCallback = null;

    /**
     * The callback that is used to collect scan results returned by the Bluetooth scanner.
     * NB: this callback is used for older API levels, i.e. prior to Build.VERSION_CODES.LOLLIPOP
     */
    private BluetoothAdapter.LeScanCallback _leScanCallback = null;
    /**
     * The callback that is used to collect scan results returned by the Bluetooth scanner.
     * NB: this callback is used for newer API levels, i.e. Build.VERSION_CODES.LOLLIPOP and above
     */
    private ScanCallback _newLeScanCallback = null;
    /**
     * This listener is used to pass scan results back to the App layer
     */
    private BluetoothScanResultListener _scanResultListener = null;

    /**
     * Used to track the unique devices found during the scan
     */
    private HashMap<String,BluetoothDevice> _scanList = new HashMap<>();
    /**
     * The scan data associated with each device found
     */
    private ArrayList<BluetoothScanResultListener.DeviceRecord> _scanResults = new ArrayList<>();
    /**
     * The services to filter on when performing a Bluetooth scan
     */
    private ArrayList<ParcelUuid> _serviceFilterList = null;

    private boolean _scanning = false;
    private Runnable _leStopScan = null;
    private Handler _handler = null;
    private int scanPeriod = DEFAULT_SCAN_PERIOD;

    /**
     * The BluetoothObviousDevice device object that this interactor is working with.  The data
     * and error events are passed on to this object.
     */
    private BluetoothObviousDevice _bleDevice = null;

    /**
     * Lock used to make sure descriptor writes happen in sequence
     */
    private final Object _descriptorLock = new Object();
    private boolean _descriptorStatus = false;
    private boolean _clearServiceCache = false;

    /**
     * Factory for creating an instance of the BluetoothInteractor.
     * @param ctx The context to use for accessing application resources.
     * @return An instance of the BluetoothInteractor
     */
    static BluetoothInteractor getServiceClient(Context ctx) {
        if (_instance == null) {
            _instance = new BluetoothInteractor();
            _instance._handler = new Handler(ctx.getMainLooper());
        }
        return _instance;
    }

    /**
     * This method passed the scan results to the App listener setup when the scan operation was started
     */
    private void _notifyScanResults() {
        if (_scanResultListener == null) { return; }
        _scanResultListener.onScanResults(_scanResults);
    }

    /**
     * Pass the connection event on to the App
     * @param status the state change status
     * @param state the state that was reached
     */
    private void _notifyConnectionStateChange(final int status, final int state) {
        if (_bleDevice != null && _bleDevice.getConnectionListener() != null) {
            _handler.post(new Runnable() {
                final BluetoothObviousDevice notifyDev = _bleDevice;
                @Override
                public void run() {
                    notifyDev.getConnectionListener().onConnectionStateChange(status, state);
                }
            });
        }
    }

    /**
     * Used to pass characteristic data to the App.  This method is used for both read and notified/indicated
     * data characteristics.
     * @param datatype the characteristic that has changed.
     * @param rawdata the raw data bytes of the characteristic value.
     */
    private void _notifyDataChange(final String datatype, final byte[] rawdata) {
        if (_bleDevice != null) {
            _handler.post(new Runnable() {
                final BluetoothObviousDevice notifyDev = _bleDevice;
                @Override
                public void run() {
                    notifyDev.onDataNotification(datatype,rawdata);
                }
            });
        }
    }

    /**
     * Used to pass characteristic read errors to the App.
     * @param datatype the characteristic that has changed.
     * @param status the Bluetooth error status code.
     */
    private void _notifyReadErrorStatus(final String datatype, final int status) {
        if (_bleDevice != null) {
            _handler.post(new Runnable() {
                final BluetoothObviousDevice notifyDev = _bleDevice;
                @Override
                public void run() {
                    notifyDev.onDataReadErrorNotification(datatype,status);
                }
            });
        }
    }

    /**
     * Used to pass characteristic write errors to the App.
     * @param datatype the characteristic that has changed.
     * @param status the Bluetooth error status code.
     */
    private void _notifyWriteStatus(final String datatype, final int status) {
        if (_bleDevice != null) {
            _handler.post(new Runnable() {
                final BluetoothObviousDevice notifyDev = _bleDevice;
                @Override
                public void run() {
                    notifyDev.onDataWriteNotification(datatype,status);
                }
            });
        }
    }

    /**
     * Setup the OS Bluetooth adapter/
     * @param curActivity The Android Activity using the Bluetooth adapter
     * @return true if the hardware has been initialized, false otherwise
     */
    private boolean _setupBluetoothHardware(FragmentActivity curActivity) {
        boolean enabled = true;

        _bluetoothManager = (BluetoothManager)curActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        if (_bluetoothManager != null) {
            _defaultBluetoothAdapter = _bluetoothManager.getAdapter();
        }

        if (_defaultBluetoothAdapter != null) {
            enabled = _defaultBluetoothAdapter.isEnabled();
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "Bluetooth enabled: " + enabled);
            }
        }

        if (_defaultBluetoothAdapter == null || !_defaultBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "Start Enable Bluetooth Activity");
            }
            curActivity.startActivityForResult(enableBtIntent,ENABLE_REQUEST_CODE);
            enabled = false;
        }

        _leScanCallback = new BluetoothAdapter.LeScanCallback() {
            long lastProgressiveNotification = 0;
            @Override
            public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                lastProgressiveNotification = _callbackProcessResults(device,lastProgressiveNotification);
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            _newLeScanCallback = new ScanCallback() {
                long lastProgressiveNotification = 0;
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    _processScanResult(result);
                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    super.onBatchScanResults(results);
                    for (ScanResult newResult : results) {
                        _processScanResult(newResult);
                    }
                }

                @Override
                public void onScanFailed(int errorCode) {
                    super.onScanFailed(errorCode);
                }

                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                private void _processScanResult(ScanResult result) {
                    lastProgressiveNotification = _callbackProcessResults(result.getDevice(),lastProgressiveNotification);
                }
            };
        }

        _leStopScan = new Runnable() {
            @Override
            public void run() {
                _scanning = false;

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    _defaultBluetoothAdapter.stopLeScan(_leScanCallback);
                } else {
                    if (_defaultBluetoothAdapter.getBluetoothLeScanner() != null) {
                        _defaultBluetoothAdapter.getBluetoothLeScanner().stopScan(_newLeScanCallback);
                    }
                }
                _notifyScanResults();

                if (BuildConfig.DEBUG) {
                    Log.d(LOG_TAG, "_leStopScan - finished stop scan runnable");
                }
            }
        };

        return enabled;
    }

    /**
     * Process the scan results from the scan callback
     * @param device The device that was found diring the scan
     * @param lastProgressiveNotification The timestamp of the last time the scan results were passed back to the App listener
     * @return The new notification timestamp.
     */
    private long _callbackProcessResults(final BluetoothDevice device, final long lastProgressiveNotification) {
        String deviceId = device.getAddress();
        String name = device.getName();
        if (!_scanList.containsKey(deviceId)) {
            _scanList.put(deviceId, device);
            _scanResults.add(new BluetoothScanResultListener.DeviceRecord((name == null || "".equals(name)) ? deviceId : name, deviceId));
        }
        long curTime = System.currentTimeMillis();
        if (curTime-lastProgressiveNotification > 80) {
            _notifyScanResults();
            return curTime;
        }
        return lastProgressiveNotification;
    }

    /**
     * Start the Bluetooth low energy scan process
     * @param activity The activity that wants to initiate the scan.
     * @param scanServices The array of services that should be used to filter the reults
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void startBLEScan(FragmentActivity activity, ArrayList<ParcelUuid> scanServices) {
        if (_scanning) { return; }

        if (_defaultBluetoothAdapter == null || !_defaultBluetoothAdapter.isEnabled()) {
            // If we need to ask the user about enabling Bluetooth then cancel the scan
            if (!_setupBluetoothHardware(activity)) {
                if (BuildConfig.DEBUG) {
                    Log.d(LOG_TAG, "_setupBluetoothHardware - enable Bluetooth activity started, cancel scan");
                }
                _scanning = false;
                _notifyScanResults();
                return;
            }
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "_setupBluetoothHardware - complete");
            }
        }

        if (!LocationStatusInteractor.isLocationEnabled(activity)) {
            // Location services is not enable, request this now
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "LocationStatusInteractor.requestLocationEnable - enable Location services");
            }
            LocationStatusInteractor.requestLocationEnable(activity);
            _scanning = false;
            _notifyScanResults();
            return;
        }

        _scanList.clear();
        _scanResults.clear();

        for (BluetoothDevice connectedDev : _bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)) {
            String deviceId = connectedDev.getAddress();
            if (!_scanList.containsKey(deviceId)) {
                if (BuildConfig.DEBUG) {
                    Log.d(LOG_TAG, "startBLEScan - Adding other connected device " + deviceId);
                }
                _scanList.put(deviceId, connectedDev);
            }
        }

        _scanning = true;
        _handler.postDelayed(_leStopScan, scanPeriod);
        if (scanServices == null || scanServices.size() == 0) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                _defaultBluetoothAdapter.startLeScan(_leScanCallback);
            } else {
                ScanSettings.Builder newSettings = new ScanSettings.Builder();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    newSettings.setMatchMode(ScanSettings.MATCH_MODE_STICKY);
                    newSettings.setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT);
                }
                newSettings.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
                newSettings.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
                if (_defaultBluetoothAdapter.getBluetoothLeScanner() != null) {
                    _defaultBluetoothAdapter.getBluetoothLeScanner().startScan(new ArrayList<ScanFilter>(), newSettings.build(), _newLeScanCallback);
                }
            }
        } else {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                UUID[] serviceUuids = new UUID[scanServices.size()];
                int i = 0;
                for (ParcelUuid filterUUID : scanServices) {
                    serviceUuids[i++] = filterUUID.getUuid();
                }
                _defaultBluetoothAdapter.startLeScan(serviceUuids, _leScanCallback);
            } else {
                ScanSettings.Builder newSettings = new ScanSettings.Builder();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    newSettings.setMatchMode(ScanSettings.MATCH_MODE_STICKY);
                    newSettings.setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT);
                }
                newSettings.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
                newSettings.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
                ArrayList<ScanFilter> filterList = new ArrayList<>();
                for (ParcelUuid filterUUID : scanServices) {
                    ScanFilter.Builder newFilter = new ScanFilter.Builder();
                    newFilter.setServiceUuid(filterUUID);
                    filterList.add(newFilter.build());
                }
                if (_defaultBluetoothAdapter.getBluetoothLeScanner() != null) {
                    _defaultBluetoothAdapter.getBluetoothLeScanner().startScan(filterList, newSettings.build(), _newLeScanCallback);
                }
            }
        }
    }

    /**
     * Stop the Bluetooth low energy scan that is in progress.
     */
    private void stopBLEScan() {
        if (!_scanning) { return; }

        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "stopBLEScan - Server stopping scan");
        }
        _scanning = false;
        _handler.removeCallbacks(_leStopScan);
        _handler.post(_leStopScan);
    }

    /**
     * Set the service scan filter that should be used for the next scan operation.  The filter only lasts
     * for one scan operation.  After the scan is performed, the filter is cleared.  If you want to use
     * the filter for the next scan it should be set again before starting the scan.
     * @param serviceFilter An array of service UUIDs that should be applied on the next scan operation
     */
    void setScanServiceFilter(String[] serviceFilter) {
        _serviceFilterList = new ArrayList<>();
        for (String serviceUUID : serviceFilter) {
            try {
                _serviceFilterList.add(ParcelUuid.fromString(serviceUUID));
            } catch(NullPointerException | IllegalArgumentException ex) {
                // skip nulls or invalid service ids
            }
        }
    }

    /**
     * Start the scan Bluetooth low energy devices.
     * @param activity The activity that started the scan operation
     * @param _lisstener The App listener that is interested in the results of the scan.
     */
    void startScanForDevices(FragmentActivity activity, BluetoothScanResultListener _lisstener) {
        _scanResultListener = _lisstener;
        startBLEScan(activity,_serviceFilterList);
        _serviceFilterList = null;
    }

    /**
     * Used to write to a Bluetooth characteristic associated with the currently connected device.
     * @param bluetoothObviousDevice The Bluetooth device that we should be working with.
     * @param rawdata The raw data bytes that are to be sent to the deivce
     * @param service The UUID of the service that we are writing.
     * @param characteristic The UUID of the characteristic that we are writing.
     * @return true if the lowlevel write was performed, false otherwise.
     */
    boolean sendDataToDevice(BluetoothObviousDevice bluetoothObviousDevice, byte[] rawdata, String service, String characteristic) {
        if (_bleDevice == null || _deviceGatt == null || !bluetoothObviousDevice.getDeviceId().equals(_bleDevice.getDeviceId())) {
            return false;
        }

        final BluetoothGattCharacteristic gc = _setupCharacteristic(rawdata, service, characteristic);
        if (gc != null) {
            gc.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            return _deviceGatt.writeCharacteristic(gc);
        }
        return false;
    }

    /**
     * Used to write to a Bluetooth characteristic associated with the currently connected device.  This method uses the without response writing method.
     * Using this method does not wait for an ACK from the Bluetooth device.
     * @param bluetoothObviousDevice The Bluetooth device that we should be working with.
     * @param rawdata The raw data bytes that are to be sent to the deivce
     * @param service The UUID of the service that we are writing.
     * @param characteristic The UUID of the characteristic that we are writing.
     * @return true if the lowlevel write was performed, false otherwise.
     */
    boolean sendDataToDeviceWithoutResponse(BluetoothObviousDevice bluetoothObviousDevice, byte[] rawdata, String service, String characteristic) {
        if (_bleDevice == null || _deviceGatt == null || !bluetoothObviousDevice.getDeviceId().equals(_bleDevice.getDeviceId())) {
            return false;
        }

        final BluetoothGattCharacteristic gc = _setupCharacteristic(rawdata, service, characteristic);
        if (gc != null) {
            gc.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            return _deviceGatt.writeCharacteristic(gc);
        }
        return false;
    }

    /**
     * Used to read a Bluetooth characteristic associated with the currently connected device.
     * @param bluetoothObviousDevice The Bluetooth device that we should be working with.
     * @param service The UUID of the service that we are reading.
     * @param characteristic The UUID of the characteristic that we are reading.
     * @return true if the lowlevel read was performed, false otherwise
     */
    boolean requestDataFromDevice(BluetoothObviousDevice bluetoothObviousDevice, String service, String characteristic) {
        if (_bleDevice == null || _deviceGatt == null || !bluetoothObviousDevice.getDeviceId().equals(_bleDevice.getDeviceId())) {
            return false;
        }

        BluetoothGattCharacteristic gc = _setupCharacteristic(new byte[0], service, characteristic);
        if (gc != null) {
            return _deviceGatt.readCharacteristic(gc);
        }
        return false;
    }

    /**
     * Creates a OS BluetoothGattCharacteristic object that can be used for reading or writing data using the Bluetooth device
     * @param rawdata The data that is being sent to the device.  If null then this characteristic is setup for a read operation.
     * @param service The UUID of the service that we are reading.
     * @param characteristic The UUID of the characteristic that we are reading.
     * @return A BluetoothGattCharacteristic representing the data or null if the correct service and characteristic does not exist
     */
    private BluetoothGattCharacteristic _setupCharacteristic(byte[] rawdata, String service, String characteristic) {
        BluetoothGattCharacteristic gc = null;

        if (rawdata != null) {
            BluetoothGattService gs = _deviceGatt.getService(UUID.fromString(service));
            if (gs != null) {
                gc = gs.getCharacteristic(UUID.fromString(characteristic));
                if (gc != null && rawdata.length > 0) {
                    gc.setValue(rawdata);
                }
            }
        }
        return gc;
    }

    /**
     * Start the connection to a Bluetooth device
     * @param context The Android Application context to use when starting the Bluetooth connection.
     * @param bluetoothObviousDevice The BluetoothObviousDevice associated with this connection.
     */
    @TargetApi(Build.VERSION_CODES.M)
    void connectToDevice(Context context, BluetoothObviousDevice bluetoothObviousDevice) {
        if (_defaultBluetoothAdapter == null) { return; }

        if (_scanning) {
            stopBLEScan();
        }

        _bleDevice = bluetoothObviousDevice;
        if (_gattCallback == null) {
            _gattCallback = new BluetoothInteractorGattCallback();
        }

        _clearServiceCache = false;
        BluetoothDevice dev = _defaultBluetoothAdapter.getRemoteDevice(bluetoothObviousDevice.getDeviceId());
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            _deviceGatt = dev.connectGatt(context, false, _gattCallback, BluetoothDevice.TRANSPORT_LE);
        } else {
            _deviceGatt = dev.connectGatt(context, false, _gattCallback);
        }
    }

    /**
     * Start the Bluetooth disconnection process.
     * @param bluetoothObviousDevice The BluetoothObviousDevice associated with this connection.
     */
    void disconnectFromDevice(BluetoothObviousDevice bluetoothObviousDevice) {
        if (_bleDevice == null || _deviceGatt == null || !_bleDevice.getDeviceId().equals(bluetoothObviousDevice.getDeviceId())) {
            return;
        }

        BluetoothObviousDevice tmpDev = _bleDevice;
        _bleDevice = null;
        if (tmpDev  != null) {
            tmpDev.deviceConnected = false;
        }

        BluetoothGatt tmpGatt = _deviceGatt;
        _deviceGatt = null;

        if (tmpGatt != null) {
            if (_clearServiceCache) {
                _refreshServiceCache(tmpGatt);
            }
            tmpGatt.disconnect();
            tmpGatt.close();
        }
    }

    /**
     * This method is used to workaround a bug in older version of Android where the device services are cached even though the Service Changed characteristic
     * is present on the device.
     * @param tmpGatt the Gatt device being used for the current connection.
     */
    private void _refreshServiceCache(BluetoothGatt tmpGatt) {
        // Android should not keep the services cached when the Service Changed characteristic is present and the device is not bonded.
        // However, there is a bug in <= Android 6.0, it is keeping them anyway and the only way to clear services is by using this hidden refresh method.
        try {
            final Method refresh = tmpGatt.getClass().getMethod("refresh");
            refresh.invoke(tmpGatt);
        } catch (Exception e) {
            Log.d(LOG_TAG, "Refreshing failed");
        }
    }

    /**
     * Start characteristic notification/indication configuration process for the active connection.
     */
    private void _startNotificationSetup() {
        _handler.post(new Runnable() {
            @Override
            public void run() {
                _setupBluetoothNotifications();
            }
        });
    }

    /**
     * Iterate through all the discovered services and characteristics and setup the CCCD for notifications/indications are required.  The discovered
     * services and characteristics are compared against the list of services configured in the BluetoothObviousDevice associated to the connection.
     */
    private void _setupBluetoothNotifications() {
        List<BluetoothGattService> serviceList = _deviceGatt.getServices();

        HashMap<String,String[]> notifyList = _bleDevice.getServerInformation();
        for (BluetoothGattService srv : serviceList) {
            String serviceUUID = srv.getUuid().toString();
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "acquireService - " + serviceUUID);
            }

            // Set the local _clearServiceCache flag so ensure that we clear the service cache if the service change characteristic is present and
            // the device is not bonded.  This is to work around an Android bug that the services are cached regardless when not bonded
            if (BluetoothServiceConstants.BLE_SERVICE_GENERIC_ATTR.equals(serviceUUID)) {
                if (srv.getCharacteristic(UUID.fromString(BluetoothServiceConstants.BLE_CHARACTERISTIC_GATT_ATTR_SERVICE_CHANGED)) != null) {
                    _clearServiceCache = (_deviceGatt.getDevice().getBondState() != BluetoothDevice.BOND_BONDED);
                    Log.d(LOG_TAG, "Set clear service cache flag to " + _clearServiceCache);
                }
            }

            // skip if this is not the service we a looking for
            if (!notifyList.containsKey(serviceUUID)) {
                if (BuildConfig.DEBUG) {
                    Log.d(LOG_TAG, "Skipping service not correct for this device " + serviceUUID);
                }
                continue;
            }

            List<String> charUUIDList = Arrays.asList(notifyList.get(serviceUUID));
            for (BluetoothGattCharacteristic tmpAttr : srv.getCharacteristics()) {
                if (BuildConfig.DEBUG) {
                    Log.d(LOG_TAG, "\t\t+++++ Check characteristic -- " + tmpAttr.getUuid() + " START");
                }
                if (charUUIDList.contains(tmpAttr.getUuid().toString())) {
                    if (BuildConfig.DEBUG) {
                        Log.d(LOG_TAG, "\t\t+++++ Requesting notifications/indication -- " + tmpAttr.getUuid());
                    }
                    boolean status = _deviceGatt.setCharacteristicNotification(tmpAttr, true);
                    if (BuildConfig.DEBUG) {
                        Log.d(LOG_TAG, "\t\t+++++ " + (status ? "OK" : "FAILED"));
                    }
                    final BluetoothGattDescriptor gd = tmpAttr.getDescriptor(UUID.fromString(BluetoothServiceConstants.BLE_CLIENT_CONFIG_DESCRIPTOR));
                    if (gd != null && gd.getUuid().toString().equals(BluetoothServiceConstants.BLE_CLIENT_CONFIG_DESCRIPTOR)) {
                        boolean cccdRequired = false;
                        if ((tmpAttr.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                            if (BuildConfig.DEBUG) {
                                Log.d(LOG_TAG, "\t\t\t+++++     send notification request");
                            }
                            gd.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            cccdRequired = true;
                        } else if ((tmpAttr.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                            if (BuildConfig.DEBUG) {
                                Log.d(LOG_TAG, "\t\t\t+++++     send indication request");
                            }
                            gd.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                            cccdRequired = true;
                        }

                        if (cccdRequired) {
                            synchronized (_descriptorLock) {
                                _descriptorStatus = false;
                                int retry = 0;
                                while(!_descriptorStatus) {
                                    if (_writeDescriptorProper(gd)) {
                                        if (BuildConfig.DEBUG) {
                                            Log.d(LOG_TAG, "\t\t\t+++++     write descriptor status = true");
                                            Log.d(LOG_TAG, "\t\t\t+++++     WAITING...");
                                        }
                                        try {
                                            _descriptorStatus = false;
                                            _descriptorLock.wait(500);
                                            if (BuildConfig.DEBUG) {
                                                Log.d(LOG_TAG, "\t\t\t+++++     WAITING..." + (_descriptorStatus ? "OK" : "FAILED"));
                                            }
                                        } catch(InterruptedException ex) {
                                            if (BuildConfig.DEBUG) {
                                                Log.d(LOG_TAG, "\t\t\t+++++     WAITING...FAILED");
                                            }
                                        }
                                    } else {
                                        if (BuildConfig.DEBUG) {
                                            Log.d(LOG_TAG, "\t\t\t+++++     write descriptor status = false");
                                        }
                                    }
                                    if (!_descriptorStatus && ++retry < 3) { break; }
                                }
                            }
                        }
                    }
                }
                if (BuildConfig.DEBUG) {
                    Log.d(LOG_TAG, "\t\t+++++ Check characteristic -- " + tmpAttr.getUuid() + " - DONE");
                }
            }
        }
        if (_bleDevice != null) {
            _bleDevice.deviceConnected = true;
            _notifyConnectionStateChange(BluetoothGatt.GATT_SUCCESS,BluetoothProfile.STATE_CONNECTED);
        }
    }

    /**
     * This method is used to workaround a bug in the Android Bluetooth descriptor setup where the descriptor's
     * descriptor write type is incorrectly inherited from the parent characteristic.  This causes descriptor write
     * problems if the parent characteristic has not response enabled.
     * @param gd The descriptor that is being written
     * @return true if the descriptor write was performed, false otherwise.
     */
    private boolean _writeDescriptorProper(final BluetoothGattDescriptor gd) {
        if (_deviceGatt == null || gd == null)
            return false;

        final BluetoothGattCharacteristic parentCharacteristic = gd.getCharacteristic();
        if (parentCharacteristic == null) {
            return false;
        }

        int tmpWriteType = parentCharacteristic.getWriteType();
        parentCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        boolean result = _deviceGatt.writeDescriptor(gd);
        parentCharacteristic.setWriteType(tmpWriteType);
        return result;
    }

    /**
     * Callback class used to handle the Bluetooth events generated by the OS Bluetooth Gatt connection.
     */
    private class BluetoothInteractorGattCallback extends BluetoothGattCallback {

        private BluetoothInteractorGattCallback() {
            super();
        }

        /**
         * Handel the connection state change events during device connection/disconnection.
         * @param gatt The Gatt device that generated the state change.
         * @param status The status of the state change.
         * @param newState The state that device was trying to reach.
         */
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if ((status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) ||
                    (status == BluetoothServiceConstants.UNKNOWN_BLE_TIMEOUT_STATUS && newState == BluetoothProfile.STATE_DISCONNECTED) ||
                    (status == BluetoothServiceConstants.UNKNOWN_BLE_TERMINATED_STATUS && newState == BluetoothProfile.STATE_DISCONNECTED) ||
                    (status == BluetoothGatt.GATT_FAILURE && newState == BluetoothProfile.STATE_CONNECTED) ||
                    (status == BluetoothServiceConstants.UNKNOWN_BLE_ERROR_STATUS)) {

                if (BuildConfig.DEBUG) {
                    Log.d(LOG_TAG,"onConnectionStateChange() -- status = " + status);
                    Log.d(LOG_TAG,"onConnectionStateChange() --  state = " + newState);
                }
                _notifyConnectionStateChange(((status == BluetoothServiceConstants.UNKNOWN_BLE_TIMEOUT_STATUS || status == BluetoothServiceConstants.UNKNOWN_BLE_TERMINATED_STATUS) ? BluetoothGatt.GATT_SUCCESS : status), newState);

                BluetoothGatt tmpGatt = _deviceGatt;
                if (tmpGatt != null) {
                    if (_clearServiceCache) {
                        _refreshServiceCache(tmpGatt);
                    }
                    tmpGatt.disconnect();
                    tmpGatt.close();
                }
                _deviceGatt = null;

                BluetoothObviousDevice tmpDev = _bleDevice;
                if (tmpDev  != null) {
                    tmpDev.deviceConnected = false;
                }
                _bleDevice = null;
                _gattCallback = null;

            } else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                _deviceGatt = gatt;

                // If possible request a low latency/high priority connection with the device so that file transfers will be faster and more reliable
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    _deviceGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                }
                // Delay the service discovery to give the device some time to setup after the connection
                _handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!_deviceGatt.discoverServices()) {
                            if (_bleDevice != null) {
                                _bleDevice.deviceConnected = false;
                            }
                            _bleDevice = null;

                            _deviceGatt.disconnect();
                            _deviceGatt.close();
                            _deviceGatt = null;

                            _notifyConnectionStateChange(BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_DISCONNECTED);
                        } else if (_bleDevice != null) {
                            _bleDevice.deviceConnected = true;
                        }
                    }
                },600);
            }
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "onConnectionStateChange() --  DONE");
            }
        }

        /**
         * Callback to notify the Gatt device that the service discovery process has completed.
         * @param gatt The Gatt device that generated the event.
         * @param status The status of the service discovery process
         */
        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // if services have been successfully discovered, setup any notifications/indications for the characteristics
                _startNotificationSetup();
            } else {
                onConnectionStateChange(gatt,BluetoothGatt.GATT_SUCCESS,BluetoothProfile.STATE_DISCONNECTED);
            }
        }

        /**
         * Callback notification for data read requests.
         * @param gatt The gatt device that generated the event.
         * @param characteristic The characteristic that was being read.
         * @param status The status of the read operation.
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _notifyDataChange(characteristic.getUuid().toString(), characteristic.getValue());
            } else {
                _notifyReadErrorStatus(characteristic.getUuid().toString(),status);
            }
        }

        /**
         * Callback notification for characteristic write requests.
         * @param gatt The gatt device that generated the event.
         * @param characteristic The characteristic that was being written.
         * @param status The status of the write operation.
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            _notifyWriteStatus(characteristic.getUuid().toString(),status);
        }

        /**
         * Callback notification for characteristic change events (i.e. notifications or indications)
         * @param gatt The gatt device that generated the event.
         * @param characteristic The characteristic that has changed.
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            _notifyDataChange(characteristic.getUuid().toString(),characteristic.getValue());
        }

        /**
         * Callback notification of the status of a descriptor write.
         * @param gatt The gatt device that generated the event.
         * @param descriptor The descriptor that was being written.
         * @param status The status of the descriptor write.
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            synchronized (_descriptorLock) {
                if (BuildConfig.DEBUG) {
                    Log.d(LOG_TAG, "onDescriptorWrite() -- status = " + status);
                    Log.d(LOG_TAG, "onDescriptorWrite() --  Signal _descriptorLock");
                }
                _descriptorStatus = true;
                _descriptorLock.notifyAll();
            }
        }
    }
}
