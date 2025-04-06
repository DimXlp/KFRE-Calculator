package com.dimxlp.kfrecalculator.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddPatientActivity extends AppCompatActivity implements MedicationPickerBottomSheet.OnMedicationSelectedListener {

    private static final String TAG = "RAFI|AddPatient";

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
        setContentView(R.layout.activity_add_patient);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        setTopBarFunctionalities();
        setupDatePicker();
        setupDiseaseChecklist();
    }

    private void setTopBarFunctionalities() {
        appLogo.setOnClickListener(v -> {
            Log.d(TAG, "App Logo clicked");
            Intent intent = new Intent(AddPatientActivity.this, DashboardActivity.class);
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
                    Toast.makeText(this, "Profile Activity coming soon", Toast.LENGTH_SHORT).show();
                } else if (itemId == R.id.menu_logout) {
                    Toast.makeText(this, "Logout feature coming soon", Toast.LENGTH_SHORT).show();
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

        addPatientButton.setOnClickListener(v -> savePatient());
    }

    private void setupDatePicker() {
        dobTextView.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePicker = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> dobTextView.setText(dayOfMonth + "/" + (month + 1) + "/" + year),
                    calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
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

        for (int i = 0; i < medHistoryContainer.getChildCount(); i++) {
            View container = medHistoryContainer.getChildAt(i);
            LinearLayout medsLayout = container.findViewById(R.id.medicationsContainer);
            if (medsLayout != null && currentDiseaseId.equals(medsLayout.getTag())) {

                // Create horizontal layout
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);

                // TextView for medication info
                TextView medView = new TextView(this);
                medView.setText("- " + medicationName + " (" + frequency + ")");
                medView.setTextColor(getColor(R.color.textSecondary));
                medView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

                // Delete button
                Button deleteButton = new Button(this);
                deleteButton.setText("🗑");
                deleteButton.setBackgroundColor(getColor(android.R.color.transparent));
                deleteButton.setTextColor(getColor(R.color.colorHighRisk));
                deleteButton.setOnClickListener(v -> {
                    medsLayout.removeView(row);
                    medicationAssignmentsByDisease.get(currentDiseaseId).remove(assignment);
                    Log.d(TAG, "Removed medication: " + medicationName);
                });

                row.addView(medView);
                row.addView(deleteButton);
                medsLayout.addView(row);

                break;
            }
        }
    }

    private void savePatient() {
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String dob = dobTextView.getText().toString();
        String gender = getSelectedGender();
        String notes = notesEditText.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || dob.isEmpty() || gender == null) {
            Toast.makeText(this, "Please fill all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> historyMap = new HashMap<>();
        for (Map.Entry<String, Disease> entry : selectedDiseases.entrySet()) {
            String diseaseName = entry.getKey();
            Disease d = entry.getValue();
            d.setHasDisease(true);
            d.setDetails(diseaseDetailsByName.getOrDefault(diseaseName, ""));
            historyMap.put(diseaseName, d.toMap());
        }

        Map<String, Object> patient = new HashMap<>();
        patient.put("firstName", firstName);
        patient.put("lastName", lastName);
        patient.put("fullName", firstName + " " + lastName);
        patient.put("dob", dob);
        patient.put("gender", gender);
        patient.put("notes", notes);
        patient.put("risk2Yr", 0.0);
        patient.put("risk5Yr", 0.0);
        patient.put("risk", Risk.UNKNOWN);
        patient.put("active", true);
        patient.put("userId", auth.getCurrentUser().getUid());
        patient.put("history", historyMap);

        db.collection("Patients")
                .add(patient)
                .addOnSuccessListener(documentReference -> {
                    String patientId = documentReference.getId();
                    Log.d(TAG, "Patient saved with ID: " + patientId);
                    saveDiseases(patientId);
                    Toast.makeText(this, "Patient added!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(AddPatientActivity.this, PatientDetailsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding patient", e);
                    Toast.makeText(this, "Error adding patient.", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveDiseases(String patientId) {
        for (Map.Entry<String, Disease> entry : selectedDiseases.entrySet()) {
            String diseaseName = entry.getKey();
            Disease disease = entry.getValue();
            disease.setPatientId(patientId);
            disease.setDetails(diseaseDetailsByName.getOrDefault(diseaseName, ""));

            db.collection("Diseases")
                    .add(disease.toMap())
                    .addOnSuccessListener(ref -> {
                        String diseaseId = ref.getId();
                        Log.d(TAG, "Disease saved: " + disease.getName() + " | ID: " + diseaseId);
                        disease.setDiseaseId(diseaseId);
                        saveMedicationAssignments(patientId, diseaseName, diseaseId);

                        disease.setDiseaseId(diseaseId);
                        db.collection("Diseases").document(diseaseId)
                                .update("diseaseId", diseaseId)
                                .addOnSuccessListener(unused -> Log.d(TAG, "Disease ID updated: " + diseaseId))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to update disease ID", e));
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save disease: " + disease.getName(), e));
        }
    }

    private void saveMedicationAssignments(String patientId, String diseaseName, String diseaseId) {
        List<MedicationAssignment> assignments = medicationAssignmentsByDisease.get(diseaseName);
        if (assignments == null) return;

        for (MedicationAssignment assignment : assignments) {
            assignment.setPatientId(patientId);
            assignment.setDiseaseId(diseaseId);

            db.collection("MedicationAssignments")
                    .add(assignment.toMap())
                    .addOnSuccessListener(ref -> {
                        String generatedId = ref.getId();
                        assignment.setAssignmentId(generatedId);

                        db.collection("MedicationAssignments").document(generatedId)
                                .update("assignmentId", generatedId)
                                .addOnSuccessListener(unused -> Log.d(TAG, "Assignment ID updated: " + generatedId))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to update assignment ID", e));

                        Log.d(TAG, "MedicationAssignment saved with ID: " + generatedId);
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error saving medication assignment", e));
        }
    }

    private String getSelectedGender() {
        int selectedId = genderRadioGroup.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton selected = findViewById(selectedId);
            return selected.getText().toString();
        }
        return null;
    }
}
