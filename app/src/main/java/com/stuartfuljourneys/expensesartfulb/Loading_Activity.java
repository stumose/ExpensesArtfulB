package com.stuartfuljourneys.expensesartfulb;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;


public class Loading_Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_loading);

        new Handler().postDelayed(() -> {
            Intent intent = new Intent(Loading_Activity.this, SignIn_SignUp_Activity.class);
            startActivity(intent);
            finish();
        }, 2000); // 2 second delay


    }


}