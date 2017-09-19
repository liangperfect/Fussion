
package com.westalgo.factorycamera.ui;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.westalgo.factorycamera.R;
import com.westalgo.factorycamera.settings.SettingUtil;

import java.util.ArrayList;
import java.util.List;

public class ISOSettingAdapter extends BaseAdapter {
    private Context mContext;
    private String TAG = "ISOSettingAdapter";
    private final List<FilterHolder> filterArray = new ArrayList<FilterHolder>();
    LayoutInflater inflater;
    private HorizontalListView listParent;
    public void setListParent(HorizontalListView listParent) {
        this.listParent = listParent;
    }

    private static final int[] ISO_VALUE_IDS = {
            R.string.camera_iso_auto,
            R.string.camera_iso_100,
            R.string.camera_iso_200,
            R.string.camera_iso_400,
            R.string.camera_iso_800,
            R.string.camera_iso_1600
    };
    //store supported ISO settings
    public static String[] ISO_VALUES;
    public static String ISO_SUPPORTED_STRING;


    public ISOSettingAdapter(Context c) {
        mContext = c;
        inflater = LayoutInflater.from(c);
    }

    public int getCount() {
        return filterArray.size();
    }

    public FilterHolder getItem(int position) {
        return position < filterArray.size() ? filterArray.get(position) : null;
    }

    public long getItemId(int position) {
        return position;
    }

    private static class FilterHolder{
//        ImageButton filterImageBtn;
        TextView filterNameTv;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        FilterHolder mFilterHolder;
        mFilterHolder = getItem(position);
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.iso_item, null);
        }
        if (mFilterHolder != null) {
            // mFilterHolder.filterImageBtn = (ImageButton) convertView.findViewById(R.id.image);
            mFilterHolder.filterNameTv = (TextView) convertView.findViewById(R.id.iso_value_tv);
            // mFilterHolder.filterImageBtn.setImageResource(FILTER_BACKGOUND_IDS[position]);
            //mFilterHolder.filterNameTv.setText(ISO_VALUE_IDS[position]);
            //mFilterHolder.filterNameTv.setTag(mContext.getResources().getString(ISO_VALUE_IDS[position]));

            if (ISO_VALUES != null && ISO_VALUES.length > 1) {
                mFilterHolder.filterNameTv.setText(ISO_VALUES[position].toUpperCase());
                mFilterHolder.filterNameTv.setTag(ISO_VALUES[position]);
            } else {
                mFilterHolder.filterNameTv.setText(ISO_VALUE_IDS[position]);
                mFilterHolder.filterNameTv.setTag(mContext.getResources().getString(ISO_VALUE_IDS[position]));
            }
        }

        if (listParent != null) {
            if (listParent.getSelectedItemPosition() == position) {
                setSeletedItem(position, convertView, true);
            } else {
                setSeletedItem(position, convertView, false);
            }
        }
        return convertView;
    }

    public void setSeletedItem(int position, View v, boolean selected) {
        if (position < 0 || position > filterArray.size()) {
            return;
        }
        try {
            // ImageButton image = (ImageButton) v.findViewById(R.id.image);
            TextView tv = (TextView) v.findViewById(R.id.iso_value_tv);
            // image.setSelected(selected);
            tv.setSelected(selected);
            if (selected) {
                tv.setTextColor(android.graphics.Color.GREEN);
            } else {
                tv.setTextColor(android.graphics.Color.WHITE);
            }
        } catch (Exception e) {
        }
    }

    public String getSelectedTagValue() {
        if (listParent != null) {
            if (ISO_VALUES != null && ISO_VALUES.length > 1) {
                return ISO_VALUES[listParent.getSelectedItemPosition()];
            }

            return mContext.getResources().getString(ISO_VALUE_IDS[listParent.getSelectedItemPosition()]);
        }
        return null;
    }

    public int getISOValueIndex(String value){
        if (value != null) {
            if (ISO_VALUES != null && ISO_VALUES.length > 1) {
                for (int i = 0; i < ISO_VALUES.length; i++) {
                    if (value.equalsIgnoreCase(ISO_VALUES[i])) {
                        return i;
                    }
                }
            }
            for (int i = 0; i < ISO_VALUE_IDS.length; i++) {
                if (value.equalsIgnoreCase(mContext.getResources().getString(ISO_VALUE_IDS[i]))) {
                    return i;
                }
            }
        }
        return -1;
    }

    public void createFilters() {
        FilterHolder mHolder;
        for (int i = 0; i < ISO_VALUE_IDS.length; i++) {
            mHolder = new FilterHolder();
            filterArray.add(mHolder);
        }
        Log.i(TAG, "createFilters end, count: "+filterArray.size());
    }

    public int getEntryPos(FilterHolder info) {
        int pos = -1;
        for (int i = 0, len = filterArray.size(); i < len; i++) {
            if (info == filterArray.get(i)) {
                pos = i;
                break;
            }
        }
        return pos;
    }

    public List<FilterHolder> getFilterEntry() {
        return filterArray;
    }

    public void clear() {
        filterArray.clear();
        notifyDataSetChanged();
    }

    public void updateISOItems(){
        if (ISO_SUPPORTED_STRING == null || ISO_SUPPORTED_STRING.equalsIgnoreCase("")) {
            return;
        }
        ISO_VALUES = SettingUtil.getStringValueSplitWhiteSpace(ISO_SUPPORTED_STRING);
        if (ISO_VALUES != null) {
            Log.i(TAG, "updateISOItems...refresh iso setting items, length: "+ISO_VALUES.length);
            clear();
            createISOItems();
            notifyDataSetChanged();
        }
    }

    public void createISOItems() {
        FilterHolder mHolder;
        for (int i = 0; i < ISO_VALUES.length; i++) {
            mHolder = new FilterHolder();
            filterArray.add(mHolder);
        }
        Log.i(TAG, "createFilters end, count: "+filterArray.size());
    }

}
