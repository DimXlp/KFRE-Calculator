package com.dimxlp.kfrecalculator.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dimxlp.kfrecalculator.R;

public class QuickKfreCalculatorFragment extends BaseKfreCalculatorFragment {

    private static final String TAG = "RAFI|KfreQuickCalc";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        // Do not show Save button in quick calculator
        if (view != null) {
            view.findViewById(R.id.btnSave).setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_base_kfre_calculator;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }
}
