package rnb;

import android.graphics.Paint;
import android.text.GetChars;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.Spanned;

import java.lang.reflect.Array;
import java.util.Arrays;

public class TextBlockSpannable implements Spannable, GetChars {

    private ContentBlock textBlock;

    private Object[] spans;
    private int[] spanData;
    private int spanCount;

    private static final int START = 0;
    private static final int END = 1;
    private static final int FLAGS = 2;
    private static final int COLUMNS = 3;

    public TextBlockSpannable(ContentBlock textBlock, Paint furiganaPaint) {
        this.textBlock = textBlock;

        spans = new Object[10];
        // Invariant: this.spanData.length = this.spans.length * COLUMNS
        spanData = new int[10 * COLUMNS];

        setReadings(textBlock, furiganaPaint);
    }

    public void reset(ContentBlock newTextBlock, Paint furiganaPaint) {
        this.textBlock = newTextBlock;
        Arrays.fill(spans, null);
        // Note: spanData isn't accessed when there are no spans
        this.spanCount = 0;

        setReadings(newTextBlock, furiganaPaint);
    }

    private void setReadings(ContentBlock textBlock, Paint furiganaPaint) {
        for (Book.Furigana reading : textBlock.readings) {
            int startOffset = reading.location & 0xFFFF;
            int endOffset = startOffset + (reading.location >> 16);

            FuriganaSpan span = new FuriganaSpan();
            span.reading = reading.reading;
            span.furiganaPaint = new Paint(furiganaPaint);

            setSpan(
                    span,
                    startOffset,
                    endOffset,
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            );
        }
    }

    @Override
    public void setSpan(Object what, int start, int end, int flags) {
        int len = length();
        if (end < start || start < 0 || end > len) {
            throw new RuntimeException(start + ".." + end + ", len=" + len);
        }

        int count = spanCount;
        Object[] spans = this.spans;
        int[] data = spanData;

        for (int i = 0; i < count; i++) {
            // Updating an existing span
            if (spans[i] == what) {
                int oldStart = data[i * COLUMNS + START];
                int oldEnd = data[i * COLUMNS + END];

                data[i * COLUMNS + START] = start;
                data[i * COLUMNS + END] = end;
                data[i * COLUMNS + FLAGS] = flags;

                sendSpanChanged(what, oldStart, oldEnd, start, end);
                return;
            }
        }

        if (count + 1 >= spans.length) {
            Object[] newSpans = new Object[count * 2];
            int[] newData = new int[newSpans.length * COLUMNS];

            System.arraycopy(spans, 0, newSpans, 0, count);
            System.arraycopy(data, 0, newData, 0, count * COLUMNS);

            this.spans = newSpans;
            this.spanData = newData;
        }

        this.spans[count] = what;
        spanData[count * COLUMNS + START] = start;
        spanData[count * COLUMNS + END] = end;
        spanData[count * COLUMNS + FLAGS] = flags;
        spanCount = count + 1;
    }

    @Override
    public void removeSpan(Object what) {
        int count = spanCount;
        Object[] spans = this.spans;
        int[] data = spanData;

        for (int i = count - 1; i >= 0; i--) {
            if (spans[i] == what) {
                int c = count - (i + 1);

                System.arraycopy(spans, i + 1, spans, i, c);
                System.arraycopy(data, (i + 1) * COLUMNS, data, i * COLUMNS, c * COLUMNS);

                spanCount = count - 1;

                return;
            }
        }
    }

    /**
     * @noinspection unchecked
     */
    @Override
    public <T> T[] getSpans(int start, int end, Class<T> type) {
        int count = 0;

        int spanCount = this.spanCount;
        Object[] spans = this.spans;
        int[] data = spanData;
        Object[] ret = null;
        Object ret1 = null;

        for (int i = 0; i < spanCount; i++) {
            int spanStart = data[i * COLUMNS + START];
            int spanEnd = data[i * COLUMNS + END];

            if (spanStart > end) {
                continue;
            }
            if (spanEnd < start) {
                continue;
            }

            if (spanStart != spanEnd && start != end) {
                if (spanStart == end) {
                    continue;
                }
                if (spanEnd == start) {
                    continue;
                }
            }

            // verify span class as late as possible, since it is expensive
            if (type != null && type != Object.class && !type.isInstance(spans[i])) {
                continue;
            }

            if (count == 0) {
                ret1 = spans[i];
                count++;
            } else {
                if (count == 1) {
                    ret = (Object[]) Array.newInstance(type, spanCount - i + 1);
                    ret[0] = ret1;
                }

                int prio = data[i * COLUMNS + FLAGS] & Spanned.SPAN_PRIORITY;
                if (prio != 0) {
                    int j;

                    for (j = 0; j < count; j++) {
                        int p = getSpanFlags(ret[j]) & Spanned.SPAN_PRIORITY;

                        if (prio > p) {
                            break;
                        }
                    }

                    System.arraycopy(ret, j, ret, j + 1, count - j);
                    ret[j] = spans[i];
                    count++;
                } else {
                    ret[count++] = spans[i];
                }
            }
        }

        if (count == 0) {
            return emptyArray(type);
        }
        if (count == 1) {
            ret = (Object[]) Array.newInstance(type, 1);
            ret[0] = ret1;
            return (T[]) ret;
        }
        if (count == ret.length) {
            return (T[]) ret;
        }

        Object[] nret = (Object[]) Array.newInstance(type, count);
        System.arraycopy(ret, 0, nret, 0, count);
        return (T[]) nret;
    }

    @Override
    public int getSpanStart(Object tag) {
        for (int i = spanCount - 1; i >= 0; i--) {
            if (spans[i] == tag) {
                return spanData[i * COLUMNS + START];
            }
        }

        return -1;
    }

    @Override
    public int getSpanEnd(Object tag) {
        for (int i = spanCount - 1; i >= 0; i--) {
            if (spans[i] == tag) {
                return spanData[i * COLUMNS + END];
            }
        }

        return -1;
    }

    @Override
    public int getSpanFlags(Object tag) {
        for (int i = spanCount - 1; i >= 0; i--) {
            if (spans[i] == tag) {
                return spanData[i * COLUMNS + FLAGS];
            }
        }

        return 0;
    }

    @Override
    public int nextSpanTransition(int start, int limit, Class type) {
        Object[] spans = this.spans;
        int[] data = spanData;

        if (type == null) {
            type = Object.class;
        }

        for (int i = 0; i < spanCount; i++) {
            int st = data[i * COLUMNS + START];
            int en = data[i * COLUMNS + END];

            Object span = spans[i];
            if (st > start && st < limit && type.isInstance(span)) {
                limit = st;
            }
            if (en > start && en < limit && type.isInstance(span)) {
                limit = en;
            }
        }

        return limit;
    }

    @Override
    public int length() {
        return textBlock.data.length;
    }

    @Override
    public char charAt(int index) {
        return textBlock.data[index];
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        // Note: This is called when copying or sharing text
        return new String(textBlock.data, start, end - start);
    }

    @Override
    public void getChars(int start, int end, char[] buf, int off) {
        System.arraycopy(textBlock.data, start, buf, off, end - start);
    }

    // This is needed to clean up the spans for selection
    private void sendSpanChanged(Object what, int s, int e, int st, int en) {
        SpanWatcher[] watchers = getSpans(Math.min(s, st), Math.max(e, en), SpanWatcher.class);

        for (SpanWatcher spanWatcher : watchers) {
            spanWatcher.onSpanChanged(this, what, s, e, st, en);
        }
    }

    private static final int CACHE_SIZE = 18;

    private static int nextEmptyArrayIndex = 0;
    private static final Object[][] EMPTY_ARRAYS = new Object[CACHE_SIZE][];
    /**
     * @noinspection rawtypes
     */
    private static final Class[] EMPTY_ARRAY_TYPES = new Class[CACHE_SIZE];

    @SuppressWarnings("unchecked")
    private static <T> T[] emptyArray(Class<T> kind) {
        int nextIndex = nextEmptyArrayIndex;
        for (int i = 0; i < nextIndex; i++) {
            if (EMPTY_ARRAY_TYPES[i] == kind) {
                return (T[]) EMPTY_ARRAYS[i];
            }
        }

        T[] newEmptyArray = (T[]) Array.newInstance(kind, 0);
        if (nextIndex >= EMPTY_ARRAY_TYPES.length) {
            return newEmptyArray;
        }

        EMPTY_ARRAY_TYPES[nextIndex] = kind;
        EMPTY_ARRAYS[nextIndex] = newEmptyArray;
        nextEmptyArrayIndex = nextIndex + 1;

        return newEmptyArray;
    }
}
