package com.dimxlp.kfrecalculator.model;

public class Medication {
    private String medicationId;
    private String name;
    private String dosage;
    private String addedBy;
    private long createdAt;

    public Medication() {}

    public Medication(String medicationId, String name, String dosage, String addedBy, long createdAt) {
        this.medicationId = medicationId;
        this.name = name;
        this.dosage = dosage;
        this.addedBy = addedBy;
        this.createdAt = createdAt;
    }

    public Medication(String medicationId, String name, String dosage) {
        this.medicationId = medicationId;
        this.name = name;
        this.dosage = dosage;
    }

    public String getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(String medicationId) {
        this.medicationId = medicationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(String addedBy) {
        this.addedBy = addedBy;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
