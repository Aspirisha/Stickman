package com.autumncoding.stickman;

import com.autamncoding.stickman.R;

import android.os.Bundle;
import android.app.Activity;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.Window;
import android.widget.LinearLayout;

public class MainActivity extends Activity {

	GameHolder gameHolder;
	static int layout_height;
	static int layout_width;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		gameHolder = new GameHolder(this);
		setContentView(gameHolder);
		gameHolder.post(new Runnable() {

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
	            gameHolder.playingTable.setMetrics();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
