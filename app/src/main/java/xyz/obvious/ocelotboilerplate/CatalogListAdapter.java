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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.obvious.mobileapi.OcelotCatalogListResultListener;
import com.squareup.picasso.Picasso;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Custom ArrayAdapter for displaying the catalog items available for the manufacturer.  This adapter
 * uses a custom view to display catalog thumbnails, prices, names and descriptions.
 */
public class CatalogListAdapter extends ArrayAdapter<OcelotCatalogListResultListener.CatalogItem> {

    private int itemResId;
    private LayoutInflater inflater;

    /**
     * Constructor for the ArrayAdapter.
     * @param context the context to use for the adapter.
     * @param resource the custom view that should be used to display the content.
     */
    CatalogListAdapter(@NonNull Context context, int resource) {
        super(context, resource);
        itemResId = resource;
        inflater = LayoutInflater.from(context);
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

        Object viewHolder;

        if (convertView == null) {
            convertView = inflater.inflate(itemResId,null);
            viewHolder = new ViewHolder();
            ((ViewHolder)viewHolder).name = convertView.findViewById(R.id.itemname);
            ((ViewHolder)viewHolder).description = convertView.findViewById(R.id.itemdescr);
            ((ViewHolder)viewHolder).price = convertView.findViewById(R.id.itemprice);
            ((ViewHolder)viewHolder).thumbNail = convertView.findViewById(R.id.itemthumb);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = convertView.getTag();
        }

        OcelotCatalogListResultListener.CatalogItem infoItem = getItem(position);
        if (infoItem != null && viewHolder != null) {
            ((ViewHolder)viewHolder).name.setText(infoItem.name);
            ((ViewHolder)viewHolder).description.setText(infoItem.description);
            ((ViewHolder)viewHolder).price.setText(String.format(Locale.getDefault(), "%1.2f\n", infoItem.itemprice));
            if (infoItem.thumbnailurl != null && infoItem.thumbnailurl.length() > 0 && !"null".equals(infoItem.thumbnailurl)) {
                ((ViewHolder) viewHolder).thumbNail.setVisibility(View.VISIBLE);
                Picasso.get().load(infoItem.thumbnailurl).into(((ViewHolder) viewHolder).thumbNail);
            } else {
                ((ViewHolder) viewHolder).thumbNail.setVisibility(View.INVISIBLE);
            }
        } else {
            Log.d(this.getClass().getSimpleName(), "itemInfo is null");
        }
        return convertView;
    }

    /**
     * Mark the Item id as stable
     * @return true
     */
    @Override
    public boolean hasStableIds() {
        return true;
    }

    /**
     * Return the Item id of the requested position.
     * @param position The position of the item.
     * @return the stable Item id at the requested position.
     */
    @Override
    public long getItemId(int position) {
        OcelotCatalogListResultListener.CatalogItem posItem = getItem(position);
        if (posItem != null) {
            return posItem.id;
        }
        return super.getItemId(position);
    }

    /**
     * Private holder class to cache the views used by the active view being displayed for an item.
     */
    private static class ViewHolder {
        ImageView thumbNail = null;
        TextView name = null;
        TextView description = null;
        TextView price = null;
    }
}
