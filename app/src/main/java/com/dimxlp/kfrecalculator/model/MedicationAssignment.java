package com.dimxlp.kfrecalculator.model;

public class MedicationAssignment {
    private String medicationId;
    private String frequency;

    public MedicationAssignment() {}

    public MedicationAssignment(String medicationId, String frequency) {
        this.medicationId = medicationId;
        this.frequency = frequency;
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
}
