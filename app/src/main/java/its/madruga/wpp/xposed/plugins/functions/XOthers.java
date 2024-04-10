package its.madruga.wpp.xposed.plugins.functions;

import static de.robv.android.xposed.XposedHelpers.findClass;
import static its.madruga.wpp.xposed.plugins.core.XMain.mApp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.HashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import its.madruga.wpp.xposed.Unobfuscator;
import its.madruga.wpp.xposed.models.XHookBase;

public class XOthers extends XHookBase {

    public static HashMap<Integer, Boolean> props = new HashMap<>();

    public XOthers(ClassLoader loader, XSharedPreferences preferences) {
        super(loader, preferences);
    }

    @Override
    public void doHook() throws Exception {

        var deprecatedMethod = Unobfuscator.loadDeprecatedMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(deprecatedMethod));

        XposedBridge.hookMethod(deprecatedMethod, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Date date = new Date(10554803081056L);
                param.setResult(date);
            }
        });
        var novoTema = prefs != null && prefs.getBoolean("novotema", false);
        var menuWIcons = prefs != null && prefs.getBoolean("menuwicon", false);
        var newSettings = prefs != null && prefs.getBoolean("novaconfig", false);
        var filterChats = prefs != null && prefs.getBoolean("novofiltro", false);
        var strokeButtons = prefs != null && prefs.getBoolean("strokebuttons", false);
        var outlinedIcons = prefs != null && prefs.getBoolean("outlinedicons", false);
        var separateGroups = prefs != null && prefs.getBoolean("separategroups", false);

        props.put(4524, novoTema);
        props.put(4497, menuWIcons);
        props.put(4023, newSettings);
        props.put(5171, filterChats);
        props.put(5834, strokeButtons);
        props.put(5509, outlinedIcons);
        props.put(2358, separateGroups);

        var methodProps = Unobfuscator.loadPropsMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(methodProps));

        XposedBridge.hookMethod(methodProps, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                int i = (int) (param.args.length > 2 ? param.args[2] : param.args[1]);

                var propValue = props.get(i);

                if (propValue != null) {
                    var stacktrace = Thread.currentThread().getStackTrace();
                    var stackTraceElement = stacktrace[6];
                    if (stackTraceElement != null) {
                        if (stackTraceElement.getClassName().equals("com.whatsapp.HomeActivity$TabsPager")) {
                            if (i == 3289) {
                                param.setResult(false);
                            }
                        } else {
                            param.setResult(propValue);
                        }
                    } else {
                        param.setResult(propValue);
                    }
                }
            }
        });

        var homeActivity = findClass("com.whatsapp.HomeActivity", loader);
        XposedHelpers.findAndHookMethod(homeActivity, "onCreateOptionsMenu", Menu.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Menu menu = (Menu) param.args[0];
                Activity home = (Activity) param.thisObject;
                if (!newSettings) {
                    var menuItem = menu.add(0, 0, 0, "Restart WhatsApp").setOnMenuItemClickListener(item -> {
                        Intent intent = mApp.getPackageManager().getLaunchIntentForPackage(mApp.getPackageName());
                        if (mApp != null) {

                            home.finishAffinity();
                            mApp.startActivity(intent);
                        }
                        Runtime.getRuntime().exit(0);
                        return true;
                    });

                    if (menuItem.getActionView() != null) {
                        var root = menuItem.getActionView().getRootView();
                        logDebug("Instance of MenuItem view: " + root.getClass().getName());
                        logDebug("Instance of MenuItem backg: " + root.getBackground().getClass().getName());
                    }
                }
                var shared = mApp.getSharedPreferences(mApp.getPackageName() + "_mdgwa_preferences", Context.MODE_PRIVATE);
                var dndmode = shared.getBoolean("dndmode", false);
                var hidedndicon = prefs.getBoolean("hidedndicon", false);
                var idIconOn = mApp.getResources().getIdentifier("ic_location_nearby", "drawable", mApp.getPackageName());
                var iconDND = mApp.getDrawable(idIconOn);
                if (dndmode) {
                    var idIconOff = mApp.getResources().getIdentifier("ic_location_nearby_disabled", "drawable", mApp.getPackageName());
                    iconDND = mApp.getDrawable(idIconOff);
                }
                var item = menu.add(0, 0, 1, "Dnd Mode " + dndmode);
                if(hidedndicon) {
                    item.setIcon(iconDND);
                    item.setShowAsAction(0);
                }
                item.setOnMenuItemClickListener(menuItem -> {
                    if (!dndmode) {
                        new AlertDialog.Builder(home)
                                .setTitle("DND Mode")
                                .setMessage("When Do Not Disturb mode is on, you won't be able to send or receive messages.")
                                .setPositiveButton("Activate", (dialog, which) -> {
                                    shared.edit().putBoolean("dndmode", !dndmode).apply();
                                    XposedBridge.log(String.valueOf(shared.getBoolean("dndmode", false)));

                                    Intent intent = mApp.getPackageManager().getLaunchIntentForPackage(mApp.getPackageName());
                                    if (mApp != null) {
                                        home.finishAffinity();
                                        mApp.startActivity(intent);
                                    }
                                    Runtime.getRuntime().exit(0);
                                })
                                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                                .create().show();
                        return true;
                    }

                    shared.edit().putBoolean("dndmode", !dndmode).apply();
                    logDebug(String.valueOf(shared.getBoolean("dndmode", false)));

                    Intent intent = mApp.getPackageManager().getLaunchIntentForPackage(mApp.getPackageName());
                    if (mApp != null) {
                        home.finishAffinity();
                        mApp.startActivity(intent);
                    }
                    Runtime.getRuntime().exit(0);

                    return true;
                });
            }
        });
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Others";
    }
}
