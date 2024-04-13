package its.madruga.wpp.views;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.radiobutton.MaterialRadioButton;

import its.madruga.wpp.BuildConfig;
import its.madruga.wpp.R;

public class RadioButton extends LinearLayout {

    public static String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    public RadioButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RadioButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        inflate(context, R.layout.radio_button_layout, (ViewGroup) getRootView());

        var sharedPreferences = context.getSharedPreferences("its.madruga.wpp_preferences", Context.MODE_PRIVATE);
        var editor = sharedPreferences.edit();
        var a = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.RadioButton,
                0, 0
        );
        try {

            var title = a.getText(R.styleable.RadioButton_android_text);
            var summary = a.getText(R.styleable.RadioButton_android_summary);
            var entries = a.getTextArray(R.styleable.RadioButton_android_entries);
            var entryValues = a.getTextArray(R.styleable.RadioButton_android_entryValues);
            var tag = attrs.getAttributeValue(ANDROID_NS, "tag");

            // Replacing old antirevoke preferences
            int selected;
            try {
                selected = sharedPreferences.getInt(tag, 0);
            } catch (ClassCastException e) {
                editor.remove(tag);
                editor.apply();
                selected = sharedPreferences.getInt(tag, 0);
            }

            var radioGroupTitle = (TextView) findViewById(R.id.radio_button_title);
            radioGroupTitle.setText(title);

            var radioGroupSummary = (TextView) findViewById(R.id.radio_button_description);
            if (summary == null) radioGroupSummary.setVisibility(GONE);
            else radioGroupSummary.setText(summary);

            var radioGroup = (RadioGroup) findViewById(R.id.radio_group);
            var radioButton = (MaterialRadioButton) radioGroup.findViewById(R.id.default_button);

            radioGroup.removeView(radioButton);

            for (int i = 0; i < entries.length; i++) {
                var newButton = getNewRadioButton(radioButton);
                var buttonTag = Integer.parseInt(entryValues[i].toString());
                var buttonTitle = entries[i];

                newButton.setId(buttonTag);
                newButton.setText(buttonTitle);
                if (selected == 0 && i == 0) {
                    newButton.setChecked(true);
                } else if (selected == buttonTag) {
                    newButton.setChecked(true);
                }
                radioGroup.addView(newButton);
            }

            radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
//                Toast.makeText(getContext(), "Checked: " + checkedId, Toast.LENGTH_SHORT).show();
                editor.putInt(tag, checkedId).apply();
                if (sharedPreferences.getBoolean("autoreboot", false)) {
                    Intent intent = new Intent(BuildConfig.APPLICATION_ID + ".WHATSAPP.RESTART");
                    radioGroup.getContext().sendBroadcast(intent);
                }
            });
        } finally {
            editor.apply();
            a.recycle();
        }
    }

    private MaterialRadioButton getNewRadioButton(MaterialRadioButton oldRadioButton) {
        var radioButton = new MaterialRadioButton(getContext());
        radioButton.setLayoutParams(oldRadioButton.getLayoutParams());
        return radioButton;
    }
}
