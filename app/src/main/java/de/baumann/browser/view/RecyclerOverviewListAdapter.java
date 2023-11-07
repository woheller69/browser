package de.baumann.browser.view;

import static de.baumann.browser.database.UserScript.DOC_START;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import java.util.Collections;
import java.util.List;
import de.baumann.browser.R;
import de.baumann.browser.database.UserScript;
import de.baumann.browser.database.UserScriptsHelper;

public class RecyclerOverviewListAdapter extends RecyclerView.Adapter<RecyclerOverviewListAdapter.ScriptViewHolder> {

    private Context context;
    private final List<UserScript> userScripts;
    private final EditText editText;

    UserScriptsHelper database;

    public RecyclerOverviewListAdapter(Context context, List<UserScript> userScripts, EditText editText) {
        this.context = context;
        this.userScripts = userScripts;
        this.editText = editText;
        this.database = new UserScriptsHelper(context);
    }

    @Override
    public ScriptViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_icon_right, parent, false);
        return new ScriptViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ScriptViewHolder holder, int position) {
        holder.script.setText((userScripts.get(position).getType().equals(DOC_START)? "\u23ee: ":"\u23ed: ") + userScripts.get(position).getName());
        holder.script.setOnClickListener(v -> editText.setText(userScripts.get(holder.getBindingAdapterPosition()).getScript()));
        holder.delete.setOnClickListener(v -> onItemDismiss(holder.getBindingAdapterPosition()));
        holder.active.setVisibility(View.VISIBLE);
        holder.active.setChecked(userScripts.get(holder.getBindingAdapterPosition()).isActive());
        holder.active.setOnClickListener(v -> {
            UserScript script = userScripts.get(holder.getBindingAdapterPosition());
            script.setActive(holder.active.isChecked());
            userScripts.get(holder.getBindingAdapterPosition()).setActive(holder.active.isChecked());
            database.updateScript(script);
            notifyItemChanged(holder.getBindingAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return userScripts.size();
    }


    public void onItemDismiss(int position) {
        database.deleteScript(userScripts.get(position).getId());
        userScripts.remove(position);
        notifyItemRemoved(position);
    }

    public void onItemMove(int fromPosition, int toPosition) {
        // For updating the database records
        UserScript fromUserScript = userScripts.get(fromPosition);
        int fromRank = fromUserScript.getRank();
        UserScript toUserScript = userScripts.get(toPosition);
        int toRank = toUserScript.getRank();

        fromUserScript.setRank(toRank);
        toUserScript.setRank(fromRank);
        database.updateScript(fromUserScript);
        database.updateScript(toUserScript);
        Collections.swap(userScripts, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);

    }

    public static class ScriptViewHolder extends RecyclerView.ViewHolder {

        private TextView script;
        private ImageButton delete;
        private CheckBox active;

        public ScriptViewHolder(View itemView) {
            super(itemView);
            this.script = (TextView) itemView.findViewById(R.id.whitelist_item_domain);
            this.delete = (ImageButton) itemView.findViewById(R.id.whitelist_item_cancel);
            this.active = (CheckBox) itemView.findViewById(R.id.active);
        }

    }
}