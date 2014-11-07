package com.autumncoding.stickman;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * This class represents joints that connect different drawing primitives. 
 * @author Andy
 *
 */
public class Joint implements Serializable {
	private static final long serialVersionUID = 1L;
	private boolean isParentJoint = false;
	private boolean isChildJoint = false;
	private boolean isFreeJoint = true;
	private int distToFreeJoint;
	private Joint m_parent;
	private ArrayList<Joint> m_childrenJoints;
	private AbstractDrawingPrimitive m_primitive; // primitive, to which this joint belongs
	private Vector2DF m_point;
	
	public Joint(AbstractDrawingPrimitive pr, Vector2DF p) {
		m_childrenJoints = new ArrayList<Joint>();
		m_primitive = pr;
		m_point = new Vector2DF(p);
	}
	
	/**
	 * Sets current joint to be free, i.e. it's not connected to anything, and so
	 * it is not a child joint, nor a parent joint.
	 */
	public void setFree() {
		isParentJoint = false;
		isChildJoint = false;
		isFreeJoint = true;
		distToFreeJoint = 0;
		m_parent = null;
		m_childrenJoints.clear();
	}

	/**
	 * Sets current joint to be parent of joint (newChild).
	 * @param newChild new child of this joint
	 */
	public void addChild(Joint newChild) {
		if (!m_childrenJoints.contains(newChild)) {
			m_childrenJoints.add(newChild);
			isParentJoint = true;
			isChildJoint = false;
			isFreeJoint = false;
			m_parent = null;
		}
	}
	
	/**
	 * Sets current joint to be child of joint (newParent)
	 * @param newParent new parent of this joint
	 */
	public void connectToParent(Joint newParent) {
		isParentJoint = false;
		isChildJoint = true;
		isFreeJoint = false;
		m_parent = newParent;
		m_childrenJoints.clear(); 
	}
	
	public boolean isParent() {
		return isParentJoint;
	}
	
	public boolean isFree() {
		return isFreeJoint;
	}
	
	public boolean isChild() {
		return isChildJoint;
	}

	public int getDistToFreeJoint() {
		return distToFreeJoint;
	}
	
	public void setDistToFreeJoint(int dist) {
		distToFreeJoint = dist;
	}
	
	public Joint getMyParent() {
		return m_parent;
	}
	
	public ArrayList<Joint> getMyChildren() {
		return m_childrenJoints;
	}
	
	public DrawingPrimitive getMyPrimitive() {
		return m_primitive;
	}
	
	public Vector2DF getMyPoint() {
		return m_point;
	}
	
	public void setMyPoint(Vector2DF p) {
		m_point.x = p.x;
		m_point.y = p.y;
	}
	
	public void rotate(float fi, float cx, float cy) {
		float new_x = (float) (cx + (m_point.x - cx) * Math.cos(fi) - (m_point.y - cy) * Math.sin(fi));
		float new_y = (float) (cy + (m_point.x - cx) * Math.sin(fi) + (m_point.y - cy) * Math.cos(fi));
		m_point.x = new_x;
		m_point.y = new_y;
	}
	
	public void translate() {
		
	}
	
	public void removeChild(Joint ch) {
		m_childrenJoints.remove(ch);
		if (m_childrenJoints.isEmpty()) {
			isFreeJoint = true;
			isParentJoint = false;
		}
	}
}