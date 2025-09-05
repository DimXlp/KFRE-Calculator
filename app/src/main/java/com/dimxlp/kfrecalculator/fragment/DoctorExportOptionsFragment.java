package com.dimxlp.kfrecalculator.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.handler.OnNextHandler;
import com.dimxlp.kfrecalculator.viewmodel.DoctorExportViewModel;

public class DoctorExportOptionsFragment extends Fragment implements OnNextHandler {
    private static final String TAG = "RAFI|DoctorExportOpts";

    private DoctorExportViewModel vm;
    private RadioGroup formatGroup;
    private CheckBox cbCharts, cbAnonymize;
    private Spinner languageSpinner;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView()");
        View root = inflater.inflate(R.layout.fragment_doctor_export_options, container, false);

        vm = new ViewModelProvider(requireActivity()).get(DoctorExportViewModel.class);

        formatGroup = root.findViewById(R.id.formatGroup);
        cbCharts = root.findViewById(R.id.cbIncludeCharts);
        cbAnonymize = root.findViewById(R.id.cbAnonymize);
        languageSpinner = root.findViewById(R.id.languageSpinner);

        // Format
        formatGroup.check(vm.getFormat() == DoctorExportViewModel.Format.PDF ? R.id.rbPdf : R.id.rbCsv);

        // Checkboxes
        cbCharts.setChecked(vm.isIncludeCharts());
        cbAnonymize.setChecked(vm.isAnonymize());

        // Language spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"English", "Greek"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);
        languageSpinner.setSelection(vm.getLanguage() == DoctorExportViewModel.Language.ENGLISH ? 0 : 1);

        // Listeners update VM immediately
        formatGroup.setOnCheckedChangeListener((g, id) ->
                vm.setFormat(id == R.id.rbPdf ? DoctorExportViewModel.Format.PDF : DoctorExportViewModel.Format.CSV));

        cbCharts.setOnCheckedChangeListener((buttonView, isChecked) -> vm.setIncludeCharts(isChecked));
        cbAnonymize.setOnCheckedChangeListener((buttonView, isChecked) -> vm.setAnonymize(isChecked));

        return root;
    }

    @Override
    public boolean onNext() {
        int pos = languageSpinner.getSelectedItemPosition();
        if (pos < 0) {
            Toast.makeText(requireContext(), R.string.export_err_language_required, Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Validation failed: language not selected");
            return false;
        }
        vm.setLanguage(pos == 0 ? DoctorExportViewModel.Language.ENGLISH : DoctorExportViewModel.Language.GREEK);
        Log.d(TAG, "onNext(): saved options -> format=" + vm.getFormat()
                + ", charts=" + vm.isIncludeCharts()
                + ", anonymize=" + vm.isAnonymize()
                + ", language=" + vm.getLanguage());
        return true;
    }
}
