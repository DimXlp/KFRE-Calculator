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

import com.google.android.material.tabs.TabLayoutMediator;
import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.adapter.ProfileViewPagerAdapter;
import com.dimxlp.kfrecalculator.databinding.ActivityProfileBinding;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private ActivityProfileBinding binding;
    private ImageView appLogo, profileImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Log.d(TAG, "Activity created.");

        setTopBarFunctionalities();

        // Pass intent extras to the adapter
        ProfileViewPagerAdapter adapter = new ProfileViewPagerAdapter(this);
        adapter.setFragmentArguments(getIntent().getExtras());
        binding.profileViewPager.setAdapter(adapter);

        // Link TabLayout with ViewPager2
        new TabLayoutMediator(binding.profileTabLayout, binding.profileViewPager,
                (tab, position) -> tab.setText(getTabTitle(position))
        ).attach();
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
}