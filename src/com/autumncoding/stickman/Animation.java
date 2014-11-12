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
	private static final long serialVersionUID = -1101650775627736580L;
	private static Animation instance = new Animation();
	private ArrayList<AnimationFrame> m_frames;
	private int m_currentFrameIndex = 0;
	private AnimationFrame m_currentFrame = null;
	private AnimationFrame m_prevFrame = null;
	
	private Animation() {
		m_frames = new ArrayList<AnimationFrame>();
		m_currentFrame = new AnimationFrame();
		m_frames.add(m_currentFrame);
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
	
	public void addFrame() {
		m_currentFrame = m_currentFrame.copy();
		m_frames.add(m_currentFrameIndex + 1, m_currentFrame);
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
			}
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
				ostream.flush();
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
