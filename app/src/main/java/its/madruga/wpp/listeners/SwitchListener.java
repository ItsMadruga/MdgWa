package its.madruga.wpp.listeners;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.CompoundButton;

import its.madruga.wpp.BuildConfig;
import its.madruga.wpp.R;
import its.madruga.wpp.views.AbstractSwitchButton;
import its.madruga.wpp.views.SwitchButton;

public class SwitchListener implements CompoundButton.OnCheckedChangeListener {
    private final SharedPreferences.Editor mEditor;
    private final SharedPreferences mShared;

    public boolean checkReboot = true;

    public SwitchListener(SharedPreferences mShared, SharedPreferences.Editor mEditor) {
        this.mEditor = mEditor;
        this.mShared = mShared;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        var switchButton = (AbstractSwitchButton) buttonView.getTag();
        if (switchButton.getId() == R.id.hidereceipt) {
            SwitchButton view = buttonView.getRootView().findViewById(R.id.blueonreply);
            SwitchButton view2 = buttonView.getRootView().findViewById(R.id.hideread);
            view.setEnabled(!isChecked && view2.isChecked());
            if (isChecked) {
                view.setChecked(false);
            }
        }else if (switchButton.getId() == R.id.hideread) {
            SwitchButton view = buttonView.getRootView().findViewById(R.id.blueonreply);
            SwitchButton view2 = buttonView.getRootView().findViewById(R.id.hidereceipt);
            view.setEnabled(isChecked && !view2.isChecked());
        }

        if (switchButton.isRebootEnabled() && mShared.getBoolean("autoreboot", false) && checkReboot) {
            Intent intent = new Intent(BuildConfig.APPLICATION_ID + ".WHATSAPP.RESTART");
            buttonView.getContext().sendBroadcast(intent);
        }
        mEditor.putBoolean((String) switchButton.getTag(), isChecked).apply();
    }
}