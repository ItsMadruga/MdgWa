package its.madruga.wpp.activities.personalization;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import its.madruga.wpp.R;
import its.madruga.wpp.activities.BaseActivity;
import its.madruga.wpp.activities.functions.FunctionsMediaActivity;
import its.madruga.wpp.activities.functions.FunctionsPrivacyActivity;

public class PersonalizationActivity extends BaseActivity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perso);
        var container = findViewById(R.id.container);
        configureListeners(container);
    }
}
