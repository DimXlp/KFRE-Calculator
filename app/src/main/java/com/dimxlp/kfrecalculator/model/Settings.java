package com.dimxlp.kfrecalculator.model;

public class Settings {
    private String userId;
    private boolean autosave;
    private boolean notifications;
    private String exportFormat;
    private boolean darkMode;

    public Settings() {}

    public Settings(String userId, boolean autosave, boolean notifications, String exportFormat, boolean darkMode) {
        this.userId = userId;
        this.autosave = autosave;
        this.notifications = notifications;
        this.exportFormat = exportFormat;
        this.darkMode = darkMode;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isAutosave() {
        return autosave;
    }

    public void setAutosave(boolean autosave) {
        this.autosave = autosave;
    }

    public boolean isNotifications() {
        return notifications;
    }

    public void setNotifications(boolean notifications) {
        this.notifications = notifications;
    }

    public String getExportFormat() {
        return exportFormat;
    }

    public void setExportFormat(String exportFormat) {
        this.exportFormat = exportFormat;
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }
}
