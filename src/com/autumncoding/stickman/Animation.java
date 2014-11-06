package com.autumncoding.stickman;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
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

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class Animation implements Serializable {
	private static final long serialVersionUID = -1101650775627736580L;
	private static Animation instance = new Animation();
	private Context context = null;
	
	private Animation() {
		m_frames = new ArrayList<Animation.AnimationFrame>();
		m_frames.add(new AnimationFrame());
	}
	
	public static Animation getInstance() {
		return instance;
	}
	
	public void setContext(Context c) {
		context = c;
	}
	
	public class AnimationFrame implements Serializable {
		private static final long serialVersionUID = 4425662302112250971L;
		public AnimationFrame() {
			m_primitives = new LinkedList<DrawingPrimitive>();
		}
		private LinkedList<DrawingPrimitive> m_primitives;
		public LinkedList<DrawingPrimitive> getPrimitives() {
			return m_primitives;
		}
	}
	private ArrayList<AnimationFrame> m_frames;
	
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
		    File file = new File(context.getExternalFilesDir(null), "StickmanSaves");
		    if (!file.mkdirs()) {
		        Log.i("Saving data", "Directory not created");
		    }
		    Log.e("File name: ", file.getAbsolutePath() + "/" + fileName);
			ObjectOutputStream ostream = new ObjectOutputStream(new FileOutputStream(file.getAbsolutePath() + "/" + fileName));
			ostream.writeObject(instance);
			ostream.close();
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("Saving data", "Exception caught");
		}
		
		return true;
	}
	
	public void LoadFormFile(String fileName) {
		ObjectInput input = null;
		File file = new File(context.getExternalFilesDir(null), "StickmanSaves");
	    if (!file.mkdirs()) {
	        Log.i("Saving data", "Directory not created");
	    }
		try { 
			InputStream istream = new FileInputStream(file.getAbsolutePath() + "/" + fileName);
		    InputStream buffer = new BufferedInputStream(istream);
		    input = new ObjectInputStream (buffer);
		    instance = (Animation)input.readObject();
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
