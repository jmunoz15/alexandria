package it.jaschke.alexandria;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import it.jaschke.alexandria.api.Callback;
import it.jaschke.alexandria.data.AlexandriaContract;

public class BookDetailActivity extends AppCompatActivity implements Callback {

    Fragment nextFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);
        FragmentManager fragmentManager = getSupportFragmentManager();

        nextFragment = new BookDetail();
        nextFragment.setArguments(getIntent().getExtras());
        fragmentManager.beginTransaction()
                .replace(R.id.container, nextFragment)
                .commit();
    }

    @Override
    public void onItemSelected(String ean) {

    }

    @Override
    public void onItemRemoved(String ean) {
        getContentResolver().delete(AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(ean)), null, null);
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_TEXT, ean);
        setResult(RESULT_OK, intent);
        finish();

    }
}
