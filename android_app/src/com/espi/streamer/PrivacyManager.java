package com.espi.streamer;

import android.content.Context;
import android.content.SharedPreferences;

public class PrivacyManager {
    private static final String PREF = "privacy";
    private static final String CONSENT = "consent";
    private static final String REMOTE = "remote_control";

    private final SharedPreferences prefs;

    public PrivacyManager(Context context) {
        prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void setConsent(boolean value) {
        prefs.edit().putBoolean(CONSENT, value).apply();
    }

    public boolean hasConsent() {
        return prefs.getBoolean(CONSENT, false);
    }

    public void setRemoteControlEnabled(boolean value) {
        prefs.edit().putBoolean(REMOTE, value).apply();
    }

    public boolean isRemoteControlEnabled() {
        return prefs.getBoolean(REMOTE, false);
    }
}
