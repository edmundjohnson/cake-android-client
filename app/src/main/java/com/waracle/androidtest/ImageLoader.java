package com.waracle.androidtest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidParameterException;

/**
 * Image loader class.
 * Created by Riad on 20/05/2015.
 */
public class ImageLoader {

    private static final String TAG = ImageLoader.class.getSimpleName();

    public ImageLoader() { /**/ }

    /**
     * Simple function for loading a bitmap image from the web
     *
     * @param url       image url
     * @param imageView view to set image too.
     */
    public void load(String url, ImageView imageView) {
        if (TextUtils.isEmpty(url)) {
            throw new InvalidParameterException("URL is empty!");
        }

        // Can you think of a way to improve loading of bitmaps
        // that have already been loaded previously??
        // ECJ: Cache them in memory (though I would use Picasso)

//        try {
//            setImageView(imageView, convertToBitmap(loadImageData(url)));
//        } catch (IOException e) {
//            Log.e(TAG, e.getMessage());
//        }

        // Load data from net.
        new LoadImageDataTask(imageView).execute(url, null, null);

    }

//    private static byte[] loadImageData(String url) throws IOException {
//        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
//        InputStream inputStream = null;
//        try {
//            try {
//                // Read data from workstation
//                inputStream = connection.getInputStream();
//            } catch (IOException e) {
//                // Read the error from the workstation
//                inputStream = connection.getErrorStream();
//            }
//
//            // Can you think of a way to make the entire
//            // HTTP more efficient using HTTP headers??
//
//            return StreamUtils.readUnknownFully(inputStream);
//        } finally {
//            // Close the input stream if it exists.
//            StreamUtils.close(inputStream);
//
//            // Disconnect the connection
//            connection.disconnect();
//        }
//    }

    private static Bitmap convertToBitmap(byte[] data) {
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

    private static void setImageView(ImageView imageView, Bitmap bitmap) {
        imageView.setImageBitmap(bitmap);
    }

    /**
     * A task which fetches image data and loads it into an ImageView.
     */
    private static class LoadImageDataTask extends AsyncTask<String, Integer, byte[]> {
        private final WeakReference<ImageView> mImageView;

        /**
         * Constructor.
         * @param imageView the ImageView in which the image will be displayed
         */
        LoadImageDataTask(ImageView imageView) {
            super();
            mImageView = new WeakReference<>(imageView);
        }

        /**
         * Fetches the image data from the server and returns it.
         * This is run in the background thread.
         * @param args the arguments passed to the background task
         * @return the image data from the server
         */
        @Override
        protected byte[] doInBackground(@Nullable final String... args) {
            byte[] bytes = null;
            if (args == null || args.length == 0 || args[0] == null || args[0].trim().isEmpty()) {
                return null;
            }
            String url = args[0];
            InputStream inputStream = null;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();

                try {
                    // Read data from workstation
                    inputStream = connection.getInputStream();
                } catch (IOException e) {
                    // Read the error from the workstation
                    inputStream = connection.getErrorStream();
                }

                // Can you think of a way to make the entire
                // HTTP more efficient using HTTP headers??
                // ECJ: I would have to research that one.

                bytes = StreamUtils.readUnknownFully(inputStream);
            } catch (IOException e) {
                Log.e(TAG, "Exception while loading image data", e);

            } finally {
                // Close the input stream if it exists.
                StreamUtils.close(inputStream);

                // Disconnect the connection
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return bytes;
        }

        /**
         * This is run in the UI thread when doInBackground() has completed.
         * @param bytes the data that was fetched in the background
         */
        @Override
        protected void onPostExecute(final byte[] bytes) {
            if (mImageView != null) {
                setImageView(mImageView.get(), convertToBitmap(bytes));
            }
        }

    }

}
