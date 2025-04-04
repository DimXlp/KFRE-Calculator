package com.dimxlp.kfrecalculator.model;

import java.util.HashMap;
import java.util.Map;

public class MedicationAssignment {
    private String assignmentId;
    private String patientId;
    private String diseaseId;
    private String medicationId;
    private String frequency;

    public MedicationAssignment() {

    }

    public MedicationAssignment(String assignmentId, String patientId, String diseaseId, String medicationId, String frequency) {
        this.assignmentId = assignmentId;
        this.patientId = patientId;
        this.diseaseId = diseaseId;
        this.medicationId = medicationId;
        this.frequency = frequency;
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getDiseaseId() {
        return diseaseId;
    }

    public void setDiseaseId(String diseaseId) {
        this.diseaseId = diseaseId;
    }

    public String getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(String medicationId) {
        this.medicationId = medicationId;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("assignmentId", assignmentId);
        map.put("patientId", patientId);
        map.put("diseaseId", diseaseId);
        map.put("medicationId", medicationId);
        map.put("frequency", frequency);
        return map;
    }
}
