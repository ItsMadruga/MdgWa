package its.madruga.wpp.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import de.hdodenhof.circleimageview.CircleImageView;
import its.madruga.wpp.R;
import its.madruga.wpp.utils.colors.ColorPickerDialog;
import its.madruga.wpp.utils.colors.IColors;

public class ColorPickerButton extends LinearLayout {
    public static String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    public ColorPickerButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ColorPickerButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        inflate(context, R.layout.change_color_button_layout, (ViewGroup) getRootView());

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ColorPickerButton, 0, 0);

        try {
            var tag = attrs.getAttributeValue(ANDROID_NS, "tag");
            var title = a.getText(R.styleable.ColorPickerButton_android_text);
            var summary = a.getText(R.styleable.ColorPickerButton_android_summary);

            var sumView = (TextView) findViewById(R.id.color_argb_text);
            var titleView = (TextView) findViewById(R.id.change_color_title);
            var colorPickerV = (CircleImageView) findViewById(R.id.color_argb_view);

            var shared = context.getSharedPreferences("its.madruga.wpp_preferences", Context.MODE_PRIVATE);
            var colorARGB = shared.getString(tag, "#0");

            if (!colorARGB.equals("#0")) {
                colorPickerV.setImageDrawable(new ColorDrawable(IColors.parseColor(colorARGB)));
            }

            var formatedText = String.format(String.valueOf(summary), colorARGB);
            titleView.setText(title);
            sumView.setText(formatedText);

            findViewById(R.id.container).setOnClickListener(view -> {
                new ColorPickerDialog(context, tag, sumView, summary, colorPickerV);
            });

        } finally {
            a.recycle();
        }
    }
}
