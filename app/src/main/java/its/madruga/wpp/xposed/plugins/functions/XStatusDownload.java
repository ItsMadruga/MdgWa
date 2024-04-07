package its.madruga.wpp.xposed.plugins.functions;

import static its.madruga.wpp.xposed.plugins.core.XMain.mApp;

import android.media.MediaScannerConnection;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import its.madruga.wpp.xposed.Unobfuscator;
import its.madruga.wpp.xposed.models.XHookBase;

public class XStatusDownload extends XHookBase {
    public XStatusDownload(ClassLoader loader, XSharedPreferences preferences) {
        super(loader, preferences);
    }

    public void doHook() throws Exception {
        if (!prefs.getBoolean("downloadstatus", false)) return;
        var setPageActiveMethod = Unobfuscator.loadStatusActivePage(loader);
        var fieldList = Unobfuscator.getFieldByType(setPageActiveMethod.getDeclaringClass(), List.class);
        logDebug("List field: " + fieldList.getName());
        logDebug(Unobfuscator.getMethodDescriptor(setPageActiveMethod));
        var mediaClass = Unobfuscator.loadStatusDownloadMediaClass(loader);
        logDebug("Media class: " + mediaClass.getName());
        var menuStatusClass = Unobfuscator.loadMenuStatusClass(loader);
        logDebug("MenuStatus class: " + menuStatusClass.getName());
        var fieldFile = Unobfuscator.loadStatusDownloadFileField(loader);
        logDebug("File field: " + fieldFile.getName());
        var clazzSubMenu = Unobfuscator.loadStatusDownloadSubMenuClass(loader);
        logDebug("SubMenu class: " + clazzSubMenu.getName());
        var clazzMenu = Unobfuscator.loadStatusDownloadMenuClass(loader);
        logDebug("Menu class: " + clazzMenu.getName());
        var menuField = Unobfuscator.getFieldByType(clazzSubMenu,clazzMenu);
        logDebug("Menu field: " + menuField.getName());

       XposedBridge.hookMethod(setPageActiveMethod, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                var position = (int) param.args[1];
                var list = (List<?>) XposedHelpers.getObjectField(param.args[0], fieldList.getName());
                var message = list.get(position);
                if (message != null && mediaClass.isInstance(message)) {

                    XposedHelpers.findAndHookMethod(menuStatusClass, "onClick", View.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            var fileData = XposedHelpers.getObjectField(message, "A01");
                            var file = (File) XposedHelpers.getObjectField(fileData, fieldFile.getName());
                            Field subMenuField = Arrays.stream(param.thisObject.getClass().getDeclaredFields()).filter(f -> f.getType() == Object.class && clazzSubMenu.isInstance(XposedHelpers.getObjectField(param.thisObject, f.getName()))).findFirst().orElse(null);
                            Object submenu = XposedHelpers.getObjectField(param.thisObject, subMenuField.getName());
                            Object menuObj = XposedHelpers.getObjectField(submenu, menuField.getName());
                            var menu = (MenuItem) XposedHelpers.callMethod(menuObj, "findItem", 0x7f0b1009);
                            if (menu != null) return;
                            menu = (MenuItem) XposedHelpers.callMethod(menuObj, "add", 0, 0x7f0b1009, 0, "Download");
                            menu.setOnMenuItemClickListener(item -> {
                                var dest = getPathDestination(file);
                                if (copyFile(file, dest)) {
                                    Toast.makeText(mApp, "Saved to " + dest, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(mApp, "Error when saving, try again", Toast.LENGTH_SHORT).show();
                                }
                                return true;
                            });
                        }
                    });

                }
            }
        });
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Download Status";
    }

    public static boolean copyFile(@NonNull File from, @NonNull File to) {
        try (FileInputStream in = new FileInputStream(from);
             FileOutputStream out = new FileOutputStream(to)) {
            byte[] bArr = new byte[1024];
            while (true) {
                int read = in.read(bArr);
                if (read <= 0) {
                    in.close();
                    out.close();

                    MediaScannerConnection.scanFile(mApp,
                            new String[]{to.getAbsolutePath()},
                            new String[]{getMimeTypeFromExtension(to.getAbsolutePath())},
                            (path, uri) -> {
                            });

                    return true;
                }
                out.write(bArr, 0, read);
            }
        } catch (IOException e) {
            XposedBridge.log(e.getMessage());
            return false;
        }
    }

    @NonNull
    private static File getPathDestination(@NonNull File f) {
        var fileName = f.getName().toLowerCase();

        var mediaPath = getStatusFolderPath(getMimeTypeFromExtension(fileName));
        if (!mediaPath.exists())
            mediaPath.mkdirs();

        return new File(mediaPath, f.getName());
    }

    @NonNull
    private static File getStatusFolderPath(@NonNull String mimeType) {
        String folderPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        if (mimeType.contains("video")) {
            folderPath += "/Movies/WhatsApp/MdgWa Status/Status Videos/";
        } else if (mimeType.contains("image")) {
            folderPath += "/Pictures/WhatsApp/MdgWa Status/Status Images/";
        } else if (mimeType.contains("audio")) {
            folderPath += "/Music/WhatsApp/MdgWa Status/Status Sounds/";
        } else {
            folderPath += "/Download/WhatsApp/MdgWa Status/Status Media/";
        }
        return new File(folderPath);
    }

    public static String getMimeTypeFromExtension(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }
}