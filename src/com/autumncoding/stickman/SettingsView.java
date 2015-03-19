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
	private SeekBar m_maxObjSeekBar = null;
	private Spinner m_languageSpinner = null;
	private TextView m_fpsText = null;
	private TextView m_objsNumberText = null;
	private TextView m_header = null;
	private TextView m_animFpsLabel = null;
	private TextView m_langSpinLabel = null;
	private TextView m_maxNumberOfObjsLabel = null;
	private CheckBox m_saveBox = null;
	private CheckBox m_interpBox = null;
	private CheckBox m_loopBox = null;
	private CheckBox m_hintsBox = null;
	
	public SettingsView(Context context) {
		super(context);
		inflate(context, R.layout.settings_layout, this);
		setBackgroundResource(R.drawable.background);
	
		m_fpsSeekBar = (SeekBar)findViewById(R.id.animation_fps_seekbar);
		m_maxObjSeekBar = (SeekBar)findViewById(R.id.seekbar2);
		m_languageSpinner = (Spinner)findViewById(R.id.language_spinner);
		m_fpsText = (TextView)findViewById(R.id.fps_text);
		m_header = (TextView)findViewById(R.id.textView1);
		m_animFpsLabel = (TextView)findViewById(R.id.animation_fps_label);
		m_langSpinLabel = (TextView)findViewById(R.id.language_spinner_label);
		m_saveBox = (CheckBox)findViewById(R.id.checkBox1);
		m_interpBox = (CheckBox)findViewById(R.id.checkBox2);
		m_loopBox = (CheckBox)findViewById(R.id.checkBox3);
		m_hintsBox = (CheckBox)findViewById(R.id.checkBox4);
		m_maxNumberOfObjsLabel = (TextView)findViewById(R.id.max_objects_label);
		m_objsNumberText = (TextView)findViewById(R.id.maxObjs);
		
		m_maxObjSeekBar.setMax(GameData.maxPrimitivesNumber - 1);
		CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				switch (buttonView.getId()) {
				case R.id.checkBox1:
					GameData.saveToTemp = isChecked;
					break;
				case R.id.checkBox2:
					GameData.enableInterpolation = isChecked;
					break;
				case R.id.checkBox3:
					GameData.playInLoop = isChecked;
					break;
				case R.id.checkBox4:
					GameData.showPopupHints = isChecked;
					break;
				}
			}
			
		};
		
		m_saveBox.setOnCheckedChangeListener(listener);
		m_interpBox.setOnCheckedChangeListener(listener);
		m_loopBox.setOnCheckedChangeListener(listener);
		m_hintsBox.setOnCheckedChangeListener(listener);
		
		SeekBar.OnSeekBarChangeListener seekbarListener = new SeekBar.OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				switch (seekBar.getId()) {
				case R.id.animation_fps_seekbar:
					Animation.getInstance().setAnimationFPS(seekBar.getProgress() + 1);
					break;
				case R.id.seekbar2:
					GameData.maxPrimitivesNumber = seekBar.getProgress() + 1;
					break;
				}
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				switch (seekBar.getId()) {
				case R.id.animation_fps_seekbar:
					m_fpsText.setText(String.valueOf(progress + 1));
					break;
				case R.id.seekbar2:
					m_objsNumberText.setText(String.valueOf(progress + 1));
					break;
				}
			}
		};
		
		m_fpsSeekBar.setOnSeekBarChangeListener(seekbarListener);
		m_maxObjSeekBar.setOnSeekBarChangeListener(seekbarListener);
		
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
		m_saveBox.setText(GameData.res.getString(R.string.save_or_not));
		m_interpBox.setText(GameData.res.getString(R.string.enable_interpolation));
		m_loopBox.setText(GameData.res.getString(R.string.loop_animation));
		m_fpsText.setText(String.valueOf(m_fpsSeekBar.getProgress() + 1));
		m_hintsBox.setText(GameData.res.getString(R.string.show_hints));
		m_maxNumberOfObjsLabel.setText(GameData.res.getString(R.string.max_number_of_objects));
	}
	
	void updateSettings() {
		m_saveBox.setChecked(GameData.saveToTemp);
		m_loopBox.setChecked(GameData.playInLoop);
		m_interpBox.setChecked(GameData.enableInterpolation);
		m_hintsBox.setChecked(GameData.showPopupHints);
		m_fpsText.setText(String.valueOf(Animation.getInstance().getFps()));
		
		m_fpsSeekBar.setMax(GameData.maxAnimationFps - 1);
		m_fpsSeekBar.setProgress(Animation.getInstance().getFps() - 1);
		
		m_maxObjSeekBar.setMax(GameData.absoluteMaxOfObjects - 1);
		m_maxObjSeekBar.setProgress(GameData.maxPrimitivesNumber - 1);
		
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
