package com.stuartfuljourneys.expensesartfulb;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import com.google.firebase.auth.FirebaseAuth;

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

            if (id == R.id.nav_menu && !(context instanceof Main_menu_Activity)) {
                startActivity(new Intent(context, Main_menu_Activity.class));
            } else if (id == R.id.nav_expenses && !(context instanceof Expenses_Enter_Activity)) {
                    startActivity(new Intent(context, Expenses_Enter_Activity.class));
            } else if (id == R.id.nav_expenses_graphs && !(context instanceof Other_Activity)) {
                startActivity(new Intent(context, Other_Activity.class));
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
            if (!(context instanceof Main_menu_Activity)) {
                startActivity(new Intent(context, Main_menu_Activity.class));
            }
            return true;
        } else if (id == R.id.action_signout) {
            // Sign out logic — for now just redirect to SignInActivity

            FirebaseAuth.getInstance().signOut();

            Intent intent = new Intent(context, SignIn_SignUp_Activity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        } else if (id == R.id.back) {
            // 👈 Handle the Back button
            finish(); // This will close the current activity and go back to the previous one
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
