package its.madruga.wpp.xposed.plugins.functions;

import androidx.annotation.NonNull;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import its.madruga.wpp.xposed.Unobfuscator;
import its.madruga.wpp.xposed.models.XHookBase;

public class XChatLimit extends XHookBase {
    public XChatLimit(@NonNull ClassLoader loader, @NonNull XSharedPreferences preferences) {
        super(loader, preferences);
    }

    @Override
    public void doHook() throws Throwable {

        var chatLimitDeleteMethod = Unobfuscator.loadChatLimitDeleteMethod(loader);
        var chatLimitDelete2Method = Unobfuscator.loadChatLimitDelete2Method(loader);
        XposedBridge.hookMethod(chatLimitDeleteMethod, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                var stacktrace = Thread.currentThread().getStackTrace();
                for (StackTraceElement stackTraceElement : stacktrace) {
                    var className = stackTraceElement.getClassName();
                    var methodName = stackTraceElement.getMethodName();
                    if (Objects.equals(className, chatLimitDelete2Method.getDeclaringClass().getName()) && Objects.equals(methodName, chatLimitDelete2Method.getName())) {
                        if (prefs.getBoolean("revokeallmessages", false))
                            param.setResult(0L);
                    }
                }

            }
        });

        var seeMoreMethod = Unobfuscator.loadSeeMoreMethod(loader);
        XposedBridge.hookMethod(seeMoreMethod, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!prefs.getBoolean("removeseemore", false))return;
                param.args[0] = 0;
            }
        });
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Chat Limit";
    }
}
