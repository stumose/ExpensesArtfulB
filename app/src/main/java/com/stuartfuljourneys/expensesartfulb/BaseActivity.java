package com.stuartfuljourneys.expensesartfulb;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

// BaseActivity.java
public abstract class BaseActivity extends AppCompatActivity {

    protected DrawerLayout drawerLayout;
    protected NavigationView navView;
    protected MaterialToolbar topAppBar; // Or Toolbar if you're not using Material

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void setupNavigation() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.nav_view);
        topAppBar = findViewById(R.id.topAppBar);
        setSupportActionBar(topAppBar); // <-- IMPORTANT


        topAppBar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            Context context = BaseActivity.this;

            if (id == R.id.nav_menu && !(context instanceof Main_menu)) {
                startActivity(new Intent(context, Main_menu.class));
            } else if (id == R.id.nav_expenses && !(context instanceof Expenses_Enter)) {
                    startActivity(new Intent(context, Expenses_Enter.class));
            } else if (id == R.id.nav_expenses_graphs && !(context instanceof Other)) {
                startActivity(new Intent(context, Other.class));
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.top_app_bar_menu, menu); // Inflate your menu XML
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        int id = item.getItemId();
        Context context = BaseActivity.this;

        if (id == R.id.action_home) {
            if (!(context instanceof Main_menu)) {
                startActivity(new Intent(context, Main_menu.class));
            }
            return true;
        } else if (id == R.id.action_signout) {
            // Sign out logic — for now just redirect to SignInActivity
            Intent intent = new Intent(context, SignIn_SignUp.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return true;
        } else if (id == R.id.back) {
            // 👈 Handle the Back button
            finish(); // This will close the current activity and go back to the previous one
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
