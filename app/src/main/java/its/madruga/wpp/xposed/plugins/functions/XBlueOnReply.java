package its.madruga.wpp.xposed.plugins.functions;

import static its.madruga.wpp.xposed.plugins.functions.XAntiRevoke.getRawString;
import static its.madruga.wpp.xposed.plugins.functions.XAntiRevoke.stripJID;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.collection.ArraySet;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import its.madruga.wpp.xposed.Unobfuscator;
import its.madruga.wpp.xposed.models.XHookBase;
import its.madruga.wpp.xposed.plugins.core.XMain;
import its.madruga.wpp.xposed.plugins.privacy.XHideView;

public class XBlueOnReply extends XHookBase {

    private static final ArraySet<String> messages = new ArraySet<>();
    private static Object mWaJobManager;
    private static Field fieldMessageKey;
    private static Class<?> mGenJidClass;
    private static Method mGenJidMethod;
    private static Class<?> mSendReadClass;
    private static Method WaJobManagerMethod;
    private static String currentJid;
    private SharedPreferences mPrefs;


    public XBlueOnReply(@NonNull ClassLoader loader, @NonNull XSharedPreferences preferences) {
        super(loader, preferences);
    }

    @Override
    public void doHook() throws Throwable {

        mPrefs = XMain.mApp.getSharedPreferences("blueonreply", Context.MODE_PRIVATE);

        var onStartMethod = Unobfuscator.loadAntiRevokeOnStartMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(onStartMethod));

        var onResumeMethod = Unobfuscator.loadAntiRevokeOnResumeMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(onResumeMethod));

        var convChatField = Unobfuscator.loadAntiRevokeConvChatField(loader);
        logDebug(Unobfuscator.getFieldDescriptor(convChatField));

        var chatJidField = Unobfuscator.loadAntiRevokeChatJidField(loader);
        logDebug(Unobfuscator.getFieldDescriptor(chatJidField));

        var bubbleMethod = Unobfuscator.loadAntiRevokeBubbleMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(bubbleMethod));

        fieldMessageKey = Unobfuscator.loadAntiRevokeMessageKeyField(loader);
        logDebug(Unobfuscator.getFieldDescriptor(fieldMessageKey));

        var messageSendClass = XposedHelpers.findClass("com.whatsapp.jobqueue.job.SendE2EMessageJob", loader);

        WaJobManagerMethod = Unobfuscator.loadBlueOnReplayWaJobManagerMethod(loader);

        var messageJobMethod = Unobfuscator.loadBlueOnReplayMessageJobMethod(loader);

        mSendReadClass = XposedHelpers.findClass("com.whatsapp.jobqueue.job.SendReadReceiptJob", loader);
        var subClass = Arrays.stream(mSendReadClass.getConstructors()).filter(c -> c.getParameterTypes().length == 8).findFirst().orElse(null).getParameterTypes()[0];
        mGenJidClass = Arrays.stream(subClass.getFields()).filter(field -> Modifier.isStatic(field.getModifiers())).findFirst().orElse(null).getType();
        mGenJidMethod = Arrays.stream(mGenJidClass.getMethods()).filter(m -> m.getParameterCount() == 1 && !Modifier.isStatic(m.getModifiers())).findFirst().orElse(null);

        XposedBridge.hookMethod(onStartMethod, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                var chatField = XposedHelpers.getObjectField(param.thisObject, convChatField.getName());
                var chatJidObj = XposedHelpers.getObjectField(chatField, chatJidField.getName());
                var jid = stripJID(getRawString(chatJidObj));
                if (!Objects.equals(jid, currentJid)) {
                    currentJid = jid;
                    XposedBridge.log("Changed Start");
                    messages.clear();
                }
            }
        });

        XposedBridge.hookMethod(onResumeMethod, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                var chatField = XposedHelpers.getObjectField(param.thisObject, convChatField.getName());
                var chatJidObj = XposedHelpers.getObjectField(chatField, chatJidField.getName());
                var jid = stripJID(getRawString(chatJidObj));
                if (!Objects.equals(jid, currentJid)) {
                    currentJid = jid;
                    messages.clear();
                }
            }
        });

        XposedBridge.hookMethod(bubbleMethod, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                var objMessage = param.args[2];
                var fieldMessageDetails = XposedHelpers.getObjectField(objMessage, fieldMessageKey.getName());
                var messageKey = (String) XposedHelpers.getObjectField(fieldMessageDetails, "A01");
                if (XposedHelpers.getBooleanField(fieldMessageDetails,"A02"))return;
                messages.add(messageKey);
            }
        });

        XposedBridge.hookMethod(messageJobMethod, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!messageSendClass.isInstance(param.thisObject)) return;
                if (!prefs.getBoolean("blueonreply", false)) return;
                new Handler(Looper.getMainLooper()).post(() -> sendBlue((String) XposedHelpers.getObjectField(messageSendClass.cast(param.thisObject), "jid")));
            }
        });

        XposedBridge.hookAllConstructors(WaJobManagerMethod.getDeclaringClass(), new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mWaJobManager = param.thisObject;
            }
        });
    }

    private void sendBlue(String currentJid){
        logDebug("messages: " + Arrays.toString(messages.toArray(new String[0])));
        if (messages.isEmpty()) return;
        try {
            logDebug("Blue on Reply: " + currentJid);
            var arr_s = messages.toArray(new String[0]);
            var genInstance = XposedHelpers.newInstance(mGenJidClass);
            var gen = XposedHelpers.callMethod(genInstance, mGenJidMethod.getName(), currentJid);
            var sendJob = XposedHelpers.newInstance(mSendReadClass, gen, null, null, null, arr_s, -1, 0L, false);
            WaJobManagerMethod.invoke(mWaJobManager, sendJob);
            messages.clear();
        }catch (Exception e) {
            XposedBridge.log("Error: " + e.getMessage());
        }
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Blue on Reply";
    }
}
