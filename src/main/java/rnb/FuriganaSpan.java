package rnb;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.ReplacementSpan;

public class FuriganaSpan extends ReplacementSpan {

    public char[] reading;
    public Paint furiganaPaint;
    private int textWidth = 0;
    private int readingWidth = 0;

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        int newTextWidth = (int) paint.measureText(text, start, end);
        textWidth = newTextWidth;

        int readingLength = reading.length;
        Paint furiganaPaint = this.furiganaPaint;

        int newReadingWidth = (int) furiganaPaint.measureText(reading, 0, readingLength);
        readingWidth = newReadingWidth;

        int numTextChars = end - start;
        float letterSpacing = 0.0f;
        if (readingLength <= numTextChars) {
            // <= 1 character in reading per character in text
            letterSpacing = 0.6f;
        } else if (2 * numTextChars > readingLength) {
            // (1, 2) characters in reading per character in text
            letterSpacing = 0.3f;
        }
        furiganaPaint.setLetterSpacing(letterSpacing);

        return Math.max(newTextWidth, newReadingWidth);
    }

    @Override
    public void draw(
            Canvas canvas,
            CharSequence text,
            int start,
            int end,
            float x,
            int top,
            int y,
            int bottom,
            Paint paint
    ) {
        canvas.save();

        float furiganaOffsetX = 0f;
        float textOffsetX = 0f;
        if (textWidth > readingWidth) {
            furiganaOffsetX = (textWidth - readingWidth) / 2f;
        } else if (textWidth < readingWidth) {
            textOffsetX = (readingWidth - textWidth) / 2f;
        }
        canvas.translate(furiganaOffsetX, -y);

        Paint furiganaPaint = this.furiganaPaint;
        canvas.drawText(reading, 0, reading.length, x, top + y + furiganaPaint.descent(), furiganaPaint);

        canvas.restore();

        // Draw the original text.
        canvas.save();
        canvas.translate(textOffsetX, 0f);
        canvas.drawText(text, start, end, x, y, paint);
        canvas.restore();
    }
}
