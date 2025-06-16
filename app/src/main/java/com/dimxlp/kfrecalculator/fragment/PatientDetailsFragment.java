package com.dimxlp.kfrecalculator.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.dimxlp.kfrecalculator.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.Arrays;
import java.util.List;

public class PatientDetailsFragment extends Fragment {

    // Views
    private TextView nameTextView, ageGenderTextView, dobTextView, lastUpdatedTextView, notesTextView;
    private ChipGroup historyChipsGroup;
    private LinearLayout medicationsContainer, assessmentsContainer;

    public PatientDetailsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Views
        nameTextView = view.findViewById(R.id.patientDetailName);
        ageGenderTextView = view.findViewById(R.id.patientDetailAgeGender);
        dobTextView = view.findViewById(R.id.patientDetailDob);
        lastUpdatedTextView = view.findViewById(R.id.patientDetailLastUpdated);
        notesTextView = view.findViewById(R.id.patientNotes);
        historyChipsGroup = view.findViewById(R.id.historyChips);
        medicationsContainer = view.findViewById(R.id.medicationsContainer);
        assessmentsContainer = view.findViewById(R.id.assessmentsContainer);

        // Load mock data
        loadPatientInfo();
        loadMedicalHistory();
        loadMedications();
        loadAssessments();
    }

    private void loadPatientInfo() {
        nameTextView.setText("John Doe");
        ageGenderTextView.setText("54 • Male");
        dobTextView.setText("DOB: 12 Mar 1971");
        lastUpdatedTextView.setText("Last updated: May 2025");
    }

    private void loadMedicalHistory() {
        List<String> conditions = Arrays.asList("Diabetes", "Hypertension", "CKD Stage 3");

        historyChipsGroup.removeAllViews();
        for (String condition : conditions) {
            Chip chip = new Chip(requireContext());
            chip.setText(condition);
            chip.setChipBackgroundColorResource(R.color.colorAccentLight);
            chip.setTextColor(getResources().getColor(R.color.colorPrimary, null));
            chip.setChipCornerRadius(24);
            chip.setClickable(false);
            chip.setCheckable(false);
            historyChipsGroup.addView(chip);
        }

        notesTextView.setText("Patient shows early signs of nephropathy. Monitoring required.");
    }

    private void loadMedications() {
        medicationsContainer.removeAllViews();

        List<String> meds = Arrays.asList(
                "Metformin 500mg • Daily",
                "Lisinopril 10mg • Every Morning"
        );

        for (String med : meds) {
            TextView medView = new TextView(requireContext());
            medView.setText(med);
            medView.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
            medView.setTextColor(getResources().getColor(R.color.textSecondary, null));
            medView.setPadding(0, 8, 0, 8);
            medicationsContainer.addView(medView);
        }
    }

    private void loadAssessments() {
        assessmentsContainer.removeAllViews();

        List<String> results = Arrays.asList(
                "2-Yr Risk: 3.2% • 5-Yr Risk: 7.6% • Apr 2025",
                "2-Yr Risk: 2.8% • 5-Yr Risk: 6.5% • Jan 2025"
        );

        for (String result : results) {
            TextView resultView = new TextView(requireContext());
            resultView.setText(result);
            resultView.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
            resultView.setTextColor(getResources().getColor(R.color.textSecondary, null));
            resultView.setPadding(0, 8, 0, 8);
            assessmentsContainer.addView(resultView);
        }
    }
}
