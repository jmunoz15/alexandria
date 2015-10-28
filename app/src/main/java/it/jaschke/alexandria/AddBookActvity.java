package it.jaschke.alexandria;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

public class AddBookActvity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment nextFragment;
        nextFragment = new AddBook();
        fragmentManager.beginTransaction()
                .replace(R.id.container, nextFragment)
                .commit();
    }
}
