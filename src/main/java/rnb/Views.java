package rnb;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class Views {

    public static class MainActivity {
        public final ListView listView;
        public final ProgressView progressView;

        public MainActivity(ListView listView, ProgressView progressView) {
            this.listView = listView;
            this.progressView = progressView;
        }
    }

    public static MainActivity setContentView(rnb.MainActivity activity) {
        float density = activity.getResources().getDisplayMetrics().density;

        ListView listView = new ListView(activity);
        {
            listView.setDivider(null);
            listView.setSelector(new ColorDrawable(Color.TRANSPARENT));
            listView.setScrollBarStyle(ListView.SCROLLBARS_OUTSIDE_OVERLAY);

            activity.addContentView(listView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        }

        ProgressView progressView = new ProgressView(activity);
        {
            FrameLayout.LayoutParams progressLayoutParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM | Gravity.END
            );
            int margin = (int) (8 * density);
            progressLayoutParams.leftMargin = margin;
            progressLayoutParams.topMargin = margin;
            progressLayoutParams.rightMargin = margin;
            progressLayoutParams.bottomMargin = margin;

            activity.addContentView(progressView, progressLayoutParams);
        }

        return new MainActivity(listView, progressView);
    }

    public static TextView textBlockItem(ViewGroup parent) {
        TextView view = new LimitedWidthTextView(parent.getContext());
        view.setLayoutParams(
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        );

        return view;
    }

    public static ImageView imageItem(ViewGroup parent) {
        ImageView view = new ImageView(parent.getContext());
        view.setLayoutParams(
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        );
        view.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        return view;
    }

    public static ListView setContentView(SelectBookActivity activity) {
        ListView listView = new ListView(activity);
        listView.setDivider(null);
        listView.setScrollBarStyle(ListView.SCROLLBARS_OUTSIDE_OVERLAY);

        activity.setContentView(listView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        return listView;
    }

    public static TextView fileItem(ViewGroup parent) {
        TextView view = new TextView(parent.getContext());
        view.setLayoutParams(
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        );

        float density = view.getResources().getDisplayMetrics().density;

        int horizontalPadding = (int) (16 * density);
        int verticalPadding = (int) (8 * density);
        view.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);

        view.setMinHeight((int) (56 * density));

        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

        try (TypedArray a = view.getContext().obtainStyledAttributes(new int[]{
                android.R.attr.selectableItemBackground,
                android.R.attr.textColorPrimary
        })) {
            view.setBackground(a.getDrawable(0));
            view.setTextColor(a.getColor(1, 0));
        }

        return view;
    }
}
