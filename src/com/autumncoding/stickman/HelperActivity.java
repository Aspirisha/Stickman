package com.autumncoding.stickman;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import com.autamncoding.stickman.R;

public class HelperActivity extends Activity implements OnClickListener {
	private HelpView m_helpView = null;
	private boolean m_exitOnBack = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		m_helpView = new HelpView(this);
		setContentView(m_helpView);
		m_helpView.setMyClickListener(this);
	}

	@Override
	public void onClick(View v) {
		m_exitOnBack = false;
		switch (v.getId()) {
		case R.id.helpbtn1:
			setContentView(R.layout.about_layout);
			break;
		case R.id.helpbtn2:
			setContentView(R.layout.help_draw_layout);
			break;
		case R.id.helpbtn3:
			setContentView(R.layout.help_moving_layout);
			break;
		case R.id.helpbtn4:
			setContentView(R.layout.help_connection_layout);
			break;
		case R.id.helpbtn5:
			setContentView(R.layout.help_frames_layout);
			break;
		case R.id.helpbtn6:
			setContentView(R.layout.help_animation_layout);
			break;
		case R.id.helpbtn7:
			setContentView(R.layout.help_save_layout);
			break;
		default:
			m_exitOnBack = true; // TODO not sure
		}
		
	}
	
	@Override
	public void onBackPressed() {
		if (!m_exitOnBack) {
			setContentView(m_helpView);
			m_exitOnBack = true;
		} else {
			Intent myIntent = new Intent(getBaseContext(), MainActivity.class);
			myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(myIntent);
		}
		
	}
	
}
