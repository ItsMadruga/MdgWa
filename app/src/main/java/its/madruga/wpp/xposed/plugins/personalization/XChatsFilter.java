package its.madruga.wpp.xposed.plugins.personalization;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import its.madruga.wpp.xposed.Unobfuscator;
import its.madruga.wpp.xposed.models.XHookBase;
import its.madruga.wpp.xposed.plugins.core.XMain;

public class XChatsFilter extends XHookBase {

    public final int CHATS = 200;
    public final int STATUS = 300;
    public final int CALLS = 400;
    public final int COMMUNITY = 600;
    public final int GROUPS = 800;
    public final ArrayList<Integer> tabs = new ArrayList<>();
    public int tabCount = 0;
    private int idGroupId = 0;

    public XChatsFilter(ClassLoader loader, XSharedPreferences preferences) {
        super(loader, preferences);
        tabs.add(CHATS);
        tabs.add(GROUPS);
        tabs.add(STATUS);
        tabs.add(COMMUNITY);
        tabs.add(CALLS);
    }

    public void doHook() throws Exception {

        var separateGroups = prefs.getBoolean("separategroups", false);
        if (!separateGroups) return;

        var cFrag = XposedHelpers.findClass("com.whatsapp.conversationslist.ConversationsFragment", loader);
        var home = XposedHelpers.findClass("com.whatsapp.HomeActivity", loader);

        // Modifying tab list order
        hookTabList(home);
        // Setting up fragments
        hookTabInstance(cFrag);
        // Setting group tab name
        hookTabName(home);
        // Setting group icon
        hookTabIcon();
        // Setting tab count
        hookTabCount();
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Chats Filter";
    }

    private void hookTabCount() throws Exception {

        var runMethod = Unobfuscator.loadTabCountMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(runMethod));
        var idField = Unobfuscator.getFieldByType(runMethod.getDeclaringClass(), int.class);
        var pagerField = Unobfuscator.loadTabCountField(loader);
        XposedBridge.hookMethod(runMethod, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                var id = (int) getObjectField(param.thisObject, idField.getName());
                if (id != 32) return;

                var homeActivity = XposedHelpers.getObjectField(param.thisObject, "A00");
                var a1 = XposedHelpers.getObjectField(homeActivity, pagerField.getName());
                var chatCount = 0;
                var groupCount = 0;
                // Fiz ele pegar direto da database, esse metodo que dei hook, e chamado sempre q vc muda de tab, entra/sai de um chat ->
                // ou quando a lista e atualizada, ent ele sempre vai atualizar
                var db = SQLiteDatabase.openDatabase("/data/data/com.whatsapp/databases/msgstore.db", null, SQLiteDatabase.OPEN_READONLY);
                // essa coluna que eu peguei, mostra a quantidade de mensagens n lidas (obvio ne).
                // nao coloquei apenas > 0 pq quando vc marca um chat como nao lido, esse valor fica -1
                // entao pra contar direitinho deixei != 0
                var sql = "SELECT * FROM chat WHERE unseen_message_count != 0";
                var cursor = db.rawQuery(sql, null);
                while (cursor.moveToNext()) {
                    // row da jid do chat
                    @SuppressLint("Range") int jid = cursor.getInt(cursor.getColumnIndex("jid_row_id"));
                    // verifica se esta arquivado ou n
                    @SuppressLint("Range") int hidden = cursor.getInt(cursor.getColumnIndex("hidden"));
                    if (hidden == 1) return;
                    // aqui eu fiz pra verificar se e grupo ou n, ai ele pega as infos da jid de acordo com a row da jid ali de cima
                    var sql2 = "SELECT * FROM jid WHERE _id == ?";
                    var cursor1 = db.rawQuery(sql2, new String[]{String.valueOf(jid)});
                    while (cursor1.moveToNext()) {
                        // esse server armazena oq ele e, s.whatsapp.net, lid, ou g.us
                        @SuppressLint("Range") var server = cursor1.getString(cursor1.getColumnIndex("server"));
                        // separacao simples
                        if (server.equals("g.us")) {
                            groupCount++;
                        } else {
                            chatCount++;
                        }
                    }
                }
                // cada tab tem sua classe, ent eu percorro todas pra funcionar dboa
                for (int i = 0; i < tabs.size(); i++) {
                    var q = XposedHelpers.callMethod(a1, "A00", a1, i);

                    // deixei a de call pq a de gp nao aparece
                    if (tabs.get(i) == CHATS) {
                        setObjectField(q, "A01", groupCount);
                    } else if (tabs.get(i) == CALLS) {
                        setObjectField(q, "A01", chatCount);
                    }
                }
            }
        });

        var enableCountMethod = Unobfuscator.loadEnableCountTabMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(enableCountMethod));
        //Issaq meio q ativa o contador da tab de grupo, mas fica totalmente igual ao chats
        XposedBridge.hookMethod(enableCountMethod, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                var indexTab = (int) param.args[2];
                if (indexTab == tabs.indexOf(CALLS)) {
                    param.args[2] = tabs.indexOf(CHATS);
                } else if (indexTab == tabs.indexOf(CHATS)) {
                    param.args[2] = tabs.indexOf(GROUPS);
                }
            }
        });
    }

    private void hookTabIcon() throws Exception {
        var iconTabMethod = Unobfuscator.loadIconTabMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(iconTabMethod));
        var iconField = Unobfuscator.loadIconTabField(loader);
        var iconFrameField = Unobfuscator.loadIconTabLayoutField(loader);
        var iconMenuField = Unobfuscator.loadIconMenuField(loader);

        XposedBridge.hookMethod(iconTabMethod, new XC_MethodHook() {
            @SuppressLint("ResourceType")
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                var superClass = param.thisObject.getClass().getSuperclass();
                if (superClass != null && superClass == iconTabMethod.getDeclaringClass()) {
                    var field1 = superClass.getDeclaredField(iconField.getName()).get(param.thisObject);
                    var field2 = getObjectField(field1, iconFrameField.getName());
                    var menu = getObjectField(field2, iconMenuField.getName());
                    if (menu != null) {
                        var menuItem = (MenuItem) callMethod(menu, "findItem", GROUPS);
                        if (menuItem != null) {
                            var id = XMain.mApp.getResources().getIdentifier("home_tab_communities_selector", "drawable", XMain.mApp.getPackageName());
                            menuItem.setIcon(id);
                        }
                    }
                }
            }
        });
    }

    @SuppressLint("ResourceType")
    private void hookTabName(Class<?> home) throws Exception {
        var tabNameMethod = Unobfuscator.loadTabNameMethod(loader);
        var idGroupId = getGroupId();
        logDebug(Unobfuscator.getMethodDescriptor(tabNameMethod));
        var activityField = Unobfuscator.getFieldByType(tabNameMethod.getDeclaringClass(), home);
        activityField.setAccessible(true);
        XposedBridge.hookMethod(tabNameMethod, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                var tab = (int) param.args[0];
                var activity = (Activity) activityField.get(param.thisObject);
                if (tab == GROUPS) {
                    if (idGroupId != 0) {
                        param.setResult(activity.getString(idGroupId));
                    } else {
                        param.setResult("Groups");
                    }
                }
            }
        });
    }

    private int getGroupId() {
        Resources resources = XMain.mApp.getResources();
        for (int i = 0x7f120500; i < 0x7f12ffff; i++) {
            try {
                if (resources.getString(i).equalsIgnoreCase("groups")) {
                    idGroupId = i;
                    XposedBridge.log("idGroupString: " + Integer.toHexString(idGroupId));
                    break;
                }
            } catch (Resources.NotFoundException ignored) {
            }
        }
        return idGroupId;
    }

    private void hookTabInstance(Class<?> cFrag) throws Exception {
        var getTabMethod = Unobfuscator.loadGetTabMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(getTabMethod));
        var methodTabInstance = Unobfuscator.loadTabFragmentMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(methodTabInstance));

        XposedBridge.hookMethod(getTabMethod, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                var tabId = ((Number) tabs.get((int) param.args[0])).intValue();

                if (tabId == GROUPS || tabId == CHATS) {
                    var convFragment = XposedHelpers.findConstructorExact(cFrag.getName(), loader).newInstance();
                    var convFragmentClass = convFragment.getClass();
                    XposedHelpers.setAdditionalInstanceField(convFragment, "isGroup", tabId == GROUPS);
                    XposedBridge.hookMethod(methodTabInstance, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            var isGroup = false;
                            var isGroupField = XposedHelpers.getAdditionalInstanceField(param.thisObject, "isGroup");

                            // Temp fix for
                            if (isGroupField == null) {
                                logDebug("-----------------------------------");
                                logDebug("isGroupTabCount: " + tabCount);
                                logDebug("isGroupTabField: " + (isGroupField != null));
                                logDebug("isGroupTabCount >= 2: " + (tabCount >= 2));
                                logDebug("-----------------------------------");
                                isGroup = tabCount >= 2;
                                tabCount++;
                                if (tabCount == 4) tabCount = 0;
                            } else {
                                isGroup = (boolean) isGroupField;
                            }
                            logDebug("[•] isGroup: " + isGroup);

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
                        }
                    });
                    param.setResult(convFragment);
                }
            }
        });

        var fabintMethod = Unobfuscator.loadFabMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(fabintMethod));

        XposedBridge.hookMethod(fabintMethod, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                var isGroup = false;

                var isGroupField = XposedHelpers.getAdditionalInstanceField(param.thisObject, "isGroup");
                if (isGroupField != null)
                    isGroup = (boolean) isGroupField;

                if (isGroup) {
                    param.setResult(GROUPS);
                }
            }
        });
    }

    private void hookTabList(@NonNull Class<?> home) throws Exception {
        var onCreateTabList = Unobfuscator.loadTabListMethod(loader);
        logDebug(Unobfuscator.getMethodDescriptor(onCreateTabList));

        var fieldTabsList = Arrays.stream(home.getDeclaredFields()).filter(f -> f.getType().equals(List.class)).findFirst().orElse(null);
        if (fieldTabsList == null) {
            logDebug("fieldTabsList not found!");
            return;
        }

        fieldTabsList.setAccessible(true);
        XposedBridge.hookMethod(onCreateTabList, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ArrayList<Integer> tabList = ((ArrayList<Integer>) fieldTabsList.get(null));

                if (tabList == null) {
                    logDebug("FieldTabList is null!");
                    return;
                }

                tabList.clear();
                tabList.addAll(tabs);
            }
        });
    }
}
