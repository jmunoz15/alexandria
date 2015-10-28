package it.jaschke.alexandria;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import it.jaschke.alexandria.api.Callback;


public class MainActivity extends AppCompatActivity implements Callback {

    public static boolean IS_TABLET = false;
    private CharSequence title;
    private BroadcastReceiver messageReciever;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IS_TABLET = isTablet();
        if (IS_TABLET) {
            setContentView(R.layout.activity_main_tablet);
        } else {
            setContentView(R.layout.activity_main);
        }

        messageReciever = new MessageReciever();
        IntentFilter filter = new IntentFilter(getString(R.string.message_event));
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReciever, filter);
        title = getTitle();

        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment nextFragment;
        nextFragment = new ListOfBooks();
        fragmentManager.beginTransaction()
                .replace(R.id.container, nextFragment)
                .addToBackStack((String) title)
                .commit();
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
                    .replace(id, fragment)
                    .addToBackStack(getString(R.string.book_detail))
                    .commit();

        }
        else{
            Intent intent = new Intent(this, BookDetailActivity.class);
            intent.putExtras(args);
            startActivity(intent);

        }


    }

    public void goBack(View view) {
        getSupportFragmentManager().popBackStack();
    }

    private boolean isTablet() {
        return (getApplicationContext().getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() < 2) {
            finish();
        }
        super.onBackPressed();
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