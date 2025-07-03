package com.dimxlp.kfrecalculator.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.enumeration.Risk;
import com.dimxlp.kfrecalculator.model.KfreCalculation;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PatientKfreCalculatorFragment extends BaseKfreCalculatorFragment {

    private static final String TAG = "RAFI|PatientKfreCalc";
    private String patientId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            patientId = getArguments().getString("patientId");
            Log.d(TAG, "onCreate: Received patientId: " + patientId);
        } else {
            Log.w(TAG, "onCreate: No arguments received, patientId is null.");
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_base_kfre_calculator;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        Button btnSave = view.findViewById(R.id.btnSave);
        btnSave.setVisibility(View.VISIBLE);
        btnSave.setOnClickListener(v -> {
            Log.d(TAG, "Save button clicked.");
            showNoteBottomSheet();
        });

        return view;
    }

    private void showNoteBottomSheet() {
        if (resultContainer.getVisibility() != View.VISIBLE) {
            Log.w(TAG, "showNoteBottomSheet: Save clicked before calculation was performed.");
            Toast.makeText(getContext(), "Please calculate before saving.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "showNoteBottomSheet: Showing bottom sheet to add note.");
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_add_kfre_note, null);
        EditText inputNote = sheetView.findViewById(R.id.inputNote);
        Button btnConfirmSave = sheetView.findViewById(R.id.btnConfirmSave);
        Button btnCancel = sheetView.findViewById(R.id.btnCancel);

        btnConfirmSave.setOnClickListener(v -> {
            String note = inputNote.getText().toString().trim();
            Log.d(TAG, "Confirm save clicked. Note length: " + note.length());
            bottomSheetDialog.dismiss();
            saveCalculation(note);
        });

        btnCancel.setOnClickListener(v -> {
            Log.d(TAG, "Cancel button clicked in bottom sheet.");
            bottomSheetDialog.dismiss();
            Snackbar.make(requireView(), "Save canceled — note not added", Snackbar.LENGTH_SHORT).show();
        });

        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.setCancelable(true);
        bottomSheetDialog.setOnCancelListener(dialog -> {
            Log.d(TAG, "Bottom sheet was canceled (dismissed via back press or touch outside).");
            Snackbar.make(requireView(), "Save canceled — note not added", Snackbar.LENGTH_SHORT).show();
        });

        bottomSheetDialog.show();
    }

    private void saveCalculation(String notes) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Log.e(TAG, "saveCalculation: Cannot save, user is not logged in (UID is null).");
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int age = Integer.parseInt(inputAge.getText().toString().trim());
            double egfr = Double.parseDouble(inputEgfr.getText().toString().trim());
            double acr = Double.parseDouble(inputAlbuminuria.getText().toString().trim());
            String gender = getSelectedGender();

            String calcId = UUID.randomUUID().toString();
            long now = System.currentTimeMillis();

            KfreCalculation calculation = new KfreCalculation(
                    calcId,
                    patientId,
                    uid,
                    age,
                    gender,
                    egfr,
                    acr,
                    risk2Yr,
                    risk5Yr,
                    now,
                    now,
                    notes
            );

            Risk riskCategory;
            if (risk2Yr >= 40.0) {
                riskCategory = Risk.HIGH;
            } else if (risk2Yr >= 10.0) {
                riskCategory = Risk.MEDIUM;
            } else {
                riskCategory = Risk.LOW;
            }

            Log.i(TAG, "saveCalculation: Saving KFRE calculation with ID: " + calcId + " for patientId: " + patientId);
            Log.d(TAG, "saveCalculation: Values - Age: " + age + ", eGFR: " + egfr + ", ACR: " + acr + ", 2-Yr: " + risk2Yr + ", 5-Yr: " + risk5Yr);

            FirebaseFirestore.getInstance()
                    .collection("KfreCalculations")
                    .document(calcId)
                    .set(calculation)
                    .addOnSuccessListener(unused -> {
                        Log.d(TAG, "saveCalculation: Successfully saved calculation to Firestore.");
                        Toast.makeText(getContext(), "Calculation saved", Toast.LENGTH_SHORT).show();

                        updatePatientWithNewRisk(riskCategory, risk2Yr, risk5Yr);

                        Log.d(TAG, "saveCalculation: Setting fragment result to reload assessments.");
                        requireActivity()
                                .getSupportFragmentManager()
                                .setFragmentResult("reload_kfre_assessments", new Bundle());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "saveCalculation: Failed to save calculation to Firestore.", e);
                        Toast.makeText(getContext(), "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (NumberFormatException e) {
            Log.e(TAG, "saveCalculation: Invalid number format in input fields.", e);
            Toast.makeText(getContext(), "Invalid input fields", Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePatientWithNewRisk(Risk riskCategory, double risk2Yr, double risk5Yr) {
        if (patientId == null || patientId.isEmpty()) {
            Log.e(TAG, "updatePatientWithNewRisk: patientId is null, cannot update patient record.");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Log.d(TAG, "Attempting to update patient: " + patientId);

        // Prepare the data to update
        Map<String, Object> patientUpdates = new HashMap<>();
        patientUpdates.put("risk", riskCategory.name());
        patientUpdates.put("risk2Yr", risk2Yr);
        patientUpdates.put("risk5Yr", risk5Yr);
        patientUpdates.put("lastAssessment", new Date());
        patientUpdates.put("lastUpdated", System.currentTimeMillis());

        // Update the specific fields in the Patient document
        db.collection("Patients").document(patientId)
                .update(patientUpdates)
                .addOnSuccessListener(aVoid -> Log.i(TAG, "Successfully updated patient record for patientId: " + patientId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update patient record for patientId: " + patientId, e));
    }
}
