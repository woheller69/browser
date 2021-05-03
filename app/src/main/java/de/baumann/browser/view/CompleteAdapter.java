package de.baumann.browser.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.baumann.browser.database.Record;
import de.baumann.browser.R;


public class CompleteAdapter extends BaseAdapter implements Filterable {
    private class CompleteFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            if (prefix == null) {
                return new FilterResults();
            }

            resultList.clear();
            for (CompleteItem item : originalList) {
                if (item.getTitle().contains(prefix) || item.getTitle().toLowerCase().contains(prefix) || item.getURL().contains(prefix)) {
                    if (item.getTitle().contains(prefix) || item.getTitle().toLowerCase().contains(prefix) ) {
                        item.setIndex(item.getTitle().indexOf(prefix.toString()));
                    } else if (item.getURL().contains(prefix)) {
                        item.setIndex(item.getURL().indexOf(prefix.toString()));
                    }
                    resultList.add(item);
                }
            }

            Collections.sort(resultList, (first, second) -> Integer.compare(first.getIndex(), second.getIndex()));

            FilterResults results = new FilterResults();
            results.values = resultList;
            results.count = resultList.size();

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            notifyDataSetChanged();
        }
    }

    private static class CompleteItem {
        private final String title;

        String getTitle() {
            return title;
        }

        private final String url;

        String getURL() {
            return url;
        }

        private int index = Integer.MAX_VALUE;

        int getIndex() {
            return index;
        }

        void setIndex(int index) {
            this.index = index;
        }

        private CompleteItem(String title, String url) {
            this.title = title;
            this.url = url;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof CompleteItem)) {
                return false;
            }

            CompleteItem item = (CompleteItem) object;
            return item.getTitle().equals(title) && item.getURL().equals(url);
        }

        @Override
        public int hashCode() {
            if (title == null || url == null) {
                return 0;
            }

            return title.hashCode() & url.hashCode();
        }
    }

    private static class Holder {
        private TextView titleView;
        private TextView urlView;
    }

    private final Context context;
    private final int layoutResId;
    private final List<CompleteItem> originalList;
    private final List<CompleteItem> resultList;
    private final CompleteFilter filter = new CompleteFilter();

    public CompleteAdapter(Context context, int layoutResId, List<Record> recordList) {
        this.context = context;
        this.layoutResId = layoutResId;
        this.originalList = new ArrayList<>();
        this.resultList = new ArrayList<>();
        getRecordList(recordList);
    }

    private void getRecordList(List<Record> recordList) {
        for (Record record : recordList) {
            if (record.getTitle() != null
                    && !record.getTitle().isEmpty()
                    && record.getURL() != null
                    && !record.getURL().isEmpty()) {
                originalList.add(new CompleteItem(record.getTitle(), record.getURL()));
            }
        }

        Set<CompleteItem> set = new HashSet<>(originalList);
        originalList.clear();
        originalList.addAll(set);
    }

    @Override
    public int getCount() {
        return resultList.size();
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    @Override
    public Object getItem(int position) {
        return resultList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        Holder holder;

        if (view == null) {
            view = LayoutInflater.from(context).inflate(layoutResId, null, false);
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.primaryDarkColor));
            holder = new Holder();
            holder.titleView = view.findViewById(R.id.record_item_title);
            holder.titleView.setTextColor(ContextCompat.getColor(context, R.color.color_light));
            holder.urlView = view.findViewById(R.id.record_item_time);
            view.setTag(holder);
        } else {
            holder = (Holder) view.getTag();
        }

        CompleteItem item = resultList.get(position);
        holder.titleView.setText(item.title);
        holder.urlView.setVisibility(View.GONE);
        holder.urlView.setText(item.url);

        return view;
    }
}