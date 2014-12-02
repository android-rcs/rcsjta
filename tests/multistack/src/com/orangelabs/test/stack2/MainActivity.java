package com.orangelabs.test.stack2;

import android.app.Activity;
import android.os.Bundle;
import android.widget.CheckBox;

public class MainActivity extends Activity {
	public static CheckBox statusCheckbox = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
	}
}
