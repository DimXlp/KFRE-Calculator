package com.dimxlp.kfrecalculator.fragment;

import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.databinding.FragmentPreferencesBinding;
import com.dimxlp.kfrecalculator.utils.AppSettings;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Arrays;
import java.util.List;

public class PreferencesFragment extends Fragment {

    private static final String TAG = "RAFI|PreferencesFragment";
    private FragmentPreferencesBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPreferencesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadPreferences();
        setupListeners();
    }

    private void loadPreferences() {
        binding.darkModeSwitch.setChecked(AppSettings.isDarkModeEnabled(requireContext()));
        binding.dateFormatValue.setText(AppSettings.getDateFormat(requireContext()));
        binding.languageValue.setText(AppSettings.getLanguage(requireContext()));
        binding.assessmentFrequencyValue.setText(AppSettings.getAssessmentFrequency(requireContext()));
        binding.recentPatientsPeriodValue.setText(AppSettings.getRecentPatientsPeriod(requireContext()) + " days");
        binding.autoExportSwitch.setChecked(AppSettings.isAutoExportEnabled(requireContext()));
        Log.d(TAG, "All preferences loaded into UI using AppSettings.");
    }

    private void setupListeners() {
        binding.darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppSettings.setDarkModeEnabled(requireContext(), isChecked);
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        binding.autoExportSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppSettings.setAutoExportEnabled(requireContext(), isChecked);
        });

        binding.dateFormatLayout.setOnClickListener(v -> showSelectionBottomSheet("Date Format",
                Arrays.asList("dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd"),
                selected -> {
                    AppSettings.setDateFormat(requireContext(), selected);
                    binding.dateFormatValue.setText(selected);
                }));

        binding.languageLayout.setOnClickListener(v -> showSelectionBottomSheet("App Language",
                Arrays.asList("English", "Greek"),
                selected -> {
                    AppSettings.setLanguage(requireContext(), selected);
                    binding.languageValue.setText(selected);
                }));

        binding.assessmentFrequencyLayout.setOnClickListener(v -> showSelectionBottomSheet("Default Assessment Frequency",
                Arrays.asList("3 months", "6 months", "12 months"),
                selected -> {
                    AppSettings.setAssessmentFrequency(requireContext(), selected);
                    binding.assessmentFrequencyValue.setText(selected);
                }));

        binding.recentPatientsPeriodLayout.setOnClickListener(v -> showRecentPatientsPeriodSheet());
    }

    private void showSelectionBottomSheet(String title, List<String> options, OptionClickListener listener) {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_selection, null);
        bottomSheetDialog.setContentView(sheetView);

        TextView bottomSheetTitle = sheetView.findViewById(R.id.bottomSheetTitle);
        RecyclerView optionsRecyclerView = sheetView.findViewById(R.id.optionsRecyclerView);

        bottomSheetTitle.setText(title);

        SelectionAdapter adapter = new SelectionAdapter(options, option -> {
            listener.onOptionClicked(option);
            bottomSheetDialog.dismiss();
        });

        optionsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        optionsRecyclerView.setAdapter(adapter);

        bottomSheetDialog.show();
    }

    private void showRecentPatientsPeriodSheet() {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.botom_sheet_recent_patients_selection, null);
        bottomSheetDialog.setContentView(sheetView);

        TextInputEditText inputDays = sheetView.findViewById(R.id.inputDays);
        Button saveButton = sheetView.findViewById(R.id.saveButton);

        // Pre-fill the input with the current setting
        int currentPeriod = AppSettings.getRecentPatientsPeriod(requireContext());
        inputDays.setText(String.valueOf(currentPeriod));

        saveButton.setOnClickListener(v -> {
            try {
                int days = Integer.parseInt(inputDays.getText().toString());
                AppSettings.setRecentPatientsPeriod(requireContext(), days);
                binding.recentPatientsPeriodValue.setText(days + " days");
                Log.i(TAG, "Recent Patients Period set to: " + days);
                bottomSheetDialog.dismiss();
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid number entered for period.", e);
                inputDays.setError("Please enter a valid number");
            }
        });

        bottomSheetDialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private interface OptionClickListener {
        void onOptionClicked(String option);
    }

    private static class SelectionAdapter extends RecyclerView.Adapter<SelectionAdapter.ViewHolder> {
        private final List<String> options;
        private final OptionClickListener listener;

        SelectionAdapter(List<String> options, OptionClickListener listener) {
            this.options = options;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bottom_sheet_option, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String option = options.get(position);
            holder.optionTextView.setText(option);
            holder.itemView.setOnClickListener(v -> listener.onOptionClicked(option));
        }

        @Override
        public int getItemCount() {
            return options.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView optionTextView;
            ViewHolder(View view) {
                super(view);
                optionTextView = (TextView) view;
            }
        }
    }
}