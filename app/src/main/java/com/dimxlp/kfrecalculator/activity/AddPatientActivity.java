package com.dimxlp.kfrecalculator.activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.ui.MedicationPickerBottomSheet;
import com.dimxlp.kfrecalculator.model.Disease;
import com.dimxlp.kfrecalculator.model.MedicationAssignment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddPatientActivity extends AppCompatActivity implements MedicationPickerBottomSheet.OnMedicationSelectedListener {

    private static final String TAG = "RAFI|AddPatient";

    private TextInputEditText firstNameEditText, lastNameEditText, notesEditText;
    private TextView dobTextView;
    private RadioGroup genderRadioGroup;
    private LinearLayout medHistoryContainer;
    private Button addPatientButton;

    private final List<String> diseasesList = List.of("Diabetes", "Hypertension", "Cardiovascular Disease", "Kidney Disease");
    private final Map<String, Disease> selectedDiseases = new HashMap<>();
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
        setupDatePicker();
        setupDiseaseChecklist();
    }

    private void initViews() {
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
            inputDetails.setHint("Enter details");
            inputDetails.setVisibility(View.GONE);

            Button btnAddMedication = view.findViewById(R.id.btnAddMedication);
            LinearLayout medicationsContainer = view.findViewById(R.id.medicationsContainer);
            btnAddMedication.setVisibility(View.GONE);

            txtDiseaseName.setText(disease);

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    inputDetails.setVisibility(View.VISIBLE);
                    btnAddMedication.setVisibility(View.VISIBLE);
                    selectedDiseases.put(disease, new Disease(disease));
                } else {
                    inputDetails.setVisibility(View.GONE);
                    btnAddMedication.setVisibility(View.GONE);
                    medicationsContainer.removeAllViews();
                    selectedDiseases.remove(disease);
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

        Disease disease = selectedDiseases.get(currentDiseaseId);
        disease.addMedication(new MedicationAssignment(medicationId, medicationName, frequency));

        for (int i = 0; i < medHistoryContainer.getChildCount(); i++) {
            View container = medHistoryContainer.getChildAt(i);
            LinearLayout medsLayout = container.findViewById(R.id.medicationsContainer);
            if (medsLayout != null && currentDiseaseId.equals(medsLayout.getTag())) {
                TextView medView = new TextView(this);
                medView.setText("- " + medicationName + " (" + frequency + ")");
                medView.setTextColor(getColor(R.color.textSecondary));
                medsLayout.addView(medView);
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

        Map<String, Object> patient = new HashMap<>();
        patient.put("firstName", firstName);
        patient.put("lastName", lastName);
        patient.put("dob", dob);
        patient.put("gender", gender);
        patient.put("notes", notes);
        patient.put("risk2Yr", 0.0);
        patient.put("risk5Yr", 0.0);
        patient.put("active", true);

        String uid = auth.getCurrentUser().getUid();

        db.collection("Users").document(uid).collection("Patients")
                .add(patient)
                .addOnSuccessListener(documentReference -> {
                    saveDiseases(documentReference);
                    Toast.makeText(this, "Patient added!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding patient", e);
                    Toast.makeText(this, "Error adding patient.", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveDiseases(DocumentReference patientRef) {
        for (Disease disease : selectedDiseases.values()) {
            Map<String, Object> diseaseData = new HashMap<>();
            diseaseData.put("name", disease.getName());
            diseaseData.put("Medications", disease.getMedicationsAsMap());

            patientRef.collection("Diseases").add(diseaseData);
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
