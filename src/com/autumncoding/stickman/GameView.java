package com.autumncoding.stickman;

import java.util.ArrayList;
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
    private CentralJoint menu_central_joint;
    private Stick menu_stick;
    private Circle menu_circle;
    
    private LinkedList<DrawingPrimitive> drawing_queue;
    private long prev_drawing_time = System.currentTimeMillis();
   
    
    public static Stick currently_touched_stick;
    public static CentralJoint currently_touched_joint;
    public static Circle currently_touched_circle;
    
    public GameView(Context context) {
    	super(context);
    	this.setBackgroundColor(Color.WHITE);
    	
    	gameLoopThread = new GameLoopThread(this);
    	
    	holder = getHolder();
    	debug_paint = new Paint();
    	debug_paint.setColor(Color.BLACK);
    	debug_paint.setAntiAlias(true);
    	debug_paint.setDither(true);
    	debug_paint.setStrokeWidth(2f);
    	debug_paint.setStyle(Paint.Style.STROKE);
    	debug_paint.setStrokeJoin(Paint.Join.ROUND);
    	debug_paint.setStrokeCap(Paint.Cap.ROUND);
    	debug_paint.setTextSize(getResources().getDimension(R.dimen.debug_font_size));
		
    	menu_line_paint = new Paint();
    	menu_line_paint.setColor(Color.BLACK);
    	menu_line_paint.setStrokeJoin(Paint.Join.ROUND);
    	menu_line_paint.setStrokeCap(Paint.Cap.ROUND);
    	menu_line_paint.setAntiAlias(true);
    	menu_line_paint.setDither(true);
    	menu_line_paint.setStrokeWidth(3f);
    	
    	
    	game_data = GameData.getInstance();
    	game_data.init(this);
    	menu_central_joint = game_data.getMenuCentralJoint();
    	menu_stick = game_data.getMenuStick();
    	menu_circle = game_data.getMenuCircle();
    	
    	drawing_queue = game_data.getDrawingQueue();
    	touch_thread = new TouchEventThread();
    	touch_thread.init();
    	
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
    	menu_central_joint.setPosition(MainActivity.layout_width - 20, MainActivity.layout_height - 20);
    	menu_stick.setPosition(MainActivity.layout_width - 140, MainActivity.layout_height - 20, MainActivity.layout_width - 40, MainActivity.layout_height - 20);
    	menu_circle.setPosition(MainActivity.layout_width - 180, MainActivity.layout_height - 20, 0);
    	game_data.setMetrics();
    }
    
    @Override
    public boolean onTouchEvent(final MotionEvent ev) {
        // Let the ScaleGestureDetector inspect all events.    	
    	int pointerIndex = -1;
    	
    	synchronized (getHolder()) {
    		switch (ev.getAction()) {
    		case MotionEvent.ACTION_DOWN: 
    			touch_thread.pushEvent(ev.getX(), ev.getY(), MotionEvent.ACTION_DOWN);
    			mActivePointerId = ev.getPointerId(0);
    			break;

    		case MotionEvent.ACTION_POINTER_DOWN: 
    			touch_thread.pushEvent(ev.getX(), ev.getY(), MotionEvent.ACTION_POINTER_DOWN);
    			break;
    			
    		case MotionEvent.ACTION_MOVE: 
    			pointerIndex = ev.findPointerIndex(mActivePointerId);
    			touch_thread.pushEvent(ev.getX(pointerIndex), ev.getY(pointerIndex), MotionEvent.ACTION_MOVE);   			
    			break;

    		case MotionEvent.ACTION_UP: 
    			mActivePointerId = INVALID_POINTER_ID;
    			touch_thread.pushEvent(0, 0, MotionEvent.ACTION_UP);
    			break;

    		case MotionEvent.ACTION_CANCEL: {
    			mActivePointerId = INVALID_POINTER_ID;
    			break;
    		}

    		case MotionEvent.ACTION_POINTER_UP: 
    			pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
    			touch_thread.pushEvent(ev.getX(pointerIndex), ev.getY(pointerIndex), MotionEvent.ACTION_MOVE);
    			final int pointerId = ev.getPointerId(pointerIndex);
    			if (pointerId == mActivePointerId) {
    				// This was our active pointer going up. Choose a new
    				// active pointer and adjust accordingly.
    				final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
    				mActivePointerId = ev.getPointerId(newPointerIndex);
    			}
    			break;
    		
    		default:
    			Log.i("Default event", "default event happened");
    			
    		}
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
	        menu_central_joint.draw(canvas);
	        menu_stick.draw(canvas);
	        menu_circle.draw(canvas);
	        // finally draw all joints
	        for (DrawingPrimitive v : drawing_queue)
	        	v.draw(canvas);
        }
        canvas.restore();
        prev_drawing_time = cur_drawing_time;
    }
    
    private boolean inMenu(CentralJoint joint) {
    	int dy = 40;
    	if (joint.getMyY() >= MainActivity.layout_height - dy)
    		return true;
    	return false;
    }
    
    private boolean inMenu(Stick stick) {
    	int dy = 40;
    	return (!stick.isHigher(MainActivity.layout_height - dy));
    }
    
 
}

