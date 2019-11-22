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
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.obvious.mobileapi.OcelotToggleEventListener;

import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Custom ArrayAdapter for displaying the feature items available for the manufacturer.  This adapter
 * uses a custom view to display feature names and status icons.
 */
public class FeatureListAdapter extends ArrayAdapter<HashMap<String,String>> {

    private int itemResId;
    private LayoutInflater inflater;
    private FeatureListOnClickListener _listener;

    private SparseIntArray _featurePosition = new SparseIntArray();

    public interface FeatureListOnClickListener {
        void onToggleCheckChanged(int position, int featureId, boolean isChecked);
    }

    /**
     * Constructor for the ArrayAdapter.
     * @param context the context to use for the adapter.
     * @param resource the custom view that should be used to display the content.
     */
    FeatureListAdapter(@NonNull Context context, int resource, FeatureListOnClickListener listener) {
        super(context, resource);
        itemResId = resource;
        inflater = LayoutInflater.from(context);
        _listener = listener;
    }

    /**
     * Create the custom view that is used to display the adapter data and return it to the view.
     * @param position The position in the list that is being displayed.
     * @param convertView The current view available for reuse in the adapter.
     * @param parent The parent view
     * @return the new view and data that should be displayed
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        final Object viewHolder;

        if (convertView == null) {
            convertView = inflater.inflate(itemResId,null);
            viewHolder = new ViewHolder();
            ((ViewHolder)viewHolder).name = convertView.findViewById(R.id.featurename);
            ((ViewHolder)viewHolder).stateIcon = convertView.findViewById(R.id.featurestate);
            ((ViewHolder)viewHolder).toggleSwitch = convertView.findViewById(R.id.featurestoggle);
            ((ViewHolder)viewHolder).toggleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    final int position = ((ViewHolder)viewHolder).position;
                    final int featureid = ((ViewHolder)viewHolder).featureid;

                    if (buttonView.isPressed() && position != -1) {
                        buttonView.setEnabled(false);
                        if (FeatureListAdapter.this._listener != null) {
                            FeatureListAdapter.this._listener.onToggleCheckChanged(position, featureid, isChecked);
                        }
                    }
                }
            });
            convertView.setTag(viewHolder);
        } else {
            viewHolder = convertView.getTag();
        }

        HashMap<String,String> infoItem = getItem(position);
        if (infoItem != null && viewHolder != null) {
            ((ViewHolder)viewHolder).position = position;
            String curFeature = infoItem.get(ObviousFeatureFragment.FEATURE_ID);
            if (curFeature != null) {
                ((ViewHolder) viewHolder).featureid = Integer.valueOf(curFeature);
            } else {
                ((ViewHolder) viewHolder).featureid = -1;
            }
            ((ViewHolder)viewHolder).name.setText(infoItem.get(ObviousFeatureFragment.FEATURE_NAME));
            String itemStatus = infoItem.get(ObviousFeatureFragment.FEATURE_STATUS);
            if (itemStatus == null) { itemStatus = OcelotToggleEventListener.OcelotEnableStatus.Enabled.toString(); }
            String itemActive = infoItem.get(ObviousFeatureFragment.FEATURE_ACTIVE);
            if (itemActive == null) { itemActive = OcelotToggleEventListener.OcelotToggleStatus.Unknown.toString(); }
            boolean featureEnabled = OcelotToggleEventListener.OcelotEnableStatus.Enabled.toString().equals(itemStatus);
            (((ViewHolder)viewHolder).stateIcon).setImageResource(featureEnabled ? R.drawable.ic_feature_enabled_24px : R.drawable.ic_feature_disabled_24px);
            (((ViewHolder)viewHolder).stateIcon).setImageAlpha(featureEnabled ? 0xff : (0xff / 4));
            if (featureEnabled && !itemActive.equals(OcelotToggleEventListener.OcelotToggleStatus.Unsupported.toString())) {
                boolean toggleEnabled = OcelotToggleEventListener.OcelotToggleStatus.Activated.toString().equals(itemActive);
                (((ViewHolder)viewHolder).toggleSwitch).setChecked(toggleEnabled);
                (((ViewHolder)viewHolder).toggleSwitch).setVisibility(View.VISIBLE);
                (((ViewHolder)viewHolder).toggleSwitch).setEnabled(true);
            } else {
                (((ViewHolder)viewHolder).toggleSwitch).setVisibility(View.INVISIBLE);
                (((ViewHolder)viewHolder).toggleSwitch).setEnabled(false);
            }
        } else {
            if (viewHolder != null) {
                ((ViewHolder) viewHolder).position = -1;
                ((ViewHolder) viewHolder).featureid = -1;
            }
            Log.d(this.getClass().getSimpleName(), "itemInfo is null");
        }
        return convertView;
    }

    @Override
    public void add(@Nullable HashMap<String, String> infoItem) {
        if (infoItem == null) { return; }

        String featureStr = infoItem.get(ObviousFeatureFragment.FEATURE_ID);
        if (featureStr == null) { return; }

        int featureid = Integer.valueOf(featureStr);
        int featurePos = _featurePosition.get(featureid,-1);
        if (featurePos == -1 || getCount() == 0) {
            super.add(infoItem);
            _featurePosition.put(featureid,getPosition(infoItem));
        } else {
            HashMap<String, String> curInfo = getItem(featurePos);
            infoItem.put(ObviousFeatureFragment.FEATURE_NAME,curInfo.get(ObviousFeatureFragment.FEATURE_NAME));
            remove(curInfo);
            insert(infoItem,featurePos);
            _featurePosition.put(featureid,featurePos);
        }
    }

    @Override
    public void clear() {
        super.clear();
        _featurePosition.clear();
    }

    /**
     * Private holder class to cache the views used by the active view being displayed for an item.
     */
    private static class ViewHolder {
        ImageView stateIcon = null;
        TextView name = null;
        Switch toggleSwitch = null;
        int position = -1;
        int featureid = -1;
    }
}
