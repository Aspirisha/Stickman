package com.autumncoding.stickman;

import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

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
	private static final long max_delta_between_drawings = 1000 / GameData.FPS;
	private boolean is_inited = false;
	// drawing entities
	private LinkedList<DrawingPrimitive> drawing_queue;
	private Circle menu_circle;
    private Stick menu_stick;
    
    float mLastTouchX = 0;
	float mLastTouchY = 0;
	private int lastTouchMenuIndex = -1;
	
	private LinkedList<MotionEvent> touch_events;
	private LinkedList<Long> events_time;
	private LinkedList<MotionEvent> bufferTouchEvents;
	private LinkedList<Long> bufferEventsTime;
	public ReentrantLock lock;
	
	private long startTime = 0;
	private long lastTouchTime = System.currentTimeMillis();
	private DrawingPrimitive lastTouchedPrimitive = null;
	private boolean movementIsScaling = false;
	
	private PointF m_startDrawingPoint = null;
	private PointF m_farthestDrawingPoint = null;
	private float m_maxDist = -1;
	private boolean m_drawingIsStarted = false;
	private boolean m_drawingHasIntersection = false;
	
	TouchState currentWorkingState = TouchState.DRAWING;
	
	public void setRunning(boolean run) {
        isRunning = run;
	}
	
	TouchEventThread(GameView _gameView) {
		m_gameView = _gameView;
		touch_events = new LinkedList<MotionEvent>();
		events_time = new LinkedList<Long>();
		bufferTouchEvents = new LinkedList<MotionEvent>();
		bufferEventsTime = new LinkedList<Long>();
		lock = new ReentrantLock();
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
			if ((!touch_events.isEmpty() || !bufferTouchEvents.isEmpty()) && dt <= max_delta_between_drawings) {
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
		
		if (lock.tryLock()) {
			touch_events.addAll(bufferTouchEvents);
			events_time.addAll(bufferEventsTime);
			bufferTouchEvents.clear();
			bufferEventsTime.clear();
			event = touch_events.pollFirst();
			eventTime = events_time.pollFirst();
			lock.unlock();
		} else {
			return;
		}
		
		if (event.getY() < GameData.getMenuBottom()) {
			processEventInMenu(event);
		} else {
			switch (currentWorkingState) {
			case DRAGGING:
				processEventDragging(event, eventTime);
				break;
			case DRAWING: {
				if (GameData.maxPrimitivesNumber > drawing_queue.size()) {
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
			case 0:
				synchronized (game_data.getLocker()) {
					Animation.getInstance().SaveToFile("AnimSave.sav");
					lastTouchMenuIndex = 0;
				}
				break;
			case 1:
				synchronized (game_data.getLocker()) {
					Animation.getInstance().LoadFormFile("AnimSave.sav");
					drawing_queue = Animation.getInstance().getFrame(0);
					game_data.setDrawingQueue(drawing_queue);
					m_gameView.setDrawingQueue(drawing_queue);
					lastTouchMenuIndex = 1;
				}
				break;
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
			case 7:
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
			synchronized (game_data.getLocker()) {
				GameData.drawnPoints.add(new PointF(x, y));
			}
			m_startDrawingPoint.x = x;
			m_startDrawingPoint.y = y;
			m_maxDist = -1;
			m_drawingIsStarted = true;
			break;

		case MotionEvent.ACTION_MOVE: {
			if (!m_drawingIsStarted)
				break;
			PointF p1 = GameData.drawnPoints.get(GameData.drawnPoints.size() - 1); // prev point
			synchronized (game_data.getLocker()) {
				float dist = PointF.length(x - p1.x, y - p1.y);
				if (dist < 5f) // avoid noise from fingers, slow motions etc
					break;
				GameData.drawnPoints.add(new PointF(x, y));
			}
			if (!m_drawingHasIntersection) {
				float dx = x - p1.x;
				float dy = y - p1.y;

				int maxI = GameData.drawnPoints.size() - 3;
				for (int i = 0; i < maxI; i++) {
					PointF p3 = GameData.drawnPoints.get(i);
					PointF p4 = GameData.drawnPoints.get(i + 1);
					float det = dx * (p3.y - p4.y) - dy * (p3.x - p4.x);
					if (det != 0) {
						float alpha = ((p3.x - p1.x) * (p3.y - p4.y) - (p3.x - p4.x) * (p3.y - p1.y)) / det;
						float beta = (dx * (p3.y - p1.y) - (p3.x - p1.x) * dy) / det;
						m_drawingHasIntersection = (beta >= 0 && beta <= 1 && alpha >= 0 && alpha <= 1);
					} else {
						if ((p1.x - p3.x) * (p4.y - p3.y) == (p1.y - p3.y) * (p4.x - p3.x)) {
							if (p1.x != x) {

							}
						}
					}

					if (m_drawingHasIntersection)
						break;
				}
			}
		}
		float dist = PointF.length(x - m_startDrawingPoint.x, y - m_startDrawingPoint.y);
		if (m_maxDist < dist) {
			m_maxDist = dist;
			m_farthestDrawingPoint.x = x;
			m_farthestDrawingPoint.y = y;
		}
		break;
			
		case MotionEvent.ACTION_UP: {
			if (!m_drawingIsStarted)
				break;
			
			DrawingPrimitive newPrimitive = null;
			if (!m_drawingHasIntersection) {
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
				GameData.drawnPoints.clear();
			}
			m_drawingHasIntersection = false;
			
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
			
			synchronized (game_data.getLocker()) {
				if (primitive != null) {
					primitive.setUntouched();
					m_gameView.setTouchedPrimitive(null);
					int index = 0;
					int size = drawing_queue.size();
					for (int i = 0; i < size; i++) {
						DrawingPrimitive pr = drawing_queue.get(index);
						if (pr.isOutOfBounds()) {
							pr.disconnectFromEverybody();
							drawing_queue.remove(index);
						} else
							index++;
					}
					
				}
			}
			break;
		}
		}	
	}
	
	private void processEventPlaying(MotionEvent event)
	{
				
	}
	
	public void pushEvent(MotionEvent ev) {
		long time = System.currentTimeMillis();
		
		if (lock.tryLock()) {
			try {
				touch_events.addAll(bufferTouchEvents);
				events_time.addAll(bufferEventsTime);
				bufferTouchEvents.clear();
				bufferEventsTime.clear();
				touch_events.add(ev);
				events_time.add(time);
			} finally {
				lock.unlock();
			}
		} else {
			bufferTouchEvents.add(ev);
			bufferEventsTime.add(time);
		}
	}
	
}
