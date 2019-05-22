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

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;

class BluetoothServiceConstants {

    public static final String BLE_SERVICE_GENERIC_ACCESS = "00001800-0000-1000-8000-00805f9b34fb";
    public static final String BLE_SERVICE_GENERIC_ATTR   = "00001801-0000-1000-8000-00805f9b34fb";
    public static final String BLE_SERVICE_DEVICE_INFO    = "0000180a-0000-1000-8000-00805f9b34fb";
    public static final String BLE_SERVICE_BATTERY        = "0000180f-0000-1000-8000-00805f9b34fb";

    /**
     * Bluetooth GATT service id for Running Speed/Cadence
     */
    public static final String BLE_SERVICE_RUNSC          = "00001814-0000-1000-8000-00805f9b34fb";
    /**
     * Bluetooth GATT service id for Cycling Speed/Cadence
     */
    public static final String BLE_SERVICE_CYCLESC        = "00001816-0000-1000-8000-00805f9b34fb";
    /**
     * Bluetooth GATT service id for Cycling Power
     */
    public static final String BLE_SERVICE_CYCLEPOWER     = "00001818-0000-1000-8000-00805f9b34fb";
    /**
     * Bluetooth GATT service id for Location and Navigation
     */
    public static final String BLE_SERVICE_LOCATION       = "00001819-0000-1000-8000-00805f9b34fb";
    /**
     * Bluetooth GATT service id for Heart rate
     */
    public static final String BLE_SERVICE_HEARTRATE      = "0000180d-0000-1000-8000-00805f9b34fb";

    public static final String BLE_SERVICE_4IIII_V2       = "5b774111-d526-7b9a-4ae7-e59d015d79ed";
    public static final String BLE_SERVICE_4IIII_V1       = "00004111-0000-1000-8000-00805f9b34fb";

    /**
     * Bluetooth Characteristic configuration descriptor
     */
    public static final String BLE_CLIENT_CONFIG_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb";

    /**
     * Bluetooth GAP device name characteristic
     */
    public static final String BLE_CHARACTERISTIC_GAP_DEVICE_NAME       = "00002a00-0000-1000-8000-00805f9b34fb";
    /**
     * Bluetooth GAP appearance characteristic
     */
    public static final String BLE_CHARACTERISTIC_GAP_APPEARANCE        = "00002a01-0000-1000-8000-00805f9b34fb";
    /**
     * Bluetooth GAP privacy flag characteristic
     */
    public static final String BLE_CHARACTERISTIC_GAP_PRIVACY_FLAG      = "00002a02-0000-1000-8000-00805f9b34fb";
    /**
     * Bluetooth GAP Reconnection address characteristic
     */
    public static final String BLE_CHARACTERISTIC_GAP_RECONNECTION_ADDR = "00002a03-0000-1000-8000-00805f9b34fb";
    /**
     * Bluetooth GAP privacy flag characteristic
     */
    public static final String BLE_CHARACTERISTIC_GAP_CONNECTION_PARAMS = "00002a02-0000-1000-8000-00805f9b34fb";

    /**
     * Bluetooth GATT generic attribute service changed characteristic
     */
    public static final String BLE_CHARACTERISTIC_GATT_ATTR_SERVICE_CHANGED = "00002a05-0000-1000-8000-00805f9b34fb";

    /**
     * Bluetooth GATT device info manufacturer string characteristic
     */
    public static final String BLE_CHARACTERISTIC_DEVICE_INFO_MANUFACTURER = "00002a29-0000-1000-8000-00805f9b34fb";
    /**
     * Bluetooth GATT device info model string characteristic
     */
    public static final String BLE_CHARACTERISTIC_DEVICE_INFO_MODEL        = "00002a24-0000-1000-8000-00805f9b34fb";
    /**
     * Bluetooth GATT device info serial number string characteristic
     */
    public static final String BLE_CHARACTERISTIC_DEVICE_INFO_SN           = "00002a25-0000-1000-8000-00805f9b34fb";
    /**
     * Bluetooth GATT device info firmware revision string characteristic
     */
    public static final String BLE_CHARACTERISTIC_DEVICE_INFO_FWREV        = "00002a26-0000-1000-8000-00805f9b34fb";
    /**
     * Bluetooth GATT device info hardware revision string characteristic
     */
    public static final String BLE_CHARACTERISTIC_DEVICE_INFO_HWREV        = "00002a27-0000-1000-8000-00805f9b34fb";
    /**
     * Bluetooth GATT device info software revision string characteristic
     */
    public static final String BLE_CHARACTERISTIC_DEVICE_INFO_SWREV        = "00002a28-0000-1000-8000-00805f9b34fb";

    /**
     * Bluetooth GATT Battery Service battery level
     */
    public static final String BLE_CHARACTERISTIC_BATTERY_LEVEL = "00002a19-0000-1000-8000-00805f9b34fb";

    /**
     * Bluetooth GATT Running Speed/Cadence data characteristic
     */
    public static final String BLE_CHARACTERISTIC_RUNSC_DATA      = "00002a53-0000-1000-8000-00805f9b34fb";
    /**
     * Bluetooth GATT Cycling Speed/Cadence data characteristic
     */
    public static final String BLE_CHARACTERISTIC_CYCLESC_DATA    = "00002a5b-0000-1000-8000-00805f9b34fb";
    /**
     * Bluetooth GATT Cycling Power data characteristic
     */
    public static final String BLE_CHARACTERISTIC_CYCLEPOWER_DATA = "00002a63-0000-1000-8000-00805f9b34fb";
    /**
     * Bluetooth GATT Cycling Power location characteristic
     */
    public static final String BLE_CHARACTERISTIC_CYCLEPOWER_LOCATION = "00002a5d-0000-1000-8000-00805f9b34fb";
    /**
     * Bluetooth GATT Cycling Power control point characteristic
     */
    public static final String BLE_CHARACTERISTIC_CYCLEPOWER_FEATURE = "00002a65-0000-1000-8000-00805f9b34fb";
    /**
     * Bluetooth GATT Cycling Power control point characteristic
     */
    public static final String BLE_CHARACTERISTIC_CYCLEPOWER_CONTROLPOINT = "00002a66-0000-1000-8000-00805f9b34fb";
    /**
     * Bluetooth GATT Location and Navigation characteristic
     */
    public static final String BLE_CHARACTERISTIC_LOCATION_DATA   = "00002a67-0000-1000-8000-00805f9b34fb";

    /**
     * Bluetooth GATT Heart rate data characteristic
     */
    public static final String BLE_CHARACTERISTIC_HEARTRATE_DATA  = "00002a37-0000-1000-8000-00805f9b34fb";

    public static final String BLE_CHARACTERISTIC_4IIII_V1_DATA   = "00004321-0000-1000-8000-00805f9b34fb";
    public static final String BLE_CHARACTERISTIC_4IIII_V2_DATA   = "5b774321-d526-7b9a-4ae7-e59d015d79ed";
    public static final int BLE_SERVICE_ID_LENGTH  = 8;

    /**
     * Connection state constant for service connected
     */
    public static final int CONNECTION_STATE_CONNECTED = BluetoothProfile.STATE_CONNECTED;
    /**
     * Connection state constant for service disconnected
     */
    public static final int CONNECTION_STATE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;
    /**
     * Connection state status constant for state transition successful
     */
    public static final int CONNECTION_STATUS_SUCCESS = BluetoothGatt.GATT_SUCCESS;
    /**
     * Connection state status constant for state transition successful
     */
    public static final int CONNECTION_STATUS_FAILURE = BluetoothGatt.GATT_FAILURE;
    /**
     * Connection state status constant for state transition successful
     */
    public static final int CONNECTION_STATUS_SERVICE_FAILURE = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;

    public static final int UNKNOWN_BLE_ERROR_STATUS = 133;
    public static final int UNKNOWN_BLE_TIMEOUT_STATUS = 8;
    public static final int UNKNOWN_BLE_TERMINATED_STATUS = 22;
}
