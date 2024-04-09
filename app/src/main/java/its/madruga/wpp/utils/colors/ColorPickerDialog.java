package its.madruga.wpp.utils.colors;

import static androidx.core.content.ContextCompat.getSystemService;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatEditText;

import com.skydoves.colorpickerview.AlphaTileView;
import com.skydoves.colorpickerview.ColorHsvPalette;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.flag.BubbleFlag;
import com.skydoves.colorpickerview.flag.FlagMode;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;
import com.skydoves.colorpickerview.sliders.AlphaSlideBar;
import com.skydoves.colorpickerview.sliders.BrightnessSlideBar;

import de.hdodenhof.circleimageview.CircleImageView;
import its.madruga.wpp.BuildConfig;
import its.madruga.wpp.R;

public class ColorPickerDialog extends Dialog {
    private final String tag;
    private final TextView sumView;
    private final CharSequence summary;
    private final CircleImageView colorPickerV;
    private boolean firstLoad;
    private boolean isTextEdited = false;

    public ColorPickerDialog(@NonNull Context context, String tag, TextView sumView, CharSequence summary, CircleImageView colorPickerV) {
        super(context);
        this.tag = tag;
        this.sumView = sumView;
        this.summary = summary;
        this.colorPickerV = colorPickerV;
        this.firstLoad = true;
        init();
    }

    @SuppressLint("SetTextI18n")
    private void init() {
        setContentView(R.layout.color_picker_dialog);
        var invalidColorTV = findViewById(R.id.invalid_color);
        var applyButton = findViewById(R.id.confirm_button);
        var cancelButton = findViewById(R.id.cancel_button);
        var resetButton = findViewById(R.id.reset);
        var resources = getContext().getResources();
        var sharedPreferences = getContext().getSharedPreferences("its.madruga.wpp_preferences", Context.MODE_PRIVATE);
        var editor = sharedPreferences.edit();
        var currentColor = sharedPreferences.getString(tag, "#0");
        var argbText = (AppCompatEditText) findViewById(R.id.color_argb_text);
        var alphaTileView = (AlphaTileView) findViewById(R.id.alphaTileView);
        var alphaSlider = (AlphaSlideBar) findViewById(com.skydoves.colorpickerview.R.id.alphaSlideBar);
        var colorPicker = (ColorPickerView) findViewById(com.skydoves.colorpickerview.R.id.colorPickerView);

        var brightnessSlider = (BrightnessSlideBar) findViewById(com.skydoves.colorpickerview.R.id.brightnessSlideBar);
        var bubbleFlag = new BubbleFlag(getContext());

        // Setting components
        brightnessSlider.setSelectorDrawableRes(com.skydoves.colorpickerview.R.drawable.colorpickerview_wheel);
        alphaSlider.setSelectorDrawableRes(com.skydoves.colorpickerview.R.drawable.colorpickerview_wheel);
        bubbleFlag.setFlagMode(FlagMode.FADE);

        // Setting up colorPicker
        colorPicker.attachAlphaSlider(alphaSlider);
        colorPicker.attachBrightnessSlider(brightnessSlider);
        colorPicker.setFlagView(bubbleFlag);

        if (firstLoad) {
            colorPicker.setColorListener((ColorEnvelopeListener) (envelope, fromUser) -> {
                if (!isTextEdited) {
                    argbText.setText("#" + envelope.getHexCode());
                }
                if (!applyButton.isEnabled() && !envelope.getHexCode().equals("0")) {
                    applyButton.setEnabled(true);
                }
                isTextEdited = false;
                editor.putString(tag, "#" + envelope.getHexCode());
                alphaTileView.setPaintColor(envelope.getColor());

            });

            applyButton.setOnClickListener(v -> {
                var newColor = String.valueOf(argbText.getText());
                editor.putString(tag, newColor).apply();
                sumView.setText(String.format(String.valueOf(summary), newColor));
                colorPickerV.setImageDrawable(new ColorDrawable(IColors.parseColor(newColor)));
                if (sharedPreferences.getBoolean("autoreboot", false)) {
                    Intent intent = new Intent(BuildConfig.APPLICATION_ID + ".WHATSAPP.RESTART");
                    applyButton.getContext().sendBroadcast(intent);
                }
                dismiss();
            });
        }

        cancelButton.setOnClickListener(v -> dismiss());
        resetButton.setOnClickListener(v -> {
            editor.remove(tag).apply();
            dismiss();

        });
        var window = getWindow();
        if (window != null) {
            var params = window.getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(params);
            window.setDimAmount(0.7f);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        setCancelable(true);
        show();

        colorPicker.post(() -> {
            colorPicker.setPaletteDrawable(new ColorHsvPalette(resources, Bitmap.createBitmap(colorPicker.getWidth(), colorPicker.getHeight(), Bitmap.Config.ARGB_8888)));
            if (firstLoad) {
                firstLoad = false;
                argbText.setText(currentColor);
                if (!currentColor.equals("#0")) {
                    try {
                        colorPicker.selectByHsvColor(IColors.parseColor(currentColor));
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
                argbText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        try {
                            var colorString = s.toString();
                            var color = IColors.parseColor(colorString);
                            if (invalidColorTV.getVisibility() == View.VISIBLE) {
                                invalidColorTV.setVisibility(View.GONE);
                                applyButton.setEnabled(true);
                                isTextEdited = true;
                                colorPicker.selectByHsvColor(color);
                                alphaTileView.setPaintColor(color);
                            }
                        } catch (Exception e) {
                            if (invalidColorTV.getVisibility() == View.GONE) {
                                invalidColorTV.setVisibility(View.VISIBLE);
                                alphaTileView.setPaintColor(Color.TRANSPARENT);
                                applyButton.setEnabled(false);
                            }
                        }
                    }
                });

                argbText.setOnEditorActionListener((v, actionId, event) -> {
                    var inputMethodManager = (InputMethodManager) getSystemService(getContext(), InputMethodManager.class);
                    if (inputMethodManager != null) {
                        inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                    return true;
                });

                if (String.valueOf(argbText.getText()).equals("#0")) {
                    applyButton.setEnabled(false);
                }
            }

        });
    }

}
