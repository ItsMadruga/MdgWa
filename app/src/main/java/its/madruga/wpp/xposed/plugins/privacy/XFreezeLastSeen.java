package its.madruga.wpp.xposed.plugins.privacy;

import androidx.annotation.NonNull;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import its.madruga.wpp.xposed.Unobfuscator;
import its.madruga.wpp.xposed.models.XHookBase;

public class XFreezeLastSeen extends XHookBase {
    public XFreezeLastSeen(ClassLoader loader, XSharedPreferences preferences) {
        super(loader, preferences);
    }

    @Override
    public void doHook() {
            var method = Unobfuscator.loadFreezeSeenMethod(loader);
            logDebug(Unobfuscator.getMethodDescriptor(method));
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (prefs.getBoolean("freezelastseen", false))
                        param.setResult(null);
                }
            });
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Freeze Last Seen";
    }


}
