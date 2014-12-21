package com.autumncoding.stickman;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;


import android.content.Context;
import android.graphics.Canvas;

enum PrimitiveType {
	STICK,
	CIRCLE,
};

public abstract class AbstractDrawingPrimitive implements Serializable {
	private static final long serialVersionUID = 6036735876597370696L;
	protected ArrayList<Joint> joints;
	protected boolean hasParent;
	//protected ArrayList<Connection> m_connections;
	protected int m_treeNumber;
	protected boolean isScalable;
	protected int m_number = 0;
	protected transient boolean m_isTouched;
	protected transient boolean m_isOutOfBounds = false;
	protected transient Context m_context;
	protected int m_ancestorsNumber; // not only childrem, all ancestors
	protected Connection m_parentConnection;
	protected ArrayList<Connection> m_childrenConnections;
	
	
	
	abstract void translate(float x, float y);
	abstract public boolean checkTouch(float touch_x, float touch_y);
	abstract public void draw(Canvas canvas);
	abstract public PrimitiveType GetType();
	abstract public float getDistToMe(float from_x, float from_y);
	abstract public void setUntouched();
	abstract public void scale(float cx, float cy, float rate);
	abstract public AbstractDrawingPrimitive getCopy(); 
	abstract public void setActiveColour();
	abstract public void setUnactiveColour();
	abstract public void rotate(float fi, float cx, float cy);
	abstract public void applyMove(float new_x, float new_y, float prev_x, float prev_y, boolean isScaling);
	abstract float distTo(AbstractDrawingPrimitive pr);
	
	
	AbstractDrawingPrimitive(Context context) {
		m_childrenConnections = new ArrayList<Connection>();
		joints = new ArrayList<Joint>();
		m_context = context;
		hasParent = false;
		isScalable = true;
		m_number = Animation.getInstance().getCurrentframe().getPrimitives().size();
		m_isTouched = false;
		m_ancestorsNumber = 0;
		m_parentConnection = null;
	}
	
	public AbstractDrawingPrimitive(AbstractDrawingPrimitive pr) {
		// do NOT copy joints or connection
		m_context = pr.m_context;
		joints = new ArrayList<Joint>();
		m_childrenConnections = new ArrayList<Connection>();
		m_parentConnection = null;
		hasParent = pr.hasParent;
		isScalable = pr.isScalable;
		m_treeNumber = pr.m_treeNumber;
		m_number = pr.m_number;
		m_isTouched = false;
	}

	public Context getContext() {
		return m_context;
	}
	
	public boolean tryConnection(LinkedList<AbstractDrawingPrimitive> neighbours) {
		float min_dist = 10000;
		AbstractDrawingPrimitive closest_primitive = null;
		for (AbstractDrawingPrimitive pr : neighbours) {
			if (pr.getTreeNumber() == m_treeNumber) // we don't want cycles
				continue;
			float cur_dist = distTo(pr);
			if (cur_dist < min_dist) {
				min_dist = cur_dist;
				closest_primitive = pr;
			}
		}

		if (min_dist <= GameData.min_dist_to_connect_square) {
			connectToParent(closest_primitive);
				return true;
		}

		return false;
	}

	
	public boolean isTouched() {
		return m_isTouched;
	}
	
	
	public boolean connectToParent(AbstractDrawingPrimitive primitive) {
		// Do not allow cycles to appear in primitive forest
		if (hasParent || m_treeNumber == primitive.getTreeNumber())
			return false;
		
		Joint newParentJoint = null;
		Joint myChildJoint = null;
		ArrayList<Joint> primitivesJoints = primitive.getMyJoints();
		Vector2DF dv = null;
		float minLen = 1000000000f; // CHANGE then
		for (Joint myJoint : joints) {
			for (Joint externJoint : primitivesJoints) {
				float len = Vector2DF.distSquare(myJoint.getMyPoint(), externJoint.getMyPoint());
				if (len < minLen) {
					minLen = len;
					myChildJoint = myJoint;
					if (!externJoint.isChild())
						newParentJoint = externJoint;
					else
						newParentJoint = externJoint.getMyParent();
				}
			}
		}
		
		dv = Vector2DF.sub(newParentJoint.getMyPoint(), myChildJoint.getMyPoint());
		// here be accurate: 
		// primitive is not a must to be parent! For it can be his parent for example 
		AbstractDrawingPrimitive newParent = newParentJoint.getMyPrimitive(); 
				
		ArrayList<Connection> temp = new ArrayList<Connection>();
		temp.addAll(m_childrenConnections);
		
		for (Connection con : temp) {
			if (con.myJoint == myChildJoint && con.myRelation == Relation.PRIMITIVE_IS_CHILD) {
				Joint ch = con.primitiveJoint;
				ch.connectToParent(newParentJoint);
				newParentJoint.addChild(ch);
				AbstractDrawingPrimitive reattachedChild = ch.getMyPrimitive();
				reattachedChild.disconnectFromParent();
				reattachedChild.ConnectToParent(newParent, ch, newParentJoint);
				newParent.ConnectToChild(reattachedChild, newParentJoint, ch);
				disconnectFromChild(reattachedChild);
				reattachedChild.translate(dv.x, dv.y);
			}
		}

		translate(dv.x, dv.y);

		myChildJoint.connectToParent(newParentJoint);
		newParentJoint.addChild(myChildJoint);
		ConnectToParent(newParent, myChildJoint, newParentJoint);
		newParent.ConnectToChild(this, newParentJoint, myChildJoint);
		
		hasParent = true;
		isScalable = false;
		
		Animation.getInstance().getCurrentframe().removeRoot(this); // TODO same when removing primitive
		newParent.updateSubtreeNumber(newParent.getTreeNumber());
		return true;
	}

	public void ConnectToParent(AbstractDrawingPrimitive primitive, Joint myJoint, Joint primitiveJoint) {
		hasParent = true;
		isScalable = false;
		Connection newConnection = new Connection();
		newConnection.myRelation = Relation.PRIMITIVE_IS_PARENT;
		newConnection.myJoint = myJoint;
		newConnection.primitiveJoint = primitiveJoint;
		newConnection.primitive = primitive;
		m_parentConnection = newConnection;
	}
	
	
	public void ConnectToChild(AbstractDrawingPrimitive primitive, Joint myJoint, Joint primitiveJoint) {
		Connection con = new Connection();
		con.myRelation = Relation.PRIMITIVE_IS_CHILD;
		con.myJoint = myJoint;
		con.primitive = primitive;
		con.primitiveJoint = primitiveJoint;
		m_childrenConnections.add(con);
		isScalable = false;
	}

	
	public void disconnectFromChild(AbstractDrawingPrimitive p) {
		Connection toRemove = null;
		
		for (Connection con : m_childrenConnections) {
			if (con.primitive == p) {
				con.primitiveJoint.setFree();
				con.myJoint.removeChild(con.primitiveJoint);
				toRemove = con;
				break;
			}
		}
		
		m_childrenConnections.remove(toRemove);
		isScalable = (m_childrenConnections.isEmpty() && m_parentConnection == null);
	}

	protected void incAncestorsNumber() {
		m_ancestorsNumber++;
		if (m_parentConnection != null)
			m_parentConnection.primitive.incAncestorsNumber();
	}
	
	
	public void disconnectFromParent() {
		if (hasParent) {
			hasParent = false;
			AbstractDrawingPrimitive exParent = null;
			
			
			m_parentConnection.myJoint.setFree();
			m_parentConnection.primitiveJoint.removeChild(m_parentConnection.myJoint);
			exParent = m_parentConnection.primitiveJoint.getMyPrimitive();
			exParent.disconnectFromChild(this);
			m_parentConnection = null;
			
			Animation.getInstance().getCurrentframe().addRoot(this);
			updateSubtreeNumber(Animation.getInstance().getCurrentframe().getTreesNumber());
			isScalable = (m_childrenConnections.isEmpty());
		}
	}


	
	public ArrayList<Joint> getMyJoints() {
		return joints;
	}

	
	public void updateSubtreeNumber(int number) {
		m_treeNumber = number;
		for (Connection con : m_childrenConnections) {
			con.primitive.updateSubtreeNumber(number);
		}
	}

	
	public int getTreeNumber() {
		return m_treeNumber;
	}

	
	public void setTreeNumber(int number) {
		m_treeNumber = number;
	}

	
	public boolean isOutOfBounds() {
		return m_isOutOfBounds;
	}
	
	
	public void checkOutOfBounds() {
		for (Connection con : m_childrenConnections) {
			con.primitive.checkOutOfBounds();
		}
	}
	
	
	public void disconnectFromEverybody() {
		while (!m_childrenConnections.isEmpty()) {
			Connection con = m_childrenConnections.get(0);
			con.primitive.disconnectFromParent();
		}
		disconnectFromParent();
		m_parentConnection = null;
		hasParent = false;
	}
	
	
	public void setTransitiveFields(Context context) {
		m_context = context;
		m_isTouched = false;
		m_isOutOfBounds = false;
	}
	
	
	public ArrayList<Connection> getMyChildrenConnections() {
		return m_childrenConnections;
	}
	
	public void addChildConnection(Connection childCon) {
		m_childrenConnections.add(childCon);
	}
	
	public Connection getMyParentConnection() {
		return m_parentConnection;
	}
	
	public void setParentConnection(Connection parentCon) {
		m_parentConnection = parentCon;
		if (parentCon != null)
			hasParent = true;
	}
	
	private void writeObject(java.io.ObjectOutputStream  stream) throws IOException {		
		if (m_parentConnection != null) {
			stream.writeInt(m_childrenConnections.size() + 1);
			stream.writeObject(m_parentConnection);
		} else
			stream.writeInt(m_childrenConnections.size());
			
		for (Connection con : m_childrenConnections)
			stream.writeObject(con);
		stream.writeInt(joints.size());
		for (Joint j : joints)
			stream.writeObject(j);
		stream.writeBoolean(hasParent);
		stream.writeInt(m_treeNumber);
		stream.writeBoolean(isScalable);
		stream.writeInt(m_number);
	}
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		m_childrenConnections = new ArrayList<Connection>();
		m_parentConnection = null;
		int sz = stream.readInt();
		
		if (sz > 0) {
			Connection newConnection = null;
			newConnection = (Connection)stream.readObject();
			if (newConnection.myRelation == Relation.PRIMITIVE_IS_PARENT) // first goes parent if he exists
				m_parentConnection = newConnection;
			else 
				m_childrenConnections.add(newConnection);

			
			for (int i = 1; i < sz; i++) {
				newConnection = (Connection)stream.readObject();
				m_childrenConnections.add(newConnection);
			}
		}
		
		sz = stream.readInt();
		joints = new ArrayList<Joint>();
		for (int i = 0; i < sz; i++) 
			joints.add((Joint)stream.readObject());
		for (Joint j : joints)
			j.setMyPrimitive(this);
		hasParent = stream.readBoolean();
		m_treeNumber = stream.readInt();
		isScalable = stream.readBoolean();
		m_number = stream.readInt();
	}
	
	public void restoreMyFieldsByIndexes(LinkedList<AbstractDrawingPrimitive> q) { // should be called after all the primitives of the frame are loaded
		for (Connection con: m_childrenConnections)
			con.reastoreMyFieldsMyIndexes(q, this);	
		if (m_parentConnection != null)
			m_parentConnection.reastoreMyFieldsMyIndexes(q, this);
	}
	
	
	public void setMyNumber(int newNumber) {
		m_number = newNumber;
	}

	
	public int getMyNumber() {
		return m_number;
	}
	
	
	public boolean hasParent() {
		return hasParent;
	}
}
