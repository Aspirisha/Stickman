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
	private CentralJoint menu_central_joint;
	private Circle menu_circle;
    private Stick menu_stick;
    
    float mLastTouchX;
	float mLastTouchY;
	
	LinkedList<MotionEvent> touch_events;
	
	private long startTime = 0;
	private int pointers_on_screen = 0;
	
	public void setRunning(boolean run) {
        isRunning = run;
	}
	
	TouchEventThread(GameView _gameView) {
		m_gameView = _gameView;
		touch_events = new LinkedList<MotionEvent>();
		mLastTouchX = 0;
		mLastTouchY = 0;
		setName("Touch thread");
	}
	
	public void init() {
		game_data = GameData.getInstance();
		menu_central_joint = game_data.getMenuCentralJoint();
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
		int eventCode = event.getAction() & MotionEvent.ACTION_MASK;
		float x = event.getX();
		float y = event.getY();
		
		switch (eventCode) {
		case MotionEvent.ACTION_DOWN: {
			pointers_on_screen++;
			
			mLastTouchX = x;
			mLastTouchY = y;
			
			boolean something_is_touched = false;
			
			for (DrawingPrimitive primitive : drawing_queue) {
				primitive.checkTouched(x, y);
				if (primitive.isTouched()) {
					something_is_touched = true;
					drawing_queue.remove(primitive); // to make it drawing last
					drawing_queue.add(primitive);
					m_gameView.setTouchedPrimitive(primitive);
					break;
				}
			}
			
			if (!something_is_touched) {
    			menu_central_joint.checkTouched(x, y);
    			menu_stick.checkTouched(x, y);
    			menu_circle.checkTouched(x, y);
			}
			break;
		}
		
		case MotionEvent.ACTION_POINTER_DOWN: {
			pointers_on_screen++;
			DrawingPrimitive primitive = m_gameView.getTouchedPrimitive();
			if (primitive == null)
				break;
			
			int index = event.getActionIndex();
			float touch_x = event.getX(index);
			float touch_y = event.getY(index);
			primitive.checkScaleTouched(touch_x, touch_y);
			
			break;
		}
		
		case MotionEvent.ACTION_POINTER_UP: {
			pointers_on_screen--;
			break;
		}
		
		case MotionEvent.ACTION_MOVE: {
			DrawingPrimitive touchedPrimitive = m_gameView.getTouchedPrimitive();
			if (touchedPrimitive != null) {
				touchedPrimitive.applyMove(x, y, mLastTouchX, mLastTouchY);
				
				if (touchedPrimitive.GetType() == PrimitiveType.STICK) {
					float min_dist = 10000;
					DrawingPrimitive closest_primitive = null;
					for (DrawingPrimitive primitive : drawing_queue) {
						if (primitive == touchedPrimitive)
							continue;
						float cur_dist = touchedPrimitive.distTo(primitive);
						if (cur_dist < min_dist) {
							min_dist = cur_dist;
							closest_primitive = primitive;
						}
					}
	
					if (min_dist <= 100)
						touchedPrimitive.connectTo(closest_primitive);
					else 
						touchedPrimitive.setNotConnected();
				}
			} else {
				DrawingPrimitive pr = null;
				
				if (menu_central_joint.isTouched()) {
					pr = new CentralJoint(menu_stick.getContext());
					pr.copy(menu_central_joint);
					menu_central_joint.setUntouched();
				}
				else if (menu_stick.isTouched()) {
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
			pointers_on_screen--;
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
	}
	
}
