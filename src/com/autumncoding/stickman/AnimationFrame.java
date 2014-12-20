package com.autumncoding.stickman;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;

import com.autumncoding.stickman.DrawingPrimitive.Connection;
import com.autumncoding.stickman.DrawingPrimitive.Relation;

public class AnimationFrame implements Serializable {
	private static final long serialVersionUID = 4425662302112250971L;
	private LinkedList<DrawingPrimitive> m_primitives;
	private LinkedList<DrawingPrimitive> m_roots;
	
	public void clear() {
		m_roots.clear();
		m_primitives.clear();
	}
	
	public LinkedList<DrawingPrimitive> getPrimitives() {
		return m_primitives;
	}
	
	public LinkedList<DrawingPrimitive> getRoots() {
		return m_roots;
	}
	
	public AnimationFrame() {
		m_primitives = new LinkedList<DrawingPrimitive>();
		m_roots = new LinkedList<DrawingPrimitive>();
	}
	
	public void addRoot(DrawingPrimitive root) {
    	m_roots.add(root);
    	root.setTreeNumber(m_roots.size() - 1);
    }
	
	public void addPrimitive(DrawingPrimitive pr) {
		m_primitives.add(pr);
		pr.setMyNumber(m_primitives.size() - 1);
	}
	

    public DrawingPrimitive getRootWithBiggestTreeNumber() {
    	DrawingPrimitive ret = null;
    	int maxNumber = -1;
    	for (DrawingPrimitive pr : m_roots) {
    		if (pr.getTreeNumber() > maxNumber) {
    			maxNumber = pr.getTreeNumber();
    			ret = pr;
    		}
    	}
    	
    	return ret; // return m_treesNumber;
    }
    
    public int getTreesNumber() {
    	return m_roots.size();
    }
    
    public boolean removeRoot(DrawingPrimitive root) {
    	boolean retVal = m_roots.remove(root);
    	
    	if (!retVal)
    		return false;
    	int oldNumber = root.getTreeNumber();
    	for (DrawingPrimitive pr : m_roots) {
    		if (pr.getTreeNumber() >= oldNumber) {
    			pr.updateSubtreeNumber(pr.getTreeNumber() - 1);
    		}
    	}
    	return true;
    }
	
	public AnimationFrame copy() {
		AnimationFrame newFrame = new AnimationFrame();
		
		for (int i = 0; i < m_primitives.size(); ++i) {
			DrawingPrimitive newPrimitive = m_primitives.get(i).getCopy();
			newFrame.m_primitives.add(newPrimitive);
			if (m_roots.contains(m_primitives.get(i)))
				newFrame.m_roots.add(newPrimitive);
		}
		
		for (int i = 0; i < m_primitives.size(); ++i) {
			DrawingPrimitive oldPrimitive = m_primitives.get(i);
			DrawingPrimitive newPrimitive = newFrame.m_primitives.get(i);
			
			for (Connection con : oldPrimitive.getMyConnections()) {
				int primitiveIndex = m_primitives.indexOf(con.primitive);
				int myJointIndex = oldPrimitive.getMyJoints().indexOf(con.myJoint);
				int primJointIndex = con.primitive.getMyJoints().indexOf(con.primitiveJoint);
				
				Connection newCon = new Connection();
				newCon.myJoint = newPrimitive.getMyJoints().get(myJointIndex);
				newCon.primitiveJoint = newFrame.m_primitives.get(primitiveIndex).getMyJoints().get(primJointIndex);
				newCon.myRelation = con.myRelation;
				newCon.primitive = newFrame.m_primitives.get(primitiveIndex);
				newPrimitive.getMyConnections().add(newCon);
				
				if (newCon.myRelation == Relation.PRIMITIVE_IS_PARENT) {
					newCon.myJoint.connectToParent(newCon.primitiveJoint);
					newCon.primitiveJoint.addChild(newCon.myJoint);
				} 
			}
		}
		
		return newFrame;
	}
	
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeInt(m_primitives.size());
		for (DrawingPrimitive pr : m_primitives)
			stream.writeObject(pr);
	}
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		int sz = stream.readInt();
		m_primitives = new LinkedList<DrawingPrimitive>();
		for (int i = 0; i < sz; i++)
			m_primitives.add((DrawingPrimitive)stream.readObject());
		m_roots = new LinkedList<DrawingPrimitive>();
		
	}
	
	public void restorePrimitivesFieldsByIndexes(){
		for (DrawingPrimitive pr : m_primitives) {
			pr.restoreMyFieldsByIndexes(m_primitives);
			if (!pr.hasParent())
				m_roots.add(pr);
		}
	}
	
	void removePrimitive(DrawingPrimitive pr) {
		pr.disconnectFromEverybody();
		m_primitives.remove(pr);
		removeRoot(pr);
		for (DrawingPrimitive v : m_primitives) {
			if (v.getMyNumber() > pr.getMyNumber())
				v.setMyNumber(v.getMyNumber() - 1);
		}
	}
}