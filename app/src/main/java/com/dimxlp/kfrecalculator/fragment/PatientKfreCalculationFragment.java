package com.dimxlp.kfrecalculator.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.model.KfreCalculation;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;
import java.util.UUID;

public class PatientKfreCalculationFragment extends Fragment {

    private static final String TAG = "RAFI|PatientKfreCalc";

    private TextInputEditText inputAge, inputEgfr, inputAlbuminuria;
    private RadioGroup radioGender;
    private LinearLayout resultContainer;
    private TextView txtRisk2Yr, txtRisk5Yr, txtRiskLevel, txtRiskMessage;
    private Button btnCalculate, btnClear, btnSave;

    private double risk2Yr, risk5Yr;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_base_kfre_calculator, container, false);

        inputAge = view.findViewById(R.id.inputAge);
        inputEgfr = view.findViewById(R.id.inputEgfr);
        inputAlbuminuria = view.findViewById(R.id.inputAlbuminuria);
        radioGender = view.findViewById(R.id.radioGender);

        txtRisk2Yr = view.findViewById(R.id.txtRisk2Yr);
        txtRisk5Yr = view.findViewById(R.id.txtRisk5Yr);
        txtRiskLevel = view.findViewById(R.id.txtRiskLevel);
        txtRiskMessage = view.findViewById(R.id.txtRiskMessage);
        resultContainer = view.findViewById(R.id.resultContainer);

        btnCalculate = view.findViewById(R.id.btnCalculate);
        btnClear = view.findViewById(R.id.btnClear);
        btnSave = view.findViewById(R.id.btnSave); // You must add this to the layout

        btnCalculate.setOnClickListener(v -> performCalculation());
        btnClear.setOnClickListener(v -> clearFields());
//        btnSave.setOnClickListener(v -> saveCalculation());

        return view;
    }

    private void performCalculation() {
        String ageStr = inputAge.getText().toString().trim();
        String egfrStr = inputEgfr.getText().toString().trim();
        String acrStr = inputAlbuminuria.getText().toString().trim();

        if (ageStr.isEmpty() || egfrStr.isEmpty() || acrStr.isEmpty() || radioGender.getCheckedRadioButtonId() == -1) {
            Toast.makeText(getContext(), "Please complete all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int age = Integer.parseInt(ageStr);
            double egfr = Double.parseDouble(egfrStr);
            double acr = Double.parseDouble(acrStr);
            String gender = getSelectedGender();

            Log.d(TAG, "Age: " + age + ", Gender: " + gender + ", eGFR: " + egfr + ", ACR: " + acr);

            // Dummy calculation logic (replace with actual model/formula)
            risk2Yr = Math.min(100.0, egfr * 0.5 + acr * 0.3 - age * 0.2);
            risk5Yr = Math.min(100.0, risk2Yr + 10);

            txtRisk2Yr.setText(String.format(Locale.getDefault(), "%.2f%%", risk2Yr));
            txtRisk5Yr.setText(String.format(Locale.getDefault(), "%.2f%%", risk5Yr));
            txtRiskLevel.setText(risk2Yr > 20 ? "High Risk" : "Low Risk");
            txtRiskMessage.setText(risk2Yr > 20 ? "Consider referral to nephrologist." : "Continue monitoring.");
            resultContainer.setVisibility(View.VISIBLE);

        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid number format", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearFields() {
        inputAge.setText("");
        inputEgfr.setText("");
        inputAlbuminuria.setText("");
        radioGender.clearCheck();
        resultContainer.setVisibility(View.GONE);
    }

    private String getSelectedGender() {
        int selectedId = radioGender.getCheckedRadioButtonId();
        if (selectedId == R.id.radioMale) return "male";
        if (selectedId == R.id.radioFemale) return "female";
        return "";
    }

//    private void saveCalculation() {
//        if (resultContainer.getVisibility() != View.VISIBLE) {
//            Toast.makeText(getContext(), "Please perform a calculation first", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        String uid = FirebaseAuth.getInstance().getUid();
//        if (uid == null) {
//            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        String id = UUID.randomUUID().toString();
//        int age = Integer.parseInt(inputAge.getText().toString().trim());
//        double egfr = Double.parseDouble(inputEgfr.getText().toString().trim());
//        double acr = Double.parseDouble(inputAlbuminuria.getText().toString().trim());
//        String gender = getSelectedGender();
//
//        KfreCalculation calculation = new KfreCalculation(
//                id, uid, age, gender, egfr, acr, risk2Yr, risk5Yr, Timestamp.now()
//        );
//
//        FirebaseFirestore.getInstance()
//                .collection("KfreCalculations")
//                .document(id)
//                .set(calculation)
//                .addOnSuccessListener(unused -> Toast.makeText(getContext(), "Calculation saved", Toast.LENGTH_SHORT).show())
//                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to save", Toast.LENGTH_SHORT).show());
//    }
}
