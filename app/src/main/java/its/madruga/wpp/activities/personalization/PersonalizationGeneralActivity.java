package its.madruga.wpp.activities.personalization;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputEditText;

import its.madruga.wpp.R;
import its.madruga.wpp.activities.BaseActivity;

public class PersonalizationGeneralActivity extends BaseActivity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perso_general);
        var container = findViewById(R.id.container);
        configureListeners(container);
    }
}
