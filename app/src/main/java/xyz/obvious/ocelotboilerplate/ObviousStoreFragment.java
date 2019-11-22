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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.obvious.mobileapi.OcelotCatalogCheckoutResultListener;
import com.obvious.mobileapi.OcelotCatalogInteractor;
import com.obvious.mobileapi.OcelotCatalogListResultListener;
import com.obvious.mobileapi.payment.OcelotCheckoutWidget;
import com.obvious.mobileapi.payment.OcelotPaymentClient;

import java.util.ArrayList;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import xyz.obvious.manufacturer.ObviousProductIdentifier;

public class ObviousStoreFragment extends Fragment implements
        OcelotCatalogCheckoutResultListener,
        OcelotCatalogListResultListener,
        ObviousBoilerplateActivity.OnFragmentBackpressListener
{

    private final static int STORE_PAGE = 0;
    private final static int PAYMENT_PAGE = 1;

    private View rootView = null;
    private ProgressDialog _progressDlg = null;

    private OcelotCatalogInteractor catalogInteractor = null;
    private CatalogListAdapter defaultCatalogAdapter = null;
    private OcelotPaymentClient paymentClient = null;

    private int _checkoutCartid = -1;
    private SparseBooleanArray _cartContent = null;

    private boolean _pendingPaymentResult = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View tmpView = inflater.inflate(R.layout.activity_obvious_store, container, false);
        if (tmpView == null) {
            return null;
        }

        Button tmpBtn = tmpView.findViewById(R.id.checkoutbutton);
        if (tmpBtn != null) {
            tmpBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    _checkoutClicked();
                }
            });
        }

        tmpBtn = tmpView.findViewById(R.id.paymentbutton);
        if (tmpBtn != null) {
            tmpBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    _paymentClicked();
                }
            });
        }

        rootView = tmpView;
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        _storeClicked();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (_progressDlg != null && !_pendingPaymentResult) {
            _clearProgressDialog();
        }
    }

    private void _clearProgressDialog() {
        if (_progressDlg != null) {
            _progressDlg.dismiss();
            _progressDlg = null;
        }
    }

    /**
     * Use an OcelotCatalogInteractor to create a cart with the desired items to be purchased.  The OcelotCatalogInteractor returns the cart identifier
     * back to the applications so that it can be used later to complete the purchase when the payment information is collected.
     */
    private void _checkoutClicked() {
        final ViewFlipper flipper = rootView.findViewById(R.id.store_flipper);
        if (flipper == null || flipper.getDisplayedChild() != STORE_PAGE) {
            return;
        }

        String _devSN = null;
        if (getFragmentManager() != null) {
            Fragment state = getFragmentManager().findFragmentByTag(ObviousBoilerplateActivity.STATE_TAG);
            if (state instanceof ObviousAppStateFragment) {
                _devSN = ((ObviousAppStateFragment) state).getDeviceSerialnumber();
            }
        }

        long[] featureIds = null;
        _cartContent = null;
        ListView tmpList = flipper.findViewById(R.id.cataloglist);
        if (tmpList != null && tmpList.getCheckedItemCount() > 0) {
            featureIds = tmpList.getCheckedItemIds();
            _cartContent = tmpList.getCheckedItemPositions();
        }

        if (catalogInteractor != null && featureIds != null) {
            ArrayList<CatalogItem> cartItems = new ArrayList<>();
            for (int idx=0; idx < _cartContent.size(); idx++) {
                if (_cartContent.valueAt(idx)) {
                    cartItems.add(defaultCatalogAdapter.getItem(_cartContent.keyAt(idx)));
                }
            }
            catalogInteractor.checkoutCart( cartItems, _devSN, ObviousProductIdentifier.MANUFACTURER_PRODUCT_ID1);
        }

        OcelotCheckoutWidget ccInput = flipper.findViewById(R.id.cartcard);
        ccInput.clear();
    }

    /**
     * When the result of the cart creation is returned by the OcelotCatalogInteractor, we display the
     * cart summary to the user and allow them to enter the payment information if they want to continue
     * with the feature purchase.
     *
     * @param cartid The cart id associated with the pending feature purchase.
     * @param carttotal The total purchase amount in USD.
     */
    private void _displayCartSummary(int cartid, float carttotal) {
        final ViewFlipper flipper = rootView.findViewById(R.id.store_flipper);
        if (flipper == null) {
            return;
        }

        _checkoutCartid = cartid;

        TextView tmpText = flipper.findViewById(R.id.totalprice);
        if (tmpText != null) {
            tmpText.setText(String.valueOf(carttotal));
        }

        TextView tmpItem = flipper.findViewById(R.id.cartlabel);
        TextView tmpPrice = flipper.findViewById(R.id.cartprice);
        if (tmpItem != null && tmpPrice != null) {
            String msg = getString(R.string.obvious_cart_overview);
            String msgPrice = "\n";
            if (defaultCatalogAdapter != null && _cartContent != null) {
                for (int idx=0; idx < _cartContent.size(); idx++) {
                    if (_cartContent.valueAt(idx)) {
                        OcelotCatalogListResultListener.CatalogItem item = defaultCatalogAdapter.getItem(_cartContent.keyAt(idx));
                        if (item != null) {
                            msg = msg.concat(String.format(Locale.getDefault(), "\t%s\n", item.name));
                            msgPrice = msgPrice.concat(String.format(Locale.getDefault(), "%1.2f\n", item.itemprice));
                        }
                    }
                }
            }
            tmpItem.setText(msg);
            tmpPrice.setText(msgPrice);
        }

        OcelotCheckoutWidget ccInput = flipper.findViewById(R.id.cartcard);
        if (ccInput != null) {
            ccInput.clear();
        }

        flipper.setInAnimation(getContext(), R.anim.transition_in_left);
        flipper.setOutAnimation(getContext(), R.anim.transition_out_left);
        flipper.showNext();
    }

    /**
     * Complete the tokenization of the payment information and pass that on to OcelotCatalogInteractor.
     * The OcelotCatalogInteractor completes the purchase transaction.  The result of the payment is return via
     * the OcelotCatalogCheckoutResultListener interface.
     */
    private void _paymentClicked() {
        final ViewFlipper flipper = rootView.findViewById(R.id.store_flipper);
        if (flipper == null || flipper.getDisplayedChild() != PAYMENT_PAGE || getActivity() == null) {
            return;
        }

        paymentClient = OcelotPaymentClient.getDemoPaymentClient(getActivity().getApplicationContext(), catalogInteractor, this);
        OcelotCheckoutWidget ccInput = flipper.findViewById(R.id.cartcard);
        if (!paymentClient.validatePaymentData(ccInput)) {
            Toast.makeText(getContext(), R.string.obvious_payment_invalidcard, Toast.LENGTH_SHORT).show();
            return;
        }

        // Dismiss the soft keyboard
        FragmentActivity tmpAct = getActivity();
        if (tmpAct != null) {
            InputMethodManager inputMethodManager = (InputMethodManager)getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null && tmpAct.getCurrentFocus() != null) {
                inputMethodManager.hideSoftInputFromWindow(tmpAct.getCurrentFocus().getWindowToken(), 0);
            }
        }

        _progressDlg = ProgressDialog.show(getContext(),null,getString(R.string.obvious_payment_processing),true,false);
        _progressDlg.show();

        if (catalogInteractor != null && _checkoutCartid != -1) {
            String _devSN = null;
            if (getFragmentManager() != null) {
                Fragment state = getFragmentManager().findFragmentByTag(ObviousBoilerplateActivity.STATE_TAG);
                if (state instanceof ObviousAppStateFragment) {
                    _devSN = ((ObviousAppStateFragment) state).getDeviceSerialnumber();
                }
            }
            paymentClient.startPaymentProcessing(ccInput, _checkoutCartid, _devSN, ObviousProductIdentifier.MANUFACTURER_PRODUCT_ID1);
            ccInput.clear();
        } else {
            _clearProgressDialog();
            Toast.makeText(getContext(), R.string.obvious_payment_invalidcard, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Display a status message indicating the result of the payment transaction.
     * @param status The transaction status.
     */
    private void _cartComplete(int status) {
        _clearProgressDialog();

        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
        alert.setTitle(R.string.obvious_feature_status_title)
                .setCancelable(true);

        if (status == 0) {
            alert.setMessage(R.string.obvious_payment_status_complete);
            alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    if (getFragmentManager() != null) {
                        Fragment state = getFragmentManager().findFragmentByTag(ObviousBoilerplateActivity.STATE_TAG);
                        if (state instanceof ObviousAppStateFragment) {
                            ((ObviousAppStateFragment) state).setForceFeatureUpdate(true);
                        }
                    }
                    FragmentActivity tmpAct = getActivity();
                    if (tmpAct instanceof ObviousBoilerplateActivity) {
                        ((ObviousBoilerplateActivity) tmpAct).performNavigation(R.id.navigation_home);
                    }
                }
            });
        } else {
            // TODO: probably should display more detailed message base on status codes.
            alert.setMessage(R.string.obvious_payment_status_failed);
        }
        alert.show();
    }

    /**
     * Use an OcelotCatalogInteractor to load the default catalog for the current manufacturer.  The interactor
     * must be configured with the proper callback so that the app will receive the catalog events.
     */
    private void _storeClicked() {
        String _devSN = null;
        if (getFragmentManager() != null) {
            Fragment state = getFragmentManager().findFragmentByTag(ObviousBoilerplateActivity.STATE_TAG);
            if (state instanceof ObviousAppStateFragment) {
                _devSN = ((ObviousAppStateFragment) state).getDeviceSerialnumber();
            }
        }

        if (_devSN == null) {
            return;
        }

        final ViewFlipper flipper = rootView.findViewById(R.id.store_flipper);
        if (flipper == null) {
            return;
        }

        flipper.setInAnimation(getContext(),R.anim.transition_in_left);
        flipper.setOutAnimation(getContext(),R.anim.transition_out_left);
        flipper.setDisplayedChild(STORE_PAGE);

        ListView tmpList = flipper.findViewById(R.id.cataloglist);
        if (tmpList != null) {
            if (defaultCatalogAdapter == null && getContext() != null) {
                defaultCatalogAdapter = new CatalogListAdapter(getContext(), R.layout.activity_obvious_catalog_item);
            }
            tmpList.setAdapter(defaultCatalogAdapter);
            tmpList.clearChoices();
            tmpList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // TODO:  add any extra handling for item selection
                }
            });
        }

        if (!_pendingPaymentResult) {
            _progressDlg = ProgressDialog.show(getContext(), null, getString(R.string.obvious_store_connecting), true, false);
            _progressDlg.show();
        }

        // setup the OcelotCatalogInteractor with the proper callbacks so that the app can process the catalog and check out events
        if (catalogInteractor == null) {
            catalogInteractor = OcelotCatalogInteractor.getCatalogInteractor();
            catalogInteractor.setAPIKey(ObviousProductIdentifier.MANUFACTURER_API_KEY);
            catalogInteractor.setCatalogListener(this);
            catalogInteractor.setCheckoutListener(this);
        }
        catalogInteractor.getDefaultCatalog(ObviousProductIdentifier.MANUFACTURER_PRODUCT_ID1);
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
        if (!_pendingPaymentResult) {
            _clearProgressDialog();
        }

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

    /**
     * This event indicates that the server was unable to load the default catalog for the manufacturer.
     */
    @Override
    public void onCatalogListFail() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    defaultCatalogAdapter.clear();
                    defaultCatalogAdapter.notifyDataSetChanged();
                }
            });
        }
    }
    //
    // implementation of the OcelotCatalogListResultListener - END
    //


    //
    // implementation of the OcelotCatalogCheckoutResultListener - START
    //

    /**
     * This callback method is call if the cart was successfully created and the total price
     * was calculated.
     *
     * @param totalCost The total amount for the cart contents.
     * @param cartId The cart id of the newly created cart for the transaction.
     */
    @Override
    public void onCheckoutCartSuccess(final float totalCost, final int cartId) {
        // navigate to the summary page and CC data collection page
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    _displayCartSummary(cartId, totalCost);
                }
            });
        }
    }

    /**
     * This callback is called when the server was unable to create a cart for the requested transaction
     */
    @Override
    public void onCheckoutCartFail() {
        _checkoutCartid = -1;
        // TODO: display cart error message
    }

    /**
     * This callback method is used to notify the app that the payment was successfully processed and the
     * purchased features will be available on the device after the next feature update is completed
     */
    @Override
    public void onCheckoutPaySuccess() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    _cartComplete(0);
                }
            });
        }
        _pendingPaymentResult = false;
    }

    /**
     * This callback method is called when the payment processing could not be completed.
     */
    @Override
    public void onCheckoutPayFail() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    _cartComplete(-1);
                }
            });
        }
        _pendingPaymentResult = false;
    }

    @Override
    public void onCheckoutPayActionRequired(String paymentSecret) {
        _pendingPaymentResult = true;
        paymentClient.authenticatePayment(this, paymentSecret);
    }

    //
    // implementation of the OcelotCatalogCheckoutResultListener - END
    //

    /**
     * Handle the back press event so that it returns to the device page and properly
     * sets the bottom navigation view.
     * @return true if the back press is handled, false to perform the default behaviour.
     */
    @Override
    public boolean onBackPressed() {
        boolean status = false;
        final ViewFlipper flipper = rootView.findViewById(R.id.store_flipper);
        if (flipper != null && flipper.getDisplayedChild() == PAYMENT_PAGE) {
            flipper.setInAnimation(getContext(), R.anim.transition_in_right);
            flipper.setOutAnimation(getContext(), R.anim.transition_out_right);
            flipper.showPrevious();
            status = true;
        }
        return status;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (paymentClient != null && _checkoutCartid != -1) {
            _pendingPaymentResult = true;
            paymentClient.onPaymentResult(requestCode, data, _checkoutCartid);
        }
    }
}
