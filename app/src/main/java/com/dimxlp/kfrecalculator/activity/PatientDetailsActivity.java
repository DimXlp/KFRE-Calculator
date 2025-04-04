package com.dimxlp.kfrecalculator.activity;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.fragment.InputFragment;
import com.dimxlp.kfrecalculator.fragment.PatientDetailsFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class PatientDetailsActivity extends AppCompatActivity {

    private static final String TAG = "RAFI|PatientDetail";

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_details);

        tabLayout = findViewById(R.id.patientDetailTabLayout);
        viewPager = findViewById(R.id.patientDetailViewPager);

        viewPager.setAdapter(new PatientDetailsPagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Info");
                    break;
                case 1:
                    tab.setText("Calculator");
                    break;
                // You can add more tabs here (e.g., History, Notes)
            }
        }).attach();

        Log.d(TAG, "PatientDetailsActivity initialized");
    }

    private static class PatientDetailsPagerAdapter extends FragmentStateAdapter {

        public PatientDetailsPagerAdapter(@NonNull AppCompatActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new PatientDetailsFragment();
                case 1:
                    return new InputFragment();  // You already have this fragment
                default:
                    return new PatientDetailsFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2; // Update this if you add more fragments
        }
    }
}
