package com.plista.demo.infeed;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.plista.demo.infeed.google_ima.GoogleIMAActivity;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

    }

    //
    // Click Handlers
    //

    public void startGoogleIMAActivity(View view) {
        Log.d(TAG, "startGoogleIMAActivity()");

        Intent intent = new Intent(this, GoogleIMAActivity.class);
        startActivity(intent);
    }

    public void startJWPlayerActivity(View view) {
        Log.d(TAG, "startJWPlaxerActivity()");
    }
}
