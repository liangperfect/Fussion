
package com.westalgo.factorycamera;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import com.westalgo.factorycamera.R;

public class VerifyResultListAdapter extends BaseAdapter {
    private Context mContext;
    private String TAG = "VerifyResultListAdapter";
    private final List<FilterHolder> filterArray = new ArrayList<FilterHolder>();
    LayoutInflater inflater;
    public static final int RESULTS_LENGTH = 7;

    private float[] verifyItemResult = {-1,-1,-1,-1,-1,-1,-1};

    private static final int[] ITEM_NAME_IDS = {
            R.string.verify_err,
            R.string.verify_distance,
            R.string.verify_delta_y,
            R.string.verify_linear_ori_left,
            R.string.verify_linear_dst_left,
            R.string.verify_linear_ori_right,
            R.string.verify_linear_dst_right
    };


    public VerifyResultListAdapter(Context c, float[] results) {
        mContext = c;
        inflater = LayoutInflater.from(c);
        if (results != null && results.length == RESULTS_LENGTH) {
            verifyItemResult = results;
        }
        initResultItems();
    }

    public void refresh(float[] listValue) {
        if (listValue != null && listValue.length == RESULTS_LENGTH) {
            Log.i(TAG, "refresh, update verify result items");
            verifyItemResult = listValue;
        } else {
            Log.w(TAG, "refresh, update verify result items failed.");
        }
        notifyDataSetChanged();
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
        TextView resultTitle;
        TextView resultValue;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        FilterHolder mFilterHolder;
        mFilterHolder = getItem(position);
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.verify_result_item, null);
        }
        if (mFilterHolder != null) {
            mFilterHolder.resultTitle = (TextView) convertView.findViewById(R.id.verify_item_title);
            mFilterHolder.resultValue = (TextView) convertView.findViewById(R.id.verify_item_content);
            mFilterHolder.resultTitle.setText(ITEM_NAME_IDS[position]);
            mFilterHolder.resultValue.setText(String.valueOf(verifyItemResult[position]));
            // Log.d(TAG, "--->getView, position: "+position+", string: "+ITEM_NAME_IDS[position]
            // +", value: "+verifyItemResult[position]);
        }

        return convertView;
    }

    public void setSeletedItem(int position, View v, boolean selected) {
        if (position < 0 || position > filterArray.size()) {
            return;
        }
//        try {
//            ImageButton image = (ImageButton) v.findViewById(R.id.image);
//            TextView tv = (TextView) v.findViewById(R.id.filter_name_tv);
//            image.setSelected(selected);
//            tv.setSelected(selected);
//        } catch (Exception e) {
//        }
    }

    public void initResultItems() {
        Log.d(TAG, "initResultItems");
        FilterHolder mHolder;
        for (int i = 0; i < ITEM_NAME_IDS.length; i++) {
            mHolder = new FilterHolder();
            filterArray.add(mHolder);
        }
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

}
