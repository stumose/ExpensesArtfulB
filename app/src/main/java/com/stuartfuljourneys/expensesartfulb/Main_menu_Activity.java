package com.stuartfuljourneys.expensesartfulb;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Main_menu_Activity extends BaseActivity {

    DrawerLayout drawerLayout;
    NavigationView navView;
    MaterialToolbar topAppBar;

    private TextView welcomeText;
    private Button btnEnterExpenses, btnPrevExpenses, btnViewInsights;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_menu);

        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.nav_view);
        topAppBar = findViewById(R.id.topAppBar);

        setupNavigation();

        welcomeText = findViewById(R.id.textWelcome);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // 5. Get the current user and fetch their profile
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            fetchUserNickname(currentUser.getUid());
        } else {
            // This case should ideally not happen if your login flow is correct.
            // It's a good practice to handle it anyway.
            welcomeText.setText("Welcome, Guest!");
        }

        btnEnterExpenses = findViewById(R.id.btnEnterExp);
        btnPrevExpenses = findViewById(R.id.btnPrevTransactions);
        btnViewInsights = findViewById(R.id.btnInsights);

        // Set the click listener for the "Enter Expenses" button
        btnEnterExpenses.setOnClickListener(v -> {
            // Create an Intent to start the Expenses_Enter activity. [3, 5]
            android.content.Intent intent = new android.content.Intent(Main_menu_Activity.this, Expenses_Enter_Activity.class);
            startActivity(intent);
        });

// Set the click listener for the "View Expenses" button
        btnPrevExpenses.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(Main_menu_Activity.this, Expenses_LastTransactions_Activity.class);
            startActivity(intent);
         });

// Set the click listener for the "View Insights" button
        btnViewInsights.setOnClickListener(v -> {
            // For now, just show a temporary "coming soon" message. [2, 4, 8]
            Toast.makeText(this, "Insights screen coming soon!", Toast.LENGTH_SHORT).show();
        });


    }

    /**
     * Fetches the user's nickname from Firestore using their UID.
     * @param uid The unique ID of the logged-in user.
     */
    private void fetchUserNickname(String uid) {
        db.collection("users").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // Get the nickname from the document
                            String nickname = document.getString("nickname");
                            // Set the welcome text
                            welcomeText.setText("Hello, " + nickname + "!");
                        } else {
                            // The user is authenticated but has no profile document.
                            // This can happen if the profile creation failed during sign-up.
                            Toast.makeText(this, "Could not find user profile.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Handle the error of failing to fetch the document
                        Toast.makeText(this, "Failed to get user details.", Toast.LENGTH_SHORT).show();
                    }
                });
    }


}