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

import java.util.ArrayList;

/**
 * This interface is implemented by the object interested in getting the scan results from the
 * Bluetooth low energy scanner
 */
interface BluetoothScanResultListener {
    /**
     * Class representing the data associated with a scanned device.
     */
    class DeviceRecord {
        private String name;
        private String mac;
        DeviceRecord(String name, String mac) {
            this.name = name;
            this.mac = mac;
        }

        /**
         * Get the name of the device scan record.
         * @return The advertised name of the device.
         */
        String getName() {
            return name;
        }
        /**
         * Get the Bluetooth MAC of the device scanned
         * @return Bluetooth MAC address
         */
        String getAddress() {
            return mac;
        }
    }

    /**
     * Callback notification with the list of devices found during the scan process
     * @param results The array of devices found.
     */
    void onScanResults(ArrayList<DeviceRecord> results);
}
