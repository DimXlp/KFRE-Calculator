package com.dimxlp.kfrecalculator.fragment;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
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

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class DoctorExportScopeFragment extends Fragment implements OnNextHandler {
    private static final String TAG = "RAFI|DoctorExportScope";

    private DoctorExportViewModel vm;

    private RadioGroup scopeGroup;
    private View patientsContainer, rangeContainer;
    private EditText patientsInput;     // comma-separated IDs (temporary)
    private Button btnStart, btnEnd;    // date pickers

    private Date startDate, endDate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView()");
        View root = inflater.inflate(R.layout.fragment_doctor_export_scope, container, false);

        vm = new ViewModelProvider(requireActivity()).get(DoctorExportViewModel.class);

        scopeGroup        = root.findViewById(R.id.scopeGroup);
        patientsContainer = root.findViewById(R.id.patientsContainer);
        rangeContainer    = root.findViewById(R.id.rangeContainer);
        patientsInput     = root.findViewById(R.id.inputPatients);
        btnStart          = root.findViewById(R.id.btnPickStart);
        btnEnd            = root.findViewById(R.id.btnPickEnd);

        // Restore pre-existing state
        switch (vm.getScopeType()) {
            case ALL_PATIENTS:     scopeGroup.check(R.id.rbAll); break;
            case SELECTED_PATIENTS:scopeGroup.check(R.id.rbSelected); break;
            case DATE_RANGE:       scopeGroup.check(R.id.rbDateRange); break;
            case CURRENT_PATIENT:  scopeGroup.check(R.id.rbCurrent); break;
        }
        startDate = vm.getStartDate();
        endDate   = vm.getEndDate();
        if (!vm.getSelectedPatientIds().isEmpty()) {
            patientsInput.setText(TextUtils.join(", ", vm.getSelectedPatientIds()));
        }

        toggleContainers();

        scopeGroup.setOnCheckedChangeListener((g, id) -> {
            if (id == R.id.rbAll)      vm.setScopeType(DoctorExportViewModel.ScopeType.ALL_PATIENTS);
            if (id == R.id.rbSelected) vm.setScopeType(DoctorExportViewModel.ScopeType.SELECTED_PATIENTS);
            if (id == R.id.rbDateRange)vm.setScopeType(DoctorExportViewModel.ScopeType.DATE_RANGE);
            if (id == R.id.rbCurrent)  vm.setScopeType(DoctorExportViewModel.ScopeType.CURRENT_PATIENT);
            toggleContainers();
        });

        btnStart.setOnClickListener(v -> pickDate(true));
        btnEnd.setOnClickListener(v -> pickDate(false));

        return root;
    }

    private void toggleContainers() {
        DoctorExportViewModel.ScopeType t = vm.getScopeType();
        patientsContainer.setVisibility(t == DoctorExportViewModel.ScopeType.SELECTED_PATIENTS ? View.VISIBLE : View.GONE);
        rangeContainer.setVisibility(t == DoctorExportViewModel.ScopeType.DATE_RANGE ? View.VISIBLE : View.GONE);
        Log.d(TAG, "toggleContainers(): " + t);
    }

    private void pickDate(boolean isStart) {
        final Calendar c = Calendar.getInstance();
        Date d = isStart ? startDate : endDate;
        if (d != null) c.setTime(d);
        int y = c.get(Calendar.YEAR), m = c.get(Calendar.MONTH), day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dlg = new DatePickerDialog(requireContext(),
                (DatePicker view, int year, int month, int dayOfMonth) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(year, month, dayOfMonth, 0, 0, 0);
                    if (isStart) { startDate = chosen.getTime(); Log.d(TAG, "Picked start: " + startDate); }
                    else         { endDate   = chosen.getTime(); Log.d(TAG, "Picked end: " + endDate); }
                }, y, m, day);
        dlg.show();
    }

    @Override
    public boolean onNext() {
        DoctorExportViewModel.ScopeType t = vm.getScopeType();
        Log.d(TAG, "onNext() scope=" + t);

        if (t == DoctorExportViewModel.ScopeType.SELECTED_PATIENTS) {
            String raw = patientsInput.getText() == null ? "" : patientsInput.getText().toString().trim();
            if (raw.isEmpty()) {
                Toast.makeText(requireContext(), R.string.export_err_patients_required, Toast.LENGTH_SHORT).show();
                return false;
            }
            String[] parts = raw.split(",");
            Set<String> ids = new HashSet<>();
            for (String p : parts) {
                String id = p.trim();
                if (!id.isEmpty()) ids.add(id);
            }
            if (ids.isEmpty()) {
                Toast.makeText(requireContext(), R.string.export_err_patients_required, Toast.LENGTH_SHORT).show();
                return false;
            }
            vm.setSelectedPatientIds(ids);
        } else if (t == DoctorExportViewModel.ScopeType.DATE_RANGE) {
            if (startDate == null || endDate == null) {
                Toast.makeText(requireContext(), R.string.export_err_dates_required, Toast.LENGTH_SHORT).show();
                return false;
            }
            if (startDate.after(endDate)) {
                Toast.makeText(requireContext(), R.string.export_err_dates_order, Toast.LENGTH_SHORT).show();
                return false;
            }
            vm.setDateRange(startDate, endDate);
        }
        // CURRENT_PATIENT / ALL_PATIENTS need no extra validation here.
        return true;
    }
}
