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

import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import pub.devrel.easypermissions.EasyPermissions;

public class ObviousBoilerplateActivity extends AppCompatActivity
{
    public static final String STATE_TAG = "STATE_FRAG";
    private BottomNavigationView navigation = null;
    private int currentNavId = 0;

    private boolean _ignoreNavEvent = false;

    private static final SparseArray<String> FRAGMENT_TAGS = new SparseArray<>();
    static {
        FRAGMENT_TAGS.put(R.id.navigation_home,"HOME_FRAG");
        FRAGMENT_TAGS.put(R.id.navigation_store,"STORE_FRAG");
    }

    /**
     * Back press handling interface.
     */
    interface OnFragmentBackpressListener {
        boolean onBackPressed();
    }

    /**
     * Listener for processing the BottomNavigation view selections
     */
    private BottomNavigationView.OnNavigationItemSelectedListener _navigationListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            if (_ignoreNavEvent) {
                _ignoreNavEvent = false;
                return true;
            }

            if (currentNavId == item.getItemId()) {
                return false;
            }

            return performNavigation(item.getItemId());
        }
    };

    /**
     * Initialize the Activity
     * @param savedInstanceState The saved state if the Activity has been restarted.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_obvious_mainnav);

        ActionBar tmpBar = getSupportActionBar();
        if (tmpBar != null) {
            tmpBar.setDisplayShowHomeEnabled(true);
            tmpBar.setDisplayUseLogoEnabled(true);
            tmpBar.setLogo(R.drawable.ic_logo_text_white);
            tmpBar.setTitle(R.string.app_short_name);
            tmpBar.show();
        }

        Fragment tmpFrag = getSupportFragmentManager().findFragmentByTag(ObviousBoilerplateActivity.STATE_TAG);
        if (tmpFrag == null) {
            getSupportFragmentManager().beginTransaction().add(new ObviousAppStateFragment(), ObviousBoilerplateActivity.STATE_TAG).commit();
        }

        currentNavId = R.id.navigation_home;
        tmpFrag = new ObviousFeatureFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.mainnav_content, tmpFrag, FRAGMENT_TAGS.get(R.id.navigation_home)).commit();

        navigation = findViewById(R.id.mainbottomnav);
        if (navigation != null) {
            navigation.setOnNavigationItemSelectedListener(_navigationListener);
        }
    }

    /**
     * This method is call when ever a started activity has completed and a result was requested when started.
     * @param requestCode The request code that was used to start the Activity.
     * @param resultCode The result code fo the Activity.
     * @param data The data associated with the Activity result.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Fragment curFragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAGS.get(R.id.navigation_home));
        if (curFragment instanceof ObviousFeatureFragment) {
            curFragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * This method is call by the OS after a permession request was made by the Activity.
     * @param requestCode The request code of the permission request.
     * @param permissions The permissions that were originally requested.
     * @param grantResults The results of the granted permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // proxy permission result to EasyPermissions
        Fragment curFragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAGS.get(R.id.navigation_home));
        if (curFragment instanceof EasyPermissions.PermissionCallbacks) {
            curFragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Pass the back press events on to the currently displayed fragment so it can be properly handled by the fragment if
     * it wants to override the default back press behaviour.
     */
    @Override
    public void onBackPressed() {
        FragmentManager fragMgr = getSupportFragmentManager();
        Fragment frag = fragMgr.findFragmentByTag(FRAGMENT_TAGS.get(currentNavId));

        if (frag instanceof OnFragmentBackpressListener && ((OnFragmentBackpressListener) frag).onBackPressed()) {
            return;
        }

        super.onBackPressed();
        if (currentNavId == R.id.navigation_store) {
            if (navigation != null && fragMgr.getBackStackEntryCount() == 0) {
                navigation.setSelectedItemId(R.id.navigation_home);
            }
        }
    }

    /**
     * This method is used to fill the main content view with the fragment representing the selection that was
     * made in the Bottom navigation view.
     * @param menuItemId the resource id of the menu item selected
     * @return true if the navigation event was handled, false otherwise.
     */
    boolean performNavigation(int menuItemId) {

        String _devSN = null;

        if (getSupportFragmentManager() != null) {
            Fragment state = getSupportFragmentManager().findFragmentByTag(ObviousBoilerplateActivity.STATE_TAG);
            if (state instanceof ObviousAppStateFragment) {
                _devSN = ((ObviousAppStateFragment) state).getDeviceSerialnumber();
            }
        }

        Fragment tmpFrag;
        if (navigation != null) {
            navigation.setVisibility(View.VISIBLE);
        }

        FragmentManager fragMgr = getSupportFragmentManager();
        if (fragMgr == null) {
            return false;
        }

        switch (menuItemId) {
            case R.id.navigation_home:
                if (fragMgr.getBackStackEntryCount() > 0) {
                    fragMgr.popBackStackImmediate();
                }

                currentNavId = menuItemId;
                tmpFrag = fragMgr.findFragmentByTag(FRAGMENT_TAGS.get(R.id.navigation_home));
                if (tmpFrag instanceof ObviousFeatureFragment) {
                    if (navigation != null && navigation.getSelectedItemId() != menuItemId) {
                        _ignoreNavEvent = true;
                        navigation.setSelectedItemId(menuItemId);
                    }
                    return true;
                } else {
                    tmpFrag = new ObviousFeatureFragment();
                    fragMgr.beginTransaction().replace(R.id.mainnav_content, tmpFrag,FRAGMENT_TAGS.get(R.id.navigation_home)).commit();
                }
                return true;

            case R.id.navigation_store:
                if (_devSN != null) {
                    tmpFrag = fragMgr.findFragmentByTag(FRAGMENT_TAGS.get(R.id.navigation_store));
                    if (tmpFrag == null) {
                        tmpFrag = new ObviousStoreFragment();

                        if (fragMgr.getBackStackEntryCount() >= 1) {
                            fragMgr.popBackStack();
                        }

                        fragMgr.beginTransaction().replace(R.id.mainnav_content, tmpFrag,FRAGMENT_TAGS.get(R.id.navigation_store))
                                .addToBackStack("STORE_STACK")
                                .setTransition(FragmentTransaction.TRANSIT_NONE)
                                .setReorderingAllowed(true)
                                .commit();
                    }
                    currentNavId = menuItemId;
                    return true;
                }
                break;
        }
        return false;
    }
}
