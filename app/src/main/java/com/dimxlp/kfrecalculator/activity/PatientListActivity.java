package com.dimxlp.kfrecalculator.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.adapter.PatientAdapter;
import com.dimxlp.kfrecalculator.model.Patient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PatientListActivity extends AppCompatActivity {

    private static final String TAG = "RAFI|PatientList";

    private RecyclerView recyclerView;
    private EditText searchEditText;
    private RadioGroup filterRadioGroup;
    private FloatingActionButton fabAddPatient;
    private ImageView profileImage;

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
        setupRecyclerView();
        setupListeners();
        fetchPatients();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.patientListRecyclerView);
        searchEditText = findViewById(R.id.searchPatientEditText);
        filterRadioGroup = findViewById(R.id.patientListFilterRadioGroup);
        fabAddPatient = findViewById(R.id.patientListFabAddPatient);
        profileImage = findViewById(R.id.patientListProfileImg);
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

        profileImage.setOnClickListener(v -> {
            Log.d(TAG, "Profile picture clicked");
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

    private void fetchPatients() {
        Log.d(TAG, "Fetching patients from Firestore...");
        String userId = FirebaseAuth.getInstance().getUid();

        FirebaseFirestore.getInstance().collection("Patients")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allPatients.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Patient patient = doc.toObject(Patient.class);
                        allPatients.add(patient);
                    }
                    Log.d(TAG, "Fetched " + allPatients.size() + " patients");
                    applyCombinedFilters();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch patients", e));
    }

    private void filterPatients(String searchQuery) {
        String query = searchQuery.toLowerCase(Locale.getDefault());
        int checkedId = filterRadioGroup.getCheckedRadioButtonId();

        filteredPatients.clear();
        for (Patient p : allPatients) {
            boolean matchesSearch = p.getFullName().toLowerCase(Locale.getDefault()).contains(query);
            boolean matchesFilter = (checkedId == R.id.patientListFilterAllRadio) ||
                    (checkedId == R.id.patientListFilterActiveRadio && p.isActive()) ||
                    (checkedId == R.id.patientListFilterInactiveRadio && !p.isActive());

            if (matchesSearch && matchesFilter) {
                filteredPatients.add(p);
            }
        }

        adapter.notifyDataSetChanged();
        Log.d(TAG, "Filtered list: " + filteredPatients.size());
    }
}
