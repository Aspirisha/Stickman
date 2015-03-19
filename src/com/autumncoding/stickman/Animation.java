package com.autumncoding.stickman;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;

import com.autamncoding.stickman.R;
import com.autumncoding.stickman.TouchEventThread.TouchState;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Environment;
import android.util.Log;



public class Animation implements Serializable {
	enum AnimationState {
		EDIT,
		PLAY
	}
	private static final long serialVersionUID = -1101650775627736580L;
	private static Animation instance = new Animation();
	private ArrayList<AnimationFrame> m_frames;
	private int m_currentFrameIndex = 0;
	private AnimationFrame m_currentFrame = null;
	private AnimationFrame m_prevFrame = null;
	private float m_fps = 1;
	private volatile AnimationState m_state = AnimationState.EDIT;
	private Thread m_animationThread = null;
	private long m_frameStartTime = 0;
	private LinkedList<AbstractDrawingPrimitive> m_drawnOnCurrentFrameQueue = null;
	
	private Animation() {
		m_frames = new ArrayList<AnimationFrame>();
		m_currentFrame = new AnimationFrame();
		m_drawnOnCurrentFrameQueue = new LinkedList<AbstractDrawingPrimitive>();
		m_frames.add(m_currentFrame);
	}
	
	public int getFps() {
		return (int) m_fps;
	}
	
	public AnimationState getState() {
		return m_state;
	}
	
	public boolean isPlaying() {
		return (m_state == AnimationState.PLAY);
	}
	
	public float getTimePassedFromFrameStart() {
		long t = System.currentTimeMillis();
		float r = (t - m_frameStartTime) * m_fps / 1000f;
		return (r < 1.0f ? r : 1.0f);
	}
	
	public void setAnimationFPS(float fps) {
		if (fps > R.integer.max_animation_fps || fps < 1)
			return;
		m_fps = fps;
	}
	
	public static Animation getInstance() {
		return instance;
	}
	
	public AnimationFrame getCurrentframe() {
		return m_currentFrame;
	}
	
	public AnimationFrame getPrevframe() {
		return m_prevFrame;
	}
	
	public int getCurrentFrameNumber() {
		return m_currentFrameIndex + 1;
	}
	
	public int getFramesNumber() {
		return m_frames.size();
	}
	
	public void addPrimitive(AbstractDrawingPrimitive pr) {
		AnimationFrame frame = null;
		
		
		if (m_currentFrame.getPrimitives().size() < GameData.maxPrimitivesNumber) {
			m_drawnOnCurrentFrameQueue.push(pr);
			AbstractDrawingPrimitive.setSuccessorAndPredecessor(pr, null);
			AbstractDrawingPrimitive prev = pr;
			m_currentFrame.addRoot(pr);
			m_currentFrame.getPrimitives().add(pr);
			
			for (int i = m_currentFrameIndex + 1; i < m_frames.size(); i++) {
				frame = m_frames.get(i);
				if (frame.getPrimitives().size() < GameData.maxPrimitivesNumber) {
					AbstractDrawingPrimitive newPrimitive = pr.getCopy();
					AbstractDrawingPrimitive.setSuccessorAndPredecessor(newPrimitive, prev);
					prev = newPrimitive;
					frame.addRoot(newPrimitive);
					frame.getPrimitives().add(newPrimitive);
				}
				else
					break;
			}
		}

	}
	
	public void removePrimitive(AbstractDrawingPrimitive pr) {
		AnimationFrame frame = null;
		AbstractDrawingPrimitive curPrim = pr;
		for (int i = m_currentFrameIndex; i < m_frames.size() && curPrim != null; i++) {
			frame = m_frames.get(i);
			frame.removePrimitive(curPrim);
			curPrim = curPrim.m_successor;
		}
		
		m_drawnOnCurrentFrameQueue.remove(pr);
	}
	
	public void addFrame() {
		m_prevFrame = m_currentFrame;
		m_currentFrame = m_currentFrame.copy();
		m_frames.add(m_currentFrameIndex + 1, m_currentFrame);
		m_currentFrameIndex++;
		GameData.drawing_queue = m_currentFrame.getPrimitives();
		GameData.prevDrawingQueue = m_prevFrame.getPrimitives();
		for (AbstractDrawingPrimitive pr : GameData.prevDrawingQueue)
			pr.setUnactiveColour();
		m_drawnOnCurrentFrameQueue.clear();
		GameData.framesChanged = true;
	}
	
	public void removeFrame() {
		if (!m_drawnOnCurrentFrameQueue.isEmpty()) {
			AbstractDrawingPrimitive pr = m_drawnOnCurrentFrameQueue.poll();
			if (m_currentFrame.getPrimitives().contains(pr))
				removePrimitive(pr);
			return;
		}
		
		if (m_frames.size() == 1) {
			m_currentFrame.clear();
			GameData.drawing_queue = m_currentFrame.getPrimitives();
			GameData.prevDrawingQueue = null;
			return;
		}
		m_frames.remove(m_currentFrameIndex);
		GameData.framesChanged = true;
		if (m_currentFrameIndex > 0) {
			m_currentFrameIndex--;
			if (m_currentFrameIndex > 0) {
				m_prevFrame = m_frames.get(m_currentFrameIndex - 1);
				GameData.prevDrawingQueue = m_prevFrame.getPrimitives();
				
				for (AbstractDrawingPrimitive pr : GameData.prevDrawingQueue)
					pr.setUnactiveColour();
			}
		}
		
		m_currentFrame = m_frames.get(m_currentFrameIndex);
		GameData.drawing_queue = m_currentFrame.getPrimitives();
		
		for (AbstractDrawingPrimitive pr : GameData.drawing_queue)
			pr.setActiveColour();
	}
	
	void clear() {
		int framesNumber = m_frames.size();
		for (int i = 0; i < framesNumber; i++)
			removeFrame();
		if (framesNumber == 0) {
			m_currentFrame = new AnimationFrame();
			m_frames.add(m_currentFrame);
			m_prevFrame = null;
			m_currentFrameIndex = 0;
		}
	}
	
	public void stopAnimation() {
		m_state = AnimationState.EDIT;
	}
	
	private void onStopAnimation() {
		GameData.menuPencil.setActive();
		GameData.menuPlay.setActive();
		GameData.menuBin.setAvailable();
		GameData.menuDrag.setTouched();
		GameData.menuNew.setAvailable();
		GameData.touchState = TouchState.DRAGGING;
		
		m_animationThread = null;
		if (m_currentFrameIndex > 0) {
			m_prevFrame = m_frames.get(m_currentFrameIndex - 1);
			GameData.prevDrawingQueue = m_prevFrame.getPrimitives();
			GameData.menuPrev.setAvailable();
		}
		if (hasNextFrame())
			GameData.menuNext.setAvailable();
	}
	
	public void Play(boolean play) {
		if (m_state == AnimationState.EDIT && play) {
			m_state = AnimationState.PLAY;
			m_animationThread = new Thread() {
				@Override
				public void run() {
					float sleepTime = (long) (1000f / m_fps);
					GameData.menuPencil.setUnavailable();
					GameData.menuBin.setUnavailable();
					GameData.menuDrag.setUnavailable();
					GameData.menuNew.setUnavailable();
					GameData.menuPrev.setUnavailable();
					GameData.menuNext.setUnavailable();
					
					// remote to the first frame
					m_currentFrame = m_frames.get(0);
					m_currentFrameIndex = 0;
					m_prevFrame = null;
					
					GameData.currentFrameIndex = 1;
					GameData.framesChanged = true;
					GameData.prevDrawingQueue = null;
					GameData.drawing_queue = m_currentFrame.getPrimitives();
							
					for (AbstractDrawingPrimitive pr : GameData.drawing_queue)
						pr.setActiveColour();
					if (hasNextFrame()) {
						AnimationFrame nextFrame = m_frames.get(m_currentFrameIndex + 1);
						for (AbstractDrawingPrimitive pr : nextFrame.getPrimitives())
							pr.setActiveColour();
					}					
					while (m_state == AnimationState.PLAY) {
						m_frameStartTime = System.currentTimeMillis();
						try {
							sleep((long) sleepTime);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if (m_state != AnimationState.PLAY)
							break;
						synchronized (GameData.getLocker()) {
							if (!hasNextFrame()) {
								if (!GameData.playInLoop)
									break;
								setCurrentframe(0);
								GameData.menuNext.setUnavailable();
							} else	
								switchToNextFrame();
						
							// it's needed check for we have switched frame 
							if (hasNextFrame()) {
								AnimationFrame nextFrame = m_frames.get(m_currentFrameIndex + 1);
								for (AbstractDrawingPrimitive pr : nextFrame.getPrimitives())
									pr.setActiveColour();
							}	
							
							GameData.prevDrawingQueue = null;
							//GameData.menuPrev.setAvailable();
						}
						
					}
					synchronized (GameData.getLocker()) {
						m_state = AnimationState.EDIT;
						onStopAnimation();
					}
					
				}
			};
			m_animationThread.start();
		} else if (m_state == AnimationState.PLAY && !play) {
			synchronized (GameData.getLocker()) {
				m_state = AnimationState.EDIT;
				onStopAnimation();
			}
		}
	}
	
	public AnimationFrame switchToNextFrame() {
		if (m_currentFrameIndex < m_frames.size() - 1) {
			m_prevFrame = m_frames.get(m_currentFrameIndex);
			m_currentFrame = m_frames.get(++m_currentFrameIndex);
			
			GameData.currentFrameIndex = m_currentFrameIndex + 1;
			GameData.framesChanged = true;
			GameData.prevDrawingQueue = m_prevFrame.getPrimitives();
			GameData.drawing_queue = m_currentFrame.getPrimitives();
					
			for (AbstractDrawingPrimitive pr : GameData.drawing_queue)
				pr.setActiveColour();
			for (AbstractDrawingPrimitive pr : GameData.prevDrawingQueue)
				pr.setUnactiveColour();
			
			if (m_state != AnimationState.PLAY) {
				GameData.menuPrev.setAvailable();
				if (!hasNextFrame())
					GameData.menuNext.setUnavailable();
			}
			
			m_drawnOnCurrentFrameQueue.clear();
			
			return m_currentFrame;
		}
		
		return null;
	}
	
	public AnimationFrame switchToPrevFrame() {
		if (m_currentFrameIndex > 0) {
			m_currentFrame = m_frames.get(--m_currentFrameIndex);
			GameData.currentFrameIndex = m_currentFrameIndex + 1;
			GameData.drawing_queue = m_currentFrame.getPrimitives();
			
			GameData.framesChanged = true;
			
			for (AbstractDrawingPrimitive pr : GameData.drawing_queue)
				pr.setActiveColour();
			
			if (m_currentFrameIndex > 0) {
				m_prevFrame = m_frames.get(m_currentFrameIndex - 1);
				GameData.prevDrawingQueue = m_prevFrame.getPrimitives();
				for (AbstractDrawingPrimitive pr : GameData.prevDrawingQueue)
					pr.setUnactiveColour();
			}
			else {
				m_prevFrame = null;
				GameData.prevDrawingQueue = null;
				GameData.menuPrev.setUnavailable();
			}
			
			GameData.menuNext.setAvailable();
			m_drawnOnCurrentFrameQueue.clear();
			return m_currentFrame;
		}
		return null;
	}
	
	public LinkedList<AbstractDrawingPrimitive> getFrame(int index) {		
		if (index >= m_frames.size() || index < 0)
			return null;
		
		return m_frames.get(index).getPrimitives();
	}
	
	public boolean hasNextFrame() {
		return (m_currentFrameIndex < m_frames.size() - 1);
	}
	
	public boolean hasPrevFrame() {
		return (m_currentFrameIndex > 0);
	}
	
	// can be called only by seekBar in main activity so son't update it
	public void setCurrentframe(int frameIndex) {
		if (frameIndex < 0 || frameIndex >= m_frames.size() || frameIndex == m_currentFrameIndex)
			return;
		
		m_currentFrameIndex = frameIndex;
		GameData.currentFrameIndex = frameIndex + 1;
		
		m_currentFrame = m_frames.get(m_currentFrameIndex);
		GameData.drawing_queue = m_currentFrame.getPrimitives();
		
		GameData.framesChanged = true;
		
		for (AbstractDrawingPrimitive pr : GameData.drawing_queue)
			pr.setActiveColour();
		
		if (m_currentFrameIndex > 0) {
			m_prevFrame = m_frames.get(m_currentFrameIndex - 1);
			GameData.prevDrawingQueue = m_prevFrame.getPrimitives();
			for (AbstractDrawingPrimitive pr : GameData.prevDrawingQueue)
				pr.setUnactiveColour();
		}
		else {
			m_prevFrame = null;
			GameData.prevDrawingQueue = null;
			GameData.menuPrev.setUnavailable();
		}
		
		if (hasNextFrame())
			GameData.menuNext.setAvailable();
		else
			GameData.menuNext.setUnavailable();
		
		if (hasPrevFrame())
			GameData.menuPrev.setAvailable();
		else
			GameData.menuPrev.setUnavailable();
		
		m_drawnOnCurrentFrameQueue.clear();
	}
	
	public void drawFrame(Canvas canvas) {
		float t = getTimePassedFromFrameStart();
		if (getState() == AnimationState.EDIT) {
        	if (GameData.prevDrawingQueue != null) {
	        	for (AbstractDrawingPrimitive pr : GameData.prevDrawingQueue)
	        		pr.draw(canvas);
        	}
	        for (AbstractDrawingPrimitive v : GameData.drawing_queue)
	        	v.drawLine(canvas);
	        for (AbstractDrawingPrimitive v : GameData.drawing_queue)
	        	v.drawJoints(canvas);
    	} else {
    		if (GameData.enableInterpolation) {
    			drawInterpolatedFrame(canvas, t);
    		} else {
    			drawWithoutInterpolation(canvas);
    		}
    	}
	}
	
	private void drawInterpolatedFrame(Canvas canvas, float t) {
		if (hasNextFrame()) {
			for (AbstractDrawingPrimitive v : GameData.drawing_queue)
				v.drawLineBlendingWithSuccessor(canvas, t);
			for (AbstractDrawingPrimitive v : GameData.drawing_queue)
				v.drawJointsBlendingWithSuccessor(canvas, t);
			AnimationFrame nextFrame = m_frames.get(m_currentFrameIndex + 1);
			for (AbstractDrawingPrimitive pr : nextFrame.getPrimitives()) {
				if (pr.m_predecessor == null)
					pr.drawLineBlendingWithNoSuccessor(canvas, 1 - t);
			}
			for (AbstractDrawingPrimitive pr : nextFrame.getPrimitives()) {
				if (pr.m_predecessor == null)
					pr.drawJointsBlendingWithNoSuccessor(canvas, 1 - t);
			}
		} else {
			for (AbstractDrawingPrimitive v : GameData.drawing_queue)
				v.drawLineBlendingWithSuccessor(canvas, 0);
			for (AbstractDrawingPrimitive v : GameData.drawing_queue)
				v.drawJointsBlendingWithSuccessor(canvas, 0);
		}
	}
	
	private void drawWithoutInterpolation(Canvas canvas) {
		for (AbstractDrawingPrimitive v : GameData.drawing_queue)
			v.drawLineBlendingWithSuccessor(canvas, 0);
		for (AbstractDrawingPrimitive v : GameData.drawing_queue)
			v.drawJointsBlendingWithSuccessor(canvas, 0);
	}
	
	public boolean SaveToFile(String fileName, boolean isTemp) {
		try {
			String state = Environment.getExternalStorageState();
		    if (!Environment.MEDIA_MOUNTED.equals(state)) {
		        return false;
		    }
		    ObjectOutputStream ostream = null;
		    if (!isTemp)
		    	ostream = new ObjectOutputStream(new FileOutputStream(fileName));
		    else
		    	ostream = new ObjectOutputStream(GameData.mainActivity.openFileOutput("temp.ani", Context.MODE_PRIVATE));
		    	
		    ostream.writeFloat(GameData.fieldRect.width());
		    ostream.writeFloat(GameData.fieldRect.height());
			ostream.writeInt(m_frames.size());
			
			for (AnimationFrame fr : m_frames) {
				ostream.writeObject(fr);
			}
			ostream.close();
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("Saving data", "Exception caught");
		}
		
		return true;
	}
	
	public void loadFromFile(String fileName, boolean isTemp) throws IOException, ClassNotFoundException {
		ObjectInput input = null;
		try { 
			InputStream istream = null;
			if (!isTemp)
				istream = new FileInputStream(fileName);
			else
				istream = GameData.mainActivity.openFileInput("temp.ani");
			
		    InputStream buffer = new BufferedInputStream(istream);
		    input = new ObjectInputStream (buffer);
		    
		    float hostWidth = input.readFloat();
		    float hostHeight = input.readFloat();
		    
		    float scale = Math.min(GameData.fieldRect.width() / hostWidth, GameData.fieldRect.height() / hostHeight);
		    
		    
		    int sz = input.readInt();
		    m_frames.clear();
		    for (int i = 0; i < sz; i++)
		    	m_frames.add((AnimationFrame)input.readObject());
		    for (int i = 0; i < m_frames.size(); i++) {
		    	AnimationFrame frame = m_frames.get(i);
		    	LinkedList<AbstractDrawingPrimitive> NextDrawingSequence = null;
		    	if (i + 1 < m_frames.size())
		    		NextDrawingSequence = m_frames.get(i + 1).getPrimitives();
		    	
		    	frame.restorePrimitivesFieldsByIndexes(NextDrawingSequence);
		    }
		    
		    
		    m_currentFrame = m_frames.get(0); // any saved animation has at least 1 frame
		    for (AnimationFrame frame : m_frames) {
		    	LinkedList<AbstractDrawingPrimitive> framePrimitives = frame.getPrimitives();
		    	for (AbstractDrawingPrimitive pr: framePrimitives) {
		    		pr.setTransientFields(GameData.mainActivity);
		    		pr.noSaturationScale(0, 0, scale);
		    	}
		    	
		    	for (AbstractDrawingPrimitive pr : frame.getRoots()) {
		    		pr.updateCentre(pr.m_rotationCentre);
		    	}
		    }
		    
		    m_currentFrameIndex = -1;
		    setCurrentframe(0);
		} catch (IOException e) { 
			e.printStackTrace();
			throw e;
		} catch (ClassNotFoundException e) {
			throw e;
		} finally{
	        try {
				input.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	}
}
