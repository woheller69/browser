package de.baumann.browser.preferences;

//Originally taken from https://github.com/calsurferpunk/Orbtrack/blob/develop/app/src/main/java/com/nikolaiapps/orbtrack/SwitchTextPreference.java
//which is licensed under the Apache License 2.0 http://www.apache.org/licenses/LICENSE-2.0

import de.baumann.browser.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.internal.PreferenceImageView;

public class SwitchTextPreference extends Preference
{
    private boolean showSwitch;
    private String switchKey;
    private String titleText;
    private String valueText;
    private String hint;
    private boolean switchDefault;
    private String defaultText;
    private EditText valueView;
    private TextView SwitchTextTitle;
    private SwitchCompat switchView;
    private int mIcon;

    public SwitchTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);

        this.setLayoutResource(R.layout.switch_text_preference_layout);

        TypedArray valueArray;

        //set defaults
        showSwitch = true;
        switchKey = null;
        switchDefault = true;
        valueText = "";
        hint = "";
        defaultText="";

        //if there are attributes, retrieve them
        if(attrs != null)
        {
            valueArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SwitchTextPreference, 0, 0);
            showSwitch = valueArray.getBoolean(R.styleable.SwitchTextPreference_showSwitch, true);
            switchKey = valueArray.getString(R.styleable.SwitchTextPreference_switchKey);
            switchDefault = valueArray.getBoolean(R.styleable.SwitchTextPreference_switchDefault,true);
            defaultText = valueArray.getString(R.styleable.SwitchTextPreference_defaultText);
            titleText = valueArray.getString(R.styleable.SwitchTextPreference_titleText);
            hint = valueArray.getString(R.styleable.SwitchTextPreference_hint);
            mIcon=valueArray.getResourceId(R.styleable.SwitchTextPreference_icon,0);
            valueArray.recycle();
        }

    }

    public SwitchTextPreference(Context context, AttributeSet attrs, int defStyleAttr)
    {
        this(context, attrs, defStyleAttr, 0);
    }

    @SuppressWarnings("unused")
    public SwitchTextPreference(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(PreferenceViewHolder holder)
    {
        super.onBindViewHolder(holder);

        final Context context = this.getContext();
        final String preferenceName = this.getKey();
        final CharSequence summary = this.getSummary();
        final TextView summaryView;
        final ViewGroup rootView;
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        final CompoundButton.OnCheckedChangeListener checkedChangeListener;
        final PreferenceImageView icon;

        //get displays
        rootView = (ViewGroup)holder.itemView;
        valueView = rootView.findViewById(R.id.Switch_Text_Preference_Value_Text);
        summaryView = rootView.findViewById(R.id.Switch_Text_Preference_Summary);
        SwitchTextTitle = rootView.findViewById(R.id.Switch_Text_Preference_No_Switch_Title);
        switchView = rootView.findViewById(R.id.Switch_Text_Preference_Switch);
        valueText = sp.getString(preferenceName, defaultText);
        icon=rootView.findViewById(R.id.Switch_Text_Preference_icon);
        if (mIcon!=0) {
            icon.setImageResource(mIcon);
            icon.setVisibility(View.VISIBLE);
        } else{
            icon.setVisibility(View.GONE);
        }

        //set displays
        rootView.setClickable(false);
        valueView.setHint(hint);
        valueView.setOnTouchListener((v, event) -> {
            valueView.setFocusableInTouchMode(true);
            return false;
        });

        valueView.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(valueView.getWindowToken(), 0);

                String stringValue = v.getText().toString();
                boolean save = !stringValue.equals(sp.getString(preferenceName, defaultText));  //only save if changed

                if(save)
                {
                    if(showSwitch && switchView != null && switchView.isChecked()){
                        if (stringValue.equals("")) {    //if text deleted replace with defaultText
                            stringValue=defaultText;
                            valueView.setText(defaultText);
                        }
                        sp.edit().putString(preferenceName, stringValue).apply();
                        valueText = stringValue;
                        valueView.setFocusable(false);
                    }
                }
                return true;
            }
            return false;
        });

        if(summary != null && summary.length() > 0)
        {
            summaryView.setText(summary);
            summaryView.setVisibility(View.VISIBLE);
        }
        if(showSwitch)
        {
            switchView.setText(titleText);
            switchView.setChecked(sp.getBoolean(switchKey,switchDefault));
            checkedChangeListener = (buttonView, isChecked) -> {
                //if showing switch and value exists
                if(showSwitch && valueView != null)
                {
                    //update text and state
                    updateValueText(isChecked);
                    valueView.setEnabled(isChecked);

                    //if saving switch
                    if(switchKey != null)
                    {
                        //save setting
                        sp.edit().putBoolean(switchKey, isChecked).apply();
                    }
                }
            };
            switchView.setOnCheckedChangeListener(checkedChangeListener);
            checkedChangeListener.onCheckedChanged(switchView, switchView.isChecked());
        }
        else
        {
            SwitchTextTitle.setText(titleText);
            updateValueText(false);
        }
        setShowSwitch(showSwitch);
    }

    //Updates value text based on switch state
    private void updateValueText(boolean switchChecked)
    {
        //use enabled text is switch checked, else disabled text
        valueView.setText(valueText);
    }

    //Sets if showing switch
    public void setShowSwitch(boolean show)
    {
        showSwitch = show;

        //if views exist
        if(SwitchTextTitle != null)
        {
            //hide if showing switch
            SwitchTextTitle.setVisibility(showSwitch ? View.GONE : View.VISIBLE);
        }
        if(switchView != null)
        {
            //show if showing switch
            switchView.setVisibility(showSwitch ? View.VISIBLE : View.GONE);
        }
    }
}
