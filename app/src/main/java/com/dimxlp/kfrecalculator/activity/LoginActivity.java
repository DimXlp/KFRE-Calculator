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
import com.dimxlp.kfrecalculator.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "RAFI|Login";
    private static final String BASE_URL = "http://10.0.2.2:8080"; // emulator -> host machine
    private static final MediaType JSON = MediaType.parse("application/json");
    private final OkHttpClient http = new OkHttpClient();
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
                            prepareUserToBeMovedToWebApp(documentSnapshot, uid, user);
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

    private void prepareUserToBeMovedToWebApp(DocumentSnapshot documentSnapshot, String uid, FirebaseUser user) {
        // RETURNING USER → sync to backend here
        User appUser =
                documentSnapshot.toObject(User.class);

        // Defensive fallback if mapping fails
        if (appUser == null) {
            appUser = new User();
            appUser.setUserId(uid);
            appUser.setEmail(user.getEmail());
            // if you store displayName/photo in FirebaseUser, you can copy them:
            if (user.getDisplayName() != null) {
                appUser.setFullName(user.getDisplayName());
            }
            if (user.getPhotoUrl() != null) {
                appUser.setProfileImageUrl(user.getPhotoUrl().toString());
            }
            appUser.setCreatedAt(System.currentTimeMillis());
        }
        // Always update lastLogin on each successful login
        appUser.setLastLogin(System.currentTimeMillis());

        // Get fresh ID token and sync to backend
        User finalAppUser = appUser;
        user.getIdToken(true)
                .addOnSuccessListener(tokenResult -> {
                    String idToken = tokenResult.getToken();
                    JSONObject body = buildUserUpsertBody(finalAppUser, user);
                    syncUserWithBackend(body, idToken);

                    // (Optional) smoke-check:
                    // checkCurrentAccount(idToken);

                    // Continue app flow (e.g., go to Dashboard)
                    startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getIdToken failed", e);
                    Log.d(TAG, "Profile is completed. Redirecting to Dashboard.");
                    // Continue anyway to keep UX responsive
                    startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
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
            if (u.getRole() != null)            o.put("role", u.getRole());      // "INDIVIDUAL" | "DOCTOR"
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