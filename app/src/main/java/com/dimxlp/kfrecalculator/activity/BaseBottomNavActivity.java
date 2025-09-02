package com.dimxlp.kfrecalculator.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.util.SparseArray;

import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.util.UserPrefs;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BaseBottomNavActivity extends AppCompatActivity {

    protected BottomNavigationView bottomNav;
    private final SparseArray<Class<?>> navRoutes = new SparseArray<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupBottomNav();
    }

    protected void setupBottomNav() {
        bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null) return;

        String role = UserPrefs.role(this);
        boolean isDoctor = "doctor".equalsIgnoreCase(role);

        bottomNav.getMenu().clear();
        bottomNav.inflateMenu(isDoctor ? R.menu.bottom_nav_doctor : R.menu.bottom_nav_individual);

        buildRoutes(isDoctor);

        bottomNav.setSelectedItemId(getBottomNavSelectedItemId());
        bottomNav.setOnItemSelectedListener(this::handleNavSelection);
    }

    private void buildRoutes(boolean isDoctor) {
        navRoutes.clear();
        navRoutes.put(R.id.nav_dashboard, dashboardActivityClass());
        if (isDoctor) {
            navRoutes.put(R.id.nav_patients, patientsActivityClass());
        } else {
            navRoutes.put(R.id.nav_calculations, calculationsActivityClass());
        }
        navRoutes.put(R.id.nav_quick_calc, quickCalcActivityClass());
        navRoutes.put(R.id.nav_export, exportActivityClass());
    }

    protected @IdRes int getBottomNavSelectedItemId() {
        return R.id.nav_dashboard;
    }

    private boolean handleNavSelection(MenuItem item) {
        int id = item.getItemId();
        if (id == getBottomNavSelectedItemId()) return true;

        Class<?> target = navRoutes.get(id);
        return target != null && navigateTo(target);
    }

    private boolean navigateTo(Class<?> target) {
        try {
            Intent i = new Intent(this, target).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            return true;
        } catch (Exception e) {
            Log.e("RAFI|BaseBottomNav", "Failed to navigate to " + target, e);
            return false;
        }
    }

    // Override these if your classes differ
    protected Class<?> dashboardActivityClass() { return com.dimxlp.kfrecalculator.activity.DashboardActivity.class; }
    protected Class<?> patientsActivityClass() { return com.dimxlp.kfrecalculator.activity.PatientListActivity.class; }
    protected Class<?> calculationsActivityClass() { return com.dimxlp.kfrecalculator.activity.DashboardActivity.class; }
    protected Class<?> quickCalcActivityClass() { return com.dimxlp.kfrecalculator.activity.DashboardQuickCalculationActivity.class; }
    protected Class<?> exportActivityClass() { return com.dimxlp.kfrecalculator.activity.DashboardActivity.class; }
}
