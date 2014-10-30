package com.autumncoding.stickman;

public class Joint {
	private boolean isParentJoint = false;
	private boolean isChildJoint = false;
	private boolean isFreeJoint = true;
	private int distToFreeJoint;
	private Joint m_parent;
	private DrawingPrimitive myPrimitive;
	
	public void setFree() {
		isParentJoint = false;
		isChildJoint = false;
		isFreeJoint = true;
		distToFreeJoint = 0;
		m_parent = null;
	}

	public void setParent() {
		isParentJoint = true;
		isChildJoint = false;
		isFreeJoint = false;
		m_parent = null;
	}
	
	public void setChild(Joint newParent) {
		isParentJoint = false;
		isChildJoint = true;
		isFreeJoint = false;
		m_parent = newParent;
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
	
}