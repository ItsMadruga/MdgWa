package its.madruga.wpp.xposed.plugins.functions;


import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.collection.ArraySet;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
import its.madruga.wpp.xposed.plugins.core.WppCore;
import its.madruga.wpp.xposed.plugins.core.XMain;

public class XBlueTick extends XHookBase {

    private static final ArraySet<String> messages = new ArraySet<>();
    private static Object mWaJobManager;
    private static Field fieldMessageKey;
    private static Class<?> mSendReadClass;
    private static Method WaJobManagerMethod;
    private static String currentJid;

    public XBlueTick(@NonNull ClassLoader loader, @NonNull XSharedPreferences preferences) {
        super(loader, preferences);
    }

    @Override
    public void doHook() throws Throwable {


        var bubbleMethod = Unobfuscator.loadAntiRevokeBubbleMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(bubbleMethod));

        fieldMessageKey = Unobfuscator.loadMessageKeyField(loader);
        logDebug(Unobfuscator.getFieldDescriptor(fieldMessageKey));

        var messageSendClass = XposedHelpers.findClass("com.whatsapp.jobqueue.job.SendE2EMessageJob", loader);


        WaJobManagerMethod = Unobfuscator.loadBlueOnReplayWaJobManagerMethod(loader);

        var messageJobMethod = Unobfuscator.loadBlueOnReplayMessageJobMethod(loader);

        mSendReadClass = XposedHelpers.findClass("com.whatsapp.jobqueue.job.SendReadReceiptJob", loader);


        WppCore.addListenerChat((conv, type) -> {
            var jid = WppCore.getCurrentRawJID();
            if (!Objects.equals(jid, currentJid)) {
                currentJid = jid;
                XposedBridge.log("Changed Start");
                messages.clear();
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
                menuItem.setIcon(Utils.getID("ic_notif_mark_read", "drawable"));
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
                var userJid = XposedHelpers.callMethod(message, userJidMethod.getName());
                var jid = WppCore.getRawString(userJid);
                messages.clear();
                messages.add(messageKey);
                currentJid = jid;
            }
        });

        var viewButtonMethod = Unobfuscator.loadBlueOnReplayViewButtonMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(viewButtonMethod));
        XposedBridge.hookMethod(viewButtonMethod, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!prefs.getBoolean("hidestatusview", false)) return;
                var view = (View) param.getResult();
                var contentView = (LinearLayout) view.findViewById(Utils.getID("bottom_sheet", "id"));
                var infoBar = contentView.findViewById(Utils.getID("info", "id"));
                var buttonImage = new ImageView(XMain.mApp);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int) Utils.dipToPixels(32), (int) Utils.dipToPixels(32));
                params.gravity = Gravity.CENTER_VERTICAL;
                params.setMargins(Utils.dipToPixels(5), Utils.dipToPixels(5), 0, 0);
                buttonImage.setLayoutParams(params);
                buttonImage.setImageResource(Utils.getID("ic_notif_mark_read", "drawable"));
                GradientDrawable border = new GradientDrawable();
                border.setShape(GradientDrawable.RECTANGLE);
                border.setStroke(1, Color.WHITE);
                border.setCornerRadius(20);
                border.setColor(Color.parseColor("#80000000"));
                buttonImage.setBackground(border);
                view.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                    if (infoBar.getVisibility() != View.VISIBLE) {
                        if (contentView.getChildAt(0) == buttonImage) return;
                        contentView.setOrientation(LinearLayout.HORIZONTAL);
                        contentView.addView(buttonImage, 0);
                    }
                });
                buttonImage.setOnClickListener(v -> {
                    new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(XMain.mApp, "Sending read blue tick..", Toast.LENGTH_SHORT).show());
                    sendBlueTickStatus(currentJid);
                });
            }
        });

        /// Add button to send View Once to Target
        var menuMethod = Unobfuscator.loadViewOnceDownloadMenuMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(menuMethod));
        var menuIntField = Unobfuscator.loadViewOnceDownloadMenuField(loader);
        logDebug(Unobfuscator.getFieldDescriptor(menuIntField));
        var classThreadMessage = Unobfuscator.loadThreadMessageClass(loader);

        XposedBridge.hookMethod(menuMethod, new XC_MethodHook() {
            @Override
            @SuppressLint("DiscouragedApi")
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (XposedHelpers.getIntField(param.thisObject, menuIntField.getName()) == 3) {
                    Menu menu = (Menu) param.args[0];
                    MenuItem item = menu.add(0, 0, 0, "View Once").setIcon(Utils.getID("ic_notif_mark_read", "drawable"));
                    item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                    item.setOnMenuItemClickListener(item1 -> {
                        var messageField = Unobfuscator.getFieldByExtendType(menuMethod.getDeclaringClass(), classThreadMessage);
                        var messageObject = XposedHelpers.getObjectField(param.thisObject, messageField.getName());
                        sendBlueTickMedia(messageObject);
                        Toast.makeText(XMain.mApp, "Sending read blue tick..", Toast.LENGTH_SHORT).show();
                        return true;
                    });
                }

            }
        });
    }

    private void sendBlueTickMsg(String currentJid) {
        logDebug("messages: " + Arrays.toString(messages.toArray(new String[0])));
        if (messages.isEmpty() || currentJid == null || currentJid.contains(Utils.getMyNumber()))
            return;
        try {
            logDebug("Blue on Reply: " + currentJid);
            var arr_s = messages.toArray(new String[0]);
            var userJid = WppCore.createUserJid(currentJid);
            var sendJob = XposedHelpers.newInstance(mSendReadClass, userJid, null, null, null, arr_s, -1, 0L, false);
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
            var userJidSender = WppCore.createUserJid("status@broadcast");
            var userJid = WppCore.createUserJid(currentJid);
            var sendJob = XposedHelpers.newInstance(mSendReadClass, userJidSender, userJid, null, null, arr_s, -1, 0L, false);
            WaJobManagerMethod.invoke(mWaJobManager, sendJob);
            messages.clear();
        } catch (Throwable e) {
            XposedBridge.log("Error: " + e.getMessage());
        }
    }

    private void sendBlueTickMedia(Object messageObject) {
        try {
            logDebug("sendBlue: " + WppCore.getCurrentRawJID());
            var sendPlayerClass = XposedHelpers.findClass("com.whatsapp.jobqueue.job.SendPlayedReceiptJob", loader);
            var sendJob = XposedHelpers.newInstance(sendPlayerClass, messageObject);
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
