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

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.google.android.material.snackbar.Snackbar;
import com.obvious.mobileapi.OcelotDeviceInfo;
import com.obvious.mobileapi.OcelotDeviceInfoListener;
import com.obvious.mobileapi.OcelotFeatureEventListener;
import com.obvious.mobileapi.OcelotFeatureManager;
import com.obvious.mobileapi.OcelotFirmwareAvailableListener;
import com.obvious.mobileapi.OcelotFirmwareEventListener;
import com.obvious.mobileapi.OcelotFirmwareManager;
import com.obvious.mobileapi.OcelotToggleEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import xyz.obvious.manufacturer.ObviousProductIdentifier;

public class ObviousFeatureFragment extends Fragment implements
        OcelotFeatureEventListener,
        OcelotFirmwareAvailableListener,
        OcelotFirmwareEventListener,
        ObviousBoilerplateActivity.OnFragmentBackpressListener,
        EasyPermissions.PermissionCallbacks,
        FeatureListAdapter.FeatureListOnClickListener,
        OcelotToggleEventListener,
        OcelotDeviceInfoListener
{

    private String LOG_TAG = ObviousFeatureFragment.class.getSimpleName();

    static final String FEATURE_NAME = "name";
    static final String FEATURE_STATUS = "state";
    static final String FEATURE_ACTIVE = "active";
    static final String FEATURE_ID = "fid";

    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
    };

    private final int REQUIRED_PERMISSIONS_REQUEST_CODE = 1234;

    private final static int SCAN_PAGE = 0;
    private final static int FEATURE_PAGE = 1;

    private View rootView = null;

    private OcelotFeatureManager _obviousMgr = null;
    private OcelotFirmwareManager _obviousFirmwareMgr = null;
    private Iterator<Map.Entry<String,Integer>> featureKeys = null;
    private Map.Entry<String,Integer> featureEntry = null;
    private boolean _pendingReset = false;
    private boolean _pendingToggle = false;

    private BluetoothInteractor _serviceClient = null;

    private ProgressDialog _progressDlg = null;
    private HashMap<String, Integer> _obviousFeatures = null;

    private String _prodId = null;
    private String _fwver = null;;
    private String _fwbootver = null;;
    private String _fwsoftver = null;;

    private ArrayAdapter<String> scanListAdapter = null;
    private FeatureListAdapter featureListAdapter = null;
    private ArrayList<String> deviceMap = null;
    private String selectedName = null;
    private String selectedMAC = null;
    private BluetoothObviousDevice _bleDev;

    private String _devSN = null;

    private Snackbar _firmwareAvail = null;
    private boolean _firmwareInprogress = false;
    private boolean _firmwareManualCheck = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View tmpView = inflater.inflate(R.layout.activity_obvious_home, container, false);
        if (tmpView == null) {
            return null;
        }

        Button tmpBtn = tmpView.findViewById(R.id.scanbutton);
        if (tmpBtn != null) {
            tmpBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    _scanForDevice();
                }
            });
        }

        ListView tmpList = tmpView.findViewById(R.id.scanlist);
        if (tmpList != null) {
            if (scanListAdapter == null && getContext() != null) {
                scanListAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1,new ArrayList<String>());
            }
            tmpList.setAdapter(scanListAdapter);
            tmpList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    selectedName = scanListAdapter.getItem(position);
                    if (selectedName != null) {
                        selectedName = selectedName.split("\t")[0];
                    }
                    selectedMAC = deviceMap.get(position);
                    _updateDataDisplay(selectedName);
                    _connectClicked();
                }
            });
        }
        tmpList = tmpView.findViewById(R.id.featurelist);
        if (tmpList != null) {
            if (featureListAdapter == null && getContext() != null) {
                featureListAdapter = new FeatureListAdapter(getContext(), R.layout.activity_obvious_feature_item, this);
            }
            tmpList.setAdapter(featureListAdapter);
        }

        ImageButton tmpImgBtn = tmpView.findViewById(R.id.featuredevicefirmwarecheck);
        if (tmpImgBtn != null) {
            tmpImgBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    _firmwareManualCheck = true;
                    _startFirmwareCheckProcess();
                }
            });
        }

        rootView = tmpView;
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getActivity() != null && _serviceClient == null) {
            _serviceClient = BluetoothInteractor.getServiceClient(getActivity().getApplication());

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (_bleDev != null && _bleDev.isConnected()) {
            ViewFlipper tmpFlipper = rootView.findViewById(R.id.main_flipper);
            if (tmpFlipper != null) {
                tmpFlipper.setDisplayedChild(FEATURE_PAGE);
                _updateDataDisplay(selectedName);
            }

            if (getFragmentManager() != null) {
                Fragment state = getFragmentManager().findFragmentByTag(ObviousBoilerplateActivity.STATE_TAG);
                if (state instanceof ObviousAppStateFragment) {
                    if (((ObviousAppStateFragment) state).isForceFeatureUpdate()) {
                        ((ObviousAppStateFragment) state).setForceFeatureUpdate(false);
                        _startFeatureUpdateProcess();
                    }
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (_bleDev != null && _bleDev.isConnected()) {
            _bleDev.disconnect();
            _bleDev = null;
        }
    }

    private void _clearProgressDialog() {
        if (_progressDlg != null) {
            _progressDlg.dismiss();
            _progressDlg = null;
        }
    }

    /**
     * Configure the OcelotFeatureManager to prepare for performing a feature update.
     */
    private void _setupFeatureManager() {
        // Setup the feature manager
        if (_obviousMgr == null) {
            _obviousMgr = OcelotFeatureManager.getFeatureManager();
            _obviousMgr.setFeatureEventListener(this);
            if (getActivity() != null && getActivity().getApplicationContext() != null) {
                _obviousMgr.setToggleConfigs(this,getActivity().getApplicationContext());
            }
            if (getActivity() != null) {
                _obviousMgr.setFileCacheDirectory(getActivity().getCacheDir().getAbsolutePath());
            }
            _obviousMgr.setAPIKey(ObviousProductIdentifier.MANUFACTURER_API_KEY);
        }
    }

    /**
     * Configure the OcelotFirmwareManager.  This needs to be done to check for available firmware
     * and also to download and install new firmware on the Obvious enabled device.
     */
    private void _setupFirmwareManager() {
        // Setup the firmware manager
        if (_obviousFirmwareMgr == null) {
            _obviousFirmwareMgr = OcelotFirmwareManager.getFirmwareManager();
            _obviousFirmwareMgr.setEventListener(this);
            if (getActivity() != null) {
                _obviousFirmwareMgr.setFileCacheDirectory(getActivity().getCacheDir().getAbsolutePath());
            }
            _obviousFirmwareMgr.setAPIKey(ObviousProductIdentifier.MANUFACTURER_API_KEY);
        }
    }

    /**
     * Start the Bluetooth low energy scan procedure.
     */
    private void _scanForDevice() {
        if (getContext() != null && !EasyPermissions.hasPermissions(getContext(), REQUIRED_PERMISSIONS)) {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, getString(R.string.permission_rationale), REQUIRED_PERMISSIONS_REQUEST_CODE, REQUIRED_PERMISSIONS);
            return;
        }

        if (_bleDev != null) {
            if (_bleDev.isConnected()) {
                _bleDev.disconnect();
            }
            _bleDev = null;
            _pendingReset = false;
            _updateDataDisplay(null);
        }

        deviceMap = new ArrayList<>();
        selectedMAC = null;

        scanListAdapter.clear();
        scanListAdapter.notifyDataSetChanged();

        // TODO: Add the appropriate service filter for your device.
        _serviceClient.setScanServiceFilter(new String[] {"EF680100-9B35-4933-9B10-52FFA9740042"});
        _serviceClient.startScanForDevices(getActivity(), new BluetoothScanResultListener() {
            @Override
            public void onScanResults(ArrayList<DeviceRecord> results) {
                boolean hasChange = false;
                for (DeviceRecord item : results) {
                    if (!deviceMap.contains(item.getAddress())) {
                        scanListAdapter.add(item.getName() + "\t(" + item.getAddress() + ")");
                        deviceMap.add(item.getAddress());
                        hasChange = true;
                    }
                }
                if (hasChange) {
                    scanListAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    /**
     * Used the Bluetooth interactor to connect to the Obvious enabled Bluetooth device
     */
    private void _connectClicked() {
        if (selectedMAC == null) { return; }

        if (_bleDev != null && _bleDev.isConnected()) {
            _bleDev.disconnect();
            _bleDev = null;

            _updateDataDisplay(null);
            return;
        }

        _pendingReset = false;

        if (_progressDlg != null) {
            _progressDlg.dismiss();
            _progressDlg = null;
        }
        _progressDlg = ProgressDialog.show(getContext(),null, getString(R.string.obvious_device_connecting),true,false);
        _progressDlg.show();

        _devSN = null;
        _bleDev = new BluetoothObviousDevice(_serviceClient,selectedMAC);

        // When connection to a new device, we want to initialize new managers
        _obviousMgr = null;
        _obviousFirmwareMgr = null;

        // The first thing we want to do is check if there is any new firmware for the Obvious device.
        // We setup the firmware manager at this point and configure the connector so that the manager can
        // communicate with the Obvious profile.
        _setupFirmwareManager();
        if (_obviousFirmwareMgr != null) {
            _bleDev.setupObvious(_obviousFirmwareMgr.getDeviceConnector());
        }

        if (getActivity() != null) {
            // setup the connection state listener for the Bluetooth connection.  This allows the App to know when connection/disconnection events happen so we can take the appropriate action.
            _bleDev.connect(getActivity().getApplicationContext(), new BluetoothConnectionStateListener() {
                @Override
                public void onConnectionStateChange(int status, int newState) {
                    if (BuildConfig.DEBUG) {
                        Log.d(LOG_TAG, String.format("status = %d, state = %d", status, newState));
                    }
                    if ((status == BluetoothServiceConstants.CONNECTION_STATUS_SUCCESS && newState == BluetoothServiceConstants.CONNECTION_STATE_DISCONNECTED) ||
                            (status != BluetoothServiceConstants.CONNECTION_STATUS_SUCCESS && newState == BluetoothServiceConstants.CONNECTION_STATE_CONNECTED) ||
                            (status == BluetoothServiceConstants.UNKNOWN_BLE_ERROR_STATUS) || (status == BluetoothServiceConstants.UNKNOWN_BLE_TIMEOUT_STATUS)) {
                        if (BuildConfig.DEBUG) {
                            Log.d(LOG_TAG, "\t---- Disconnected");
                        }
                        // The firmware update process may restart the device so we will ignore this event if there is a firmware update in progress
                        if (_firmwareInprogress || _pendingToggle) {
                            return;
                        }
                        if (_bleDev != null) {
                            _bleDev.disconnect();
                        }
                        _bleDev = null;
                        _updateDataDisplay((_pendingReset || _pendingToggle) ? selectedName : null);
                    }
                    if (status == BluetoothServiceConstants.CONNECTION_STATUS_SUCCESS && newState == BluetoothServiceConstants.CONNECTION_STATE_CONNECTED) {
                        if (BuildConfig.DEBUG) {
                            Log.d(LOG_TAG, "\t++++ Connected");
                        }
                        // The firmware update process may restart the device so we will ignore this event if there is a firmware update in progress
                        if (_firmwareInprogress || _pendingToggle) {
                            return;
                        }
                        _updateDataDisplay(selectedName);

                        // After the device is connected start the firmware check process
                        _firmwareManualCheck = false;
                        _startFirmwareCheckProcess();
                    }
                }
            });
        }
    }

    private void _updateDataDisplay(String devName) {
        if (_progressDlg != null && !_pendingToggle) {
            _progressDlg.dismiss();
            _progressDlg = null;
        }

        final ViewFlipper flipper = rootView.findViewById(R.id.main_flipper);
        if (flipper != null) {
            int newPage = (devName == null ? SCAN_PAGE : FEATURE_PAGE);
            int curPage = flipper.getDisplayedChild();
            if (curPage  != newPage) {
                if (newPage > curPage) {
                    flipper.setInAnimation(getContext(), R.anim.transition_in_left);
                    flipper.setOutAnimation(getContext(), R.anim.transition_out_left);
                } else {
                    flipper.setInAnimation(getContext(), R.anim.transition_in_right);
                    flipper.setOutAnimation(getContext(), R.anim.transition_out_right);
                }
                flipper.setDisplayedChild(newPage);
            }
        }

        TextView tmpText = rootView.findViewById(R.id.featuredevicename);
        if (tmpText != null) {
            if (devName != null && !"".equals(devName)) {
                tmpText.setText(devName);
            } else {
                tmpText.setText(R.string.obvious_device_blank);
            }
        }

        tmpText = rootView.findViewById(R.id.featuredeviceserial);
        if (tmpText != null) {
            if (devName != null && _devSN != null && !"".equals(_devSN)) {
                tmpText.setText(_devSN);
            } else {
                tmpText.setText(R.string.obvious_device_blank);
            }
        }
        tmpText = (TextView)rootView.findViewById(R.id.featuredevicefirmware);
        if (tmpText != null) {
            if (devName != null && _fwver != null && !"".equals(_fwver)) {
                tmpText.setText(_fwver);
            } else {
                tmpText.setText(R.string.obvious_device_blank);
            }
        }
        tmpText = (TextView)rootView.findViewById(R.id.featuredeviceboot);
        if (tmpText != null) {
            if (devName != null && _fwbootver != null && !"".equals(_fwbootver)) {
                tmpText.setText(_fwbootver);
            } else {
                tmpText.setText(R.string.obvious_device_blank);
            }
        }
        tmpText = (TextView)rootView.findViewById(R.id.featuredevicesoft);
        if (tmpText != null) {
            if (devName != null && _fwsoftver != null && !"".equals(_fwsoftver)) {
                tmpText.setText(_fwsoftver);
            } else {
                tmpText.setText(R.string.obvious_device_blank);
            }
        }

        if (_bleDev != null && !_bleDev.isConnected()) {
            if (featureListAdapter != null) {
                featureListAdapter.clear();
                featureListAdapter.notifyDataSetChanged();
            }
            if (!_pendingReset) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                alert.setTitle(R.string.obvious_feature_status_title)
                        .setMessage(R.string.obvious_device_lostconnect)
                        .setCancelable(true);
                alert.show();
            }
        }
    }

    /**
     * Process the status of the feature check.
     * @param featureid The feature id of the feature being checked
     * @param featureName The display name of the feature
     * @param status The enabled (1) or disabled (0) status of the feature
     */
    private void _updateFeatureDisplay(int featureid, String featureName, OcelotFeatureStatus status) {
        if (featureListAdapter != null) {
            HashMap<String,String> featureInfo = new HashMap<>();
            featureInfo.put(ObviousFeatureFragment.FEATURE_NAME, featureName);
            featureInfo.put(ObviousFeatureFragment.FEATURE_STATUS, status.getEnableState().toString());
            featureInfo.put(ObviousFeatureFragment.FEATURE_ACTIVE, status.getToggleState().toString());
            featureInfo.put(ObviousFeatureFragment.FEATURE_ID, String.valueOf(featureid));
            featureListAdapter.add(featureInfo);
            featureListAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Use the OcelotFeatureManager to query the available features for this device.
     */
    private void _checkFeatureStatus() {
        if (_obviousMgr != null) {
            if (featureListAdapter != null)  {
                featureListAdapter.clear();
                featureListAdapter.notifyDataSetChanged();
            }

            if (_progressDlg != null) {
                _progressDlg.dismiss();
                _progressDlg = null;
            }

            _progressDlg = ProgressDialog.show(getContext(), null, getString(R.string.obvious_feature_loadstatus), true, false);
            _progressDlg.show();

            _obviousMgr.getFeatureList();
        }
    }

    /**
     * For each available feature, query the state of the feature from the device.
     */
    private void _processFeatureList() {
        if (_obviousFeatures == null || featureKeys == null || !featureKeys.hasNext()) {
            featureKeys = null;
            featureEntry = null;
            _obviousFeatures = null;

            if (_progressDlg != null) {
                _progressDlg.dismiss();
                _progressDlg = null;
            }

            _updateDataDisplay(selectedName);
            return;
        }

        featureEntry = featureKeys.next();
        _obviousMgr.checkFeatureStatus(featureEntry.getValue());
    }

    /**
     * Use the OcelotFeatureManager to perform a feature update.  This will communicate with the Obvious portal
     * to verify the enabled features and upload a new feature update to the device if any changes are required.
     */
    private void _startFeatureUpdateProcess() {
        if (_progressDlg != null) {
            _progressDlg.dismiss();
            _progressDlg = null;
        }

        _setupFeatureManager();
        if (_obviousMgr != null && _bleDev != null) {
            _bleDev.setupObvious(_obviousMgr.getDeviceConnector());
        }
        if (!_firmwareManualCheck && _obviousMgr != null && _bleDev != null) {
            _progressDlg = ProgressDialog.show(getContext(), null, getString(R.string.obvious_feature_update), true, false);
            _progressDlg.show();
            _obviousMgr.startFeatureUpdate();
        }
        _firmwareManualCheck = false;
    }

    /**
     * Use the OcelotFirmwareManager to connect to the Obvious portal to check if there are any new firmwares available for the device
     */
    private void _startFirmwareCheckProcess() {
        _clearProgressDialog();
        _progressDlg = ProgressDialog.show(getContext(),null, getString(R.string.obvious_firmware_check),true,false);
        _progressDlg.show();

        // inject the connector for the firmware manager before starting the check process
        _setupFirmwareManager();
        if (_obviousFirmwareMgr != null && _bleDev != null) {
            _bleDev.setupObvious(_obviousFirmwareMgr.getDeviceConnector());
            _obviousFirmwareMgr.upgradeCheck(_getAppVersion(), this);
        }
    }

    /**
     * Use the OcelotFirmwareManager to download the new firmware from the Obvious portal and upload it to the device.
     */
    private void _startFirmwareUpdateProcess() {
        _clearProgressDialog();

        if (BuildConfig.DEBUG) {
            Log.d(this.getClass().getSimpleName(), "_startFirmwareUpdateProcess() - SNAKEBAR UPGRADE CLICKED");
        }

        _progressDlg = ProgressDialog.show(getContext(),null, getString(R.string.obvious_firmware_ota),true,false);
        _progressDlg.show();

        // inject the connector for the firmware manager before starting the update process
        _setupFirmwareManager();
        if (_obviousFirmwareMgr != null && _bleDev != null) {
            _bleDev.setupObvious(_obviousFirmwareMgr.getDeviceConnector());
            _obviousFirmwareMgr.startFirmwareUpgrade(_getAppVersion());
        }
    }

    private String _getAppVersion() {
        final String VERSION_SUFFIX_DELIMITER = "-";

        String versionStr = getString(R.string.obvious_device_blank);
        if (getActivity() != null) {
            PackageInfo pinfo;
            try {
                pinfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                pinfo = null;
            }
            if (pinfo != null) {
                versionStr = pinfo.versionName;
                if (versionStr.contains(VERSION_SUFFIX_DELIMITER)) {
                    versionStr = versionStr.substring(0, versionStr.indexOf(VERSION_SUFFIX_DELIMITER));
                }
            }
        }
        return versionStr;
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
        if (_progressDlg != null) {
            _progressDlg.dismiss();
            _progressDlg = null;
        }
        if (status == OcelotFeatureEventListener.FEATUREUPDATE_SUCCESS) {
            AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
            alert.setTitle(R.string.obvious_feature_status_title)
                    .setMessage(R.string.obvious_feature_success)
                    .setCancelable(true);
            alert.show();
        } else if (status == OcelotFeatureEventListener.FEATUREUPDATE_SUCCESS_RESETTING) {
            _progressDlg = ProgressDialog.show(getContext(), null, getString(R.string.obvious_feature_resetting), true, false);
            _progressDlg.show();
            _pendingReset = true;
            return;
        } else if ((status == OcelotFeatureEventListener.FEATUREUPDATE_RESET_COMPLETE)) {
            if (_pendingReset) {
                _pendingReset = false;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            _connectClicked();
                        }
                    });
                }
            }
            return;
        } else if (status == OcelotFeatureEventListener.FEATUREUPDATE_NOT_PROVISIONED) {
            _progressDlg = ProgressDialog.show(getContext(), null, getString(R.string.obvious_feature_provisioning), true, false);
            _progressDlg.show();
            return;
        } else {
            String msg = String.format(Locale.getDefault(),"%s (%d).", getString(R.string.obvious_feature_failed), status);
            AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
            alert.setTitle(R.string.obvious_feature_status_title)
                    .setMessage(msg)
                    .setCancelable(true);
            alert.show();
        }
        _checkFeatureStatus();
        _pendingReset = false;
    }

    /**
     * Notification of the provisioning status of the device.  This is sent for the response of
     * a provisioning process request.
     * @param status The status of the provisioning process.
     */
    @Override
    public void onProvisioningStatus(int status) {
        _clearProgressDialog();

        String msg = String.format(Locale.getDefault(),"%s (%d).", getString(R.string.obvious_feature_provfailed), status);
        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
        alert.setTitle(R.string.obvious_feature_status_title)
                .setMessage(msg)
                .setCancelable(true);
        alert.show();
    }

    /**
     * Notification of the activation status of the requested feature.
     * @param featureid the feaure id that was checked.
     * @param status the status of the feature.
     */
    @Override
    public void onCheckFeatureStatus(int featureid, int status) {
        // Not implemented - using new enabled and toggle information statuses
    }

    /**
     * Notification that the feature list request has been completed.
     * @param features A HashMap containing the feature name and feature id. If feature is null, then there
     *                 was an error retrieving the list.
     */
    @Override
    public void onFeatureList(HashMap<String, Integer> features) {
        featureListAdapter.clear();
        featureListAdapter.notifyDataSetChanged();
        _obviousMgr.getDeviceInfo(this);

        _obviousFeatures = new HashMap<>(features);
        featureKeys = _obviousFeatures.entrySet().iterator();
        _processFeatureList();
    }
    //
    // OcelotFeatureEventListener interface implementation END
    //

    //
    // OcelotFirmwareAvailableListener interface implementation START
    //
    /**
     * Notify the listener of the current firmware available from the server
     * @param currentVersionCode The version code of the current firmware on the device
     * @param ocelotFirmwareInfo This is a OcelotFirmwareInfo object containing the details of the firmware available
     */
    @Override
    public void onFirmwareUpgradeAvailable(long currentVersionCode, OcelotFirmwareInfo ocelotFirmwareInfo) {
        _clearProgressDialog();

        if (currentVersionCode != OcelotFirmwareManager.INVALID_FIRMWARE_VERSION && ocelotFirmwareInfo != null) {
            if (BuildConfig.DEBUG) {
                Log.d(this.getClass().getSimpleName(), "onFirmwareUpgradeAvailable() - ocelotFirmwareInfo.versionCode = " + ocelotFirmwareInfo.versionCode);
            }
            if (_firmwareAvail != null && _firmwareAvail.isShown()) {
                _firmwareAvail.dismiss();
            }
            _firmwareAvail = Snackbar.make(rootView,R.string.obvious_firmware_message,Snackbar.LENGTH_INDEFINITE);
            _firmwareAvail.setAction(R.string.obvious_firmware_upgrade, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    _firmwareInprogress = true;
                    _startFirmwareUpdateProcess();
                }
            });
            _firmwareAvail.show();
        }

        // Start the feature update process after the firmware check process
        _startFeatureUpdateProcess();
    }
    //
    // OcelotFirmwareAvailableListener interface implementation END
    //

    //
    // OcelotFirmwareEventListener interface implementation START
    //

    /**
     * Firmware update progress event.  This callback triggered when the firmware file transfer progress should be updated.
     * @param serialnumber The serialnumber of the device that is being firmware updated.
     * @param percent The percentage complete of the firmware transfer.
     */
    @Override
    public void onFirmwareProgressUpdate(String serialnumber, int percent) {
        if (percent == OcelotFirmwareManager.FIRMWARE_PROGRESS_PREPARING) {
            _clearProgressDialog();
            _progressDlg = ProgressDialog.show(getContext(),null, getString(R.string.obvious_firmware_otaprepare),true,false);
            _progressDlg.show();
        } else if (percent == OcelotFirmwareManager.FIRMWARE_PROGRESS_RESETTING) {
            _clearProgressDialog();
            _progressDlg = ProgressDialog.show(getContext(),null, getString(R.string.obvious_firmware_otaresetting),true,false);
            _progressDlg.show();
        } else {
            if (_progressDlg == null || _progressDlg.isIndeterminate()) {
                _clearProgressDialog();

                _progressDlg = new ProgressDialog(getContext());
                _progressDlg.setIndeterminate(false);
                _progressDlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                _progressDlg.setTitle(selectedName);
                _progressDlg.setMessage(getString(R.string.obvious_firmware_otaprogress));
                _progressDlg.setMax(100);
                _progressDlg.setCancelable(false);
                _progressDlg.show();
            }

            _progressDlg.setProgress(percent);
            if (!_progressDlg.isShowing()) {
                _progressDlg.show();
            }
        }
    }

    /**
     * This callback indicates that the OcelotFirmwareManager was unable to download the new firmware from the server.
     */
    @Override
    public void onFirmwareDownloadFailed() {
        _firmwareInprogress = false;
        _clearProgressDialog();
        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
        alert.setTitle(R.string.obvious_firmware_otastatus)
                .setMessage(R.string.obvious_firmware_otadownloaderror)
                .setCancelable(true);
        alert.show();
    }

    /**
     * This callback is called when the firmware update process has successfully completed.
     */
    @Override
    public void onFirmwareUpgradeSuccessful() {
        _clearProgressDialog();
        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
        alert.setTitle(R.string.obvious_firmware_otastatus)
                .setMessage(R.string.obvious_firmware_otaupgradesuccess)
                .setCancelable(true);
        alert.show();

        _progressDlg = ProgressDialog.show(getContext(), null, getString(R.string.obvious_firmware_loadversioninfo), true, false);
        _progressDlg.show();

        _obviousMgr = null;
        _obviousFirmwareMgr.getDeviceInfo(this);
    }

    /**
     * This callback indicates that the Firmware update transfer to the device failed.
     */
    @Override
    public void onFirmwareUpgradeFailed() {
        _firmwareInprogress = false;
        _clearProgressDialog();
        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
        alert.setTitle(R.string.obvious_firmware_otastatus)
                .setMessage(R.string.obvious_firmware_otaupgradeerror)
                .setCancelable(true);
        alert.show();
    }
    //
    // OcelotDeviceInfoListener interface implementation END
    //

    //
    // OcelotFeatureEventListener interface implementation START
    //
    @Override
    public void onDeviceInfoAvailable(OcelotDeviceInfo ocelotDeviceInfo) {
        _devSN = ocelotDeviceInfo.getSerialNumber();
        _prodId = ocelotDeviceInfo.getProductIdentifier();
        if (_prodId == null || _prodId.equals("") || _prodId.equals("-1")) {
            _prodId = ObviousProductIdentifier.MANUFACTURER_PRODUCT_ID1;
        }
        _fwver = ocelotDeviceInfo.getFirmwareVersion();
        _fwbootver = ocelotDeviceInfo.getBootLoaderVersion();
        _fwsoftver = ocelotDeviceInfo.getSoftDeviceVersion();
        if (getFragmentManager() != null) {
            Fragment state = getFragmentManager().findFragmentByTag(ObviousBoilerplateActivity.STATE_TAG);
            if (state instanceof ObviousAppStateFragment) {
                ((ObviousAppStateFragment) state).setDeviceSerialnumber(_devSN);
            }
        }

        if (_firmwareInprogress) {
            _firmwareInprogress = false;
            _updateDataDisplay(selectedName);
        }
    }
    //
    // OcelotDeviceInfoListener interface implementation END
    //

    //
    // OcelotToggleEventListener interface implementation START
    //
    @Override
    public void onCheckFeatureStatuses(Map<Integer, OcelotFeatureStatus> featureStatusMap) {

    }

    @Override
    public void onCheckFeatureStatus(int featureId, OcelotFeatureStatus ocelotFeatureStatus) {
        if (featureEntry != null && featureEntry.getValue() == featureId) {
            _updateFeatureDisplay(featureEntry.getValue(), featureEntry.getKey(), ocelotFeatureStatus);
        } else {
            featureKeys = null;
        }
        _processFeatureList();
    }

    @Override
    public void didToggleFeatureStatus(int featureId, OcelotFeatureStatus ocelotFeatureStatus) {
        if (_progressDlg != null) {
            _progressDlg.dismiss();
            _progressDlg = null;
        }
        _pendingToggle = false;
        if (featureListAdapter != null) {
            _updateFeatureDisplay(featureId, null, ocelotFeatureStatus);
        }
    }

    @Override
    public void didToggleFeatureStatuses(Map<Integer, OcelotFeatureStatus> map) {
        if (_progressDlg != null) {
            _progressDlg.dismiss();
            _progressDlg = null;
        }
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "didToggleFeatureStatuses -- DONE");
        }
        _pendingToggle = false;
    }

    @Override
    public void featureStatusFailure() {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, " -------- Feature Status Checking or Toggling Failed --------");
        }
        if (!_pendingToggle) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    _checkFeatureStatus();
                }
            }, 300);
        } else {
            _clearProgressDialog();

            AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
            alert.setTitle(R.string.obvious_feature_status_title)
                    .setMessage(R.string.obvious_feature_togglefailed)
                    .setCancelable(true);
            alert.show();

            onBackPressed();
        }
        _pendingToggle = false;
    }
    //
    // OcelotToggleEventListener interface implementation START
    //

    @Override
    public void onToggleCheckChanged(int position, int featureId, boolean isChecked) {
        if (_obviousMgr != null) {
            if (_progressDlg != null) {
                _progressDlg.dismiss();
                _progressDlg = null;
            }
            _progressDlg = ProgressDialog.show(getContext(),null, getString(R.string.obvious_feature_toggle),true,false);
            _progressDlg.show();

            _pendingToggle = true;
            _obviousMgr.toggleFeature(featureId);
        }
    }

    //
    // EasyPermissions handing START
    //
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            onPermissionsGranted(requestCode, new ArrayList<>(Arrays.asList(REQUIRED_PERMISSIONS)));
        } else if ((requestCode == BluetoothInteractor.ENABLE_REQUEST_CODE || requestCode == LocationStatusInteractor.REQUEST_LOCATION_SETTINGS) && resultCode == AppCompatActivity.RESULT_OK) {
            _scanForDevice();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        if (requestCode == REQUIRED_PERMISSIONS_REQUEST_CODE || requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            if (perms.size() == REQUIRED_PERMISSIONS.length) {
                _scanForDevice();
            }
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (requestCode == REQUIRED_PERMISSIONS_REQUEST_CODE) {
            if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
                new AppSettingsDialog.Builder(this).build().show();
            }
        }
    }
    //
    // EasyPermissions handing END
    //

    @Override
    public boolean onBackPressed() {
        boolean status = false;
        final ViewFlipper flipper = rootView.findViewById(R.id.main_flipper);
        if (flipper != null) {
            switch (flipper.getDisplayedChild()) {
                case SCAN_PAGE:
                    if (_firmwareAvail != null) {
                        _firmwareAvail.dismiss();
                        _firmwareAvail = null;
                    }
                    break;

                case FEATURE_PAGE:
                    flipper.setInAnimation(getContext(), R.anim.transition_in_right);
                    flipper.setOutAnimation(getContext(), R.anim.transition_out_right);
                    flipper.showPrevious();
                    if (_bleDev != null && _bleDev.isConnected()) {
                        _bleDev.disconnect();
                        _bleDev = null;
                    }
                    if (featureListAdapter != null) {
                        featureListAdapter.clear();
                        featureListAdapter.notifyDataSetChanged();
                    }
                    if (_firmwareAvail != null) {
                        _firmwareAvail.dismiss();
                        _firmwareAvail = null;
                    }
                    status = true;
                    break;

                default:
                    status = false;
                    break;
            }
        }
        return status;
    }
}
