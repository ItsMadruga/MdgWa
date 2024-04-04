package its.madruga.wpp.xposed.plugins.privacy;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import its.madruga.wpp.xposed.Unobfuscator;
import its.madruga.wpp.xposed.models.XHookBase;

public class XGhostMode extends XHookBase {

    public XGhostMode(ClassLoader loader, XSharedPreferences preferences) {
        super(loader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        var ghostmode_t = prefs.getBoolean("ghostmode_t", false);
        var ghostmode_r = prefs.getBoolean("ghostmode_r", false);
        Method method = Unobfuscator.loadGhostModeMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(method));
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                var p1 = (int) param.args[2];
                if ((p1 == 1 && ghostmode_r) || (p1 == 0 && ghostmode_t)) {
                    param.setResult(null);
                }
            }
        });
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Ghost Mode";
    }
}
