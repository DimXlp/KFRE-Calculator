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
import com.dimxlp.kfrecalculator.adapter.KfreAssessmentAdapter;
import com.dimxlp.kfrecalculator.enumeration.Risk;
import com.dimxlp.kfrecalculator.enumeration.SortDirection;
import com.dimxlp.kfrecalculator.model.FilterOptionsKfre;
import com.dimxlp.kfrecalculator.model.KfreCalculation;
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

public class FullscreenPatientKfreActivity  extends AppCompatActivity {

    private RecyclerView rvKfreAssessments;
    private KfreAssessmentAdapter adapter;
    private final List<KfreCalculation> unfilteredAssessments = new ArrayList<>();
    private static final String TAG = "RAFI|FullscreenPatientKfre";
    private FilterOptionsKfre currentFilterOptions = new FilterOptionsKfre();
    private boolean dataHasChanged = false;

    private static class FilterDialogViewHolder {
        // Main Buttons
        final Button btnApply;
        final Button btnClear;

        // Category Groups
        final RadioGroup rgDateSort, rgRisk2yrSort, rgRisk5yrSort, rgRiskCategory;
        final CheckBox cbHasNote;
        final EditText etStartDate, etEndDate;

        // Headers and Clear Buttons for each category
        final View headerDateSort, headerRisk2Sort, headerRisk5Sort, headerRiskCategory, headerHasNote, headerDateRange;
        final ImageView ivClearDateSort, ivClearRisk2Sort, ivClearRisk5Sort, ivClearRiskCategory, ivClearHasNote, ivClearDateRange;

        // Content areas for expansion
        final LinearLayout contentDateSort, contentRisk2Sort, contentRisk5Sort, contentRiskCategory, contentHasNote, contentDateRange;

        public FilterDialogViewHolder(View view) {
            btnApply = view.findViewById(R.id.btnApplyFilters);
            btnClear = view.findViewById(R.id.btnClearFilters);

            rgDateSort = view.findViewById(R.id.rgDateSort);
            rgRisk2yrSort = view.findViewById(R.id.rgRisk2yrSort);
            rgRisk5yrSort = view.findViewById(R.id.rgRisk5yrSort);
            rgRiskCategory = view.findViewById(R.id.rgRiskCategory);
            cbHasNote = view.findViewById(R.id.cbHasNote);
            etStartDate = view.findViewById(R.id.etStartDate);
            etEndDate = view.findViewById(R.id.etEndDate);

            headerDateSort = view.findViewById(R.id.headerDateSort);
            headerRisk2Sort = view.findViewById(R.id.headerRisk2Sort);
            headerRisk5Sort = view.findViewById(R.id.headerRisk5Sort);
            headerRiskCategory = view.findViewById(R.id.headerRiskCategory);
            headerHasNote = view.findViewById(R.id.headerHasNote);
            headerDateRange = view.findViewById(R.id.headerDateRange);

            ivClearDateSort = headerDateSort.findViewById(R.id.ivClearCategory);
            ivClearRisk2Sort = headerRisk2Sort.findViewById(R.id.ivClearCategory);
            ivClearRisk5Sort = headerRisk5Sort.findViewById(R.id.ivClearCategory);
            ivClearRiskCategory = headerRiskCategory.findViewById(R.id.ivClearCategory);
            ivClearHasNote = headerHasNote.findViewById(R.id.ivClearCategory);
            ivClearDateRange = headerDateRange.findViewById(R.id.ivClearCategory);

            contentDateSort = view.findViewById(R.id.contentDateSort);
            contentRisk2Sort = view.findViewById(R.id.contentRisk2Sort);
            contentRisk5Sort = view.findViewById(R.id.contentRisk5Sort);
            contentRiskCategory = view.findViewById(R.id.contentRiskCategory);
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
            Log.e(TAG, "onCreate: Patient ID is null. Finishing activity.");
            finish();
            return;
        }

        rvKfreAssessments = findViewById(R.id.rvAssessments);
        adapter = new KfreAssessmentAdapter(
                this,
                new ArrayList<>(),
                new KfreAssessmentAdapter.AssessmentClickListener() {
                    @Override
                    public void onAssessmentClick(KfreCalculation calc) {
                        showAssessmentDetails(calc);
                    }

                    @Override
                    public void onAssessmentDelete(KfreCalculation calc) {
                        confirmAndDeleteAssessment(calc);
                    }
                });
        rvKfreAssessments.setLayoutManager(new LinearLayoutManager(this));
        rvKfreAssessments.setAdapter(adapter);

        TextView txtAssessmentTitle = findViewById(R.id.txtAssessmentTitle);
        txtAssessmentTitle.setText("KFRE Assessments");

        ImageView btnCollapse = findViewById(R.id.btnExpandCollapse);
        btnCollapse.setOnClickListener(v -> finish());

        ImageView btnFilter = findViewById(R.id.btnFilter);
        btnFilter.setVisibility(View.VISIBLE);
        btnFilter.setOnClickListener(v -> {
            Log.d(TAG, "Filter button clicked.");
            showFilterDialog();
        });

        loadKfreAssessments(patientId);
    }

    private void showAssessmentDetails(KfreCalculation calc) {
        Log.d(TAG, "showAssessmentDetails: Displaying details for KFRE assessmentId: " + calc.getKfreCalculationId());
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_kfre_assessment_details, null);

        TextView txtAge = sheetView.findViewById(R.id.txtDetailAge);
        TextView txtGender = sheetView.findViewById(R.id.txtDetailGender);
        TextView txtEgfr = sheetView.findViewById(R.id.txtDetailEgfr);
        TextView txtAcr = sheetView.findViewById(R.id.txtDetailAcr);
        TextView txtRisk2 = sheetView.findViewById(R.id.txtDetailRisk2);
        TextView txtRisk5 = sheetView.findViewById(R.id.txtDetailRisk5);
        TextView txtNotes = sheetView.findViewById(R.id.txtDetailNotes);
        Button btnClose = sheetView.findViewById(R.id.btnCloseDetail);

        txtAge.setText(String.valueOf(calc.getAge()));
        txtGender.setText(calc.getSex());
        txtEgfr.setText(String.format(Locale.getDefault(), "%.2f", calc.getEgfr()));
        txtAcr.setText(String.format(Locale.getDefault(), "%.2f", calc.getAcr()));
        txtRisk2.setText(String.format(Locale.getDefault(), "%.2f%%", calc.getRisk2Yr()));
        txtRisk5.setText(String.format(Locale.getDefault(), "%.2f%%", calc.getRisk5Yr()));
        txtNotes.setText(calc.getNotes() == null || calc.getNotes().trim().isEmpty() ? "—" : calc.getNotes());

        btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
    }

    private void confirmAndDeleteAssessment(KfreCalculation calc) {
        Log.d(TAG, "confirmAndDeleteAssessment: Showing confirmation for assessmentId: " + calc.getKfreCalculationId());
        new AlertDialog.Builder(this)
                .setTitle("Delete Assessment")
                .setMessage("Are you sure you want to delete this KFRE assessment?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    Log.i(TAG, "User confirmed deletion for assessmentId: " + calc.getKfreCalculationId());
                    FirebaseFirestore.getInstance()
                            .collection("KfreCalculations")
                            .document(calc.getKfreCalculationId())
                            .delete()
                            .addOnSuccessListener(unused -> {
                                Log.d(TAG, "Successfully deleted assessment: " + calc.getKfreCalculationId());
                                Toast.makeText(this, "Assessment deleted", Toast.LENGTH_SHORT).show();
                                dataHasChanged = true;
                                loadKfreAssessments(calc.getPatientId());
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to delete assessment: " + calc.getKfreCalculationId(), e);
                                Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showFilterDialog() {
        Log.d(TAG, "showFilterDialog: Displaying filter dialog.");
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_filter_kfre, null);
        dialog.setContentView(view);

        final FilterDialogViewHolder holder = new FilterDialogViewHolder(view);

        setupExpandableGroup(holder.headerDateSort, holder.contentDateSort, "Sort by Date");
        setupExpandableGroup(holder.headerRisk2Sort, holder.contentRisk2Sort, "Sort by 2-Year Risk");
        setupExpandableGroup(holder.headerRisk5Sort, holder.contentRisk5Sort, "Sort by 5-Year Risk");
        setupExpandableGroup(holder.headerRiskCategory, holder.contentRiskCategory, "Risk Category");
        setupExpandableGroup(holder.headerHasNote, holder.contentHasNote, "Has Note");
        setupExpandableGroup(holder.headerDateRange, holder.contentDateRange, "Date Range");

        populateFilterDialog(holder, currentFilterOptions);

        setupDatePicker(holder.etStartDate);
        setupDatePicker(holder.etEndDate);

        holder.btnApply.setOnClickListener(v -> {
            FilterOptionsKfre options = new FilterOptionsKfre();

            // Read values from the holder's views
            sortByDate(holder.rgDateSort, options);
            sortBy2YearRisk(holder.rgRisk2yrSort, options);
            sortBy5YearRisk(holder.rgRisk5yrSort, options);
            checkIfHasNote(holder.cbHasNote, options);
            checkRiskCategory(holder.rgRiskCategory, options);
            setDateRange(holder.etStartDate, holder.etEndDate, options);

            this.currentFilterOptions = options;

            dialog.dismiss();
            applyFilters(options);
        });

        holder.btnClear.setOnClickListener(v -> {
            holder.rgDateSort.clearCheck();
            holder.rgRisk2yrSort.clearCheck();
            holder.rgRisk5yrSort.clearCheck();
            holder.rgRiskCategory.clearCheck();
            holder.cbHasNote.setChecked(false);
            holder.etStartDate.setText("");
            holder.etEndDate.setText("");

            holder.ivClearDateSort.setVisibility(View.GONE);
            holder.ivClearRisk2Sort.setVisibility(View.GONE);
            holder.ivClearRisk5Sort.setVisibility(View.GONE);
            holder.ivClearRiskCategory.setVisibility(View.GONE);
            holder.ivClearHasNote.setVisibility(View.GONE);
            holder.ivClearDateRange.setVisibility(View.GONE);
        });

        dialog.show();
    }

    private void populateFilterDialog(FilterDialogViewHolder holder, @NonNull FilterOptionsKfre options) {
        // 1. Date Sort
        if (options.getDateSort() != SortDirection.NONE) {
            if (options.getDateSort() == SortDirection.DESCENDING) holder.rgDateSort.check(R.id.rbDateNewest);
            else holder.rgDateSort.check(R.id.rbDateOldest);
            expandSection(holder.contentDateSort, holder.headerDateSort.findViewById(R.id.ivChevron));
            holder.ivClearDateSort.setVisibility(View.VISIBLE);
            holder.ivClearDateSort.setOnClickListener(v -> {
                holder.rgDateSort.clearCheck();
                v.setVisibility(View.GONE);
            });
        } else {
            holder.rgDateSort.clearCheck();
            holder.ivClearDateSort.setVisibility(View.GONE);
        }

        // 2. 2-Year Risk Sort
        if (options.getRisk2YrSort() != SortDirection.NONE) {
            if (options.getRisk2YrSort() == SortDirection.DESCENDING) holder.rgRisk2yrSort.check(R.id.rbRisk2HighLow);
            else holder.rgRisk2yrSort.check(R.id.rbRisk2LowHigh);
            expandSection(holder.contentRisk2Sort, holder.headerRisk2Sort.findViewById(R.id.ivChevron));
            holder.ivClearRisk2Sort.setVisibility(View.VISIBLE);
            holder.ivClearRisk2Sort.setOnClickListener(v -> {
                holder.rgRisk2yrSort.clearCheck();
                v.setVisibility(View.GONE);
            });
        } else {
            holder.rgRisk2yrSort.clearCheck();
            holder.ivClearRisk2Sort.setVisibility(View.GONE);
        }

        // 3. 5-Year Risk Sort
        if (options.getRisk5YrSort() != SortDirection.NONE) {
            if (options.getRisk5YrSort() == SortDirection.DESCENDING) holder.rgRisk5yrSort.check(R.id.rbRisk5HighLow);
            else holder.rgRisk5yrSort.check(R.id.rbRisk5LowHigh);
            expandSection(holder.contentRisk5Sort, holder.headerRisk5Sort.findViewById(R.id.ivChevron));
            holder.ivClearRisk5Sort.setVisibility(View.VISIBLE);
            holder.ivClearRisk5Sort.setOnClickListener(v -> {
                holder.rgRisk5yrSort.clearCheck();
                v.setVisibility(View.GONE);
            });
        } else {
            holder.rgRisk5yrSort.clearCheck();
            holder.ivClearRisk5Sort.setVisibility(View.GONE);
        }

        // 4. Risk Category
        if (options.getRiskCategory() != null) {
            switch (options.getRiskCategory()) {
                case LOW: holder.rgRiskCategory.check(R.id.rbRiskLow); break;
                case MEDIUM: holder.rgRiskCategory.check(R.id.rbRiskMedium); break;
                case HIGH: holder.rgRiskCategory.check(R.id.rbRiskHigh); break;
            }
            expandSection(holder.contentRiskCategory, holder.headerRiskCategory.findViewById(R.id.ivChevron));
            holder.ivClearRiskCategory.setVisibility(View.VISIBLE);
            holder.ivClearRiskCategory.setOnClickListener(v -> {
                holder.rgRiskCategory.clearCheck();
                v.setVisibility(View.GONE);
            });
        } else {
            holder.rgRiskCategory.clearCheck();
            holder.ivClearRiskCategory.setVisibility(View.GONE);
        }

        // 5. Has Note
        if (options.isHasNoteOnly()) {
            holder.cbHasNote.setChecked(true);
            expandSection(holder.contentHasNote, holder.headerHasNote.findViewById(R.id.ivChevron));
            holder.ivClearHasNote.setVisibility(View.VISIBLE);
            holder.ivClearHasNote.setOnClickListener(v -> {
                holder.cbHasNote.setChecked(false);
                v.setVisibility(View.GONE);
            });
        } else {
            holder.cbHasNote.setChecked(false);
            holder.ivClearHasNote.setVisibility(View.GONE);
        }

        // 6. Date Range
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        if (options.getStartDate() != null || options.getEndDate() != null) {
            if (options.getStartDate() != null) holder.etStartDate.setText(sdf.format(options.getStartDate()));
            if (options.getEndDate() != null) holder.etEndDate.setText(sdf.format(options.getEndDate()));
            expandSection(holder.contentDateRange, holder.headerDateRange.findViewById(R.id.ivChevron));
            holder.ivClearDateRange.setVisibility(View.VISIBLE);
            holder.ivClearDateRange.setOnClickListener(v -> {
                holder.etStartDate.setText("");
                holder.etEndDate.setText("");
                v.setVisibility(View.GONE);
            });
        } else {
            holder.etStartDate.setText("");
            holder.etEndDate.setText("");
            holder.ivClearDateRange.setVisibility(View.GONE);
        }
    }

    private void expandSection(LinearLayout contentView, ImageView chevron) {
        contentView.setVisibility(View.VISIBLE);
        chevron.setRotation(180);
    }

    private void applyFilters(FilterOptionsKfre options) {
        Log.d(TAG, "applyFilters: Applying filters. Unfiltered item count: " + unfilteredAssessments.size());
        List<KfreCalculation> result = new ArrayList<>();

        for (KfreCalculation calc : unfilteredAssessments) {

            // Has note
            if (options.isHasNoteOnly() && (calc.getNotes() == null || calc.getNotes().trim().isEmpty())) {
                continue;
            }

            // Risk category filter
            if (options.getRiskCategory() != null) {
                Risk actualCategory = classifyRiskCategory(calc.getRisk2Yr());
                if (actualCategory != options.getRiskCategory()) {
                    continue;
                }
            }

            // Date range
            if (options.getStartDate() != null && calc.getCreatedAt() < options.getStartDate().getTime()) {
                continue;
            }
            if (options.getEndDate() != null && calc.getCreatedAt() > options.getEndDate().getTime()) {
                continue;
            }

            result.add(calc);
        }

        // Sorting
        if (options.getDateSort() != SortDirection.NONE) {
            Log.d(TAG, "Sorting by date: " + options.getDateSort());
            result.sort((a, b) -> {
                int cmp = Long.compare(a.getCreatedAt(), b.getCreatedAt());
                return options.getDateSort() == SortDirection.ASCENDING ? cmp : -cmp;
            });
        } else if (options.getRisk2YrSort() != SortDirection.NONE) {
            Log.d(TAG, "Sorting by 2-Year Risk: " + options.getRisk2YrSort());
            result.sort((a, b) -> {
                int cmp = Double.compare(a.getRisk2Yr(), b.getRisk2Yr());
                return options.getRisk2YrSort() == SortDirection.ASCENDING ? cmp : -cmp;
            });
        } else if (options.getRisk5YrSort() != SortDirection.NONE) {
            Log.d(TAG, "Sorting by 5-Year Risk: " + options.getRisk5YrSort());
            result.sort((a, b) -> {
                int cmp = Double.compare(a.getRisk5Yr(), b.getRisk5Yr());
                return options.getRisk5YrSort() == SortDirection.ASCENDING ? cmp : -cmp;
            });
        }

        Log.d(TAG, "Filtering complete. Filtered item count: " + result.size());
        adapter.updateData(result);
    }

    private Risk classifyRiskCategory(double risk2Yr) {
        if (risk2Yr >= 20.0) return Risk.HIGH;
        else if (risk2Yr >= 10.0) return Risk.MEDIUM;
        else return Risk.LOW;
    }


    private void setupDatePicker(EditText editText) {
        editText.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            DatePickerDialog dpd = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        cal.set(year, month, dayOfMonth);
                        editText.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime()));
                    },
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
            );
            dpd.show();
        });
    }

    private static void setDateRange(EditText etStartDate, EditText etEndDate, FilterOptionsKfre options) {
        options.setStartDate(parseDate(etStartDate.getText().toString()));
        options.setEndDate(parseDate(etEndDate.getText().toString()));
    }

    private static Date parseDate(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        try {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(text);
        } catch (ParseException e) {
            Log.e(TAG, "parseDate: Failed to parse date string: " + text, e);
            return null;
        }
    }

    private static void checkRiskCategory(RadioGroup rgRiskCategory, FilterOptionsKfre options) {
        int selectedCategory = rgRiskCategory.getCheckedRadioButtonId();
        if (selectedCategory == R.id.rbRiskLow) {
            options.setRiskCategory(Risk.LOW);
        } else if (selectedCategory == R.id.rbRiskMedium) {
            options.setRiskCategory(Risk.MEDIUM);
        } else if (selectedCategory == R.id.rbRiskHigh) {
            options.setRiskCategory(Risk.HIGH);
        }
    }

    private static void checkIfHasNote(CheckBox cbHasNote, FilterOptionsKfre options) {
        options.setHasNoteOnly(cbHasNote.isChecked());
    }

    private static void sortBy5YearRisk(RadioGroup rgRisk5yrSort, FilterOptionsKfre options) {
        int selectedRisk5 = rgRisk5yrSort.getCheckedRadioButtonId();
        if (selectedRisk5 == R.id.rbRisk5HighLow) {
            options.setRisk5YrSort(SortDirection.DESCENDING);
        } else if (selectedRisk5 == R.id.rbRisk5LowHigh) {
            options.setRisk5YrSort(SortDirection.ASCENDING);
        }
    }

    private static void sortBy2YearRisk(RadioGroup rgRisk2yrSort, FilterOptionsKfre options) {
        int selectedRisk2 = rgRisk2yrSort.getCheckedRadioButtonId();
        if (selectedRisk2 == R.id.rbRisk2HighLow) {
            options.setRisk2YrSort(SortDirection.DESCENDING);
        } else if (selectedRisk2 == R.id.rbRisk2LowHigh) {
            options.setRisk2YrSort(SortDirection.ASCENDING);
        }
    }

    private static void sortByDate(RadioGroup rgDateSort, FilterOptionsKfre options) {
        int selectedDateSortId = rgDateSort.getCheckedRadioButtonId();
        if (selectedDateSortId == R.id.rbDateNewest) {
            options.setDateSort(SortDirection.DESCENDING);
        } else if (selectedDateSortId == R.id.rbDateOldest) {
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
        TextView tvTitle = headerView.findViewById(R.id.tvGroupTitle);
        ImageView ivChevron = headerView.findViewById(R.id.ivChevron);
        tvTitle.setText(title);

        headerView.setOnClickListener(v -> {
            boolean isVisible = contentView.getVisibility() == View.VISIBLE;
            long DURATION = 250;

            // Animate Chevron
            ObjectAnimator chevronAnimator = ObjectAnimator.ofFloat(ivChevron, "rotation", isVisible ? 180f : 0f, isVisible ? 0f : 180f);
            chevronAnimator.setDuration(DURATION);

            // Animate Height
            if (isVisible) {
                // Collapse
                int initialHeight = contentView.getHeight();
                ValueAnimator heightAnimator = ValueAnimator.ofInt(initialHeight, 0);
                heightAnimator.setDuration(DURATION);
                heightAnimator.addUpdateListener(animation -> {
                    contentView.getLayoutParams().height = (int) animation.getAnimatedValue();
                    contentView.requestLayout();
                });
                heightAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        contentView.setVisibility(View.GONE);
                    }
                });
                chevronAnimator.start();
                heightAnimator.start();
            } else {
                // Expand
                contentView.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                int targetHeight = contentView.getMeasuredHeight();
                contentView.getLayoutParams().height = 0;
                contentView.setVisibility(View.VISIBLE);

                ValueAnimator heightAnimator = ValueAnimator.ofInt(0, targetHeight);
                heightAnimator.setDuration(DURATION);
                heightAnimator.addUpdateListener(animation -> {
                    contentView.getLayoutParams().height = (int) animation.getAnimatedValue();
                    contentView.requestLayout();
                });
                heightAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        contentView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    }
                });
                chevronAnimator.start();
                heightAnimator.start();
            }
        });
    }

    private void loadKfreAssessments(@NonNull String patientId) {
        Log.d(TAG, "loadKfreAssessments: Loading assessments for patientId: " + patientId);
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
                    Log.i(TAG, "loadKfreAssessments: Found " + list.size() + " assessments.");

                    // Sort the list by date ascending (oldest to newest)
                    Collections.sort(list, (a, b) ->
                            Long.compare(a.getCreatedAt(), b.getCreatedAt()));

                    // Populate the master list for filtering and clearing
                    unfilteredAssessments.clear();
                    unfilteredAssessments.addAll(list);

                    // Update the adapter with the sorted, complete list
                    adapter.updateData(unfilteredAssessments);
                })
                .addOnFailureListener(e -> Log.e(TAG, "loadKfreAssessments: Error loading assessments.", e));
    }
}