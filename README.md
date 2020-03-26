# Android\_obvious\_example
This example Android app provides a reference implementation for integrating the Obvious mobile SDK into an Android project.

## Overview
The Obvious mobile API enables apps to communicate with Bluetooth® Low Energy (BLE) devices that implement the Obvious feature management profile.  This allows the app to update the device to enable or disable optional features.  The status of optional features are configured using the Obvious cloud platform.

## Requirements
* Android Studio 3.0 or higher
* Android 4.4 and above (Android API level 19 and above)

## <a name="sectionLink-Installation"></a>Installation
The Obvious Mobile API library is distributed from a private Maven repository.  To include the API in your project, start by adding this repository to your project build.gradle file.

```groovy
repositories {
    ...
    maven {
        url "https://developer.theobvious.io/artifactory/mobileapi-release"
    }
    ...
}
```
[View source](https://github.com/justaddobvious/Android_obvious_example/blob/a23e5acd1fd847aaf2fab4e718f45f531f68540f/build.gradle#L41)

Next, add the Obvious mobile API as a dependency to your app by including the following line in the dependencies section of the app build.gradle:

```groovy
dependencies {
    ...
    implementation 'io.obvious.android:mobileapi:1.3.0'
    ...
}
```
[View source](https://github.com/justaddobvious/Android_obvious_example/blob/a23e5acd1fd847aaf2fab4e718f45f531f68540f/app/build.gradle#L55)

# Using the Obvious Mobile API
## Sample Obvious Integration
Obvious provides a reference implementation for integrating the Obvious mobile API into an Android project. This can be downloaded from our [Obvious GitHub page](https://github.com/justaddobvious/Android_obvious_example "Android Example Repository").  There are also example repositories for firmware and other mobile platform integrations.  These examples are fully functional template apps that can be copied or modified ([MIT License](https://github.com/justaddobvious/Android_obvious_example/blob/master/LICENSE)) for use as a jumping off point to create your own custom Obvious enabled mobile app.

To start just clone the repository and then import the project into Android Studio:

```
git clone https://github.com/justaddobvious/Android_obvious_example.git
```

## Integration Walkthrough
To help explain the integration implementation in the example project, the major integration points are outlined below along with a brief summary of the example code.

## Setting up the Feature Manager
The *OcelotFeatureManager* class handles the process of updating the features enabled on a device.  As the manager processes the feature update, it will pass status information to a callback that implements the *OcelotFeatureEventListener* interface.

In our example implementation the *OcelotFeatureEventListener* interface is implemented by the main display Fragment.

Source file: [ObviousFeatureFragment.java](https://github.com/justaddobvious/Android_obvious_example/blob/a23e5acd1fd847aaf2fab4e718f45f531f68540f/app/src/main/java/xyz/obvious/ocelotboilerplate/ObviousFeatureFragment.java#L72)

```java
public class ObviousFeatureFragment extends Fragment implements
        OcelotFeatureEventListener,
        ...
{
...
}
```
There are processes that the *OcelotFeatureManager* performs that require the reading and writing of temporary files, the feature manager needs to be configured with a directory location where these operation can take place.  The directory is configured using the *setFileCacheDirectory()* method of the *OcelotFeatureManager*.

The creation and configuratin of the *OcelotFeatureManager* is completed by the *_setupFeatureManager()* method of the *ObviousFeatureFragment*.

Source file: [ObviousFeatureFragment.java](https://github.com/justaddobvious/Android_obvious_example/blob/a23e5acd1fd847aaf2fab4e718f45f531f68540f/app/src/main/java/xyz/obvious/ocelotboilerplate/ObviousFeatureFragment.java#L242)

```java
    /**
     * Configure the OcelotFeatureManager to prepare for performing a feature update.
     */
    private void _setupFeatureManager() {
        // Setup the feature manager
        if (_obviousMgr == null) {

            // Create the Feature manager
            _obviousMgr = OcelotFeatureManager.getFeatureManager();
            
            // Setup the callback to get feature update status events
            _obviousMgr.setFeatureEventListener(this);
           
            // Set the directory where temporary files can be written
            if (getActivity() != null) {
                _obviousMgr.setFileCacheDirectory(getActivity().getCacheDir().getAbsolutePath());
            }

            // Tell the Feature manager the API key to use for server web API calls
            _obviousMgr.setAPIKey(ObviousProductIdentifier.MANUFACTURER_API_KEY);
        }
    }
```

There are several initialization tasked that are done by the manager during the connection to the Obvious Bluetooth device.  Therefore, it is recommended that the manager is configured prior to initiating the Bluetooth connection.

## Setting up the Firmware Manager
The *OcelotFirmwareManager* class handles the process of checking if there are any firmware updates available for the device and also performing the over the air (OTA) updating of the device firmware.  Inorder to determine if a new firmware is available, the the manager communicates with the Obvious portal.  The portal determines the newest firmware that the device can support and passes that information back to the  *OcelotFirmwareManager*.  The manager notifies the app of the firmware information through the *OcelotFirmwareAvailableListener* interface callback.  If the app determines that a firmware update is required, it can requests that the manager perform the firmware OTA update.  The status and progress events of the OTA procedure are pass from the manager to the *OcelotFirmwareEventListener* callback.

In our example implementation both the *OcelotFirmwareAvailableListener* and *OcelotFirmwareEventListener* interfaces are implemented by the main display Fragment.

Source file: [ObviousFeatureFragment.java](https://github.com/justaddobvious/Android_obvious_example/blob/a23e5acd1fd847aaf2fab4e718f45f531f68540f/app/src/main/java/xyz/obvious/ocelotboilerplate/ObviousFeatureFragment.java#L72)

```java
public class ObviousFeatureFragment extends Fragment implements
        ...
        OcelotFirmwareAvailableListener,
        OcelotFirmwareEventListener,
        ...
{
...
}
```
The creation and configuratin of the *OcelotFirmwareManager* is completed by the *_setupFirmwareManager()* method of the *ObviousFeatureFragment*:

Source file: [ObviousFeatureFragment.java](https://github.com/justaddobvious/Android_obvious_example/blob/a23e5acd1fd847aaf2fab4e718f45f531f68540f/app/src/main/java/xyz/obvious/ocelotboilerplate/ObviousFeatureFragment.java#L261)

```java
    /**
     * Configure the OcelotFirmwareManager.  This needs to be done to check for available firmware
     * and also to download and install new firmware on the Obvious enabled device.
     */
    private void _setupFirmwareManager() {
        // Setup the firmware manager
        if (_obviousFirmwareMgr == null) {
        
            // Create the Firmware manager
            _obviousFirmwareMgr = OcelotFirmwareManager.getFirmwareManager();


            // Setup the callback to get the firmware update status and progress events
            _obviousFirmwareMgr.setEventListener(this);

            // Set the directory where temporary files can be written
            if (getActivity() != null) {
                _obviousFirmwareMgr.setFileCacheDirectory(getActivity().getCacheDir().getAbsolutePath());
            }

            // Tell the Feature manager the API key to use for server web API calls
            _obviousFirmwareMgr.setAPIKey(ObviousProductIdentifier.MANUFACTURER_API_KEY);
        }
    }
```

Similar to the *OcelotFeatureManager* it is recommended that the *OcelotFirmwareManager* is configured prior starting the Bluetooth connection with the Obvious device.

## Connecting and Disconnecting with Bluetooth®
The Obvious API does not independently manage Bluetooth connections with devices.  The app developer must implement all necessary methods for interacting with the Bluetooth device.  This includes scanning, pairing, connecting, reading and writing characteristics and configuring notification or indications.  The Obvious API handles the processing of the data protocol used by the Obvious management profile.

The Bluetooth communication is taken care of by the *BluetoothInteractor* class in the sample integration project.

Source file: [BluetoothInteractor.java](https://github.com/justaddobvious/Android_obvious_example/blob/a23e5acd1fd847aaf2fab4e718f45f531f68540f/app/src/main/java/xyz/obvious/ocelotboilerplate/BluetoothInteractor.java#L63)

```java
/**
 * This class performs the communication with the OS implementation of Bluetooth protocol.
 * The current implementation only handles the communication with a single Bluetooth device and passes
 * all the events and data to the listener interfaces injected into this object.
 */
class BluetoothInteractor
{
...
    /**
     * Start the connection to a Bluetooth device
     * @param context The Android Application context to use when starting the Bluetooth connection.
     * @param bluetoothObviousDevice The BluetoothObviousDevice associated with this connection.
     */
    void connectToDevice(Context context, BluetoothObviousDevice bluetoothObviousDevice) {
        ...
    }

    /**
     * Start the Bluetooth disconnection process.
     * @param bluetoothObviousDevice The BluetoothObviousDevice associated with this connection.
     */
    void disconnectFromDevice(BluetoothObviousDevice bluetoothObviousDevice) {
        ...
    }

    /**
     * Callback class used to handle the Bluetooth events generated by the OS Bluetooth Gatt connection.
     */
    private class BluetoothInteractorGattCallback extends BluetoothGattCallback {
        /**
         * Handel the connection state change events during device connection/disconnection.
         * @param gatt The Gatt device that generated the state change.
         * @param status The status of the state change.
         * @param newState The state that device was trying to reach.
         */
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
        ...
    }
...
}
```

There are some senarios where the Obvious API requires requesting connection or disconnection from the Bluetooth device during processing of the underlying data protocol used by Obvious profile.  This is done through the *OcelotDeviceConnector* and the *OcelotDeviceConnectorCallback* that the app must implement.

In the example integration project, the app specific Bluetooth handling has been modelled in the *BluetoothObviousDevice* class.  This class implements the *OcelotDeviceConnectorCallback* interface that the *OcelotDeviceConnector* uses to make Bluetooth requests. The requests received by the *OcelotDeviceConnectorCallback* are passed on to the *BluetoothInteractor* to initiated the action. 

Source file: [BluetoothObviousDevice.java](https://github.com/justaddobvious/Android_obvious_example/blob/a23e5acd1fd847aaf2fab4e718f45f531f68540f/app/src/main/java/xyz/obvious/ocelotboilerplate/BluetoothObviousDevice.java#L43)

```java
/**
 * This class is used to interact with the obvious Bluetooth device.  It uses
 * an instance of the BluetoothInteractor to perform the Bluetooth communications
 * and provides an interface for the BluetoothInteractor to pass Bluetooth
 * events back.
 */
class BluetoothObviousDevice implements
        OcelotDeviceConnectorCallback,
        ...
{
    ...
    //
    // Implementation of the OcelotDeviceConnectorCallback interface
    //
    /**
     * Request that a connection to physical device be initiated.
     */
    @Override
    public void requestDeviceConnect() {
        // Use the BluetothInteractor to start a connection with the device
        if (connectContext != null) {
            bleConnection.connectToDevice(connectContext, this);
        }
    }

    /**
     * Request that a disconnection from the physical device be initiated.
     */
    @Override
    public void requestDeviceDisconnect() {
        // Use the BluetothInteractor to initiate a disconnection from the device
        bleConnection.disconnectFromDevice(this);
    }


}
```

The active *OcelotDeviceConnector* is pulled from the Feature or Firmware manager using the *getDeviceConnector()* method and injected into *BluetoothObviousDevice* object.

Source file: [ObviousFeatureFragment.java](https://github.com/justaddobvious/Android_obvious_example/blob/a23e5acd1fd847aaf2fab4e718f45f531f68540f/app/src/main/java/xyz/obvious/ocelotboilerplate/ObviousFeatureFragment.java#L541)

```java
    /**
     * Use the OcelotFeatureManager to perform a feature update.  This will communicate
     * with the Obvious portal to verify the enabled features and upload a new feature
     * update to the device if any changes are required.
     */
    private void _startFeatureUpdateProcess() {
        ...
        
        _setupFeatureManager();
        if (_obviousMgr != null && _bleDev != null) {

            // Inject the active connector for the Feature manager into 
            // the BluetoothObviousDevice to complete the connector setup
            _bleDev.setupObvious(_obviousMgr.getDeviceConnector());

        }
    }
```
The *BluetoothObviousDevice* class completes the connetor configuration by setting itself as the callback using the *setConnectorCallback()* method on the connector instance.

Source file: [BluetoothObviousDevice.java](https://github.com/justaddobvious/Android_obvious_example/blob/a23e5acd1fd847aaf2fab4e718f45f531f68540f/app/src/main/java/xyz/obvious/ocelotboilerplate/BluetoothObviousDevice.java#L66)

```java

    void setupObvious(OcelotDeviceConnector connector) {
        obviousConnector = connector;
        
        // Set ourself as the connector callback for handing request for this device
        obviousConnector.setConnectorCallback(this);
        ...
    }
```

## Writing Characteristics Data
Interacting with Bluetooth characteristics is also done using the *OcelotDeviceConnector*.  The connector needs to be set up with a callback implementing *OcelotDeviceConnectorCallback* that will be used to send data to the Bluetooth devices.  The Obvious Bluetooth profile uses two types of write operations, with response and without response writes.  Both write types must be implemented in order to properly communicate with the Obvious device.

Source file: [BluetoothObviousDevice.java](https://github.com/justaddobvious/Android_obvious_example/blob/a23e5acd1fd847aaf2fab4e718f45f531f68540f/app/src/main/java/xyz/obvious/ocelotboilerplate/BluetoothObviousDevice.java#L156)

```java
class BluetoothObviousDevice implements
        OcelotDeviceConnectorCallback,
        ...
{
    ...
    //
    // Implementation of the OcelotDeviceConnectorCallback interface
    //
    /**
     * Send data to the physical device connected to Obvious service with the default response options.
     * @param serviceid The UUID of service on the device that will receive the data.
     * @param characteristicid The UUID of characteristic that will be written to.
     * @param rawdata An array of bytes of data that is to be sent to the device.
     * @return true if the request was sent, false otherwise
     */
    @Override
    public boolean requestCharacteristicWrite(String serviceid,
                                              String characteristicid,
                                              byte[] rawdata)
    {
        // Use the BluetothInteractor to send the write request to the device
        return bleConnection.sendDataToDevice(this, rawdata, serviceid, characteristicid);
    }

    /**
     * Send data to the physical device connected to Obvious service; does not request a verification
     * response from the connected devices that the write was received.
     * @param serviceid The UUID of service on the device that will receive the data.
     * @param characteristicid The UUID of characteristic that will be written to.
     * @param rawdata An array of bytes of data that is to be sent to the device.
     * @return true if the request was sent, false otherwise
     */
    @Override
    public boolean requestCharacteristicWriteWithoutResponse(String serviceid,
                                                             String characteristicid,
                                                             byte[] rawdata)
    {
        // Use the BluetothInteractor to send the write request to the device
        return bleConnection.sendDataToDeviceWithoutResponse(this, rawdata, serviceid, characteristicid);
    }
    ...

    /**
     * This method is used to get the production identifier of the physical device that the connector is
     * communicating with.
     * @return The unique product identifier.
     */
    @Override
    String getObviousProductIdentifier() {
        // This returns the product identifier for the device
        // The ObviousProductIdentifier information can be downloaded from
        // the Obvious portal using the SDK generator page.
        return ObviousProductIdentifier.MANUFACTURER_PRODUCT_ID1;
    }
    ...
}
```

## Reading and Processing Characteristic Data
The connector provides methods for passing Bluetooth data received from the device to the Obvious API for processing of Obvious management profile.  The data received when the app's *BluetoothGattCallback* is called must be passed on to the Obvious library through the connector.

In the example project the *BluetoothInteractor* class receives the Android *BluetoothGattCallback* and passes the data and operation status to the *BluetoothObviousDevice* which can do any app specific or non Obvious related processing of the data.  After this the *BluetoothObviousDevice* uses the *OcelotDeviceConnector* class to relay the data and statuses to the Obvious API layer.

Source file: [BluetoothObviousDevice.java](https://github.com/justaddobvious/Android_obvious_example/blob/a23e5acd1fd847aaf2fab4e718f45f531f68540f/app/src/main/java/xyz/obvious/ocelotboilerplate/BluetoothObviousDevice.java#L186)

```java
class BluetoothObviousDevice implements
        OcelotDeviceConnectorCallback,
        ...
{
    ...
    //
    // Implementation of the OcelotDeviceConnectorCallback interface
    //
    /**
     * Request data from the physical device connected to the fitness.  Data is returned in
     * a notification.
     * @param serviceid The UUID of service on the device that will receive the data request.
     * @param characteristicid The UUID of characteristic that will be read.
     * @return true if the request was sent, false otherwise
     */
    @Override
    public boolean requestCharacteristicRead(String serviceid, String characteristicid) {
        // Use the BluetothInteractor to send the read request to the device
        return bleConnection.requestDataFromDevice(this, serviceid, characteristicid);
    }

    ...

    /**
     * This method is used to handle any Bluetooth characteristic read errors
     * @param datatype The characteristic that generated the error
     * @param status the Bluetooth error code
     */
    void onDataReadErrorNotification(String datatype, int status) {
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
        if (obviousConnector != null) {
            obviousConnector.onUpdateDeviceData(datatype, rawdata);
        }
    }
}
```

The connector methods receive the UUID of the characteristic that it was interacting with and the raw data bytes and status from the operation performed.  This data is parsed and processed by the Obvious API to support the Obvious management profile.

## Connection Status
There are situations where the Obvious API will need to perform actions when the app connects or disconnect from the Bluetooth device.  The connection and disconnection events need to be passed to the API through the connector class when the *BluetoothGattCallback* is notified of the connection state change.  The app can do any processing that it needs to bases on the connection events but must also pass the event to the Obvious API layer so that it can be properly process the the Obvious management profile.

Source file: [BluetoothObviousDevice.java](https://github.com/justaddobvious/Android_obvious_example/blob/a23e5acd1fd847aaf2fab4e718f45f531f68540f/app/src/main/java/xyz/obvious/ocelotboilerplate/BluetoothObviousDevice.java#L139)

```java
class BluetoothObviousDevice implements
        OcelotDeviceConnectorCallback,
        ...
{
    ...
    /**
     * This is the implementation of the connection state handling interface for this device.
     * @param status The status of the connection change
     * @param newState The state that the connection has transitioned to
     */
    @Override
    public void onConnectionStateChange(int status, int newState) {
        // Perform any app specific action based on the connection state change and status
        if (stateListener != null) {
            stateListener.onConnectionStateChange(status,newState);
        }

        // Pass the connection state change and status on to the Obvious API layer using
        // the connector interface
        if (obviousConnector != null) {
            obviousConnector.onConnectionStateChange(status,newState);
        }
    }
    ...
}
```

## Notifications and Indications
There are a number of characteristics in the Obvious Bluetooth profile that require notifications or indications to receive data and status information from the device.  The list of these services and characteristics are provided to the app through the *getServerInformation()* method of the connector class.  The app must enable the notification or indication for the listed Bluetooth characteristics after all services have been discovered for the device.

Our sample integration implementation, stores a HashMap of services and characteristics for the service (HashMap\<String,String[]>) in the *_serviceInfo* member field.  The *BluetoothObviousDevice* adds the Obvious services and characteristics to the HashMap.  The resulting map of services are used by the *BluetoothInteractor* class when a Bluetooth connection is establish to determine which characteristics need to have notification or indications enabled.

Source file: [BluetoothObviousDevice.java](https://github.com/justaddobvious/Android_obvious_example/blob/a23e5acd1fd847aaf2fab4e718f45f531f68540f/app/src/main/java/xyz/obvious/ocelotboilerplate/BluetoothObviousDevice.java#L66)

```java
class BluetoothObviousDevice implements
    ...
{
    private HashMap<String,String[]> _serviceInfo;

    HashMap<String,String[]> getServerInformation() {
        return new HashMap<>(_serviceInfo);
    }

    ...

    void setupObvious(OcelotDeviceConnector connector) {
        obviousConnector = connector;
        obviousConnector.setConnectorCallback(this);

        // Add the Obvious services and characteristics to the list of services handled by
        // this instance of BluetoothObviousDevice
        _serviceInfo.putAll(obviousConnector.getServerInformation());
    }
    ...
}
```

Source file: [BluetoothInteractor.java](https://github.com/justaddobvious/Android_obvious_example/blob/a23e5acd1fd847aaf2fab4e718f45f531f68540f/app/src/main/java/xyz/obvious/ocelotboilerplate/BluetoothInteractor.java#L664)

```java
class BluetoothInteractor
{
    /**
     * The BluetoothObviousDevice device object that this interactor is working with.  The data
     * and error events are passed on to this object.
     */
    private BluetoothObviousDevice _bleDevice = null;
    ...

    /**
     * Iterate through all the discovered services and characteristics and setup the CCCD for
     * notifications/indications are required.  The discovered services and characteristics are
     * compared against the list of services configured in the BluetoothObviousDevice associated
     * to the connection.
     */
    private void _setupBluetoothNotifications() {
        List<BluetoothGattService> serviceList = _deviceGatt.getServices();

        HashMap<String,String[]> notifyList = _bleDevice.getServerInformation();
        for (BluetoothGattService srv : serviceList) {
            ...
        }
        ...
    }
    ...
}
```


## Starting the Feature Update
After the Obvious API feature manager, connector and callbacks have been configured, the app is ready to start the feature check and update process.  Before starting the update the app must connect to the Bluetooth device.  The manager will handle all the details of the update process. All the app needs to do is call the *startFeatureUpdate()* of the manager after connecting.  The status and progress of the update will be returned to the app through the *OcelotFeatureEventListener* callbacks.

Source file: [ObviousFeatureFragment.java](https://github.com/justaddobvious/Android_obvious_example/blob/a23e5acd1fd847aaf2fab4e718f45f531f68540f/app/src/main/java/xyz/obvious/ocelotboilerplate/ObviousFeatureFragment.java#L541)

```java
{
public class ObviousFeatureFragment extends Fragment implements
        OcelotFeatureEventListener,
        ...
{
    ...
   /**
     * Use the OcelotFeatureManager to perform a feature update.  This will communicate with the
     * Obvious portal to verify the enabled features and upload a new feature update to the device
     * if any changes are required.
     */
    private void _startFeatureUpdateProcess() {
        ...
        _setupFeatureManager();
        if (_obviousMgr != null && _bleDev != null) {
            _bleDev.setupObvious(_obviousMgr.getDeviceConnector());
            
            // Use the OcelotFeatureManager to start the feature update process.
            _obviousMgr.startFeatureUpdate();
        }
    }

    //
    // OcelotFeatureEventListener interface implementation START
    //
    /**
     * Notification of the feature update process status.
     * @param status The status of the feature check and update.
     */
    @Override
    public void onFeatureUpdateStatus(int status) {
        ...
        if (status == OcelotFeatureEventListener.FEATUREUPDATE_SUCCESS) {
            //
            // Handle the successful update
            // This status means that the update was sent to the device without errors
            //
        } else if (status == OcelotFeatureEventListener.FEATUREUPDATE_SUCCESS_RESETTING) {
            //
            // Handle the resetting status
            // When the update is successfully sent, the device will reset so that the new configuration
            // can take effect
            //
        } else if ((status == OcelotFeatureEventListener.FEATUREUPDATE_RESET_COMPLETE)) {
            //
            // Handle the reset complete status
            // This status is sent when the API detects that the device has reset and the Bluetooth
            // connection is terminated
            //
        } else if (status == OcelotFeatureEventListener.FEATUREUPDATE_NOT_PROVISIONED) {
            //
            // Handle the provisioning error
            // This status is send when the device being updated is not provisioned in the Obvious
            // platform and in-field provisioning is not allowed
            //
        } else {
            //
            // Handle any unknown error statuses
            //
        }

        ...
    }

    /**
     * Notification of the provisioning status of the device.  This is sent for the response of
     * a provisioning process request.
     * @param status The status of the provisioning process.
     */
    @Override
    public void onProvisioningStatus(int status) {
        //
        // Handle any provisioning errors
        //
    }
    //
    // OcelotFeatureEventListener interface implementation END
    //
    ...    
}
    
```

## Verifying Feature Status
The app can ask the device for the enabled or disabled status of the supported features at any time after connecting to the Bluetooth device.  Using the *OcelotFeatureManager* class, the app can query the cloud service for a list of all the features supported by the product and then check its status with the connected device.

Source file: [ObviousFeatureFragment.java](https://github.com/justaddobvious/Android_obvious_example/blob/a23e5acd1fd847aaf2fab4e718f45f531f68540f/app/src/main/java/xyz/obvious/ocelotboilerplate/ObviousFeatureFragment.java#L499)

```java
public class ObviousFeatureFragment extends Fragment implements
        OcelotFeatureEventListener,
        ...
{
    /**
     * Use the OcelotFeatureManager to query the available features for this device.
     */
    private void _checkFeatureStatus() {
        if (_obviousMgr != null) {
            if (featureListAdapter != null)  {
                featureListAdapter.clear();
                featureListAdapter.notifyDataSetChanged();
            }

            // Query the available feature for this device
            _obviousMgr.getFeatureList();
        }
    }

}
```

The state of any supported feature can be queried one by one from the device using the manager with the *checkFeatureStatus()* method of the *OcelotFeatureManager*.  The state of the feature is returned through the *onCheckFeatureStatus()* method of the *OcelotFeatureEventListener* callback.

```java
    //
    // OcelotFeatureEventListener interface implementation START
    //
    /**
     * Notification of the activation status of the requested feature.
     * @param featureid the feaure id that was checked.
     * @param status the status of the feature.
     */
    @Override
    public void onCheckFeatureStatus(int featureid, int status) {
        // Handle status callback for the status check request
    }

    /**
     * Notification that the feature list request has been completed.
     * @param features A HashMap containing the feature name and feature id.
     *                 If feature is null, then there was an error retrieving the list.
     */
    @Override
    public void onFeatureList(HashMap<String, Integer> features) {
        // Handle the result of the feature list request.
    }
    //
    // OcelotFeatureEventListener interface implementation END
    //
   
```

## Verifying Feature Enable and Toggle Statuses
Features that have been enabled may also be toggled on and off, if it is supported on the feature. As discussed before, when the status of a feature is queried from calling the *checkFeatureStatus()* method, the *onCheckFeatureStatus()* callback is called. Instead of handling the *onCheckFeatureStatus()* callback from the *OcelotFeatureEventListener* interface, handle the *onCheckFeatureStatus()* method from the *OcelotTogglerEventListener* interface. 

The status that is returned from the *onCheckFeatureStatus()* callback will be of type *OcelotFeatureStatus*, an Obvious class that represents both the enable status and toggle status of the feature. The enable status can be returned through the *getEnableState()* getter. The toggle status can be returned through the *getToggleState()* getter. Please refer to the iOS Obvious API documentation regarding the class for more detailed information regarding each of the enable and toggle status enumerations and how to appropriately handle them. 

```java
    //
    // OcelotToggleEventListener interface implementation START
    //
    /**
     * Notification of the activation status and toggle status of the requested features.
     * @param featureStatuses The mapping of feature ids' and their corresponding statuses.
     */
     @Override
    public void onCheckFeatureStatuses(Map<Integer, OcelotFeatureStatus> featureStatuses) {
        // Handle the statuses of the checked features.
        //
        // The enable status can be returned from calling status.getEnableState() and the toggle
        // state can be returned from calling status.getToggleStatus().
        // Please refer to the iOS Obvious API documentation regarding *OcelotFeatureStatus*, 
        // *OcelotEnableStatus* and *OcelotToggleStatus* for a detailed summary of what each status 
        // type represents.
    }

    /**
     * Notification of the activation status and toggle status of the requested feature.
     * @param featureid The feaure id that was checked.
     * @param status The enable status and toggle status of the feature.
     */
     @Override
    public void onCheckFeatureStatus(int featureid, OcelotFeatureStatus status) {
        // Handle the status of the checked feature.
        //
        // The enable status can be returned from calling status.getEnableState() and the toggle
        // state can be returned from calling status.getToggleStatus().
        // Please refer to the iOS Obvious API documentation regarding *OcelotFeatureStatus*, 
        // *OcelotEnableStatus* and *OcelotToggleStatus* for a detailed summary of what each status 
        // type represents.
    }


    ...


    /**
     * Notification indicating feature status check or toggle failure. If this callback occurs,
     * the statuses of the features must be checked again by calling `checkFeatureStatus(featureid:)`
     * on all features or calling `checkFeatureStatuses(featureIds:)` to ensure the validity of the
     * feature statuses.
     */
    @Override
    public void featureStatusFailure() {
        // Handle feature status event failures here.
    }
    //
    // OcelotToggleEventListener interface implementation END
    //
   
```

## Toggling Features
Toggling a feature on or off enables or disables the functionality of the feature respectively. For example, a feature can be toggled on by either calling *toggleFeature()* on that feature when it has already been toggled off, or setting the feature's toggle status to *OcelotToggleStatus.Activated* through the *setToggleFeatureStatus()* method. The updated toggle status will be returned from the *didToggleFeatureStatus()* callback. These methods allow for your end user customers to be able to turn their features on and off depending on the situation.

Source file: [ObviousFeatureFragment.java](https://github.com/justaddobvious/Android_obvious_example/blob/a23e5acd1fd847aaf2fab4e718f45f531f68540f/app/src/main/java/xyz/obvious/ocelotboilerplate/ObviousFeatureFragment.java#L907)

```java
    //
    // OcelotToggleEventListener interface implementation START
    //

    ...


    /**
     * Notification that the toggle feature has been toggled for the
     * requested feature.
     * @param featureid The feaure id that was toggled.
     * @param status The enable status and toggle status of the feature.
     */
     @Override
    public void didToggleFeatureStatus(int featureid, OcelotFeatureStatus status) {
        // Handle the statuses of the toggled features.
        //
        // The enable status can be returned from calling status.getEnableState() and the toggle
        // state can be returned from calling status.getToggleStatus().
        // Please refer to the iOS Obvious API documentation regarding *OcelotFeatureStatus*, 
        // *OcelotEnableStatus* and *OcelotToggleStatus* for a detailed summary of what each status 
        // type represents.
    }

    /**
     * Notification that the toggle status has changed for the request features.
     * @param featureStatuses The mapping of feature ids' and their corresponding statuses.
     */
     @Override
    public void didToggleFeatureStatuses(Map<Integer, OcelotFeatureStatus> featureStatuses) {
        // Handle the status of the toggled feature.
        //
        // The enable status can be returned from calling status.getEnableState() and the toggle
        // state can be returned from calling status.getToggleStatus().
        // Please refer to the iOS Obvious API documentation regarding *OcelotFeatureStatus*, 
        // *OcelotEnableStatus* and *OcelotToggleStatus* for a detailed summary of what each status 
        // type represents.
    }

    /**
     * Notification indicating feature status check or toggle failure. If this callback occurs,
     * the statuses of the features must be checked again by calling `checkFeatureStatus(featureid:)`
     * on all features or calling `checkFeatureStatuses(featureIds:)` to ensure the validity of the
     * feature statuses.
     */
    @Override
    public void featureStatusFailure() {
        // Handle feature status event failures here.
    }
    //
    // OcelotToggleEventListener interface implementation END
    //
   
```

## Starting the Firmware Available Check
To check if there is a new firmware available for the connected Obvious device, use the *upgradeCheck(...)* method of the *OcelotFirmwareManager*.  The app passes in some version information as well as an instance of an *OcelotFirmwareAvailableListener* that will be used to return the firmware information to the app.

In our example implementation both the *OcelotFirmwareAvailableListener* and *OcelotFirmwareEventListener* interfaces are implemented by the *ObviousFeatureFragment*, so we can just pass *this* when calling the *upgradeCheck(...)* method.

Source file: [ObviousFeatureFragment.java](https://github.com/justaddobvious/Android_obvious_example/blob/a23e5acd1fd847aaf2fab4e718f45f531f68540f/app/src/main/java/xyz/obvious/ocelotboilerplate/ObviousFeatureFragment.java#L562)

```java
public class ObviousFeatureFragment extends Fragment implements
        ...
        OcelotFirmwareAvailableListener,
        OcelotFirmwareEventListener,
        ...
{
    ...
    /**
     * Use the OcelotFirmwareManager to connect to the Obvious portal to check if there are any new
     * firmwares available for the device.
     */
    private void _startFirmwareCheckProcess() {

        ...

        if (_obviousFirmwareMgr != null && _bleDev != null) {
            _bleDev.setupObvious(_obviousFirmwareMgr.getDeviceConnector());
            
            // pass in the App version and the Frangment (this) as the result callback
            _obviousFirmwareMgr.upgradeCheck(_getAppVersion(), this);
        }
    }
    ...
    
    //
    // OcelotFirmwareAvailableListener interface implementation START
    //
    /**
     * Notify the listener of the current firmware available from the server
     * @param currentVersionCode The version code of the current firmware on the device
     * @param ocelotFirmwareInfo This is a OcelotFirmwareInfo object containing the details
     *        of the firmware available
     */
    @Override
    public void onFirmwareUpgradeAvailable(long currentVersionCode,
                                           OcelotFirmwareInfo ocelotFirmwareInfo)
    {
        if (currentVersionCode != OcelotFirmwareManager.INVALID_FIRMWARE_VERSION &&
            ocelotFirmwareInfo != null) {

            ...

            // If there is a valid new firmware available then we display a notification to 
            // the user and allow them to start the update when they are ready.
            _firmwareAvail =
                Snackbar.make(rootView,R.string.obvious_firmware_message,Snackbar.LENGTH_INDEFINITE);
            _firmwareAvail.setAction(R.string.obvious_firmware_upgrade, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    _firmwareInprogress = true;
                    _startFirmwareUpdateProcess();
                }
            });
            _firmwareAvail.show();
        }

        ...
    }
    //
    // OcelotFirmwareAvailableListener interface implementation END
    //
    ...    
} 
```
The app can start the OTA firmware update process as soon as it knows that there is one available or it can present the user with a notification and allow the user to perform the update at their convenience.  The example app chooses the later option.  A snackbar notification is displayed and the user can click on the action button to initiate the OTA procedure.

## Starting the Firmware Update
A firmware OTA upgrade can be started at any time after determining that there is a new update available.  The app uses *OcelotFirmwareManager* and invokes the *startFirmwareUpgrade(...)*.  Just like the firmware check process the app passes in some version information that the manager users to download the proper firmware for the device.  The *OcelotFirmwareEventListener* would of been setup during the configruation stage of the firmware manager that was completed earlier in the process.

In our example implementation both the *OcelotFirmwareEventListener* interface is implemented by the *ObviousFeatureFragment*.  The manager passes upload progress events and completion statuses back the the listener.  This allows the app to display a progress bar to visually represent the percent complete status of the OTA process, as well as any other status or errors that may occur.

Source file: [ObviousFeatureFragment.java](https://github.com/justaddobvious/Android_obvious_example/blob/a23e5acd1fd847aaf2fab4e718f45f531f68540f/app/src/main/java/xyz/obvious/ocelotboilerplate/ObviousFeatureFragment.java#L578)

```java
{
public class ObviousFeatureFragment extends Fragment implements
        ...
        OcelotFirmwareAvailableListener,
        OcelotFirmwareEventListener,
        ...
{
    ...
    /**
     * Use the OcelotFirmwareManager to download the new firmware from the Obvious portal
     * and upload it to the device.
     */
    private void _startFirmwareUpdateProcess() {
        ...

        // inject the connector for the firmware manager before starting the update process
        _setupFirmwareManager();
        if (_obviousFirmwareMgr != null && _bleDev != null) {
            _bleDev.setupObvious(_obviousFirmwareMgr.getDeviceConnector());
            
            // Pass the version information to the manager so that it can download the
            // require firmware and star the OTA process
            _obviousFirmwareMgr.startFirmwareUpgrade(_getAppVersion());
        }
    }

    //
    // OcelotFirmwareEventListener interface implementation START
    //
    /**
     * Firmware update progress event.  This callback triggered when the firmware file transfer
     * progress should be updated.
     * @param serialnumber The serialnumber of the device that is being firmware updated.
     * @param percent The percentage complete of the firmware transfer.
     */
    @Override
    public void onFirmwareProgressUpdate(String serialnumber, int percent) {
        if (percent == OcelotFirmwareManager.FIRMWARE_PROGRESS_PREPARING) {
            //
            // The manager has requested that the device enter OTA update mode
            // a message can be displayed at this point to notify the user of this.
            //
        } else if (percent == OcelotFirmwareManager.FIRMWARE_PROGRESS_RESETTING) {
            //
            // The manager has requested that the device restart.  This status
            // can be displayed to the user
            //
        } else {
            //
            // if the percent parameter is not a special value the it represents the
            // OTA update percentage complete.  The app can update the progress bar
            // now with the new percentage complete.
            //
        }
    }

    /**
     * This callback indicates that the OcelotFirmwareManager was unable to download
     * the new firmware from the server.
     */
    @Override
    public void onFirmwareDownloadFailed() {
        //
        // The manager attempted to download the new firmware for the device from the Obvious portal
        // but an error occured during the download process.  A status message should be displayed
        // to the user.
        //
    }

    /**
     * This callback is called when the firmware update process has successfully completed.
     */
    @Override
    public void onFirmwareUpgradeSuccessful() {
        //
        // The OTA upgrade process has completed successfully and the device has been rebooted
        // so that the new firmware can take affect.
        //
    }

    /**
     * This callback indicates that the Firmware update transfer to the device failed.
     */
    @Override
    public void onFirmwareUpgradeFailed() {
        //
        // The manager an error occured during the OTA upgrade process.
        // A status message should be displayed to the user advising
        // of the error.  The manager restarts the device at this point
        // so that normal operation can resume.
        //
    }
    //
    // OcelotFeatureEventListener interface implementation END
    //
    ...    
}
    
```

On successful completion of the OTA process, the Obvious API request that the connected device reboot itself so that the newly updated firmware can be validated and start running.  This mean that after the update, the connection with the device will be lost.  The app must reestablish the Bluetooth connection inorder to continue using the device.

## Purchasing Features
The *OcelotCatalogInteractor* class handles processes related to purchasing features. This includes providing a catalog of purchasable features, and checking out selected catalogs of features for purchasing. The developer API key must also be set so that HTTP requests can be performed correctly.

In order for your end user customers to be able to purhcase features, first, present the catalog from which features can be pruchased from. The default catalog can be obtained from calling the *getDefaultCatalog()* method from the *OcelotCatalogInteractor* class. The resulting catalog will be returned as a list of *CatalogItem* from the *onCatalogListSuccess()* callback, which is implemented by the *OcelotCatalogListResultListener* interface.

The *CatalogItem* class is a Obvious class type that represents a purchasable bundle of feature or features that can be purchased by the end user.

Source file: [ObviousStoreFragment.java](https://github.com/justaddobvious/Android_obvious_example/blob/a23e5acd1fd847aaf2fab4e718f45f531f68540f/app/src/main/java/xyz/obvious/ocelotboilerplate/ObviousStoreFragment.java#L315)

```java
public class ObviousStoreFragment extends Fragment implements
        OcelotCatalogCheckoutResultListener,
        OcelotCatalogListResultListener,
        ...
{

    ...

    /**
     * Use an OcelotCatalogInteractor to load the default catalog for the current manufacturer.  The interactor
     * must be configured with the proper callback so that the app will receive the catalog events.
     */
    private void _storeClicked() {
        
        ...

        // setup the OcelotCatalogInteractor with the proper callbacks so that the app can process the catalog and check out events
        if (catalogInteractor == null) {
            catalogInteractor = OcelotCatalogInteractor.getCatalogInteractor();
            catalogInteractor.setAPIKey(ObviousProductIdentifier.MANUFACTURER_API_KEY);
            catalogInteractor.setCatalogListener(this);
            catalogInteractor.setCheckoutListener(this);
        }
        catalogInteractor.getDefaultCatalog();

        ...

    }

    //
    // implementation of the OcelotCatalogListResultListener - START
    //
    /**
     * This callback is triggered when the server successfully retrieves the catalog items for the manufacturer
     * @param catalogItems An ArrayList containing the items available for purchase.
     */
    @Override
    public void onCatalogListSuccess(final ArrayList<CatalogItem> catalogItems) {

        ...

        if (defaultCatalogAdapter == null) {
            return;
        }

        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    defaultCatalogAdapter.clear();
                    defaultCatalogAdapter.addAll(catalogItems);
                    defaultCatalogAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    ...

    /**
     * This event indicates that the server was unable to load the default catalog for the manufacturer.
     */
    @Override
    public void onCatalogListFail() {
        // Handle the event of a catalog list request failure.
    }
    //
    // implementation of the OcelotCatalogListResultListener - END
    //

    ...

}
```

If an end user requests to purchase a selected list of *CatalogItem*. The purhcase checkout process is initiated by the *checkoutCart()* method from the *OcelotCatalogInteractor* class. The checkout result will be finished processing once either the *onCheckoutCartSuccess()* method or *onCheckoutCartFail()* method is called back, which indicate a checkout success or failure respectively. These two callbacks are implemented from the *OcelotCatalogCheckoutResultListener* interface.

The end user can confirm their purchase by starting the payment process through the *startPaymentProcessing()* method from the *OcelotPaymentClient* class. The *OcelotPaymentClient* class handles the payment process of purhcasing catalog items. The payment will be finished processing once either the *onCheckoutPaySuccess()* method or *onCheckoutPayFail()* method is called back, which indicate payment processing success or failure respectively. These two callbacks are implemented from the *OcelotCatalogCheckoutResultListener* interface.

Sometimes a payment may require additional authentication. If this is the case, the method *onCheckoutPayActionRequired()* is called back instead. The *OcelotPaymentClient* instance must authenticate the payment by calling *authenticatePayment()* method, passing the payment secret into it. After calling the method to authenticate the payment, the *onCheckoutPaySuccess()* method or *onCheckoutPayFail()* method is called back.

Source file: [ObviousStoreFragment.java](https://github.com/justaddobvious/Android_obvious_example/blob/a23e5acd1fd847aaf2fab4e718f45f531f68540f/app/src/main/java/xyz/obvious/ocelotboilerplate/ObviousStoreFragment.java#L137)

```java
public class ObviousStoreFragment extends Fragment implements
        OcelotCatalogCheckoutResultListener,
        OcelotCatalogListResultListener,
        ...
{

    private int _checkoutCartid = -1;
    ...

    private void _checkoutClicked(String serialNumber, String productIdentifier) {
        
        ...

        if (catalogInteractor != null) {
            ArrayList<CatalogItem> cartItems = new ArrayList<>();
            for (int idx = 0; idx < _cartContent.size(); idx++) {
                if (_cartContent.valueAt(idx)) {
                    cartItems.add(defaultCatalogAdapter.getItem(_cartContent.keyAt(idx)));
                }
            }
            catalogInteractor.checkoutCart(cartItems, serialNumber, productIdentifier);
        }

        ...

    }

    private void _paymentClicked(String serialNumber, String productIdentifier) {
        
        ...

        paymentClient = OcelotPaymentClient.getPaymentClient(getActivity().getApplicationContext(), catalogInteractor, this);
        OcelotCheckoutWidget ccInput = flipper.findViewById(R.id.cartcard);
        if (!paymentClient.validatePaymentData(ccInput)) {
            Toast.makeText(getContext(), R.string.obvious_payment_invalidcard, Toast.LENGTH_SHORT).show();
            return;
        }

        ...

        if (catalogInteractor != null && _checkoutCartid != -1) {
            paymentClient.startPaymentProcessing(ccInput, _checkoutCartid, serialNumber, productIdentifier);
            ccInput.clear();
        } else {
            _clearProgressDialog();
            Toast.makeText(getContext(), R.string.obvious_payment_invalidcard, Toast.LENGTH_SHORT).show();
        }

        ...

    }

    ...

    @Override
    public void onCheckoutCartSuccess(final float totalCost, final int cartId) {
        _checkoutCartid = cartid;
        // Handle checkout cart success event here.
        //
        // The callback returns the total price in cents, as an integer, as well as the cartId, 
        // a unique identifier that represents the current successful checked out features.
        //
        // The cartId will used as a parameter when starting the payment process for when the 
        // end user chooses to confirm the catalog items that they have selected to purchase.
    }

    @Override
    public void onCheckoutCartFail() {
        // Handle checkout cart failure event here.
    }

    @Override
    public void onCheckoutPaySuccess() {
        // Handle payment processing success event here.
    }

    @Override
    public void onCheckoutPayActionRequired(String paymentSecret) {
        // If a payment requires additional authentication, pass the payment secret from this 
        // callback to the payment client method *authenticatePayment(resultFragment:paymentInfo:)*.
        paymentClient.authenticatePayment(this, paymentSecret);
    }

    @Override
    public void onCheckoutPayFail() {
        // Handle payment processing failure event here.
    }

    ...

}

```
