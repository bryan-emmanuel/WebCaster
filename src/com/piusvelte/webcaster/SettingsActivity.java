package com.piusvelte.webcaster;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class SettingsActivity extends FragmentActivity implements
		OnClickListener {

	private static final String TAG = "SettingsActivity";

	public static final String EXTRA_APP_ID = "com.piusvelte.webcaster.APP_ID";
	public static final String EXTRA_HOST = "com.piusvelte.webcaster.HOST";

	private EditText appIdView;
	private EditText hostView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);

		appIdView = (EditText) findViewById(R.id.setting_app_id);
		hostView = (EditText) findViewById(R.id.setting_host);

		Intent intent = getIntent();

		if (intent != null) {
			appIdView.setText(intent.getStringExtra(EXTRA_APP_ID));
			hostView.setText(intent.getStringExtra(EXTRA_HOST));
		}

		Button submit = (Button) findViewById(R.id.button_submit);
		submit.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.button_submit) {
			setResult(
					RESULT_OK,
					new Intent().putExtra(EXTRA_APP_ID,
							appIdView.getText().toString()).putExtra(
							EXTRA_HOST, hostView.getText().toString()));
			finish();
		}
	}

}
