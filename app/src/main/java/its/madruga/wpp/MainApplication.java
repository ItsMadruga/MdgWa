package its.madruga.wpp;

import android.app.Application;

public class MainApplication extends Application {

    static {
        try {
            System.loadLibrary("darker");
        } catch (UnsatisfiedLinkError ignored) {
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
