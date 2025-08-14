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
import com.dimxlp.kfrecalculator.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CreateProfileActivity extends AppCompatActivity {

    private static final String TAG = "RAFI|CreateProfileActivity";
    private static final String BASE_URL = "http://10.0.2.2:8080"; // emulator -> host machine
    private static final MediaType JSON = MediaType.parse("application/json");
    private final OkHttpClient http = new OkHttpClient();

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

                    prepareUserToBeMovedToWebApp(uid, firstName, lastName, role);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating profile: " + e.getMessage());
                    Toast.makeText(this, "Error updating profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void prepareUserToBeMovedToWebApp(String uid, String firstName, String lastName, Role role) {
        // 1) Reload the profile doc to get a complete Android User object
        db.collection("Users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    User appUser = doc.toObject(User.class);

                    // Fallback if mapping fails: build minimal User from current inputs
                    if (appUser == null) {
                        appUser = new User();
                        appUser.setUserId(uid);
                        appUser.setEmail(currentUser.getEmail());
                        appUser.setFirstName(firstName);
                        appUser.setLastName(lastName);
                        appUser.setFullName(firstName + " " + lastName);
                        appUser.setRole(role != null ? role.toString() : "INDIVIDUAL");
                        appUser.setCreatedAt(System.currentTimeMillis());
                        appUser.setLastLogin(System.currentTimeMillis());
                    }

                    // 2) Get a fresh Firebase ID token and sync to backend
                    User finalAppUser = appUser;
                    currentUser.getIdToken(true)
                            .addOnSuccessListener(tokenResult -> {
                                String idToken = tokenResult.getToken();

                                // Build request body exactly as backend expects (UserProfileUpsertRequest)
                                JSONObject body = buildUserUpsertBody(finalAppUser, currentUser);

                                // PUT /api/v1/users/self (Authorization: Bearer <ID_TOKEN>)
                                syncUserWithBackend(body, idToken);

                                // (Optional) smoke-check:
                                // checkCurrentAccount(idToken);

                                // 3) Proceed with navigation AFTER we kicked off the sync
                                startActivity(new Intent(CreateProfileActivity.this, DashboardActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "getIdToken failed", e);
                                // Proceed to app even if sync couldn't run now
                                startActivity(new Intent(CreateProfileActivity.this, DashboardActivity.class));
                                finish();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to reload profile doc after update", e);
                    // Proceed anyway to keep UX responsive
                    startActivity(new Intent(CreateProfileActivity.this, DashboardActivity.class));
                    finish();
                });
    }

    /** Build JSON for PUT /api/v1/users/self from Android User + FirebaseUser context. */
    private JSONObject buildUserUpsertBody(User u, FirebaseUser fu) {
        try {
            String fallbackUid = fu != null ? fu.getUid() : null;

            String userId = (u.getUserId() != null && !u.getUserId().isEmpty())
                    ? u.getUserId() : fallbackUid;

            // Fill first/last using fullName if necessary
            String firstName = u.getFirstName();
            String lastName  = u.getLastName();
            if ((firstName == null || firstName.isBlank()) && u.getFullName() != null) {
                String[] parts = u.getFullName().trim().split("\\s+", 2);
                firstName = parts[0];
                if (parts.length > 1 && (lastName == null || lastName.isBlank())) {
                    lastName = parts[1];
                }
            }
            if (firstName == null || firstName.isBlank()) firstName = "Unknown";
            if (lastName == null || lastName.isBlank())   lastName  = "Unknown";

            // Prefer FireBase email/photo if Android model misses them
            String email = (u.getEmail() != null) ? u.getEmail()
                    : (fu != null ? fu.getEmail() : null);
            String profileImageUrl = (u.getProfileImageUrl() != null) ? u.getProfileImageUrl()
                    : (fu != null && fu.getPhotoUrl() != null ? fu.getPhotoUrl().toString() : null);

            long now = System.currentTimeMillis();
            long createdAt  = (u.getCreatedAt() > 0) ? u.getCreatedAt() : now;
            long lastLogin  = (u.getLastLogin() > 0) ? u.getLastLogin() : now;

            JSONObject o = new JSONObject();
            // Must match backend DTO: UserProfileUpsertRequest
            o.put("userId", userId);
            o.put("email", email != null ? email : "");
            o.put("firstName", firstName);
            o.put("lastName", lastName);
            if (u.getFullName() != null)        o.put("fullName", u.getFullName());
            if (profileImageUrl != null)        o.put("profileImageUrl", profileImageUrl);
            if (u.getRole() != null)            o.put("role", u.getRole());      // "INDIVIDUAL" | "DOCTOR" | "ADMIN"
            if (u.getClinic() != null)          o.put("clinic", u.getClinic());
            o.put("createdAt", createdAt);      // epoch millis
            o.put("lastLogin", lastLogin);      // epoch millis

            return o;
        } catch (Exception e) {
            Log.e(TAG, "Failed to build JSON body", e);
            return new JSONObject();
        }
    }

    /** PUT /api/v1/users/self with Authorization: Bearer <ID_TOKEN>. */
    private void syncUserWithBackend(JSONObject body, String idToken) {
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/v1/users/self")
                .put(RequestBody.create(body.toString(), JSON))
                .addHeader("Authorization", "Bearer " + idToken)
                .build();

        http.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, java.io.IOException e) {
                Log.e(TAG, "PUT /users/self failed", e);
            }
            @Override public void onResponse(Call call, Response response) throws java.io.IOException {
                String resp = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "PUT /users/self -> " + response.code() + " | " + resp);
            }
        });
    }

    /** Optional smoke-check: GET /api/v1/account/current */
    private void checkCurrentAccount(String idToken) {
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/v1/account/current")
                .get()
                .addHeader("Authorization", "Bearer " + idToken)
                .build();

        http.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, java.io.IOException e) {
                Log.e(TAG, "GET /account/current failed", e);
            }
            @Override public void onResponse(Call call, Response response) throws java.io.IOException {
                String resp = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "GET /account/current -> " + response.code() + " | " + resp);
            }
        });
    }

}
