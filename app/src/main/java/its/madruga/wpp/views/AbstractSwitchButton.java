package its.madruga.wpp.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.materialswitch.MaterialSwitch;

import its.madruga.wpp.R;

public abstract class AbstractSwitchButton extends LinearLayout {

    public static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    public static final String MADRUGA_WPP_OTHER = "http://schemas.its.madruga.wpp/other";
    private String reboot;

    public AbstractSwitchButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AbstractSwitchButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressLint("SetTextI18n")
    protected void init(Context context, AttributeSet attrs) {
        var tag = attrs.getAttributeValue(ANDROID_NS, "tag");
        var switchButton = (MaterialSwitch) findViewById(R.id.switch_button);
        reboot = attrs.getAttributeValue(MADRUGA_WPP_OTHER, "reboot");
        switchButton.setText(switchButton.getText() + ("true".equals(reboot) ? " (reboot)" : ""));
        this.setTag(tag);
        switchButton.setTag(this);
    }

    public void setChecked(boolean checked) {
        ((MaterialSwitch) findViewById(R.id.switch_button)).setChecked(checked);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        ((MaterialSwitch) findViewById(R.id.switch_button)).setEnabled(enabled);
    }

    public boolean isRebootEnabled() {
        return "true".equals(reboot);
    }

}
