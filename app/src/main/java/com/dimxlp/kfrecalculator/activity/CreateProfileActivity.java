package com.dimxlp.kfrecalculator.activity;

import android.content.Intent;
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
import com.dimxlp.kfrecalculator.enumeration.Role;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CreateProfileActivity extends AppCompatActivity {

    private static final String TAG = "RAFI|CreateProfileActivity";

    private EditText inputFirstName, inputLastName;
    private Spinner spinnerRole;
    private Button btnSaveProfile, btnUploadPicture;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Log.e(TAG, "No authenticated user found");
            finish();
            return;
        }

        // Initialize views
        inputFirstName = findViewById(R.id.createProfileFirstNameInput);
        inputLastName = findViewById(R.id.createProfileLastNameInput);
        spinnerRole = findViewById(R.id.createProfileRoleSpinner);
        btnSaveProfile = findViewById(R.id.createProfileSaveBtn);
        btnUploadPicture = findViewById(R.id.createProfileUploadPicBtn);

        // Setup role spinner
        List<String> roles = Arrays.stream(Role.values())
                .map(Role::toString)
                .collect(Collectors.toList());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);
        spinnerRole.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                checkEnableSave();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                checkEnableSave();
            }
        });

        loadUserInfo();

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

    private void loadUserInfo() {
        String uid = currentUser.getUid();

        db.collection("Users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");
                        Role role = Role.fromString(documentSnapshot.getString("role"));

                        if (firstName != null) inputFirstName.setText(firstName);
                        if (lastName != null) inputLastName.setText(lastName);

                        if (role != null) {
                            ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spinnerRole.getAdapter();
                            int position = adapter.getPosition(role.toString());
                            if (position >= 0) spinnerRole.setSelection(position);
                        }

                        Log.d(TAG, "User info loaded into profile form");
                    } else {
                        Log.d(TAG, "No user document found to pre-fill profile");
                    }

                    checkEnableSave();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user info: " + e.getMessage());
                    Toast.makeText(this, "Could not load profile info", Toast.LENGTH_SHORT).show();
                });
    }


    private void checkEnableSave() {
        String firstName = inputFirstName.getText().toString().trim();
        String lastName = inputLastName.getText().toString().trim();
        String role = spinnerRole.getSelectedItem().toString();

        boolean validRoleSelected = !role.equals("Select Role");
        boolean enable = !firstName.isEmpty() && !lastName.isEmpty() && validRoleSelected;

        btnSaveProfile.setEnabled(enable);

        Log.d(TAG, "Save enabled: " + btnSaveProfile.isEnabled());
    }

    private void saveProfile() {
        Log.d(TAG, "Button clicked");
        String uid = currentUser.getUid();
        String firstName = inputFirstName.getText().toString().trim();
        String lastName = inputLastName.getText().toString().trim();
        Role role = Role.fromString(spinnerRole.getSelectedItem().toString());

        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", firstName);
        updates.put("lastName", lastName);
        updates.put("fullName", firstName + " " + lastName);
        updates.put("role", role.toString());
        updates.put("profileCompleted", true);
        updates.put("updatedAt", System.currentTimeMillis());

        db.collection("Users").document(uid)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "User profile updated");
                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();

                    startActivity(new Intent(CreateProfileActivity.this, DashboardActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating profile: " + e.getMessage());
                    Toast.makeText(this, "Error updating profile", Toast.LENGTH_SHORT).show();
                });
    }
}
