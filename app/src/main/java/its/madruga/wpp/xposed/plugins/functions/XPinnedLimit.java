package its.madruga.wpp.xposed.plugins.functions;

import android.annotation.SuppressLint;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import its.madruga.wpp.xposed.Unobfuscator;
import its.madruga.wpp.xposed.models.XHookBase;
import its.madruga.wpp.xposed.plugins.core.XMain;

public class XPinnedLimit extends XHookBase {

    public XPinnedLimit(@NonNull ClassLoader loader, @NonNull XSharedPreferences preferences) {
        super(loader, preferences);
    }

    @Override
    @SuppressLint("DiscouragedApi")
    public void doHook() throws Throwable {
        var pinnedLimitMethod = Unobfuscator.loadPinnedLimitMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(pinnedLimitMethod));
        var pinnedLimit2Method = Unobfuscator.loadPinnedLimit2Method(loader);
        logDebug(Unobfuscator.getMethodDescriptor(pinnedLimit2Method));
        var pinnedSetMethod = Unobfuscator.loadPinnedHashSetMethod(loader);

        var idPin = XMain.mApp.getResources().getIdentifier("menuitem_conversations_pin", "id", XMain.mApp.getPackageName());
        var idSelectAll = XMain.mApp.getResources().getIdentifier("menuitem_conversations_select_all", "id", XMain.mApp.getPackageName());


        XposedBridge.hookMethod(pinnedLimitMethod, new XC_MethodHook() {
            private Unhook hooked;

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!prefs.getBoolean("pinnedlimit", false)) return;
                if (param.args.length > 0 && param.args[0] instanceof MenuItem menuItem) {
                    logDebug("menuItem.getItemId() = " + menuItem.getItemId());
                    if (menuItem.getItemId() != idPin && menuItem.getItemId() != idSelectAll)
                        return;
                    hooked = XposedHelpers.findAndHookMethod(HashSet.class, "size", XC_MethodReplacement.returnConstant(-57));
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (hooked != null) hooked.unhook();
            }
        });

        XposedBridge.hookMethod(pinnedLimit2Method, new XC_MethodHook() {
            private LinkedHashSet fakeHash;
            private Set realHash;
            private Unhook hooked;

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!prefs.getBoolean("pinnedlimit", false)) return;
                var field = Unobfuscator.getFieldByType(param.thisObject.getClass(), pinnedSetMethod.getDeclaringClass());
                realHash = (Set) pinnedSetMethod.invoke(field.get(param.thisObject));
                fakeHash = new LinkedHashSet<>();
                logDebug(pinnedSetMethod.getReturnType());
                hooked = XposedBridge.hookMethod(pinnedSetMethod, XC_MethodReplacement.returnConstant(fakeHash));
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (hooked != null) hooked.unhook();
                if (fakeHash != null && !fakeHash.isEmpty()) realHash.addAll(fakeHash);
                fakeHash.clear();
                fakeHash = null;
                realHash = null;
            }
        });

    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Pinned Limit";
    }
}
