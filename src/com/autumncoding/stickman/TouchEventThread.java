package com.autumncoding.stickman;

import java.util.LinkedList;

import com.autumncoding.stickman.DrawingPrimitive.PrimitiveType;

import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;

public class TouchEventThread extends Thread {
	enum TouchState {
		DRAWING,
		DRAGGING,
		WATCHING
	}
	private RectF[] menuRects = null;
	
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
	private int lastTouchMenuIndex = -1;
	
	LinkedList<MotionEvent> touch_events;
	LinkedList<Long> events_time;
	private Object queueLocker = new Object();
	
	private long startTime = 0;
	private long lastTouchTime = System.currentTimeMillis();
	private DrawingPrimitive lastTouchedPrimitive = null;
	private boolean movementIsScaling = false;
	
	private PointF m_startDrawingPoint = null;
	private PointF m_farthestDrawingPoint = null;
	private float m_maxDist = -1;
	private boolean m_drawingIsStarted = false;
	
	TouchState currentWorkingState = TouchState.DRAWING;
	
	public void setRunning(boolean run) {
        isRunning = run;
	}
	
	TouchEventThread(GameView _gameView) {
		m_gameView = _gameView;
		touch_events = new LinkedList<MotionEvent>();
		events_time = new LinkedList<Long>();
		setName("Touch thread");
		m_startDrawingPoint = new PointF();
		m_farthestDrawingPoint = new PointF();
	}
	
	synchronized void SetMenuRects(float screenWidth, float width, float height) {
		menuRects = new RectF[GameData.numberOfMenuIcons];
		
		float dx = (screenWidth - GameData.numberOfMenuIcons * width) / GameData.numberOfMenuIcons;
		float left = dx / 2f;
		float top = GameData.menuIconsTop;
		float bottom = top + height;
		for (int i = 0; i < GameData.numberOfMenuIcons; i++) {
			menuRects[i] = new RectF(left, top, left + width, bottom);
			left += width;
			left += dx;
		}
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
		
		while (isRunning) {
			startTime = System.currentTimeMillis();
			long dt = startTime - game_data.getPrevDrawingTime();
			// work here
			if (!touch_events.isEmpty() && dt <= max_delta_between_drawings) {
				processEvent();
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
	
	private void processEvent() {
		MotionEvent event = null;
		long eventTime = 0;
		
		synchronized (queueLocker) {
			event = touch_events.pollFirst();
			eventTime = events_time.pollFirst();
		}
		
		if (event.getY() < GameData.getMenuBottom()) {
			processEventInMenu(event);
		} else {
			switch (currentWorkingState) {
			case DRAGGING:
				processEventDragging(event, eventTime);
				break;
			case DRAWING: {
				int elementsNumber = 0;
				synchronized (game_data.getLocker()) {
					elementsNumber = drawing_queue.size();
				}
				if (GameData.maxPrimitivesNumber > elementsNumber) {
					processEventDrawing(event);
				}
				break;
			}
			case WATCHING:
				processEventPlaying(event);
				break;
			default:
				break;
			
			}		
		}
		
		mLastTouchX = event.getX();
		mLastTouchY = event.getY();
	}
	
	private void processEventInMenu(MotionEvent event) {
		int eventCode = event.getAction() & MotionEvent.ACTION_MASK;
		float x = event.getX();
		float y = event.getY();
		
		int touchedMenuIndex = -1;
		if (menuRects != null) {			
			for (int i = 0; i < GameData.numberOfMenuIcons; i++) {
				if (menuRects[i].contains(x, y)) {
					touchedMenuIndex = i;
					break;
				}
			}
		} else
			return;
		
		if (touchedMenuIndex == -1)
			return;
		
		switch (eventCode) {
		case MotionEvent.ACTION_DOWN: {
			GameData.setMenuTouch(touchedMenuIndex, true);
			switch (touchedMenuIndex) {
			case 2:
				currentWorkingState = TouchState.DRAWING;
				GameData.setMenuTouch(3, false);
				GameData.setMenuTouch(6, false);
				break;
			case 3:
				currentWorkingState = TouchState.DRAGGING;
				GameData.setMenuTouch(2, false);
				GameData.setMenuTouch(6, false);
				break;
			case 6:
				currentWorkingState = TouchState.WATCHING;
				GameData.setMenuTouch(2, false);
				GameData.setMenuTouch(3, false);
				break;
			default:
				lastTouchMenuIndex = touchedMenuIndex;
				break;
			}
			break;
		}
		
		case MotionEvent.ACTION_UP: {
			if (lastTouchMenuIndex != -1) { // check that needed action was performed (in fact we had to put it to a queue of needed actions)
				GameData.setMenuTouch(lastTouchMenuIndex, false);
			}
		}
		}
	}
	
	private void processEventDrawing(MotionEvent event)
	{
		int eventCode = event.getAction() & MotionEvent.ACTION_MASK;
		float x = event.getX();
		float y = event.getY();

		switch (eventCode) {
		case MotionEvent.ACTION_DOWN: 
			m_startDrawingPoint.x = x;
			m_startDrawingPoint.y = y;
			m_maxDist = -1;
			m_drawingIsStarted = true;
			break;

		case MotionEvent.ACTION_MOVE: {
			if (!m_drawingIsStarted)
				break;
			float dist = PointF.length(x - m_startDrawingPoint.x, y - m_startDrawingPoint.y);
			if (m_maxDist < dist) {
				m_maxDist = dist;
				m_farthestDrawingPoint.x = x;
				m_farthestDrawingPoint.y = y;
			}
			break;
		}
			
		case MotionEvent.ACTION_UP: {
			if (!m_drawingIsStarted)
				break;
			float dist = PointF.length(x - m_startDrawingPoint.x, y - m_startDrawingPoint.y);
			DrawingPrimitive newPrimitive = null;
			if (dist > 30f) {
				Stick stick = new Stick(m_gameView.getContext());
				
				stick.setPosition(m_startDrawingPoint.x, m_startDrawingPoint.y, x, y);
				newPrimitive = stick;
			} else {
				Circle circle = new Circle(m_gameView.getContext());
				float cx = (m_startDrawingPoint.x + m_farthestDrawingPoint.x) / 2f;
				float cy = (m_startDrawingPoint.y + m_farthestDrawingPoint.y) / 2f;
				
				float dx = m_startDrawingPoint.x - m_farthestDrawingPoint.x;
				float dy = m_startDrawingPoint.y - m_farthestDrawingPoint.y;
				float r = 30f; //(float) Math.sqrt(dx * dx + dy * dy);
				r = Math.max(r, GameData.min_circle_radius);
				
				circle.setPosition(cx, cy, r, 0);
				
				newPrimitive = circle;
			}
			synchronized (game_data.getLocker()) {
				drawing_queue.add(newPrimitive);
			}
			
			m_drawingIsStarted = false;
			break;
		}
		}
	}
	
		
	
	private void processEventDragging(MotionEvent event, long eventTime)
	{
		int eventCode = event.getAction() & MotionEvent.ACTION_MASK;
		float x = event.getX();
		float y = event.getY();
		
		switch (eventCode) {
		case MotionEvent.ACTION_DOWN: {
			boolean something_is_touched = false;
			
			for (DrawingPrimitive primitive : drawing_queue) {
				primitive.checkTouch(x, y);
				if (primitive.isTouched()) {
					something_is_touched = true;
					
					synchronized (game_data.getLocker()) {
						drawing_queue.remove(primitive); // to make it drawing last
						drawing_queue.add(primitive);
						m_gameView.setTouchedPrimitive(primitive);
					}
					
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
			if (touchedPrimitive != null) {
				synchronized (game_data.getLocker()) {
					touchedPrimitive.applyMove(x, y, mLastTouchX, mLastTouchY, movementIsScaling);
				}
				if (startTime - System.currentTimeMillis() > desiredSleepTime) {
					try {
						sleep(desiredSleepTime);
					} catch (Exception e) {
						
					}
				}
				synchronized (game_data.getLocker()) {
					touchedPrimitive.tryConnection(drawing_queue);
				}
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
					synchronized (game_data.getLocker()) {
						drawing_queue.add(pr);
					}
					m_gameView.setTouchedPrimitive(pr);
					pr.translate(x - mLastTouchX, y - mLastTouchY); 
				}
			
			}
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
	
	private void processEventPlaying(MotionEvent event)
	{
				
	}
	
	public void pushEvent(MotionEvent ev) {
		synchronized  (game_data.getLocker()) {
			touch_events.push(ev);
			events_time.push(System.currentTimeMillis());
		}
	}
	
}
