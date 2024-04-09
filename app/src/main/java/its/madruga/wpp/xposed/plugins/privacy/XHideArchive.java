package its.madruga.wpp.xposed.plugins.privacy;

import android.view.View;

import androidx.annotation.NonNull;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import its.madruga.wpp.xposed.Unobfuscator;
import its.madruga.wpp.xposed.models.XHookBase;

public class XHideArchive extends XHookBase {

    public static View.OnClickListener mOnClickListener;

    public XHideArchive(@NonNull ClassLoader loader, @NonNull XSharedPreferences preferences) {
        super(loader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("hidearchive", false))
            return;
        var archiveHideViewMethod = Unobfuscator.loadArchiveHideViewMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(archiveHideViewMethod));
        XposedBridge.hookMethod(archiveHideViewMethod, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.args[0] = false;
            }
        });
        var onclickCapture = Unobfuscator.loadArchiveOnclickCaptureMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(onclickCapture));
        XposedBridge.hookMethod(onclickCapture, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                mOnClickListener = (View.OnClickListener) param.args[0];
            }
        });
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Hide Archive";
    }
}
