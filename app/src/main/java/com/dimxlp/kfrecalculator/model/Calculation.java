package com.dimxlp.kfrecalculator.model;

public class Calculation {
    private String calculationId;
    private String patientId;
    private String userId;
    private int age;
    private String sex;
    private double egfr;
    private double acr;
    private double risk2Yr;
    private double risk5Yr;
    private long createdAt;
    private long updatedAt;
    private String notes;

    public Calculation() {}

    public Calculation(String calculationId, String patientId, String userId, int age, String sex, double egfr, double acr, double risk2Yr, double risk5Yr, long createdAt, long updatedAt, String notes) {
        this.calculationId = calculationId;
        this.patientId = patientId;
        this.userId = userId;
        this.age = age;
        this.sex = sex;
        this.egfr = egfr;
        this.acr = acr;
        this.risk2Yr = risk2Yr;
        this.risk5Yr = risk5Yr;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.notes = notes;
    }

    public String getCalculationId() {
        return calculationId;
    }

    public void setCalculationId(String calculationId) {
        this.calculationId = calculationId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public double getEgfr() {
        return egfr;
    }

    public void setEgfr(double egfr) {
        this.egfr = egfr;
    }

    public double getAcr() {
        return acr;
    }

    public void setAcr(double acr) {
        this.acr = acr;
    }

    public double getRisk2Yr() {
        return risk2Yr;
    }

    public void setRisk2Yr(double risk2Yr) {
        this.risk2Yr = risk2Yr;
    }

    public double getRisk5Yr() {
        return risk5Yr;
    }

    public void setRisk5Yr(double risk5Yr) {
        this.risk5Yr = risk5Yr;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
