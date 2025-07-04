package rnb;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Spannable;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Locale;

public class MainActivity extends Activity implements AbsListView.OnScrollListener, View.OnClickListener, FilenameFilter {

    private static final int REQUEST_PERMISSION = 1;
    private static final String EXTENSION = ".rnb";
    static final String BOOKS_DIR = "books";

    private ListView listView;
    private ProgressView progressView;
    private Book book;

    // This is effectively a separate navigation destination, but there's no special handling for
    // saving the UI state or handling the back button when it's shown. This is fine since I rarely
    // open it, and when I use it, it's for a short time.
    private ListView fileListView;
    private File[] files;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Locale.setDefault(Locale.JAPANESE);

        Views.MainActivity views = Views.setContentView(this);
        ListView listView = views.listView;
        ProgressView progressView = views.progressView;

        this.listView = listView;
        this.progressView = progressView;

        listView.setOnScrollListener(this);

        progressView.setOnClickListener(this);

        Storage.LoadResult loadResult = Storage.load(this);
        if (!Environment.isExternalStorageManager()) {
            startActivityForResult(
                    new Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.fromParts("package", getPackageName(), null)
                    ),
                    REQUEST_PERMISSION
            );
            return;
        } else if (loadResult == null) {
            setUpLibrary();
            return;
        }

        File file = new File(new File(Environment.getExternalStorageDirectory(), BOOKS_DIR), loadResult.fileName);
        setup(file);

        float offsetInContentBlock = loadResult.offsetInContentBlock;

        if (offsetInContentBlock == 0.0f) {
            listView.setSelection(loadResult.contentBlockIndex);
            return;
        }

        // Restore offset
        View view = listView.getAdapter().getView(loadResult.contentBlockIndex, null, listView);

        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();

        // Measure synchronously to figure out the height of the view that the offset applies to.
        // Use the screen dimensions since the ListView will end up using the same dimensions.
        // (The dimensions can't be taken from the ListView since it hasn't been measured yet)
        float density = resources.getDisplayMetrics().density;
        view.measure(
                MeasureSpec.makeMeasureSpec((int) (configuration.screenWidthDp * density), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec((int) (configuration.screenHeightDp * density), MeasureSpec.UNSPECIFIED)
        );

        int offset = (int) (view.getMeasuredHeight() * offsetInContentBlock);
        listView.setSelectionFromTop(loadResult.contentBlockIndex, -offset);

        // Call onScroll for the side effect of updating progressView. Don't call it directly since
        // calling it directly would prevent it from being inlined into onScroll.
        onScroll(null, loadResult.contentBlockIndex, 0, book.contentBlocks.length);
    }

    private void setup(File file) {
        try {
            if (book != null) {
                book.close();
            }

            Book newBook = new Book(file);
            book = newBook;

            listView.setAdapter(new Adapter(this, newBook));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        progressView.setProgress(firstVisibleItem, totalItemCount);
    }

    @Override
    public void onClick(View v) {
        if (v instanceof ProgressView) {
            setUpLibrary();
            return;
        }

        ListView fileListView = this.fileListView;
        int position = fileListView.getPositionForView(v);
        if (position == AdapterView.INVALID_POSITION) {
            return;
        }

        ((ViewGroup) findViewById(android.R.id.content)).removeView(fileListView);
        this.fileListView = null;

        setup(files[position]);

        files = null;
    }

    private void setUpLibrary() {
        fileListView = Views.addContentView(this);

        File dir = new File(Environment.getExternalStorageDirectory(), BOOKS_DIR);
        files = dir.listFiles(this);

        fileListView.setAdapter(new FileAdapter(this, files));
    }

    @Override
    public boolean accept(File d, String name) {
        return name.endsWith(EXTENSION);
    }

    @Override
    protected void onPause() {
        super.onPause();

        Book book = this.book;
        if (book == null) {
            return;
        }

        ListView listView = this.listView;
        View firstVisibleView = listView.getChildAt(0);
        int offset = -firstVisibleView.getTop() * 65535 / firstVisibleView.getHeight();

        Storage.save(this, book.name, listView.getFirstVisiblePosition(), offset);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            getWindow().getInsetsController().hide(WindowInsets.Type.systemBars());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PERMISSION && Environment.isExternalStorageManager()) {
            setUpLibrary();
        }
    }

    private static class Adapter extends ArrayAdapter<ContentBlock> implements View.OnAttachStateChangeListener {

        private final Book book;
        private Paint furiganaPaint;
        private Typeface bold;
        private final Spannable.Factory factory = new Spannable.Factory() {
            @Override
            public Spannable newSpannable(CharSequence source) {
                // This allows the TextBlockSpannable to be reused in getView
                if (source instanceof Spannable) {
                    return (Spannable) source;
                }

                return super.newSpannable(source);
            }
        };

        Adapter(Context context, Book book) {
            super(context, -1 /* unused */, book.contentBlocks);
            this.book = book;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ContentBlock item = book.contentBlocks[position];

            if (getItemViewType(position) == 0) {
                TextView view = (TextView) convertView;
                if (view == null) {
                    view = Views.textBlockItem(parent);
                    view.setSpannableFactory(factory);
                    view.addOnAttachStateChangeListener(this);
                }

                // Bold
                Typeface typeface = item.isBold() ? boldTypeface() : Typeface.SERIF;
                view.setTypeface(typeface);

                // Large text
                int textSizeSp = item.isTextLarge() ? 32 : 23;
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);

                Paint furiganaPaint = this.furiganaPaint;
                if (furiganaPaint == null) {
                    TextPaint basePaint = view.getPaint();
                    furiganaPaint = new Paint(basePaint);
                    furiganaPaint.setTextSize(basePaint.getTextSize() * 0.5f);

                    this.furiganaPaint = furiganaPaint;
                }

                CharSequence oldText = view.getText();
                if (oldText instanceof TextBlockSpannable) {
                    ((TextBlockSpannable) oldText).reset(item, furiganaPaint);
                    view.setText(oldText);
                } else {
                    view.setText(new TextBlockSpannable(item, furiganaPaint));
                }

                return view;
            } else {
                ImageView view = (ImageView) convertView;
                if (view == null) {
                    view = Views.imageItem(parent);
                }

                int imageIndex = item.getImageIndex();

                try {
                    ImageDecoder.Source source = ImageDecoder.createSource(book.getImageBytes(imageIndex));
                    Bitmap bm = ImageDecoder.decodeBitmap(source);
                    view.setImageBitmap(bm);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                return view;
            }
        }

        @Override
        public int getItemViewType(int position) {
            return book.contentBlocks[position].isImage() ? 1 : 0;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        private Typeface boldTypeface() {
            // Initialize this lazily since blocks with it will be seen infrequently.
            Typeface bold = this.bold;
            if (bold == null) {
                bold = Typeface.create(Typeface.SERIF, 1000, false);
                this.bold = bold;
            }
            return bold;
        }

        // Workaround for losing text selection ability, see:
        // https://issuetracker.google.com/issues/37095917
        @Override
        public void onViewAttachedToWindow(View v) {
            v.setEnabled(false);
            v.setEnabled(true);
        }

        @Override
        public void onViewDetachedFromWindow(View v) {
        }
    }

    private static class FileAdapter extends ArrayAdapter<File> {

        private final View.OnClickListener onClickListener;

        FileAdapter(MainActivity activity, File[] files) {
            super(activity, -1 /* unused */, files);
            this.onClickListener = activity;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) convertView;
            if (view == null) {
                view = Views.fileItem(parent);
            }

            File file = getItem(position);

            view.setText(file.getName().replace(EXTENSION, ""));
            view.setOnClickListener(onClickListener);

            return view;
        }
    }
}
