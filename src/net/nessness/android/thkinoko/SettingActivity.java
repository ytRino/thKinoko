package net.nessness.android.thkinoko;

import android.os.Bundle;
import android.preference.PreferenceActivity;

//import android.content.Context;
//import android.content.SharedPreferences;

public class SettingActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.knk_pref);
    }
}
