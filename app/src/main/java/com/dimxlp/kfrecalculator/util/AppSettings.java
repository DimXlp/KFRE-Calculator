package com.dimxlp.kfrecalculator.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class AppSettings {

    // --- Preference Keys ---
    public static final String KEY_DARK_MODE = "dark_mode_enabled";
    public static final String KEY_DATE_FORMAT = "date_format";
    public static final String KEY_LANGUAGE = "app_language";
    public static final String KEY_ASSESSMENT_FREQUENCY = "assessment_frequency";
    public static final String KEY_RECENT_PATIENTS_PERIOD = "recent_patients_period";
    public static final String KEY_AUTO_EXPORT = "auto_export_enabled";

    private AppSettings() {}

    private static SharedPreferences.Editor getEditor(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).edit();
    }

    private static SharedPreferences getPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static void setDarkModeEnabled(Context context, boolean isEnabled) {
        getEditor(context).putBoolean(KEY_DARK_MODE, isEnabled).apply();
    }
    public static boolean isDarkModeEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_DARK_MODE, false);
    }

    public static void setDateFormat(Context context, String dateFormat) {
        getEditor(context).putString(KEY_DATE_FORMAT, dateFormat).apply();
    }
    public static String getDateFormat(Context context) {
        return getPrefs(context).getString(KEY_DATE_FORMAT, "dd/MM/yyyy");
    }

    public static void setLanguage(Context context, String language) {
        getEditor(context).putString(KEY_LANGUAGE, language).apply();
    }
    public static String getLanguage(Context context) {
        return getPrefs(context).getString(KEY_LANGUAGE, "English");
    }

    public static void setAssessmentFrequency(Context context, String frequency) {
        getEditor(context).putString(KEY_ASSESSMENT_FREQUENCY, frequency).apply();
    }
    public static String getAssessmentFrequency(Context context) {
        return getPrefs(context).getString(KEY_ASSESSMENT_FREQUENCY, "6 months");
    }

    public static void setRecentPatientsPeriod(Context context, int days) {
        getEditor(context).putInt(KEY_RECENT_PATIENTS_PERIOD, days).apply();
    }
    public static int getRecentPatientsPeriod(Context context) {
        return getPrefs(context).getInt(KEY_RECENT_PATIENTS_PERIOD, 30);
    }

    public static void setAutoExportEnabled(Context context, boolean isEnabled) {
        getEditor(context).putBoolean(KEY_AUTO_EXPORT, isEnabled).apply();
    }
    public static boolean isAutoExportEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_AUTO_EXPORT, false);
    }
}