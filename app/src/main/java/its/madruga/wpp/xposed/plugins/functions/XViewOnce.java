package its.madruga.wpp.xposed.plugins.functions;

import static its.madruga.wpp.xposed.plugins.core.XMain.mApp;
import static its.madruga.wpp.xposed.plugins.functions.XStatusDownload.getMimeTypeFromExtension;

import android.annotation.SuppressLint;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import its.madruga.wpp.xposed.Unobfuscator;
import its.madruga.wpp.xposed.models.XHookBase;

public class XViewOnce extends XHookBase {
    public XViewOnce(ClassLoader loader, XSharedPreferences preferences) {
        super(loader, preferences);
    }

    @Override
    public void doHook() throws Exception {
        if (prefs.getBoolean("viewonce", false)) {

            var methods = Unobfuscator.loadViewOnceMethod(loader);

            for (var method : methods) {
                logDebug(Unobfuscator.getMethodDescriptor(method));
                XposedBridge.hookMethod(method, XC_MethodReplacement.DO_NOTHING);
            }
        }

        if (prefs.getBoolean("downloadviewonce", false)) {

            var menuMethod = Unobfuscator.loadViewOnceDownloadMenuMethod(loader);
            logDebug(Unobfuscator.getMethodDescriptor(menuMethod));
            var menuIntField= Unobfuscator.loadViewOnceDownloadMenuField(loader);
            logDebug(Unobfuscator.getFieldDescriptor(menuIntField));
            var initIntField = Unobfuscator.loadViewOnceDownloadMenuField2(loader);
            logDebug(Unobfuscator.getFieldDescriptor(initIntField));
            var callMethod = Unobfuscator.loadViewOnceDownloadMenuCallMethod(loader);
            logDebug(Unobfuscator.getMethodDescriptor(callMethod));
            var fileField = Unobfuscator.loadStatusDownloadFileField(loader);
            logDebug(Unobfuscator.getFieldDescriptor(fileField));

            XposedBridge.hookMethod(menuMethod, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);

                    if (XposedHelpers.getIntField(param.thisObject, menuIntField.getName()) == 3) {
                        Menu menu = (Menu) param.args[0];
                        @SuppressLint("DiscouragedApi") var idIconDownload = mApp.getResources().getIdentifier("btn_download", "drawable", mApp.getPackageName());
                        MenuItem item = menu.add(0, 0, 0, "Download").setIcon(idIconDownload);
                        item.setShowAsAction(2);
                        item.setOnMenuItemClickListener(item1 -> {
                            var i = XposedHelpers.getIntField(param.thisObject, initIntField.getName());
                            logDebug("init: " + i);
                            var message = callMethod.getParameterCount() == 2 ? XposedHelpers.callMethod(param.thisObject, callMethod.getName(), param.thisObject,i) : XposedHelpers.callMethod(param.thisObject, callMethod.getName(),i);
                            log("message: " + message);
                            if (message != null) {
                                var fileData = XposedHelpers.getObjectField(message, "A01");
                                var file = (File) XposedHelpers.getObjectField(fileData, fileField.getName());
                                var dest = getPathDestination(file);
                                if (XStatusDownload.copyFile(file,dest)) {
                                    Toast.makeText(mApp, "Saved to "+ getPathDestination(file), Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(mApp, "Error when saving, try again", Toast.LENGTH_SHORT).show();
                                }
                            }
                            return true;
                        });
                    }

                }
            });
        }

    }

    @NonNull
    @Override
    public String getPluginName() {
        return "View Once";
    }

    @NonNull
    private static File getPathDestination(@NonNull File f) {
        var fileName = f.getName().toLowerCase();

        var mediaPath = getViewOnceFolderPath(getMimeTypeFromExtension(fileName));
        if (!mediaPath.exists())
            mediaPath.mkdirs();

        return new File(mediaPath, f.getName());
    }

    private static File getViewOnceFolderPath(@NonNull String mimeType) {
        String folderPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        XposedBridge.log(mimeType);
        if (mimeType.contains("video")) {
            folderPath += "/Movies/WhatsApp/MdgWa ViewOnce/ViewOnce Videos/";
        } else if (mimeType.contains("image")) {
            folderPath += "/Pictures/WhatsApp/MdgWa ViewOnce/ViewOnce Images/";
        } else if (mimeType.contains("audio")) {
            folderPath += "/Music/WhatsApp/MdgWa ViewOnce/ViewOnce Sounds/";
        } else {
            folderPath += "/Download/WhatsApp/MdgWa ViewOnce/ViewOnce Media/";
        }
        return new File(folderPath);
    }
}