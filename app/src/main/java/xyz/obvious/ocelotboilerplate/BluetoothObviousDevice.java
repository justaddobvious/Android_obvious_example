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

import android.content.Context;
import android.util.Log;

import com.obvious.mobileapi.OcelotDeviceConnector;
import com.obvious.mobileapi.OcelotDeviceConnectorCallback;

import java.util.HashMap;

import xyz.obvious.manufacturer.ObviousProductIdentifier;


/**
 * This class is used to interact with the obvious Bluetooth device.  It uses an instance of the BluetoothInteractor
 * to perform the Bluetooth communications and provides an interface for the BluetoothInteractor to pass Bluetooth
 * events back.
 *
 */
class BluetoothObviousDevice implements
        OcelotDeviceConnectorCallback,
        BluetoothConnectionStateListener
{
    private OcelotDeviceConnector obviousConnector;
    private BluetoothInteractor bleConnection;
    private BluetoothConnectionStateListener stateListener = null;
    private String deviceId;
    private HashMap<String,String[]> _serviceInfo;
    private Context connectContext = null;
    boolean deviceConnected = false;

    /**
     * Create a BluetoothObviousDevice object for communicating with the underlying Bluetooth device.
     * @param clnt The BluetoothInteractor used to talk to the OS Bluetooth stack.
     * @param deviceId The Bluetooth identifier of the device this object is communicating with.
     */
    BluetoothObviousDevice(BluetoothInteractor clnt, String deviceId) {
        this.bleConnection = clnt;
        this.deviceId = deviceId;
        this._serviceInfo = new HashMap<>();
    }

    void setupObvious(OcelotDeviceConnector connector) {
        obviousConnector = connector;
        obviousConnector.setConnectorCallback(this);
        _serviceInfo.putAll(obviousConnector.getServerInformation());
    }

    /**
     * Retrieve the Bluetooth identifier associated with the instance.
     * @return the Bluetooth identifier (MAC).
     */
    String getDeviceId() {
        return deviceId;
    }

    /**
     * Retrieve the callback that will be notified when connection/disconnection events occur.
     * @return the current BluetoothConnectionStateListener handling the events.
     */
    BluetoothConnectionStateListener getConnectionListener() {
        return this;
    }

    /**
     * Initiate a Bluetooth connection with the Bluetooth device associated with this object.
     * @param context Application context to use when interacting with the OS Bluetooth layer.
     * @param listener The BluetoothConnectionStateListener that will handle the connection/disconnection events
     */
    void connect(Context context, BluetoothConnectionStateListener listener) {
        if (bleConnection == null) {
            deviceConnected = false;
            return;
        }

        stateListener = listener;
        connectContext = context;
        bleConnection.connectToDevice(context,this);
    }

    /**
     * Request that the Bluetooth connection with the device be terminated
     */
    void disconnect() {
        if (bleConnection == null) {
            deviceConnected = false;
            return;
        }
        connectContext = null;
        bleConnection.disconnectFromDevice(this);
    }

    /**
     * Return the connection status of the current device
     * @return true if connected, false otherwise
     */
    boolean isConnected() {
        return deviceConnected;
    }

    /**
     * This method is used for retrieving the Bluetooth services and characteristics that this device
     * is interested in getting notifications/indications from.
     * @return returns a HashMap of services who's value is an array of characteristics we are interested in listening to.
     */
    HashMap<String,String[]> getServerInformation() {
        return new HashMap<>(_serviceInfo);
    }

    /**
     * This is the implementation of the connection state handling interface for this device.
     * @param status The status of the connection change
     * @param newState The state that the connection has transitioned to
     */
    @Override
    public void onConnectionStateChange(int status, int newState) {
        if (stateListener != null) {
            stateListener.onConnectionStateChange(status,newState);
        }
        if (obviousConnector != null) {
            obviousConnector.onConnectionStateChange(status,newState);
        }
    }

    /**
     * Send data to the physical device connected to Obvious service with the default response options.
     * @param serviceid The UUID of service on the device that will receive the data.
     * @param characteristicid The UUID of characteristic that will be written to.
     * @param rawdata An array of bytes of data that is to be sent to the device.
     * @return true if the request was sent, false otherwise
     */
    @Override
    public boolean requestCharacteristicWrite(String serviceid, String characteristicid, byte[] rawdata) {
        if (BuildConfig.DEBUG) {
            Log.d(this.getClass().getSimpleName(), "Send data to device with response");
        }
        return bleConnection.sendDataToDevice(this, rawdata, serviceid, characteristicid);
    }

    /**
     * Send data to the physical device connected to Obvious service; does not request a verification response from
     * the connected devices that the write was received.
     * @param serviceid The UUID of service on the device that will receive the data.
     * @param characteristicid The UUID of characteristic that will be written to.
     * @param rawdata An array of bytes of data that is to be sent to the device.
     * @return true if the request was sent, false otherwise
     */
    @Override
    public boolean requestCharacteristicWriteWithoutResponse(String serviceid, String characteristicid, byte[] rawdata) {
        if (BuildConfig.DEBUG) {
            Log.d(this.getClass().getSimpleName(), "Send data to device without response");
        }
        return bleConnection.sendDataToDeviceWithoutResponse(this, rawdata, serviceid, characteristicid);
    }

    /**
     * Request data from the physical device connected to the fitness.  Data is returned in a notification.
     * @param serviceid The UUID of service on the device that will receive the data request.
     * @param characteristicid The UUID of characteristic that will be read.
     * @return true if the request was sent, false otherwise
     */
    @Override
    public boolean requestCharacteristicRead(String serviceid, String characteristicid) {
        if (BuildConfig.DEBUG) {
            Log.d(this.getClass().getSimpleName(), "Read data from device");
        }
        return bleConnection.requestDataFromDevice(this, serviceid, characteristicid);
    }

    /**
     * Request that a connection to physical device be initiated.
     */
    @Override
    public void requestDeviceConnect() {
        if (BuildConfig.DEBUG) {
            Log.d(this.getClass().getSimpleName(), "Request connect to device");
        }
        if (connectContext != null) {
            bleConnection.connectToDevice(connectContext, this);
        }
    }

    /**
     * Request that a disconnection from the physical device be initiated.
     */
    @Override
    public void requestDeviceDisconnect() {
        if (BuildConfig.DEBUG) {
            Log.d(this.getClass().getSimpleName(), "Request disconnect from device");
        }
        bleConnection.disconnectFromDevice(this);
    }

    /**
     * This method is used to get the production identifier of the physical device that the connector is
     * communicating with.
     * @return The unique product identifier.
     */
    @Override
    public String getObviousProductIdentifier() {
        return ObviousProductIdentifier.MANUFACTURER_PRODUCT_ID1;
    }

    /**
     * This method is used to handle any Bluetooth characteristic read errors
     * @param datatype The characteristic that generated the error
     * @param status the Bluetooth error code
     */
    void onDataReadErrorNotification(String datatype, int status) {
        if (BuildConfig.DEBUG) {
            Log.d(this.getClass().getSimpleName(), "Read error notification status = " + status);
        }
        if (obviousConnector != null) {
            obviousConnector.onReadStatus(datatype, status);
        }
    }

    /**
     * This method is used to handle any Bluetooth characteristic write errors
     * @param datatype The characteristic that generated the error
     * @param status the Bluetooth error code
     */
    void onDataWriteNotification(String datatype, int status) {
        if (BuildConfig.DEBUG) {
            Log.d(this.getClass().getSimpleName(), "Write notification status = " + status);
        }
        if (obviousConnector != null) {
            obviousConnector.onWriteStatus(datatype,status);
        }
    }

    /**
     * The data notification from the Bluetooth device containing the raw bytes of the characteristic
     * vlaue.
     * @param datatype The characteristic that has been notified/indicated/read.
     * @param rawdata Thee raw bytes of the value of the characteristic
     */
    void onDataNotification(String datatype, byte[] rawdata) {
        if (BuildConfig.DEBUG) {
            Log.d(this.getClass().getSimpleName(), "Data notification datatype = " + datatype);
        }
        if (obviousConnector != null) {
            obviousConnector.onUpdateDeviceData(datatype, rawdata);
        }
    }
}
