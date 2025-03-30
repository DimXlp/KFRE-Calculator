package com.dimxlp.kfrecalculator.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.adapter.RecentCalculationAdapter;
import com.dimxlp.kfrecalculator.adapter.RecentPatientAdapter;
import com.dimxlp.kfrecalculator.enumeration.Risk;
import com.dimxlp.kfrecalculator.model.Calculation;
import com.dimxlp.kfrecalculator.model.Patient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "RAFI|Dashboard";

    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    // Header Views
    private TextView userRole, userName;
    private ImageView profileImg;

    // Doctor Stats
    private CardView doctorStatsCard, doctorRecentCard;
    private TextView totalDoctor, highRiskDoctor, recentDoctor;
    private RecyclerView recentPatientsRecView;

    // Individual Stats
    private CardView individualStatsCard, individualRecentCard;
    private TextView totalIndividual, highRiskIndividual, recentIndividual;
    private RecyclerView recentCalculationsRecView;

    // Dividers
    private View addPatientDivider, newCalculationDivider, quickCalculationDivider;
    private View viewAllPatientsDivider, viewAllCalculationsDivider;

    // Actions
    private LinearLayout addPatientBtn, addCalcBtn, quickCalcBtn, viewAllPatientsBtn,
            viewAllCalcsBtn, exportBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupListeners();
        loadUserData();
        setupRecyclerViews();
    }

    private void initViews() {
        userRole = findViewById(R.id.dashboardRole);
        userName = findViewById(R.id.dashboardUserName);
        profileImg = findViewById(R.id.dashboardImgProfile);

        doctorStatsCard = findViewById(R.id.dashboardQuickStatsDoctorCard);
        doctorRecentCard = findViewById(R.id.dashboardRecentPatientsDoctorCard);
        totalDoctor = findViewById(R.id.dashboardTotalStatDoctor);
        highRiskDoctor = findViewById(R.id.dashboardHighRiskStatDoctor);
        recentDoctor = findViewById(R.id.dashboardRecentStatDoctor);
        recentPatientsRecView = findViewById(R.id.dashboardRecentPatientsRecView);

        individualStatsCard = findViewById(R.id.dashboardQuickStatsIndividualCard);
        individualRecentCard = findViewById(R.id.dashboardRecentCalcsIndividualCard);
        totalIndividual = findViewById(R.id.dashboardTotalStatIndividual);
        highRiskIndividual = findViewById(R.id.dashboardRiskStatIndividual);
        recentIndividual = findViewById(R.id.dashboardRecentStatIndividual);
        recentCalculationsRecView = findViewById(R.id.dashboardRecentCalcsRecView);

        addPatientBtn = findViewById(R.id.dashboardAddPatientAction);
        addCalcBtn = findViewById(R.id.dashboardNewCalculationAction);
        quickCalcBtn = findViewById(R.id.dashboardQuickCalculationAction);
        viewAllPatientsBtn = findViewById(R.id.dashboardViewAllPatientsAction);
        viewAllCalcsBtn = findViewById(R.id.dashboardViewAllCalcsAction);
        exportBtn = findViewById(R.id.dashboardExportAction);

        addPatientDivider = findViewById(R.id.dashboardAddPatientDivider);
        newCalculationDivider = findViewById(R.id.dashboardNewCalculationDivider);
        quickCalculationDivider = findViewById(R.id.dashboardQuickCalculationDivider);
        viewAllPatientsDivider = findViewById(R.id.dashboardViewAllPatientsDivider);
        viewAllCalculationsDivider = findViewById(R.id.dashboardViewAllCalculationsDivider);
    }

    private void setupListeners() {
        addPatientBtn.setOnClickListener(v -> {
            Log.d(TAG, "Add Patient clicked");
            Toast.makeText(this, "Add Patient coming soon", Toast.LENGTH_SHORT).show();
        });

        addCalcBtn.setOnClickListener(v -> {
            Log.d(TAG, "Add Calculation clicked");
            Toast.makeText(this, "Add Calculation coming soon", Toast.LENGTH_SHORT).show();
        });

        quickCalcBtn.setOnClickListener(v -> {
            Log.d(TAG, "Quick Calc clicked");
            Toast.makeText(this, "Quick Calculator coming soon", Toast.LENGTH_SHORT).show();
        });

        viewAllPatientsBtn.setOnClickListener(v -> {
            Log.d(TAG, "View All Patients clicked");
            Toast.makeText(this, "All Patients screen coming soon", Toast.LENGTH_SHORT).show();
        });

        viewAllCalcsBtn.setOnClickListener(v -> {
            Log.d(TAG, "View All Calculations clicked");
            Toast.makeText(this, "All Calculations screen coming soon", Toast.LENGTH_SHORT).show();
        });

        exportBtn.setOnClickListener(v -> {
            Log.d(TAG, "Export Data clicked");
            Toast.makeText(this, "Export feature coming soon", Toast.LENGTH_SHORT).show();
        });

        profileImg.setOnClickListener(v -> {
            Log.d(TAG, "Profile clicked");
            Toast.makeText(this, "Profile details coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadUserData() {
        if (currentUser == null) return;

        db.collection("Users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String role = doc.getString("role");
                        String lastName = doc.getString("lastName");

                        userRole.setText("doctor".equalsIgnoreCase(role) ? "Dr. " : "");
                        userName.setText(lastName != null ? lastName : "");

                        setVisibilityBasedOnRole(role);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load user info", e));
    }

    private void setVisibilityBasedOnRole(String role) {
        boolean isDoctor = "doctor".equalsIgnoreCase(role);

        doctorStatsCard.setVisibility(isDoctor ? View.VISIBLE : View.GONE);
        doctorRecentCard.setVisibility(isDoctor ? View.VISIBLE : View.GONE);

        individualStatsCard.setVisibility(isDoctor ? View.GONE : View.VISIBLE);
        individualRecentCard.setVisibility(isDoctor ? View.GONE : View.VISIBLE);

        addPatientBtn.setVisibility(isDoctor ? View.VISIBLE : View.GONE);
        addCalcBtn.setVisibility(isDoctor ? View.GONE : View.VISIBLE);
        quickCalcBtn.setVisibility(isDoctor ? View.VISIBLE : View.GONE);
        viewAllPatientsBtn.setVisibility(isDoctor ? View.VISIBLE : View.GONE);
        viewAllCalcsBtn.setVisibility(isDoctor ? View.GONE : View.VISIBLE);

        addPatientDivider.setVisibility(addPatientBtn.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
        newCalculationDivider.setVisibility(addCalcBtn.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
        quickCalculationDivider.setVisibility(quickCalcBtn.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
        viewAllPatientsDivider.setVisibility(viewAllPatientsBtn.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
        viewAllCalculationsDivider.setVisibility(viewAllCalcsBtn.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
    }

    private void setupRecyclerViews() {
        List<Patient> dummyPatients = generateDummyRecentPatients();

        // Doctor view
        recentPatientsRecView.setLayoutManager(new LinearLayoutManager(this));
        RecentPatientAdapter recentPatientAdapter = new RecentPatientAdapter(dummyPatients);
        recentPatientsRecView.setAdapter(recentPatientAdapter);

        List<Calculation> dummyCalculations = generateDummyCalculations();

        // Individual view
        recentCalculationsRecView.setLayoutManager(new LinearLayoutManager(this));
        RecentCalculationAdapter recentCalculationAdapter = new RecentCalculationAdapter(dummyCalculations);
        recentCalculationsRecView.setAdapter(recentCalculationAdapter);
    }

    private List<Patient> generateDummyRecentPatients() {
        List<Patient> list = new ArrayList<>();
        list.add(new Patient("John Doe", "1959-03-22", System.currentTimeMillis(), Risk.HIGH));
        list.add(new Patient("Maria Koutra", "1954-06-10", System.currentTimeMillis() - 86400000, Risk.MEDIUM));
        list.add(new Patient("George Xlp", "1952-01-15", System.currentTimeMillis() - 172800000, Risk.LOW));
        return list;
    }

    private List<Calculation> generateDummyCalculations() {
        List<Calculation> calculations = new ArrayList<>();

        long now = System.currentTimeMillis();

        calculations.add(new Calculation(
                "calc1", null, null, 67, "Male",
                45.0, 300.0,
                12.0, 24.0,
                now, now,
                "Stable condition"
        ));

        calculations.add(new Calculation(
                "calc2", null, null, 72, "Female",
                25.0, 900.0,
                34.0, 75.0,
                now - 86400000L, now - 86400000L,
                "High ACR, follow-up needed"
        ));

        calculations.add(new Calculation(
                "calc3", null, null, 59, "Male",
                60.0, 120.0,
                5.0, 15.0,
                now - 2 * 86400000L, now - 2 * 86400000L,
                "Improving metrics"
        ));

        return calculations;
    }

}
