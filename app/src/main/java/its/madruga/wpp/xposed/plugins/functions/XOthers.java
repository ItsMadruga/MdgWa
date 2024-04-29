package its.madruga.wpp.xposed.plugins.functions;

import static de.robv.android.xposed.XposedHelpers.findClass;
import static its.madruga.wpp.xposed.plugins.core.XMain.mApp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import its.madruga.wpp.xposed.Unobfuscator;
import its.madruga.wpp.xposed.models.XHookBase;
import its.madruga.wpp.xposed.plugins.core.DesignUtils;
import its.madruga.wpp.xposed.plugins.core.ResId;
import its.madruga.wpp.xposed.plugins.core.Utils;

public class XOthers extends XHookBase {

    public static HashMap<Integer, Boolean> props = new HashMap<>();

    public XOthers(ClassLoader loader, XSharedPreferences preferences) {
        super(loader, preferences);
    }

    @Override
    public void doHook() throws Exception {

//        var deprecatedMethod = Unobfuscator.loadDeprecatedMethod(loader);
//        logDebug(Unobfuscator.getMethodDescriptor(deprecatedMethod));
//
//        XposedBridge.hookMethod(deprecatedMethod, new XC_MethodHook() {
//            @Override
//            protected void beforeHookedMethod(MethodHookParam param) {
//                Date date = new Date(10554803081056L);
//                param.setResult(date);
//            }
//        });
        var novoTema = prefs != null && prefs.getBoolean("novotema", false);
        var menuWIcons = prefs != null && prefs.getBoolean("menuwicon", false);
        var newSettings = prefs != null && prefs.getBoolean("novaconfig", false);
        var filterChats = prefs != null && prefs.getBoolean("novofiltro", false);
        var strokeButtons = prefs != null && prefs.getBoolean("strokebuttons", false);
        var outlinedIcons = prefs != null && prefs.getBoolean("outlinedicons", false);
        var showDnd = prefs != null && prefs.getBoolean("show_dndmode", false);
        var removechannelRec = prefs != null && prefs.getBoolean("removechannel_rec", false);

        props.put(4524, novoTema);
        props.put(4497, menuWIcons);
        props.put(4023, newSettings);
        props.put(5171, filterChats);
        props.put(5834, strokeButtons);
        props.put(5509, outlinedIcons);
        props.put(2358, false);


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
                var iconDraw = home.getDrawable(home.getResources().getIdentifier("vec_account_switcher", "drawable", home.getPackageName()));
                iconDraw.setTint(0xff8696a0);
                menu.add(0, 0, 0, ResId.string.restart_whatsapp).setIcon(iconDraw).setOnMenuItemClickListener(item -> {
                    restartApp(home);
                    return true;
                });
                if (showDnd) {
                    InsertDNDOption(menu, home);
                } else {
                    mApp.getSharedPreferences(mApp.getPackageName() + "_mdgwa_preferences", Context.MODE_PRIVATE).edit().putBoolean("dndmode", false).commit();
                }
            }
        });

        if (removechannelRec) {
            var removeChannelRecClass = Unobfuscator.loadRemoveChannelRecClass(loader);
            XposedBridge.hookAllConstructors(removeChannelRecClass, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args.length > 0 && param.args[0] instanceof List list) {
                        if (list.isEmpty()) return;
                        list.clear();
                    }
                }
            });
        }
    }

    private static void restartApp(Activity home) {
        Intent intent = mApp.getPackageManager().getLaunchIntentForPackage(mApp.getPackageName());
        if (mApp != null) {
            home.finishAffinity();
            mApp.startActivity(intent);
        }
        Runtime.getRuntime().exit(0);
    }

    @SuppressLint({"DiscouragedApi", "UseCompatLoadingForDrawables"})
    private static void InsertDNDOption(Menu menu, Activity home) {
        var shared = mApp.getSharedPreferences(mApp.getPackageName() + "_mdgwa_preferences", Context.MODE_PRIVATE);
        var dndmode = shared.getBoolean("dndmode", false);
        Drawable iconDraw;
        if (dndmode) {
            iconDraw = mApp.getDrawable(mApp.getResources().getIdentifier("ic_location_nearby_disabled", "drawable", mApp.getPackageName()));
        } else {
            iconDraw = mApp.getDrawable(mApp.getResources().getIdentifier("ic_location_nearby", "drawable", mApp.getPackageName()));
        }
        var item = menu.add(0, 0, 1, "Dnd Mode " + dndmode);
        item.setIcon(iconDraw);
        item.setShowAsAction(2);
        item.setOnMenuItemClickListener(menuItem -> {
            if (!dndmode) {
                new AlertDialog.Builder(home)
                        .setTitle(ResId.string.dnd_mode_title)
                        .setMessage(ResId.string.dnd_message)
                        .setPositiveButton(ResId.string.activate, (dialog, which) -> {
                            shared.edit().putBoolean("dndmode", true).commit();
                            XposedBridge.log(String.valueOf(shared.getBoolean("dndmode", false)));
                            restartApp(home);
                        })
                        .setNegativeButton(ResId.string.cancel, (dialog, which) -> dialog.dismiss())
                        .create().show();
                return true;
            }
            shared.edit().putBoolean("dndmode", false).commit();
            restartApp(home);
            return true;
        });
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Others";
    }
}
