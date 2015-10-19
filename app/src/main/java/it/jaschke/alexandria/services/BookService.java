package it.jaschke.alexandria.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import it.jaschke.alexandria.MainActivity;
import it.jaschke.alexandria.R;
import it.jaschke.alexandria.data.AlexandriaContract;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 */
public class BookService extends IntentService {

    private static final String LOG_TAG = BookService.class.getSimpleName();
    private static final String SERVICE_ID = "Alexandria";
    private static final int ISBN_LENGTH = 13;

    public BookService() {
        super(SERVICE_ID);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            final String ean = intent.getStringExtra(getString(R.string.ean));
            if (getString(R.string.fetch_book).equals(action)) {
                fetchBook(ean);
            } else if (getString(R.string.delete_book).equals(action)) {
                deleteBook(ean);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void deleteBook(String ean) {
        if (ean != null) {
            getContentResolver().delete(AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(ean)), null, null);
        }
    }

    /**
     * Handle action fetchBook in the provided background thread with the provided
     * parameters.
     */
    private void fetchBook(String ean) {
        int responseCode = -1;

        if (ean.length() != ISBN_LENGTH) {
            return;
        }

        Cursor bookEntry = getContentResolver().query(
                AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(ean)),
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );

        if (bookEntry.getCount() > 0) {
            bookEntry.close();
            return;
        }

        bookEntry.close();

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String bookJsonString = null;

        try {
            final String ISBN_PARAM = getString(R.string.isbn) + ean;

            Uri builtUri = Uri.parse(getString(R.string.forecast_base_url)).buildUpon()
                    .appendQueryParameter(getString(R.string.query_param), ISBN_PARAM)
                    .build();

            URL url = new URL(builtUri.toString());

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod(getString(R.string.get));
            urlConnection.connect();
            responseCode = urlConnection.getResponseCode();
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                return;
            }

            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
                buffer.append("\n");
            }

            if (buffer.length() == 0) {
                return;
            }
            bookJsonString = buffer.toString();
        } catch (Exception e) {
            Log.e(LOG_TAG, getString(R.string.generic_error), e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, getString(R.string.stream_error), e);
                }
            }

        }
        try {
            if(responseCode == HttpURLConnection.HTTP_OK ) {
                JSONObject bookJson = new JSONObject(bookJsonString);
                JSONArray bookArray;
                if (bookJson.has(getString(R.string.items))) {
                    bookArray = bookJson.getJSONArray(getString(R.string.items));
                } else {
                    Intent messageIntent = new Intent(getString(R.string.message_event));
                    messageIntent.putExtra(getString(R.string.message_key), getResources().getString(R.string.not_found));
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);
                    return;
                }

                JSONObject bookInfo = ((JSONObject) bookArray.get(0)).getJSONObject(getString(R.string.volume_info));

                String title = bookInfo.getString(getString(R.string.title));

                String subtitle = "";
                if (bookInfo.has(getString(R.string.subtitle))) {
                    subtitle = bookInfo.getString(getString(R.string.subtitle));
                }

                String desc = "";
                if (bookInfo.has(getString(R.string.description))) {
                    desc = bookInfo.getString(getString(R.string.description));
                }

                String imgUrl = "";
                if (bookInfo.has(getString(R.string.image_url_path))
                        && bookInfo.getJSONObject(getString(R.string.image_url_path)).has(getString(R.string.thumbnail))) {
                    imgUrl = bookInfo.getJSONObject(getString(R.string.image_url_path)).getString(getString(R.string.thumbnail));
                }

                writeBackBook(ean, title, subtitle, desc, imgUrl);

                if (bookInfo.has(getString(R.string.authors))) {
                    writeBackAuthors(ean, bookInfo.getJSONArray(getString(R.string.authors)));
                }
                if (bookInfo.has(getString(R.string.categories))) {
                    writeBackCategories(ean, bookInfo.getJSONArray(getString(R.string.categories)));
                }
            }
            else{
                return;
            }

        } catch (JSONException e) {
            Log.e(LOG_TAG, getString(R.string.generic_error), e);
        }
    }

    private void writeBackBook(String ean, String title, String subtitle, String desc, String imgUrl) {
        ContentValues values = new ContentValues();
        values.put(AlexandriaContract.BookEntry._ID, ean);
        values.put(AlexandriaContract.BookEntry.TITLE, title);
        values.put(AlexandriaContract.BookEntry.IMAGE_URL, imgUrl);
        values.put(AlexandriaContract.BookEntry.SUBTITLE, subtitle);
        values.put(AlexandriaContract.BookEntry.DESC, desc);
        getContentResolver().insert(AlexandriaContract.BookEntry.CONTENT_URI, values);
    }

    private void writeBackAuthors(String ean, JSONArray jsonArray) throws JSONException {
        ContentValues values = new ContentValues();
        for (int i = 0; i < jsonArray.length(); i++) {
            values.put(AlexandriaContract.AuthorEntry._ID, ean);
            values.put(AlexandriaContract.AuthorEntry.AUTHOR, jsonArray.getString(i));
            getContentResolver().insert(AlexandriaContract.AuthorEntry.CONTENT_URI, values);
            values = new ContentValues();
        }
    }

    private void writeBackCategories(String ean, JSONArray jsonArray) throws JSONException {
        ContentValues values = new ContentValues();
        for (int i = 0; i < jsonArray.length(); i++) {
            values.put(AlexandriaContract.CategoryEntry._ID, ean);
            values.put(AlexandriaContract.CategoryEntry.CATEGORY, jsonArray.getString(i));
            getContentResolver().insert(AlexandriaContract.CategoryEntry.CONTENT_URI, values);
            values = new ContentValues();
        }
    }
}