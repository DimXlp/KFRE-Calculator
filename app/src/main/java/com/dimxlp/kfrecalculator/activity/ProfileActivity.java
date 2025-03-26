package com.dimxlp.kfrecalculator.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dimxlp.kfrecalculator.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "RAFI|ProfileActivity";

    private EditText inputFirstName, inputLastName;
    private Spinner spinnerRole;
    private Button btnSaveProfile, btnUploadPicture;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Log.e(TAG, "No authenticated user found");
            finish();
            return;
        }

        // Initialize views
        inputFirstName = findViewById(R.id.inputFirstName);
        inputLastName = findViewById(R.id.inputLastName);
        spinnerRole = findViewById(R.id.spinnerRole);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnUploadPicture = findViewById(R.id.btnUploadPicture);

        // Setup role spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.roles_array,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);

        // Enable Save button only if required fields are filled
        TextWatcher inputWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkEnableSave();
            }
        };

        inputFirstName.addTextChangedListener(inputWatcher);
        inputLastName.addTextChangedListener(inputWatcher);

        // Save Profile
        btnSaveProfile.setOnClickListener(v -> saveProfile());

        // Upload picture (stub)
        btnUploadPicture.setOnClickListener(v -> {
            Toast.makeText(this, "Profile picture upload not implemented yet", Toast.LENGTH_SHORT).show();
        });

        Log.d(TAG, "ProfileActivity initialized for: " + currentUser.getEmail());
    }

    private void checkEnableSave() {
        String firstName = inputFirstName.getText().toString().trim();
        String lastName = inputLastName.getText().toString().trim();
        String role = spinnerRole.getSelectedItem().toString();

        boolean validRoleSelected = !role.equals("Select Role");
        boolean enable = !firstName.isEmpty() && !lastName.isEmpty() && validRoleSelected;

        btnSaveProfile.setEnabled(enable);
    }

    private void saveProfile() {
        String uid = currentUser.getUid();
        String firstName = inputFirstName.getText().toString().trim();
        String lastName = inputLastName.getText().toString().trim();
        String role = spinnerRole.getSelectedItem().toString();
        String email = currentUser.getEmail();

        Map<String, Object> profile = new HashMap<>();
        profile.put("firstName", firstName);
        profile.put("lastName", lastName);
        profile.put("fullName", firstName + " " + lastName);
        profile.put("email", email);
        profile.put("role", role);
        profile.put("profileCompleted", true);
        profile.put("updatedAt", System.currentTimeMillis());

        db.collection("Users").document(uid)
                .set(profile)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "User profile saved");
                    Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show();

                    // TODO: Redirect to DashboardActivity or HomeActivity
                    // startActivity(new Intent(ProfileActivity.this, DashboardActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving profile: " + e.getMessage());
                    Toast.makeText(this, "Error saving profile", Toast.LENGTH_SHORT).show();
                });
    }
}
