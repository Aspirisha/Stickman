package com.autumncoding.stickman;

import java.util.ArrayList;
import java.util.LinkedList;

import android.graphics.Canvas;

public interface DrawingPrimitive  {
	enum Relation {
		PRIMITIVE_IS_PARENT,
		PRIMITIVE_IS_CHILD
	}
	
	public class Connection {
		Relation myRelation;
		DrawingPrimitive primitive;
		Joint myJoint;
		Joint primitiveJoint;
	}
	public boolean checkTouch(float touch_x, float touch_y);
	public void draw(Canvas canvas);
	public void rotate(float fi, float cx, float cy);
	public void translate(float dx, float dy);
	public float distTo(DrawingPrimitive primitive);
	public boolean isTouched();
	public float getDistToMe(float from_x, float from_y);
	public void setUntouched();
	
	public boolean tryConnection(LinkedList<DrawingPrimitive> neighbours);
	/**
	 * 
	 * @param primitive will be set as a parent for <this>, if no cycle appears because of it.
	 * Else, no connection appears.
	 * @return true, if new connection appeared
	 */
	public boolean connectToParent(DrawingPrimitive primitive);
	/**
	 * There is asymmetry in this function and previous one: the reason is that active primitive 
	 * (primitive, that creates connection) will be always child in this new connection. So, he just
	 * calls this function in ConnectToParent function, and there is no need to call this function outside of 
	 * DrawingPrimitive.connectToParent. 
	 * @param primitive Primitive, that will become a child of our primitive
	 * @param myJoint parent joint for this connection, belongs to <this>
	 * @param primitiveJoint is a child joint for this connection, primitiveJoint belongs to <primitive>
	 */
	public void ConnectToChild(DrawingPrimitive primitive, Joint myJoint, Joint primitiveJoint);
	public void disconnectFromChild(DrawingPrimitive p);
	public void disconnectFromParent();
	
	
	public void applyMove(float new_x, float new_y, float prev_x, float prev_y, boolean isScaling);
	public void copy(DrawingPrimitive p);
	
	public ArrayList<Joint> getMyJoints();
	public void setSubtreeVisited(boolean visited);
	public boolean isVisited();
	
	enum PrimitiveType {
		STICK,
		CIRCLE,
	};
	
	PrimitiveType GetType();
}
