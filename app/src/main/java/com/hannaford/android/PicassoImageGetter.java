package com.hannaford.android;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.os.AsyncTask;
import android.text.Html;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

public class PicassoImageGetter implements Html.ImageGetter {
    private static final String TAG = PicassoImageGetter.class.getSimpleName();

    private final Context context;
    private final TextView textView;

    private class ImageGetterAsyncTask extends AsyncTask<TextView, Void, Bitmap> {


        private LevelListDrawable levelListDrawable;
        private String source;

        public ImageGetterAsyncTask(String source, LevelListDrawable levelListDrawable) {
            this.source = source;
            this.levelListDrawable = levelListDrawable;
        }

        @Override
        protected Bitmap doInBackground(TextView... params) {
            try {
                Log.d(TAG, "Downloading the image from: " + source);
                return Picasso.with(context).load(source).get();
            } catch (Exception e) {
                Log.w(TAG,e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(final Bitmap bitmap) {
            try {
                Drawable d = new BitmapDrawable(context.getResources(), bitmap);
                levelListDrawable.addLevel(1, 1, d);
                // Set bounds width  and height according to the bitmap resized size
                levelListDrawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
                levelListDrawable.setLevel(1);
                textView.setText(textView.getText()); // invalidate() doesn't work correctly...
            } catch (Exception e) {
                Log.w(TAG,e);
            }
        }
    }

    public PicassoImageGetter(Context context, TextView textView){
        this.context = context;
        this.textView = textView;
    }

    @Override
    public Drawable getDrawable(String source) {
        LevelListDrawable d = new LevelListDrawable();
        Drawable empty = context.getResources().getDrawable(R.drawable.abc_btn_check_material);;
        d.addLevel(0, 0, empty);
        d.setBounds(0, 0, empty.getIntrinsicWidth(), empty.getIntrinsicHeight());
        new ImageGetterAsyncTask(source, d).execute();

        return d;
    }
}
