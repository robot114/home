package com.zsm.home.preferences;

import com.zsm.home.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class MainPreferencesActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.std_preferences);
	}
}
