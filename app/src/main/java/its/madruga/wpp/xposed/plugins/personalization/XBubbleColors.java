package its.madruga.wpp.xposed.plugins.personalization;

import static its.madruga.wpp.utils.colors.IColors.parseColor;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;

import androidx.annotation.NonNull;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import its.madruga.wpp.xposed.Unobfuscator;
import its.madruga.wpp.xposed.models.XHookBase;
import its.madruga.wpp.xposed.plugins.core.DesignUtils;

public class XBubbleColors extends XHookBase {
    public XBubbleColors(ClassLoader loader, XSharedPreferences preferences) {
        super(loader, preferences);
    }

    @Override
    public void doHook() throws Exception {
        var bubbleLeftColor = prefs.getString("bubble_left", "0");
        var bubbleRightColor = prefs.getString("bubble_right", "0");

        if (!bubbleRightColor.equals("0")) {

            var balloon = DesignUtils.getDrawableByName(Unobfuscator.BUBBLE_COLORS_BALLOON_OUTGOING_NORMAL);
            balloon.setColorFilter(new PorterDuffColorFilter(parseColor(bubbleRightColor), PorterDuff.Mode.SRC_IN));
            DesignUtils.setReplacementDrawable(Unobfuscator.BUBBLE_COLORS_BALLOON_OUTGOING_NORMAL, balloon);

            var balloonExt = DesignUtils.getDrawableByName(Unobfuscator.BUBBLE_COLORS_BALLOON_OUTGOING_NORMAL_EXT);
            balloonExt.setColorFilter(new PorterDuffColorFilter(parseColor(bubbleRightColor), PorterDuff.Mode.SRC_IN));
            DesignUtils.setReplacementDrawable(Unobfuscator.BUBBLE_COLORS_BALLOON_OUTGOING_NORMAL_EXT, balloonExt);

            var balloonFrame = DesignUtils.getDrawableByName("balloon_outgoing_frame");
            balloonFrame.setColorFilter(new PorterDuffColorFilter(parseColor(bubbleRightColor), PorterDuff.Mode.SRC_IN));
            DesignUtils.setReplacementDrawable("balloon_outgoing_frame", balloonFrame);

            var balloonPressed = DesignUtils.getDrawableByName("balloon_outgoing_pressed");
            balloonPressed.setColorFilter(new PorterDuffColorFilter(parseColor(bubbleRightColor), PorterDuff.Mode.SRC_IN));
            DesignUtils.setReplacementDrawable("balloon_outgoing_pressed", balloonPressed);

            var balloonSticker = DesignUtils.getDrawableByName("balloon_outgoing_normal_stkr");
            balloonSticker.setColorFilter(new PorterDuffColorFilter(parseColor(bubbleRightColor), PorterDuff.Mode.SRC_IN));
            DesignUtils.setReplacementDrawable("balloon_outgoing_normal_stkr", balloonSticker);
        }

        if (!bubbleLeftColor.equals("0")) {

            var balloon = DesignUtils.getDrawableByName(Unobfuscator.BUBBLE_COLORS_BALLOON_INCOMING_NORMAL);
            balloon.setColorFilter(new PorterDuffColorFilter(parseColor(bubbleLeftColor), PorterDuff.Mode.SRC_IN));
            DesignUtils.setReplacementDrawable(Unobfuscator.BUBBLE_COLORS_BALLOON_INCOMING_NORMAL, balloon);

            var balloonExt = DesignUtils.getDrawableByName(Unobfuscator.BUBBLE_COLORS_BALLOON_INCOMING_NORMAL_EXT);
            balloonExt.setColorFilter(new PorterDuffColorFilter(parseColor(bubbleLeftColor), PorterDuff.Mode.SRC_IN));
            DesignUtils.setReplacementDrawable(Unobfuscator.BUBBLE_COLORS_BALLOON_INCOMING_NORMAL_EXT, balloonExt);

            var balloonFrame = DesignUtils.getDrawableByName("balloon_incoming_frame");
            balloonFrame.setColorFilter(new PorterDuffColorFilter(parseColor(bubbleLeftColor), PorterDuff.Mode.SRC_IN));
            DesignUtils.setReplacementDrawable("balloon_incoming_frame", balloonFrame);

            var balloonPressed = DesignUtils.getDrawableByName("balloon_incoming_pressed");
            balloonPressed.setColorFilter(new PorterDuffColorFilter(parseColor(bubbleLeftColor), PorterDuff.Mode.SRC_IN));
            DesignUtils.setReplacementDrawable("balloon_incoming_pressed", balloonPressed);

            var balloonSticker = DesignUtils.getDrawableByName("balloon_incoming_normal_stkr");
            balloonSticker.setColorFilter(new PorterDuffColorFilter(parseColor(bubbleLeftColor), PorterDuff.Mode.SRC_IN));
            DesignUtils.setReplacementDrawable("balloon_incoming_normal_stkr", balloonSticker);

        }

        var methods = Unobfuscator.loadNineDrawableMethods(loader);
        for (var method : methods) {
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    var draw = (NinePatchDrawable) param.getResult();
                    var right = (boolean) param.args[3];
                    if (right) {
                        if (bubbleRightColor.equals("0"))return;
                        draw.setColorFilter(new PorterDuffColorFilter(parseColor(bubbleRightColor), PorterDuff.Mode.SRC_IN));
                    } else {
                        if (bubbleLeftColor.equals("0"))return;
                        draw.setColorFilter(new PorterDuffColorFilter(parseColor(bubbleLeftColor), PorterDuff.Mode.SRC_IN));
                    }
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
