package its.madruga.wpp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import its.madruga.wpp.activities.BaseActivity;
import its.madruga.wpp.adapters.AppListAdapter;
import its.madruga.wpp.models.AppInfoModel;
import its.madruga.wpp.xposed.XposedChecker;

public class MainActivity extends BaseActivity {

    public final static String TAG = "Debug-Main";
    public List<PackageInfo> wppPkgs = new ArrayList<>();
    public HashSet<String> wppPkgNames = new HashSet<>();


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setWppPkgs();

        if (!XposedChecker.isActive()) {
            setContentViewError();
        } else {
            setContentViewMain();
        }
    }

    private void setContentViewError() {
        setContentView(R.layout.no_active_module);
        findViewById(R.id.lsposed_download).setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mywalkb/LSPosed_mod"))));
        findViewById(R.id.lspatch_download).setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/LSPosed/LSPatch"))));
    }

    private void setContentViewMain() {
        setContentView(R.layout.activity_main);

        mShared = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        mEditor = mShared.edit();
        mEditor.putStringSet("whatsapp_packages", wppPkgNames).apply();

        var container = (ViewGroup) findViewById(R.id.container);
        var img = (ImageView) findViewById(R.id.whatsapp_version_status_img);
        var text = (TextView) findViewById(R.id.module_status_text);

        var workingText = getResources().getString(R.string.module_working_text);
        text.setText(String.format(workingText, "LSPosed"));
        img.setImageResource(R.drawable.lsposed_icon);

        findViewById(R.id.reset_preferences).setOnClickListener(v -> new MaterialAlertDialogBuilder(container.getContext()).setTitle(R.string.reset_preferences).setMessage(R.string.reset_preferences_message).setPositiveButton(android.R.string.ok, (dialog, which) -> {
            mEditor.clear().apply();
            dialog.dismiss();
            recreate();
        }).setNegativeButton(android.R.string.cancel, ((dialog, which) -> dialog.dismiss())).create().show());

        findViewById(R.id.restart_whatsapp).setOnClickListener(v -> {
            Intent intent = new Intent(BuildConfig.APPLICATION_ID + ".WHATSAPP.RESTART");
            sendBroadcast(intent);
        });

        configureListeners(container);
        setWhatsappVersionMessage();

        findViewById(R.id.mdgwa_github_onclick).setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ItsMadruga/MdgWa"))));
        findViewById(R.id.mdgwa_telegram_channel).setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/mdgwamodule"))));
        findViewById(R.id.github_darker_onclick).setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Darker935"))));
        findViewById(R.id.github_dev4mod_onclick).setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Dev4Mod"))));
        findViewById(R.id.madruga_github_onclick).setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ItsMadruga"))));

        var listView = (RecyclerView) findViewById(R.id.installed_app_list);
        var showApps = findViewById(R.id.show_supported_installed_apps);

        var dataModels = new ArrayList<AppInfoModel>();
        var pm = getPackageManager();
        for (var appInfo : wppPkgs) {
            dataModels.add(new AppInfoModel(appInfo.packageName, appInfo.versionName, appInfo.applicationInfo.loadIcon(pm)));
        }
        var adapter = new AppListAdapter(dataModels, getApplicationContext());
        listView.setLayoutManager(new LinearLayoutManager(this));
        listView.setAdapter(adapter);

        showApps.setOnClickListener(v -> {
            if (listView.getVisibility() == View.GONE) {
                Log.i(TAG, "setContentViewMain: showing");
                Animation animation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
                listView.startAnimation(animation);
                listView.setVisibility(View.VISIBLE);
            } else {
                Log.i(TAG, "setContentViewMain: hidding");
                listView.setVisibility(View.GONE);
            }
        });

        try {
            var pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            mEditor.putLong("lastUpdateTime", pInfo.lastUpdateTime).commit();
        } catch (Exception ignored) {
        }
    }

    private void setWppPkgs() {
        var pm = getPackageManager();
        var pkgs = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS);
        for (var pkg : pkgs) {
            if (pkg.permissions != null) {
                for (var permission : pkg.permissions) {
                    if (permission.name.equals("com.whatsapp.sticker.READ")) {
                        wppPkgs.add(pkg);
                        wppPkgNames.add(pkg.packageName);
                    }
                }
            }
        }
    }

    private void setWhatsappVersionMessage() {
        var msg = (MaterialTextView) findViewById(R.id.show_supported_installed_apps);
        var required = (MaterialTextView) findViewById(R.id.module_status_text);
        var requiredText = required.getText().toString();
        var installed = (MaterialTextView) findViewById(R.id.module_status_sum);
        var installedText = installed.getText().toString();
        var resources = getResources();
        var moduleInfo = BuildConfig.VERSION_NAME;
        required.setText(String.format(requiredText, moduleInfo));
        try {
            getPackageManager().getPackageInfo("com.whatsapp", PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {

            try {
                getPackageManager().getPackageInfo("com.whatsapp.w4b", PackageManager.GET_META_DATA);
            } catch (PackageManager.NameNotFoundException ex) {
                installed.setText(String.format(installedText, "--------"));
                installed.setTextColor(resources.getColor(R.color.default_red, getTheme()));
                if (msg.getVisibility() == View.GONE) {
                    msg.setVisibility(View.VISIBLE);
                }
                msg.setText(R.string.whatsapp_not_installed);
            }
        }
    }
}