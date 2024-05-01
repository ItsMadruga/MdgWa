package its.madruga.wpp.xposed.plugins.personalization;

import static its.madruga.wpp.utils.colors.IColors.parseColor;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import its.madruga.wpp.xposed.Unobfuscator;
import its.madruga.wpp.xposed.models.XHookBase;

public class XBubbleColors extends XHookBase {
    public XBubbleColors(ClassLoader loader, XSharedPreferences preferences) {
        super(loader, preferences);
    }

    @Override
    public void doHook() throws Exception {
        var bubbleLeftColor = prefs.getString("bubble_left", "0");
        var bubbleRightColor = prefs.getString("bubble_right", "0");

        if (!bubbleRightColor.equals("0")) {

            var balloonOutgoingNormalMethod = Unobfuscator.loadBubbleColorsMethod(loader, Unobfuscator.BUBBLE_COLORS_BALLOON_OUTGOING_NORMAL);
            logDebug(Unobfuscator.getMethodDescriptor(balloonOutgoingNormalMethod));
            XposedBridge.hookMethod(balloonOutgoingNormalMethod, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    var balloon = (Drawable) param.getResult();
                    balloon.setColorFilter(
                            new PorterDuffColorFilter(parseColor(bubbleRightColor), PorterDuff.Mode.SRC_IN)
                    );
                }
            });

            var balloonOutgoingNormalExtMethodExt = Unobfuscator.loadBubbleColorsMethod(loader, Unobfuscator.BUBBLE_COLORS_BALLOON_OUTGOING_NORMAL_EXT);
            logDebug(Unobfuscator.getMethodDescriptor(balloonOutgoingNormalExtMethodExt));
            XposedBridge.hookMethod(balloonOutgoingNormalExtMethodExt, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    var balloon = (Drawable) param.getResult();
                    balloon.setColorFilter(
                            new PorterDuffColorFilter(parseColor(bubbleRightColor), PorterDuff.Mode.SRC_IN)
                    );
                }
            });
        }

        if (!bubbleLeftColor.equals("0")) {


            var balloonIncomingNormalMethod = Unobfuscator.loadBubbleColorsMethod(loader, Unobfuscator.BUBBLE_COLORS_BALLOON_INCOMING_NORMAL);
            logDebug(Unobfuscator.getMethodDescriptor(balloonIncomingNormalMethod));
            XposedBridge.hookMethod(balloonIncomingNormalMethod, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    var balloon = (Drawable) param.getResult();
                    balloon.setColorFilter(
                            new PorterDuffColorFilter(parseColor(bubbleLeftColor), PorterDuff.Mode.SRC_IN)
                    );
                }
            });


            var balloonIncomingNormalMethodExt = Unobfuscator.loadBubbleColorsMethod(loader, Unobfuscator.BUBBLE_COLORS_BALLOON_INCOMING_NORMAL_EXT);
            logDebug(Unobfuscator.getMethodDescriptor(balloonIncomingNormalMethodExt));
            XposedBridge.hookMethod(balloonIncomingNormalMethodExt, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    var balloon = (Drawable) param.getResult();
                    balloon.setColorFilter(
                            new PorterDuffColorFilter(parseColor(bubbleLeftColor), PorterDuff.Mode.SRC_IN)
                    );
                }
            });
        }
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Bubble Colors";
    }
}
