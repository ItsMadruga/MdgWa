package its.madruga.wpp.xposed;

import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import java.util.HashSet;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import its.madruga.wpp.BuildConfig;
import its.madruga.wpp.xposed.plugins.core.XDatabases;
import its.madruga.wpp.xposed.plugins.core.XMain;

public class XposedMain implements IXposedHookLoadPackage {
    private static XSharedPreferences pref;

    @NonNull
    public static XSharedPreferences getPref() {
        if (pref == null) {
            pref = new XSharedPreferences(BuildConfig.APPLICATION_ID);
            pref.makeWorldReadable();
            pref.reload();
        }
        return pref;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        var packageName = lpparam.packageName;
        var classLoader = lpparam.classLoader;
        var sourceDir = lpparam.appInfo.sourceDir;
        if (packageName.equals(BuildConfig.APPLICATION_ID)) {
            XposedChecker.setActiveModule(lpparam.classLoader);
        }

        XposedBridge.log("[•] This package: " + lpparam.packageName);
        XposedBridge.log("[•] Loaded packages: " + getPref().getStringSet("whatsapp_packages", new HashSet<>()));
        if (getPref().getStringSet("whatsapp_packages", new HashSet<>()).contains(lpparam.packageName)) {
            XMain.Initialize(classLoader, getPref(), sourceDir);
            XDatabases.Initialize(classLoader, getPref());
        }
        disableSecureFlag();
    }

    public void disableSecureFlag() {
        XposedHelpers.findAndHookMethod(Window.class, "setFlags", int.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.args[0] = (int) param.args[0] & ~WindowManager.LayoutParams.FLAG_SECURE;
                param.args[1] = (int) param.args[1] & ~WindowManager.LayoutParams.FLAG_SECURE;
            }
        });

        XposedHelpers.findAndHookMethod(Window.class, "addFlags", int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.args[0] = (int) param.args[0] & ~WindowManager.LayoutParams.FLAG_SECURE;
                if ((int) param.args[0] == 0) {
                    param.setResult(null);
                }
            }
        });
    } //disable

}
