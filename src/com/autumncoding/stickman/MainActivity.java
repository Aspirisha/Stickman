package com.autumncoding.stickman;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.autamncoding.stickman.R;
import com.autumncoding.stickman.TouchEventThread.TouchState;
import com.scorchworks.demo.SimpleFileDialog;

enum ViewType {
	VIEW_INTRO,
	VIEW_GAME,
	VIEW_SETTINGS,
	NONE
}

public class MainActivity extends Activity {
	public static final String PREFS_NAME = "MyPrefsFile";
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
    private boolean m_settingsRead = false;
    
    // store helper toasts objects and timer just to be able to cancel them
    private LinkedList<Toast> m_toasts = null;
    private LinkedList<CountDownTimer> m_toastTimers = null;
    
	@Override 
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		GameData.init(this);
		readSettings();
		if (m_settingsView == null) // explicitly because we need language for hints
			initSettingsView();
        m_app = new AppIntro(this, AppIntro.LANGUAGE_ENG);
        setView(ViewType.VIEW_INTRO);
        m_gameLayout = new FrameLayout(this);
        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        m_gameWidgetsLayout = (LinearLayout) inflater.inflate(R.layout.frames_layout, null);
        
        m_toasts = new LinkedList<Toast>();
        m_toastTimers = new LinkedList<CountDownTimer>();
       
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		m_menu = menu;
		m_settingsView.updateSettings();
		return true;
	}
	
	private void initGameView() {
		m_gameView = new GameView(this);
		m_gameLayout.addView(m_gameView);
    	m_gameLayout.addView(m_gameWidgetsLayout);
		m_framesInfo = (TextView)m_gameWidgetsLayout.getChildAt(1);
    	m_frameSeekBar = (SeekBar) m_gameWidgetsLayout.getChildAt(0);
    	
    	m_framesInfo.setTextColor(getResources().getColor(R.color.label_color));
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
    	    	
		m_gameView.setBackgroundResource(R.drawable.background);
		
		if (GameData.showPopupHints) {
			LinkedList<String> messages = new LinkedList<String>();
			messages.push(GameData.res.getString(R.string.toast_help1));
			messages.push(GameData.res.getString(R.string.toast_help19));
			messages.push(GameData.res.getString(R.string.toast_help18));
			
			OnTouchListener l = new OnTouchListener() {
				
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					 if(m_gameView != null){
	                        //Set the touch location to the absolute screen location
	                        event.setLocation(event.getRawX(), event.getRawY());
	                        //Send the touch event to the view
	                        return m_gameView.onTouchEvent(event);
	                    } 
					return false;
				}
			};
			
			for (String msg : messages) {
				final Toast toast = Toast.makeText(GameData.mainActivity, msg, Toast.LENGTH_LONG);
				final View toastView = toast.getView();
				toastView.setOnTouchListener(l);
				m_toasts.push(toast);
				CountDownTimer toastTimer = new CountDownTimer(GameData.helpToastDurationPerLetter * msg.length(), 1000) {
				    public void onTick(long millisUntilFinished) {
				    	toast.show();
				    }
				    public void onFinish() {
				    	toast.show();
				    	m_toasts.remove(toast);
				    	m_toastTimers.remove(this);
				    	if (!m_toastTimers.isEmpty())
				    		m_toastTimers.get(0).start();
				 
				    }
				    
				};
				m_toastTimers.push(toastTimer);
				
			}
			m_toastTimers.get(0).start();
		}
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
	}
	
	public void setFramesSeekbarRange(int framesNumber) {
		if (framesNumber > 0)
			m_frameSeekBar.setMax(framesNumber - 1);
	}
	
	public void onCurrentframeChanged(int currentFrameIndex) {
		if (currentFrameIndex >= 0 && currentFrameIndex <= m_frameSeekBar.getMax())
			m_frameSeekBar.setProgress(currentFrameIndex);
	}
	
	public void setView(ViewType viewType) {
		if (m_viewCur == viewType) {
			return;
		}
		m_viewCur = viewType;
		
		switch (viewType) {
		case VIEW_GAME:
			if (m_settingsView == null) // explicitly because we need language for hints
				initSettingsView();
			else
				m_settingsView.updateSettings();
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
			for (CountDownTimer t : m_toastTimers)
				t.cancel();
			for (Toast t : m_toasts)
				t.cancel();
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
	
	public void openMenu() {
		this.openOptionsMenu();
	}
	
	private void onSavePressed() {
		synchronized (GameData.getLocker()) {
    		SimpleFileDialog FileOpenDialog =  new SimpleFileDialog(MainActivity.this, "FileSave",
    				new SimpleFileDialog.SimpleFileDialogListener()
    		{
    			@Override
    			public void onChosenDir(String chosenDir) {
    				// The code in this function will be executed when the dialog OK button is pushed 
    				//m_chosen = chosenDir;
    				Animation.getInstance().SaveToFile(chosenDir, false);
    				Toast.makeText(MainActivity.this, getResources().getString(R.string.file_saved_to) + 
    						chosenDir, Toast.LENGTH_LONG).show();
    			}

    		});
    		
    		//You can change the default filename using the public variable "Default_File_Name"
    		File file = new File(GameData.mainActivity.getExternalFilesDir(null) + GameData.animFolder);
 		    file.mkdirs();
    		FileOpenDialog.chooseFile_or_Dir(file.getAbsolutePath());
    		FileOpenDialog.Default_File_Name = "";

		}
	}
	
	private void onLoadPressed() {
		SimpleFileDialog FileOpenDialog =  new SimpleFileDialog(MainActivity.this, "FileOpen",
    			new SimpleFileDialog.SimpleFileDialogListener()
    	{
    		@Override
    		public void onChosenDir(String chosenDir) {
    			// The code in this function will be executed when the dialog OK button is pushed 
    			//m_chosen = chosenDir;
    			synchronized (GameData.getLocker()) {
    				try {
    					Animation.getInstance().loadFromFile(chosenDir, false);
    				} catch (IOException e) {
    					e.printStackTrace();
    					Animation.getInstance().clear();
    				} catch (ClassNotFoundException e) {
    					e.printStackTrace();
    					Animation.getInstance().clear();
    				}
    			}
    			Toast.makeText(MainActivity.this, getResources().getString(R.string.file_opened_from) + 
    					chosenDir, Toast.LENGTH_LONG).show();
    			m_gameView.updateUpperMenu();
    		}
    	});

    	//You can change the default filename using the public variable "Default_File_Name"
    	File file = new File(GameData.mainActivity.getExternalFilesDir(null) + GameData.animFolder);
    	file.mkdirs();
    	FileOpenDialog.chooseFile_or_Dir(file.getAbsolutePath());
    	FileOpenDialog.Default_File_Name = "";
	}
	
	private void onNewAnimationPressed() {
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
    	    @Override
    	    public void onClick(DialogInterface dialog, int which) {
    	        switch (which){
    	        case DialogInterface.BUTTON_POSITIVE:
    	            onSavePressed();
    	            break;

    	        case DialogInterface.BUTTON_NEGATIVE:
    	            break;
    	        }
    	        
    	        Animation.getInstance().clear();
    	    }
    	    
    	};
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage(getResources().getString(R.string.save_before_new)).setPositiveButton(getResources().getString(R.string.yes), dialogClickListener)
    	    .setNegativeButton(getResources().getString(R.string.no), dialogClickListener).show();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
		
	    switch (item.getItemId()) {
	    	case R.id.action_settings:
	    		setView(ViewType.VIEW_SETTINGS);
	    		return true;
	        case R.id.action_save:
	        	onSavePressed();
	            return true;
	        case R.id.action_load:	        		
	        	onLoadPressed();
	            return true;
	        case R.id.action_help:
	        	startActivity(new Intent(this, HelperActivity.class));
	        	return true;
	        case R.id.action_new_animation:
	        	onNewAnimationPressed();
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
			m_gameView.stopToasts();
		case VIEW_INTRO:
			for (CountDownTimer t : m_toastTimers)
				t.cancel();
			for (Toast t : m_toasts)
				t.cancel();
			finish();
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
		Animation.getInstance().Play(false);
		m_settingsRead = false;
		if (GameData.saveToTemp)
			Animation.getInstance().SaveToFile("", true);
		
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
	    editor.putBoolean("SaveTemp", GameData.saveToTemp);
	    editor.putString("Lang", GameData.lang);
	    editor.putBoolean("EnableInterp", GameData.enableInterpolation);
	    editor.putBoolean("PlayInLoop", GameData.playInLoop);
	    editor.putBoolean("popupHints", GameData.showPopupHints);
	    editor.putInt("fps", Animation.getInstance().getFps());

	    editor.commit();
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		if (!m_settingsRead)
			readSettings();
		super.onResume();
	}
	
	private void readSettings() {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	    GameData.saveToTemp = settings.getBoolean("SaveTemp", true);
	    GameData.lang = settings.getString("Lang", "NONE");
	    GameData.enableInterpolation = settings.getBoolean("EnableInterp", true);
	    GameData.playInLoop = settings.getBoolean("PlayInLoop", false);
	    GameData.showPopupHints = settings.getBoolean("popupHints", true);
	    GameData.touchState = TouchState.DRAWING;	    
	    Animation.getInstance().setAnimationFPS(settings.getInt("fps", 2));
	    
	    if (GameData.lang == "NONE") {
	    	String codes[] = getResources().getStringArray(R.array.lang_codes);
	    	String langNames[] = getResources().getStringArray(R.array.languages_array);
	    	int pos = Arrays.asList(codes).indexOf(Locale.getDefault().getLanguage());
	    	
	    	if (pos != -1) 
	    		GameData.lang = langNames[pos];
	    	else
	    		GameData.lang = "English";
	    }
	    	
	    //TODO ucomment it later, it's always copying now for imiatating first install
	    if (!settings.getBoolean("assetsAreCopied", false)) {
	    	(new AsyncTask<Void, Void, Void>() {

				@Override
				protected Void doInBackground(Void... params) {
					copyAssets();
					return null;
				}
			}).execute();
	    }
	    	
	    m_settingsRead = true;
	}
	
	public void updateTexts() {
		if (m_menu == null)
			return;
		
		m_menu.getItem(0).setTitle(GameData.res.getString(R.string.action_settings));
		m_menu.getItem(1).setTitle(GameData.res.getString(R.string.str_help));
		m_menu.getItem(2).setTitle(GameData.res.getString(R.string.str_new_anim));
		m_menu.getItem(3).setTitle(GameData.res.getString(R.string.str_save));
		m_menu.getItem(4).setTitle(GameData.res.getString(R.string.str_load));
		m_settingsView.UpdateTexts();
	}

	
	private void copyAssets() {
		AssetManager assetManager = getAssets();
		String[] files = null;
		try {
			files = assetManager.list("");
			File outFile = new File( GameData.mainActivity.getExternalFilesDir(null).getCanonicalPath() + GameData.animFolder, "");
			outFile.mkdirs();
		} catch (IOException e) {
			Log.e("tag", "Failed to get asset file list.", e);
		}
		for(String filename : files) {
			InputStream in = null;
			OutputStream outStream = null;
			try {
				in = assetManager.open(filename);

				String out = GameData.mainActivity.getExternalFilesDir(null).getCanonicalPath() + GameData.animFolder; 
				
				File outFile = new File(out, filename);
				
				
				outStream = new FileOutputStream(outFile);
				copyFile(in, outStream);
				in.close();
				in = null;
				outStream.flush();
				outStream.close();
				out = null;			    	
				
			} catch(IOException e) {
				Log.e("tag", "Failed to copy asset file: " + filename, e);
			}       
		}
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("assetsAreCopied", true);
	    editor.commit();
	}
	private void copyFile(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while((read = in.read(buffer)) != -1){
			out.write(buffer, 0, read);
		}
	}
}
