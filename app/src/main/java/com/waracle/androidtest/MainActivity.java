package com.waracle.androidtest;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;


public class MainActivity extends AppCompatActivity {

    private static final String JSON_URL = "https://gist.githubusercontent.com/hart88/198f29ec5114a3ec3460/" +
            "raw/8dd19a88f9b8d24c23d9960f3300d0c917a4f07c/cake.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Fragment is responsible for loading in some JSON and
     * then displaying a list of cakes with images.
     * Fix any crashes
     * Improve any performance issues
     * Use good coding practices to make code more secure
     */
    public static class PlaceholderFragment extends ListFragment {

        private static final String TAG = PlaceholderFragment.class.getSimpleName();
        private static final String KEY_ITEMS = "KEY_ITEMS";
        private static final String KEY_STATE = "KEY_STATE";

        private ListView mListView;
        private MyAdapter mAdapter;

        public PlaceholderFragment() { /**/ }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            mListView = (ListView) rootView.findViewById(android.R.id.list);

            return rootView;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            // Create and set the list adapter.
            mAdapter = new MyAdapter();
            mListView.setAdapter(mAdapter);

            if (savedInstanceState == null) {
                // Load data from net
                new FetchDataTask(this).execute(null, null, null);
            } else {
                // Restore the saved list
                try {
                    JSONArray array = new JSONArray(savedInstanceState.getString(KEY_ITEMS));
                    if (mAdapter != null) {
                        mAdapter.mItems = array;
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "JSONException while restoring list", e);
                }

                Parcelable listState = savedInstanceState.getParcelable(KEY_STATE);
                if (listState != null) {
                    mListView.onRestoreInstanceState(listState);
                }
            }
        }

        @Override
        public void onSaveInstanceState(Bundle state) {
            // Save the list items
            if (mAdapter != null) {
                JSONArray array = mAdapter.mItems;
                state.putString(KEY_ITEMS, array.toString());
            }

            // Save the list position
            Parcelable listState = mListView.onSaveInstanceState();
            state.putParcelable(KEY_STATE, listState);

            super.onSaveInstanceState(state);
        }

        public void setAdapterItems(JSONArray array) {
            if (mAdapter != null) {
                mAdapter.setItems(array);
                mAdapter.notifyDataSetChanged();
            }
        }

        /**
         * The list adapter.
         */
        private class MyAdapter extends BaseAdapter {

            // Can you think of a better way to represent these items???
            // ECJ: I would create a Cake data class.
            private JSONArray mItems;
            private final ImageLoader mImageLoader;

            MyAdapter() {
                this(new JSONArray());
            }

            MyAdapter(JSONArray items) {
                mItems = items;
                mImageLoader = new ImageLoader();
            }

            @Override
            public int getCount() {
                return mItems.length();
            }

            @Override
            public Object getItem(int position) {
                try {
                    return mItems.getJSONObject(position);
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage());
                }
                return null;
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            // TODO: Implement ViewHolder
            @SuppressLint("ViewHolder")
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                LayoutInflater inflater = LayoutInflater.from(getActivity());
                View root = inflater.inflate(R.layout.list_item_layout, parent, false);
                if (root != null) {
                    TextView title = (TextView) root.findViewById(R.id.title);
                    TextView desc = (TextView) root.findViewById(R.id.desc);
                    ImageView image = (ImageView) root.findViewById(R.id.image);
                    try {
                        JSONObject object = (JSONObject) getItem(position);
                        title.setText(object.getString("title"));
                        desc.setText(object.getString("desc"));
                        mImageLoader.load(object.getString("image"), image);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing JSON for list item.", e);
                        //e.printStackTrace();
                    }
                }

                return root;
            }

            void setItems(JSONArray items) {
                mItems = items;
            }

        }

        /**
         * A task which fetches the JSON data from the server and loads it into the adapter.
         */
        private static class FetchDataTask extends AsyncTask<Void, Integer, JSONArray> {
            private final WeakReference<PlaceholderFragment> mFragment;

            /**
             * Constructor.
             * @param fragment the fragment which initiated this task
             */
            FetchDataTask(PlaceholderFragment fragment) {
                super();
                mFragment = new WeakReference<>(fragment);
            }

            /**
             * Fetches the JSON data from the server and returns it.
             * This is run in the background thread.
             * @param args the arguments passed to the background task
             * @return the JSON data from the server
             */
            @Override
            protected JSONArray doInBackground(@Nullable final Void... args) {
                JSONArray array = null;
                HttpURLConnection urlConnection = null;

                try {
                    URL url = new URL(JSON_URL);
                    urlConnection = (HttpURLConnection) url.openConnection();

                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());

                    // Can you think of a way to improve the performance of loading data
                    // using HTTP headers???
                    // ECJ: I would have to Google that - I always use Retrofit

                    // Also, Do you trust any utils thrown your way????
                    // ECJ: I only use 'industry-standard' 3rd party libraries,
                    //      if that's what you mean by "utils"

                    byte[] bytes = StreamUtils.readUnknownFully(in);

                    // Read in charset of HTTP content.
                    String charset = parseCharset(urlConnection.getRequestProperty("Content-Type"));

                    // Convert byte array to appropriate encoded string.
                    String jsonText = new String(bytes, charset);

                    // Read string as JSON.
                    array = new JSONArray(jsonText);
                } catch (IOException | JSONException e) {
                    Log.e(TAG, e.getMessage());
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
                return array;
            }

            /**
             * This is run in the UI thread when doInBackground() has completed.
             * @param array the data that was fetched in the background
             */
            @Override
            protected void onPostExecute(final JSONArray array) {
                if (mFragment != null) {
                    mFragment.get().setAdapterItems(array);
                }
            }

            /**
             * Returns the charset specified in the Content-Type of this header,
             * or the HTTP default (ISO-8859-1) if none can be found.
             */
            private static String parseCharset(String contentType) {
                if (contentType != null) {
                    String[] params = contentType.split(",");
                    for (int i = 1; i < params.length; i++) {
                        String[] pair = params[i].trim().split("=");
                        if (pair.length == 2) {
                            if (pair[0].equals("charset")) {
                                return pair[1];
                            }
                        }
                    }
                }
                return "UTF-8";
            }

        }

    }

}
