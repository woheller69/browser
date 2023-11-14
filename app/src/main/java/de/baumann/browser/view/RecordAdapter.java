package de.baumann.browser.view;

import android.content.Context;
import androidx.annotation.NonNull;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import de.baumann.browser.database.FaviconHelper;
import de.baumann.browser.database.Record;
import de.baumann.browser.R;
import de.baumann.browser.unit.HelperUnit;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class RecordAdapter extends ArrayAdapter<Record> {
    private final Context context;
    private final int layoutResId;
    private final List<Record> list;

    public RecordAdapter(Context context, List<Record> list) {
        super(context, R.layout.item_icon_left, list);
        this.context = context;
        this.layoutResId = R.layout.item_icon_left;
        this.list = list;
    }

    private static class Holder {
        TextView title;
        ImageView icon;
        ImageView favicon;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        Holder holder;
        View view = convertView;

        if (view == null) {
            view = LayoutInflater.from(context).inflate(layoutResId, parent, false);
            holder = new Holder();
            holder.title = view.findViewById(R.id.record_item_title);
            holder.icon = view.findViewById(R.id.record_item_icon);
            holder.favicon=view.findViewById(R.id.record_item_favicon);
            view.setTag(holder);
        } else {
            holder = (Holder) view.getTag();
        }

        Record record = list.get(position);
        long filter = record.getIconColor();
        holder.title.setText(record.getTitle());

        HelperUnit.setFilterIcons(holder.icon,filter);

        holder.favicon.setVisibility(View.VISIBLE);
        FaviconHelper faviconHelper = new FaviconHelper(context);
        Bitmap bitmap=faviconHelper.getFavicon(record.getURL());

        if (bitmap != null){
            holder.favicon.setImageBitmap(bitmap);
        }else {
            holder.favicon.setImageResource(R.drawable.icon_image_broken);
        }

        return view;
    }
}