package com.dimxlp.kfrecalculator.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.enumeration.Role;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreateAccountActivity extends AppCompatActivity {

    private static final String TAG = "RAFI|CreateAccount";
    private EditText inputFirstName, inputLastName, inputEmail, inputPassword;
    private Button btnCreateAccount;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        inputFirstName = findViewById(R.id.createProfileFirstNameInput);
        inputLastName = findViewById(R.id.createProfileLastNameInput);
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);

        btnCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createAccount();
            }
        });
    }

    private void createAccount() {
        String firstName = inputFirstName.getText().toString().trim();
        String lastName = inputLastName.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(firstName)) {
            inputFirstName.setError("First Name is required");
            return;
        }
        if (TextUtils.isEmpty(lastName)) {
            inputLastName.setError("Last Name is required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            inputEmail.setError("Email is required");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmail.setError("Enter a valid email address");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            inputPassword.setError("Password is required");
            return;
        }
        if (password.length() < 6) {
            inputPassword.setError("Password must be at least 6 characters");
            return;
        }

        Log.d(TAG, "Creating account for: " + email);

        // Create user in Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Account creation successful
                        FirebaseUser user = mAuth.getCurrentUser();
                        Log.d(TAG, "Account creation successful for: " + user.getUid());

                        // Send email verification
//                        user.sendEmailVerification()
//                                .addOnCompleteListener(task1 -> {
//                                    if (task1.isSuccessful()) {
//                                        Log.d(TAG, "Verification email sent.");
//                                        Toast.makeText(CreateAccountActivity.this, "Verification email sent. Please check your inbox.", Toast.LENGTH_LONG).show();
//                                    } else {
//                                        Log.e(TAG, "Failed to send verification email: " + task1.getException().getMessage());
//                                    }
//                                });

                        // Save user data to Firestore
                        saveUserToDatabase(user.getUid(), firstName, lastName, email);
                    } else {
                        // Account creation failed
                        Log.e(TAG, "Account creation failed: " + task.getException().getMessage());
                        Toast.makeText(CreateAccountActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToDatabase(String userId, String firstName, String lastName, String email) {
        Map<String, Object> user = new HashMap<>();
        user.put("firstName", firstName);
        user.put("lastName", lastName);
        user.put("fullName", firstName + " " + lastName);
        user.put("email", email);
        user.put("createdAt", System.currentTimeMillis());
        user.put("role", Role.INDIVIDUAL.toString());

        db.collection("Users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User successfully added to Firestore");
                    Toast.makeText(CreateAccountActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(CreateAccountActivity.this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding user to Firestore: " + e.getMessage());
                    Toast.makeText(CreateAccountActivity.this, "Error saving user data", Toast.LENGTH_SHORT).show();
                });
    }
}
