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
	private int currentFrameIndex = 0;
	private AnimationFrame m_currentFrame = null;
	
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
	
	public void addFrame() {
		m_frames.add(currentFrameIndex + 1, m_currentFrame.copy());
	}
	
	public AnimationFrame getNextFrame() {
		if (currentFrameIndex < m_frames.size() - 1)
			return m_frames.get(++currentFrameIndex);
		return null;
	}
	
	public AnimationFrame getPrevFrame() {
		if (currentFrameIndex > 0)
			return m_frames.get(--currentFrameIndex);
		return null;
	}
	
	public LinkedList<DrawingPrimitive> getFrame(int index) {		
		if (index >= m_frames.size() || index < 0)
			return null;
		
		return m_frames.get(index).getPrimitives();
	}
	
	public boolean SaveToFile(String fileName) {
		try {
			String state = Environment.getExternalStorageState();
		    if (!Environment.MEDIA_MOUNTED.equals(state)) {
		        return false;
		    }
		    File file = new File(GameData.context.getExternalFilesDir(null), "StickmanSaves");
		    if (!file.mkdirs()) {
		        Log.i("Saving data", "Directory not created");
		    }
			ObjectOutputStream ostream = new ObjectOutputStream(new FileOutputStream(file.getAbsolutePath() + "/" + fileName));
			ostream.writeObject(m_frames);
			ostream.close();
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("Saving data", "Exception caught");
		}
		
		return true;
	}
	
	public void LoadFormFile(String fileName) {
		ObjectInput input = null;
		File file = new File(GameData.context.getExternalFilesDir(null), "StickmanSaves");
	    if (!file.mkdirs()) {
	        Log.i("Saving data", "Directory not created");
	    }
		try { 
			InputStream istream = new FileInputStream(file.getAbsolutePath() + "/" + fileName);
		    InputStream buffer = new BufferedInputStream(istream);
		    input = new ObjectInputStream (buffer);
		    m_frames = (ArrayList<AnimationFrame>)input.readObject();
		    m_currentFrame = m_frames.get(0); // any saved animation has at least 1 frame
		    for (AnimationFrame frame : m_frames) {
		    	LinkedList<DrawingPrimitive> framePrimitives = frame.getPrimitives();
		    	for (DrawingPrimitive pr: framePrimitives) {
		    		pr.setTransitiveFields(GameData.context);
		    	}
		    }
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
