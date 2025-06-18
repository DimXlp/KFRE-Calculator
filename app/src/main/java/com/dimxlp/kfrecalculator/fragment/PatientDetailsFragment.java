package com.dimxlp.kfrecalculator.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.dimxlp.kfrecalculator.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PatientDetailsFragment extends Fragment {

    private FirebaseFirestore db;
    private String patientId;

    // Views
    private TextView nameView, ageGenderView, dobView, lastUpdatedView, notesView;
    private ChipGroup historyChips;
    private LinearLayout medicationsContainer, assessmentsContainer;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getParentFragmentManager().setFragmentResultListener(
                "reload_assessments", this, (key, bundle) -> loadAssessments());
        return inflater.inflate(R.layout.fragment_patient_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // Get patientId from arguments
        patientId = getArguments() != null ? getArguments().getString("patientId") : null;
        db = FirebaseFirestore.getInstance();

        // Init views
        nameView = v.findViewById(R.id.patientDetailName);
        ageGenderView = v.findViewById(R.id.patientDetailAgeGender);
        dobView = v.findViewById(R.id.patientDetailDob);
        lastUpdatedView = v.findViewById(R.id.patientDetailLastUpdated);
        notesView = v.findViewById(R.id.patientNotes);
        historyChips = v.findViewById(R.id.historyChips);
        medicationsContainer = v.findViewById(R.id.medicationsContainer);
        assessmentsContainer = v.findViewById(R.id.assessmentsContainer);

        if (patientId != null) {
            loadPatientDetails();
            loadMedications();
            loadAssessments();
        }
    }

    private void loadPatientDetails() {
        db.collection("Patients").document(patientId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String firstName = snapshot.getString("firstName");
                        String lastName = snapshot.getString("lastName");
                        String gender = snapshot.getString("gender");
                        String dob = snapshot.getString("birthDate");
                        String notes = snapshot.getString("notes");
                        mapHistory(snapshot);
//                        Date updated = snapshot.getDate("lastUpdated");

                        nameView.setText(firstName + " " + lastName);
                        ageGenderView.setText(getAgeFromDob(dob) + " • " + gender);
                        dobView.setText("Date of Birth: " + dob);
//                        lastUpdatedView.setText("Last updated: " + formatDate(updated));
                        notesView.setText(notes != null ? notes : "No notes available");
                    }
                });
    }

    private void mapHistory(DocumentSnapshot snapshot) {
        Object historyObject = snapshot.get("history");

        if (historyObject instanceof Map) {
            Map<String, Map<String, Object>> historyMap = (Map<String, Map<String, Object>>) historyObject;
            List<String> selectedHistory = new ArrayList<>();

            for (Map.Entry<String, Map<String, Object>> entry : historyMap.entrySet()) {
                Map<String, Object> diseaseDetails = entry.getValue();
                Boolean hasDisease = (Boolean) diseaseDetails.get("hasDisease");

                if (hasDisease != null && hasDisease) {
                    String diseaseName = (String) diseaseDetails.get("name");
                    String details = (String) diseaseDetails.get("details");

                    String display = diseaseName;
                    if (details != null && !details.isEmpty()) {
                        display += " (" + details + ")";
                    }

                    selectedHistory.add(display);
                }
            }

            populateChips(selectedHistory);
        }
    }

    private void populateChips(List<String> history) {
        historyChips.removeAllViews();
        if (history != null) {
            for (String item : history) {
                Chip chip = new Chip(requireContext(), null, R.style.App_Chip_Assist);
                chip.setText(item);
                chip.setChipBackgroundColorResource(R.color.colorAccentLight);
                chip.setTextColor(getResources().getColor(R.color.colorPrimary, null));
                chip.setClickable(false);
                chip.setCheckable(false);
                historyChips.addView(chip);
            }
        }
    }

    private void loadMedications() {
        medicationsContainer.removeAllViews();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("MedicationAssignments")
                .whereEqualTo("patientId", patientId)
                .get()
                .addOnSuccessListener(assignments -> {
                    for (QueryDocumentSnapshot assignment : assignments) {
                        String medicationId = assignment.getString("medicationId");
                        String frequency = assignment.getString("frequency");

                        if (medicationId != null) {
                            db.collection("Medications").document(medicationId)
                                    .get()
                                    .addOnSuccessListener(medDoc -> {
                                        if (medDoc.exists()) {
                                            String name = medDoc.getString("name");
                                            String dosage = medDoc.getString("dosage");

                                            String display = name + " " + dosage + " • " + frequency;

                                            TextView medView = new TextView(requireContext());
                                            medView.setText(display);
                                            medView.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
                                            medView.setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary));
                                            medView.setPadding(0, 8, 0, 8);
                                            medicationsContainer.addView(medView);
                                        }
                                    });
                        }
                    }
                });
    }

    private void loadAssessments() {
        db.collection("KfreCalculations")
                .whereEqualTo("patientId", patientId)
                .get()
                .addOnSuccessListener(query -> {
                    assessmentsContainer.removeAllViews();
                    for (QueryDocumentSnapshot doc : query) {
                        Double risk2yr = doc.getDouble("risk2Yr");
                        Double risk5yr = doc.getDouble("risk5Yr");
                        long timestamp = doc.getLong("createdAt");
                        String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                .format(new Date(timestamp));

                        TextView resultView = new TextView(requireContext());
                        String formatted2yr = String.format(Locale.getDefault(), "%.2f", risk2yr);
                        String formatted5yr = String.format(Locale.getDefault(), "%.2f", risk5yr);
                        resultView.setText("2-Yr Risk: " + formatted2yr + "% • 5-Yr Risk: " + formatted5yr + "% • " + date);
                        resultView.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
                        resultView.setTextColor(getResources().getColor(R.color.textSecondary, null));
                        resultView.setPadding(0, 8, 0, 8);
                        assessmentsContainer.addView(resultView);
                    }
                });
    }

    private String getAgeFromDob(String dobString) {
        if (dobString == null || dobString.isEmpty()) return "-";

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
            Date dob = sdf.parse(dobString);

            Calendar birthCal = Calendar.getInstance();
            birthCal.setTime(dob);

            Calendar today = Calendar.getInstance();

            int age = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR);

            // Adjust if birthday hasn't occurred yet this year
            if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }

            return String.valueOf(age);
        } catch (Exception e) {
            e.printStackTrace();
            return "-";
        }
    }
}

