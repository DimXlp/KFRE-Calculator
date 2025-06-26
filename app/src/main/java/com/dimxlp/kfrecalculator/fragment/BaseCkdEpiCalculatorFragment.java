package com.dimxlp.kfrecalculator.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.dimxlp.kfrecalculator.R;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

public abstract class BaseCkdEpiCalculatorFragment extends Fragment {

    protected TextInputEditText inputAge, inputCreatinine;
    protected RadioGroup radioGender;
    protected LinearLayout resultContainer;
    protected TextView txtEgfrResult;
    protected double egfrResult;

    protected abstract int getLayoutId();
    protected abstract String getLogTag();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(getLayoutId(), container, false);

        // Initialize views
        inputAge = view.findViewById(R.id.inputAge);
        inputCreatinine = view.findViewById(R.id.inputCreatinine);
        radioGender = view.findViewById(R.id.radioGender);
        resultContainer = view.findViewById(R.id.resultContainer);
        txtEgfrResult = view.findViewById(R.id.txtEgfrResult);

        // Set listeners
        Button btnCalculate = view.findViewById(R.id.btnCalculate);
        Button btnClear = view.findViewById(R.id.btnClear);
        btnCalculate.setOnClickListener(v -> performCalculation());
        btnClear.setOnClickListener(v -> clearFields());

        return view;
    }

    protected void performCalculation() {
        String ageStr = inputAge.getText().toString().trim();
        String creatinineStr = inputCreatinine.getText().toString().trim();

        if (TextUtils.isEmpty(ageStr) || TextUtils.isEmpty(creatinineStr) || radioGender.getCheckedRadioButtonId() == -1) {
            Toast.makeText(getContext(), "Please complete all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int age = Integer.parseInt(ageStr);
            double scr = Double.parseDouble(creatinineStr);
            String gender = getSelectedGender();

            egfrResult = calculateCkdEpi2021(scr, age, gender);
            displayResults(egfrResult);

        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid number format", Toast.LENGTH_SHORT).show();
        }
    }

    protected void displayResults(double egfr) {
        String formatted = String.format(Locale.ROOT, "eGFR: %.1f mL/min/1.73m²", egfr);
        txtEgfrResult.setText(formatted);
        resultContainer.setVisibility(View.VISIBLE);
    }

    protected void clearFields() {
        inputAge.setText("");
        inputCreatinine.setText("");
        radioGender.clearCheck();
        resultContainer.setVisibility(View.GONE);
        egfrResult = 0;
    }

    protected String getSelectedGender() {
        int selectedId = radioGender.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton selectedButton = radioGender.findViewById(selectedId);
            return selectedButton.getText().toString().toLowerCase(Locale.ROOT);
        }
        return "";
    }

    /**
     * Calculates the eGFR using the 2021 CKD-EPI Creatinine equation.
     * @param scr Serum creatinine in mg/dL
     * @param age Age in years
     * @param gender "male" or "female"
     * @return eGFR in mL/min/1.73m²
     */
    private double calculateCkdEpi2021(double scr, int age, String gender) {
        double kappa = "female".equals(gender) ? 0.7 : 0.9;
        double alpha = "female".equals(gender) ? -0.241 : -0.302;
        double sexFactor = "female".equals(gender) ? 1.012 : 1.0;

        double min = Math.min(scr / kappa, 1.0);
        double max = Math.max(scr / kappa, 1.0);

        return 142 * Math.pow(min, alpha) * Math.pow(max, -1.200) * Math.pow(0.9938, age) * sexFactor;
    }
}
