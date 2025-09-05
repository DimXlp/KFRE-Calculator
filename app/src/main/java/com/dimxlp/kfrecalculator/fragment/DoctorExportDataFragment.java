package com.dimxlp.kfrecalculator.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.handler.OnNextHandler;
import com.dimxlp.kfrecalculator.viewmodel.DoctorExportViewModel;

import java.util.EnumSet;

public class DoctorExportDataFragment extends Fragment implements OnNextHandler {
    private static final String TAG = "RAFI|DoctorExportData";

    private DoctorExportViewModel vm;
    private CheckBox cbCalc, cbNotes, cbMeds;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView()");
        View root = inflater.inflate(R.layout.fragment_doctor_export_data, container, false);

        vm = new ViewModelProvider(requireActivity()).get(DoctorExportViewModel.class);
        cbCalc = root.findViewById(R.id.cbCalculations);
        cbNotes = root.findViewById(R.id.cbNotes);
        cbMeds = root.findViewById(R.id.cbMedications);

        // restore previous selection
        EnumSet<DoctorExportViewModel.DataType> set = vm.getDataTypes();
        cbCalc.setChecked(set.contains(DoctorExportViewModel.DataType.CALCULATIONS));
        cbNotes.setChecked(set.contains(DoctorExportViewModel.DataType.NOTES));
        cbMeds.setChecked(set.contains(DoctorExportViewModel.DataType.MEDICATIONS));

        return root;
    }

    @Override
    public boolean onNext() {
        EnumSet<DoctorExportViewModel.DataType> set = EnumSet.noneOf(DoctorExportViewModel.DataType.class);
        if (cbCalc.isChecked()) set.add(DoctorExportViewModel.DataType.CALCULATIONS);
        if (cbNotes.isChecked()) set.add(DoctorExportViewModel.DataType.NOTES);
        if (cbMeds.isChecked()) set.add(DoctorExportViewModel.DataType.MEDICATIONS);

        if (set.isEmpty()) {
            Toast.makeText(requireContext(), R.string.export_err_data_required, Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Validation failed: no data types selected");
            return false;
        }
        vm.setDataTypes(set);
        Log.d(TAG, "onNext(): saved data types = " + set);
        return true;
    }
}
