package rnb;

import android.content.Context;
import android.content.res.TypedArray;
import android.view.textclassifier.TextClassifier;
import android.widget.TextView;

public class LimitedWidthTextView extends TextView {

    private final int desiredWidth;

    public LimitedWidthTextView(Context context) {
        super(context);
        setTextClassifier(TextClassifier.NO_OP);
        setTextIsSelectable(true);
        setLineSpacing(0f, 1.3f);

        float density = getResources().getDisplayMetrics().density;
        setPadding(0, (int) (12 * density), 0, 0);

        try (TypedArray a = context.obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary})) {
            setTextColor(a.getColor(0, 0));
        }

        desiredWidth = (int) (864 * density);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxSize = MeasureSpec.getSize(widthMeasureSpec);
        if (maxSize > desiredWidth) {
            int horizontalPadding = (maxSize - desiredWidth) / 2;
            if (getPaddingLeft() != horizontalPadding) {
                setPadding(horizontalPadding, getPaddingTop(), horizontalPadding, getPaddingBottom());
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
