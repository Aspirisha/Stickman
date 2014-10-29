package com.autumncoding.stickman;

import java.util.LinkedList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.autamncoding.stickman.R;


public class GameView extends SurfaceView {
	private GameData game_data;
	private static final int INVALID_POINTER_ID = -1;
	private int mActivePointerId = INVALID_POINTER_ID;
	private SurfaceHolder holder;
	private GameLoopThread gameLoopThread;
	private TouchEventThread touch_thread;
    
    private Paint debug_paint;
    private float translate_x = 0;
    private float translate_y = 0;
    private Paint menu_line_paint;
    
    // share with touch thread: get from shared storage
    private Stick menu_stick;
    private Circle menu_circle;
    
    private LinkedList<DrawingPrimitive> drawing_queue;
    private long prev_drawing_time = System.currentTimeMillis();
    
    private DrawingPrimitive currently_touched_pimititve;
    
    public GameView(Context context) {
    	super(context);
    	this.setBackgroundColor(Color.WHITE);
    	game_data = GameData.getInstance();
    	game_data.init(this);
    	gameLoopThread = new GameLoopThread(this);
    	
    	holder = getHolder();
    	debug_paint = GameData.debug_paint;
		
    	menu_line_paint = GameData.menu_line_paint;
    	
    	menu_stick = game_data.getMenuStick();
    	menu_circle = game_data.getMenuCircle();
    	
    	drawing_queue = game_data.getDrawingQueue();
    	touch_thread = new TouchEventThread(this);
    	touch_thread.init();
    	currently_touched_pimititve = null;
    	
    	setWillNotDraw(true);    	
    	
    	holder.addCallback(new SurfaceHolder.Callback() {
    		
    		@Override
    		public void surfaceDestroyed(SurfaceHolder holder) {
    			boolean retry = true;
    			/*set the flag to false */
    			gameLoopThread.setRunning(false);
    			while (retry) {
    				try {
    					gameLoopThread.join();
    					retry = false;
    				} catch (InterruptedException e) {
    					// we will try it again and again...
    				}
    			}
    			for (int i = 0; i < 12; i++) {

    			}
    		}

    		@Override
    		public void surfaceCreated(SurfaceHolder holder) {
    			gameLoopThread.setRunning(true);
    			touch_thread.setRunning(true);
    			gameLoopThread.start(); 
    			touch_thread.start();
    		}

    		@Override
    		public void surfaceChanged(SurfaceHolder holder, int format,
    				int width, int height) {

    		}
    	});
	}
    
    
    public void setMetrics() {
    	menu_stick.setPosition(MainActivity.layout_width - 140, MainActivity.layout_height - 20, MainActivity.layout_width - 40, MainActivity.layout_height - 20);
    	menu_circle.setPosition(MainActivity.layout_width - 180, MainActivity.layout_height - 20, 0);
    	game_data.setMetrics();
    }
    
    @Override
    public boolean onTouchEvent(final MotionEvent ev) {    	
    	synchronized (getHolder()) {
    		touch_thread.pushEvent(ev);
    	}
        return true;
    }
    
    
    @Override  
    public void onDraw(Canvas canvas) {
    	long cur_drawing_time = System.currentTimeMillis();
    	game_data.writeDrawingTime();
    	//canvas.translate(translate_x, translate_y);
        canvas.save();
        
        // debug info
        canvas.drawText(Float.toString(1000f / (float)(cur_drawing_time - prev_drawing_time)), 30 - translate_x, 30 - translate_y, debug_paint);
        
        // bottom menu 
        canvas.drawLine(game_data.bottom_menu_x1, game_data.bottom_menu_y, game_data.bottom_menu_x2, game_data.bottom_menu_y, menu_line_paint);
        
        // top menu
        canvas.drawLine(game_data.top_menu_x1, game_data.top_menu_y, game_data.top_menu_x2, game_data.top_menu_y, menu_line_paint);
        
        
        synchronized (game_data.getLocker()) {
	        menu_stick.draw(canvas);
	        menu_circle.draw(canvas);
	        // finally draw all joints
	        for (DrawingPrimitive v : drawing_queue)
	        	v.draw(canvas);
        }
        canvas.restore();
        prev_drawing_time = cur_drawing_time;
    }
    
    
    private boolean inMenu(Stick stick) {
    	int dy = 40;
    	return (!stick.isHigher(MainActivity.layout_height - dy));
    }
    
    public DrawingPrimitive getTouchedPrimitive() {
    	return currently_touched_pimititve;
    }
 
    public void setTouchedPrimitive(DrawingPrimitive pr) {
    	currently_touched_pimititve = pr;
    }
}

