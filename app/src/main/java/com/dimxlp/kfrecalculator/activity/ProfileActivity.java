package com.dimxlp.kfrecalculator.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.dimxlp.kfrecalculator.util.UserPrefs;
import com.google.android.material.tabs.TabLayoutMediator;
import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.adapter.ProfileViewPagerAdapter;
import com.dimxlp.kfrecalculator.databinding.ActivityProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class ProfileActivity extends BaseBottomNavActivity {

    private static final String TAG = "RAFI|ProfileActivity";

    public static final String EXTRA_USER_FIRST_NAME = "USER_FIRST_NAME";
    public static final String EXTRA_USER_LAST_NAME = "USER_LAST_NAME";
    public static final String EXTRA_USER_EMAIL = "USER_EMAIL";
    public static final String EXTRA_USER_ROLE = "USER_ROLE";
    public static final String EXTRA_USER_CLINIC = "USER_CLINIC";

    private ActivityProfileBinding binding;
    private ProfileViewPagerAdapter adapter;
    private TabLayoutMediator tabMediator;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private ImageView appLogo, profileImg;

    @Override protected int getBottomNavSelectedItemId() { return R.id.nav_profile; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Log.d(TAG, "Activity created.");

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setTopBarFunctionalities();

        // Pass intent extras to the adapter
        ProfileViewPagerAdapter adapter = new ProfileViewPagerAdapter(this);
        adapter.setFragmentArguments(getIntent().getExtras());
        binding.profileViewPager.setAdapter(adapter);

        // Link TabLayout with ViewPager2
        new TabLayoutMediator(binding.profileTabLayout, binding.profileViewPager,
                (tab, position) -> tab.setText(getTabTitle(position))
        ).attach();

        // Prefill via existing extras (if any) to avoid blank UI flash
        Bundle seed = getIntent() != null ? getIntent().getExtras() : null;
        setupAdapterWith(seed);

        // Always fetch the canonical user from Firestore and re-bind UI
        fetchAndRebindUser();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Auth guard for safety
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        if (current == null) {
            Log.w(TAG, "No authenticated user; redirecting to MainActivity.");
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        }
    }

    private void setTopBarFunctionalities() {
        appLogo = findViewById(R.id.profileAppLogo);
        profileImg = findViewById(R.id.profileImg);

        appLogo.setOnClickListener(v -> {
            Log.d(TAG, "App Logo clicked");
            Intent intent = new Intent(ProfileActivity.this, DashboardActivity.class);
            startActivity(intent);
            finish();
        });

        profileImg.setOnClickListener(v -> {
            Log.d(TAG, "Profile clicked");
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenuInflater().inflate(R.menu.profile_menu, popup.getMenu());

            // Force icons to show using reflection
            try {
                java.lang.reflect.Field[] fields = popup.getClass().getDeclaredFields();
                for (java.lang.reflect.Field field : fields) {
                    if ("mPopup".equals(field.getName())) {
                        field.setAccessible(true);
                        Object menuPopupHelper = field.get(popup);
                        Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                        java.lang.reflect.Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                        setForceIcons.invoke(menuPopupHelper, true);
                        break;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error showing menu icons", e);
            }

            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.menu_profile) {
                    Toast.makeText(this, getString(R.string.already_in_profile), Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.menu_logout) {
                    Log.d(TAG, "Logout clicked");
                    FirebaseAuth.getInstance().signOut();
                    UserPrefs.clear(this);
                    Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                    intent.putExtra("SHOW_LOGOUT_MESSAGE", true);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    return true;
                }
                return false;
            });

            popup.show();
        });
    }

    private String getTabTitle(int position) {
        switch (position) {
            case 0: return "Account";
            case 1: return "Preferences";
            case 2: return "Export";
        }
        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.profile_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            Log.d(TAG, "Back button pressed.");
            finish();
            return true;
        } else if (itemId == R.id.menu_profile) {
            Log.d(TAG, "Profile menu tapped while already on Profile.");
            Toast.makeText(this, getString(R.string.already_in_profile), Toast.LENGTH_SHORT).show();
            return true;

        } else if (itemId == R.id.menu_logout) {
            Log.d(TAG, "Logout clicked");
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut();

            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            intent.putExtra("SHOW_LOGOUT_MESSAGE", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return true;

        } else if (itemId == R.id.action_about) {
            Log.d(TAG, "About menu item selected.");
//            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupAdapterWith(Bundle args) {
        if (adapter != null && tabMediator != null) {
            try {
                tabMediator.detach();
            } catch (Exception ignored) {}
        }

        adapter = new ProfileViewPagerAdapter(this);
        if (args != null) {
            adapter.setFragmentArguments(new Bundle(args)); // pass a copy
        } else {
            adapter.setFragmentArguments(new Bundle());
        }
        binding.profileViewPager.setAdapter(adapter);
        // Optionally keep pages in memory if needed for quick tab switching
        binding.profileViewPager.setOffscreenPageLimit(ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT);

        tabMediator = new TabLayoutMediator(binding.profileTabLayout, binding.profileViewPager,
                (tab, position) -> tab.setText(getTabTitle(position)));
        tabMediator.attach();
    }

    private void fetchAndRebindUser() {
        FirebaseUser current = auth.getCurrentUser();
        if (current == null) {
            Log.w(TAG, "fetchAndRebindUser: current user is null");
            return;
        }

        final String uid = current.getUid();
        Log.d(TAG, "Fetching user document for uid=" + uid);

        db.collection("Users").document(uid).get()
                .addOnSuccessListener(this::onUserDocLoaded)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load user profile", e));
    }

    private void onUserDocLoaded(DocumentSnapshot doc) {
        FirebaseUser current = auth.getCurrentUser();
        if (current == null) return;

        // Start from whatever the Activity was launched with (optional prefill)
        Bundle b = getIntent() != null && getIntent().getExtras() != null
                ? new Bundle(getIntent().getExtras())
                : new Bundle();

        // Derive canonical values
        String email  = safe(doc != null ? doc.getString("email") : null);
        if (email == null || email.isEmpty()) email = safe(current.getEmail());

        String first  = safe(doc != null ? doc.getString("firstName") : null);
        String last   = safe(doc != null ? doc.getString("lastName") : null);
        String role   = safe(doc != null ? doc.getString("role") : null);
        String clinic = safe(doc != null ? doc.getString("clinic") : null);

        if (role == null || role.isEmpty()) role = "individual";
        role = role.toLowerCase(Locale.ROOT);

        // Put canonical values using the same keys the fragments expect
        b.putString(EXTRA_USER_EMAIL, email);
        b.putString(EXTRA_USER_FIRST_NAME, first);
        b.putString(EXTRA_USER_LAST_NAME, last);
        b.putString(EXTRA_USER_ROLE, role);
        b.putString(EXTRA_USER_CLINIC, clinic);

        Log.d(TAG, "Canonical user loaded: first=" + first + ", last=" + last +
                ", role=" + role + ", clinic=" + clinic + ", email=" + email);

        UserPrefs.save(
                this,
                first, last, email, role, clinic
        );

        // Recreate adapter so fragments are re-instantiated with the canonical data
        setupAdapterWith(b);
    }

    private static String safe(String s) {
        return s == null ? null : s;
    }
}