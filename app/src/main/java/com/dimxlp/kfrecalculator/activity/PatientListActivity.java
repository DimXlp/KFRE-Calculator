package com.dimxlp.kfrecalculator.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.adapter.PatientAdapter;
import com.dimxlp.kfrecalculator.enumeration.Risk;
import com.dimxlp.kfrecalculator.model.KfreCalculation;
import com.dimxlp.kfrecalculator.model.Patient;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PatientListActivity extends AppCompatActivity {

    private static final String TAG = "RAFI|PatientList";

    private RecyclerView recyclerView;
    private EditText searchEditText;
    private RadioGroup filterRadioGroup;
    private FloatingActionButton fabAddPatient;
    private ImageView appLogo, profileImage;

    private List<Patient> allPatients = new ArrayList<>();
    private List<Patient> filteredPatients = new ArrayList<>();
    private PatientAdapter adapter;
    private String currentSearchQuery = "";
    private int currentStatusFilter = R.id.patientListFilterAllRadio;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_list);

        Log.d(TAG, "PatientListActivity initialized");

        initViews();
        setTopBarFunctionalities();
        setupRecyclerView();
        setupListeners();
        fetchPatientsAndAssessments();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.patientListRecyclerView);
        searchEditText = findViewById(R.id.searchPatientEditText);
        filterRadioGroup = findViewById(R.id.patientListFilterRadioGroup);
        fabAddPatient = findViewById(R.id.patientListFabAddPatient);
        appLogo = findViewById(R.id.patientListLogo);
        profileImage = findViewById(R.id.patientListProfileImg);
    }

    private void setTopBarFunctionalities() {
        appLogo.setOnClickListener(v -> {
            Log.d(TAG, "App Logo clicked");
            Intent intent = new Intent(PatientListActivity.this, DashboardActivity.class);
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
                    Toast.makeText(this, "Profile Activity coming soon", Toast.LENGTH_SHORT).show();
                } else if (itemId == R.id.menu_logout) {
                    Toast.makeText(this, "Logout feature coming soon", Toast.LENGTH_SHORT).show();
                }
                return false;
            });

            popup.show();
        });
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PatientAdapter(filteredPatients);
        recyclerView.setAdapter(adapter);
        adapter.setOnPatientClickListener(patient -> {
            Intent intent = new Intent(this, PatientDetailsActivity.class);
            intent.putExtra("patientId", patient.getPatientId());
            startActivity(intent);
        });
    }

    private void setupListeners() {
        fabAddPatient.setOnClickListener(v -> {
            Log.d(TAG, "Navigating to AddPatientActivity");
            startActivity(new Intent(this, AddPatientActivity.class));
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim().toLowerCase(Locale.getDefault());
                applyCombinedFilters();
            }

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });


        filterRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            currentStatusFilter = checkedId;
            applyCombinedFilters();
        });

    }

    private void applyCombinedFilters() {
        filteredPatients.clear();

        for (Patient p : allPatients) {
            // Full name fallback logic
            String fullName = p.getFullName();
            if (fullName == null || fullName.isEmpty()) {
                fullName = ((p.getFirstName() != null ? p.getFirstName() : "") + " " +
                        (p.getLastName() != null ? p.getLastName() : "")).trim();
            }
            fullName = fullName.toLowerCase(Locale.getDefault());

            // Apply search filter
            boolean matchesSearch = fullName.contains(currentSearchQuery);

            // Apply radio group filter
            boolean matchesFilter = (currentStatusFilter == R.id.patientListFilterAllRadio) ||
                    (currentStatusFilter == R.id.patientListFilterActiveRadio && p.isActive()) ||
                    (currentStatusFilter == R.id.patientListFilterInactiveRadio && !p.isActive());

            if (matchesSearch && matchesFilter) {
                filteredPatients.add(p);
            }
        }

        adapter.notifyDataSetChanged();
        Log.d(TAG, "Filtered patients: " + filteredPatients.size());
    }

    private void fetchPatientsAndAssessments() {
        long startTime = System.currentTimeMillis();
        Log.i(TAG, "Starting data fetch process...");
        String userId = FirebaseAuth.getInstance().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Step 1: Fetch all patients for the current user
        db.collection("Patients")
                .whereEqualTo("userId", userId)
                .get()
                .onSuccessTask(patientSnapshot -> {
                    // Step 2: Once patients are fetched, map them to a list and create tasks to get their assessments.
                    this.allPatients = patientSnapshot.getDocuments().stream()
                            .map(doc -> doc.toObject(Patient.class))
                            .collect(Collectors.toList());

                    Log.i(TAG, "Step 1 complete: Found " + this.allPatients.size() + " patients.");

                    if (this.allPatients.isEmpty()) {
                        return Tasks.forResult(new ArrayList<QuerySnapshot>()); // Return an empty task if no patients
                    }

                    List<Task<QuerySnapshot>> kfreTasks = fetchKfreAssessments(db);
                    // Return a single Task that completes when all individual assessment tasks are done.
                    return Tasks.whenAllSuccess(kfreTasks);
                })
                .addOnSuccessListener(kfreResults -> {
                    // Step 3: All assessments have been fetched. Now process the results.
                    processKfreAssessments(kfreResults);

                    // Step 4: All data is processed. Refresh the UI.
                    applyCombinedFilters();
                    long duration = System.currentTimeMillis() - startTime;
                    Log.i(TAG, "Data fetch and processing finished in " + duration + " ms.");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Data fetch process failed.", e);
                    Toast.makeText(this, "Failed to load patient data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void processKfreAssessments(List<QuerySnapshot> kfreResults) {
        Log.i(TAG, "Step 3 complete: Received all assessment results. Calculating risks...");

        IntStream.range(0, this.allPatients.size()).forEach(i -> {
            Patient patient = this.allPatients.get(i);
            QuerySnapshot kfreSnapshot = (QuerySnapshot) kfreResults.get(i);

            if (kfreSnapshot != null && !kfreSnapshot.isEmpty()) {
                KfreCalculation latestAssessment = kfreSnapshot.getDocuments().get(0).toObject(KfreCalculation.class);
                if (latestAssessment != null) {
                    double risk2Yr = latestAssessment.getRisk2Yr();
                    // Apply risk logic
                    if (risk2Yr >= 40) {
                        patient.setRisk(Risk.HIGH);
                    } else if (risk2Yr >= 10) {
                        patient.setRisk(Risk.MEDIUM);
                    } else {
                        patient.setRisk(Risk.LOW);
                    }
                    Log.d(TAG, "Risk for " + patient.getFullName() + " is " + patient.getRisk());
                }
            } else {
                patient.setRisk(Risk.UNKNOWN);
                Log.d(TAG, "No assessment found for " + patient.getFullName() + ". Risk set to NONE.");
            }
        });
    }

    @NonNull
    private List<Task<QuerySnapshot>> fetchKfreAssessments(FirebaseFirestore db) {
        // Using a stream to create a list of database tasks for each patient.
        List<Task<QuerySnapshot>> kfreTasks = this.allPatients.stream()
                .map(patient -> db.collection("KfreCalculations")
                        .whereEqualTo("patientId", patient.getPatientId())
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(1)
                        .get())
                .collect(Collectors.toList());

        Log.i(TAG, "Step 2 complete: Dispatched " + kfreTasks.size() + " assessment lookups.");
        return kfreTasks;
    }
}
