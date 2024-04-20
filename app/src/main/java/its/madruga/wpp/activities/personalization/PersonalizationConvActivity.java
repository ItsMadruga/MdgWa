package its.madruga.wpp.activities.personalization;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputEditText;

import its.madruga.wpp.R;
import its.madruga.wpp.activities.BaseActivity;

public class PersonalizationConvActivity extends BaseActivity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perso_conv);
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
    }
}
