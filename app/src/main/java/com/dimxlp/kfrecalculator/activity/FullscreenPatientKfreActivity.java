package com.dimxlp.kfrecalculator.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.adapter.KfreAssessmentAdapter;
import com.dimxlp.kfrecalculator.model.KfreCalculation;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FullscreenPatientKfreActivity  extends AppCompatActivity {

    private RecyclerView rvKfreAssessments;
    private KfreAssessmentAdapter adapter;
    private final List<KfreCalculation> assessmentList = new ArrayList<>();
    private static final String TAG = "RAFI|FullscreenPatientKfre";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_patient_kfre);

        String patientId = getIntent().getStringExtra("patientId");
        Log.d(TAG, "patientId = " + patientId);
        if (patientId == null) {
            Log.e(TAG, "No patient ID passed. Exiting.");
            finish();
            return;
        }

        rvKfreAssessments = findViewById(R.id.rvKfreAssessments);
        adapter = new KfreAssessmentAdapter(
                this,
                assessmentList,
                new KfreAssessmentAdapter.AssessmentClickListener() {
                    @Override
                    public void onAssessmentClick(KfreCalculation calc) {

                    }

                    @Override
                    public void onAssessmentDelete(KfreCalculation calc) {

                    }
                });
        rvKfreAssessments.setLayoutManager(new LinearLayoutManager(this));
        rvKfreAssessments.setAdapter(adapter);

        ImageView btnCollapse = findViewById(R.id.btnExpandCollapse);
        btnCollapse.setOnClickListener(v -> finish());

        ImageView btnFilter = findViewById(R.id.btnFilter);
        btnFilter.setVisibility(View.VISIBLE);
        btnFilter.setOnClickListener(v -> {
            showFilterDialog();
        });

        loadKfreAssessments(patientId);
    }

    private void showFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_filter_kfre, null);
        dialog.setContentView(view);

        // Date Sort group
        setupExpandableGroup(view,
                R.id.headerDateSort, R.id.contentDateSort, "Sort by Date");

        setupExpandableGroup(view,
                R.id.headerRisk2Sort, R.id.contentRisk2Sort, "Sort by 2-Year Risk");

        setupExpandableGroup(view,
                R.id.headerRisk5Sort, R.id.contentRisk5Sort, "Sort by 5-Year Risk");

        setupExpandableGroup(view,
                R.id.headerRiskCategory, R.id.contentRiskCategory, "Risk Category");

        setupExpandableGroup(view,
                R.id.headerHasNote, R.id.contentHasNote, "Has Note");

        setupExpandableGroup(view,
                R.id.headerDateRange, R.id.contentDateRange, "Date Range");

        // TODO: Add logic for Apply button
        dialog.show();
    }

    private void setupExpandableGroup(View root, int headerId, int contentId, String title) {
        View headerView = root.findViewById(headerId);
        LinearLayout contentView = root.findViewById(contentId);

        TextView tvTitle = headerView.findViewById(R.id.tvGroupTitle);
        ImageView ivChevron = headerView.findViewById(R.id.ivChevron);
        tvTitle.setText(title);

        headerView.setOnClickListener(v -> {
            boolean isVisible = contentView.getVisibility() == View.VISIBLE;
            contentView.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            ivChevron.setRotation(isVisible ? 0 : 180); // down = 0, up = 180
        });
    }

    private void loadKfreAssessments(@NonNull String patientId) {
        FirebaseFirestore.getInstance()
                .collection("KfreCalculations")
                .whereEqualTo("patientId", patientId)
                .get()
                .addOnSuccessListener(query -> {
                    List<KfreCalculation> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        KfreCalculation calc = doc.toObject(KfreCalculation.class);
                        list.add(calc);
                    }
                    adapter.updateData(list);
                });
    }
}
