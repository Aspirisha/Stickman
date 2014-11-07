package com.autumncoding.stickman;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

import com.autamncoding.stickman.R;

public class MainActivity extends Activity {
	static int layout_height;
	static int layout_width;
	public static final int	VIEW_INTRO = 0;
	public static final int	VIEW_GAME  = 1;
	
	int	m_viewCur = -1;
	AppIntro m_app;
	ViewIntro m_viewIntro;
	GameView m_gameView;
	
	@Override 
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		 // Create application
        m_app = new AppIntro(this, AppIntro.LANGUAGE_ENG);
        // Create view
        setView(VIEW_INTRO);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void setView(int viewID)
	{
		if (m_viewCur == viewID)
		{
			Log.d("THREE", "setView: already set");
			return;
		}
	
		m_viewCur = viewID;
		if (m_viewCur == VIEW_INTRO)
		{
	        m_viewIntro = new ViewIntro(this);
	        setContentView(m_viewIntro);
	        m_viewIntro.start();
		}
		if (m_viewCur == VIEW_GAME)
		{
			Log.d("THREE", "Switch to m_viewGame" );
			m_gameView = new GameView(this);
			m_gameView.post(new Runnable() {

				@Override
				public void run() {
					Rect rect = new Rect(); 
		            Window win = getWindow();  // Get the Window
		            win.getDecorView().getWindowVisibleDisplayFrame(rect); 
		            // Get the height of Status Bar 
		            int statusBarHeight = rect.top; 
		            // Get the height occupied by the decoration contents 
		            int contentViewTop = win.findViewById(Window.ID_ANDROID_CONTENT).getTop(); 
		            // Calculate titleBarHeight by deducting statusBarHeight from contentViewTop  
		            int titleBarHeight = contentViewTop - statusBarHeight; 
		            Log.i("MY", "titleHeight = " + titleBarHeight + " statusHeight = " + statusBarHeight + " contentViewTop = " + contentViewTop); 
		            
		            DisplayMetrics metrics = new DisplayMetrics();
		            getWindowManager().getDefaultDisplay().getMetrics(metrics);   
		            int screenHeight = metrics.heightPixels;
		            int screenWidth = metrics.widthPixels;
		            Log.i("MY", "Actual Screen Height = " + screenHeight + " Width = " + screenWidth);   
		 
		            // Now calculate the height that our layout can be set
		            // If you know that your application doesn't have statusBar added, then don't add here also. Same applies to application bar also 
		            layout_height = screenHeight - (titleBarHeight + statusBarHeight);
		            layout_width = screenWidth;
		            Log.i("MY", "Layout Height = " + layout_height);   
		            m_gameView.setMetrics();
				}
			});
			m_gameView.setBackgroundResource(R.drawable.background);
			setContentView(m_gameView);
		}
	}

	
	public AppIntro getApp()
	{
		return m_app;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.action_save:
	        	synchronized (GameData.getLocker()) {
	        		Animation.getInstance().SaveToFile("AnimSave.sav");
				}
	            return true;
	        case R.id.action_load:
	        	synchronized (GameData.getLocker()) {
	        		Animation.getInstance().LoadFormFile("AnimSave.sav");
	        		m_gameView.updateUpperMenu();
	        	}
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
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
	

}
