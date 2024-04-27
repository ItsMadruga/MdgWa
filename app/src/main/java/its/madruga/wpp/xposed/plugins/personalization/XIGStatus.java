package its.madruga.wpp.xposed.plugins.personalization;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ListView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import its.madruga.wpp.adapters.IGStatusAdapter;
import its.madruga.wpp.views.IGStatusView;
import its.madruga.wpp.xposed.Unobfuscator;
import its.madruga.wpp.xposed.models.XHookBase;
import its.madruga.wpp.xposed.plugins.core.Utils;
import its.madruga.wpp.xposed.plugins.core.WppCore;
import its.madruga.wpp.xposed.plugins.core.XMain;

public class XIGStatus extends XHookBase {
    public static ArrayList<Object> itens = new ArrayList<>();
    @SuppressLint("StaticFieldLeak")
    private static IGStatusView mStatusContainer;

    public XIGStatus(@NonNull ClassLoader loader, @NonNull XSharedPreferences preferences) {
        super(loader, preferences);
    }

    @Override
    public void doHook() throws Throwable {

        if (!prefs.getBoolean("igstatus", false) || Utils.getApplication().getPackageName().equals("com.whatsapp.w4b")) return;

        var clazz = XposedHelpers.findClass("com.whatsapp.HomeActivity", loader);

        XposedHelpers.findAndHookMethod(clazz.getSuperclass(), "onCreate", android.os.Bundle.class, new XC_MethodHook() {
            @Override
            @SuppressLint("DiscouragedApi")
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                var homeActivity = (Activity) param.thisObject;
                // create status container
                mStatusContainer = new IGStatusView(homeActivity);
                var layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, Utils.dipToPixels(105));
                layoutParams.gravity = Gravity.TOP;
                layoutParams.topMargin = prefs.getInt("chatfilter", 0) == 2 ? Utils.dipToPixels(112) : Utils.dipToPixels(56);
                mStatusContainer.setLayoutParams(layoutParams);
                mStatusContainer.setBackgroundColor(Color.TRANSPARENT);
                var mainContainer = homeActivity.findViewById(Utils.getID("main_container", "id"));
                var pagerView = (ViewGroup) mainContainer.findViewById(Utils.getID("pager", "id"));
                var pager_holder = (ViewGroup) pagerView.getParent();
                pager_holder.addView(mStatusContainer);

            }
        });

        // fix scroll
        var onScrollPagerMethod = Unobfuscator.loadScrollPagerMethod(loader);

        XposedBridge.hookMethod(onScrollPagerMethod, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                var scroll = -(float) XposedHelpers.getIntField(WppCore.getMainActivity(), "A02");
                if (mStatusContainer.isShown())
                    mStatusContainer.setTranslationY(scroll);
            }
        });

        var getViewConversationMethod = Unobfuscator.loadGetViewConversationMethod(loader);
        XposedBridge.hookMethod(getViewConversationMethod, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (XposedHelpers.findClass("com.whatsapp.conversationslist.ArchivedConversationsFragment", loader).isInstance(param.thisObject))
                    return;
                var view = (ViewGroup) param.getResult();
                if (view == null) return;
                @SuppressLint("ResourceType")
                var mainView = (ListView) view.findViewById(0x0102000a);
                mainView.setNestedScrollingEnabled(true);
                var paddingView = new View(WppCore.getMainActivity());
                paddingView.setClickable(true);
                var layoutParams = new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, Utils.dipToPixels(105));
                paddingView.setLayoutParams(layoutParams);
                mainView.addHeaderView(paddingView);
            }
        });

        // hide on tab

        var onMenuItemSelected = Unobfuscator.loadOnMenuItemSelected(loader);
        var separateGroups = prefs.getBoolean("separategroups", false);
        var onMenuItemClick = Unobfuscator.loadOnMenuItemClickClass(loader);

        XposedBridge.hookMethod(onMenuItemSelected, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!Unobfuscator.isCalledFromClass(clazz) && !Unobfuscator.isCalledFromClass(onMenuItemClick)) return;
                var index = (int) param.args[0];
                WppCore.getMainActivity().runOnUiThread(() -> {
                    XposedHelpers.setObjectField(WppCore.getMainActivity(), "A02", 0);
                    var visible = View.GONE;
                    if (index == 0 || (index == 1 && separateGroups)) {
                        visible = View.VISIBLE;
                    }
                    if (mStatusContainer.getVisibility() != visible)
                        mStatusContainer.setVisibility(visible);
                    if (visible == View.VISIBLE) mStatusContainer.setTranslationY(0);
                });
            }
        });


        var clazz2 = XposedHelpers.findClass("com.whatsapp.updates.viewmodels.UpdatesViewModel", loader);
        var onUpdateStatusChanged = Unobfuscator.loadOnUpdateStatusChanged(loader);
        logDebug(Unobfuscator.getMethodDescriptor(onUpdateStatusChanged));
        var statusInfoClass = Unobfuscator.loadStatusInfoClass(loader);
        logDebug(statusInfoClass);

        XposedBridge.hookAllConstructors(clazz2, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                itens.add(0, null);
                IGStatusAdapter mStatusAdapter = new IGStatusAdapter(WppCore.getMainActivity(), statusInfoClass);
                mStatusContainer.setAdapter(mStatusAdapter);
                mStatusContainer.updateList();
            }
        });

        var onStatusListUpdatesClass = Unobfuscator.loadStatusListUpdatesClass(loader);
        logDebug(onStatusListUpdatesClass);

        XposedBridge.hookAllConstructors(onStatusListUpdatesClass, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                var list1 = (List) param.args[2];
                var list2 = (List) param.args[3];
                itens.clear();
                itens.add(0, null);
                itens.addAll(list1);
                itens.addAll(list2);
                mStatusContainer.updateList();
            }
        });


        var onGetInvokeField = Unobfuscator.loadGetInvokeField(loader);
        logDebug(Unobfuscator.getFieldDescriptor(onGetInvokeField));
        XposedBridge.hookMethod(onUpdateStatusChanged, new XC_MethodHook() {
            private Unhook unhook;

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                var object = onGetInvokeField.get(param.args[0]);
                var StatusListUpdates = XposedHelpers.callMethod(object, "A04");
                if (StatusListUpdates == null) return;
                var lists = Arrays.stream(StatusListUpdates.getClass().getDeclaredFields()).filter(f -> f.getType().equals(List.class)).collect(Collectors.toList());
                if (lists.size() < 3) return;
                var list1 = (List) lists.get(1).get(StatusListUpdates);
                var list2 = (List) lists.get(2).get(StatusListUpdates);
                itens.clear();
                itens.add(0, null);
                itens.addAll(list1);
                itens.addAll(list2);
                mStatusContainer.updateList();
            }
        });
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "IGStatus";
    }
}
