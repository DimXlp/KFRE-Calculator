package com.dimxlp.kfrecalculator.model;

public class Report {
    private String reportId;
    private String userId;
    private String patientId;
    private long generatedAt;
    private String fileUrl;

    public Report() {}

    public Report(String reportId, String userId, String patientId, long generatedAt, String fileUrl) {
        this.reportId = reportId;
        this.userId = userId;
        this.patientId = patientId;
        this.generatedAt = generatedAt;
        this.fileUrl = fileUrl;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public long getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(long generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
}
