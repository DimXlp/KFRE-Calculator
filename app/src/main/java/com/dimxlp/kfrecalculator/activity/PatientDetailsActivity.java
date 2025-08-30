package com.dimxlp.kfrecalculator.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.fragment.PatientCkdEpiCalculatorFragment;
import com.dimxlp.kfrecalculator.fragment.PatientKfreCalculatorFragment;
import com.dimxlp.kfrecalculator.fragment.PatientDetailsFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class PatientDetailsActivity extends AppCompatActivity {

    private static final String TAG = "RAFI|PatientDetails";

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ImageView logoButton, profileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_details);

        tabLayout = findViewById(R.id.patientDetailTabLayout);
        viewPager = findViewById(R.id.patientDetailViewPager);

        String patientId = getIntent().getStringExtra("patientId");
        if (patientId == null) {
            Log.e(TAG, "No patientId passed to PatientDetailsActivity");
            finish();
            return;
        }

        initViews();
        setTopBarFunctionalities();

        viewPager.setAdapter(new PatientDetailsPagerAdapter(this, patientId));

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Info");
                    break;
                case 1:
                    tab.setText("KFRE");
                    break;
                case 2:
                    tab.setText("CKD-EPI");
                    break;
            }
        }).attach();

        Log.d(TAG, "PatientDetailsActivity initialized");
    }

    private void initViews() {
        tabLayout = findViewById(R.id.patientDetailTabLayout);
        viewPager = findViewById(R.id.patientDetailViewPager);
        logoButton = findViewById(R.id.patientDetailLogo);
        profileImage = findViewById(R.id.patientDetailProfileImg);
    }

    private void setTopBarFunctionalities() {
        logoButton.setOnClickListener(v -> {
            Log.d(TAG, "App Logo clicked");
            Intent intent = new Intent(PatientDetailsActivity.this, DashboardActivity.class);
            startActivity(intent);
            finish();
        });

        profileImage.setOnClickListener(v -> {
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
                    Log.d(TAG, "Profile menu item clicked. Starting ProfileActivity.");
                    Intent intent = new Intent(PatientDetailsActivity.this, ProfileActivity.class);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.menu_logout) {
                    Log.d(TAG, "Logout clicked");
                    FirebaseAuth.getInstance().signOut();

                    Intent intent = new Intent(PatientDetailsActivity.this, MainActivity.class);
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

    private static class PatientDetailsPagerAdapter extends FragmentStateAdapter {
        private final String patientId;

        public PatientDetailsPagerAdapter(@NonNull AppCompatActivity activity, String patientId) {
            super(activity);
            this.patientId = patientId;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Bundle args = new Bundle();
            args.putString("patientId", patientId);

            Fragment fragment;
            switch (position) {
                case 0:
                    fragment = new PatientDetailsFragment();
                    break;
                case 1:
                    fragment = new PatientKfreCalculatorFragment();
                    break;
                case 2:
                    fragment = new PatientCkdEpiCalculatorFragment();
                    break;
                default:
                    fragment = new PatientDetailsFragment();
                    break;
            }

            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
