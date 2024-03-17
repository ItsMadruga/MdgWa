package its.madruga.wpp.xposed.plugins.functions;

import static de.robv.android.xposed.XposedHelpers.findClass;
import static its.madruga.wpp.ClassesReference.MediaQuality.imainClass;
import static its.madruga.wpp.ClassesReference.MediaQuality.imethod;
import static its.madruga.wpp.ClassesReference.MediaQuality.iparam1;
import static its.madruga.wpp.ClassesReference.MediaQuality.vClassQuality;
import static its.madruga.wpp.ClassesReference.MediaQuality.vMethodResolution;
import static its.madruga.wpp.ClassesReference.MediaQuality.vParam1;
import static its.madruga.wpp.ClassesReference.MediaQuality.vParam2;
import static its.madruga.wpp.ClassesReference.MediaQuality.vmethod;
import static its.madruga.wpp.ClassesReference.MediaQuality.vmethod2;

import android.graphics.Bitmap;
import android.graphics.RecordingCanvas;
import android.util.Pair;

import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import its.madruga.wpp.xposed.models.XHookBase;

public class XMediaQuality extends XHookBase {
    public XMediaQuality(ClassLoader loader, XSharedPreferences preferences) {
        super(loader, preferences);
    }

    @Override
    public void doHook() {
        var videoQuality = prefs.getBoolean("videoquality", false);
        var imageQuality = prefs.getBoolean("imagequality", false);

        if (videoQuality) {
            XposedHelpers.findAndHookMethod(vClassQuality, loader, vMethodResolution, int.class, int.class, int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    var pair = new Pair<>(param.args[0], param.args[1]);
                    param.setResult(pair);
                }
            });

            XposedHelpers.findAndHookMethod(vClassQuality, loader, vmethod2, int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    param.setResult(1600000);
                }
            });

            XposedHelpers.findAndHookMethod(vClassQuality, loader, vmethod, findClass(vParam1, loader), findClass(vParam2, loader), int.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) {
                    return new Pair<>(true, new ArrayList<>());
                }
            });
        }

        if (imageQuality) {
            // 6Ex
            var iqClass = findClass(imainClass, loader);
            XposedHelpers.findAndHookMethod(iqClass, imethod, findClass(iparam1, loader), iqClass, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    int p1 = (int) param.args[2];
                    int[] props = {1573, 1575, 1578, 1574, 1576, 1577};
                    int max = 10000;
                    int min = 1000;
                    for (int index = 0; index < props.length; index++) {
                        if (props[index] == p1) {
                            if (index <= 2) {
                                param.setResult(min);
                            } else {
                                param.setResult(max);
                            }
                        }
                    }
                    super.beforeHookedMethod(param);
                }
            });

            // Prevent crashes in Media preview
            XposedHelpers.findAndHookMethod(RecordingCanvas.class, "throwIfCannotDraw", Bitmap.class, XC_MethodReplacement.DO_NOTHING);
        }
    }

}