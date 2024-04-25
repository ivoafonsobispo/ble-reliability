package pt.ipleiria.gattclient;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

public class ExpandableListAdapter extends BaseExpandableListAdapter {

    private final Context mContext;
    private final List<String> mListGroups;
    private final HashMap<String, List<String>> mListChildMap;

    public ExpandableListAdapter(Context context, List<String> listGroups,
                                 HashMap<String, List<String>> listChildMap) {
        this.mContext = context;
        this.mListGroups = listGroups;
        this.mListChildMap = listChildMap;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return Objects.requireNonNull(this.mListChildMap.get(this.mListGroups.get(groupPosition)))
                .get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        String childText = (String) getChild(groupPosition, childPosition);

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item, null);
        }

        TextView listItem = convertView.findViewById(R.id.listItem);
        listItem.setText(childText);
        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return Objects.requireNonNull(this.mListChildMap.get(this.mListGroups.get(groupPosition))).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return this.mListGroups.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return this.mListGroups.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        String headerTitle = (String) getGroup(groupPosition);

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.list_group, null);
        }

        TextView listGroup = convertView.findViewById(R.id.listGroup);
        listGroup.setTypeface(null, Typeface.BOLD);
        listGroup.setText(headerTitle);
        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
