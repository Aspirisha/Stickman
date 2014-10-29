package com.autumncoding.stickman;

import java.util.LinkedList;

import com.autumncoding.stickman.DrawingPrimitive.PrimitiveType;

import android.util.Log;
import android.view.MotionEvent;

public class TouchEventThread extends Thread {
	private GameData game_data;
	private GameView m_gameView;
	private boolean isRunning = false;
	static final long desiredSleepTime = 1000 / GameData.FPS;
	private static final int max_delta_between_drawings = 100;
	private boolean is_inited = false;
	
	// drawing entities
	private LinkedList<DrawingPrimitive> drawing_queue;
	private Circle menu_circle;
    private Stick menu_stick;
    
    float mLastTouchX = 0;
	float mLastTouchY = 0;
	
	LinkedList<MotionEvent> touch_events;
	LinkedList<Long> events_time;
	
	private long startTime = 0;
	private long lastTouchTime = System.currentTimeMillis();
	private DrawingPrimitive lastTouchedPrimitive = null;
	private boolean movementIsScaling = false;
	
	public void setRunning(boolean run) {
        isRunning = run;
	}
	
	TouchEventThread(GameView _gameView) {
		m_gameView = _gameView;
		touch_events = new LinkedList<MotionEvent>();
		events_time = new LinkedList<Long>();
		setName("Touch thread");
	}
	
	public void init() {
		game_data = GameData.getInstance();
    	menu_stick = game_data.getMenuStick();
    	menu_circle = game_data.getMenuCircle();
    	drawing_queue = game_data.getDrawingQueue();
    	is_inited = true;
	}
	
	@Override
	public void run() {
		long sleepTime;
		long startTime;
		
		while (isRunning) {
			startTime = System.currentTimeMillis();
			long dt = startTime - game_data.getPrevDrawingTime();
			// work here
			if (!touch_events.isEmpty() && dt <= max_delta_between_drawings) {
				synchronized (game_data.getLocker()) {
					processEvent();
				}
			}
			
			sleepTime = desiredSleepTime - (System.currentTimeMillis() - startTime);
			try {
				if (sleepTime > 0)
					sleep(sleepTime);
				else
					sleep(10);
			} catch (Exception e) {
				Log.i("Exeption: ", e.toString());
			}
		}
	}
	
	synchronized private void processEvent() {
		MotionEvent event = touch_events.pollFirst();
		long eventTime = events_time.pollFirst();
		
		int eventCode = event.getAction() & MotionEvent.ACTION_MASK;
		float x = event.getX();
		float y = event.getY();
		
		switch (eventCode) {
		case MotionEvent.ACTION_DOWN: {
			
			mLastTouchX = x;
			mLastTouchY = y;
			
			boolean something_is_touched = false;
			
			for (DrawingPrimitive primitive : drawing_queue) {
				primitive.checkTouch(x, y);
				if (primitive.isTouched()) {
					something_is_touched = true;
					drawing_queue.remove(primitive); // to make it drawing last
					drawing_queue.add(primitive);
					m_gameView.setTouchedPrimitive(primitive);
					
					if (eventTime - lastTouchTime < 400) 
						movementIsScaling = true;
					else
						movementIsScaling = false;
					
					lastTouchedPrimitive = primitive;
					lastTouchTime = eventTime;
					break;
				}
			}
			
			if (!something_is_touched) {
    			menu_stick.checkTouch(x, y);
    			menu_circle.checkTouch(x, y);
			}
			break;
		}
		
		case MotionEvent.ACTION_MOVE: {
			DrawingPrimitive touchedPrimitive = m_gameView.getTouchedPrimitive();
			int index = event.getActionIndex();
			if (touchedPrimitive != null) {
				touchedPrimitive.applyMove(x, y, mLastTouchX, mLastTouchY, movementIsScaling);
				touchedPrimitive.tryConnection(drawing_queue);
			} else {
				DrawingPrimitive pr = null;
				
				if (menu_stick.isTouched()) {
					pr = new Stick(menu_stick.getContext());
					pr.copy(menu_stick);
					menu_stick.setUntouched();
				}
				else if (menu_circle.isTouched()) {
					pr = new Circle(menu_stick.getContext());
					pr.copy(menu_circle);
					menu_circle.setUntouched();
				}
				
				if (pr != null) {
					drawing_queue.add(pr);
					m_gameView.setTouchedPrimitive(pr);
					pr.translate(x - mLastTouchX, y - mLastTouchY); 
				}
			
			}
			mLastTouchX = x;
			mLastTouchY = y;
			break;
		}
		
		case MotionEvent.ACTION_UP: {
			DrawingPrimitive primitive = m_gameView.getTouchedPrimitive();
			if (primitive != null) {
				primitive.setUntouched();
				m_gameView.setTouchedPrimitive(null);
			}
			break;
		}
		}
		
	}
	
	synchronized public void pushEvent(MotionEvent ev) {
		touch_events.push(ev);
		events_time.push(System.currentTimeMillis());
	}
	
}
