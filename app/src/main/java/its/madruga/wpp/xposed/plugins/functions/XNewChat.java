package its.madruga.wpp.xposed.plugins.functions;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import its.madruga.wpp.R;
import its.madruga.wpp.xposed.models.XHookBase;
import its.madruga.wpp.xposed.plugins.core.ResId;
import its.madruga.wpp.xposed.plugins.core.XMain;

public class XNewChat extends XHookBase {
    public XNewChat(@NonNull ClassLoader loader, @NonNull XSharedPreferences preferences) {
        super(loader, preferences);
    }

    @Override
    public void doHook() {
        var homeActivity = findClass("com.whatsapp.HomeActivity", loader);
        findAndHookMethod(homeActivity, "onCreateOptionsMenu", Menu.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                var home = (Activity) param.thisObject;
                var menu = (Menu) param.args[0];

                var item = menu.add(0, 0, 0, ResId.string.new_chat);
                item.setIcon(home.getResources().getIdentifier("vec_ic_chat_add", "drawable", home.getPackageName()));
                item.setOnMenuItemClickListener(item1 -> {
                    var view = new LinearLayout(home);
                    view.setGravity(Gravity.CENTER);
                    view.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                    var edt = new EditText(view.getContext());
                    edt.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f));
                    edt.setMaxLines(1);
                    edt.setInputType(InputType.TYPE_CLASS_PHONE);
                    edt.setTransformationMethod(null);
                    edt.setHint(ResId.string.number_with_country_code);
                    view.addView(edt);
                    new AlertDialog.Builder(home)
                            .setTitle(ResId.string.new_chat)
                            .setView(view)
                            .setPositiveButton(ResId.string.message, (dialog, which) -> {
                                var number = edt.getText().toString();
                                var numberFomatted = number.replaceAll("[+\\-()/\\s]", "");
                                Toast.makeText(home, numberFomatted, Toast.LENGTH_SHORT).show();
                                    var intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setData(Uri.parse("https://wa.me/" + numberFomatted));
                                    intent.setPackage(XMain.mApp.getPackageName());
                                    home.startActivity(intent);
                            })
                            .setNegativeButton(ResId.string.cancel,null)
                            .setCancelable(false)
                            .create().show();
                    return true;
                });

                super.afterHookedMethod(param);
            }
        });
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "New Chat";
    }
}
