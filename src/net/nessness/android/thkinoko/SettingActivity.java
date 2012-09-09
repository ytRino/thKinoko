package net.nessness.android.thkinoko;

import android.preference.PreferenceActivity;
//import android.content.Context;
//import android.content.SharedPreferences;
import android.os.Bundle;

public class SettingActivity extends PreferenceActivity {

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.knk_pref);
	}
}
