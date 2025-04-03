package com.dimxlp.kfrecalculator.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.adapter.MedicationAdapter;
import com.dimxlp.kfrecalculator.model.Medication;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MedicationPickerBottomSheet extends BottomSheetDialogFragment {

    public interface OnMedicationSelectedListener {
        void onMedicationSelected(String medicationId, String medicationName, String frequency);
    }

    private static final String TAG = "RAFI|MedicationPickerBottomSheet";
    private EditText inputSearch;
    private RecyclerView recyclerView;
    private Spinner spinnerFrequency;
    private Button btnAdd;
    private MedicationAdapter adapter;
    private List<Medication> medicationList = new ArrayList<>();

    private OnMedicationSelectedListener listener;

    public MedicationPickerBottomSheet() {}

    public void setOnMedicationSelectedListener(OnMedicationSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_medication_picker, container, false);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnMedicationSelectedListener) {
            listener = (OnMedicationSelectedListener) context;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        inputSearch = view.findViewById(R.id.inputSearchMedication);
        recyclerView = view.findViewById(R.id.medicationRecyclerView);
        spinnerFrequency = view.findViewById(R.id.spinnerFrequency);
        btnAdd = view.findViewById(R.id.btnAddSelectedMedication);

        loadMedicationsFromFirestore();

        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnAdd.setOnClickListener(v -> {
            Medication selectedMed = adapter.getSelectedMedication();
            String frequency = spinnerFrequency.getSelectedItem() != null ? spinnerFrequency.getSelectedItem().toString() : "";
            if (selectedMed != null && !frequency.isEmpty()) {
                if (listener != null) {
                    listener.onMedicationSelected(selectedMed.getMedicationId(), selectedMed.getName(), frequency);
                }
                dismiss();
            }
        });
    }

    private void loadMedicationsFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Medications")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    medicationList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        String dosage = doc.getString("dosage");
                        if (name != null && dosage != null) {
                            medicationList.add(new Medication(id, name, dosage));
                        }
                    }
                    adapter = new MedicationAdapter(medicationList);
                    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                    recyclerView.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load medications", e);
                    Toast.makeText(getContext(), "Error loading medications", Toast.LENGTH_SHORT).show();
                });
    }

}
