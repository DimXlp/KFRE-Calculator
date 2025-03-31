package com.dimxlp.kfrecalculator.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dimxlp.kfrecalculator.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "RAFI|Login";
    private EditText inputEmail, inputPassword;
    private Button btnLogin;
    private TextView txtForgotPassword, txtSignup;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtForgotPassword = findViewById(R.id.txtForgotPassword);
        txtSignup = findViewById(R.id.txtSignup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = inputEmail.getText().toString().trim();
                String password = inputPassword.getText().toString().trim();

                if (TextUtils.isEmpty(email)) {
                    inputEmail.setError("Email is required");
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    inputPassword.setError("Password is required");
                    return;
                }

                Log.d(TAG, "Attempting login for: " + email);
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Login successful");
                                checkUserProfile();
                            } else {
                                Log.e(TAG, "Login failed: " + task.getException().getMessage());
                                Toast.makeText(LoginActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        txtForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Implement forgot password functionality
            }
        });

        txtSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, CreateAccountActivity.class);
                startActivity(intent);
            }
        });
    }

    private void checkUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "User is null after login");
            return;
        }

        String uid = user.getUid();
        db.collection("Users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean profileCompleted = documentSnapshot.getBoolean("profileCompleted");

                        if (profileCompleted != null && profileCompleted) {
                            Log.d(TAG, "Profile is completed. Redirecting to Dashboard.");
                            startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                        } else {
                            Log.d(TAG, "Profile incomplete. Redirecting to Profile setup.");
                            startActivity(new Intent(LoginActivity.this, CreateProfileActivity.class));
                        }
                    } else {
                        Log.d(TAG, "No user document found. Redirecting to Profile setup.");
                        startActivity(new Intent(LoginActivity.this, CreateProfileActivity.class));
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user profile: " + e.getMessage());
                    Toast.makeText(LoginActivity.this, "Error checking user profile", Toast.LENGTH_SHORT).show();
                });
    }
}