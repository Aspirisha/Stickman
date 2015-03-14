package com.autumncoding.stickman;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;



import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.Log;

enum PrimitiveType {
	STICK,
	CIRCLE,
};

public abstract class AbstractDrawingPrimitive implements Serializable {
	private static final long serialVersionUID = 6036735876597370696L;
	
	public static final float PIDIV2 = (float) (Math.PI / 2.0f);
	public static final float PI = (float) Math.PI;
	
	protected ArrayList<Joint> joints;
	protected Joint m_primitiveCentre;
	protected transient Joint rotJoint;
	protected boolean hasParent;
	//protected ArrayList<Connection> m_connections;
	protected int m_treeNumber;
	protected boolean isScalable;
	protected int m_number = 0; // if primitive belongs to frame, it is >= 0
	protected transient boolean m_isTouched;
	protected transient boolean m_isOutOfBounds = false;
	protected transient Context m_context;
	protected transient int m_successorNumber = -1;
	protected transient int m_predecessorNumber = -1;
	protected Joint m_rotationCentre = null;
	
	protected float m_deltaScale = 1.0f;
	protected Vector2DF m_deltaPos = new Vector2DF();
	protected float m_deltaAngle = 0.0f;
	
	protected Connection m_parentConnection;
	protected ArrayList<Connection> m_childrenConnections;
	protected AbstractDrawingPrimitive m_successor = null;
	protected AbstractDrawingPrimitive m_predecessor = null;
	
	
	abstract void translate(float x, float y);
	abstract public boolean checkTouch(float touch_x, float touch_y);
	abstract public void draw(Canvas canvas);
	abstract public void drawBlendingWithSuccessor(Canvas canvas, float t); // t in [0, 1]
	abstract public void drawBlendingWithNoPredecessor(Canvas canvas, float t);
	abstract public void drawBlendingWithNoSuccessor(Canvas canvas, float t);
	abstract public PrimitiveType GetType();
	abstract public float getDistToMe(float from_x, float from_y);
	abstract public void setUntouched();
	abstract public AbstractDrawingPrimitive getCopy(); 
	abstract public void setActiveColour();
	abstract public void setUnactiveColour();
	abstract public void rotate(float fi, float cx, float cy, boolean rotateChildren);
	abstract public void applyMove(float new_x, float new_y, float prev_x, float prev_y, boolean isScaling);
	abstract public Joint getTouchedJoint();
	abstract float distTo(AbstractDrawingPrimitive pr);
	
	public void scale(float cx, float cy, float rate) {
		if (m_predecessor != null) {
			m_deltaScale *= rate;
		}
	}
	
	AbstractDrawingPrimitive(Context context) {
		m_childrenConnections = new ArrayList<Connection>();
		joints = new ArrayList<Joint>();
		m_context = context;
		hasParent = false;
		isScalable = true;
		m_number = Animation.getInstance().getCurrentframe().getPrimitives().size();
		m_isTouched = false;
		m_parentConnection = null;
		m_primitiveCentre = null;
	}
	
	public static void setSuccessorAndPredecessor(AbstractDrawingPrimitive s, AbstractDrawingPrimitive p) {
		if (s != null)
			s.m_predecessor = p;
		if (p != null)
			p.m_successor = s;
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
		m_primitiveCentre = null;
	}

	public Context getContext() {
		return m_context;
	}
	
	public void translate(Vector2DF v) {
		translate(v.x, v.y);
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
		
		AbstractDrawingPrimitive.connect(newParentJoint, myChildJoint);
		return true;
	}

	protected void updateJointColors() {
		for (Joint j : joints)
			j.updateColor();
	}

	public boolean disconnectFromParent() {
		if (hasParent)
			return disconnect(m_parentConnection.primitive, this);
		return false;
	}
	
	/**
	 * This function disconnects parent and child and leaves both of them in correct state
	 * @param parent
	 * @param child
	 * @return
	 */
	static boolean disconnect(AbstractDrawingPrimitive parent, AbstractDrawingPrimitive child) {
		if (!child.hasParent)
			return false;
		if (parent != child.m_parentConnection.primitive)
			return false;
		
		child.hasParent = false;
		child.m_parentConnection.myJoint.setFree();
		child.m_parentConnection.myJoint.setCentral(true);
		child.updateCentre(child.m_parentConnection.myJoint);
		child.m_parentConnection.primitiveJoint.removeChild(child.m_parentConnection.myJoint);
		child.m_parentConnection = null;
		
		
		Connection toRemove = null;
		for (Connection con : parent.m_childrenConnections) {
			if (con.primitive == child) {
				toRemove = con;
				break;
			}
		}
		parent.m_childrenConnections.remove(toRemove);
		parent.isScalable = (parent.m_childrenConnections.isEmpty() && parent.m_parentConnection == null);
		child.isScalable = (child.m_childrenConnections.isEmpty() && child.m_parentConnection == null);
		
		Animation.getInstance().getCurrentframe().addRoot(child);
		child.updateSubtreeNumber(Animation.getInstance().getCurrentframe().getTreesNumber()); // TODO check if it's ok
		
		child.updateJointColors();
		return true;
	}
	
	public void addParentConnection(AbstractDrawingPrimitive parent, Joint hisJoint, Joint myJoint) {
		m_parentConnection = new Connection();
		m_parentConnection.primitive = parent;
		m_parentConnection.myJoint = myJoint;
		m_parentConnection.primitiveJoint = hisJoint;
		m_parentConnection.myRelation = Relation.PRIMITIVE_IS_PARENT;
	}
	
	public void addChildConnection(AbstractDrawingPrimitive child, Joint hisJoint, Joint myJoint) {
		Connection con = new Connection();
		con.primitive = child;
		con.myJoint = myJoint;
		con.primitiveJoint = hisJoint;
		con.myRelation = Relation.PRIMITIVE_IS_CHILD;
		
		m_childrenConnections.add(con);
	}
	
	static boolean connect(Joint parentJoint, Joint childJoint) {
		AbstractDrawingPrimitive child = childJoint.getMyPrimitive();
		AbstractDrawingPrimitive parent = parentJoint.getMyPrimitive();
		
		if (child.hasParent || parent.m_treeNumber == child.m_treeNumber)
			return false;
		
		childJoint.setCentral(false);
		child.hasParent = true;
		child.addParentConnection(parent, parentJoint, childJoint);
		parent.addChildConnection(child, childJoint, parentJoint);
		
		LinkedList<Connection> toRemove = new LinkedList<Connection>();
		
		Vector2DF dv = Vector2DF.sub(parentJoint.getMyPoint(), childJoint.getMyPoint());
		
		ArrayList<Connection> tempChildChildCons = new ArrayList<Connection>();
		tempChildChildCons.addAll(child.m_childrenConnections);
		for (Connection con : tempChildChildCons) {
			if (con.myJoint == childJoint) {
				Joint tempChildJoint = con.primitiveJoint;
				AbstractDrawingPrimitive tempChild = con.primitive;
				disconnect(child, tempChild);
				connect(parentJoint, tempChildJoint);
			}
		}
		
		child.translate(dv);
		parentJoint.addChild(childJoint);
		childJoint.connectToParent(parentJoint);
		child.m_childrenConnections.removeAll(toRemove);
		child.updateJointColors();
		child.isScalable = false;
		parent.isScalable = false;
		Animation.getInstance().getCurrentframe().removeRoot(child);
		
		parent.updateSubtreeNumber(parent.m_treeNumber);
		parent.updateCentre(parent.m_rotationCentre);
		return true;
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
	
	public void updateCentre(Joint centre) {
		m_rotationCentre = centre;
		for (Connection con : m_childrenConnections) {
			con.primitive.updateCentre(centre);
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
	
	
	public void disconnectBeforeDeleting() {
		HashMap<Joint, AbstractDrawingPrimitive> newParentsForChildrenFromJoints = new HashMap<Joint, AbstractDrawingPrimitive>();
		HashMap<Joint, Joint> newParentJoints = new HashMap<Joint, Joint>();
		
		if (!m_childrenConnections.isEmpty()) {
			while (!m_childrenConnections.isEmpty()) {
				Connection con = m_childrenConnections.get(0);
				AbstractDrawingPrimitive child = con.primitive;
				Joint prevParentJoint = con.myJoint;
				Joint childJoint = con.primitiveJoint;
				
				disconnect(this, child);
				if (!newParentsForChildrenFromJoints.containsKey(prevParentJoint)) {
					newParentsForChildrenFromJoints.put(prevParentJoint, child);
					newParentJoints.put(prevParentJoint, childJoint);
				} else {
					connect(newParentJoints.get(prevParentJoint), childJoint);
				}
			}
		
		}
		if (hasParent) 
			disconnect(m_parentConnection.primitive, this);
		
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
		childCon.myJoint.updateColor();
	}
	
	public Connection getMyParentConnection() {
		return m_parentConnection;
	}
	
	public void setParentConnection(Connection parentCon) {
		m_parentConnection = parentCon;
		parentCon.myJoint.updateColor();
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
		if (!hasParent) // write centre only for root!!!
			stream.writeObject(m_rotationCentre);
		stream.writeObject(m_primitiveCentre);
		
		stream.writeInt(m_treeNumber);
		stream.writeBoolean(isScalable);
		stream.writeInt(m_number);
		
		if (m_successor != null)
			stream.writeInt(m_successor.getMyNumber());
		else
			stream.writeInt(-1);
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
		if (!hasParent) {
			m_rotationCentre = (Joint)stream.readObject();
			m_rotationCentre.setMyPrimitive(this);
		}
		m_primitiveCentre = (Joint)stream.readObject();
		m_primitiveCentre.setMyPrimitive(this);
		m_primitiveCentre.setInvisible();
		rotJoint = m_primitiveCentre;
		
		m_treeNumber = stream.readInt();
		isScalable = stream.readBoolean();
		m_number = stream.readInt();
		m_successorNumber = stream.readInt();
	}
	
	public void restoreMyFieldsByIndexes(LinkedList<AbstractDrawingPrimitive> q, 
			LinkedList<AbstractDrawingPrimitive> nextPrimitives) { // should be called after all the primitives of all frames are loaded
		for (Connection con: m_childrenConnections)
			con.restoreMyFieldsMyIndexes(q, this);	
		if (m_parentConnection != null) {
			m_parentConnection.restoreMyFieldsMyIndexes(q, this);
			m_rotationCentre = m_parentConnection.primitive.m_rotationCentre;
			rotJoint = m_rotationCentre;
		}
		
		if (nextPrimitives != null && m_successorNumber != -1) {
			AbstractDrawingPrimitive suc = null;
			for (AbstractDrawingPrimitive pr : nextPrimitives) {
				if (pr.m_number == m_successorNumber) {
					suc = pr;
					break;
				}
			}
			AbstractDrawingPrimitive.setSuccessorAndPredecessor(suc, this);
		}
	}
	
	public boolean setJointAsCentre(Joint j) {
		if (!joints.contains(j))
			return false;
		
		if (m_parentConnection != null) {
			AbstractDrawingPrimitive parent = m_parentConnection.primitive;
			Joint myJoint = m_parentConnection.myJoint;
			Joint hisJoint = m_parentConnection.primitiveJoint;
			disconnect(parent, this);
			connect(myJoint, hisJoint);
		}
		
		for (Joint joint : joints)
			joint.setCentral(false);
		
		j.setCentral(true);
		updateCentre(j);
		updateJointColors();
		return true;
	}
	
	
	public void rotateAroundRotationCentre(Joint touchedJoint, float x, float y) {
		if (touchedJoint.isChild())
			return;
		
		// joint around which real rotation is going to be
		Vector2DF centre = rotJoint.getMyPoint();
		float l1 = Vector2DF.dist(centre, x, y);
		float l0 = Vector2DF.dist(centre, touchedJoint.getMyPoint());
		if (l1 == 0 || l0 == 0)
			return;
    	float cos_theta = ((x - centre.x) * (touchedJoint.getMyPoint().x - centre.x) + 
    			(y - centre.y) * (touchedJoint.getMyPoint().y - centre.y)) / (l1 * l0);
    	if (cos_theta > 1.0f)
    		cos_theta = 1.0f;
    	else if (cos_theta < -1.0f)
    		cos_theta = -1.0f;
    	
    	float theta = (float)Math.acos(cos_theta);
    	
    	if ((touchedJoint.getMyPoint().x - centre.x) * (y - centre.y) - (x - centre.x) * (touchedJoint.getMyPoint().y - centre.y) < 0)
    		theta = -theta;
    	
		Connection con = m_parentConnection;
		AbstractDrawingPrimitive parent = this;
		while (con != null) {
			if (con.primitiveJoint == rotJoint)
				break;
			parent = m_parentConnection.primitive;
			con = parent.m_parentConnection;
		}
		
		parent.rotate(theta, centre.x, centre.y, false);
			
		for (Connection c : parent.m_childrenConnections) {
			if (c.myJoint != rotJoint)
				c.primitive.rotate(theta, centre.x, centre.y, true);
		}
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
