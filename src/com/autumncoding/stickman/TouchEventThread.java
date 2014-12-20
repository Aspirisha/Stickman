package com.autumncoding.stickman;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

import com.autumncoding.stickman.Animation.AnimationState;

import android.graphics.PointF;
import android.util.Log;
import android.view.MotionEvent;

public class TouchEventThread extends Thread {
	enum TouchState {
		DRAWING,
		DRAGGING,
		WATCHING
	}
	
	private GameData game_data;
	private GameView m_gameView;
	private boolean isRunning = false;
	static final long desiredSleepTime = 1000 / GameData.FPS;
	private static final long max_delta_between_drawings = 1000 / GameData.FPS;
	// drawing entities
    
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
	private PointF m_currentDrawingPoint = null;
	private float m_maxDist = -1;
	private boolean m_drawingIsStarted = false;
	private boolean m_drawingHasIntersection = false;
	
	TouchState currentWorkingState = TouchState.DRAWING;
	
	class CurrentDrawingState
	{
		PointF m_startDrawingPoint = new PointF();
		PointF m_currentDrawingPoint = new PointF();
		boolean m_hasIntersection = false;
		PointF m_centre = new PointF();
		float m_radius = 0;
		boolean m_isDrawingProcess = false;
	}
	
	private void CopyPoint(PointF dst, PointF src) {
		dst.x = src.x;
		dst.y = src.y;
	}
	
	public CurrentDrawingState getCurrentDrawingState()
	{
		CurrentDrawingState drawingState = new CurrentDrawingState();

		drawingState.m_hasIntersection = m_drawingHasIntersection;
		drawingState.m_isDrawingProcess = m_drawingIsStarted;
		if (m_drawingHasIntersection) {
			drawingState.m_radius = 30.0f;
			drawingState.m_centre.x = (m_farthestDrawingPoint.x + m_startDrawingPoint.x) / 2.0f;
			drawingState.m_centre.y = (m_farthestDrawingPoint.y + m_startDrawingPoint.y) / 2.0f;
		} else {
			CopyPoint(drawingState.m_startDrawingPoint, m_startDrawingPoint);
			CopyPoint(drawingState.m_currentDrawingPoint, m_currentDrawingPoint);
		}
		
		return drawingState;
	}
	
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
		m_currentDrawingPoint = new PointF();
	}
	
	public void init(ArrayList<MenuIcon> icons) {
		game_data = GameData.getInstance();
	}
	
	@Override
	public void run() {
		long sleepTime;
		
		while (isRunning) {
			startTime = System.currentTimeMillis();
			long dt = startTime - game_data.getPrevDrawingTime();
			// work here
			if (dt <= max_delta_between_drawings) {
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
		Long eventTime = (long) 0;
		
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
		
		if (event == null)
			return;
		
		if (event.getY() < GameData.getMenuBottom()) {
			processEventInMenu(event);
		} else {
			switch (currentWorkingState) {
			case DRAGGING:
				processEventDragging(event, eventTime);
				break;
			case DRAWING: {
				if (GameData.maxPrimitivesNumber > GameData.drawing_queue.size()) {
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
		synchronized (GameData.getLocker()) {

			switch (eventCode) {
			case MotionEvent.ACTION_DOWN: {
				for (int i = 0; i < GameData.numberOfMenuIcons; ++i) {
					if (GameData.menuIcons.get(i).checkTouchedNoSet(x, y))
						touchedMenuIndex = i;
				}

				if (touchedMenuIndex == -1)
					return;
				switch (touchedMenuIndex) {
				case 0:
					currentWorkingState = TouchState.DRAWING;
					GameData.menuDrag.setUntouched();
					GameData.menuPlay.setUntouched();
					break;
				case 1:
					currentWorkingState = TouchState.DRAGGING;
					GameData.menuPencil.setUntouched();
					GameData.menuPlay.setUntouched();
					break;
				case 2:
					Animation.getInstance().switchToPrevFrame();
					
					lastTouchMenuIndex = 2;
					break;
				case 3:
					Animation.getInstance().switchToNextFrame();
					lastTouchMenuIndex = 3;
					break;
				case 4:
					Animation.getInstance().addFrame();
					GameData.menuNext.setActive();
					lastTouchMenuIndex = 4;
					break;
				case 5:
					Animation.getInstance().removeFrame();
					if (!Animation.getInstance().hasNextFrame())
						GameData.menuNext.setUnavailable();
					if (!Animation.getInstance().hasPrevFrame())
						GameData.menuPrev.setUnavailable();
					lastTouchMenuIndex = 5;
					break;
				case 6:
					currentWorkingState = TouchState.WATCHING;
					Animation.getInstance().Play(true);
					GameData.menuPencil.setUntouched();
					GameData.menuDrag.setUntouched();
					break;
				default:
					lastTouchMenuIndex = touchedMenuIndex;
					break;
				}
				break;
			}

			case MotionEvent.ACTION_UP: {
				if (lastTouchMenuIndex != -1) { // check that needed action was performed (in fact we had to put it to a queue of needed actions)
					GameData.menuIcons.get(lastTouchMenuIndex).setUntouched();
				}
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
			synchronized (GameData.getLocker()) {
				GameData.drawnPoints.add(new PointF(x, y));
			}
			m_currentDrawingPoint.x = m_startDrawingPoint.x = x;
			m_currentDrawingPoint.y = m_startDrawingPoint.y = y;
			m_maxDist = -1;
			m_drawingIsStarted = true;
			break;

		case MotionEvent.ACTION_MOVE: {
			if (!m_drawingIsStarted)
				break;
			PointF p1 = GameData.drawnPoints.get(GameData.drawnPoints.size() - 1); // prev point
			synchronized (GameData.getLocker()) {
				float dist = PointF.length(x - p1.x, y - p1.y);
				if (dist < 5f) // avoid noise from fingers, slow motions etc
					break;
				m_currentDrawingPoint.x = x;
				m_currentDrawingPoint.y = y;
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
				
				circle.setPosition(cx, cy, cx + r, cy, r);
				
				newPrimitive = circle;
			}
			synchronized (GameData.getLocker()) {
				//GameData.drawing_queue.add(newPrimitive);
				newPrimitive.setMyNumber(GameData.drawing_queue.size() - 1);
				Animation.getInstance().addPrimitive(newPrimitive);
				GameData.drawnPoints.clear();
				if (GameData.drawing_queue.size() == GameData.maxPrimitivesNumber) {
					currentWorkingState = TouchState.DRAGGING;
					GameData.menuPencil.setUnavailable();
					GameData.menuDrag.setTouched();
				}
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
			
			for (DrawingPrimitive primitive : GameData.drawing_queue) {
				primitive.checkTouch(x, y);
				if (primitive.isTouched()) {
					synchronized (GameData.getLocker()) {
						GameData.drawing_queue.remove(primitive); // to make it drawing last
						GameData.drawing_queue.add(primitive);
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
			
			break;
		}
		
		case MotionEvent.ACTION_MOVE: {
			DrawingPrimitive touchedPrimitive = m_gameView.getTouchedPrimitive();
			if (touchedPrimitive != null) {
				synchronized (GameData.getLocker()) {
					touchedPrimitive.applyMove(x, y, mLastTouchX, mLastTouchY, movementIsScaling);
					touchedPrimitive.tryConnection(GameData.drawing_queue);
				}
			}
			break;
		}
		
		case MotionEvent.ACTION_UP: {
			DrawingPrimitive primitive = m_gameView.getTouchedPrimitive();
			
			synchronized (GameData.getLocker()) {
				if (primitive != null) {
					primitive.setUntouched();
					m_gameView.setTouchedPrimitive(null);
					int index = 0;
					int size = GameData.drawing_queue.size();
					for (int i = 0; i < size; i++) {
						DrawingPrimitive pr = GameData.drawing_queue.get(index);
						if (pr.isOutOfBounds()) {
							Animation.getInstance().getCurrentframe().removePrimitive(pr);
							GameData.menuPencil.setAvailable();
						} else {
							pr.setActiveColour();
							index++;
						}
					}
					
				}
			}
			break;
		}
		}	
	}
	
	private void processEventPlaying(MotionEvent event)
	{
		synchronized (GameData.getLocker()) {
			if (Animation.getInstance().getState() == AnimationState.EDIT) {
				currentWorkingState = TouchState.DRAGGING;
			}
		}
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
