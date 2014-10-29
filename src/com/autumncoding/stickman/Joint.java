package com.autumncoding.stickman;

public class Joint {
	private boolean isParentJoint = false;
	private boolean isChildJoint = false;
	private boolean isFreeJoint = true;
	private int distToFreeJoint;
	private DrawingPrimitive myPrimitive;
	
	public void setFree() {
		isParentJoint = false;
		isChildJoint = false;
		isFreeJoint = true;
		distToFreeJoint = 0;
	}

	public void setParent() {
		isParentJoint = true;
		isChildJoint = false;
		isFreeJoint = false;
	}
	
	public void setChild() {
		isParentJoint = false;
		isChildJoint = true;
		isFreeJoint = false;
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
	
}
