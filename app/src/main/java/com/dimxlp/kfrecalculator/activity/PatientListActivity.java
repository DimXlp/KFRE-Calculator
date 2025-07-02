package com.dimxlp.kfrecalculator.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.dimxlp.kfrecalculator.enumeration.SortDirection;
import com.dimxlp.kfrecalculator.model.FilterOptionsPatientList;
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
    private FilterOptionsPatientList currentFilterOptions = new FilterOptionsPatientList();

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

        SortDirection sort = currentFilterOptions.getDateSort();
        SortDirection ageSort = currentFilterOptions.getAgeSort();

        if (sort != SortDirection.NONE) {
            newlyFilteredList.sort((p1, p2) -> {
                int comparison = p1.getCreatedAt().compareTo(p2.getCreatedAt());
                // if ascending, return original comparison. if descending, reverse it.
                return sort == SortDirection.ASCENDING ? comparison : -comparison;
            });
        } else if (ageSort != SortDirection.NONE) {
            // If date sort is not active, apply age sort
            newlyFilteredList.sort((p1, p2) -> {
                int age1 = getAgeFromDob(p1.getBirthDate());
                int age2 = getAgeFromDob(p2.getBirthDate());
                int comparison = Integer.compare(age1, age2);
                return ageSort == SortDirection.ASCENDING ? comparison : -comparison;
            });
        }

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

        AnimationUtils.setupExpandableGroup(holder.headerDateSort, holder.contentDateSort, "Sort by Date");
        AnimationUtils.setupExpandableGroup(holder.headerAgeSort, holder.contentAgeSort, "Sort by Age");
        AnimationUtils.setupExpandableGroup(holder.headerStatus, holder.contentStatus, "Status");
        AnimationUtils.setupExpandableGroup(holder.headerRisk, holder.contentRisk, "Risk Category");
        AnimationUtils.setupExpandableGroup(holder.headerGender, holder.contentGender, "Gender");
        AnimationUtils.setupExpandableGroup(holder.headerAge, holder.contentAge, "Age Range");

        // These listeners make sort options mutually exclusive
        holder.rgDateSort.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != -1) {
                holder.rgAgeSort.clearCheck();
            }
        });

        holder.rgAgeSort.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != -1) {
                holder.rgDateSort.clearCheck();
            }
        });

        // Populate the dialog with current filter selections and expand active sections
        populateDialogFromOptions(holder, currentFilterOptions);

        holder.btnApply.setOnClickListener(v -> {
            readOptionsFromDialog(holder, this.currentFilterOptions);
            dialog.dismiss();
            applyCombinedFilters();
        });

        holder.btnClear.setOnClickListener(v -> {
            this.currentFilterOptions = new FilterOptionsPatientList();
            populateDialogFromOptions(holder, this.currentFilterOptions);
        });

        dialog.show();
    }

    private void populateDialogFromOptions(@NonNull DialogViewHolder holder, @NonNull FilterOptionsPatientList options) {
        setupDateSortingOptions(holder, options);
        setupAgeSortingOptions(holder, options);
        setupStatusFilterOptions(holder, options);
        setupRiskFilterOptions(holder, options);
        setupGenderFilterOptions(holder, options);
        setupAgeRangeFilterOptions(holder, options);
    }

    private void setupAgeRangeFilterOptions(@NonNull DialogViewHolder holder, @NonNull FilterOptionsPatientList options) {
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

    private void setupGenderFilterOptions(@NonNull DialogViewHolder holder, @NonNull FilterOptionsPatientList options) {
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
    }

    private void setupRiskFilterOptions(@NonNull DialogViewHolder holder, @NonNull FilterOptionsPatientList options) {
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
    }

    private void setupStatusFilterOptions(@NonNull DialogViewHolder holder, @NonNull FilterOptionsPatientList options) {
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
    }

    private void setupAgeSortingOptions(@NonNull DialogViewHolder holder, @NonNull FilterOptionsPatientList options) {
        if (options.getAgeSort() != SortDirection.NONE) {
            holder.rgAgeSort.check(options.getAgeSort() == SortDirection.ASCENDING ? R.id.rbAgeYoungest : R.id.rbAgeOldest);
            expandSection(holder.contentAgeSort, holder.ivChevronAgeSort);
            holder.ivClearAgeSort.setVisibility(View.VISIBLE);
            holder.ivClearAgeSort.setOnClickListener(v -> {
                options.setAgeSort(SortDirection.NONE);
                holder.rgAgeSort.clearCheck();
                v.setVisibility(View.GONE);
            });
        } else {
            holder.rgAgeSort.clearCheck();
            holder.ivClearAgeSort.setVisibility(View.GONE);
        }
    }

    private void setupDateSortingOptions(@NonNull DialogViewHolder holder, @NonNull FilterOptionsPatientList options) {
        if (options.getDateSort() != SortDirection.NONE) {
            holder.rgDateSort.check(options.getDateSort() == SortDirection.DESCENDING ? R.id.rbDateNewest : R.id.rbDateOldest);
            expandSection(holder.contentDateSort, holder.ivChevronDateSort);
            holder.ivClearDateSort.setVisibility(View.VISIBLE);
            holder.ivClearDateSort.setOnClickListener(v -> {
                options.setDateSort(SortDirection.NONE);
                holder.rgDateSort.clearCheck();
                v.setVisibility(View.GONE);
            });
        } else {
            holder.rgDateSort.clearCheck();
            holder.ivClearDateSort.setVisibility(View.GONE);
        }
    }

    private void expandSection(LinearLayout contentView, ImageView chevron) {
        contentView.setVisibility(View.VISIBLE);
        chevron.setRotation(180f);
    }

    private void readOptionsFromDialog(@NonNull DialogViewHolder holder, @NonNull FilterOptionsPatientList options) {
        applyDateSorting(holder, options);
        applyAgeSorting(holder, options);
        applyStatusFilter(holder, options);
        applyRiskFilter(holder, options);
        applyGenderFilter(holder, options);
        applyAgeRangeFilter(holder, options);
    }

    private static void applyAgeRangeFilter(@NonNull DialogViewHolder holder, @NonNull FilterOptionsPatientList options) {
        List<Float> values = holder.ageSlider.getValues();
        options.setMinAge(values.get(0).intValue());
        options.setMaxAge(values.get(1).intValue());
    }

    private static void applyGenderFilter(@NonNull DialogViewHolder holder, @NonNull FilterOptionsPatientList options) {
        int genderId = holder.rgGender.getCheckedRadioButtonId();
        if (genderId == R.id.rbGenderMale) options.setGender("Male");
        else if (genderId == R.id.rbGenderFemale) options.setGender("Female");
        else options.setGender(null);
    }

    private static void applyRiskFilter(@NonNull DialogViewHolder holder, @NonNull FilterOptionsPatientList options) {
        int riskId = holder.rgRisk.getCheckedRadioButtonId();
        if (riskId == R.id.rbRiskLow) options.setRiskCategory(Risk.LOW);
        else if (riskId == R.id.rbRiskMedium) options.setRiskCategory(Risk.MEDIUM);
        else if (riskId == R.id.rbRiskHigh) options.setRiskCategory(Risk.HIGH);
        else options.setRiskCategory(null);
    }

    private static void applyStatusFilter(@NonNull DialogViewHolder holder, @NonNull FilterOptionsPatientList options) {
        int statusId = holder.rgStatus.getCheckedRadioButtonId();
        if (statusId == R.id.rbStatusActive) options.setStatusIsActive(true);
        else if (statusId == R.id.rbStatusInactive) options.setStatusIsActive(false);
        else options.setStatusIsActive(null);
    }

    private static void applyAgeSorting(@NonNull DialogViewHolder holder, @NonNull FilterOptionsPatientList options) {
        int ageSortId = holder.rgAgeSort.getCheckedRadioButtonId();
        if (ageSortId == R.id.rbAgeYoungest) options.setAgeSort(SortDirection.ASCENDING);
        else if (ageSortId == R.id.rbAgeOldest) options.setAgeSort(SortDirection.DESCENDING);
        else options.setAgeSort(SortDirection.NONE);
    }

    private static void applyDateSorting(@NonNull DialogViewHolder holder, @NonNull FilterOptionsPatientList options) {
        int dateSortId = holder.rgDateSort.getCheckedRadioButtonId();
        if (dateSortId == R.id.rbDateNewest) options.setDateSort(SortDirection.DESCENDING);
        else if (dateSortId == R.id.rbDateOldest) options.setDateSort(SortDirection.ASCENDING);
        else options.setDateSort(SortDirection.NONE);
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

                    this.allPatients.sort((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()));

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
                    patient.setLastAssessment(new Date(latestAssessment.getCreatedAt()));

                    double risk2Yr = latestAssessment.getRisk2Yr();
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
                patient.setLastAssessment(null);
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
        final View headerDateSort, headerAgeSort, headerStatus, headerRisk, headerGender, headerAge;
        final LinearLayout contentDateSort, contentAgeSort, contentStatus, contentRisk, contentGender, contentAge;
        final ImageView ivChevronDateSort, ivChevronAgeSort, ivChevronStatus, ivChevronRisk, ivChevronGender, ivChevronAge;
        final ImageView ivClearDateSort, ivClearAgeSort, ivClearStatus, ivClearRisk, ivClearGender, ivClearAge;
        final RadioGroup rgDateSort, rgAgeSort, rgStatus, rgRisk, rgGender;
        final RangeSlider ageSlider;
        final Button btnApply, btnClear;

        DialogViewHolder(View view) {
            headerDateSort = view.findViewById(R.id.headerDateSort);
            headerAgeSort = view.findViewById(R.id.headerAgeSort);
            headerStatus = view.findViewById(R.id.headerStatus);
            headerRisk = view.findViewById(R.id.headerRiskCategory);
            headerGender = view.findViewById(R.id.headerGender);
            headerAge = view.findViewById(R.id.headerAgeRange);

            contentDateSort = view.findViewById(R.id.contentDateSort);
            contentAgeSort = view.findViewById(R.id.contentAgeSort);
            contentStatus = view.findViewById(R.id.contentStatus);
            contentRisk = view.findViewById(R.id.contentRiskCategory);
            contentGender = view.findViewById(R.id.contentGender);
            contentAge = view.findViewById(R.id.contentAgeRange);

            ivChevronDateSort = headerDateSort.findViewById(R.id.ivChevron);
            ivChevronAgeSort = headerAgeSort.findViewById(R.id.ivChevron);
            ivChevronStatus = headerStatus.findViewById(R.id.ivChevron);
            ivChevronRisk = headerRisk.findViewById(R.id.ivChevron);
            ivChevronGender = headerGender.findViewById(R.id.ivChevron);
            ivChevronAge = headerAge.findViewById(R.id.ivChevron);

            ivClearDateSort = headerDateSort.findViewById(R.id.ivClearCategory);
            ivClearAgeSort = headerAgeSort.findViewById(R.id.ivClearCategory);
            ivClearStatus = headerStatus.findViewById(R.id.ivClearCategory);
            ivClearRisk = headerRisk.findViewById(R.id.ivClearCategory);
            ivClearGender = headerGender.findViewById(R.id.ivClearCategory);
            ivClearAge = headerAge.findViewById(R.id.ivClearCategory);

            rgDateSort = view.findViewById(R.id.rgDateSort);
            rgAgeSort = view.findViewById(R.id.rgAgeSort);
            rgStatus = view.findViewById(R.id.rgStatus);
            rgRisk = view.findViewById(R.id.rgRiskCategory);
            rgGender = view.findViewById(R.id.rgGender);
            ageSlider = view.findViewById(R.id.sliderAgeRange);

            btnApply = view.findViewById(R.id.btnApplyFilters);
            btnClear = view.findViewById(R.id.btnClearFilters);
        }
    }
}
