package com.dimxlp.kfrecalculator.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.adapter.PatientAdapter;
import com.dimxlp.kfrecalculator.enumeration.Risk;
import com.dimxlp.kfrecalculator.model.FilterOptionsPatient;
import com.dimxlp.kfrecalculator.model.KfreCalculation;
import com.dimxlp.kfrecalculator.model.Patient;
import com.dimxlp.kfrecalculator.utils.AnimationUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.RangeSlider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PatientListActivity extends AppCompatActivity {

    private static final String TAG = "RAFI|PatientList";

    private RecyclerView recyclerView;
    private EditText searchEditText;
    private ImageButton btnShowFilters;
    private FloatingActionButton fabAddPatient;
    private ImageView appLogo, profileImage;

    private List<Patient> allPatients = new ArrayList<>();
    private final List<Patient> filteredPatients = new ArrayList<>();
    private PatientAdapter adapter;
    private String currentSearchQuery = "";
    private FilterOptionsPatient currentFilterOptions = new FilterOptionsPatient();

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

    @Override
    protected void onResume() {
        super.onResume();
        fetchPatientsAndAssessments();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.patientListRecyclerView);
        searchEditText = findViewById(R.id.searchPatientEditText);
        btnShowFilters = findViewById(R.id.btnShowFilters);
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

        btnShowFilters.setOnClickListener(v -> showFilterDialog());

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim().toLowerCase(Locale.getDefault());
                applyCombinedFilters();
            }

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void applyCombinedFilters() {
        List<Patient> newlyFilteredList = allPatients.stream()
                .filter(this::matchesAdvancedFilters)
                .filter(this::matchesSearchQuery)
                .collect(Collectors.toList());

        filteredPatients.clear();
        filteredPatients.addAll(newlyFilteredList);

        adapter.notifyDataSetChanged();
        Log.d(TAG, "Filter applied. Displaying " + filteredPatients.size() + " of " + allPatients.size() + " total patients.");
    }

    private boolean matchesSearchQuery(@NonNull Patient p) {
        if (currentSearchQuery.isEmpty()) {
            return true;
        }
        String fullName = p.getFullName() != null ? p.getFullName().toLowerCase(Locale.getDefault()) : "";
        return fullName.contains(currentSearchQuery);
    }

    private boolean matchesAdvancedFilters(@NonNull Patient p) {
        // Status Filter
        if (currentFilterOptions.getStatusIsActive() != null &&
                currentFilterOptions.getStatusIsActive() != p.isActive()) {
            return false;
        }

        // Risk Filter
        if (currentFilterOptions.getRiskCategory() != null &&
                currentFilterOptions.getRiskCategory() != p.getRisk()) {
            return false;
        }

        // Gender Filter
        if (currentFilterOptions.getGender() != null &&
                !currentFilterOptions.getGender().equalsIgnoreCase(p.getGender())) {
            return false;
        }

        // Age Filter
        int age = getAgeFromDob(p.getBirthDate());
        if (age != -1 && (age < currentFilterOptions.getMinAge() || age > currentFilterOptions.getMaxAge())) {
            return false;
        }

        return true; // Patient passed all filters
    }

    private int getAgeFromDob(String dobString) {
        if (dobString == null || dobString.isEmpty()) return -1;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date dob = sdf.parse(dobString);
            Calendar birthCal = Calendar.getInstance();
            birthCal.setTime(dob);
            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR);
            if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
            return age;
        } catch (ParseException e) {
            return -1;
        }
    }

    private void showFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_filter_patients, null);
        dialog.setContentView(view);

        final DialogViewHolder holder = new DialogViewHolder(view);

        AnimationUtils.setupExpandableGroup(holder.headerStatus, holder.contentStatus, "Status");
        AnimationUtils.setupExpandableGroup(holder.headerRisk, holder.contentRisk, "Risk Category");
        AnimationUtils.setupExpandableGroup(holder.headerGender, holder.contentGender, "Gender");
        AnimationUtils.setupExpandableGroup(holder.headerAge, holder.contentAge, "Age Range");

        // Populate the dialog with current filter selections and expand active sections
        populateDialogFromOptions(holder, currentFilterOptions);

        holder.btnApply.setOnClickListener(v -> {
            readOptionsFromDialog(holder, this.currentFilterOptions);
            dialog.dismiss();
            applyCombinedFilters();
        });

        holder.btnClear.setOnClickListener(v -> {
            this.currentFilterOptions = new FilterOptionsPatient();
            populateDialogFromOptions(holder, this.currentFilterOptions);
        });

        dialog.show();
    }

    private void populateDialogFromOptions(@NonNull DialogViewHolder holder, @NonNull FilterOptionsPatient options) {
        // Status
        if (options.getStatusIsActive() != null) {
            holder.rgStatus.check(options.getStatusIsActive() ? R.id.rbStatusActive : R.id.rbStatusInactive);
            expandSection(holder.contentStatus, holder.ivChevronStatus);
            holder.ivClearStatus.setVisibility(View.VISIBLE);
            holder.ivClearStatus.setOnClickListener(v -> {
                options.setStatusIsActive(null);
                holder.rgStatus.clearCheck();
                v.setVisibility(View.GONE);
            });
        } else {
            holder.rgStatus.clearCheck();
            holder.ivClearStatus.setVisibility(View.GONE);
        }

        // Risk Category
        if (options.getRiskCategory() != null) {
            switch (options.getRiskCategory()) {
                case LOW: holder.rgRisk.check(R.id.rbRiskLow); break;
                case MEDIUM: holder.rgRisk.check(R.id.rbRiskMedium); break;
                case HIGH: holder.rgRisk.check(R.id.rbRiskHigh); break;
            }
            expandSection(holder.contentRisk, holder.ivChevronRisk);
            holder.ivClearRisk.setVisibility(View.VISIBLE);
            holder.ivClearRisk.setOnClickListener(v -> {
                options.setRiskCategory(null);
                holder.rgRisk.clearCheck();
                v.setVisibility(View.GONE);
            });
        } else {
            holder.rgRisk.clearCheck();
            holder.ivClearRisk.setVisibility(View.GONE);
        }

        // Gender
        if (options.getGender() != null) {
            if ("Male".equalsIgnoreCase(options.getGender())) holder.rgGender.check(R.id.rbGenderMale);
            else if ("Female".equalsIgnoreCase(options.getGender())) holder.rgGender.check(R.id.rbGenderFemale);
            expandSection(holder.contentGender, holder.ivChevronGender);
            holder.ivClearGender.setVisibility(View.VISIBLE);
            holder.ivClearGender.setOnClickListener(v -> {
                options.setGender(null);
                holder.rgGender.clearCheck();
                v.setVisibility(View.GONE);
            });
        } else {
            holder.rgGender.clearCheck();
            holder.ivClearGender.setVisibility(View.GONE);
        }

        // Age Range
        holder.ageSlider.setValues((float) options.getMinAge(), (float) options.getMaxAge());
        if (options.getMinAge() > 0 || options.getMaxAge() < 120) {
            expandSection(holder.contentAge, holder.ivChevronAge);
            holder.ivClearAge.setVisibility(View.VISIBLE);
            holder.ivClearAge.setOnClickListener(v -> {
                options.setMinAge(0);
                options.setMaxAge(120);
                holder.ageSlider.setValues(0f, 120f);
                v.setVisibility(View.GONE);
            });
        } else {
            holder.ivClearAge.setVisibility(View.GONE);
        }
    }

    private void expandSection(LinearLayout contentView, ImageView chevron) {
        contentView.setVisibility(View.VISIBLE);
        chevron.setRotation(180f);
    }

    private void readOptionsFromDialog(@NonNull DialogViewHolder holder, @NonNull FilterOptionsPatient options) {
        // Status
        int statusId = holder.rgStatus.getCheckedRadioButtonId();
        if (statusId == R.id.rbStatusActive) options.setStatusIsActive(true);
        else if (statusId == R.id.rbStatusInactive) options.setStatusIsActive(false);
        else options.setStatusIsActive(null);

        // Risk
        int riskId = holder.rgRisk.getCheckedRadioButtonId();
        if (riskId == R.id.rbRiskLow) options.setRiskCategory(Risk.LOW);
        else if (riskId == R.id.rbRiskMedium) options.setRiskCategory(Risk.MEDIUM);
        else if (riskId == R.id.rbRiskHigh) options.setRiskCategory(Risk.HIGH);
        else options.setRiskCategory(null);

        // Gender
        int genderId = holder.rgGender.getCheckedRadioButtonId();
        if (genderId == R.id.rbGenderMale) options.setGender("Male");
        else if (genderId == R.id.rbGenderFemale) options.setGender("Female");
        else options.setGender(null);

        // Age
        List<Float> values = holder.ageSlider.getValues();
        options.setMinAge(values.get(0).intValue());
        options.setMaxAge(values.get(1).intValue());
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

    private static class DialogViewHolder {
        final View headerStatus, headerRisk, headerGender, headerAge;
        final LinearLayout contentStatus, contentRisk, contentGender, contentAge;
        final ImageView ivChevronStatus, ivChevronRisk, ivChevronGender, ivChevronAge;
        final ImageView ivClearStatus, ivClearRisk, ivClearGender, ivClearAge;
        final RadioGroup rgStatus, rgRisk, rgGender;
        final RangeSlider ageSlider;
        final Button btnApply, btnClear;

        DialogViewHolder(View view) {
            headerStatus = view.findViewById(R.id.headerStatus);
            headerRisk = view.findViewById(R.id.headerRiskCategory);
            headerGender = view.findViewById(R.id.headerGender);
            headerAge = view.findViewById(R.id.headerAgeRange);

            contentStatus = view.findViewById(R.id.contentStatus);
            contentRisk = view.findViewById(R.id.contentRiskCategory);
            contentGender = view.findViewById(R.id.contentGender);
            contentAge = view.findViewById(R.id.contentAgeRange);

            ivChevronStatus = headerStatus.findViewById(R.id.ivChevron);
            ivChevronRisk = headerRisk.findViewById(R.id.ivChevron);
            ivChevronGender = headerGender.findViewById(R.id.ivChevron);
            ivChevronAge = headerAge.findViewById(R.id.ivChevron);

            ivClearStatus = headerStatus.findViewById(R.id.ivClearCategory);
            ivClearRisk = headerRisk.findViewById(R.id.ivClearCategory);
            ivClearGender = headerGender.findViewById(R.id.ivClearCategory);
            ivClearAge = headerAge.findViewById(R.id.ivClearCategory);

            rgStatus = view.findViewById(R.id.rgStatus);
            rgRisk = view.findViewById(R.id.rgRiskCategory);
            rgGender = view.findViewById(R.id.rgGender);
            ageSlider = view.findViewById(R.id.sliderAgeRange);

            btnApply = view.findViewById(R.id.btnApplyFilters);
            btnClear = view.findViewById(R.id.btnClearFilters);
        }
    }
}
