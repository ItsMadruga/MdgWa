package its.madruga.wpp.xposed.plugins.functions;

import static its.madruga.wpp.xposed.plugins.functions.XAntiRevoke.stripJID;

import android.app.Activity;
import android.os.Message;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import its.madruga.wpp.xposed.Unobfuscator;
import its.madruga.wpp.xposed.models.XHookBase;
import its.madruga.wpp.xposed.plugins.core.WppCore;

public class XCallPrivacy extends XHookBase {
    public XCallPrivacy(@NonNull ClassLoader loader, @NonNull XSharedPreferences preferences) {
        super(loader, preferences);
    }

    @Override
    public void doHook() throws Throwable {

        var onCallReceivedMethod = Unobfuscator.loadAntiRevokeOnCallReceivedMethod(loader);
        var callEndMethod = Unobfuscator.loadAntiRevokeCallEndMethod(loader);
        var callState = Enum.valueOf((Class<Enum>) XposedHelpers.findClass("com.whatsapp.voipcalling.CallState", loader), "ACTIVE");

        XposedBridge.hookMethod(onCallReceivedMethod, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object callinfo = ((Message) param.args[0]).obj;
                logDebug("callinfo: " + callinfo);
                Class<?> callInfoClass = XposedHelpers.findClass("com.whatsapp.voipcalling.CallInfo", loader);
                if (callinfo == null || !callInfoClass.isInstance(callinfo)) return;
                if ((boolean) XposedHelpers.callMethod(callinfo, "isCaller")) return;
                var type = prefs.getInt("call_privacy", 0);
                var block = false;
                switch (type) {
                    case 0:
                        break;
                    case 1:
                        block = true;
                        break;
                    case 2:
                        block = checkCallBlock(callinfo);
                        break;
                }
                if (!block) return;
                XposedHelpers.callMethod(param.thisObject, callEndMethod.getName(), callState, callinfo);
                XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.whatsapp.voipcalling.Voip", loader), "endCall", true);
                param.setResult(false);
            }
        });


    }

    public boolean checkCallBlock(Object callinfo) throws IllegalAccessException, InvocationTargetException {
        var userJid = XposedHelpers.callMethod(callinfo, "getPeerJid");
        var jid = WppCore.stripJID(WppCore.getRawString(userJid));
        var contactName = WppCore.getContactName(userJid);
        return contactName == null || contactName.equals(jid);
    }


    @NonNull
    @Override
    public String getPluginName() {
        return "Call Privacy";
    }
}
