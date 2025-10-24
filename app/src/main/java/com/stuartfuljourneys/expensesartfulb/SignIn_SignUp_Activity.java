package com.stuartfuljourneys.expensesartfulb;

import android.annotation.SuppressLint;
import android.content.Intent;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

// Import Firebase Auth
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.Executor;


public class SignIn_SignUp_Activity extends AppCompatActivity {

    private EditText emailField, nickname, passwordField; // Renamed for clarity
    private Button btnLogin, btnSignUp;
    private FirebaseAuth mAuth; // Firebase Auth instance
    private BiometricPrompt biometricPrompt;
    private Executor executor;

    @Override
    protected void onStart() {
        super.onStart();
        // Check if a user is already logged in
        if (mAuth.getCurrentUser() != null) {
            // If yes, show the biometric prompt to unlock the app
            showBiometricPrompt();
        }
        // If no user is logged in, do nothing and let them sign in manually.
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin_signup);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        executor = ContextCompat.getMainExecutor(this);

// --- Initialize the Biometric Prompt ---
        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Toast.makeText(getApplicationContext(), "Authentication succeeded!", Toast.LENGTH_SHORT).show();
                // On success, go to the main menu
                navigateToMainMenu();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
                // You could optionally sign the user out here if they fail too many times
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(), "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                // If the user cancels, they stay on the login screen
            }
        });

        // --- Find views and set listeners ---
        emailField = findViewById(R.id.inputUseremail); // Assumes this EditText is for email
        passwordField = findViewById(R.id.inputPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignUp = findViewById(R.id.btnSignUp);

        btnLogin.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            // This is the new Firebase login logic
            loginUserWithFirebase(email, password);
        });

        btnSignUp.setOnClickListener(v -> {
            // Navigate to a dedicated SignUpActivity
            // We are changing 'Other.class' to a new, more descriptive 'SignUpActivity.class'
            startActivity(new Intent(this, SignUp_Activity.class));
        });
    }

    private void showBiometricPrompt() {
        BiometricManager biometricManager = BiometricManager.from(this);
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK) != BiometricManager.BIOMETRIC_SUCCESS) {
            // If biometrics aren't available, just log the user in.
            // Or you could show a message. For now, we'll just proceed.
            navigateToMainMenu();
            return;
        }

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric login for Your App")
                .setSubtitle("Log in using your biometric credential")
                .setNegativeButtonText("Use account password") // User can cancel to log in manually
                .build();

        biometricPrompt.authenticate(promptInfo);
    }


    private void loginUserWithFirebase(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();

                        // No need to manually save username in SharedPreferences for auth state.
                        // Firebase handles the session automatically.
                        navigateToMainMenu();

                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(this, "Authentication failed. Check credentials.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToMainMenu() {
        Intent intent = new Intent(this, Other_Activity.class);
        // Add flags to clear the back stack so the user can't navigate back to the login screen
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Close the login activity
    }
}