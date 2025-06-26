package com.dimxlp.kfrecalculator.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.model.CkdEpiCalculation;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.UUID;

public class PatientCkdEpiCalculatorFragment extends BaseCkdEpiCalculatorFragment {

    private static final String TAG = "RAFI|PatientCkdEpiCalc";
    private String patientId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            patientId = getArguments().getString("patientId");
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_base_ckd_epi_calculator;
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
        btnSave.setOnClickListener(v -> showNoteBottomSheet());

        return view;
    }

    private void showNoteBottomSheet() {
        if (resultContainer.getVisibility() != View.VISIBLE) {
            Toast.makeText(getContext(), "Please calculate before saving.", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_add_kfre_note, null);
        EditText inputNote = sheetView.findViewById(R.id.inputNote);
        Button btnConfirmSave = sheetView.findViewById(R.id.btnConfirmSave);
        Button btnCancel = sheetView.findViewById(R.id.btnCancel);

        btnConfirmSave.setOnClickListener(v -> {
            String note = inputNote.getText().toString().trim();
            bottomSheetDialog.dismiss();
            saveCalculation(note);
        });

        btnCancel.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Snackbar.make(requireView(), "Save canceled — note not added", Snackbar.LENGTH_SHORT).show();
        });

        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.setCancelable(true);
        bottomSheetDialog.setOnCancelListener(dialog ->
                Snackbar.make(requireView(), "Save canceled — note not added", Snackbar.LENGTH_SHORT).show()
        );

        bottomSheetDialog.show();
    }

    private void saveCalculation(String notes) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int age = Integer.parseInt(inputAge.getText().toString().trim());
            double creatinine = Double.parseDouble(inputCreatinine.getText().toString().trim());
            String gender = getSelectedGender();

            String calcId = UUID.randomUUID().toString();
            long now = System.currentTimeMillis();

            CkdEpiCalculation calculation = new CkdEpiCalculation(
                    calcId,
                    patientId,
                    uid,
                    creatinine,
                    gender,
                    age,
                    egfrResult,
                    now,
                    now,
                    notes
            );

            FirebaseFirestore.getInstance()
                    .collection("CkdEpiCalculations")
                    .document(calcId)
                    .set(calculation)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(getContext(), "Calculation saved", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show());

            // You might want to reload a list of assessments here, similar to the KFRE calculator
            // requireActivity().getSupportFragmentManager().setFragmentResult("reload_assessments", new Bundle());

        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid input fields", Toast.LENGTH_SHORT).show();
        }
    }
}
