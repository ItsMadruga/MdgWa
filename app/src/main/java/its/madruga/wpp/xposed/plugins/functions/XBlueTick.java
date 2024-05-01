package its.madruga.wpp.xposed.plugins.functions;

import static its.madruga.wpp.xposed.plugins.functions.XAntiRevoke.getRawString;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.collection.ArraySet;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import its.madruga.wpp.xposed.Unobfuscator;
import its.madruga.wpp.xposed.models.XHookBase;
import its.madruga.wpp.xposed.plugins.core.Utils;
import its.madruga.wpp.xposed.plugins.core.XMain;

public class XBlueTick extends XHookBase {

    private static final ArraySet<String> messages = new ArraySet<>();
    private static Object mWaJobManager;
    private static Field fieldMessageKey;
    private static Class<?> mGenJidClass;
    private static Method mGenJidMethod;
    private static Class<?> mSendReadClass;
    private static Method WaJobManagerMethod;
    private static String currentJid;

    public XBlueTick(@NonNull ClassLoader loader, @NonNull XSharedPreferences preferences) {
        super(loader, preferences);
    }

    @Override
    public void doHook() throws Throwable {

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
                var jid = getRawString(chatJidObj);
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
                var jid = getRawString(chatJidObj);
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
                if (XposedHelpers.getBooleanField(fieldMessageDetails, "A02")) return;
                messages.add(messageKey);
            }
        });

        XposedBridge.hookMethod(messageJobMethod, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!messageSendClass.isInstance(param.thisObject)) return;
                if (!prefs.getBoolean("blueonreply", false)) return;
                new Handler(Looper.getMainLooper()).post(() -> sendBlueTickMsg((String) XposedHelpers.getObjectField(messageSendClass.cast(param.thisObject), "jid")));
            }
        });

        XposedBridge.hookAllConstructors(WaJobManagerMethod.getDeclaringClass(), new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mWaJobManager = param.thisObject;
            }
        });


        var onCreateMenuConversationMethod = Unobfuscator.loadBlueOnReplayCreateMenuConversationMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(onCreateMenuConversationMethod));
        XposedBridge.hookMethod(onCreateMenuConversationMethod, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!prefs.getBoolean("hideread", false) || prefs.getBoolean("hidereceipt", false))
                    return;
                var menu = (Menu) param.args[0];
                var menuItem = menu.add(0, 0, 0, "Read Tick");
                menuItem.setShowAsAction(2);
                @SuppressLint({"UseCompatLoadingForDrawables", "DiscouragedApi"})
                var drawable = XMain.mApp.getDrawable(XMain.mApp.getResources().getIdentifier("ic_notif_mark_read", "drawable", XMain.mApp.getPackageName()));
                menuItem.setIcon(drawable);
                menuItem.setOnMenuItemClickListener(item -> {
                    new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(XMain.mApp, "Sending read blue tick..", Toast.LENGTH_SHORT).show());
                    sendBlueTickMsg(currentJid);
                    return true;
                });
            }
        });

        var setPageActiveMethod = Unobfuscator.loadStatusActivePage(loader);
        logDebug(Unobfuscator.getMethodDescriptor(setPageActiveMethod));
        var fieldList = Unobfuscator.getFieldByType(setPageActiveMethod.getDeclaringClass(), List.class);
        XposedBridge.hookMethod(setPageActiveMethod, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                var position = (int) param.args[1];
                var list = (List<?>) XposedHelpers.getObjectField(param.args[0], fieldList.getName());
                var message = list.get(position);
                var messageKeyObject = XposedHelpers.getObjectField(message, fieldMessageKey.getName());
                var messageKey = (String) XposedHelpers.getObjectField(messageKeyObject, "A01");
                var userJidClass = XposedHelpers.findClass("com.whatsapp.jid.UserJid", loader);
                var userJidMethod = Arrays.stream(fieldMessageKey.getDeclaringClass().getDeclaredMethods()).filter(m -> m.getReturnType().equals(userJidClass)).findFirst().orElse(null);
                var userJid =  XposedHelpers.callMethod(message, userJidMethod.getName());
                var jid = getRawString(userJid);
                messages.clear();
                messages.add(messageKey);
                currentJid = jid;
            }
        });

        var viewButtonMethod = Unobfuscator.loadBlueOnReplayViewButtonMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(viewButtonMethod));
        XposedBridge.hookMethod(viewButtonMethod, new XC_MethodHook() {
            @Override
            @SuppressLint("DiscouragedApi")
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!prefs.getBoolean("hidestatusview", false)) return;
                var view = (View) param.getResult();
                var id1 = XMain.mApp.getResources().getIdentifier("bottom_sheet", "id", XMain.mApp.getPackageName());
                var contentView = (LinearLayout) view.findViewById(id1);
                @SuppressLint("UseCompatLoadingForDrawables")
                var drawable = XMain.mApp.getDrawable(XMain.mApp.getResources().getIdentifier("ic_notif_mark_read", "drawable", XMain.mApp.getPackageName()));
                var buttonImage = new ImageView(XMain.mApp);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int) Utils.dipToPixels(32), (int) Utils.dipToPixels(32));
                params.gravity = Gravity.END;
                params.setMargins(0, 0, (int) Utils.dipToPixels(8), 0);
                buttonImage.setLayoutParams(params);
                buttonImage.setImageDrawable(drawable);
                GradientDrawable border = new GradientDrawable();
                border.setShape(GradientDrawable.RECTANGLE);
                border.setStroke(2, Color.WHITE);
                border.setCornerRadius(20);
                border.setColor(Color.parseColor("#80000000"));
                buttonImage.setBackground(border);
                contentView.addView(buttonImage,0);
                contentView.setPadding(0, contentView.getPaddingTop()- (int) Utils.dipToPixels(32),0, 0);
                buttonImage.setOnClickListener(v -> {
                    new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(XMain.mApp, "Sending read blue tick..", Toast.LENGTH_SHORT).show());
                    sendBlueTickStatus(currentJid);
                });
            }
        });
    }

    private void sendBlueTickMsg(String currentJid) {
        logDebug("messages: " + Arrays.toString(messages.toArray(new String[0])));
        if (messages.isEmpty() || currentJid == null || currentJid.contains(Utils.getMyNumber())) return;
        try {
            logDebug("Blue on Reply: " + currentJid);
            var arr_s = messages.toArray(new String[0]);
            var genInstance = XposedHelpers.newInstance(mGenJidClass);
            var gen = XposedHelpers.callMethod(genInstance, mGenJidMethod.getName(), currentJid);
            var sendJob = XposedHelpers.newInstance(mSendReadClass, gen, null, null, null, arr_s, -1, 0L, false);
            WaJobManagerMethod.invoke(mWaJobManager, sendJob);
            messages.clear();
        } catch (Throwable e) {
            XposedBridge.log("Error: " + e.getMessage());
        }
    }

    private void sendBlueTickStatus(String currentJid) {
        logDebug("messages: " + Arrays.toString(messages.toArray(new String[0])));
        if (messages.isEmpty() || currentJid == null || currentJid.equals("status_me")) return;
        try {
            logDebug("sendBlue: " + currentJid);
            var arr_s = messages.toArray(new String[0]);
            var genInstance = XposedHelpers.newInstance(mGenJidClass);
            var gen = XposedHelpers.callMethod(genInstance, mGenJidMethod.getName(), "status@broadcast");
            var gen2 = XposedHelpers.callMethod(genInstance, mGenJidMethod.getName(), currentJid);
            var sendJob = XposedHelpers.newInstance(mSendReadClass, gen, gen2, null, null, arr_s, -1, 0L, false);
            WaJobManagerMethod.invoke(mWaJobManager, sendJob);
            messages.clear();
        } catch (Throwable e) {
            XposedBridge.log("Error: " + e.getMessage());
        }
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Blue Tick";
    }


}
