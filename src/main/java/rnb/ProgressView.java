package rnb;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.text.BoringLayout;
import android.text.Layout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.View;

public class ProgressView extends View implements CharSequence {

    private final char[] chars = new char[]{'0', '0', '.', '0'};
    private int start;
    private int length;

    private final TextPaint paint;
    private BoringLayout layout;
    private final BoringLayout.Metrics metrics;

    private int lastNumerator = 0;
    private int lastDenominator = 0;

    public ProgressView(Context context) {
        super(context);

        paint = new TextPaint();
        paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, context.getResources().getDisplayMetrics()));

        try (TypedArray a = context.obtainStyledAttributes(new int[]{android.R.attr.textColorSecondary})) {
            paint.setColor(a.getColor(0, 0));
        }

        start = 1;
        length = chars.length - 1;

        int width = (int) paint.measureText(chars, start, length);

        metrics = BoringLayout.isBoring(this, paint, TextDirectionHeuristics.LTR, false, null);
        layout = new BoringLayout(this, paint, width, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, metrics, false);

        setWillNotDraw(false);
    }

    @SuppressLint("Range") // Layout#getWidth doesn't use @IntRange in the same way as MeasureSpec
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        BoringLayout layout = this.layout;
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(layout.getWidth(), MeasureSpec.EXACTLY);
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(layout.getHeight(), MeasureSpec.EXACTLY);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        layout.draw(canvas);
    }

    public void setProgress(int numerator, int denominator) {
        if (numerator == lastNumerator && denominator == lastDenominator) {
            return;
        }
        lastNumerator = numerator;
        lastDenominator = denominator;

        char old0 = chars[0];
        char old1 = chars[1];
        char old3 = chars[3];

        if (denominator == 0) {
            chars[0] = '0';
            chars[1] = '0';
            chars[3] = '0';
            recreateLayoutIfChanged(old0, old1, old3);
            return;
        }

        if (numerator == denominator) {
            chars[0] = '9';
            chars[1] = '9';
            chars[3] = '9';
            recreateLayoutIfChanged(old0, old1, old3);
            return;
        }

        int progress = numerator * 1000 / denominator;
        chars[3] = (char) ('0' + (progress % 10));

        progress /= 10;
        chars[1] = (char) ('0' + (progress % 10));

        progress /= 10;
        chars[0] = (char) ('0' + (progress % 10));

        recreateLayoutIfChanged(old0, old1, old3);
    }

    private void recreateLayoutIfChanged(char old0, char old1, char old3) {
        if (old0 == chars[0] && old1 == chars[1] && old3 == chars[3]) {
            return;
        }

        start = chars[0] == '0' ? 1 : 0;
        length = chars.length - start;

        int width = (int) paint.measureText(chars, start, length);
        layout = layout.replaceOrMake(this, paint, width, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, metrics, false);

        requestLayout();
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(int index) {
        return chars[index + start];
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        // This method isn't used
        throw new RuntimeException("");
    }
}
