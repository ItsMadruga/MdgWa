package its.madruga.wpp.xposed;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

public class XposedChecker {
    public static boolean isActive() {
        return false;
    }

    public static void setActiveModule(ClassLoader loader) {
        XposedHelpers.findAndHookMethod("its.madruga.wpp.xposed.XposedChecker", loader, "isActive",XC_MethodReplacement.returnConstant(true));
    }
}
