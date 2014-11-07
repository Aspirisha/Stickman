package com.autumncoding.stickman;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;

import com.autumncoding.stickman.DrawingPrimitive.Connection;

import android.content.Context;

public abstract class AbstractDrawingPrimitive implements DrawingPrimitive, Serializable {
	private static final long serialVersionUID = 6036735876597370696L;
	protected ArrayList<Joint> joints;
	protected boolean hasParent;
	protected ArrayList<Connection> m_connections;
	protected int m_treeNumber;
	protected boolean isScalable;
	protected int m_number = 0;
	protected transient boolean m_isTouched;
	protected transient boolean m_isOutOfBounds = false;
	protected transient Context m_context;
	
	AbstractDrawingPrimitive(Context context) {
		m_connections = new ArrayList<DrawingPrimitive.Connection>();
		joints = new ArrayList<Joint>();
		m_context = context;
		hasParent = false;
		isScalable = true;
		Animation.getInstance().getCurrentframe().addRoot(this);
		m_treeNumber = Animation.getInstance().getCurrentframe().getTreesNumber();
		m_number = Animation.getInstance().getCurrentframe().getPrimitives().size();
		m_isTouched = false;
	}

	@Override
	public Context getContext() {
		return m_context;
	}

	/*void writeObject(java.io.ObjectOutputStream out) {
		try {
			out.writeObject(joints);
			out.writeObject(hasParent);
			out.writeObject(m_connections);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/
	
	@Override
	public boolean tryConnection(LinkedList<DrawingPrimitive> neighbours) {
		float min_dist = 10000;
		DrawingPrimitive closest_primitive = null;
		for (DrawingPrimitive pr : neighbours) {
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

	@Override
	public boolean isTouched() {
		return m_isTouched;
	}
	
	@Override
	public boolean connectToParent(DrawingPrimitive primitive) {
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
		DrawingPrimitive newParent = newParentJoint.getMyPrimitive(); 
				
		ArrayList<Connection> temp = new ArrayList<DrawingPrimitive.Connection>();
		temp.addAll(m_connections);
		
		for (Connection con : temp) {
			if (con.myJoint == myChildJoint && con.myRelation == Relation.PRIMITIVE_IS_CHILD) {
				Joint ch = con.primitiveJoint;
				ch.connectToParent(newParentJoint);
				newParentJoint.addChild(ch);
				DrawingPrimitive reattachedChild = ch.getMyPrimitive();
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

	public void ConnectToParent(DrawingPrimitive primitive, Joint myJoint, Joint primitiveJoint) {
		hasParent = true;
		isScalable = false;
		Connection newConnection = new Connection();
		newConnection.myRelation = Relation.PRIMITIVE_IS_PARENT;
		newConnection.myJoint = myJoint;
		newConnection.primitiveJoint = primitiveJoint;
		newConnection.primitive = primitive;
		m_connections.add(newConnection);
	}
	
	@Override
	public void ConnectToChild(DrawingPrimitive primitive, Joint myJoint, Joint primitiveJoint) {
		Connection con = new Connection();
		con.myRelation = Relation.PRIMITIVE_IS_CHILD;
		con.myJoint = myJoint;
		con.primitive = primitive;
		con.primitiveJoint = primitiveJoint;
		m_connections.add(con);
		isScalable = false;
	}

	@Override
	public void disconnectFromChild(DrawingPrimitive p) {
		Connection toRemove = null;
		
		for (Connection con : m_connections) {
			if (con.primitive == p) {
				con.primitiveJoint.setFree();
				con.myJoint.removeChild(con.primitiveJoint);
				toRemove = con;
				break;
			}
		}
		
		m_connections.remove(toRemove);
		isScalable = m_connections.isEmpty();
	}

	@Override
	public void disconnectFromParent() {
		if (hasParent) {
			hasParent = false;
			DrawingPrimitive exParent = null;
			
			Connection toRemove = null;
			for (Connection con : m_connections) {
				if (con.myRelation == Relation.PRIMITIVE_IS_PARENT) {
					con.myJoint.setFree();
					con.primitiveJoint.removeChild(con.myJoint);
					exParent = con.primitiveJoint.getMyPrimitive();
					toRemove = con;
					break;
				}
			}
			m_connections.remove(toRemove);
			exParent.disconnectFromChild(this);
			
			Animation.getInstance().getCurrentframe().addRoot(this);
			updateSubtreeNumber(Animation.getInstance().getCurrentframe().getTreesNumber());
			isScalable = m_connections.isEmpty();
		}
	}


	@Override
	public ArrayList<Joint> getMyJoints() {
		return joints;
	}

	@Override
	public void updateSubtreeNumber(int number) {
		m_treeNumber = number;
		for (Connection con : m_connections) {
			if (con.myRelation == Relation.PRIMITIVE_IS_CHILD)
				con.primitive.updateSubtreeNumber(number);
		}
	}

	@Override
	public int getTreeNumber() {
		return m_treeNumber;
	}

	@Override
	public void setTreeNumber(int number) {
		m_treeNumber = number;
	}

	@Override
	public boolean isOutOfBounds() {
		return m_isOutOfBounds;
	}
	
	@Override
	public void checkOutOfBounds() {
		for (Connection con : m_connections) {
			if (con.myRelation == Relation.PRIMITIVE_IS_CHILD)
				con.primitive.checkOutOfBounds();
		}
	}
	
	@Override
	public void disconnectFromEverybody() {
		while (!m_connections.isEmpty()) {
			Connection con = m_connections.get(0);
			if (con.myRelation == Relation.PRIMITIVE_IS_CHILD) {
				con.primitive.disconnectFromParent();
			} else {
				disconnectFromParent();
			}
		}
	}
	
	@Override
	public void setTransitiveFields(Context context) {
		m_context = context;
		m_isTouched = false;
		m_isOutOfBounds = false;
	}
	
	@Override
	public ArrayList<Connection> getMyConnections() {
		return m_connections;
	}

}
