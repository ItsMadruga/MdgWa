package its.madruga.wpp.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.google.android.material.textview.MaterialTextView;

import its.madruga.wpp.R;

public class TextViewButton extends LinearLayout {

    public static String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    public TextViewButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public TextViewButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        inflate(context, R.layout.textview_button_layout, (ViewGroup) getRootView());

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.TextViewButton,
                0, 0
        );

        try {
            var title = a.getText(R.styleable.TextViewButton_android_text);
            var summary = a.getText(R.styleable.TextViewButton_android_summary);
            var icon = a.getDrawable(R.styleable.TextViewButton_android_icon);
            var loadClass = a.getString(R.styleable.TextViewButton_loadClass);
            setTag(loadClass);

            var titleView = (MaterialTextView) findViewById(R.id.title);
            titleView.setText(title);

            var summaryView = (MaterialTextView) findViewById(R.id.summary);
            if (summary != null) {
                summaryView.setText(summary);
            } else summaryView.setVisibility(GONE);

            var iconView = (AppCompatImageView) findViewById(R.id.icon);
            if (icon != null) {
                iconView.setImageDrawable(icon);
            } else iconView.setVisibility(GONE);


        } finally {
            a.recycle();
        }
    }

}
