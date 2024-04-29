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

public class XCallPrivacy extends XHookBase {

    private static Object mActivity;
    private Field contactManagerField;
    private Method getContactMethod;

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

        contactManagerField = Unobfuscator.loadContactManagerField(loader);
        getContactMethod = Unobfuscator.loadGetContactInfoMethod(loader);

        XposedBridge.hookAllMethods(XposedHelpers.findClass("com.whatsapp.HomeActivity", loader), "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mActivity = param.thisObject;
            }
        });

    }

    public boolean checkCallBlock(Object callinfo) throws IllegalAccessException, InvocationTargetException {
        var userJid = XposedHelpers.callMethod(callinfo, "getPeerJid");
        var contactManager = contactManagerField.get(mActivity);
        var jid = stripJID((String) XposedHelpers.callMethod(userJid, "getRawString"));
        var contact = getContactMethod.invoke(contactManager, userJid);
        var stringField = Arrays.stream(contact.getClass().getDeclaredFields()).filter(f -> f.getType().equals(String.class)).toArray(Field[]::new);
        var saveName = stringField[3].get(contact);
        logDebug("jid: " + jid + " saveName: " + saveName);
        return saveName == null || saveName.equals(jid);
    }


    @NonNull
    @Override
    public String getPluginName() {
        return "Call Privacy";
    }
}
