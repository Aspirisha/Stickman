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
	private AnimationState m_state = AnimationState.EDIT;
	private Thread m_animationThread = null;
	
	private Animation() {
		m_frames = new ArrayList<AnimationFrame>();
		m_currentFrame = new AnimationFrame();
		m_frames.add(m_currentFrame);
	}
	
	public AnimationState getState() {
		return m_state;
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
	
	public void addPrimitive(DrawingPrimitive pr) {
		AnimationFrame frame = null;
		for (int i = m_currentFrameIndex; i < m_frames.size(); i++) {
			frame = m_frames.get(i);
			if (frame.getPrimitives().size() < GameData.maxPrimitivesNumber) {
				DrawingPrimitive newPrimitive = pr.getCopy();
				frame.addRoot(newPrimitive);
				frame.getPrimitives().add(newPrimitive);
			}
			else
				break;
		}
	}
	
	public void addFrame() {
		m_currentFrame = m_currentFrame.copy();
		m_frames.add(m_currentFrameIndex + 1, m_currentFrame);
	}
	
	public void removeFrame() {
		if (m_frames.size() == 1) {
			m_currentFrame.clear();
			GameData.drawing_queue = m_currentFrame.getPrimitives();
			GameData.prevDrawingQueue = null;
			return;
		}
		m_frames.remove(m_currentFrameIndex);
		if (m_currentFrameIndex > 0) {
			m_currentFrameIndex--;
			if (m_currentFrameIndex > 0) {
				m_prevFrame = m_frames.get(m_currentFrameIndex - 1);
				GameData.prevDrawingQueue = m_prevFrame.getPrimitives();
				
				for (DrawingPrimitive pr : GameData.prevDrawingQueue)
					pr.setUnactiveColour();
			}
		}
		
		m_currentFrame = m_frames.get(m_currentFrameIndex);
		GameData.drawing_queue = m_currentFrame.getPrimitives();
		
		for (DrawingPrimitive pr : GameData.drawing_queue)
			pr.setActiveColour();
	}
	
	private void onStopAnimation() {
		m_state = AnimationState.EDIT;
		GameData.menuPencil.setActive();
		GameData.menuPlay.setActive();
		GameData.menuBin.setAvailable();
		GameData.menuDrag.setTouched();
		GameData.menuNew.setAvailable();
		
		m_animationThread.stop();
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
		if (play && m_state == AnimationState.EDIT) {
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
					while (true) {
						try {
							sleep((long) sleepTime);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						synchronized (GameData.getLocker()) {
							if (!hasNextFrame())
								break;
							switchToNextFrame();
							GameData.prevDrawingQueue = null;
							GameData.menuPrev.setUnavailable();
						}
						
					}
					synchronized (GameData.getLocker()) {
						 onStopAnimation();
					}
					
				}
			};
			m_animationThread.start();
		} else if (!play && m_state == AnimationState.PLAY) {
			synchronized (GameData.getLocker()) {
				 onStopAnimation();
			}
		}
	}
	
	public AnimationFrame switchToNextFrame() {
		if (m_currentFrameIndex < m_frames.size() - 1) {
			m_prevFrame = m_frames.get(m_currentFrameIndex);
			m_currentFrame = m_frames.get(++m_currentFrameIndex);
			GameData.prevDrawingQueue = m_prevFrame.getPrimitives();
			GameData.drawing_queue = m_currentFrame.getPrimitives();
					
			for (DrawingPrimitive pr : GameData.drawing_queue)
				pr.setActiveColour();
			for (DrawingPrimitive pr : GameData.prevDrawingQueue)
				pr.setUnactiveColour();
			
			GameData.menuPrev.setAvailable();
			if (!hasNextFrame())
				GameData.menuNext.setUnavailable();
			return m_currentFrame;
		}
		
		return null;
	}
	
	public AnimationFrame switchToPrevFrame() {
		if (m_currentFrameIndex > 0) {
			m_currentFrame = m_frames.get(--m_currentFrameIndex);
			GameData.drawing_queue = m_currentFrame.getPrimitives();
			
			for (DrawingPrimitive pr : GameData.drawing_queue)
				pr.setActiveColour();
			
			if (m_currentFrameIndex > 0) {
				m_prevFrame = m_frames.get(m_currentFrameIndex - 1);
				GameData.prevDrawingQueue = m_prevFrame.getPrimitives();
				for (DrawingPrimitive pr : GameData.prevDrawingQueue)
					pr.setUnactiveColour();
			}
			else {
				m_prevFrame = null;
				GameData.prevDrawingQueue = null;
				GameData.menuPrev.setUnavailable();
			}
			
			GameData.menuNext.setAvailable();
			return m_currentFrame;
		}
		return null;
	}
	
	public LinkedList<DrawingPrimitive> getFrame(int index) {		
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
	
	public boolean SaveToFile(String fileName) {
		try {
			String state = Environment.getExternalStorageState();
		    if (!Environment.MEDIA_MOUNTED.equals(state)) {
		        return false;
		    }
			ObjectOutputStream ostream = new ObjectOutputStream(new FileOutputStream(fileName));
			
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
	
	public void LoadFormFile(String fileName) {
		ObjectInput input = null;
		try { 
			InputStream istream = new FileInputStream(fileName);
		    InputStream buffer = new BufferedInputStream(istream);
		    input = new ObjectInputStream (buffer);
		    
		    int sz = input.readInt();
		    m_frames.clear();
		    for (int i = 0; i < sz; i++)
		    	m_frames.add((AnimationFrame)input.readObject());
		    for (AnimationFrame frame : m_frames)
		    	frame.restorePrimitivesFieldsByIndexes();
		    
		    m_currentFrameIndex = 0;
		    m_currentFrame = m_frames.get(0); // any saved animation has at least 1 frame
		    for (AnimationFrame frame : m_frames) {
		    	LinkedList<DrawingPrimitive> framePrimitives = frame.getPrimitives();
		    	for (DrawingPrimitive pr: framePrimitives) {
		    		pr.setTransitiveFields(GameData.context);
		    	}
		    }
		    GameData.drawing_queue = m_currentFrame.getPrimitives();
		    GameData.prevDrawingQueue = null;
		} catch (Exception e) { 
			e.printStackTrace();
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
