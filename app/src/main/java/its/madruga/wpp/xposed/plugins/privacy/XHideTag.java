package its.madruga.wpp.xposed.plugins.privacy;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import its.madruga.wpp.xposed.Unobfuscator;
import its.madruga.wpp.xposed.models.XHookBase;

public class XHideTag extends XHookBase {
    public XHideTag(ClassLoader loader, XSharedPreferences preferences) {
        super(loader, preferences);
    }

    @Override
    public void doHook() throws Exception {
        Method method = Unobfuscator.loadForwardTagMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(method));
        Class<?> forwardClass = Unobfuscator.loadForwardClassMethod(loader);
        logDebug("ForwardClass: " + forwardClass.getName());

        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if(!prefs.getBoolean("hidetag", false))return;
                var arg = (int) param.args[0];
                if (arg == 1) {
                    var stacktrace = Thread.currentThread().getStackTrace();
                    var stackTraceElement = stacktrace[6];
                    if (stackTraceElement != null) {
                        var callerName = stackTraceElement.getClassName();
                        if (callerName.equals(forwardClass.getName())) {
                            param.args[0] = 0;
                        }
                    }
                }
            }
        });
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Hide Tag";
    }
}
