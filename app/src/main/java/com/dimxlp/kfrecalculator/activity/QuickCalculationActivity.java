package com.dimxlp.kfrecalculator.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dimxlp.kfrecalculator.R;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

public class QuickCalculationActivity extends AppCompatActivity {

    private static final String TAG = "RAFI|QuickCalc";

    private TextInputEditText inputAge, inputEgfr, inputAlbuminuria;
    private RadioGroup radioGender;
    private Button btnCalculate, btnClear;
    private LinearLayout resultContainer;
    private TextView txtRisk2Yr, txtRisk5Yr, txtRiskLevel, txtRiskMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_calculation);

        initViews();
        setupListeners();
    }

    private void initViews() {
        inputAge = findViewById(R.id.inputAge);
        inputEgfr = findViewById(R.id.inputEgfr);
        inputAlbuminuria = findViewById(R.id.inputAlbuminuria);
        radioGender = findViewById(R.id.radioGender);

        btnCalculate = findViewById(R.id.btnCalculate);
        btnClear = findViewById(R.id.btnClear);

        resultContainer = findViewById(R.id.resultContainer);
        txtRisk2Yr = findViewById(R.id.txtRisk2Yr);
        txtRisk5Yr = findViewById(R.id.txtRisk5Yr);
        txtRiskLevel = findViewById(R.id.txtRiskLevel);
        txtRiskMessage = findViewById(R.id.txtRiskMessage);
    }

    private void setupListeners() {
        btnCalculate.setOnClickListener(v -> performCalculation());
        btnClear.setOnClickListener(v -> clearFields());
    }

    private void performCalculation() {
        String ageStr = inputAge.getText().toString().trim();
        String egfrStr = inputEgfr.getText().toString().trim();
        String acrStr = inputAlbuminuria.getText().toString().trim();

        if (ageStr.isEmpty() || egfrStr.isEmpty() || acrStr.isEmpty() || radioGender.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please complete all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int age = Integer.parseInt(ageStr);
            double egfr = Double.parseDouble(egfrStr);
            double acr = Double.parseDouble(acrStr);
            String gender = getSelectedGender();

            Log.d(TAG, "Age: " + age);
            Log.d(TAG, "gender: " + gender);
            Log.d(TAG, "egfr: " + egfr);
            Log.d(TAG, "acr: " + acr);
            double risk2Yr = calculateKFRE(age, gender, egfr, acr, 2);
            double risk5Yr = calculateKFRE(age, gender, egfr, acr, 5);

            txtRisk2Yr.setText(String.format(Locale.getDefault(), "2-Year Risk: %.2f%%", risk2Yr));
            txtRisk5Yr.setText(String.format(Locale.getDefault(), "5-Year Risk: %.2f%%", risk5Yr));

            String level, message;
            int levelColor;

            if (risk2Yr >= 20.0) {
                level = "High";
                levelColor = getColor(R.color.colorHighRiskStat);
                message = "Your risk of kidney failure is high. Please consult a specialist.";
            } else if (risk2Yr >= 10.0) {
                level = "Medium";
                levelColor = getColor(R.color.colorMediumRiskStat);
                message = "Your risk is moderate. Monitoring is recommended.";
            } else {
                level = "Low";
                levelColor = getColor(R.color.colorLowRiskStat);
                message = "Your risk is low.";
            }

            txtRiskLevel.setText("Risk Level: " + level);
            txtRiskLevel.setTextColor(levelColor);
            txtRisk2Yr.setTextColor(levelColor);
            txtRisk5Yr.setTextColor(levelColor);
            txtRiskMessage.setText(message);
            resultContainer.setVisibility(View.VISIBLE);

            Log.d(TAG, "Calculation complete: 2yr=" + risk2Yr + ", 5yr=" + risk5Yr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid numeric input", Toast.LENGTH_SHORT).show();
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
        if (selectedId != -1) {
            RadioButton selected = findViewById(selectedId);
            return selected.getText().toString();
        }
        return null;
    }

    /**
     * Calculates KFRE risk (2-year or 5-year) based on the Tangri 4-variable model.
     *
     * @param age   Age in years
     * @param sex   "Male", "Female", or "Other"
     * @param egfr  eGFR (mL/min/1.73m²)
     * @param acr   Albumin-to-Creatinine Ratio (ACR) in mg/g
     * @param years 2 or 5 (for 2-year or 5-year risk)
     * @return Risk percentage (0–100)
     */
    private double calculateKFRE(int age, String sex, double egfr, double acr, int years) {
        // Safety for ACR
        acr = Math.max(acr, 0);
        double logAcr = Math.log(acr + 1);
        int isMale = "male".equalsIgnoreCase(sex) ? 1 : 0;

        Log.d(TAG, "Calc acr: " + acr);
        Log.d(TAG, "logAcr: " + logAcr);
        Log.d(TAG, "isMale: " + isMale);

        double intercept, coefAge, coefSexMale, coefEgfr, coefLogAcr, base;

        if (years == 2) {
            intercept = -0.5567;
            coefAge = 0.2202;
            coefSexMale = 0.2467;
            coefEgfr = -0.5567;
            coefLogAcr = 0.8465;
            base = 0.914;
        } else if (years == 5) {
            intercept = -0.2202;
            coefAge = 0.2467;
            coefSexMale = 0.4510;
            coefEgfr = -0.5567;
            coefLogAcr = 0.4510;
            base = 0.965;
        } else {
            throw new IllegalArgumentException("Only 2 or 5-year risk supported");
        }

        double lp = intercept +
                (coefAge * age) +
                (coefSexMale * isMale) +
                (coefEgfr * egfr) +
                (coefLogAcr * logAcr);

        double risk = 1 - Math.pow(base, Math.exp(lp));
        return Math.max(0, Math.min(risk * 100, 100.0));
    }
}
