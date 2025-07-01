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
import com.dimxlp.kfrecalculator.fragment.QuickCkdEpiCalculatorFragment;
import com.dimxlp.kfrecalculator.fragment.QuickKfreCalculatorFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;

public class DashboardQuickCalculationActivity extends AppCompatActivity {

    private static final String TAG = "RAFI|DashboardQuickCalculation";

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ImageView logoButton, profileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_quick_calculation);

        initViews();
        setTopBarFunctionalities();
        setupViewPager();

        Log.d(TAG, "DashboardQuickCalculationActivity initialized.");
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        logoButton = findViewById(R.id.logoButton);
        profileImage = findViewById(R.id.profileImage);
    }

    private void setupViewPager() {
        FragmentStateAdapter pagerAdapter = new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                if (position == 0)
                    return new QuickKfreCalculatorFragment();
                else
                    return new QuickCkdEpiCalculatorFragment();
            }

            @Override
            public int getItemCount() {
                return 2;
            }
        };

        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(position == 0 ? "KFRE" : "CKD-EPI")
        ).attach();
    }

    private void setTopBarFunctionalities() {
        logoButton.setOnClickListener(v -> {
            Log.d(TAG, "Back button clicked, finishing activity.");
            finish();
        });

        profileImage.setOnClickListener(v -> {
            Log.d(TAG, "Profile clicked, showing profile menu.");
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenuInflater().inflate(R.menu.profile_menu, popup.getMenu());

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
                    Toast.makeText(this, "Profile Activity coming soon", Toast.LENGTH_SHORT).show();
                } else if (itemId == R.id.menu_logout) {
                    FirebaseAuth.getInstance().signOut();
                    Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
                return false;
            });

            popup.show();
        });
    }
}