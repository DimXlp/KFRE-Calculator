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

        String patientId = getIntent().getStringExtra("patientId");
        if (patientId == null) {
            Log.e(TAG, "No patientId passed to PatientDetailsActivity");
            finish();
            return;
        }
        viewPager.setAdapter(new PatientDetailsPagerAdapter(this, patientId));

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
                    fragment = new InputFragment();
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
            return 2; // Update this if you add more fragments
        }
    }
}
