package its.madruga.wpp.xposed.plugins.functions;

import static its.madruga.wpp.ClassesReference.AntiRevoke.bubbleViewClass;
import static its.madruga.wpp.ClassesReference.AntiRevoke.bubbleViewMethod;
import static its.madruga.wpp.ClassesReference.AntiRevoke.classRevokeMessage;
import static its.madruga.wpp.ClassesReference.AntiRevoke.fieldMessageKey;
import static its.madruga.wpp.ClassesReference.AntiRevoke.iconId;
import static its.madruga.wpp.ClassesReference.AntiRevoke.methodRevokeMessage;
import static its.madruga.wpp.ClassesReference.AntiRevoke.onResume;
import static its.madruga.wpp.ClassesReference.AntiRevoke.onStart;
import static its.madruga.wpp.ClassesReference.AntiRevoke.stringId;
import static its.madruga.wpp.ClassesReference.AntiRevoke.unknownStatusPlaybackMethod;
import static its.madruga.wpp.xposed.plugins.core.XMain.mApp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.AsyncTask;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import its.madruga.wpp.ClassesReference;
import its.madruga.wpp.xposed.models.XHookBase;

public class XAntiRevoke extends XHookBase {

    private static HashSet<String> messageRevokedList = new HashSet<>();
    @SuppressLint("StaticFieldLeak")
    private static Activity mConversation;
    private static SharedPreferences mShared;

    public XAntiRevoke(ClassLoader loader, XSharedPreferences preferences) {
        super(loader, preferences);
    }

    private void isMRevoked(Object objMessage, TextView dateTextView, String antirevokeType) {
        if (dateTextView == null) return;
        var fieldMessageDetails = XposedHelpers.getObjectField(objMessage, fieldMessageKey);
        var messageKey = (String) XposedHelpers.getObjectField(fieldMessageDetails, "A01");
        var stripJID = stripJID(getJidAuthor(objMessage));
        if (messageRevokedList.isEmpty()) {
            String[] currentRevokedMessages = getRevokedMessages(objMessage);
            if (currentRevokedMessages == null) currentRevokedMessages = new String[]{""};
            Collections.addAll(messageRevokedList, currentRevokedMessages);
        }
        if (messageRevokedList != null && messageRevokedList.contains(messageKey)) {
            var antirevokeValue = prefs.getInt(antirevokeType, 0);
            if (antirevokeValue == 1) {
                // Text
                var newTextData = mApp.getString(stringId) + " | " + dateTextView.getText();
                dateTextView.setText(newTextData);
            } else if (antirevokeValue == 2) {
                // Icon
                var drawable = mApp.getDrawable(iconId);
                drawable.setColorFilter(new PorterDuffColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP));
                dateTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null);
                dateTextView.setCompoundDrawablePadding(5);
            }
        } else {
            dateTextView.setCompoundDrawables(null, null, null, null);
            var revokeNotice = mApp.getString(stringId) + " | ";
            var dateText = dateTextView.getText().toString();
            if (dateText.contains(revokeNotice)) {
                dateTextView.setText(dateText.replace(revokeNotice, ""));
            }
        }
    }

    @Override
    public void doHook() {
        mShared = mApp.getSharedPreferences(mApp.getPackageName() + "_mdgwa_preferences", Context.MODE_PRIVATE);
        var antirevoke = prefs.getInt("antirevoke", 0);
        var antirevokestatus = prefs != null ? prefs.getInt("antirevokestatus", 0) : 0;

//        Toast.makeText(mContext, "AR: " + antirevoke + " / ARS: " + antirevokestatus, Toast.LENGTH_SHORT).show();
        var classMessage = XposedHelpers.findClass(ClassesReference.AntiRevoke.classMessage, loader);
        var classThreadMessage = XposedHelpers.findClass(ClassesReference.AntiRevoke.threadMessage, loader);
        try {
            XposedHelpers.findAndHookMethod("com.whatsapp.Conversation", loader, onResume, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    mConversation = (Activity) param.thisObject;
                    var chatField = XposedHelpers.getObjectField(mConversation, ClassesReference.AntiRevoke.convChatField);
                    var chatJidField = XposedHelpers.getObjectField(chatField, ClassesReference.AntiRevoke.chatJidField);
                    setCurrentJid(stripJID(getRawString(chatJidField)));
                }
            });
            XposedHelpers.findAndHookMethod("com.whatsapp.Conversation", loader, onStart, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    mConversation = (Activity) param.thisObject;
                    var chatField = XposedHelpers.getObjectField(mConversation, ClassesReference.AntiRevoke.convChatField);
                    var chatJidField = XposedHelpers.getObjectField(chatField, ClassesReference.AntiRevoke.chatJidField);
                    setCurrentJid(stripJID(getRawString(chatJidField)));
                }
            });
            if (antirevoke != 0 || antirevokestatus != 0) {
                XposedHelpers.findAndHookMethod(classRevokeMessage, loader, methodRevokeMessage, classMessage, boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        var objMessage = classThreadMessage.cast(param.args[0]);
                        var fieldMessageDetails = XposedHelpers.getObjectField(objMessage, fieldMessageKey);
                        var fieldIsFromMe = XposedHelpers.getBooleanField(fieldMessageDetails, "A02");
                        if (!fieldIsFromMe) {
                            if (antiRevoke(objMessage) != 0) param.setResult(true);
                        }
                    }
                });
            }
            XposedHelpers.findAndHookMethod(bubbleViewClass, loader, bubbleViewMethod, ViewGroup.class, TextView.class, classMessage, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    var objMessage = param.args[2];
                    var dateTextView = (TextView) param.args[1];
                    isMRevoked(objMessage, dateTextView, "antirevoke");
                }
            });
            var statusPlaybackClass = XposedHelpers.findClass("com.whatsapp.status.playback.fragment.StatusPlaybackContactFragment", loader);
            XposedHelpers.findAndHookMethod(statusPlaybackClass, unknownStatusPlaybackMethod, classMessage, statusPlaybackClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    var obj = param.args[1];
                    var objMessage = param.args[0];
                    var fragment = XposedHelpers.findClass("com.whatsapp.status.playback.fragment.StatusPlaybackBaseFragment", loader);
                    var tv = (TextView) XposedHelpers.getObjectField(XposedHelpers.findField(fragment, "A04").get(obj), "A0D");
                    isMRevoked(objMessage, tv, "antirevokestatus");
                }
            });
        } catch (Exception e) {
            XposedBridge.log(e.getMessage());
        }

    }

    private static void saveRevokedMessage(String authorJid, String messageKey, Object objMessage) {
        String newRevokedMessages;
        String[] revokedMessagesArray = getRevokedMessages(objMessage);
        if (revokedMessagesArray != null) {
            HashSet<String> newRevokedMessagesArray = new HashSet<>();
            Collections.addAll(newRevokedMessagesArray, revokedMessagesArray);
            newRevokedMessagesArray.add(messageKey);
            messageRevokedList = newRevokedMessagesArray;
            newRevokedMessages = Arrays.toString(newRevokedMessagesArray.toArray());
        } else {
            newRevokedMessages = "[" + messageKey + "]";
            messageRevokedList = new HashSet<>(Collections.singleton(messageKey));
        }
        mShared.edit().putString(authorJid + "_revoked", newRevokedMessages).apply();
    }

    private int antiRevoke(Object objMessage) {
        var messageKey = (String) XposedHelpers.getObjectField(objMessage, "A01");
        var stripJID = stripJID(getJidAuthor(objMessage));
        var revokeboolean = stripJID.equals("status") ? prefs.getInt("antirevokestatus", 0) : prefs.getInt("antirevoke", 0);
        if (revokeboolean == 0) return revokeboolean;
        if (!messageRevokedList.contains(messageKey)) {
            try {
                AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
                    saveRevokedMessage(stripJID, messageKey, objMessage);
                    try {
                        if (mConversation != null && getCurrentJid().equals(stripJID)) {
                            if (mConversation.hasWindowFocus()) {
                                mConversation.startActivity(mConversation.getIntent());
                                mConversation.overridePendingTransition(0, 0);
                            } else {
                                mConversation.recreate();
                            }
                        }
                    } catch (Exception e) {
                        XposedBridge.log(e.getMessage());
                    }
                });
            } catch (Exception e) {
                XposedBridge.log(e.getMessage());
            }
        }
        return revokeboolean;
    }

    private static String[] getRevokedMessages(Object objMessage) {
        String stripJID = stripJID(getJidAuthor(objMessage));
        try {
            String revokedsString = mShared.getString(stripJID + "_revoked", "");
            if (revokedsString.isEmpty()) {
                return null;
            } else return StringToStringArray(revokedsString);
        } catch (Exception e) {
            XposedBridge.log(e.getMessage());
            return null;
        }
    }

    private static String stripJID(String str) {
        try {
            return (str.contains("@g.us") || str.contains("@s.whatsapp.net") || str.contains("@broadcast")) ? str.substring(0, str.indexOf("@")) : str;
        } catch (Exception e) {
            XposedBridge.log(e.getMessage());
            return str;
        }
    }

    private static String getJidAuthor(Object objMessage) {
        Object fieldMessageDetails = XposedHelpers.getObjectField(objMessage, fieldMessageKey);
        Object fieldMessageAuthorJid = XposedHelpers.getObjectField(fieldMessageDetails, "A00");
        if (fieldMessageAuthorJid == null) return "";
        else return getRawString(fieldMessageAuthorJid);
    }

    private static String getRawString(Object objJid) {
        if (objJid == null) return "";
        else return (String) XposedHelpers.callMethod(objJid, "getRawString");
    }

    private static void setCurrentJid(String jid) {
        if (jid == null || mShared == null) return;
        mShared.edit().putString("jid", jid).apply();
    }

    private static String getCurrentJid() {
        if (mShared == null) return "";
        else return mShared.getString("jid", "");
    }

    private static String[] StringToStringArray(String str) {
        try {
            return str.substring(1, str.length() - 1).replaceAll("\\s", "").split(",");
        } catch (Exception unused) {
            return null;
        }
    }

}
