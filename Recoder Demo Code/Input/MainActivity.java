package com.example.settagtest;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		TextView tv = new TextView(this);
        ArrayList x = new ArrayList();
        x.add(tv);
        
        // bug due to this line
        tv.setTag(R.id.abc, tv);
		
		setContentView(R.layout.activity_main);
	}
}
