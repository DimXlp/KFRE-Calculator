package com.dimxlp.kfrecalculator.viewmodel;

import android.net.Uri;
import android.util.Log;
import androidx.lifecycle.ViewModel;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class DoctorExportViewModel extends ViewModel {
    private static final String TAG = "RAFI|DoctorExportVM";

    // ---------- SCOPE ----------
    public enum ScopeType { ALL_PATIENTS, SELECTED_PATIENTS, DATE_RANGE, CURRENT_PATIENT }
    private ScopeType scopeType = ScopeType.ALL_PATIENTS;
    private final Set<String> selectedPatientIds = new HashSet<>();
    private Date startDate = null;
    private Date endDate = null;
    private String currentPatientId = null;

    // ---------- DATA ----------
    public enum DataType { CALCULATIONS, NOTES, MEDICATIONS }
    private final EnumSet<DataType> dataTypes = EnumSet.of(DataType.CALCULATIONS);

    // ---------- OPTIONS ----------
    public enum Format { PDF, CSV }
    public enum Language { ENGLISH, GREEK }
    private Format format = Format.PDF;
    private boolean includeCharts = true;
    private boolean anonymize = false;
    private Language language = Language.ENGLISH;

    // ---------- DESTINATION ----------
    public enum Destination { SAVE, SHARE }
    private Destination destination = Destination.SAVE;
    private Uri saveTreeUri = null; // optional; directory chosen via ACTION_OPEN_DOCUMENT_TREE

    // ----- Scope -----
    public ScopeType getScopeType() { return scopeType; }
    public void setScopeType(ScopeType scopeType) {
        Log.d(TAG, "setScopeType: " + scopeType);
        this.scopeType = scopeType;
    }
    public Set<String> getSelectedPatientIds() { return selectedPatientIds; }
    public void setSelectedPatientIds(Set<String> ids) {
        selectedPatientIds.clear();
        if (ids != null) selectedPatientIds.addAll(ids);
        Log.d(TAG, "setSelectedPatientIds: " + selectedPatientIds);
    }
    public Date getStartDate() { return startDate; }
    public Date getEndDate()   { return endDate; }
    public void setDateRange(Date start, Date end) {
        this.startDate = start; this.endDate = end;
        Log.d(TAG, "setDateRange: " + start + " -> " + end);
    }
    public String getCurrentPatientId() { return currentPatientId; }
    public void setCurrentPatientId(String id) {
        this.currentPatientId = id;
        if (id != null) {
            scopeType = ScopeType.CURRENT_PATIENT;
            selectedPatientIds.clear();
            selectedPatientIds.add(id);
        }
        Log.d(TAG, "setCurrentPatientId: " + id + " (scope forced to CURRENT_PATIENT)");
    }

    // ----- Data -----
    public EnumSet<DataType> getDataTypes() { return EnumSet.copyOf(dataTypes); }
    public void setDataTypes(EnumSet<DataType> set) {
        dataTypes.clear();
        if (set != null) dataTypes.addAll(set);
        Log.d(TAG, "setDataTypes: " + dataTypes);
    }

    // ----- Options -----
    public Format getFormat() { return format; }
    public void setFormat(Format format) {
        this.format = format;
        Log.d(TAG, "setFormat: " + format);
    }
    public boolean isIncludeCharts() { return includeCharts; }
    public void setIncludeCharts(boolean includeCharts) {
        this.includeCharts = includeCharts;
        Log.d(TAG, "setIncludeCharts: " + includeCharts);
    }
    public boolean isAnonymize() { return anonymize; }
    public void setAnonymize(boolean anonymize) {
        this.anonymize = anonymize;
        Log.d(TAG, "setAnonymize: " + anonymize);
    }
    public Language getLanguage() { return language; }
    public void setLanguage(Language language) {
        this.language = language;
        Log.d(TAG, "setLanguage: " + language);
    }

    // ----- Destination -----
    public Destination getDestination() { return destination; }
    public void setDestination(Destination d) {
        this.destination = d;
        Log.d(TAG, "setDestination: " + d);
    }
    public Uri getSaveTreeUri() { return saveTreeUri; }
    public void setSaveTreeUri(Uri uri) {
        this.saveTreeUri = uri;
        Log.d(TAG, "setSaveTreeUri: " + uri);
    }
}
