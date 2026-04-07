package com.stuartfuljourneys.expensesartfulb;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

public class Other_Activity extends BaseActivity {

    DrawerLayout drawerLayout;
    NavigationView navView;
    MaterialToolbar topAppBar;

    Button btnOne, btnTwo;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_other);

        // === START: Import verbs from Google Sheet CSV ===
        // MOved to MainMenuActivity
        // === END: CSV Import ===

        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.nav_view);
        topAppBar = findViewById(R.id.topAppBar);

        setupNavigation();



        btnOne = findViewById(R.id.btnOne);
        btnTwo = findViewById(R.id.btnTwo);

        btnOne.setOnClickListener(v ->
                startActivity(new Intent(this, Main_menu_Activity.class)
                ));

    }
}