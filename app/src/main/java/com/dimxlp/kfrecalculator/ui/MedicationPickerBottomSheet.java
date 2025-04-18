package com.dimxlp.kfrecalculator.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MedicationPickerBottomSheet extends BottomSheetDialogFragment {

    public interface OnMedicationSelectedListener {
        void onMedicationSelected(String medicationId, String medicationName, String frequency);
    }

    private static final String TAG = "RAFI|MedicationPickerBottomSheet";
    private EditText inputSearch;
    private RecyclerView recyclerView;
    private Button btnShowAddDialog;
    private Spinner spinnerFrequency;
    private EditText inputCustomFrequency;
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
        inputCustomFrequency = view.findViewById(R.id.inputCustomFrequency);
        btnAdd = view.findViewById(R.id.btnAddSelectedMedication);
        btnShowAddDialog = view.findViewById(R.id.btnShowAddMedicationDialog);

        ArrayAdapter<CharSequence> frequencyAdapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.frequency_options,
                android.R.layout.simple_spinner_item
        );
        frequencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFrequency.setAdapter(frequencyAdapter);

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

        spinnerFrequency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                if (selected.equalsIgnoreCase("Custom...")) {
                    inputCustomFrequency.setVisibility(View.VISIBLE);
                } else {
                    inputCustomFrequency.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnAdd.setOnClickListener(v -> {
            Medication selectedMed = adapter.getSelectedMedication();
            String frequency;
            if (spinnerFrequency.getSelectedItem().toString().equalsIgnoreCase("Custom...")) {
                frequency = inputCustomFrequency.getText().toString().trim();
            } else {
                frequency = spinnerFrequency.getSelectedItem().toString();
            }
            if (selectedMed != null && !frequency.isEmpty()) {
                if (listener != null) {
                    listener.onMedicationSelected(selectedMed.getMedicationId(), selectedMed.getName(), frequency);
                }
                dismiss();
            }
        });

        btnShowAddDialog.setOnClickListener(v -> showAddMedicationDialog());
    }

    private void showAddMedicationDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_medication, null);
        EditText inputName = dialogView.findViewById(R.id.inputMedicationName);
        EditText inputDosage = dialogView.findViewById(R.id.inputMedicationDosage);

        new AlertDialog.Builder(getContext())
                .setTitle("Add Medication")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = inputName.getText().toString().trim();
                    String dosageValue = inputDosage.getText().toString().trim();
                    String dosage = dosageValue + " mg";

                    if (!name.isEmpty() && !dosageValue.isEmpty()) {
                        Map<String, Object> newMed = new HashMap<>();
                        newMed.put("name", name);
                        newMed.put("dosage", dosage);

                        FirebaseFirestore.getInstance().collection("Medications")
                                .add(newMed)
                                .addOnSuccessListener(docRef -> {
                                    Toast.makeText(getContext(), "Medication added", Toast.LENGTH_SHORT).show();
                                    loadMedicationsFromFirestore(); // refresh list
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Failed to add medication", Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Error adding new medication", e);
                                });
                    } else {
                        Toast.makeText(getContext(), "Please fill both fields", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
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
