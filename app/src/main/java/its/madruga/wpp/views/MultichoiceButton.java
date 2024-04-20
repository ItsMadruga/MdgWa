package its.madruga.wpp.views;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

import de.hdodenhof.circleimageview.CircleImageView;
import its.madruga.wpp.BuildConfig;
import its.madruga.wpp.R;
import its.madruga.wpp.utils.colors.ColorPickerDialog;
import its.madruga.wpp.utils.colors.IColors;

public class MultichoiceButton extends LinearLayout {
    public static String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    public static final String MADRUGA_WPP_OTHER = "http://schemas.its.madruga.wpp/other";
    private String reboot;

    public MultichoiceButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public MultichoiceButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @SuppressLint("SetTextI18n")
    private void init(Context context, AttributeSet attrs) {
        inflate(context, R.layout.multichoice_button_layout, (ViewGroup) getRootView());

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MultichoiceButton, 0, 0);

        try {
            var tag = attrs.getAttributeValue(ANDROID_NS, "tag");
            reboot = attrs.getAttributeValue(MADRUGA_WPP_OTHER, "reboot");
            var title = a.getText(R.styleable.MultichoiceButton_android_text);
            var summary = a.getText(R.styleable.MultichoiceButton_android_summary);
            var choices = a.getTextArray(R.styleable.MultichoiceButton_android_entries);
            var choicesValues =a.getTextArray(R.styleable.MultichoiceButton_android_entryValues);
            var options = Arrays.stream(choicesValues).map(item-> new MultiChoiceOption((String) item, false)).toArray(MultiChoiceOption[]::new);
            var titleView = ((TextView) findViewById(R.id.multichoice_title));
            titleView.setText(title);
            titleView.setText(titleView.getText() + ("true".equals(reboot) ? " " + context.getString(R.string.reboot_whatsapp) : ""));
            ((TextView) findViewById(R.id.multichoice_content)).setText(summary);
            var shared = context.getSharedPreferences("its.madruga.wpp_preferences", Context.MODE_PRIVATE);
            var choicesStore = shared.getString(tag, null);
            if (choicesStore != null) {
                Arrays.stream(choicesStore.split(",")).forEach(value -> {
                    for (var option : options) {
                        if (option.value.equals(value)) {
                            option.selected = true;
                            break;
                        }
                    }
                });
            }
            findViewById(R.id.container).setOnClickListener(view -> {
                var resultValues = new boolean[choices.length];
                for (int i = 0; i < options.length; i++) {
                    resultValues[i] = options[i].selected;
                }
                new AlertDialog.Builder(context)
                        .setTitle(title)
                        .setMultiChoiceItems(choices, resultValues, (v, which, isChecked) -> resultValues[which] = isChecked)
                        .setPositiveButton("OK", (dialog, which) -> {
                            for (int i = 0; i < resultValues.length; i++) {
                                options[i].selected = resultValues[i];
                            }
                            Log.d("MultichoiceButton", Arrays.toString(resultValues));
                            var editor = shared.edit();
                            var values = Arrays.stream(options).filter(option -> option.selected).map(option -> option.value).collect(Collectors.toCollection(ArrayList::new));
                            if (values.isEmpty()) {
                                editor.putString(tag, null);
                            }
                            editor.putString(tag, String.join(",", values));
                            editor.apply();
                            if ("true".equals(reboot)) {
                                Intent intent = new Intent(BuildConfig.APPLICATION_ID + ".WHATSAPP.RESTART");
                                context.sendBroadcast(intent);
                            }
                        }).setNegativeButton(R.string.cancel, null).show();
            });
        } finally {
            a.recycle();
        }
    }

    private static class MultiChoiceOption {
        public String value;

        public boolean selected;
        public MultiChoiceOption(String value,boolean selected) {
            this.value = value;
            this.selected = selected;
        }
    }

}
