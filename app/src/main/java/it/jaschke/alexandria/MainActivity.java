package it.jaschke.alexandria;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.widget.Toast;

import it.jaschke.alexandria.api.BookSearchAdapter;
import it.jaschke.alexandria.api.Callback;
import it.jaschke.alexandria.data.AlexandriaContract;


public class MainActivity extends AppCompatActivity implements Callback, LoaderManager.LoaderCallbacks<Cursor> {

    private final int LOADER_ID = 10;
    private final int DETAIL_RQ = 0;

    private final String DETAIL_TAG = "Detail";

    private CharSequence title;
    private BroadcastReceiver messageReciever;
    private BookSearchAdapter mSearchAdapter;
    private String mSearchText;
    private Fragment nextFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        messageReciever = new MessageReciever();
        IntentFilter filter = new IntentFilter(getString(R.string.message_event));
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReciever, filter);
        title = getTitle();

        FragmentManager fragmentManager = getSupportFragmentManager();

        nextFragment = new ListOfBooks();
        fragmentManager.beginTransaction()
                .replace(R.id.container, nextFragment)
                .addToBackStack((String) title)
                .commit();

        final String selection = AlexandriaContract.BookEntry.TITLE + " =? ";
        Cursor cursor = getContentResolver().query(
                AlexandriaContract.BookEntry.CONTENT_URI,
                null, // leaving "columns" null just returns all the columns.
                selection, // cols for "where" clause
                new String[]{"-1"}, // values for "where" clause
                null  // sort order
        );

        mSearchAdapter = new BookSearchAdapter(this, cursor, 0);
    }

    public void updateFirstItem(String ean) {
        if (findViewById(R.id.right_container) != null) {
            onItemSelected(ean);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        searchView.setSuggestionsAdapter(mSearchAdapter);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mSearchText = newText;
                MainActivity.this.restartLoader();
                return false;
            }
        });
        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                Cursor cursor = mSearchAdapter.getCursor();
                onItemSelected(cursor.getString(cursor.getColumnIndex(AlexandriaContract.BookEntry._ID)));

                return false;
            }
        });


        return true;

    }


    public void setTitle(int titleId) {
        title = getString(titleId);
    }


    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReciever);
        super.onDestroy();
    }

    @Override
    public void onItemSelected(String ean) {
        Bundle args = new Bundle();
        args.putString(getString(R.string.ean_key), ean);

        if (findViewById(R.id.right_container) != null) {
            int id = R.id.right_container;
            BookDetail fragment = new BookDetail();
            fragment.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                    .replace(id, fragment, DETAIL_TAG)
                    .commit();
        } else {
            Intent intent = new Intent(this, BookDetailActivity.class);
            intent.putExtras(args);
            startActivityForResult(intent, DETAIL_RQ);
        }


    }

    private void restartLoader() {
        getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final String selection = AlexandriaContract.BookEntry.TITLE + " LIKE ? OR " + AlexandriaContract.BookEntry.SUBTITLE + " LIKE ? ";


        if (mSearchText.length() > 0) {
            mSearchText = "%" + mSearchText + "%";
            return new CursorLoader(
                    this,
                    AlexandriaContract.BookEntry.CONTENT_URI,
                    null,
                    selection,
                    new String[]{mSearchText, mSearchText},
                    null
            );
        }

        final String selectionEmpty = AlexandriaContract.BookEntry.TITLE + " =? ";
        return new CursorLoader(
                this,
                AlexandriaContract.BookEntry.CONTENT_URI,
                null,
                selectionEmpty,
                new String[]{"-1"},
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mSearchAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void onItemRemoved(String ean) {
        getContentResolver().delete(AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(ean)), null, null);
        ((ListOfBooks) nextFragment).restartLoader();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == DETAIL_RQ) && (resultCode == RESULT_OK)) {
            getContentResolver().delete(AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(data.getStringExtra(Intent.EXTRA_TEXT))), null, null);
            ((ListOfBooks) nextFragment).restartLoader();
        }
    }

    private class MessageReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getStringExtra(getString(R.string.message_key)) != null) {
                Toast.makeText(MainActivity.this, intent.getStringExtra(getString(R.string.message_key)), Toast.LENGTH_LONG).show();
            }
        }
    }
}