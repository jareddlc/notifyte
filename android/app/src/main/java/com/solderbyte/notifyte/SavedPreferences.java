package com.solderbyte.notifyte;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;


public class SavedPreferences {
    private static final String LOG_TAG = "Notifyte:Preferences";

    private SharedPreferences preferences = null;
    private Editor editor = null;

    public static final String STRING_DEFAULT = "default";
    public static final boolean BOOEAN_DEFAULT = false;
    public static final int INT_DEFAULT = 0;

    public static String DEVICE_NAME = "deviceName";
    public static String DEVICE_ADDR = "deviceAddress";

    public SavedPreferences() {}

    public void init(Context context) {
        Log.d(LOG_TAG, "init");
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        editor = preferences.edit();
    }

    public boolean getBoolean(String key) {
        boolean value = preferences.getBoolean(key + ":boolean", BOOEAN_DEFAULT);
        return value;
    }

    public int getInt(String key) {
        return preferences.getInt(key + ":int", INT_DEFAULT);
    }

    public String getString(String key) {
        String value = preferences.getString(key + ":string", STRING_DEFAULT);
        return value;
    }

    public void saveBoolean(String key, boolean value) {
        editor.putBoolean(key + ":boolean", value);
        editor.commit();
    }

    public void saveInt(String key, int value) {
        editor.putInt(key + ":int", value);
        editor.commit();
    }

    public void saveString(String key, String value) {
        editor.putString(key + ":string", value);
        editor.commit();
    }
}
