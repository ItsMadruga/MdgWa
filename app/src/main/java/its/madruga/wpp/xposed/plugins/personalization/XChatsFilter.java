package its.madruga.wpp.xposed.plugins.personalization;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static its.madruga.wpp.ClassesReference.ChatsFilter.classGetTab;
import static its.madruga.wpp.ClassesReference.ChatsFilter.classTabName;
import static its.madruga.wpp.ClassesReference.ChatsFilter.classTabsList;
import static its.madruga.wpp.ClassesReference.ChatsFilter.fieldTabsList;
import static its.madruga.wpp.ClassesReference.ChatsFilter.methodGetTab;
import static its.madruga.wpp.ClassesReference.ChatsFilter.methodTabIcon;
import static its.madruga.wpp.ClassesReference.ChatsFilter.methodTabInstance;
import static its.madruga.wpp.ClassesReference.ChatsFilter.methodTabName;
import static its.madruga.wpp.ClassesReference.ChatsFilter.nameId;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import its.madruga.wpp.xposed.models.XHookBase;

public class XChatsFilter extends XHookBase {

    public final int CHATS = 200;
    public final int STATUS = 300;
    public final int CALLS = 400;
    public final int COMMUNITY = 600;
    public final int GROUPS = 800;
    public final ArrayList<Integer> tabs = new ArrayList<>();
    public int tabCount = 0;

    public XChatsFilter(ClassLoader loader, XSharedPreferences preferences) {
        super(loader, preferences);
        var newHome = prefs.getBoolean("novahome", false);
        if (newHome) {
            tabs.add(CHATS);
            tabs.add(GROUPS);
            tabs.add(STATUS);
            tabs.add(COMMUNITY);
            tabs.add(CALLS);
        } else {
            tabs.add(COMMUNITY);
            tabs.add(CHATS);
            tabs.add(GROUPS);
            tabs.add(STATUS);
            tabs.add(CALLS);
        }
    }

    public void doHook() {

        var separateGroups = prefs.getBoolean("separategroups", false);
        if (!separateGroups) return;

        var cFrag = XposedHelpers.findClass("com.whatsapp.conversationslist.ConversationsFragment", loader);
        var home = XposedHelpers.findClass("com.whatsapp.HomeActivity", loader);

        // Modifying tab list order
        hookTabList(home);
        // Setting up fragments
        hookTabInstance(cFrag);
        // Setting group tab name
        hookTabName();
        // Setting group icon
        hookTabIcon();

    }

    private void hookTabIcon() {
        findAndHookMethod(classTabsList, loader, methodTabIcon, new XC_MethodHook() {
            @SuppressLint("ResourceType")
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                var superClass = param.thisObject.getClass().getSuperclass();
                if (superClass != null && superClass.getName().equals(classTabsList)) {
                    var fields = superClass.getDeclaredFields();
                    for (var field : fields) {
                        XposedBridge.log("Field: " + superClass.getName() + ";->" + field.getName() + ":" + field.getType().getName());
                    }
                    var field1 = superClass.getDeclaredField("A09").get(param.thisObject);
//                    var field1 = getObjectField(superClass, "A09");
                    var field2 = getObjectField(field1, "A01");
                    var menu = getObjectField(field2, "A03");
                    if (menu != null) {
                        var menuItem = (MenuItem) callMethod(menu, "findItem", GROUPS);
                        if (menuItem != null) {
                            menuItem.setIcon(0x7f0803ce);
                        }
                    }
                }

                super.afterHookedMethod(param);
            }
        });
    }

    @SuppressLint("ResourceType")
    private void hookTabName() {
        findAndHookMethod(classTabName, loader, methodTabName, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                var tab = (int) param.args[0];
                var activity = (Activity) XposedHelpers.getObjectField(param.thisObject, "A02");
                if (tab == GROUPS) {
                    var name = activity.getResources().getString(nameId);
                    param.setResult(name);
                }
            }
        });
    }

    private void hookTabInstance(Class<?> cFrag) {
        XposedHelpers.findAndHookMethod(classGetTab, loader, methodGetTab, int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                var tabId = ((Number) tabs.get((int) param.args[0])).intValue();

                if (tabId == GROUPS || tabId == CHATS) {
                    var convFragment = XposedHelpers.findConstructorExact(cFrag.getName(), loader).newInstance();
                    var convFragmentClass = convFragment.getClass();
                    XposedHelpers.setAdditionalInstanceField(convFragment, "isGroup", tabId == GROUPS);
                    XposedHelpers.findAndHookMethod(convFragmentClass.getName(), convFragmentClass.getClassLoader(), methodTabInstance, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            var isGroup = false;
                            var isGroupField = XposedHelpers.getAdditionalInstanceField(param.thisObject, "isGroup");

                            // Temp fix for
                            if (isGroupField == null) {
                                XposedBridge.log("-----------------------------------");
                                XposedBridge.log("isGroupTabCount: " + tabCount);
                                XposedBridge.log("isGroupTabField: " + (isGroupField != null));
                                XposedBridge.log("isGroupTabCount >= 2: " + (tabCount >= 2));
                                XposedBridge.log("-----------------------------------");
                                isGroup = tabCount >= 2;
                                tabCount++;
                                if (tabCount == 4) tabCount = 0;
                            } else {
                                isGroup = (boolean) isGroupField;
                            }
                            XposedBridge.log("[•] isGroup: " + isGroup);

                            var chatsList = (List) param.getResult();
                            var editableChatList = new ArrayList<>();
                            var requiredServer = isGroup ? "g.us" : "s.whatsapp.net";
                            for (var chat : chatsList) {
                                var server = (String) callMethod(getObjectField(chat, "A00"), "getServer");
                                if (server.equals(requiredServer)) {
                                    editableChatList.add(chat);
                                }
                            }
                            param.setResult(editableChatList);
                            super.afterHookedMethod(param);
                        }
                    });
                    param.setResult(convFragment);
                } else {
                    super.afterHookedMethod(param);
                }
            }
        });
    }

    private void hookTabList(Class<?> home) {
        if (!prefs.getBoolean("separategroups", false)) return;
        XposedHelpers.findAndHookMethod(classTabsList, loader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                var newHome = prefs != null && prefs.getBoolean("novahome", false);
                tabs.clear();
                if (newHome) {
                    tabs.add(CHATS);
                    tabs.add(GROUPS);
                    tabs.add(STATUS);
                    tabs.add(COMMUNITY);
                    tabs.add(CALLS);
                } else {
                    tabs.add(COMMUNITY);
                    tabs.add(CHATS);
                    tabs.add(GROUPS);
                    tabs.add(STATUS);
                    tabs.add(CALLS);
                }
                XposedHelpers.setStaticObjectField(home, fieldTabsList, tabs);
                XposedBridge.log(tabs.toString());
            }
        });
    }
}
