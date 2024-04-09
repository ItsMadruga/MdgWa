package its.madruga.wpp.xposed.plugins.privacy;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;
import java.util.HashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import its.madruga.wpp.xposed.Unobfuscator;
import its.madruga.wpp.xposed.models.XHookBase;

public class XHideView extends XHookBase {

    public XHideView(ClassLoader loader, XSharedPreferences preferences) {
        super(loader, preferences);
    }

    @Override
    public void doHook() throws Exception {

        Method hideViewOpenChatMethod = Unobfuscator.loadHideViewOpenChatMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(hideViewOpenChatMethod));

        XposedBridge.hookMethod(hideViewOpenChatMethod, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (prefs.getBoolean("hideread", false))
                    param.setResult(new HashMap<>());
            }
        });

        Method hideViewInChatMethod = Unobfuscator.loadHideViewInChatMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(hideViewInChatMethod));

        XposedBridge.hookMethod(hideViewInChatMethod, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (prefs.getBoolean("hideread", false))
                    param.setResult(null);
            }
        });



        var methodPlayerViewJid = Unobfuscator.loadHideViewAudioMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(methodPlayerViewJid));
        XposedBridge.hookMethod(methodPlayerViewJid, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (prefs.getBoolean("hideonceseen", false))
                    param.setResult(true);
            }
        });

        var methodHideViewJid = Unobfuscator.loadHideViewJidMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(methodHideViewJid));
        XposedBridge.hookMethod(methodHideViewJid, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (prefs.getBoolean("hidestatusview", false))
                    param.setResult(null);
            }
        });

    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Hide View";
    }

}
