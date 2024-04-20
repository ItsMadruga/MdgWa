package its.madruga.wpp.xposed.plugins.core;

import de.robv.android.xposed.XSharedPreferences;
import its.madruga.wpp.core.databases.Axolotl;
import its.madruga.wpp.core.databases.MessageStore;
import its.madruga.wpp.xposed.Unobfuscator;

public class XDatabases {

    public static MessageStore msgstore;
    public static Axolotl axolotl;

    public static void Initialize(ClassLoader loader, XSharedPreferences pref) throws Exception {
        var msgstoreClass = Unobfuscator.loadMessageStoreClass2(loader);
       // var axolotlClass = Unobfuscator.loadAxolotlClass(loader);
        msgstore = new MessageStore(msgstoreClass.getName(), loader);
//        axolotl = new Axolotl(axolotlClass.getName(), loader);
    }
}
