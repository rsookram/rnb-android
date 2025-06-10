package rnb;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;

public class SelectBookActivity extends Activity implements FilenameFilter, View.OnClickListener {

    private static final String EXTENSION = ".rnb";

    private static final int REQUEST_PERMISSION = 1;
    public static final String EXTRA_PATH = "a";

    private ListView listView;
    private File[] files;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        listView = Views.setContentView(this);

        if (Environment.isExternalStorageManager()) {
            setup();
        } else {
            startActivityForResult(
                    new Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.fromParts("package", getPackageName(), null)
                    ),
                    REQUEST_PERMISSION
            );
        }
    }

    private void setup() {
        File dir = new File(Environment.getExternalStorageDirectory(), "books");
        files = dir.listFiles(this);

        listView.setAdapter(new FileAdapter(this, files));
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
            setup();
        }
    }

    @Override
    public boolean accept(File d, String name) {
        return name.endsWith(EXTENSION);
    }

    @Override
    public void onClick(View v) {
        int position = listView.getPositionForView(v);
        if (position == AdapterView.INVALID_POSITION) {
            return;
        }

        setResult(RESULT_OK, new Intent().putExtra(EXTRA_PATH, files[position].getPath()));
        finish();
    }

    private static class FileAdapter extends ArrayAdapter<File> {

        private final View.OnClickListener onClickListener;

        FileAdapter(SelectBookActivity activity, File[] files) {
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
