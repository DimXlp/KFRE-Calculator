package com.dimxlp.kfrecalculator.activity;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.dimxlp.kfrecalculator.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "RAFI|MainActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: MainActivity started");

        setContentView(R.layout.activity_main);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        Log.d(TAG, "Firebase initialized");

        // Ensure the NavHostFragment is retrieved correctly
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            Log.d(TAG, "NavHostFragment found and NavController initialized");

            // Set up Bottom Navigation with Navigation Controller
            BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_nav);
            NavigationUI.setupWithNavController(bottomNavigationView, navController);
            Log.d(TAG, "BottomNavigationView linked with NavController");
        } else {
            Log.e(TAG, "NavHostFragment is NULL - Navigation setup failed");
        }
    }
}
