package com.dimxlp.kfrecalculator.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dimxlp.kfrecalculator.R;

public class QuickCkdEpiCalculatorFragment extends BaseCkdEpiCalculatorFragment {

    private static final String TAG = "RAFI|CkdEpiQuickCalc";

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_base_ckd_epi_calculator;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Creating quick CKD-EPI calculator view.");
        View view = super.onCreateView(inflater, container, savedInstanceState);

        // The "Save" button is not needed in the quick calculator.
        if (view != null) {
            Log.d(TAG, "onCreateView: Hiding save button.");
            view.findViewById(R.id.btnSave).setVisibility(View.GONE);
        }

        return view;
    }
}
