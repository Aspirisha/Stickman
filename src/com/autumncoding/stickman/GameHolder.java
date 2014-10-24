package com.autumncoding.stickman;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.LinearLayout;

public class GameHolder extends LinearLayout {
    float fromPosition;
    PlayingTableView playingTable;
    
    public enum REDRAW_TYPE {
    	DRAW_THROWN_CARDS,
    	SHOW_LEFT_CLOWD,
    	SHOW_RIGHT_CLOWD,
    	SHOW_OWN_CLOWD,
    	HIDE_LEFT_CLOWD,
    	HIDE_RIGHT_CLOWD,
    	HIDE_OWN_CLOWD,
    }
    
    
	public GameHolder(Context context) {
		super(context);
		playingTable = new PlayingTableView(context);
		addView(playingTable);
	}
	
	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec)
	{
	    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

	}
	
}