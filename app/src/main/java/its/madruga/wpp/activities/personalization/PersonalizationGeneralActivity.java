package its.madruga.wpp.activities.personalization;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import its.madruga.wpp.BuildConfig;
import its.madruga.wpp.R;
import its.madruga.wpp.activities.BaseActivity;

public class PersonalizationGeneralActivity extends BaseActivity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perso_general);
        var container = findViewById(R.id.container);
        configureListeners(container);

        TextInputEditText secondstotime = findViewById(R.id.secondstotime);
        secondstotime.setText(mShared.getString("secondstotime", ""));
        secondstotime.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mEditor.putString("secondstotime", s.toString()).apply();
            }
        });

        findViewById(R.id.reset_preferences).setOnClickListener(v -> new MaterialAlertDialogBuilder(container.getContext()).setTitle(R.string.reset_preferences).setMessage(R.string.reset_preferences_message).setPositiveButton(android.R.string.ok, (dialog, which) -> {
            mEditor.clear().apply();
            dialog.dismiss();
            recreate();
        }).setNegativeButton(android.R.string.cancel, ((dialog, which) -> dialog.dismiss())).create().show());

        findViewById(R.id.restart_whatsapp).setOnClickListener(v -> {
            Intent intent = new Intent(BuildConfig.APPLICATION_ID + ".WHATSAPP.RESTART");
            sendBroadcast(intent);
        });
    }
}
