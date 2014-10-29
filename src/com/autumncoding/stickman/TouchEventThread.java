package com.autumncoding.stickman;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Stack;

import com.autumncoding.stickman.DrawingPrimitive.PrimitiveType;
import com.autumncoding.stickman.DrawingPrimitive.VisitColor;

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
					
					if (eventTime - lastTouchTime < 500 && lastTouchedPrimitive == primitive) 
						movementIsScaling = true;
					else
						movementIsScaling = false;
					
					lastTouchedPrimitive = primitive;
					lastTouchTime = eventTime;
					break;
				}
			}
			
			if (!something_is_touched) {
    			menu_central_joint.checkTouch(x, y);
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
				boolean needRecountTree = touchedPrimitive.tryConnection(drawing_queue);
				if (needRecountTree)
					recountDrawingTree(touchedPrimitive);
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
			DrawingPrimitive primitive = m_gameView.getTouchedPrimitive();
			if (primitive != null) {
				primitive.setUntouched();
				m_gameView.setTouchedPrimitive(null);
			}
			if (movementIsScaling) {
				lastTouchTime = -1;
			}
			break;
		}
		}
		
	}
	
	synchronized public void pushEvent(MotionEvent ev) {
		touch_events.push(ev);
		events_time.push(System.currentTimeMillis());
	}
	
	/**
	 * @param root is the root of the primitives tree we need to rearrange 
	 * (we need to change some relations so that primitive, which is closer 
	 *  to free joint is child of primitive that is farther)
	 */
	private void recountDrawingTree(DrawingPrimitive root) {
		Stack<DrawingPrimitive> stack = new Stack<DrawingPrimitive>();
		LinkedList<DrawingPrimitive> q = new LinkedList<DrawingPrimitive>();
		
		for (DrawingPrimitive pr : drawing_queue)
			pr.setVisitColor(VisitColor.WHITE);
		stack.push(root);
		while (!stack.isEmpty()) {
			DrawingPrimitive s = stack.pop();
			ArrayList<DrawingPrimitive> children = s.getConnectedPrimitives();
			for (DrawingPrimitive pr : children) {
				if (pr.getVisitColor() == VisitColor.WHITE) {
					stack.push(pr);
					if (pr.isLeafInPrimitiveTree()) {
						q.push(pr);
						pr.setVisitColor(VisitColor.BLACK);
						pr.setDepth(0);
					} else {
						pr.setVisitColor(VisitColor.GRAY);	
					}
				}
			}
		}
		
		// now call BFS with this q to recount depths
		while (!q.isEmpty()) {
			DrawingPrimitive s = q.pollFirst();
			stack.push(s);
			ArrayList<DrawingPrimitive> children = s.getConnectedPrimitives();
			float depth = s.getDepth();
			
			for (DrawingPrimitive pr : children) {
				if (pr.getVisitColor() == VisitColor.GRAY) {
					q.add(pr);
					pr.setVisitColor(VisitColor.BLACK);
					pr.setDepth(depth + 1);
				}
			}
		}	
		
		for (DrawingPrimitive pr: stack) {
			float depth = pr.getDepth();
			for (DrawingPrimitive connected : pr.getConnectedPrimitives()) {
				if (depth < connected.getDepth()) {
					pr.addParent(connected);
					connected.addChild(pr);
				} else {
					pr.addChild(connected);
					connected.addParent(pr);
				}
			}
		}
	}
	
}
