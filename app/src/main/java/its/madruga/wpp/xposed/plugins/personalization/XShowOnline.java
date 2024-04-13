package its.madruga.wpp.xposed.plugins.personalization;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import java.util.HashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import its.madruga.wpp.xposed.Unobfuscator;
import its.madruga.wpp.xposed.models.XHookBase;
import its.madruga.wpp.xposed.plugins.core.XMain;

public class XShowOnline extends XHookBase {

    public static HashMap<Object, View> views = new HashMap<>();
    private Object mStatusUser;
    private Object mInstancePresence;

    public XShowOnline(@NonNull ClassLoader loader, @NonNull XSharedPreferences preferences) {
        super(loader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("dotonline", false)) return;


        var classViewHolder = XposedHelpers.findClass("com.whatsapp.conversationslist.ViewHolder", loader);
        XposedBridge.hookAllConstructors(classViewHolder, new XC_MethodHook() {
            @SuppressLint("ResourceType")
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                var view = (View) param.args[1];
                var context = (Context) param.args[0];
                views.remove(param.thisObject);
                views.put(param.thisObject, view);
                var bottomLayout = (LinearLayout) view.findViewById(XMain.mApp.getResources().getIdentifier("bottom_row", "id", XMain.mApp.getPackageName()));
                var imageView = new ImageView(context);
                imageView.setId(0x7FFF0001);
                imageView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));
                imageView.setForegroundGravity(Gravity.CENTER_VERTICAL);
                ShapeDrawable shapeDrawable = new ShapeDrawable(new OvalShape());
                shapeDrawable.getPaint().setColor(Color.GREEN);
                // Definir o tamanho do círculo
                shapeDrawable.setIntrinsicHeight(20);
                shapeDrawable.setIntrinsicWidth(20);
                imageView.setImageDrawable(shapeDrawable);
                bottomLayout.addView(imageView);
            }
        });

        var onChangeStatus = Unobfuscator.loadOnChangeStatus(loader);
        logDebug(Unobfuscator.getMethodDescriptor(onChangeStatus));
        var field1 = Unobfuscator.loadViewHolderField1(loader);
        logDebug(Unobfuscator.getFieldDescriptor(field1));
        var getStatusUser = Unobfuscator.loadGetStatusUserMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(getStatusUser));
        var sendPresenceMethod = Unobfuscator.loadSendPresenceMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(sendPresenceMethod));


        XposedBridge.hookAllConstructors(getStatusUser.getDeclaringClass(), new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mStatusUser = param.thisObject;
            }
        });

        XposedBridge.hookAllConstructors(sendPresenceMethod.getDeclaringClass(), new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mInstancePresence = param.thisObject;
            }
        });

        XposedBridge.hookMethod(onChangeStatus, new XC_MethodHook() {
            @Override
            @SuppressLint("ResourceType")
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                var viewHolder = field1.get(field1.getDeclaringClass().cast(param.thisObject));
                var object = param.args[0];
                var view = (View) views.get(viewHolder);
                var csDot = (ImageView) view.findViewById(0x7FFF0001);
                csDot.setVisibility(View.INVISIBLE);
                var jidClass = XposedHelpers.findClass("com.whatsapp.jid.Jid", loader);
                var jidFiled = Unobfuscator.getFieldByExtendType(object.getClass(), jidClass);
                var jidObject = jidFiled.get(object);
                var jid = (String) XposedHelpers.callMethod(jidObject, "getRawString");
                if (jid.contains("@g.us")) return;
                var clazz = sendPresenceMethod.getParameterTypes()[1];
                var instance = XposedHelpers.newInstance(clazz, new Object[]{null, null});
                sendPresenceMethod.invoke(null, jidObject, instance, mInstancePresence);
                var status = (String) getStatusUser.invoke(mStatusUser, object);
                if (TextUtils.isEmpty(status) || status.contains(" ")) return;{
                    csDot.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Conversation";
    }
}
