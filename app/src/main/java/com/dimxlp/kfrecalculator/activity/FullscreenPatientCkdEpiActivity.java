package com.dimxlp.kfrecalculator.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.adapter.CkdEpiAssessmentAdapter;
import com.dimxlp.kfrecalculator.enumeration.SortDirection;
import com.dimxlp.kfrecalculator.model.CkdEpiCalculation;
import com.dimxlp.kfrecalculator.model.FilterOptionsCkdEpi;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FullscreenPatientCkdEpiActivity extends AppCompatActivity {

    private RecyclerView rvCkdEpiAssessments;
    private CkdEpiAssessmentAdapter adapter;
    private final List<CkdEpiCalculation> unfilteredAssessments = new ArrayList<>();
    private static final String TAG = "RAFI|FullscreenPatientCkdEpi";
    private FilterOptionsCkdEpi currentFilterOptions = new FilterOptionsCkdEpi();
    private boolean dataHasChanged = false;

    private static class FilterDialogViewHolder {
        final Button btnApply;
        final Button btnClear;
        final RadioGroup rgDateSort, rgEgfrSort;
        final CheckBox cbHasNote;
        final EditText etStartDate, etEndDate;
        final View headerDateSort, headerEgfrSort, headerHasNote, headerDateRange;
        final ImageView ivClearDateSort, ivClearEgfrSort, ivClearHasNote, ivClearDateRange;
        final LinearLayout contentDateSort, contentEgfrSort, contentHasNote, contentDateRange;

        public FilterDialogViewHolder(View view) {
            btnApply = view.findViewById(R.id.btnApplyFilters);
            btnClear = view.findViewById(R.id.btnClearFilters);
            rgDateSort = view.findViewById(R.id.rgDateSort);
            rgEgfrSort = view.findViewById(R.id.rgEgfrSort);
            cbHasNote = view.findViewById(R.id.cbHasNote);
            etStartDate = view.findViewById(R.id.etStartDate);
            etEndDate = view.findViewById(R.id.etEndDate);
            headerDateSort = view.findViewById(R.id.headerDateSort);
            headerEgfrSort = view.findViewById(R.id.headerEgfrSort);
            headerHasNote = view.findViewById(R.id.headerHasNote);
            headerDateRange = view.findViewById(R.id.headerDateRange);
            ivClearDateSort = headerDateSort.findViewById(R.id.ivClearCategory);
            ivClearEgfrSort = headerEgfrSort.findViewById(R.id.ivClearCategory);
            ivClearHasNote = headerHasNote.findViewById(R.id.ivClearCategory);
            ivClearDateRange = headerDateRange.findViewById(R.id.ivClearCategory);
            contentDateSort = view.findViewById(R.id.contentDateSort);
            contentEgfrSort = view.findViewById(R.id.contentEgfrSort);
            contentHasNote = view.findViewById(R.id.contentHasNote);
            contentDateRange = view.findViewById(R.id.contentDateRange);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_patient_assessment);
        Log.d(TAG, "onCreate: Activity created.");

        String patientId = getIntent().getStringExtra("patientId");
        if (patientId == null) {
            Log.e(TAG, "Patient ID is null. Finishing activity.");
            finish();
            return;
        }
        Log.i(TAG, "Displaying fullscreen assessments for patientId: " + patientId);

        rvCkdEpiAssessments = findViewById(R.id.rvAssessments);
        adapter = new CkdEpiAssessmentAdapter(
                this,
                new ArrayList<>(),
                new CkdEpiAssessmentAdapter.AssessmentClickListener() {
                    @Override
                    public void onAssessmentClick(CkdEpiCalculation calc) {
                        showAssessmentDetails(calc);
                    }
                    @Override
                    public void onAssessmentDelete(CkdEpiCalculation calc) {
                        confirmAndDeleteAssessment(calc);
                    }
                });
        rvCkdEpiAssessments.setLayoutManager(new LinearLayoutManager(this));
        rvCkdEpiAssessments.setAdapter(adapter);

        TextView txtAssessmentTitle = findViewById(R.id.txtAssessmentTitle);
        txtAssessmentTitle.setText("CKD-EPI Assessments");

        findViewById(R.id.btnExpandCollapse).setOnClickListener(v -> finish());
        findViewById(R.id.btnFilter).setOnClickListener(v -> showFilterDialog());

        loadCkdEpiAssessments(patientId);
    }

    private void showAssessmentDetails(CkdEpiCalculation calc) {
        Log.d(TAG, "showAssessmentDetails: Displaying details for CKD-EPI assessment created at " + new Date(calc.getCreatedAt()));
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_ckd_epi_assessment_details, null);

        // Find all views from the layout
        TextView txtAge = sheetView.findViewById(R.id.txtDetailAge);
        TextView txtGender = sheetView.findViewById(R.id.txtDetailGender);
        TextView txtCreatinine = sheetView.findViewById(R.id.txtDetailCreatinine);
        TextView txtEgfr = sheetView.findViewById(R.id.txtDetailEgfr);
        TextView txtNotes = sheetView.findViewById(R.id.txtDetailNotes);
        Button btnClose = sheetView.findViewById(R.id.btnCloseDetail);

        // Set data for relevant fields
        txtAge.setText(String.valueOf(calc.getAge()));
        txtGender.setText(calc.getSex());
        txtCreatinine.setText(calc.getCreatinine() + " mg/dL");
        txtEgfr.setText(String.format(Locale.getDefault(), "%.2f", calc.getResult()) + " mL/min/1.73m²");
        txtNotes.setText(calc.getNotes() == null || calc.getNotes().trim().isEmpty() ? "—" : calc.getNotes());

        btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
    }

    private void confirmAndDeleteAssessment(CkdEpiCalculation calc) {
        Log.d(TAG, "confirmAndDeleteAssessment: Showing confirmation dialog for CKD-EPI assessmentId: " + calc.getCkdEpiCalculationId());
        new AlertDialog.Builder(this)
                .setTitle("Delete Assessment")
                .setMessage("Are you sure you want to delete this CKD-EPI assessment?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    Log.i(TAG, "User confirmed deletion for assessmentId: " + calc.getCkdEpiCalculationId());
                    FirebaseFirestore.getInstance()
                            .collection("CkdEpiCalculations")
                            .document(calc.getCkdEpiCalculationId())
                            .delete()
                            .addOnSuccessListener(unused -> {
                                Log.d(TAG, "Successfully deleted assessment: " + calc.getCkdEpiCalculationId());
                                Toast.makeText(this, "Assessment deleted", Toast.LENGTH_SHORT).show();
                                dataHasChanged = true;
                                loadCkdEpiAssessments(calc.getPatientId());
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to delete assessment: " + calc.getCkdEpiCalculationId(), e);
                                Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showFilterDialog() {
        Log.d(TAG, "showFilterDialog: Displaying filter dialog.");
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_filter_ckd_epi, null);
        dialog.setContentView(view);

        final FilterDialogViewHolder holder = new FilterDialogViewHolder(view);

        setupExpandableGroup(holder.headerDateSort, holder.contentDateSort, "Sort by Date");
        setupExpandableGroup(holder.headerEgfrSort, holder.contentEgfrSort, "Sort by eGFR");
        setupExpandableGroup(holder.headerHasNote, holder.contentHasNote, "Has Note");
        setupExpandableGroup(holder.headerDateRange, holder.contentDateRange, "Date Range");

        populateFilterDialog(holder, currentFilterOptions);
        setupDatePicker(holder.etStartDate);
        setupDatePicker(holder.etEndDate);

        holder.btnApply.setOnClickListener(v -> {
            FilterOptionsCkdEpi options = new FilterOptionsCkdEpi();
            sortByDate(holder.rgDateSort, options);
            sortByEgfr(holder.rgEgfrSort, options);
            checkIfHasNote(holder.cbHasNote, options);
            setDateRange(holder.etStartDate, holder.etEndDate, options);
            this.currentFilterOptions = options;
            dialog.dismiss();
            applyFilters(options);
        });

        holder.btnClear.setOnClickListener(v -> {
            holder.rgDateSort.clearCheck();
            holder.rgEgfrSort.clearCheck();
            holder.cbHasNote.setChecked(false);
            holder.etStartDate.setText("");
            holder.etEndDate.setText("");
            holder.ivClearDateSort.setVisibility(View.GONE);
            holder.ivClearEgfrSort.setVisibility(View.GONE);
            holder.ivClearHasNote.setVisibility(View.GONE);
            holder.ivClearDateRange.setVisibility(View.GONE);
        });

        dialog.show();
    }

    private void populateFilterDialog(FilterDialogViewHolder holder, @NonNull FilterOptionsCkdEpi options) {
        if (options.getDateSort() != SortDirection.NONE) {
            if (options.getDateSort() == SortDirection.DESCENDING) holder.rgDateSort.check(R.id.rbDateNewest);
            else holder.rgDateSort.check(R.id.rbDateOldest);
            expandSection(holder.contentDateSort, holder.headerDateSort.findViewById(R.id.ivChevron));
            holder.ivClearDateSort.setVisibility(View.VISIBLE);
            holder.ivClearDateSort.setOnClickListener(v -> { holder.rgDateSort.clearCheck(); v.setVisibility(View.GONE); });
        }
        if (options.getEgfrSort() != SortDirection.NONE) {
            if (options.getEgfrSort() == SortDirection.DESCENDING) holder.rgEgfrSort.check(R.id.rbEgfrHighLow);
            else holder.rgEgfrSort.check(R.id.rbEgfrLowHigh);
            expandSection(holder.contentEgfrSort, holder.headerEgfrSort.findViewById(R.id.ivChevron));
            holder.ivClearEgfrSort.setVisibility(View.VISIBLE);
            holder.ivClearEgfrSort.setOnClickListener(v -> { holder.rgEgfrSort.clearCheck(); v.setVisibility(View.GONE); });
        }
        if (options.isHasNoteOnly()) {
            holder.cbHasNote.setChecked(true);
            expandSection(holder.contentHasNote, holder.headerHasNote.findViewById(R.id.ivChevron));
            holder.ivClearHasNote.setVisibility(View.VISIBLE);
            holder.ivClearHasNote.setOnClickListener(v -> { holder.cbHasNote.setChecked(false); v.setVisibility(View.GONE); });
        }
        if (options.getStartDate() != null || options.getEndDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            if (options.getStartDate() != null) holder.etStartDate.setText(sdf.format(options.getStartDate()));
            if (options.getEndDate() != null) holder.etEndDate.setText(sdf.format(options.getEndDate()));
            expandSection(holder.contentDateRange, holder.headerDateRange.findViewById(R.id.ivChevron));
            holder.ivClearDateRange.setVisibility(View.VISIBLE);
            holder.ivClearDateRange.setOnClickListener(v -> {
                holder.etStartDate.setText("");
                holder.etEndDate.setText("");
                v.setVisibility(View.GONE);
            });
        }
    }

    private void expandSection(LinearLayout contentView, ImageView chevron) {
        contentView.setVisibility(View.VISIBLE);
        chevron.setRotation(180);
    }

    private void applyFilters(FilterOptionsCkdEpi options) {
        Log.d(TAG, "applyFilters: Applying filters. Unfiltered item count: " + unfilteredAssessments.size());
        List<CkdEpiCalculation> result = new ArrayList<>();
        for (CkdEpiCalculation calc : unfilteredAssessments) {
            if (options.isHasNoteOnly() && (calc.getNotes() == null || calc.getNotes().trim().isEmpty())) {
                continue;
            }
            if (options.getStartDate() != null && calc.getCreatedAt() < options.getStartDate().getTime()) {
                continue;
            }
            if (options.getEndDate() != null && calc.getCreatedAt() > options.getEndDate().getTime()) {
                continue;
            }
            result.add(calc);
        }

        if (options.getDateSort() != SortDirection.NONE) {
            Log.d(TAG, "Sorting by date: " + options.getDateSort());
            result.sort((a, b) -> {
                int cmp = Long.compare(a.getCreatedAt(), b.getCreatedAt());
                return options.getDateSort() == SortDirection.ASCENDING ? cmp : -cmp;
            });
        } else if (options.getEgfrSort() != SortDirection.NONE) {
            Log.d(TAG, "Sorting by eGFR: " + options.getEgfrSort());
            result.sort((a, b) -> {
                int cmp = Double.compare(a.getResult(), b.getResult());
                return options.getEgfrSort() == SortDirection.ASCENDING ? cmp : -cmp;
            });
        }
        adapter.updateData(result);
    }

    private void setupDatePicker(EditText editText) {
        editText.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        cal.set(year, month, dayOfMonth);
                        editText.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime()));
                    },
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    private static void setDateRange(EditText etStartDate, EditText etEndDate, FilterOptionsCkdEpi options) {
        options.setStartDate(parseDate(etStartDate.getText().toString()));
        options.setEndDate(parseDate(etEndDate.getText().toString()));
    }

    private static Date parseDate(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        try {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(text);
        } catch (ParseException e) {
            return null;
        }
    }

    private static void checkIfHasNote(CheckBox cbHasNote, FilterOptionsCkdEpi options) {
        options.setHasNoteOnly(cbHasNote.isChecked());
    }

    private static void sortByEgfr(RadioGroup rgEgfrSort, FilterOptionsCkdEpi options) {
        int selected = rgEgfrSort.getCheckedRadioButtonId();
        if (selected == R.id.rbEgfrHighLow) {
            options.setEgfrSort(SortDirection.DESCENDING);
        } else if (selected == R.id.rbEgfrLowHigh) {
            options.setEgfrSort(SortDirection.ASCENDING);
        }
    }

    private static void sortByDate(RadioGroup rgDateSort, FilterOptionsCkdEpi options) {
        int selected = rgDateSort.getCheckedRadioButtonId();
        if (selected == R.id.rbDateNewest) {
            options.setDateSort(SortDirection.DESCENDING);
        } else if (selected == R.id.rbDateOldest) {
            options.setDateSort(SortDirection.ASCENDING);
        }
    }

    @Override
    public void finish() {
        Log.d(TAG, "finish: Activity finishing.");
        if (dataHasChanged) {
            Log.d(TAG, "finish: Data has changed, setting result to RESULT_OK.");
            setResult(Activity.RESULT_OK);
        } else {
            Log.d(TAG, "finish: Data has not changed, result will be RESULT_CANCELED.");
        }
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void setupExpandableGroup(View headerView, LinearLayout contentView, String title) {
        ((TextView) headerView.findViewById(R.id.tvGroupTitle)).setText(title);
        ImageView ivChevron = headerView.findViewById(R.id.ivChevron);
        headerView.setOnClickListener(v -> {
            boolean isVisible = contentView.getVisibility() == View.VISIBLE;
            ObjectAnimator.ofFloat(ivChevron, "rotation", isVisible ? 180f : 0f, isVisible ? 0f : 180f).setDuration(250).start();
            if (isVisible) {
                // Collapse
                ValueAnimator anim = ValueAnimator.ofInt(contentView.getHeight(), 0);
                anim.addUpdateListener(animation -> {
                    contentView.getLayoutParams().height = (int) animation.getAnimatedValue();
                    contentView.requestLayout();
                });
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(Animator animation) { contentView.setVisibility(View.GONE); }
                });
                anim.setDuration(250).start();
            } else {
                // Expand
                contentView.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                contentView.getLayoutParams().height = 0;
                contentView.setVisibility(View.VISIBLE);
                ValueAnimator anim = ValueAnimator.ofInt(0, contentView.getMeasuredHeight());
                anim.addUpdateListener(animation -> {
                    contentView.getLayoutParams().height = (int) animation.getAnimatedValue();
                    contentView.requestLayout();
                });
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(Animator animation) { contentView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT; }
                });
                anim.setDuration(250).start();
            }
        });
    }

    private void loadCkdEpiAssessments(@NonNull String patientId) {
        Log.d(TAG, "loadCkdEpiAssessments: Loading assessments for patientId: " + patientId);
        FirebaseFirestore.getInstance()
                .collection("CkdEpiCalculations")
                .whereEqualTo("patientId", patientId)
                .get()
                .addOnSuccessListener(query -> {
                    List<CkdEpiCalculation> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        list.add(doc.toObject(CkdEpiCalculation.class));
                    }
                    Log.i(TAG, "loadCkdEpiAssessments: Found " + list.size() + " assessments.");
                    Collections.sort(list, (a, b) -> Long.compare(a.getCreatedAt(), b.getCreatedAt()));
                    unfilteredAssessments.clear();
                    unfilteredAssessments.addAll(list);
                    adapter.updateData(unfilteredAssessments);
                })
                .addOnFailureListener(e -> Log.e(TAG, "loadCkdEpiAssessments: Error loading assessments.", e));
    }
}