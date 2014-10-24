package com.autumncoding.stickman;

import java.util.LinkedList;

import android.util.Log;
import android.view.MotionEvent;

public class TouchEventThread extends Thread {
	private GameData game_data;
	private boolean isRunning = false;
	static final long FPS = 50;
	static final long ticksPS = 1000 / FPS;
	private static final int max_delta_between_drawings = 100;
	private boolean is_inited = false;
	
	// drawing entities
	private LinkedList<DrawingPrimitive> drawing_queue;
	private CentralJoint menu_central_joint;
	private Circle menu_circle;
	
    private Stick menu_stick;
    float mLastTouchX;
	float mLastTouchY;
	
	LinkedList<Float> x_touches;
	LinkedList<Float> y_touches;
	LinkedList<Integer> motion_events;
	
	private long startTime = 0;
	
	public void setRunning(boolean run) {
        isRunning = run;
	}
	
	TouchEventThread() {
		x_touches = new LinkedList<Float>();
		y_touches = new LinkedList<Float>();
		motion_events = new LinkedList<Integer>();
		mLastTouchX = 0;
		mLastTouchY = 0;
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
			if (!x_touches.isEmpty() && dt <= max_delta_between_drawings) {
				synchronized (game_data.getLocker()) {
					processEvent();
				}
			}
			
			sleepTime = ticksPS - (System.currentTimeMillis() - startTime);
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
		
		int event = motion_events.pollFirst();
		float x = x_touches.pollFirst();
		float y = y_touches.pollFirst();
		
		switch (event) {
		case MotionEvent.ACTION_DOWN: {
			mLastTouchX = x;
			mLastTouchY = y;
			
			PlayingTableView.currently_touched_stick = null;
			PlayingTableView.currently_touched_joint = null;
			PlayingTableView.currently_touched_circle = null;
			boolean something_is_touched = false;
			
			for (DrawingPrimitive primitive : drawing_queue) {
				primitive.checkTouched(x, y);
				if (primitive.isTouched()) {
					something_is_touched = true;
					drawing_queue.remove(primitive); // to make it drawing last
					drawing_queue.add(primitive);
					switch (primitive.GetType()) {
					case JOINT:
						PlayingTableView.currently_touched_joint = (CentralJoint)primitive;
						break;
					case STICK:
						PlayingTableView.currently_touched_stick = (Stick)primitive;
						break;
					case CIRCLE:
						PlayingTableView.currently_touched_circle = (Circle)primitive;
						break;
					default:
						break;
					}
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
		case MotionEvent.ACTION_MOVE: {
			if (PlayingTableView.currently_touched_stick != null) {
				switch (PlayingTableView.currently_touched_stick.getTouchState()) {
				case JOINT1:
					PlayingTableView.currently_touched_stick.scale(x, y);
					break;
				case JOINT2:
					PlayingTableView.currently_touched_stick.rotateAroundJoint1(x, y);
					break;
				case STICK:
					PlayingTableView.currently_touched_stick.translate(x - mLastTouchX, y - mLastTouchY);
					break;
				default:
					break;
				}
				
				float min_dist = 10000;
				DrawingPrimitive closest_primitive = null;
				for (DrawingPrimitive primitive : drawing_queue) {
					if (primitive == PlayingTableView.currently_touched_stick)
						continue;
					float cur_dist = PlayingTableView.currently_touched_stick.distTo(primitive);
					if (cur_dist < min_dist) {
						min_dist = cur_dist;
						closest_primitive = primitive;
					}
				}

				if (min_dist <= 100)
					PlayingTableView.currently_touched_stick.connectTo(closest_primitive);
				else 
					PlayingTableView.currently_touched_stick.set_not_connected();
			}

			
		    if (PlayingTableView.currently_touched_joint != null) {
				PlayingTableView.currently_touched_joint.translate(x - mLastTouchX, y - mLastTouchY);
			}
		    
		    if (PlayingTableView.currently_touched_circle != null) {
		    	PlayingTableView.currently_touched_circle.applyMove(x, y, mLastTouchX, mLastTouchY);
		    }
		    
		    if (menu_central_joint.isTouched()) {
				PlayingTableView.currently_touched_joint = new CentralJoint(menu_stick.getContext());
				PlayingTableView.currently_touched_joint.CopyJoint(menu_central_joint);
				menu_central_joint.setUntouched();
				drawing_queue.add(PlayingTableView.currently_touched_joint);
				PlayingTableView.currently_touched_joint.translate(x - mLastTouchX, y - mLastTouchY);
			}
		    else if (menu_stick.isTouched()) {
				PlayingTableView.currently_touched_stick = new Stick(menu_stick.getContext());
				PlayingTableView.currently_touched_stick.CopyStick(menu_stick);
				menu_stick.setUntouched();
				drawing_queue.add(PlayingTableView.currently_touched_stick);
				PlayingTableView.currently_touched_stick.translate(x - mLastTouchX, y - mLastTouchY);
			}
			else if (menu_circle.isTouched()) {
				PlayingTableView.currently_touched_circle = new Circle(menu_stick.getContext());
				PlayingTableView.currently_touched_circle.CopyCircle(menu_circle);
				menu_circle.setUntouched();
				drawing_queue.add(PlayingTableView.currently_touched_circle);
				PlayingTableView.currently_touched_circle.translate(x - mLastTouchX, y - mLastTouchY);
			}
			mLastTouchX = x;
			mLastTouchY = y;
			break;
		}
		
		case MotionEvent.ACTION_UP: {
			if (PlayingTableView.currently_touched_joint != null) {
				PlayingTableView.currently_touched_joint.setUntouched();
				/*if (inMenu(PlayingTableView.currently_touched_joint)) {
					drawing_queue.removeLast();
				}*/
				PlayingTableView.currently_touched_joint = null;
			}

			if (PlayingTableView.currently_touched_stick != null) {
				PlayingTableView.currently_touched_stick.setUntouched();
				/*if (inMenu(PlayingTableView.currently_touched_stick)) {
					drawing_queue.removeLast();
				}*/
				PlayingTableView.currently_touched_stick = null;
			}
			break;
		}
		}
	}
	
	synchronized public void pushEvent(float x, float y, int event) {
		x_touches.add(x);
		y_touches.add(y);
		motion_events.add(event);
	}
	
}
