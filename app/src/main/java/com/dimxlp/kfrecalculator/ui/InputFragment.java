package com.dimxlp.kfrecalculator.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.dimxlp.kfrecalculator.R;
import com.google.android.material.slider.Slider;

public class InputFragment extends Fragment {

    private static final String TAG = "RAFI|InputFragment";

    public InputFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_input, container, false);

        // Initialize UI components
        EditText ageInput = view.findViewById(R.id.input_age);
        Spinner genderSpinner = view.findViewById(R.id.input_gender);
        Slider egfrSlider = view.findViewById(R.id.input_egfr);
        Slider acrSlider = view.findViewById(R.id.input_acr);
        Button calculateButton = view.findViewById(R.id.button_calculate);

        // Add a listener for eGFR slider
        egfrSlider.addOnChangeListener((slider, value, fromUser) -> {
            Log.d(TAG, "eGFR value changed: " + value);
        });

        // Add a listener for ACR slider
        acrSlider.addOnChangeListener((slider, value, fromUser) -> {
            Log.d(TAG, "ACR value changed: " + value);
        });

        // Set default values or listeners if needed
        calculateButton.setOnClickListener(v -> {
            String age = ageInput.getText().toString();
            String gender = genderSpinner.getSelectedItem().toString();
            var egfr = egfrSlider.getValue();
            var acr = acrSlider.getValue();

            // TODO: Navigate to ResultsFragment and pass data
        });

        return view;
    }
}
