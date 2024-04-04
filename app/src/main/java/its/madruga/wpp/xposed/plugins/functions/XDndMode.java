package its.madruga.wpp.xposed.plugins.functions;

import static its.madruga.wpp.xposed.plugins.core.XMain.mApp;

import android.content.Context;

import androidx.annotation.NonNull;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import its.madruga.wpp.xposed.Unobfuscator;
import its.madruga.wpp.xposed.models.XHookBase;

public class XDndMode extends XHookBase {
    public XDndMode(ClassLoader loader, XSharedPreferences preferences) {
        super(loader, preferences);
    }

    @Override
    public void doHook() throws Exception {
        var shared = mApp.getSharedPreferences(mApp.getPackageName() + "_mdgwa_preferences", Context.MODE_PRIVATE);
        if (!shared.getBoolean("dndmode", false)) return;
        var dndMethod = Unobfuscator.loadDndModeMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(dndMethod));
        XposedBridge.hookMethod(dndMethod, XC_MethodReplacement.DO_NOTHING);
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Dnd Mode";
    }
}
