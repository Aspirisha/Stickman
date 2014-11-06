package com.autumncoding.stickman;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;

import com.autumncoding.stickman.DrawingPrimitive.Connection;

public class AnimationFrame implements Serializable {
	private static final long serialVersionUID = 4425662302112250971L;
	private LinkedList<DrawingPrimitive> m_primitives;
	private ArrayList<DrawingPrimitive> roots;
	public LinkedList<DrawingPrimitive> getPrimitives() {
		return m_primitives;
	}
	
	public AnimationFrame() {
		m_primitives = new LinkedList<DrawingPrimitive>();
		roots = new ArrayList<DrawingPrimitive>();
	}
	
	public void addRoot(DrawingPrimitive root) {
    	roots.add(root);
    }
	

    public DrawingPrimitive getRootWithBiggestTreeNumber() {
    	DrawingPrimitive ret = null;
    	int maxNumber = -1;
    	for (DrawingPrimitive pr : roots) {
    		if (pr.getTreeNumber() > maxNumber) {
    			maxNumber = pr.getTreeNumber();
    			ret = pr;
    		}
    	}
    	
    	return ret;
    }
    
    public int getTreesNumber() {
    	return roots.size();
    }
    
    public boolean removeRoot(DrawingPrimitive root) {
    	for (DrawingPrimitive pr : roots) {
    		if (pr.getTreeNumber() >= root.getTreeNumber()) {
    			pr.updateSubtreeNumber(pr.getTreeNumber() - 1);
    		}
    	}
    	return roots.remove(root);
    }
	
	public AnimationFrame copy() {
		AnimationFrame newFrame = new AnimationFrame();
		
		for (int i = 0; i < m_primitives.size(); ++i) {
			DrawingPrimitive newPrimitive = m_primitives.get(i).getCopy();
			newFrame.m_primitives.add(newPrimitive);
		}
		
		for (int i = 0; i < m_primitives.size(); ++i) {
			DrawingPrimitive oldPrimitive = m_primitives.get(i);
			DrawingPrimitive newPrimitive = newFrame.m_primitives.get(i);
			//oldPrimitive.get
		//	for (Connection con : oldPrimitive.)
		}
		
		return newFrame;
	}
}