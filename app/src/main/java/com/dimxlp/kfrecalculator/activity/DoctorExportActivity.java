package com.dimxlp.kfrecalculator.activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.fragment.DoctorExportDataFragment;
import com.dimxlp.kfrecalculator.fragment.DoctorExportDestinationFragment;
import com.dimxlp.kfrecalculator.fragment.DoctorExportOptionsFragment;
import com.dimxlp.kfrecalculator.fragment.DoctorExportScopeFragment;
import com.dimxlp.kfrecalculator.fragment.DoctorExportSummaryFragment;
import com.dimxlp.kfrecalculator.viewmodel.DoctorExportViewModel;
import com.dimxlp.kfrecalculator.handler.OnNextHandler;

public class DoctorExportActivity extends AppCompatActivity {

    private static final String TAG = "RAFI|DoctorExport";

    private enum Step { SCOPE, DATA, OPTIONS, DESTINATION, SUMMARY }
    private Step currentStep = Step.SCOPE;

    private DoctorExportViewModel vm;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_export);

        vm = new ViewModelProvider(this).get(DoctorExportViewModel.class);

        // ... your existing logo/profile/back/next setup ...

        findViewById(R.id.exportDoctorBackBtn).setOnClickListener(v -> {
            Log.d(TAG, "Back clicked, step=" + currentStep);
            switch (currentStep) {
                case SCOPE:
                    finish();
                    break;
                case DATA:
                    currentStep = Step.SCOPE;
                    swapFragment(new DoctorExportScopeFragment(), false);
                    break;
                case OPTIONS:
                    currentStep = Step.DATA;
                    swapFragment(new DoctorExportDataFragment(), false);
                    break;
                case DESTINATION:
                    currentStep = Step.OPTIONS;
                    swapFragment(new DoctorExportOptionsFragment(), false);
                    break;
                case SUMMARY:
                    currentStep = Step.DESTINATION;
                    swapFragment(new DoctorExportDestinationFragment(), false);
                    break;
            }
            updateButtons();
        });

        findViewById(R.id.exportDoctorNextBtn).setOnClickListener(v -> {
            Log.d(TAG, "Primary clicked, step=" + currentStep);
            Fragment f = getSupportFragmentManager().findFragmentById(R.id.exportDoctorContentContainer);
            boolean ok = !(f instanceof OnNextHandler)
                    || ((OnNextHandler) f).onNext();
            if (!ok) return;

            switch (currentStep) {
                case SCOPE:
                    currentStep = Step.DATA;
                    swapFragment(new DoctorExportDataFragment(), true);
                    break;
                case DATA:
                    currentStep = Step.OPTIONS;
                    swapFragment(new DoctorExportOptionsFragment(), true);
                    break;
                case OPTIONS:
                    currentStep = Step.DESTINATION;
                    swapFragment(new DoctorExportDestinationFragment(), true);
                    break;
                case DESTINATION:
                    currentStep = Step.SUMMARY;
                    swapFragment(new DoctorExportSummaryFragment(), true);
                    break;
                case SUMMARY:
                    runExport();
                    break;
            }
            updateButtons();
        });

        // First screen
        swapFragment(new DoctorExportScopeFragment(), false);
        updateButtons();
    }

    private void swapFragment(Fragment fragment, boolean forward) {
        Log.d(TAG, "swapFragment: " + fragment.getClass().getSimpleName() + " forward=" + forward);
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right,
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right
                )
                .replace(R.id.exportDoctorContentContainer, fragment)
                .commit();
    }

    private void updateButtons() {
        Button nextBtn = findViewById(R.id.exportDoctorNextBtn);
        if (currentStep == Step.SUMMARY) {
            nextBtn.setText(R.string.export_action_export);
        } else {
            nextBtn.setText(R.string.export_action_next);
        }
    }

    private void runExport() {
        Log.i(TAG, "runExport() START");
        // Pull everything from VM
        DoctorExportViewModel vm = new ViewModelProvider(this).get(DoctorExportViewModel.class);

        Log.d(TAG, "Export params:"
                + " scope=" + vm.getScopeType()
                + ", selected=" + vm.getSelectedPatientIds()
                + ", range=" + vm.getStartDate() + "->" + vm.getEndDate()
                + ", data=" + vm.getDataTypes()
                + ", format=" + vm.getFormat()
                + ", charts=" + vm.isIncludeCharts()
                + ", anonymize=" + vm.isAnonymize()
                + ", language=" + vm.getLanguage()
                + ", dest=" + vm.getDestination()
                + ", saveTreeUri=" + vm.getSaveTreeUri());

        // TODO: hand off to your real exporter.
        Toast.makeText(this, "Export started…", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "runExport() END (stub)");
        // You may finish() or keep screen:
        // finish();
    }

    @Override protected void onStart()   { super.onStart();   Log.i(TAG,"onStart()"); }
    @Override protected void onResume()  { super.onResume();  Log.i(TAG,"onResume()"); }
    @Override protected void onPause()   { Log.i(TAG,"onPause()");   super.onPause(); }
    @Override protected void onStop()    { Log.i(TAG,"onStop()");    super.onStop(); }
    @Override protected void onDestroy() { Log.i(TAG,"onDestroy()"); super.onDestroy(); }
}
