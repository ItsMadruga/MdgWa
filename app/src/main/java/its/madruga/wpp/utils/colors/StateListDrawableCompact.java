package its.madruga.wpp.utils.colors;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;

import androidx.annotation.Nullable;

import java.lang.reflect.Method;

import de.robv.android.xposed.XposedHelpers;

public class StateListDrawableCompact {
    private static final Class<?> mClass = StateListDrawable.class;

    private StateListDrawableCompact() {
    }

    public static int getStateCount(StateListDrawable stateListDrawable) {
        try {
            Method method = XposedHelpers.findMethodExact(mClass, Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? "hidden_getStateCount" : "getStateCount");
            if (method != null) {
                Object invoke = method.invoke(stateListDrawable);
                if (invoke instanceof Integer) {
                    return (Integer) invoke;
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    @Nullable
    public static Drawable getStateDrawable(StateListDrawable stateListDrawable, int i) {
        try {
            Method method = XposedHelpers.findMethodExact(mClass, Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? "hidden_getStateDrawable" : "getStateDrawable");
            if (method != null) {
                Object invoke = method.invoke(stateListDrawable, i);
                if (invoke instanceof Drawable) {
                    return (Drawable) invoke;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}