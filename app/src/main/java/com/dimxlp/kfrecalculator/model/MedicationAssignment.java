package com.dimxlp.kfrecalculator.model;

public class MedicationAssignment {
    private String medicationId;
    private String medicationName;
    private String diseaseId;
    private String frequency;

    public MedicationAssignment() {}

    public MedicationAssignment(String medicationId, String medicationName, String frequency) {
        this.medicationId = medicationId;
        this.medicationName = medicationName;
        this.frequency = frequency;
    }

    public String getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(String medicationId) {
        this.medicationId = medicationId;
    }

    public String getMedicationName() {
        return medicationName;
    }

    public void setMedicationName(String medicationName) {
        this.medicationName = medicationName;
    }

    public String getDiseaseId() {
        return diseaseId;
    }

    public void setDiseaseId(String diseaseId) {
        this.diseaseId = diseaseId;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }
}
