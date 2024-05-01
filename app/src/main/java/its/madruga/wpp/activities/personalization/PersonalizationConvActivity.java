package its.madruga.wpp.activities.personalization;

import android.content.Intent;
import android.net.Uri;
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

        findViewById(R.id.mdgwa_github_onclick).setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ItsMadruga/MdgWa"))));
        findViewById(R.id.mdgwa_telegram_channel).setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/mdgwamodule"))));
        findViewById(R.id.github_darker_onclick).setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Darker935"))));
        findViewById(R.id.github_dev4mod_onclick).setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Dev4Mod"))));
        findViewById(R.id.github_cvnertnc_onclick).setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/cvnertnc"))));
        findViewById(R.id.madruga_github_onclick).setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ItsMadruga"))));

    }
}
