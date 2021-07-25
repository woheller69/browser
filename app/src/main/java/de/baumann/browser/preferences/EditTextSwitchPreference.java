package de.baumann.browser.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.appcompat.widget.SwitchCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceViewHolder;

import de.baumann.browser.R;

public class EditTextSwitchPreference extends EditTextPreference {

    private String EditTextSwitchKey;
    private boolean EditTextSwitchKeyDefaultValue;
    private boolean switchAttached=false;

    public EditTextSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        EditTextSwitchKey=null;
        EditTextSwitchKeyDefaultValue=false;
        TypedArray valueArray;
        if(attrs != null)
        {
            valueArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.EditTextSwitchPreference, 0, 0);
            EditTextSwitchKey = valueArray.getString(R.styleable.EditTextSwitchPreference_editTextSwitchKey);
            EditTextSwitchKeyDefaultValue = valueArray.getBoolean(R.styleable.EditTextSwitchPreference_editTextSwitchKeyDefaultValue,false);
            valueArray.recycle();
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        final ViewGroup rootView;
        final SwitchCompat onOffSwitch;
        final CompoundButton.OnCheckedChangeListener checkedChangeListener;
        super.onBindViewHolder(holder);
        rootView = (ViewGroup)holder.itemView;
        if (!switchAttached&&(EditTextSwitchKey!=null)){
            onOffSwitch=new SwitchCompat(getContext());
            rootView.addView(onOffSwitch);
            switchAttached=true;
            onOffSwitch.setChecked(sp.getBoolean(EditTextSwitchKey,EditTextSwitchKeyDefaultValue));
            checkedChangeListener = (buttonView, isChecked) -> {
                if(EditTextSwitchKey != null)
                {
                    sp.edit().putBoolean(EditTextSwitchKey, isChecked).apply();
                }
            };
            onOffSwitch.setOnCheckedChangeListener(checkedChangeListener);
            checkedChangeListener.onCheckedChanged(onOffSwitch, onOffSwitch.isChecked());
        }
    }
}
