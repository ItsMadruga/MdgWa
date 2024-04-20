package its.madruga.wpp.utils.colors;

import android.annotation.SuppressLint;
import android.content.Context;

import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.flag.FlagView;

@SuppressLint("ViewConstructor")
public class CustomFlagView extends FlagView {
    public CustomFlagView(Context context, int layout) {
        super(context, layout);
    }

    @Override
    public void onRefresh(ColorEnvelope colorEnvelope) {

    }

    @Override
    public void onFlipped(Boolean isFlipped) {

    }
}
