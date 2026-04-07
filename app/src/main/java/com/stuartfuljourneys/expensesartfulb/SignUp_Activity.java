package com.stuartfuljourneys.expensesartfulb;

// In SignUpActivity.java

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore; // <-- Import Firestore

import java.util.HashMap;
import java.util.Map;

public class SignUp_Activity extends AppCompatActivity {

    private EditText emailField, passwordField, nicknameField; // Add nickname field
    private Button btnRegister;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db; // <-- Add Firestore instance

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // <-- Initialize Firestore

        emailField = findViewById(R.id.inputUseremail);
        passwordField = findViewById(R.id.inputPassword);
        nicknameField = findViewById(R.id.inputNickName); // <-- Get the new field
        btnRegister = findViewById(R.id.btnSignUp);

        btnRegister.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();
            String nickname = nicknameField.getText().toString().trim(); // <-- Get nickname value

            if (email.isEmpty() || password.length() < 6 || nickname.isEmpty()) {
                Toast.makeText(this, "Please fill all fields.", Toast.LENGTH_LONG).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // User was created in Auth, now save their nickname in Firestore
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                saveNicknameToFirestore(user, nickname);
                            }
                        } else {
                            Toast.makeText(SignUp_Activity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    private void saveNicknameToFirestore(FirebaseUser user, String nickname) {
        String uid = user.getUid(); // The user's unique ID from Authentication

        // Create a new user profile map
        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("nickname", nickname);
        userProfile.put("email", user.getEmail());
        // You can add other fields here, like joinDate, etc.

        // Create a document in the "users" collection with the UID as the document ID
        db.collection("users").document(uid).set(userProfile)
                .addOnSuccessListener(aVoid -> {
                    // Profile created successfully
                    Toast.makeText(SignUp_Activity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    finish(); // Go back to the sign-in screen
                })
                .addOnFailureListener(e -> {
                    // Handle the error
                    Toast.makeText(SignUp_Activity.this, "Error saving profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
