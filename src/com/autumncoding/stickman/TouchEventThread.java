package com.autumncoding.stickman;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

import com.autamncoding.stickman.R;
import com.autumncoding.stickman.Animation.AnimationState;

import android.graphics.PointF;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

public class TouchEventThread extends Thread {
	enum TouchState {
		DRAWING,
		DRAGGING
	}
	
	enum TouchHelpType {
		ON_PLAY_PRESSED,
		ON_DRAW_PRESSED,
		ON_NEXT_PRESSED,
		ON_PREV_PRESSED,
		ON_NEW_PRESSED,
		ON_DELETE_PRESSED,
		ON_DRAW,
		ON_SCALE,
		ON_ROTATE,
		ON_SET_CENTRE,
		ON_SET_ROOT,
		ON_DRAG_PRESSED,
		ON_DRAG_PARENT,
		ON_DRAG_CHILD,
		ON_DROP
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
	private AbstractDrawingPrimitive lastTouchedPrimitive = null;
	private boolean movementIsScaling = false;
	
	private PointF m_startDrawingPoint = null;
	private PointF m_farthestDrawingPoint = null;
	private PointF m_currentDrawingPoint = null;
	private float m_maxDist = -1;
	private boolean m_drawingIsStarted = false;
	private boolean m_drawingHasIntersection = false;
	
	private HashMap<TouchHelpType, Long> lastTimesOfToasts;
	
	class CurrentDrawingState {
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
		lastTimesOfToasts = new HashMap<TouchEventThread.TouchHelpType, Long>();
		
		for (TouchHelpType type : TouchHelpType.values()) {
			lastTimesOfToasts.put(type, 0L);
		}
			
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
			switch (GameData.touchState) {
			case DRAGGING:
				processEventDragging(event, eventTime);
				break;
			case DRAWING: {
				if (GameData.maxPrimitivesNumber > GameData.drawing_queue.size()) {
					processEventDrawing(event);
				}
				break;
			}
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
					GameData.touchState = TouchState.DRAWING;
					GameData.menuDrag.setUntouched();
					GameData.menuPlay.setUntouched();
					showPopupHint(TouchHelpType.ON_DRAW_PRESSED);
					break;
				case 1:
					GameData.touchState = TouchState.DRAGGING;
					GameData.menuPencil.setUntouched();
					GameData.menuPlay.setUntouched();
					showPopupHint(TouchHelpType.ON_DRAG_PRESSED);
					break;
				case 2:
					Animation.getInstance().switchToPrevFrame();
					lastTouchMenuIndex = 2;
					showPopupHint(TouchHelpType.ON_PREV_PRESSED);
					break;
				case 3:
					Animation.getInstance().switchToNextFrame();
					lastTouchMenuIndex = 3;
					showPopupHint(TouchHelpType.ON_NEXT_PRESSED);
					break;
				case 4:
					Animation.getInstance().addFrame();
					GameData.menuPrev.setActive();
					lastTouchMenuIndex = 4;
					showPopupHint(TouchHelpType.ON_NEW_PRESSED);
					break;
				case 5:
					Animation.getInstance().removeFrame();
					if (!Animation.getInstance().hasNextFrame())
						GameData.menuNext.setUnavailable();
					if (!Animation.getInstance().hasPrevFrame())
						GameData.menuPrev.setUnavailable();
					showPopupHint(TouchHelpType.ON_DELETE_PRESSED);
					lastTouchMenuIndex = 5;
					break;
				case 6:
					Animation.getInstance().Play(!Animation.getInstance().isPlaying());
					showPopupHint(TouchHelpType.ON_PLAY_PRESSED);
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
	
	
	private void showPopupHint(final TouchHelpType type) {
		if (!GameData.showPopupHinst)
			return;

		long curTime = System.currentTimeMillis();
		
		if (curTime - lastTimesOfToasts.get(type) < GameData.intervalBetweenSameToasts)
			return;
		
		lastTimesOfToasts.put(type, curTime);
		
		Runnable show_toast = new Runnable() {
			public void run() {
				LinkedList<String> messages = new LinkedList<String>();
				switch (type) {
				case ON_PLAY_PRESSED:
					messages.add(GameData.res.getString(R.string.toast_help2));
					break;
				case ON_DRAG_PRESSED:
					messages.add(GameData.res.getString(R.string.toast_help3));
					break;
				case ON_DRAW:
					messages.add(GameData.res.getString(R.string.toast_help4));
					messages.add(GameData.res.getString(R.string.toast_help17));
					break;
				case ON_ROTATE:
					messages.add(GameData.res.getString(R.string.toast_help5));
					break;
				case ON_DELETE_PRESSED:
					messages.add(GameData.res.getString(R.string.toast_help6));
					break;
				case ON_DRAG_PARENT:
					messages.add(GameData.res.getString(R.string.toast_help7));
					break;
				case ON_DRAW_PRESSED:
					messages.add(GameData.res.getString(R.string.toast_help8));
					break;
				case ON_NEW_PRESSED:
					messages.add(GameData.res.getString(R.string.toast_help9));
					break;
				case ON_NEXT_PRESSED:
					messages.add(GameData.res.getString(R.string.toast_help10));
					break;
				case ON_PREV_PRESSED:
					messages.add(GameData.res.getString(R.string.toast_help11));
					break;
				case ON_DRAG_CHILD:
					messages.add(GameData.res.getString(R.string.toast_help12));
					break;
				case ON_SCALE:
					messages.add(GameData.res.getString(R.string.toast_help13));
					break;
				case ON_SET_CENTRE:
					messages.add(GameData.res.getString(R.string.toast_help14));
					break;
				case ON_SET_ROOT:
					messages.add(GameData.res.getString(R.string.toast_help15));
					break;
				case ON_DROP:
					messages.add(GameData.res.getString(R.string.toast_help16));
					break;
				default:
					break;
				}
				
				for (String msg : messages) {
					final Toast toast = Toast.makeText(GameData.mainActivity, msg, Toast.LENGTH_LONG);
					toast.show();
	
					new CountDownTimer(GameData.helpToastDurationPerLetter * msg.length(), 1000) {
					    public void onTick(long millisUntilFinished) {toast.show();}
					    public void onFinish() {toast.show();}
					}.start();
				}
			}
		};

		GameData.mainActivity.runOnUiThread(show_toast);
	}
	
	private void processEventDrawing(MotionEvent event) {
		if (Animation.getInstance().getState() == AnimationState.PLAY)
			return;
		
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
			showPopupHint(TouchHelpType.ON_DRAW);
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
			
			AbstractDrawingPrimitive newPrimitive = null;
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
				newPrimitive.setMyNumber(GameData.drawing_queue.size());
				Animation.getInstance().addPrimitive(newPrimitive);
				GameData.drawnPoints.clear();
				if (GameData.drawing_queue.size() == GameData.maxPrimitivesNumber) {
					GameData.touchState = TouchState.DRAGGING;
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
	
		
	
	private void processEventDragging(MotionEvent event, long eventTime) {
		if (Animation.getInstance().getState() == AnimationState.PLAY)
			return;
		
		int eventCode = event.getAction() & MotionEvent.ACTION_MASK;
		float x = event.getX();
		float y = event.getY();
		
		switch (eventCode) {
		case MotionEvent.ACTION_DOWN: {
			
			for (AbstractDrawingPrimitive primitive : GameData.drawing_queue) {
				primitive.checkTouch(x, y);
				if (primitive.isTouched()) {
					synchronized (GameData.getLocker()) {
						GameData.drawing_queue.remove(primitive); // to make it drawing last
						GameData.drawing_queue.add(primitive);
						m_gameView.setTouchedPrimitive(primitive);
					}
					
					if (eventTime - lastTouchTime < 400) {
						movementIsScaling = true;
						if (!primitive.hasParent && !primitive.hasChildren())
							showPopupHint(TouchHelpType.ON_SCALE);
						else {
							Joint j = primitive.getTouchedJoint();
							if (j != null) {
								showPopupHint(TouchHelpType.ON_SET_CENTRE);
								primitive.setJointAsCentre(j);
							}
							else {
								showPopupHint(TouchHelpType.ON_SET_ROOT);
								primitive.setAsRoot();
							}
						}
					}
					else {
						movementIsScaling = false;
						if (primitive.getTouchedJoint() != null) 
							showPopupHint(TouchHelpType.ON_ROTATE);
						else {
							if (primitive.hasParent)
								showPopupHint(TouchHelpType.ON_DRAG_CHILD);
							else
								showPopupHint(TouchHelpType.ON_DRAG_PARENT);
						}
					}
					
					lastTouchedPrimitive = primitive;
					lastTouchTime = eventTime;
					break;
				}
			}
			
			break;
		}
		
		case MotionEvent.ACTION_MOVE: {
			AbstractDrawingPrimitive touchedPrimitive = m_gameView.getTouchedPrimitive();
			if (touchedPrimitive != null) {
				synchronized (GameData.getLocker()) {
					touchedPrimitive.applyMove(x, y, mLastTouchX, mLastTouchY, movementIsScaling);
					touchedPrimitive.tryConnection(GameData.drawing_queue);
					if (touchedPrimitive.isOutOfBounds()) {
						showPopupHint(TouchHelpType.ON_DROP);
					}
				}
			}
			break;
		}
		
		case MotionEvent.ACTION_UP: {
			AbstractDrawingPrimitive primitive = m_gameView.getTouchedPrimitive();
			
			synchronized (GameData.getLocker()) {
				if (primitive != null) {					
					primitive.setUntouched();
					m_gameView.setTouchedPrimitive(null);
					int index = 0;
					int size = GameData.drawing_queue.size();
					for (int i = 0; i < size; i++) {
						AbstractDrawingPrimitive pr = GameData.drawing_queue.get(index);
						if (pr.isOutOfBounds()) {
							Animation.getInstance().removePrimitive(pr);
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
