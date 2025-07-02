package com.dimxlp.kfrecalculator.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.activity.AddOrEditPatientActivity;
import com.dimxlp.kfrecalculator.activity.FullscreenPatientCkdEpiActivity;
import com.dimxlp.kfrecalculator.activity.FullscreenPatientKfreActivity;
import com.dimxlp.kfrecalculator.adapter.CkdEpiAssessmentAdapter;
import com.dimxlp.kfrecalculator.adapter.KfreAssessmentAdapter;
import com.dimxlp.kfrecalculator.model.CkdEpiCalculation;
import com.dimxlp.kfrecalculator.model.KfreCalculation;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PatientDetailsFragment extends Fragment {

    private static final String TAG = "RAFI|PatientDetailsFragment";
    private FirebaseFirestore db;
    private String patientId;

    // Views
    private TextView nameView, ageGenderView, dobView, lastUpdatedView, notesView;
    private ImageButton editPatientButton;
    private ImageView riskIconView;
    private TextView riskTextView;
    private ChipGroup historyChips;
    private LinearLayout medicationsContainer;
    private RecyclerView rvKfreAssessments;
    private RecyclerView rvCkdEpiAssessments;
    private KfreAssessmentAdapter kfreAdapter;
    private CkdEpiAssessmentAdapter ckdEpiAdapter;

    private final ActivityResultLauncher<Intent> fullscreenKfreLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(TAG, "Received result from FullscreenPatientKfreActivity.");
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.d(TAG, "Data has changed, reloading KFRE assessments.");
                    loadKfreAssessments();
                }
            }
    );

    private final ActivityResultLauncher<Intent> fullscreenCkdEpiLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(TAG, "Received result from FullscreenPatientCkdEpiActivity.");
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.d(TAG, "Data has changed, reloading CKD-EPI assessments.");
                    loadCkdEpiAssessments();
                }
            }
    );

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreateView called.");
        getParentFragmentManager().setFragmentResultListener(
                "reload_kfre_assessments", this, (key, bundle) -> {
                    Log.d(TAG, "Received fragment result to reload KFRE assessments.");
                    loadKfreAssessments();
                });
        getParentFragmentManager().setFragmentResultListener(
                "reload_ckd_epi_assessments", this, (key, bundle) -> {
                    Log.d(TAG, "Received fragment result to reload CKD-EPI assessments.");
                    loadCkdEpiAssessments();
                });
        return inflater.inflate(R.layout.fragment_patient_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        Log.d(TAG, "onViewCreated called.");

        // Get patientId from arguments
        patientId = getArguments() != null ? getArguments().getString("patientId") : null;
        db = FirebaseFirestore.getInstance();

        // Init views
        editPatientButton = v.findViewById(R.id.btnEditPatient);
        nameView = v.findViewById(R.id.patientDetailName);
        ageGenderView = v.findViewById(R.id.patientDetailAgeGender);
        dobView = v.findViewById(R.id.patientDetailDob);
        notesView = v.findViewById(R.id.patientNotes);
        riskIconView = v.findViewById(R.id.patientDetailRiskIcon);
        riskTextView = v.findViewById(R.id.patientDetailRiskText);
        historyChips = v.findViewById(R.id.historyChips);
        medicationsContainer = v.findViewById(R.id.medicationsContainer);
        rvKfreAssessments = v.findViewById(R.id.rvKfreAssessments);
        rvCkdEpiAssessments = v.findViewById(R.id.rvCkdEpiAssessments);

        kfreAdapter = new KfreAssessmentAdapter(getContext(), new ArrayList<>(), new KfreAssessmentAdapter.AssessmentClickListener() {
            @Override
            public void onAssessmentClick(KfreCalculation calc) {
                showKfreAssessmentDetails(calc);
            }

            @Override
            public void onAssessmentDelete(KfreCalculation calc) {
                confirmAndDeleteKfreAssessment(calc);
            }
        });
        rvKfreAssessments.setLayoutManager(new LinearLayoutManager(getContext()));
        rvKfreAssessments.setAdapter(kfreAdapter);

        ckdEpiAdapter = new CkdEpiAssessmentAdapter(getContext(), new ArrayList<>(), new CkdEpiAssessmentAdapter.AssessmentClickListener() {
            @Override
            public void onAssessmentClick(CkdEpiCalculation calc) {
                showCkdEpiAssessmentDetails(calc);
            }

            @Override
            public void onAssessmentDelete(CkdEpiCalculation calc) {
                confirmAndDeleteCkdEpiAssessment(calc);
            }
        });
        rvCkdEpiAssessments.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCkdEpiAssessments.setAdapter(ckdEpiAdapter);

        editPatientButton.setOnClickListener(view -> {
            Log.d(TAG, "Edit patient button clicked for patientId: " + patientId);
            Intent intent = new Intent(getContext(), AddOrEditPatientActivity.class);
            intent.putExtra("patientId", patientId);
            startActivity(intent);
        });

        if (patientId != null) {
            Log.i(TAG, "Displaying details for patientId: " + patientId);
            loadPatientDetails();
            loadMedications();
            loadKfreAssessments();
            loadCkdEpiAssessments();
            setupKfreCardExpansion(v);
            setupCkdEpiCardExpansion(v);
        } else {
            Log.e(TAG, "Patient ID is null. Cannot load details.");
        }
    }

    private void showCkdEpiAssessmentDetails(CkdEpiCalculation calc) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_ckd_epi_assessment_details, null);

        // Find all views from the layout
        TextView txtAge = sheetView.findViewById(R.id.txtDetailAge);
        TextView txtGender = sheetView.findViewById(R.id.txtDetailGender);
        TextView txtCreatinine = sheetView.findViewById(R.id.txtDetailCreatinine);
        TextView txtEgfr = sheetView.findViewById(R.id.txtDetailEgfr);
        TextView txtNotes = sheetView.findViewById(R.id.txtDetailNotes);
        Button btnClose = sheetView.findViewById(R.id.btnCloseDetail);

        // Set data for relevant fields
        txtAge.setText(String.valueOf(calc.getAge()));
        txtGender.setText(calc.getSex());
        txtCreatinine.setText(calc.getCreatinine() + " mg/dL");
        txtEgfr.setText(String.format(Locale.getDefault(), "%.2f", calc.getResult()) + " mL/min/1.73m²");
        txtNotes.setText(calc.getNotes() == null || calc.getNotes().trim().isEmpty() ? "—" : calc.getNotes());

        btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
    }

    private void confirmAndDeleteCkdEpiAssessment(CkdEpiCalculation calc) {
        Log.d(TAG, "Showing delete confirmation for CKD-EPI assessment: " + calc.getCkdEpiCalculationId());
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Assessment")
                .setMessage("Are you sure you want to delete this CKD-EPI assessment?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    Log.i(TAG, "Deleting CKD-EPI assessment: " + calc.getCkdEpiCalculationId());
                    FirebaseFirestore.getInstance()
                            .collection("CkdEpiCalculations")
                            .document(calc.getCkdEpiCalculationId())
                            .delete()
                            .addOnSuccessListener(unused -> {
                                Log.d(TAG, "Successfully deleted CKD-EPI assessment.");
                                Toast.makeText(getContext(), "Assessment deleted", Toast.LENGTH_SHORT).show();
                                loadCkdEpiAssessments();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to delete CKD-EPI assessment: " + calc.getCkdEpiCalculationId(), e);
                                Toast.makeText(getContext(), "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showKfreAssessmentDetails(KfreCalculation calc) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_kfre_assessment_details, null);

        TextView txtAge = sheetView.findViewById(R.id.txtDetailAge);
        TextView txtGender = sheetView.findViewById(R.id.txtDetailGender);
        TextView txtEgfr = sheetView.findViewById(R.id.txtDetailEgfr);
        TextView txtAcr = sheetView.findViewById(R.id.txtDetailAcr);
        TextView txtRisk2 = sheetView.findViewById(R.id.txtDetailRisk2);
        TextView txtRisk5 = sheetView.findViewById(R.id.txtDetailRisk5);
        TextView txtNotes = sheetView.findViewById(R.id.txtDetailNotes);
        Button btnClose = sheetView.findViewById(R.id.btnCloseDetail);

        txtAge.setText(String.valueOf(calc.getAge()));
        txtGender.setText(calc.getSex());
        txtEgfr.setText(String.format(Locale.getDefault(), "%.2f", calc.getEgfr()));
        txtAcr.setText(String.format(Locale.getDefault(), "%.2f", calc.getAcr()));
        txtRisk2.setText(String.format(Locale.getDefault(), "%.2f%%", calc.getRisk2Yr()));
        txtRisk5.setText(String.format(Locale.getDefault(), "%.2f%%", calc.getRisk5Yr()));
        txtNotes.setText(calc.getNotes() == null || calc.getNotes().trim().isEmpty() ? "—" : calc.getNotes());

        btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
    }

    private void confirmAndDeleteKfreAssessment(KfreCalculation calc) {
        Log.d(TAG, "Showing delete confirmation for KFRE assessment: " + calc.getKfreCalculationId());
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Assessment")
                .setMessage("Are you sure you want to delete this KFRE assessment?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    Log.i(TAG, "Deleting KFRE assessment: " + calc.getKfreCalculationId());
                    FirebaseFirestore.getInstance()
                            .collection("KfreCalculations")
                            .document(calc.getKfreCalculationId())
                            .delete()
                            .addOnSuccessListener(unused -> {
                                Log.d(TAG, "Successfully deleted KFRE assessment.");
                                Toast.makeText(getContext(), "Assessment deleted", Toast.LENGTH_SHORT).show();
                                loadKfreAssessments();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to delete KFRE assessment: " + calc.getKfreCalculationId(), e);
                                Toast.makeText(getContext(), "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadPatientDetails() {
        Log.d(TAG, "Loading patient details...");
        db.collection("Patients").document(patientId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Log.d(TAG, "Patient details found.");
                        String firstName = snapshot.getString("firstName");
                        String lastName = snapshot.getString("lastName");
                        String gender = snapshot.getString("gender");
                        String dob = snapshot.getString("birthDate");
                        String notes = snapshot.getString("notes");
                        mapMedicalHistory(snapshot);

                        nameView.setText(firstName + " " + lastName);
                        ageGenderView.setText(getAgeFromDob(dob) + " • " + gender);
                        dobView.setText("Date of Birth: " + dob);
                        notesView.setText(notes != null ? notes : "No notes available");
                    } else {
                        Log.w(TAG, "Patient document not found for id: " + patientId);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading patient details.", e));
    }

    private void mapMedicalHistory(DocumentSnapshot snapshot) {
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
        Log.d(TAG, "Loading medications...");
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

    private void loadKfreAssessments() {
        Log.d(TAG, "Loading KFRE assessments...");
        db.collection("KfreCalculations")
                .whereEqualTo("patientId", patientId)
                .get()
                .addOnSuccessListener(query -> {
                    List<KfreCalculation> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        KfreCalculation calc = doc.toObject(KfreCalculation.class);
                        list.add(calc);
                    }
                    Log.d(TAG, "Found " + list.size() + " KFRE assessments.");

                    if (!list.isEmpty()) {
                        Collections.sort(list, (a, b) ->
                                Long.compare(b.getCreatedAt(), a.getCreatedAt()));

                        kfreAdapter.updateData(list);

                        KfreCalculation latestAssessment = list.get(0);
                        updateRiskIndicator(latestAssessment.getRisk2Yr());
                    } else {
                        setNoRiskDataState();
                        kfreAdapter.updateData(new ArrayList<>());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading KFRE assessments.", e);
                    setNoRiskDataState();
                });
    }

    private void updateRiskIndicator(double risk2Yr) {
        if (getContext() == null) return; // Ensure fragment is still attached

        int riskColorId, riskColor;
        String riskText;
        int iconResId;

        if (risk2Yr >= 40) {
            riskText = "High Risk";
            riskColorId = R.color.colorHighRiskStat;
            riskColor = ContextCompat.getColor(getContext(), R.color.colorHighRiskStat);
            iconResId = R.drawable.ic_risk;
        } else if (risk2Yr >= 10) {
            riskText = "Medium Risk";
            riskColorId = R.color.colorMediumRiskStat;
            riskColor = ContextCompat.getColor(getContext(), R.color.colorMediumRiskStat);
            iconResId = R.drawable.ic_medium;
        } else {
            riskText = "Low Risk";
            riskColorId = R.color.colorLowRiskStat;
            riskColor = ContextCompat.getColor(getContext(), R.color.colorLowRiskStat);
            iconResId = R.drawable.ic_tick;
        }

        riskTextView.setText(riskText);
        riskTextView.setTextColor(riskColor);
        riskIconView.setImageResource(iconResId);
        riskIconView.setBackgroundColor(ContextCompat.getColor(requireContext(), riskColorId));

        riskIconView.setVisibility(View.VISIBLE);
        riskTextView.setVisibility(View.VISIBLE);
    }

    /**
     * Sets the risk indicator to a default "No Data" state.
     */
    private void setNoRiskDataState() {
        if (getContext() == null) return; // Ensure fragment is still attached

        int defaultColor = ContextCompat.getColor(getContext(), R.color.colorRecentLast);
        riskTextView.setText("N/A");
        riskTextView.setTextColor(defaultColor);
        riskIconView.setImageResource(R.drawable.ic_question);
        riskIconView.setBackgroundTintList(ColorStateList.valueOf(defaultColor));
    }

    private void loadCkdEpiAssessments() {
        Log.d(TAG, "Loading CKD-EPI assessments...");
        db.collection("CkdEpiCalculations")
                .whereEqualTo("patientId", patientId)
                .get()
                .addOnSuccessListener(query -> {
                    List<CkdEpiCalculation> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        CkdEpiCalculation calc = doc.toObject(CkdEpiCalculation.class);
                        list.add(calc);
                    }
                    Log.d(TAG, "Found " + list.size() + " CKD-EPI assessments.");

                    Collections.sort(list, (a, b) ->
                            Long.compare(a.getCreatedAt(), b.getCreatedAt()));

                    ckdEpiAdapter.updateData(list);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading CKD-EPI assessments.", e));
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
            Log.e(TAG, "Failed to parse DOB string: " + dobString, e);
            e.printStackTrace();
            return "-";
        }
    }

    private void setupKfreCardExpansion(View rootView) {
        View kfreCard = rootView.findViewById(R.id.kfreAssessmentCard);
        ImageView expandButton = kfreCard.findViewById(R.id.btnKfreExpandCollapse);

        expandButton.setOnClickListener(v -> {
            Log.d(TAG, "KFRE card expand button clicked. Launching fullscreen activity.");
            Intent intent = new Intent(getContext(), FullscreenPatientKfreActivity.class);
            intent.putExtra("patientId", patientId);
            fullscreenKfreLauncher.launch(intent);
        });
    }

    private void setupCkdEpiCardExpansion(View rootView) {
        View ckdEpiCard = rootView.findViewById(R.id.ckdEpiAssessmentCard);
        ImageView expandButton = ckdEpiCard.findViewById(R.id.btnCkdEpiExpandCollapse);

        expandButton.setOnClickListener(v -> {
            Log.d(TAG, "CKD-EPI card expand button clicked. Launching fullscreen activity.");
            Intent intent = new Intent(getContext(), FullscreenPatientCkdEpiActivity.class);
            intent.putExtra("patientId", patientId);
            fullscreenCkdEpiLauncher.launch(intent);
        });
    }
}

