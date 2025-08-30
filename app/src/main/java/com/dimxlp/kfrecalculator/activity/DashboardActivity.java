package com.dimxlp.kfrecalculator.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.adapter.RecentCalculationAdapter;
import com.dimxlp.kfrecalculator.adapter.RecentPatientAdapter;
import com.dimxlp.kfrecalculator.enumeration.Risk;
import com.dimxlp.kfrecalculator.enumeration.Role;
import com.dimxlp.kfrecalculator.model.KfreCalculation;
import com.dimxlp.kfrecalculator.model.Patient;
import com.dimxlp.kfrecalculator.util.UserPrefs;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    private ImageView individualRiskImage;
    private TextView totalIndividual, riskIndividual, lastCalculationIndividual;
    private RecyclerView recentCalculationsRecView;

    // Dividers
    private View addPatientDivider, newCalculationDivider, quickCalculationDivider;
    private View viewAllPatientsDivider, viewAllCalculationsDivider;

    // Actions
    private LinearLayout addPatientBtn, addCalcBtn, quickCalcBtn, viewAllPatientsBtn,
            viewAllCalcsBtn, exportBtn;

    // Charts
    private CardView pieChartDoctorCard, lineChartIndividualCard;
    private PieChart pieChartDoctor;
    private LineChart lineChartIndividual;

    private String userFirstName, userLastName, userRoleStr, userClinic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupListeners();
        setupRecyclerViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Loading user data.");
        applyWelcomeFromCache();
        refreshUserProfileFromServer();
    }

    private void applyWelcomeFromCache() {
        String cachedRole = UserPrefs.role(this);
        String cachedFirst = UserPrefs.first(this);
        String cachedLast = UserPrefs.last(this);
        setGreeting(cachedRole, cachedFirst, cachedLast);
    }

    private void refreshUserProfileFromServer() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) return;

        db.collection("Users")
                .document(u.getUid())
                .get(Source.SERVER)
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        // still show something sensible
                        setGreeting(null, null, null);
                        return;
                    }

                    userFirstName = doc.getString("firstName");
                    userLastName = doc.getString("lastName");
                    userRoleStr = doc.getString("role");
                    userClinic = doc.getString("clinic");
                    String email = doc.getString("email");
                    if (email == null) email = u.getEmail();

                    String roleLower = userRoleStr == null ? "individual" : userRoleStr.toLowerCase();

                    // save fresh snapshot for future instant reads
                    com.dimxlp.kfrecalculator.util.UserPrefs.save(
                            this,
                            userFirstName,
                            userLastName,
                            email,
                            roleLower,
                            userClinic
                    );

                    // apply greeting/UI now
                    setGreeting(roleLower, userFirstName, userLastName);

                    // keep the rest of your role-based dashboard behavior
                    Role roleEnum = Role.fromString(roleLower);
                    setVisibilityBasedOnRole(roleEnum);

                    if (roleEnum == Role.DOCTOR) {
                        loadPatientData();
                        setupRecyclerViews();
                    } else {
                        // keep whatever you already do for individuals
                        setupIndividualQuickStats(generateDummyCalculations());
                        setupCharts(generateDummyCalculations(), roleEnum);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load user info from server", e);
                    // fall back to cache if server read fails
                    applyWelcomeFromCache();
                });
    }

    private void setGreeting(String roleLower, String firstName, String lastName) {
        boolean isDoctor = "doctor".equalsIgnoreCase(roleLower);

        if (isDoctor) {
            userRole.setText("Dr. ");
            userName.setText(pickNameForDoctor(lastName));
        } else {
            userRole.setText("");
            userName.setText(pickNameForIndividual(firstName));
        }
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
        individualRiskImage = findViewById(R.id.dashboardRiskImageIndividual);
        totalIndividual = findViewById(R.id.dashboardTotalStatIndividual);
        riskIndividual = findViewById(R.id.dashboardRiskStatIndividual);
        lastCalculationIndividual = findViewById(R.id.dashboardLastCalculationIndividual);
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

        pieChartDoctorCard = findViewById(R.id.dashboardRiskPieChartDoctorCard);
        lineChartIndividualCard = findViewById(R.id.dashboardRiskLineChartIndividualCard);
        pieChartDoctor = findViewById(R.id.pieChartRiskDistribution);
        lineChartIndividual = findViewById(R.id.lineChartRiskOverTime);
    }

    private void setupListeners() {
        addPatientBtn.setOnClickListener(v -> {
            Log.d(TAG, "Add Patient clicked");
            Intent intent = new Intent(DashboardActivity.this, AddOrEditPatientActivity.class);
            startActivity(intent);
        });

        addCalcBtn.setOnClickListener(v -> {
            Log.d(TAG, "Add Calculation clicked");
            Toast.makeText(this, "Add Calculation coming soon", Toast.LENGTH_SHORT).show();
        });

        quickCalcBtn.setOnClickListener(v -> {
            Log.d(TAG, "Quick Calc clicked");
            Intent intent = new Intent(DashboardActivity.this, DashboardQuickCalculationActivity.class);
            startActivity(intent);
        });

        viewAllPatientsBtn.setOnClickListener(v -> {
            Log.d(TAG, "View All Patients clicked");
            Intent intent = new Intent(DashboardActivity.this, PatientListActivity.class);
            startActivity(intent);
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
                    Intent intent = new Intent(DashboardActivity.this, ProfileActivity.class);
                    // Pass the fetched user data to the next activity
                    intent.putExtra("USER_FIRST_NAME", userFirstName);
                    intent.putExtra("USER_LAST_NAME", userLastName);
                    intent.putExtra("USER_EMAIL", currentUser.getEmail());
                    intent.putExtra("USER_ROLE", userRoleStr);
                    intent.putExtra("USER_CLINIC", userClinic);
                    startActivity(intent);
                    return true; // Return true to indicate the click was handled
                } else if (itemId == R.id.menu_logout) {
                    Log.d(TAG, "Logout clicked");
                    FirebaseAuth.getInstance().signOut();
                    UserPrefs.clear(this);
                    Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
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

    private void loadUserData() {
        if (currentUser == null) return;

        db.collection("Users")
                .document(currentUser.getUid())
                .get(Source.SERVER)
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        userFirstName = doc.getString("firstName");
                        userLastName = doc.getString("lastName");
                        userRoleStr = doc.getString("role");
                        userClinic = doc.getString("clinic");

                        Role role = Role.fromString(doc.getString("role"));

                        userRole.setText(role == Role.DOCTOR ? "Dr. " : "");
                        userName.setText(role == Role.DOCTOR ?
                                pickNameForDoctor(UserPrefs.last(this)) :
                                pickNameForIndividual(UserPrefs.first(this)));

                        setVisibilityBasedOnRole(role);

                        if (role == Role.DOCTOR) {
                            loadPatientData();
                            setupRecyclerViews();
                        } else {
                            setupIndividualQuickStats(generateDummyCalculations());
                            setupCharts(generateDummyCalculations(), role);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load user info", e));
    }

    private String pickNameForDoctor(String lastName) {
        if (isNotNullOrBlank(lastName)) return lastName.trim();
        return fallbackNameFromAuth();
    }

    private String pickNameForIndividual(String firstName) {
        if (isNotNullOrBlank(firstName)) return firstName.trim();
        return fallbackNameFromAuth();
    }

    private boolean isNotNullOrBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private String fallbackNameFromAuth() {
        FirebaseUser u = currentUser != null ? currentUser : FirebaseAuth.getInstance().getCurrentUser();
        if (u != null && u.getDisplayName() != null && !u.getDisplayName().trim().isEmpty()) {
            return u.getDisplayName().trim();
        }
        if (u != null && u.getEmail() != null) {
            String email = u.getEmail();
            int at = email.indexOf('@');
            return at > 0 ? email.substring(0, at) : email;
        }
        return "User";
    }


    private void loadPatientData() {
        if (currentUser == null) return;

        db.collection("Patients")
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Integer totalPatientCount = calculatePatientStats(queryDocumentSnapshots);
                    if (totalPatientCount == null) return;
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading patient data", e);
                    totalDoctor.setText("0");
                });
    }

    private void calculateRecentPatients(QuerySnapshot patientSnapshots) {
        // Define "recent" as within the last 30 days
        long thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000;
        long recentThreshold = System.currentTimeMillis() - thirtyDaysInMillis;

        List<Patient> recentPatientsList = new ArrayList<>();
        for (DocumentSnapshot doc : patientSnapshots.getDocuments()) {
            Timestamp createdAtTimestamp = doc.getTimestamp("createdAt");
            if (createdAtTimestamp != null && createdAtTimestamp.toDate().getTime() >= recentThreshold) {
                Patient patient = doc.toObject(Patient.class);
                if(patient != null){
                    patient.setPatientId(doc.getId());
                    recentPatientsList.add(patient);
                }
            }
        }

        // Update the quick stat TextView
        recentDoctor.setText(String.valueOf(recentPatientsList.size()));
        Log.d(TAG, "Recent patient count: " + recentPatientsList.size());

        // Sort by creation date to show newest first
        recentPatientsList.sort((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()));

        // Directly setup the RecyclerView with the fetched data
        setupRecentPatientsRecyclerView(recentPatientsList);
    }

    private void calculatePatientRiskDistribution(QuerySnapshot patientSnapshots) {
        int totalPatientCount = patientSnapshots.size();
        if (totalPatientCount == 0) return;

        final AtomicInteger patientsProcessed = new AtomicInteger(0);
        final AtomicInteger lowRiskCount = new AtomicInteger(0);
        final AtomicInteger mediumRiskCount = new AtomicInteger(0);
        final AtomicInteger highRiskCount = new AtomicInteger(0);

        for (DocumentSnapshot patientDoc : patientSnapshots.getDocuments()) {
            String patientId = patientDoc.getId();
            db.collection("KfreCalculations")
                    .whereEqualTo("patientId", patientId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            KfreCalculation latestCalc = task.getResult().getDocuments().get(0).toObject(KfreCalculation.class);
                            if (latestCalc != null) {
                                double risk2Yr = latestCalc.getRisk2Yr();
                                // Categorize based on risk
                                if (risk2Yr >= 40.0) {
                                    highRiskCount.incrementAndGet();
                                } else if (risk2Yr >= 10.0) {
                                    mediumRiskCount.incrementAndGet();
                                } else {
                                    lowRiskCount.incrementAndGet();
                                }
                            }
                        } else {
                            Log.e(TAG, "Could not fetch calculation for risk distribution: " + patientId, task.getException());
                        }

                        // When the last patient is processed, update the UI
                        if (patientsProcessed.incrementAndGet() == totalPatientCount) {
                            highRiskDoctor.setText(String.valueOf(highRiskCount.get()));
                            setupDoctorPieChart(lowRiskCount.get(), mediumRiskCount.get(), highRiskCount.get());
                        }
                    });
        }
    }

    @Nullable
    private Integer calculatePatientStats(QuerySnapshot queryDocumentSnapshots) {
        // Calculating total patients
        int totalPatientCount = queryDocumentSnapshots.size();
        totalDoctor.setText(String.valueOf(totalPatientCount));
        Log.d(TAG, "Successfully loaded patient count: " + totalPatientCount);

        if (totalPatientCount > 0) {
            // Calculating risk distribution
            calculatePatientRiskDistribution(queryDocumentSnapshots);
            // Calculating recent patients
            calculateRecentPatients(queryDocumentSnapshots);
        } else {
            // If there are no patients, set stats to 0 and clear the chart
            highRiskDoctor.setText("0");
            recentDoctor.setText("0");
            setupDoctorPieChart(0, 0, 0);
        }

        return totalPatientCount;
    }

    private void setupIndividualQuickStats(List<KfreCalculation> kfreCalculations) {
        long now = System.currentTimeMillis();

        long totalCalcs = kfreCalculations.size();

        String lastCalc = totalCalcs > 0
                ? android.text.format.DateFormat.format("dd MMM yyyy", kfreCalculations.get(0).getCreatedAt()).toString()
                : "-";

        String riskLabel = "-";
        if (totalCalcs > 0) {
            double lastRisk = kfreCalculations.get(0).getRisk2Yr();
            if (lastRisk < 5) riskLabel = "Low";
            else if (lastRisk < 15) riskLabel = "Medium";
            else riskLabel = "High";
        }

        totalIndividual.setText(String.valueOf(totalCalcs));
        lastCalculationIndividual.setText(lastCalc);
        riskIndividual.setText(riskLabel);

        if (Risk.MEDIUM.getRisk().equals(riskIndividual.getText())) {
            individualRiskImage.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorMediumRiskStat));
            individualRiskImage.setImageResource(R.drawable.ic_medium);
        } else if (Risk.LOW.getRisk().equals(riskIndividual.getText())) {
            individualRiskImage.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorLowRiskStat));
            individualRiskImage.setImageResource(R.drawable.ic_tick);
        }
    }


    private void setupCharts(List<KfreCalculation> kfreCalculations, Role role) {
        setupIndividualLineChart(kfreCalculations);
    }

    private void setupDoctorPieChart(int low, int medium, int high) {
        List<PieEntry> entries = new ArrayList<>();
        if (low > 0) entries.add(new PieEntry(low, "Low"));
        if (medium > 0) entries.add(new PieEntry(medium, "Medium"));
        if (high > 0) entries.add(new PieEntry(high, "High"));

        // If there is no data, clear the chart
        if (entries.isEmpty()) {
            pieChartDoctor.clear();
            pieChartDoctor.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "Risk Distribution");

        List<Integer> pieColors = new ArrayList<>();
        if (low > 0) pieColors.add(ContextCompat.getColor(this, R.color.colorAccentDark));
        if (medium > 0) pieColors.add(ContextCompat.getColor(this, R.color.colorMediumRisk));
        if (high > 0) pieColors.add(ContextCompat.getColor(this, R.color.colorHighRisk));

        dataSet.setColors(pieColors);

        PieData pieData = new PieData(dataSet);

        pieData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        pieData.setValueTextSize(12f);
        pieData.setValueTextColor(ContextCompat.getColor(this, R.color.colorOnPrimary));

        pieChartDoctor.setData(pieData);
        pieChartDoctor.setUsePercentValues(false);
        pieChartDoctor.setEntryLabelColor(ContextCompat.getColor(this, R.color.colorOnPrimary));
        pieChartDoctor.setEntryLabelTextSize(12f);
        pieChartDoctor.setCenterText("Patients");
        pieChartDoctor.setCenterTextSize(16f);
        pieChartDoctor.getDescription().setEnabled(false);
        pieChartDoctor.getLegend().setEnabled(false); // Disabling legend for a cleaner look
        pieChartDoctor.invalidate(); // Refresh the chart
    }

    private void setupIndividualLineChart(List<KfreCalculation> kfreCalculations) {
        List<KfreCalculation> reversedCalcs = new ArrayList<>(kfreCalculations);
        Collections.reverse(reversedCalcs);

        List<Entry> twoYearEntries = IntStream.range(0, reversedCalcs.size())
                .mapToObj(i -> new Entry(i, (float) reversedCalcs.get(i).getRisk2Yr()))
                .collect(Collectors.toList());


        LineDataSet twoYearDataSet = new LineDataSet(twoYearEntries, "2-Year KFRE Risk");
        int twoYearLineColor = ContextCompat.getColor(getApplicationContext(), R.color.colorSecondary);
        twoYearDataSet.setColors(twoYearLineColor);
        twoYearDataSet.setValueTextSize(10f);
        twoYearDataSet.setLineWidth(2f);
        twoYearDataSet.setCircleRadius(4f);
        twoYearDataSet.setCircleColor(twoYearLineColor);

        List<Entry> fiveYearEntries = IntStream.range(0, reversedCalcs.size())
                .mapToObj(i -> new Entry(i, (float) reversedCalcs.get(i).getRisk5Yr()))
                .collect(Collectors.toList());

        LineDataSet fiveYearDataSet = new LineDataSet(fiveYearEntries, "5-Year KFRE Risk");
        int fiveYearLineColor = ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryVariant);
        fiveYearDataSet.setColors(fiveYearLineColor);
        fiveYearDataSet.setValueTextSize(10f);
        fiveYearDataSet.setLineWidth(2f);
        fiveYearDataSet.setCircleRadius(4f);
        fiveYearDataSet.setCircleColor(fiveYearLineColor);

        LineData lineData = new LineData(twoYearDataSet, fiveYearDataSet);
        lineChartIndividual.setData(lineData);
        lineChartIndividual.setDrawGridBackground(false);

        Description description = new Description();
        description.setText("Time (Oldest → Newest)");
        lineChartIndividual.setDescription(description);

        XAxis xAxis = lineChartIndividual.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        lineChartIndividual.getAxisRight().setEnabled(false);
        lineChartIndividual.invalidate();
    }

    private void setVisibilityBasedOnRole(Role role) {
        boolean isDoctor = role == Role.DOCTOR;

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

        pieChartDoctorCard.setVisibility(isDoctor ? View.VISIBLE : View.GONE);
        lineChartIndividualCard.setVisibility(isDoctor ? View.GONE : View.VISIBLE);
    }

    private void setupRecentPatientsRecyclerView(List<Patient> recentPatients) {
        recentPatientsRecView.setLayoutManager(new LinearLayoutManager(this));
        RecentPatientAdapter adapter = new RecentPatientAdapter(recentPatients);
        recentPatientsRecView.setAdapter(adapter);
        adapter.setOnPatientClickListener(patient -> {
            Intent intent = new Intent(this, PatientDetailsActivity.class);
            intent.putExtra("patientId", patient.getPatientId());
            startActivity(intent);
        });
    }

    private void setupRecyclerViews() {
        List<KfreCalculation> dummyKfreCalculations = generateDummyCalculations();
        recentCalculationsRecView.setLayoutManager(new LinearLayoutManager(this));
        RecentCalculationAdapter recentCalculationAdapter = new RecentCalculationAdapter(dummyKfreCalculations);
        recentCalculationsRecView.setAdapter(recentCalculationAdapter);
    }

    private List<KfreCalculation> generateDummyCalculations() {
        List<KfreCalculation> kfreCalculations = new ArrayList<>();

        long now = System.currentTimeMillis();

        kfreCalculations.add(new KfreCalculation(
                "calc1", null, null, 67, "Male",
                45.0, 300.0,
                14.0, 24.0,
                now, now,
                "Stable condition"
        ));

        kfreCalculations.add(new KfreCalculation(
                "calc2", null, null, 72, "Female",
                25.0, 900.0,
                34.0, 75.0,
                now - 86400000L, now - 86400000L,
                "High ACR, follow-up needed"
        ));

        kfreCalculations.add(new KfreCalculation(
                "calc3", null, null, 59, "Male",
                60.0, 120.0,
                5.0, 15.0,
                now - 2 * 86400000L, now - 2 * 86400000L,
                "Improving metrics"
        ));

        return kfreCalculations;
    }

}
