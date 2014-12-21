package com.autumncoding.stickman;

import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;

enum Relation {
	PRIMITIVE_IS_PARENT,
	PRIMITIVE_IS_CHILD
}

public class Connection implements Serializable{
	private static final long serialVersionUID = -3556215467308870723L;
	Relation myRelation;
	AbstractDrawingPrimitive primitive;
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

	public void reastoreMyFieldsMyIndexes(LinkedList<AbstractDrawingPrimitive> q, AbstractDrawingPrimitive myPrimitive) {
		primitive = null; // TODO make it efficient
		for (AbstractDrawingPrimitive pr : q) {
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

