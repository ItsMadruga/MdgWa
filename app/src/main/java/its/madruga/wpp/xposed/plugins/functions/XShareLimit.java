package its.madruga.wpp.xposed.plugins.functions;

import androidx.annotation.NonNull;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import its.madruga.wpp.xposed.Unobfuscator;
import its.madruga.wpp.xposed.models.XHookBase;

public class XShareLimit extends XHookBase {
    public XShareLimit(ClassLoader loader, XSharedPreferences preferences) {
        super(loader, preferences);
    }

    public void doHook() throws Exception {
        var shareLimitMethod = Unobfuscator.loadShareLimitMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(shareLimitMethod));
        var shareLimitField = Unobfuscator.loadShareLimitField(loader);
        logDebug(Unobfuscator.getFieldDescriptor(shareLimitField));

        XposedBridge.hookMethod(
                shareLimitMethod,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (prefs.getBoolean("removeforwardlimit", false)) {
                            XposedHelpers.setBooleanField(param.thisObject, shareLimitField.getName(), true);
                        }
                        super.beforeHookedMethod(param);
                    }
                });
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Share Limit";
    }
}
