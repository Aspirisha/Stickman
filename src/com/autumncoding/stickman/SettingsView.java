package com.autumncoding.stickman;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.autamncoding.stickman.R;

public class SettingsView extends LinearLayout {
	private SeekBar m_fpsSeekBar = null;
	private Spinner m_languageSpinner = null;
	private TextView m_fpsText = null;
	
	public SettingsView(Context context) {
		super(context);
		inflate(context, R.layout.settings_layout, this);
		setBackgroundResource(R.drawable.background);
	
		m_fpsSeekBar = (SeekBar)findViewById(R.id.animation_fps_seekbar);
		m_languageSpinner = (Spinner)findViewById(R.id.language_spinner);
		m_fpsText = (TextView)findViewById(R.id.fps_text);
		
		m_fpsText.setText("Current fps: " + String.valueOf(m_fpsSeekBar.getProgress() + 1));
		
		m_fpsSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				Animation.getInstance().setAnimationFPS(seekBar.getProgress());
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				m_fpsText.setText("Current fps: " + String.valueOf(progress + 1));
			}
		});
	}
	
	
}
