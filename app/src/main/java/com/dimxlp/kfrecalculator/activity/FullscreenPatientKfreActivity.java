package com.dimxlp.kfrecalculator.activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.adapter.KfreAssessmentAdapter;
import com.dimxlp.kfrecalculator.model.KfreCalculation;
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
                        // Optional: handle click (e.g., show dialog or ignore)
                    }

                    @Override
                    public void onAssessmentDelete(KfreCalculation calc) {
                        // Optional: disable deletion or show Toast
                    }
                });
        rvKfreAssessments.setLayoutManager(new LinearLayoutManager(this));
        rvKfreAssessments.setAdapter(adapter);

        ImageView btnCollapse = findViewById(R.id.btnExpandCollapse);
        btnCollapse.setOnClickListener(v -> finish());

        loadKfreAssessments(patientId);
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
