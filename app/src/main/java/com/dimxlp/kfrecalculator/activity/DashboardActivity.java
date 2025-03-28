package com.dimxlp.kfrecalculator.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.adapter.RecentPatientAdapter;
import com.dimxlp.kfrecalculator.enumeration.Risk;
import com.dimxlp.kfrecalculator.model.Patient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "RAFI|Dashboard";
    private TextView tvDoctorName;
    private LinearLayout addPatientAction, quickCalcAction, viewAllAction, exportDataAction;
    private RecyclerView recentRecView;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Firebase
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        // UI references
        tvDoctorName = findViewById(R.id.dashboardDoctorName);
        addPatientAction = findViewById(R.id.dashboardAddPatientAction);
        quickCalcAction = findViewById(R.id.dashboardQuickCalcAction);
        viewAllAction = findViewById(R.id.dashboardViewAllAction);
        exportDataAction = findViewById(R.id.dashboardExportAction);
        recentRecView = findViewById(R.id.dashboardRecentRecView);
        ImageView profileImg = findViewById(R.id.dashboardImgProfile);

        // Set up RecyclerView
        recentRecView.setLayoutManager(new LinearLayoutManager(this));
        List<Patient> dummyList = generateDummyRecentPatients();
        RecentPatientAdapter adapter = new RecentPatientAdapter(dummyList);
        recentRecView.setAdapter(adapter);

        // Set greeting name
        if (currentUser != null) {
            String uid = currentUser.getUid();
            db.collection("Users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("firstName");
                            tvDoctorName.setText(name != null ? name : "Doctor");
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to load user data", e));
        }

        // Click listeners
        addPatientAction.setOnClickListener(v -> {
            Log.d(TAG, "Add New Patient clicked");
            Toast.makeText(this, "Add Patient screen coming soon", Toast.LENGTH_SHORT).show();
        });

        quickCalcAction.setOnClickListener(v -> {
            Log.d(TAG, "Quick Calculator clicked");
            Toast.makeText(this, "Quick Calculator coming soon", Toast.LENGTH_SHORT).show();
        });

        viewAllAction.setOnClickListener(v -> {
            Log.d(TAG, "View All Patients clicked");
            Toast.makeText(this, "View All Patients screen coming soon", Toast.LENGTH_SHORT).show();
        });

        exportDataAction.setOnClickListener(v -> {
            Log.d(TAG, "Export Data clicked");
            Toast.makeText(this, "Export functionality coming soon", Toast.LENGTH_SHORT).show();
        });

        profileImg.setOnClickListener(v -> {
            Log.d(TAG, "Profile image clicked");
            Toast.makeText(this, "Profile details coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private List<Patient> generateDummyRecentPatients() {
        List<Patient> patients = new ArrayList<>();

        patients.add(new Patient("John Doe", "1959-03-22", System.currentTimeMillis(), Risk.HIGH));
        patients.add(new Patient("Maria Koutra", "1954-06-10", System.currentTimeMillis() - 172800000, Risk.MEDIUM));
        patients.add(new Patient("George Xlp", "1952-01-15", System.currentTimeMillis() - 259200000, Risk.LOW)); //

        return patients;
    }

}
