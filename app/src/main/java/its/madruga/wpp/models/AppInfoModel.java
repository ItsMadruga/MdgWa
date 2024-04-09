package its.madruga.wpp.models;

import android.graphics.drawable.Drawable;

public class AppInfoModel {
    String packageName;
    String version;
    Drawable icon;

    public AppInfoModel(String name, String version, Drawable icon) {
        this.packageName = name;
        this.version = version;
        this.icon = icon;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getVersion() {
        return version;
    }

    public Drawable getIcon() {
        return icon;
    }

}