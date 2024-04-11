package its.madruga.wpp.xposed;

import androidx.annotation.NonNull;

import java.util.HashSet;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.lsposed.disableflagsecure.XDisableFlagSecure;
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
            XMain.Initialize(classLoader, getPref(),sourceDir);
            XDatabases.Initialize(classLoader, getPref());
        }

        XDisableFlagSecure.doHook(lpparam);
    }

}
