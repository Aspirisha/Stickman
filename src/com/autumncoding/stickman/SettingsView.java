package com.autumncoding.stickman;

import java.util.Locale;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.autamncoding.stickman.R;

public class SettingsView extends LinearLayout {
	private SeekBar m_fpsSeekBar = null;
	private Spinner m_languageSpinner = null;
	private TextView m_fpsText = null;
	private TextView m_header = null;
	private TextView m_animFpsLabel = null;
	private TextView m_langSpinLabel = null;
	private CheckBox m_saveBox = null;
	
	private String curFpsText = getResources().getString(R.string.current_fps_text);
	
	public SettingsView(Context context) {
		super(context);
		inflate(context, R.layout.settings_layout, this);
		setBackgroundResource(R.drawable.background);
	
		m_fpsSeekBar = (SeekBar)findViewById(R.id.animation_fps_seekbar);
		m_languageSpinner = (Spinner)findViewById(R.id.language_spinner);
		m_fpsText = (TextView)findViewById(R.id.fps_text);
		m_header = (TextView)findViewById(R.id.textView1);
		m_animFpsLabel = (TextView)findViewById(R.id.animation_fps_label);
		m_langSpinLabel = (TextView)findViewById(R.id.language_spinner_label);
		m_saveBox = (CheckBox)findViewById(R.id.checkBox1);
		
		m_saveBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				GameData.saveToTemp = isChecked;
			}
		});
		
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
				m_fpsText.setText(curFpsText + String.valueOf(progress + 1));
			}
		});
		
		
		m_languageSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				String text = parent.getSelectedItem().toString();
				onLanguageChange(text, position);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
				
			}
		});
		
		updateSettings();
	}
	
	private void onLanguageChange(String newLanguage, int position) {
		Resources res = getResources();
		//String ar[] = res.getStringArray(R.array.languages_array);
		String codes[] = res.getStringArray(R.array.lang_codes);
		String code = codes[position];
		// Change locale settings in the app.

		DisplayMetrics dm = res.getDisplayMetrics();

		android.content.res.Configuration conf = res.getConfiguration();

		conf.locale = new Locale(code);
		res.updateConfiguration(conf, null);
		GameData.lang = newLanguage;
		GameData.mainActivity.updateTexts();
	}
	
	void UpdateTexts() {
		m_header.setText(GameData.res.getString(R.string.settings_header));
		m_animFpsLabel.setText(GameData.res.getString(R.string.str_anim_fps));
		m_langSpinLabel.setText(GameData.res.getString(R.string.str_lang_spinner));
		curFpsText = getResources().getString(R.string.current_fps_text);
		m_saveBox.setText(GameData.res.getString(R.string.save_or_not));
		m_fpsText.setText(curFpsText + String.valueOf(m_fpsSeekBar.getProgress() + 1));
	}
	
	void updateSettings() {
		m_saveBox.setChecked(GameData.saveToTemp);
	    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(), R.array.languages_array, android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    m_languageSpinner.setAdapter(adapter);
	    if (!GameData.lang.equals(null)) {
	        int spinnerPosition = adapter.getPosition(GameData.lang);
	        m_languageSpinner.setSelection(spinnerPosition);
	        onLanguageChange(GameData.lang, spinnerPosition);
	    }
	}
}
