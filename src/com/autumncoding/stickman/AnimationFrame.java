package com.autumncoding.stickman;

import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;

public class AnimationFrame implements Serializable {
	private static final long serialVersionUID = 4425662302112250971L;
	private LinkedList<AbstractDrawingPrimitive> m_primitives;
	private LinkedList<AbstractDrawingPrimitive> m_roots;
	
	public void clear() {
		m_roots.clear();
		m_primitives.clear();
	}
	
	public LinkedList<AbstractDrawingPrimitive> getPrimitives() {
		return m_primitives;
	}
	
	public LinkedList<AbstractDrawingPrimitive> getRoots() {
		return m_roots;
	}
	
	public AnimationFrame() {
		m_primitives = new LinkedList<AbstractDrawingPrimitive>();
		m_roots = new LinkedList<AbstractDrawingPrimitive>();
	}
	
	public void addRoot(AbstractDrawingPrimitive root) {
    	m_roots.add(root);
    	root.updateSubtreeNumber(m_roots.size());
    }
	
	public void addPrimitive(AbstractDrawingPrimitive pr) {
		m_primitives.add(pr);
		pr.setMyNumber(m_primitives.size() - 1);
	}
	
    public int getTreesNumber() {
    	return m_roots.size();
    }
    
    public boolean removeRoot(AbstractDrawingPrimitive root) {
    	boolean retVal = m_roots.remove(root);
    	
    	if (!retVal)
    		return false;
    	int oldNumber = root.getTreeNumber();
    	for (AbstractDrawingPrimitive pr : m_roots) {
    		if (pr.getTreeNumber() > oldNumber) {
    			pr.updateSubtreeNumber(pr.getTreeNumber() - 1);
    		}
    	}
    	return true;
    }
	
	public AnimationFrame copy() {
		AnimationFrame newFrame = new AnimationFrame();
		
		for (int i = 0; i < m_primitives.size(); ++i) {
			AbstractDrawingPrimitive currentPrimitive = m_primitives.get(i);
			AbstractDrawingPrimitive newPrimitive = currentPrimitive.getCopy();
			newFrame.m_primitives.add(newPrimitive);
			AbstractDrawingPrimitive.setSuccessorAndPredecessor(currentPrimitive.m_successor, newPrimitive);
			AbstractDrawingPrimitive.setSuccessorAndPredecessor(newPrimitive, currentPrimitive);
			if (m_roots.contains(currentPrimitive))
				newFrame.m_roots.add(newPrimitive);
		}
		
		for (int i = 0; i < m_primitives.size(); ++i) {
			AbstractDrawingPrimitive oldPrimitive = m_primitives.get(i);
			AbstractDrawingPrimitive newPrimitive = newFrame.m_primitives.get(i);
			
			// update rotation centre
			Joint oldRotationCentre = oldPrimitive.m_rotationCentre;
			AbstractDrawingPrimitive oldRotationPrimitive = oldRotationCentre.getMyPrimitive();
			int rotationJointIndex = oldRotationPrimitive.getMyJoints().indexOf(oldRotationCentre);
			newPrimitive.m_rotationCentre = oldRotationPrimitive.m_successor.joints.get(rotationJointIndex);
			
			// update connections
			for (Connection con : oldPrimitive.getMyChildrenConnections()) {
				int primitiveIndex = m_primitives.indexOf(con.primitive);
				int myJointIndex = oldPrimitive.getMyJoints().indexOf(con.myJoint);
				int primJointIndex = con.primitive.getMyJoints().indexOf(con.primitiveJoint);
				
				Connection newCon = new Connection();
				newCon.myJoint = newPrimitive.getMyJoints().get(myJointIndex);
				newCon.primitiveJoint = newFrame.m_primitives.get(primitiveIndex).getMyJoints().get(primJointIndex);
				newCon.myRelation = con.myRelation;
				newCon.primitive = newFrame.m_primitives.get(primitiveIndex);
				
				// managing joints
				newCon.myJoint.addChild(newCon.primitiveJoint);
				newCon.primitiveJoint.connectToParent(newCon.myJoint);
				newPrimitive.addChildConnection(newCon);
			}
			
			// copying parent connection if exists
			if (oldPrimitive.hasParent()) {
				Connection con = oldPrimitive.getMyParentConnection();
				
				int primitiveIndex = m_primitives.indexOf(con.primitive);
				int myJointIndex = oldPrimitive.getMyJoints().indexOf(con.myJoint);
				int primJointIndex = con.primitive.getMyJoints().indexOf(con.primitiveJoint);
				
				Connection newCon = new Connection();
				newCon.myJoint = newPrimitive.getMyJoints().get(myJointIndex);
				newCon.primitiveJoint = newFrame.m_primitives.get(primitiveIndex).getMyJoints().get(primJointIndex);
				newCon.myRelation = Relation.PRIMITIVE_IS_PARENT;
				newCon.primitive = newFrame.m_primitives.get(primitiveIndex);
				newPrimitive.setParentConnection(newCon);
				
				// managing joints
				newCon.myJoint.connectToParent(newCon.primitiveJoint);
				newCon.primitiveJoint.addChild(newCon.myJoint);
			}
		}
		
		for (AbstractDrawingPrimitive pr : newFrame.m_primitives)
			pr.updateJointColors();
		return newFrame;
	}
	
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeInt(m_primitives.size());
		for (AbstractDrawingPrimitive pr : m_primitives)
			stream.writeObject(pr);
	}
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		int sz = stream.readInt();
		m_primitives = new LinkedList<AbstractDrawingPrimitive>();
		for (int i = 0; i < sz; i++)
			m_primitives.add((AbstractDrawingPrimitive)stream.readObject());
		m_roots = new LinkedList<AbstractDrawingPrimitive>();
		
	}
	
	public void restorePrimitivesFieldsByIndexes(LinkedList<AbstractDrawingPrimitive> nextPrimitives){
		for (AbstractDrawingPrimitive pr : m_primitives) {
			pr.restoreMyFieldsByIndexes(m_primitives, nextPrimitives);
			if (!pr.hasParent())
				m_roots.add(pr);
		}
		for (AbstractDrawingPrimitive pr : m_primitives) {
			pr.updateJointColors();
		}
		
	}
	
	void removePrimitive(AbstractDrawingPrimitive pr) {
		pr.disconnectBeforeDeleting();
		m_primitives.remove(pr);
		removeRoot(pr);
		if (pr.m_successor != null) {
			pr.m_successor.m_predecessor = null;
		}
		if (pr.m_predecessor != null) {
			pr.m_predecessor.m_successor = null;
		}
		
		for (AbstractDrawingPrimitive v : m_primitives) {
			if (v.getMyNumber() > pr.getMyNumber())
				v.setMyNumber(v.getMyNumber() - 1);
		}
	}
}