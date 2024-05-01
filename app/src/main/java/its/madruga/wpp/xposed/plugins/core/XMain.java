package its.madruga.wpp.xposed.plugins.core;

import static its.madruga.wpp.BuildConfig.DEBUG;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import its.madruga.wpp.BuildConfig;
import its.madruga.wpp.xposed.Unobfuscator;
import its.madruga.wpp.xposed.UnobfuscatorCache;
import its.madruga.wpp.xposed.models.XHookBase;
import its.madruga.wpp.xposed.plugins.functions.XAntiEditMessage;
import its.madruga.wpp.xposed.plugins.functions.XAntiRevoke;
import its.madruga.wpp.xposed.plugins.functions.XBlueTick;
import its.madruga.wpp.xposed.plugins.functions.XCallPrivacy;
import its.madruga.wpp.xposed.plugins.functions.XChatLimit;
import its.madruga.wpp.xposed.plugins.functions.XDndMode;
import its.madruga.wpp.xposed.plugins.functions.XMediaQuality;
import its.madruga.wpp.xposed.plugins.functions.XNewChat;
import its.madruga.wpp.xposed.plugins.functions.XOthers;
import its.madruga.wpp.xposed.plugins.functions.XPinnedLimit;
import its.madruga.wpp.xposed.plugins.functions.XShareLimit;
import its.madruga.wpp.xposed.plugins.functions.XStatusDownload;
import its.madruga.wpp.xposed.plugins.functions.XViewOnce;
import its.madruga.wpp.xposed.plugins.personalization.XBioAndName;
import its.madruga.wpp.xposed.plugins.personalization.XBubbleColors;
import its.madruga.wpp.xposed.plugins.personalization.XChangeColors;
import its.madruga.wpp.xposed.plugins.personalization.XChatsFilter;
import its.madruga.wpp.xposed.plugins.personalization.XShowOnline;
import its.madruga.wpp.xposed.plugins.personalization.XSecondsToTime;
import its.madruga.wpp.xposed.plugins.privacy.XFreezeLastSeen;
import its.madruga.wpp.xposed.plugins.privacy.XGhostMode;
import its.madruga.wpp.xposed.plugins.privacy.XHideArchive;
import its.madruga.wpp.xposed.plugins.privacy.XHideReceipt;
import its.madruga.wpp.xposed.plugins.privacy.XHideTag;
import its.madruga.wpp.xposed.plugins.privacy.XHideView;

public class XMain {
    public static Application mApp;
    public static ArrayList<String> list = new ArrayList<>();

    public static void Initialize(@NonNull ClassLoader loader, @NonNull XSharedPreferences pref, String sourceDir) {

        if (!Unobfuscator.initDexKit(sourceDir)) {
            XposedBridge.log("Can't init dexkit");
            return;
        }
        XposedHelpers.findAndHookMethod(Instrumentation.class, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
            @SuppressWarnings("deprecation")
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                mApp = (Application) param.args[0];
                new UnobfuscatorCache(mApp,pref);
                XDatabases.Initialize(loader, pref);
                PackageManager packageManager = mApp.getPackageManager();
                pref.registerOnSharedPreferenceChangeListener((sharedPreferences, s) -> pref.reload());
                PackageInfo packageInfo = packageManager.getPackageInfo(mApp.getPackageName(), 0);
                XposedBridge.log(packageInfo.versionName);
                plugins(loader, pref);
                registerReceivers();
                if (DEBUG)
                    XposedHelpers.setStaticIntField(XposedHelpers.findClass("com.whatsapp.util.Log", loader), "level", 5);
            }
        });

        XposedHelpers.findAndHookMethod("com.whatsapp.HomeActivity", loader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (!list.isEmpty()) {
                    new AlertDialog.Builder((Activity) param.thisObject)
                            .setTitle("Error detected")
                            .setMessage("The following options aren't working:\n\n" + String.join("\n", list.toArray(new String[0])))
                            .show();
                }
            }
        });
    }

    private static void registerReceivers() {
        BroadcastReceiver restartReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(context, "Rebooting " + context.getPackageManager().getApplicationLabel(context.getApplicationInfo()) + "...", Toast.LENGTH_SHORT).show();
                new Handler(Looper.getMainLooper()).postDelayed(() -> Utils.doRestart(context), 1000);
            }
        };
        var intentRestart = new IntentFilter(BuildConfig.APPLICATION_ID + ".WHATSAPP.RESTART");
        ContextCompat.registerReceiver(mApp, restartReceiver, intentRestart, ContextCompat.RECEIVER_EXPORTED);
    }

    private static void plugins(@NonNull ClassLoader loader, @NonNull XSharedPreferences pref) {

        var classes = new Class<?>[]{
                XAntiEditMessage.class,
                XAntiRevoke.class,
                XBioAndName.class,
                XBlueTick.class,
                XBubbleColors.class,
                XCallPrivacy.class,
                XChangeColors.class,
                XChatLimit.class,
                XChatsFilter.class,
                XShowOnline.class,
                XDndMode.class,
                XFreezeLastSeen.class,
                XGhostMode.class,
                XHideArchive.class,
                XHideReceipt.class,
                XHideTag.class,
                XHideView.class,
                XMediaQuality.class,
                XNewChat.class,
                XOthers.class,
                XPinnedLimit.class,
                XSecondsToTime.class,
                XShareLimit.class,
                XStatusDownload.class,
                XViewOnce.class,
        };

        for (var classe : classes) {
            try {
                var constructor = classe.getConstructor(ClassLoader.class, XSharedPreferences.class);
                var plugin = (XHookBase) constructor.newInstance(loader, pref);
                plugin.doHook();
            } catch (Throwable e) {
                XposedBridge.log(e);
                list.add(classe.getSimpleName());
            }
        }
    }


}
