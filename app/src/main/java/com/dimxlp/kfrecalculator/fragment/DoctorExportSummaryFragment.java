package com.dimxlp.kfrecalculator.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.handler.OnNextHandler;
import com.dimxlp.kfrecalculator.viewmodel.DoctorExportViewModel;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class DoctorExportSummaryFragment extends Fragment implements OnNextHandler {
    private static final String TAG = "RAFI|DoctorExportSum";

    private DoctorExportViewModel vm;
    private TextView tvScope, tvData, tvOptions, tvDest;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView()");
        View root = inflater.inflate(R.layout.fragment_doctor_export_summary, container, false);

        vm = new ViewModelProvider(requireActivity()).get(DoctorExportViewModel.class);
        tvScope   = root.findViewById(R.id.tvScope);
        tvData    = root.findViewById(R.id.tvData);
        tvOptions = root.findViewById(R.id.tvOptions);
        tvDest    = root.findViewById(R.id.tvDestination);

        bindSummary();
        return root;
    }

    private void bindSummary() {
        // Scope
        String scopeText;
        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
        switch (vm.getScopeType()) {
            case CURRENT_PATIENT:
                scopeText = getString(R.string.summary_scope_current);
                break;
            case ALL_PATIENTS:
                scopeText = getString(R.string.summary_scope_all);
                break;
            case SELECTED_PATIENTS:
                scopeText = getString(R.string.summary_scope_selected,
                        joinIds(vm.getSelectedPatientIds().toArray(new String[0])));
                break;
            case DATE_RANGE:
            default:
                scopeText = getString(R.string.summary_scope_range,
                        formatDate(vm.getStartDate(), df), formatDate(vm.getEndDate(), df));
        }
        tvScope.setText(scopeText);

        // Data
        ArrayList<String> dataNames = new ArrayList<>();
        for (DoctorExportViewModel.DataType t : vm.getDataTypes()) {
            switch (t) {
                case CALCULATIONS: dataNames.add(getString(R.string.export_data_calculations)); break;
                case NOTES:        dataNames.add(getString(R.string.export_data_notes)); break;
                case MEDICATIONS:  dataNames.add(getString(R.string.export_data_medications)); break;
            }
        }
        tvData.setText(getString(R.string.summary_data, TextUtils.join(", ", dataNames)));

        // Options
        String fmt  = vm.getFormat() == DoctorExportViewModel.Format.PDF ? getString(R.string.export_format_pdf) : getString(R.string.export_format_csv);
        String ch   = vm.isIncludeCharts() ? getString(R.string.summary_yes) : getString(R.string.summary_no);
        String anon = vm.isAnonymize() ? getString(R.string.summary_yes) : getString(R.string.summary_no);
        String lang = vm.getLanguage() == DoctorExportViewModel.Language.ENGLISH ? "English" : "Greek";
        tvOptions.setText(getString(R.string.summary_options, fmt, ch, anon, lang));

        // Destination
        String dest = vm.getDestination() == DoctorExportViewModel.Destination.SAVE
                ? getString(R.string.summary_dest_save)
                : getString(R.string.summary_dest_share);
        tvDest.setText(dest);
    }

    private static String formatDate(Date d, DateFormat df) {
        return d == null ? "-" : df.format(d);
    }

    private static String joinIds(String[] ids) {
        return (ids == null || ids.length == 0) ? "-" : TextUtils.join(", ", ids);
    }

    @Override
    public boolean onNext() {
        // Nothing to validate here – Export will be triggered by the Activity
        Log.d(TAG, "onNext(): summary confirmed");
        return true;
    }
}
