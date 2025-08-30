package com.dimxlp.kfrecalculator.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.enumeration.Risk;
import com.dimxlp.kfrecalculator.ui.MedicationPickerBottomSheet;
import com.dimxlp.kfrecalculator.model.Disease;
import com.dimxlp.kfrecalculator.model.MedicationAssignment;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;

public class AddOrEditPatientActivity extends AppCompatActivity implements MedicationPickerBottomSheet.OnMedicationSelectedListener {

    private static final String TAG = "RAFI|AddOrEditPatient";

    private FirebaseUser currentUser;
    private boolean isEditMode = false;
    private String patientId;

    private ImageView appLogo, profileImg;
    private TextInputEditText firstNameEditText, lastNameEditText, notesEditText;
    private TextView dobTextView;
    private RadioGroup genderRadioGroup;
    private LinearLayout medHistoryContainer;
    private Button addPatientButton;

    private final List<String> diseasesList = List.of("Diabetes", "Hypertension", "Cardiovascular Disease", "Kidney Disease");
    private final Map<String, Disease> selectedDiseases = new HashMap<>();
    private final Map<String, List<MedicationAssignment>> medicationAssignmentsByDisease = new HashMap<>();
    private final Map<String, String> diseaseDetailsByName = new HashMap<>();
    private String currentDiseaseId = null;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_or_edit_patient);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        if (getIntent().hasExtra("patientId")) {
            isEditMode = true;
            patientId = getIntent().getStringExtra("patientId");
            Log.d(TAG, "Activity in Edit Mode for patient: " + patientId);
        }

        initViews();
        setTopBarFunctionalities();
        setupDatePicker();
        setupDiseaseChecklist();

        if (isEditMode) {
            loadPatientData();
        }
    }

    private void setTopBarFunctionalities() {
        appLogo.setOnClickListener(v -> {
            Log.d(TAG, "App Logo clicked");
            Intent intent = new Intent(AddOrEditPatientActivity.this, DashboardActivity.class);
            startActivity(intent);
            finish();
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
                    Intent intent = new Intent(AddOrEditPatientActivity.this, ProfileActivity.class);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.menu_logout) {
                    Log.d(TAG, "Logout clicked");
                    FirebaseAuth.getInstance().signOut();

                    Intent intent = new Intent(AddOrEditPatientActivity.this, MainActivity.class);
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

    private void initViews() {
        appLogo = findViewById(R.id.addPatientAppLogo);
        profileImg = findViewById(R.id.addPatientProfileImg);
        firstNameEditText = findViewById(R.id.addPatientFirstName);
        lastNameEditText = findViewById(R.id.addPatientLastName);
        dobTextView = findViewById(R.id.addPatientDob);
        genderRadioGroup = findViewById(R.id.addPatientGenderRadio);
        medHistoryContainer = findViewById(R.id.addPatientMedHistoryContainer);
        notesEditText = findViewById(R.id.addPatientNotes);
        addPatientButton = findViewById(R.id.btnAddPatient);

        if (isEditMode) {
            addPatientButton.setText("Update Patient");
        }

        addPatientButton.setOnClickListener(v -> saveOrUpdatePatient());
    }

    private void setupDatePicker() {
        dobTextView.setOnClickListener(v -> {
            // 1. Get the current date from the TextView to use as a default
            final Calendar calendar = Calendar.getInstance();
            String currentDob = dobTextView.getText().toString();

            // 2. If a valid date is already set, parse it and update the calendar
            if (!currentDob.isEmpty() && !currentDob.equalsIgnoreCase("Select date")) {
                SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
                try {
                    Date date = sdf.parse(currentDob);
                    if (date != null) {
                        calendar.setTime(date);
                    }
                } catch (ParseException e) {
                    Log.e(TAG, "Error parsing DOB, defaulting to today.", e);
                }
            }

            // 3. Use the calendar's values to create the DatePickerDialog
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePicker = new DatePickerDialog(this,
                    (view, selectedYear, selectedMonth, selectedDayOfMonth) -> {
                        // Set the text with the newly selected date
                        dobTextView.setText(selectedDayOfMonth + "/" + (selectedMonth + 1) + "/" + selectedYear);
                    },
                    year, month, day);
            datePicker.show();
        });
    }

    private void setupDiseaseChecklist() {
        for (String disease : diseasesList) {
            View view = LayoutInflater.from(this).inflate(R.layout.item_disease_section, medHistoryContainer, false);

            TextView txtDiseaseName = view.findViewById(R.id.txtDiseaseName);
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(disease);
            checkBox.setChecked(false);

            EditText inputDetails = new EditText(this);
            inputDetails.setHint("Enter details for " + disease);
            inputDetails.setVisibility(View.GONE);

            Button btnAddMedication = view.findViewById(R.id.btnAddMedication);
            LinearLayout medicationsContainer = view.findViewById(R.id.medicationsContainer);
            btnAddMedication.setVisibility(View.GONE);

            txtDiseaseName.setText(disease);

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    inputDetails.setVisibility(View.VISIBLE);
                    btnAddMedication.setVisibility(View.VISIBLE);
                    Disease d = new Disease(null, null, disease, true, "");
                    selectedDiseases.put(disease, d);
                    medicationAssignmentsByDisease.put(disease, new ArrayList<>());
                } else {
                    inputDetails.setVisibility(View.GONE);
                    btnAddMedication.setVisibility(View.GONE);
                    medicationsContainer.removeAllViews();
                    selectedDiseases.remove(disease);
                    medicationAssignmentsByDisease.remove(disease);
                }
            });

            inputDetails.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    diseaseDetailsByName.put(disease, inputDetails.getText().toString().trim());
                }
            });

            btnAddMedication.setOnClickListener(v -> {
                currentDiseaseId = disease;
                MedicationPickerBottomSheet sheet = new MedicationPickerBottomSheet();
                sheet.setOnMedicationSelectedListener(this);
                sheet.show(getSupportFragmentManager(), "MedicationPicker");
            });

            medicationsContainer.setTag(disease);

            LinearLayout sectionLayout = (LinearLayout) view;
            sectionLayout.addView(checkBox);
            sectionLayout.addView(inputDetails);

            medHistoryContainer.addView(view);
        }
    }

    @Override
    public void onMedicationSelected(String medicationId, String medicationName, String frequency) {
        Log.d(TAG, "Medication selected: " + medicationName + " - " + frequency);
        if (currentDiseaseId == null || !selectedDiseases.containsKey(currentDiseaseId)) return;

        MedicationAssignment assignment = new MedicationAssignment(null, null, currentDiseaseId, medicationId, frequency);
        medicationAssignmentsByDisease.get(currentDiseaseId).add(assignment);

        addMedicationView(currentDiseaseId, assignment, medicationName);
    }

    private void addMedicationView(String diseaseName, MedicationAssignment assignment, String medicationName) {
        IntStream.range(0, medHistoryContainer.getChildCount())
                .mapToObj(medHistoryContainer::getChildAt)
                .map(container -> (LinearLayout) container.findViewById(R.id.medicationsContainer))
                .filter(medsLayout -> medsLayout != null && diseaseName.equals(medsLayout.getTag()))
                .findFirst()
                .ifPresent(medsLayout -> {
                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);

                    TextView medView = new TextView(this);
                    medView.setText("- " + medicationName + " (" + assignment.getFrequency() + ")");
                    medView.setTextColor(getColor(R.color.textSecondary));
                    medView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

                    Button deleteButton = new Button(this);
                    deleteButton.setText("🗑");
                    deleteButton.setBackgroundColor(getColor(android.R.color.transparent));
                    deleteButton.setTextColor(getColor(R.color.colorHighRisk));
                    deleteButton.setOnClickListener(v -> {
                        medsLayout.removeView(row);
                        medicationAssignmentsByDisease.get(diseaseName).remove(assignment);
                        Log.d(TAG, "Removed medication: " + medicationName);
                    });

                    row.addView(medView);
                    row.addView(deleteButton);
                    medsLayout.addView(row);
                });
    }

    private void saveOrUpdatePatient() {
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String dob = dobTextView.getText().toString();
        String gender = getSelectedGender();
        if (firstName.isEmpty() || lastName.isEmpty() || dob.isEmpty() || gender == null) {
            Toast.makeText(this, "Please fill all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isEditMode) {
            updatePatient();
        } else {
            createPatient();
        }
    }

    private void createPatient() {
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String dob = dobTextView.getText().toString();
        String gender = getSelectedGender();
        String notes = notesEditText.getText().toString().trim();

        Map<String, Object> patient = new HashMap<>();
        patient.put("firstName", firstName);
        patient.put("lastName", lastName);
        patient.put("fullName", firstName + " " + lastName);
        patient.put("birthDate", dob);
        patient.put("gender", gender);
        patient.put("notes", notes);
        patient.put("risk2Yr", 0.0);
        patient.put("risk5Yr", 0.0);
        patient.put("risk", Risk.UNKNOWN);
        patient.put("active", true);
        patient.put("userId", auth.getCurrentUser().getUid());
        patient.put("createdAt", FieldValue.serverTimestamp());

        Map<String, Object> historyMap = new HashMap<>();
        for (Map.Entry<String, Disease> entry : selectedDiseases.entrySet()) {
            String diseaseName = entry.getKey();
            Disease d = entry.getValue();
            d.setDetails(diseaseDetailsByName.getOrDefault(diseaseName, ""));
            historyMap.put(diseaseName, d.toMap());
        }
        patient.put("history", historyMap);

        db.collection("Patients").add(patient).addOnSuccessListener(documentReference -> {
            String newPatientId = documentReference.getId();
            documentReference.update("patientId", newPatientId);
            Log.d(TAG, "Patient created with ID: " + newPatientId);
            saveDiseasesAndMedications(newPatientId);
            Toast.makeText(this, "Patient added!", Toast.LENGTH_SHORT).show();
            navigateToDetails(newPatientId);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error adding patient", e);
            Toast.makeText(this, "Error adding patient.", Toast.LENGTH_SHORT).show();
        });
    }

    private void updatePatient() {
        DocumentReference patientRef = db.collection("Patients").document(patientId);
        Map<String, Object> patientUpdate = new HashMap<>();
        patientUpdate.put("firstName", firstNameEditText.getText().toString().trim());
        patientUpdate.put("lastName", lastNameEditText.getText().toString().trim());
        patientUpdate.put("fullName", firstNameEditText.getText().toString().trim() + " " + lastNameEditText.getText().toString().trim());
        patientUpdate.put("birthDate", dobTextView.getText().toString());
        patientUpdate.put("gender", getSelectedGender());
        patientUpdate.put("notes", notesEditText.getText().toString().trim());
        patientUpdate.put("updatedAt", FieldValue.serverTimestamp());

        Map<String, Object> historyMap = new HashMap<>();
        for (Map.Entry<String, Disease> entry : selectedDiseases.entrySet()) {
            String diseaseName = entry.getKey();
            Disease d = entry.getValue();
            d.setDetails(diseaseDetailsByName.getOrDefault(diseaseName, ""));
            historyMap.put(diseaseName, d.toMap());
        }
        patientUpdate.put("history", historyMap);

        patientRef.update(patientUpdate).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Patient document updated.");
            handleSubCollectionUpdates();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error updating patient", e);
            Toast.makeText(this, "Error updating patient.", Toast.LENGTH_SHORT).show();
        });
    }

    private void handleSubCollectionUpdates() {
        // Create a task for deleting all old diseases
        Task<Void> deleteDiseasesTask = db.collection("Diseases").whereEqualTo("patientId", patientId).get()
                .onSuccessTask(queryDocumentSnapshots -> {
                    List<Task<Void>> deleteTasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        deleteTasks.add(doc.getReference().delete());
                    }
                    return Tasks.whenAll(deleteTasks);
                });

        // Create a task for deleting all old medication assignments
        Task<Void> deleteMedsTask = db.collection("MedicationAssignments").whereEqualTo("patientId", patientId).get()
                .onSuccessTask(queryDocumentSnapshots -> {
                    List<Task<Void>> deleteTasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        deleteTasks.add(doc.getReference().delete());
                    }
                    return Tasks.whenAll(deleteTasks);
                });

        // Wait for BOTH deletion tasks to complete
        Tasks.whenAll(deleteDiseasesTask, deleteMedsTask).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Finished deleting all old sub-collection data.");
            // After all deletions are done, start the save process and wait for it to complete
            saveDiseasesAndMedications(patientId).addOnSuccessListener(aVoid1 -> {
                Log.d(TAG, "Finished saving all new sub-collection data.");
                Toast.makeText(this, "Patient updated!", Toast.LENGTH_SHORT).show();
                navigateToDetails(patientId);
            });
        });
    }

    /**
     * Saves all selected diseases and their associated medications.
     * Returns a single Task that completes when ALL save operations are finished.
     */
    private Task<Void> saveDiseasesAndMedications(String targetPatientId) {
        List<Task<Void>> allSaveTasks = new ArrayList<>();
        if (selectedDiseases.isEmpty()) {
            return Tasks.forResult(null);
        }

        for (Map.Entry<String, Disease> entry : selectedDiseases.entrySet()) {
            String diseaseName = entry.getKey();
            Disease disease = entry.getValue();
            disease.setPatientId(targetPatientId);
            disease.setDetails(diseaseDetailsByName.getOrDefault(diseaseName, ""));

            // Create a task that saves a disease and then saves its medications
            Task<Void> fullDiseaseSaveTask = db.collection("Diseases").add(disease.toMap())
                    .onSuccessTask(diseaseRef -> {
                        // Chain the subsequent operations:
                        // 1. Update the newly created disease with its own ID.
                        Task<Void> updateIdTask = diseaseRef.update("diseaseId", diseaseRef.getId());
                        // 2. Save all medications for this disease.
                        Task<Void> saveMedsTask = saveMedicationAssignments(targetPatientId, diseaseName, diseaseRef.getId());
                        // Return a new task that completes when both of the above are done.
                        return Tasks.whenAll(updateIdTask, saveMedsTask);
                    });

            allSaveTasks.add(fullDiseaseSaveTask);
        }

        // Return a single master task that waits for ALL disease/medication chains to finish.
        return Tasks.whenAll(allSaveTasks);
    }

    /**
     * Saves all medication assignments for a given disease.
     * Returns a single Task that completes when ALL assignments are saved.
     */
    private Task<Void> saveMedicationAssignments(String patientId, String diseaseName, String diseaseId) {
        List<MedicationAssignment> assignments = medicationAssignmentsByDisease.get(diseaseName);
        if (assignments == null || assignments.isEmpty()) {
            return Tasks.forResult(null);
        }

        List<Task<Void>> assignmentTasks = new ArrayList<>();
        for (MedicationAssignment assignment : assignments) {
            assignment.setPatientId(patientId);
            assignment.setDiseaseId(diseaseId);

            // For each assignment, create a task that adds the document, then updates it with its ID.
            Task<Void> task = db.collection("MedicationAssignments").add(assignment.toMap())
                    .onSuccessTask(ref -> ref.update("assignmentId", ref.getId()));

            assignmentTasks.add(task);
        }

        // Return a single task that waits for all assignments in this batch to complete.
        return Tasks.whenAll(assignmentTasks);
    }

    private void loadPatientData() {
        db.collection("Patients").document(patientId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                firstNameEditText.setText(doc.getString("firstName"));
                lastNameEditText.setText(doc.getString("lastName"));
                dobTextView.setText(doc.getString("birthDate"));
                notesEditText.setText(doc.getString("notes"));
                String gender = doc.getString("gender");
                if (gender != null) {
                    if (gender.equalsIgnoreCase("Male")) {
                        ((RadioButton) findViewById(R.id.addPatientMaleRadio)).setChecked(true);
                    } else if (gender.equalsIgnoreCase("Female")) {
                        ((RadioButton) findViewById(R.id.addPatientFemaleRadio)).setChecked(true);
                    }
                }
                loadDiseasesAndMedications();
            } else {
                Toast.makeText(this, "Patient data not found.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to load patient data.", Toast.LENGTH_SHORT).show());
    }

    private void loadDiseasesAndMedications() {
        db.collection("Diseases").whereEqualTo("patientId", patientId).get().addOnSuccessListener(diseaseSnaps -> {
            for (QueryDocumentSnapshot diseaseDoc : diseaseSnaps) {
                Disease disease = diseaseDoc.toObject(Disease.class);
                String diseaseName = disease.getName();

                selectedDiseases.put(diseaseName, disease);
                diseaseDetailsByName.put(diseaseName, disease.getDetails());
                medicationAssignmentsByDisease.put(diseaseName, new ArrayList<>());

                IntStream.range(0, medHistoryContainer.getChildCount())
                        .mapToObj(medHistoryContainer::getChildAt)
                        .filter(ViewGroup.class::isInstance)
                        .map(ViewGroup.class::cast)
                        .forEach(sectionView -> {
                            CheckBox cb = findCheckboxByText(sectionView, diseaseName);
                            if (cb != null) {
                                cb.setChecked(true);
                                EditText detailsInput = findEditTextInView(sectionView);
                                if (detailsInput != null) {
                                    detailsInput.setText(disease.getDetails());
                                }
                                loadMedicationsForDisease(disease.getDiseaseId(), diseaseName);
                            }
                        });
            }
        });
    }

    private CheckBox findCheckboxByText(ViewGroup group, String text) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof CheckBox && ((CheckBox) child).getText().toString().equals(text)) {
                return (CheckBox) child;
            }
        }
        return null;
    }

    private EditText findEditTextInView(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof EditText) {
                return (EditText) child;
            }
        }
        return null;
    }

    private void loadMedicationsForDisease(String diseaseId, String diseaseName) {
        db.collection("MedicationAssignments").whereEqualTo("diseaseId", diseaseId).get().addOnSuccessListener(medSnaps -> {
            for (QueryDocumentSnapshot medAssignDoc : medSnaps) {
                MedicationAssignment assignment = medAssignDoc.toObject(MedicationAssignment.class);
                medicationAssignmentsByDisease.get(diseaseName).add(assignment);
                db.collection("Medications").document(assignment.getMedicationId()).get().addOnSuccessListener(medDoc -> {
                    if (medDoc.exists()) {
                        addMedicationView(diseaseName, assignment, medDoc.getString("name"));
                    }
                });
            }
        });
    }

    private String getSelectedGender() {
        int selectedId = genderRadioGroup.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton selected = findViewById(selectedId);
            return selected.getText().toString();
        }
        return null;
    }

    private void navigateToDetails(String targetPatientId) {
        Intent intent = new Intent(AddOrEditPatientActivity.this, PatientDetailsActivity.class);
        intent.putExtra("patientId", targetPatientId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
