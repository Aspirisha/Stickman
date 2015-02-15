package com.autumncoding.stickman;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.autamncoding.stickman.R;
import com.scorchworks.demo.SimpleFileDialog;

enum ViewType {
	VIEW_INTRO,
	VIEW_GAME,
	VIEW_SETTINGS,
	NONE
}

public class MainActivity extends Activity {
	static int layout_height;
	static int layout_width;
	ViewType m_viewCur = ViewType.NONE;
	AppIntro m_app;
	private ViewIntro m_viewIntro = null;
	private GameView m_gameView = null;
	private SettingsView m_settingsView = null;
	private FrameLayout m_gameLayout = null;
    private LinearLayout m_gameWidgetsLayout = null;
    private SeekBar m_frameSeekBar = null;
    private TextView m_framesInfo = null;
    private Menu m_menu = null;
    
	@Override 
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
        m_app = new AppIntro(this, AppIntro.LANGUAGE_ENG);
        setView(ViewType.VIEW_INTRO);
        m_gameLayout = new FrameLayout(this);
        m_gameWidgetsLayout = new LinearLayout(this);
        GameData.init(this);
        Animation.getInstance().setAnimationFPS(getResources().getInteger(R.integer.default_anim_fps));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		m_menu = menu;
		return true;
	}
	
	private void initGameView() {
		m_gameView = new GameView(this);
		m_framesInfo = new TextView(this);
    	m_frameSeekBar = new SeekBar(this);
    	m_frameSeekBar.setMax(0);
    	m_frameSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				Animation.getInstance().setCurrentframe(seekBar.getProgress());
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				GameData.currentFrameIndex = progress + 1;
			}
		});
    	
    	m_gameWidgetsLayout.setOrientation(LinearLayout.VERTICAL);
    	m_gameWidgetsLayout.addView(m_frameSeekBar);
    	m_gameWidgetsLayout.addView(m_framesInfo);
    	m_framesInfo.setGravity(Gravity.RIGHT);
    	m_framesInfo.setTextColor(getResources().getColor(R.color.label_color));
    	m_gameLayout.addView(m_gameView);
    	m_gameLayout.addView(m_gameWidgetsLayout);
    	
    	setContentView(m_gameLayout);
    	
		m_gameView.setBackgroundResource(R.drawable.background);
	}
	
	private void initSettingsView() {
		m_settingsView = new SettingsView(this);
	}
	
	void UpdateFramesInfo(int cur, int max) {
		m_framesInfo.setText(cur + "/" + max);
	}
	
	public void CountMetrics() {
		Rect rect = new Rect(); 
		Window win = GameData.mainActivity.getWindow();  // Get the Window
		win.getDecorView().getWindowVisibleDisplayFrame(rect); 
		// Get the height of Status Bar 
		int statusBarHeight = rect.top; 
		// Get the height occupied by the decoration contents 
		int contentViewTop = win.findViewById(Window.ID_ANDROID_CONTENT).getTop(); 
		// Calculate titleBarHeight by deducting statusBarHeight from contentViewTop  
		int titleBarHeight = contentViewTop - statusBarHeight; 

		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);   
		int screenHeight = metrics.heightPixels;
		int screenWidth = metrics.widthPixels;

		// Now calculate the height that our layout can be set
		// If you know that your application doesn't have statusBar added, then don't add here also. Same applies to application bar also 
		layout_height = screenHeight - (titleBarHeight + statusBarHeight);
		layout_width = screenWidth;
		Log.i("MY", "Layout Height = " + layout_height);   
		GameData.setMetrics();
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 
				LinearLayout.LayoutParams.WRAP_CONTENT);
		lp.setMargins(getResources().getDimensionPixelSize(R.dimen.frames_seek_bar_left_padding), 
				GameData.topMenuHeight + m_frameSeekBar.getHeight() / 2,
				getResources().getDimensionPixelSize(R.dimen.frames_seek_bar_right_padding), 0); //getResources().getDimensionPixelSize(R.dimen.frames_seek_bar_top_padding
		m_frameSeekBar.setLayoutParams(lp);
		lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 
				LinearLayout.LayoutParams.WRAP_CONTENT);
		m_framesInfo.setPadding(0, 0, getResources().getDimensionPixelSize(R.dimen.frames_counter_right_padding), 0);
	}
	
	public void setFramesSeekbarRange(int framesNumber) {
		if (framesNumber > 0)
			m_frameSeekBar.setMax(framesNumber - 1);
	}
	
	public void onCurrentframeChanged(int currentFrameIndex) {
		if (currentFrameIndex >= 0 && currentFrameIndex <= m_frameSeekBar.getMax())
			m_frameSeekBar.setProgress(currentFrameIndex);
	}
	
	public void setView(ViewType viewType)
	{
		if (m_viewCur == viewType) {
			return;
		}
		m_viewCur = viewType;
		
		switch (viewType) {
		case VIEW_GAME:
			if (m_gameView == null) 
				initGameView();

			setContentView(m_gameLayout);
			break;
		case VIEW_INTRO:
			m_viewIntro = new ViewIntro(this);
	        setContentView(m_viewIntro);
	        m_viewIntro.start();
			break;
		case VIEW_SETTINGS:
			if (m_settingsView == null)
				initSettingsView();
			Animation.getInstance().stopAnimation();
			setContentView(m_settingsView);
			break;
		default:
			break;
		
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);

	    getBaseContext().getResources().updateConfiguration(newConfig, getBaseContext().getResources().getDisplayMetrics());
	    //setContentView(R.layout.main);
	    setTitle(R.string.app_name);
	}
	
	public AppIntro getApp() {
		return m_app;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
		
	    switch (item.getItemId()) {
	    	case R.id.action_settings:
	    		setView(ViewType.VIEW_SETTINGS);
	    		return true;
	        case R.id.action_save:
	        	synchronized (GameData.getLocker()) {
	        		SimpleFileDialog FileOpenDialog =  new SimpleFileDialog(MainActivity.this, "FileSave",
	        				new SimpleFileDialog.SimpleFileDialogListener()
	        		{
	        			@Override
	        			public void onChosenDir(String chosenDir) 
	        			{
	        				// The code in this function will be executed when the dialog OK button is pushed 
	        				//m_chosen = chosenDir;
	        				Animation.getInstance().SaveToFile(chosenDir);
	        				Toast.makeText(MainActivity.this, getResources().getString(R.string.file_saved_to) + 
	        						chosenDir, Toast.LENGTH_LONG).show();
	        			}
	        		});
	        		
	        		//You can change the default filename using the public variable "Default_File_Name"
	        		File file = new File(GameData.mainActivity.getExternalFilesDir(null), "StickmanSaves");
	     		    file.mkdirs();
	        		FileOpenDialog.chooseFile_or_Dir(file.getAbsolutePath());
	        		FileOpenDialog.Default_File_Name = "";

				}
	            return true;
	        case R.id.action_load:	        		
	        	SimpleFileDialog FileOpenDialog =  new SimpleFileDialog(MainActivity.this, "FileOpen",
	        			new SimpleFileDialog.SimpleFileDialogListener()
	        	{
	        		@Override
	        		public void onChosenDir(String chosenDir) 
	        		{
	        			// The code in this function will be executed when the dialog OK button is pushed 
	        			//m_chosen = chosenDir;
	        			synchronized (GameData.getLocker()) {
	        				Animation.getInstance().loadFromFile(chosenDir);
	        			}
	        			Toast.makeText(MainActivity.this, getResources().getString(R.string.file_opened_from) + 
	        					chosenDir, Toast.LENGTH_LONG).show();
	        			m_gameView.updateUpperMenu();
	        		}
	        	});

	        	//You can change the default filename using the public variable "Default_File_Name"
	        	File file = new File(GameData.mainActivity.getExternalFilesDir(null), "StickmanSaves");
	        	file.mkdirs();
	        	FileOpenDialog.chooseFile_or_Dir(file.getAbsolutePath());
	        	FileOpenDialog.Default_File_Name = "";
	        		
	            return true;
	        case R.id.action_help:
	        	startActivity(new Intent(this, HelperActivity.class));
	        	return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
	public void onBackPressed() {
		switch (m_viewCur) {
		case NONE:
			break;
		case VIEW_GAME:
			finish();
			break;
		case VIEW_INTRO:
			break;
		case VIEW_SETTINGS:
			setView(ViewType.VIEW_GAME);
			break;
		default:
			break;
		
		}
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}
	
	public void UpdateTexts() {
		if (m_menu == null)
			return;
		
		m_menu.getItem(0).setTitle(GameData.res.getString(R.string.action_settings));
		m_menu.getItem(1).setTitle(GameData.res.getString(R.string.str_load));
		m_menu.getItem(2).setTitle(GameData.res.getString(R.string.str_save));
		
		m_settingsView.UpdateTexts();
	}

}
