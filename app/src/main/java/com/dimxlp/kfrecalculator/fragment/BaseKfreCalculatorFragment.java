package com.dimxlp.kfrecalculator.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.dimxlp.kfrecalculator.R;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

public abstract class BaseKfreCalculatorFragment extends Fragment {

    protected TextInputEditText inputAge, inputEgfr, inputAlbuminuria;
    protected RadioGroup radioGender;
    protected LinearLayout resultContainer;
    protected TextView txtRisk2Yr, txtRisk5Yr, txtRiskLevel, txtRiskMessage;

    protected double risk2Yr, risk5Yr;

    protected abstract int getLayoutId();
    protected abstract String getLogTag();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(getLayoutId(), container, false);

        inputAge = view.findViewById(R.id.inputAge);
        inputEgfr = view.findViewById(R.id.inputEgfr);
        inputAlbuminuria = view.findViewById(R.id.inputAlbuminuria);
        radioGender = view.findViewById(R.id.radioGender);

        txtRisk2Yr = view.findViewById(R.id.txtRisk2Yr);
        txtRisk5Yr = view.findViewById(R.id.txtRisk5Yr);
        txtRiskLevel = view.findViewById(R.id.txtRiskLevel);
        txtRiskMessage = view.findViewById(R.id.txtRiskMessage);
        resultContainer = view.findViewById(R.id.resultContainer);

        view.findViewById(R.id.btnCalculate).setOnClickListener(v -> performCalculation());
        view.findViewById(R.id.btnClear).setOnClickListener(v -> clearFields());

        return view;
    }

    protected void performCalculation() {
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
            String sex = getSelectedGender();

            Log.d(getLogTag(), "Age: " + age);
            Log.d(getLogTag(), "Gender: " + sex);
            Log.d(getLogTag(), "eGFR: " + egfr);
            Log.d(getLogTag(), "ACR: " + acr);

            risk2Yr = calculateKFRE(age, sex, egfr, acr, 2);
            risk5Yr = calculateKFRE(age, sex, egfr, acr, 5);

            displayResults(risk2Yr, risk5Yr);

            Log.d(getLogTag(), "Calculation complete: 2yr=" + risk2Yr + ", 5yr=" + risk5Yr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid numeric input", Toast.LENGTH_SHORT).show();
        }
    }

    protected void displayResults(double risk2Yr, double risk5Yr) {
        txtRisk2Yr.setText(String.format(Locale.getDefault(), "2-Year Risk: %.2f%%", risk2Yr));
        txtRisk5Yr.setText(String.format(Locale.getDefault(), "5-Year Risk: %.2f%%", risk5Yr));

        String level, message;
        int levelColor;

        if (risk2Yr >= 40.0) {
            level = "High";
            levelColor = ContextCompat.getColor(requireContext(), R.color.colorHighRiskStat);
            message = "Your risk of kidney failure is high. Please consult a specialist.";
        } else if (risk2Yr >= 10.0) {
            level = "Medium";
            levelColor = ContextCompat.getColor(requireContext(), R.color.colorMediumRiskStat);
            message = "Your risk is moderate. Monitoring is recommended.";
        } else {
            level = "Low";
            levelColor = ContextCompat.getColor(requireContext(), R.color.colorLowRiskStat);
            message = "Your risk is low.";
        }

        txtRiskLevel.setText("Risk Level: " + level);
        txtRiskLevel.setTextColor(levelColor);
        txtRisk2Yr.setTextColor(levelColor);
        txtRisk5Yr.setTextColor(levelColor);
        txtRiskMessage.setText(message);
        resultContainer.setVisibility(View.VISIBLE);
    }

    protected void clearFields() {
        inputAge.setText("");
        inputEgfr.setText("");
        inputAlbuminuria.setText("");
        radioGender.clearCheck();
        resultContainer.setVisibility(View.GONE);
    }

    protected String getSelectedGender() {
        int selectedId = radioGender.getCheckedRadioButtonId();
        if (selectedId == R.id.radioMale) return "male";
        else if (selectedId == R.id.radioFemale) return "female";
        return "";
    }

    /**
     * Calculates KFRE risk (2-year or 5-year) based on the Tangri 4-variable model.
     *
     * @param age   Age in years
     * @param sex   "Male", "Female"
     * @param egfr  eGFR (mL/min/1.73m²)
     * @param acr   Albumin-to-Creatinine Ratio (ACR) in mg/g
     * @param years 2 or 5 (for 2-year or 5-year risk)
     * @return Risk percentage (0–100)
     */
    protected double calculateKFRE(int age, String sex, double egfr, double acr, int years) {
        acr = Math.max(acr, 1e-6); // Prevent log(0)
        double logAcr = Math.log(acr);
        int isMale = "male".equalsIgnoreCase(sex) ? 1 : 0;

        // Mean values from original Canadian cohort (Tangri et al., 2011)
        double meanAge = 70.36;
        double meanMale = 0.5642;
        double meanEgfr = 36.11; // 5 × 7.222
        double meanLogAcr = 5.137;

        // Coefficients
        double coefAgePer10Yr = -0.2201;
        double coefSexMale = 0.2467;
        double coefEgfrPer5 = -0.5567;
        double coefLogAcr = 0.4510;

        // Baseline survival
        double baselineSurvival;
        if (years == 2) {
            baselineSurvival = 0.9751;
        } else if (years == 5) {
            baselineSurvival = 0.9240;
        } else {
            throw new IllegalArgumentException("Only 2 or 5-year risk supported");
        }

        // Calculate centered and scaled values
        double xAge = (age / 10.0) - (meanAge / 10.0);
        double xSex = isMale - meanMale;
        double xEgfr = (egfr / 5.0) - (meanEgfr / 5.0);
        double xLogAcr = logAcr - meanLogAcr;

        // Linear predictor
        double lp = (coefAgePer10Yr * xAge) +
                (coefSexMale * xSex) +
                (coefEgfrPer5 * xEgfr) +
                (coefLogAcr * xLogAcr);

        // Risk calculation
        double risk = 1 - Math.pow(baselineSurvival, Math.exp(lp));
        return Math.max(0, Math.min(risk * 100.0, 100.0)); // Convert to percentage
    }
}
