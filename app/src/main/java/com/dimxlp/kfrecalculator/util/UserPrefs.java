package com.dimxlp.kfrecalculator.util;

import android.content.Context;
import android.content.SharedPreferences;

public final class UserPrefs {
    private static final String P = "user_prefs";
    private static final String K_FIRST="first", K_LAST="last", K_EMAIL="email",
            K_ROLE="role", K_CLINIC="clinic";

    private UserPrefs() {}

    public static void save(Context c, String first, String last, String email, String role, String clinic) {
        SharedPreferences sp = c.getSharedPreferences(P, Context.MODE_PRIVATE);
        sp.edit()
                .putString(K_FIRST, first)
                .putString(K_LAST, last)
                .putString(K_EMAIL, email)
                .putString(K_ROLE, role)
                .putString(K_CLINIC, clinic)
                .apply();
    }

    public static void clear(Context c) {
        c.getSharedPreferences(P, Context.MODE_PRIVATE).edit().clear().apply();
    }

    public static String first(Context c){ return c.getSharedPreferences(P, Context.MODE_PRIVATE).getString(K_FIRST, null); }
    public static String last(Context c){ return c.getSharedPreferences(P, Context.MODE_PRIVATE).getString(K_LAST, null); }
    public static String email(Context c){ return c.getSharedPreferences(P, Context.MODE_PRIVATE).getString(K_EMAIL, null); }
    public static String role(Context c){ return c.getSharedPreferences(P, Context.MODE_PRIVATE).getString(K_ROLE, null); }
    public static String clinic(Context c){ return c.getSharedPreferences(P, Context.MODE_PRIVATE).getString(K_CLINIC, null); }
}
