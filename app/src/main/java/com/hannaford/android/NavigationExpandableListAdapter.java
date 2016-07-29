package com.hannaford.android;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v4.view.GravityCompat;
import android.text.Html;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.hannaford.android.MainActivity;
import com.hannaford.android.PicassoImageGetter;
import com.hannaford.android.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NavigationExpandableListAdapter extends BaseExpandableListAdapter {

    private MainActivity context;
    private JSONArray navigation;
    private LayoutInflater layoutInflater;

    public NavigationExpandableListAdapter(MainActivity context, JSONArray navigation) {
        this.context = context;
        this.navigation = navigation;
        layoutInflater = (LayoutInflater) context.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public Object getChild(int listPosition, int expandedListPosition) {
        try{
            JSONObject group = ((JSONObject) getGroup(listPosition));
            return group.getJSONArray("children").getJSONObject(expandedListPosition);
        }catch(JSONException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getChildId(int listPosition, int expandedListPosition) {
        return expandedListPosition;
    }

    @Override
    public View getChildView(int listPosition, final int expandedListPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        try{
            JSONObject item = (JSONObject) getChild(listPosition, expandedListPosition);
            if (convertView == null) {
                convertView = layoutInflater.inflate(R.layout.navigation_expandable_list_item, null);
            }
            TextView expandedListTextView = (TextView) convertView
                    .findViewById(R.id.text);
            expandedListTextView.setText(item.getString("text"));
            return convertView;
        }catch(JSONException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getChildrenCount(int listPosition) {
        try {
            JSONObject item = (JSONObject) getGroup(listPosition);
            if(item.has("children")){
                return item.getJSONArray("children").length();
            }else{
                return 0;
            }
        }catch(JSONException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object getGroup(int listPosition) {
        try {
            return navigation.getJSONObject(listPosition);
        }catch(JSONException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getGroupCount() {
        return navigation.length();
    }

    @Override
    public long getGroupId(int listPosition) {
        return listPosition;
    }

    @Override
    public View getGroupView(int listPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        try{
            JSONObject item = (JSONObject) getGroup(listPosition);
            String text = item.getString("text");
            convertView = layoutInflater.inflate(R.layout.navigation_expandable_list_group, null);
            TextView listTitleTextView = (TextView) convertView
                    .findViewById(R.id.text);
            listTitleTextView.setText(text);
            if(item.has("children")) {
                Drawable indicator;
                if (isExpanded) {
                    indicator = context.getResources().getDrawable(android.R.drawable.arrow_down_float);
                } else {
                    indicator = context.getResources().getDrawable(android.R.drawable.arrow_up_float);
                }
                listTitleTextView.setCompoundDrawables(null, null, indicator, null);
            }
            return convertView;
        }catch(JSONException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int listPosition, int expandedListPosition) {
        return true;
    }
}