package its.madruga.wpp.xposed.plugins.privacy;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import its.madruga.wpp.xposed.Unobfuscator;
import its.madruga.wpp.xposed.models.XHookBase;

public class XHideView extends XHookBase {

    public XHideView(ClassLoader loader, XSharedPreferences preferences) {
        super(loader, preferences);
    }

    @Override
    public void doHook() throws Exception {

        var hideread = prefs.getBoolean("hideread", false);
        var hidereadstatus = prefs.getBoolean("hidestatusview", false);

        if (!hideread && !hidereadstatus) return;

        if (hideread) {
            Method methodHideViewCollection = Unobfuscator.loadHideViewCollectionMethod(loader);
            logDebug(Unobfuscator.getMethodDescriptor(methodHideViewCollection));
            XposedBridge.hookMethod(methodHideViewCollection, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(new HashMap<>());
                }
            });

//            var receiptMethod = Unobfuscator.loadReceiptMethod(loader);
//            logDebug(Unobfuscator.getMethodDescriptor(receiptMethod));
//            XposedBridge.hookMethod(receiptMethod, new XC_MethodHook() {
//                @Override
//                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                    var p4 = param.args[4];
//                    if (p4 != null && p4.equals("read")) {
//                        param.args[4] = null;
//                    }
//                }
//            });

        }

        if (hidereadstatus) {
            var methodHideViewJid = Unobfuscator.loadHideViewJidMethod(loader);
            logDebug(Unobfuscator.getMethodDescriptor(methodHideViewJid));
            XposedBridge.hookMethod(methodHideViewJid, XC_MethodReplacement.returnConstant(null));

//            var methodHideViewJid = Unobfuscator.loadHideViewJidMethod2(loader);
//            logDebug(Unobfuscator.getMethodDescriptor(methodHideViewJid));
//            XposedBridge.hookMethod(methodHideViewJid, new XC_MethodHook() {
//                @Override
//                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                    var messageObj = param.args[0];
//                    logDebug("Read Status: " + messageObj);
//                    Class<?> deviceJidClass = XposedHelpers.findClass("com.whatsapp.jid.DeviceJid", loader);
//                    var fDeviceJid = Arrays.stream(messageObj.getClass().getFields()).filter(m -> m.getType().equals(deviceJidClass)).findFirst().orElse(null);
//                    fDeviceJid.setAccessible(true);
//                    fDeviceJid.set(messageObj, null);
//                }
//            });
        }
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Hide View";
    }

}
