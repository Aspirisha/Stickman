package com.autumncoding.stickman;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;

import android.content.Context;
import android.graphics.Canvas;

public interface DrawingPrimitive  {
	enum Relation {
		PRIMITIVE_IS_PARENT,
		PRIMITIVE_IS_CHILD
	}
	
	public class Connection implements Serializable{
		private static final long serialVersionUID = -3556215467308870723L;
		Relation myRelation;
		DrawingPrimitive primitive;
		Joint myJoint;
		Joint primitiveJoint;
		
		private int ___tempMyJointIndex;
		private int ___tempPrimitiveJointIndex;
		private int ___tempPrimitiveIndex;
		
		private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
			stream.writeObject(myRelation);
			stream.writeInt(primitive.getMyNumber());
			stream.writeInt(myJoint.getMyPrimitive().getMyJoints().indexOf(myJoint));
			stream.writeInt(primitive.getMyJoints().indexOf(primitiveJoint));
		}

		private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
			myRelation = (Relation)stream.readObject();
			___tempPrimitiveIndex = stream.readInt();
			___tempMyJointIndex = stream.readInt();
			___tempPrimitiveJointIndex = stream.readInt();
		}

		public void reastoreMyFieldsMyIndexes(LinkedList<DrawingPrimitive> q, DrawingPrimitive myPrimitive) {
			primitive = null; // TODO make it efficient
			for (DrawingPrimitive pr : q) {
				if (pr.getMyNumber() == ___tempPrimitiveIndex) {
					primitive = pr;
					break;
				}
			}
			myJoint = myPrimitive.getMyJoints().get(___tempMyJointIndex);
			primitiveJoint = primitive.getMyJoints().get(___tempPrimitiveJointIndex);
			
			if (myRelation == Relation.PRIMITIVE_IS_CHILD) {
				myJoint.addChild(primitiveJoint);
				primitiveJoint.connectToParent(myJoint);
				//myJoint.setMyPrimitive((AbstractDrawingPrimitive)myPrimitive);
			} else {
				primitiveJoint.addChild(myJoint);
				myJoint.connectToParent(primitiveJoint);
			}
		}
		
	}
	public boolean checkTouch(float touch_x, float touch_y);
	public void draw(Canvas canvas);
	public void rotate(float fi, float cx, float cy);
	public void translate(float dx, float dy);
	public void scale(float cx, float cy, float rate);
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
	void ConnectToParent(DrawingPrimitive primitive, Joint myJoint, Joint primitiveJoint);
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
	public void disconnectFromEverybody();
	
	public void applyMove(float new_x, float new_y, float prev_x, float prev_y, boolean isScaling);
	public void copy(DrawingPrimitive p);
	
	public ArrayList<Joint> getMyJoints();
	public void updateSubtreeNumber(int number);
	public int getTreeNumber();
	public void setTreeNumber(int number);
	public boolean isOutOfBounds();
	public void setTransitiveFields(Context context);
	public DrawingPrimitive getCopy();
	public ArrayList<Connection> getMyConnections();
	public void setActiveColour();
	public void setUnactiveColour();
	public void setMyNumber(int newNumber);
	public int getMyNumber();
	public void restoreMyFieldsByIndexes(LinkedList<DrawingPrimitive> q);
	public boolean hasParent();
	
	public  void checkOutOfBounds();
	enum PrimitiveType {
		STICK,
		CIRCLE,
	};
	
	PrimitiveType GetType();
	Context getContext();
	
}
