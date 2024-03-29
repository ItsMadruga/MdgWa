package its.madruga.wpp.xposed.plugins.privacy;

import static its.madruga.wpp.ClassesReference.HideForward.classMessageInfo;
import static its.madruga.wpp.ClassesReference.HideForward.methodSetForward;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import its.madruga.wpp.ClassesReference;
import its.madruga.wpp.xposed.models.XHookBase;

public class XHideTag extends XHookBase {
    public XHideTag(ClassLoader loader, XSharedPreferences preferences) {
        super(loader, preferences);
    }

    @Override
    public void doHook() {
        var hidetag = prefs.getBoolean("hidetag", false);
        if (!hidetag)
            return;

        XposedHelpers.findAndHookMethod(classMessageInfo, loader, methodSetForward, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                var arg = (int) param.args[0];
                if (arg == 1) {
                    var stacktrace = Thread.currentThread().getStackTrace();
                    var stackTraceElement = stacktrace[6];
                    if (stackTraceElement != null) {
                        var callerName = stackTraceElement.getClassName();
                        if (callerName.equals(ClassesReference.HideForward.rightCallerClass)) {
                            param.args[0] = 0;
                        }
                    }
                }
                super.beforeHookedMethod(param);
            }
        });
    }
}
