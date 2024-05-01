package its.madruga.wpp.xposed.plugins.functions;

import static its.madruga.wpp.xposed.plugins.core.XMain.mApp;
import static its.madruga.wpp.xposed.plugins.functions.XStatusDownload.getMimeTypeFromExtension;

import android.annotation.SuppressLint;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import its.madruga.wpp.xposed.Unobfuscator;
import its.madruga.wpp.xposed.models.XHookBase;
import its.madruga.wpp.xposed.plugins.core.ResId;

public class XViewOnce extends XHookBase {
    public XViewOnce(ClassLoader loader, XSharedPreferences preferences) {
        super(loader, preferences);
    }

    @Override
    public void doHook() throws Exception {
        var methods = Unobfuscator.loadViewOnceMethod(loader);
        var classViewOnce = Unobfuscator.loadViewOnceClass(loader);
        logDebug(classViewOnce);
        var classViewOnce2 = Unobfuscator.loadViewOnceClass2(loader);
        logDebug(classViewOnce2);

        for (var method : methods) {
            logDebug(Unobfuscator.getMethodDescriptor(method));
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!prefs.getBoolean("viewonce", false)) return;
                    if ((int) param.getResult() != 2 && (Unobfuscator.isCalledFromClass(classViewOnce) || Unobfuscator.isCalledFromClass(classViewOnce2))) {
                        param.setResult(0);
                    }
                }
            });
        }

        if (prefs.getBoolean("downloadviewonce", false)) {

            var menuMethod = Unobfuscator.loadViewOnceDownloadMenuMethod(loader);
            logDebug(Unobfuscator.getMethodDescriptor(menuMethod));
            var menuIntField = Unobfuscator.loadViewOnceDownloadMenuField(loader);
            logDebug(Unobfuscator.getFieldDescriptor(menuIntField));
            var initIntField = Unobfuscator.loadViewOnceDownloadMenuField2(loader);
            logDebug(Unobfuscator.getFieldDescriptor(initIntField));
            var callMethod = Unobfuscator.loadViewOnceDownloadMenuCallMethod(loader);
            logDebug(Unobfuscator.getMethodDescriptor(callMethod));
            var fileField = Unobfuscator.loadStatusDownloadFileField(loader);
            logDebug(Unobfuscator.getFieldDescriptor(fileField));

            XposedBridge.hookMethod(menuMethod, new XC_MethodHook() {
                @Override
                @SuppressLint("DiscouragedApi")
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);

                    if (XposedHelpers.getIntField(param.thisObject, menuIntField.getName()) == 3) {
                        Menu menu = (Menu) param.args[0];
                        var idIconDownload = mApp.getResources().getIdentifier("btn_download", "drawable", mApp.getPackageName());
                        MenuItem item = menu.add(0, 0, 0, ResId.string.download).setIcon(idIconDownload);
                        item.setShowAsAction(2);
                        item.setOnMenuItemClickListener(item1 -> {
                            var i = XposedHelpers.getIntField(param.thisObject, initIntField.getName());
                            var message = callMethod.getParameterCount() == 2 ? XposedHelpers.callMethod(param.thisObject, callMethod.getName(), param.thisObject, i) : XposedHelpers.callMethod(param.thisObject, callMethod.getName(), i);
                            if (message != null) {
                                var fileData = XposedHelpers.getObjectField(message, "A01");
                                var file = (File) XposedHelpers.getObjectField(fileData, fileField.getName());
                                if (copyFile(file)) {
                                    Toast.makeText(mApp, mApp.getString(ResId.string.saved_to) + getDestination(file), Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(mApp, mApp.getString(ResId.string.error_when_saving_try_again), Toast.LENGTH_SHORT).show();
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

    public static String getDestination(File file) {
        var folderPath = Environment.getExternalStorageDirectory() + "/Pictures/WhatsApp/MdgWa ViewOnce/";
        var filePath = new File(folderPath);
        if (!filePath.exists()) filePath.mkdirs();
        return filePath.getAbsolutePath() + "/" + file.getName();
    }

    private static boolean copyFile(File p) {
        if (p == null) return false;

        var destination = getDestination(p);

        try (FileInputStream in = new FileInputStream(p);
             FileOutputStream out = new FileOutputStream(destination)) {
            byte[] bArr = new byte[1024];
            while (true) {
                int read = in.read(bArr);
                if (read <= 0) {
                    in.close();
                    out.close();

                    String[] parts = destination.split("\\.");
                    String ext = parts[parts.length - 1].toLowerCase();

                    MediaScannerConnection.scanFile(mApp,
                            new String[]{destination},
                            new String[]{getMimeTypeFromExtension(ext)},
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
}