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

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * This class is used as a retained fragment for storing the application state that needs to
 * survive a configuration chage.
 */
public class ObviousAppStateFragment extends Fragment {

    /**
     * The current connected device serialnumber
     */
    private String deviceSerial = null;
    /**
     * A flag indicating that the feature update process should be started when the feature fragment started
     */
    private boolean forceFeatureUpdate = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    /**
     * Get the current connected device serialnumber
     * @return the serialnumber
     */
    String getDeviceSerialnumber() {
        return deviceSerial;
    }

    /**
     * Set the curren connecged device serialnumber
     * @param deviceSerial the new serialnumber
     */
    void setDeviceSerialnumber(String deviceSerial) {
        this.deviceSerial = deviceSerial;
    }

    /**
     * The status of the force feature update flag
     * @return the force update state
     */
    boolean isForceFeatureUpdate() {
        return forceFeatureUpdate;
    }

    /**
     * Set the the force feature update flag
     * @param forceFeatureUpdate the new force feature update flag state
     */
    void setForceFeatureUpdate(boolean forceFeatureUpdate) {
        this.forceFeatureUpdate = forceFeatureUpdate;
    }
}
