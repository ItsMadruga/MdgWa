package its.madruga.wpp.xposed.plugins.privacy;

import android.view.View;

import androidx.annotation.NonNull;

import org.json.JSONArray;

import java.util.HashSet;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import its.madruga.wpp.xposed.Unobfuscator;
import its.madruga.wpp.xposed.models.XHookBase;

public class XHideArchive extends XHookBase {

    public static final HashSet<View.OnClickListener> mClickListenerList = new HashSet<>();

    public XHideArchive(@NonNull ClassLoader loader, @NonNull XSharedPreferences preferences) {
        super(loader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("hidearchive", false))
            return;
        var archiveHideViewMethod = Unobfuscator.loadArchiveHideViewMethod(loader);
        for (var method : archiveHideViewMethod) {
            logDebug(Unobfuscator.getMethodDescriptor(method));
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.args[0] = false;
                }
            });
        }
        var onclickCapture = Unobfuscator.loadArchiveOnclickCaptureMethod(loader);
        for (var method : onclickCapture) {
            logDebug(Unobfuscator.getMethodDescriptor(method));
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    mClickListenerList.add((View.OnClickListener) param.args[0]);
                }
            });
        }


    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Hide Archive";
    }
}
