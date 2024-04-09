package its.madruga.wpp.activities.functions;

import android.os.Bundle;

import androidx.annotation.Nullable;

import its.madruga.wpp.R;
import its.madruga.wpp.activities.BaseActivity;

public class FunctionsMediaActivity extends BaseActivity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_functions_media);
        var container = findViewById(R.id.container);
        configureListeners(container);
    }
}
