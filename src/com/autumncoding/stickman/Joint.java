package com.autumncoding.stickman;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import com.autamncoding.stickman.R;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

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
	private transient Joint m_parent;
	private transient ArrayList<Joint> m_childrenJoints;
	private transient AbstractDrawingPrimitive m_primitive; // primitive, to which this joint belongs
	private Vector2DF m_point;
	private Paint m_paint;
	
	private void writeObject(java.io.ObjectOutputStream  stream) throws IOException {
		stream.writeBoolean(isParentJoint);
		stream.writeBoolean(isChildJoint);
		stream.writeBoolean(isFreeJoint);
		stream.writeObject(m_point);
	}
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		isParentJoint = stream.readBoolean();
		isChildJoint = stream.readBoolean();
		isFreeJoint = stream.readBoolean();
		m_point = (Vector2DF) stream.readObject();
		m_parent = null;
		m_childrenJoints = new ArrayList<Joint>();
		m_primitive = null;
		m_paint = GameData.root_joint_paint;
	}
	
	public Joint(AbstractDrawingPrimitive pr, Vector2DF p) {
		m_childrenJoints = new ArrayList<Joint>();
		m_primitive = pr;
		m_point = new Vector2DF(p);
		m_paint = GameData.root_joint_paint;
	}
	
	public void updateColor() {
		if (!m_primitive.hasParent())
			m_paint = GameData.root_joint_paint;
		else if (!isChildJoint)
			m_paint = GameData.child_joint_paint; // TODO rename to free joint paint
		else 
			m_paint = m_parent.m_paint;
	}
	
	public void setMyPrimitive(AbstractDrawingPrimitive pr) {
		m_primitive = pr;
	}
	/**
	 * Sets current joint to be free, i.e. it's not connected to anything, and so
	 * it is not a child joint, nor a parent joint.
	 */
	public void setFree() {
		isParentJoint = false;
		isChildJoint = false;
		isFreeJoint = true;
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
			m_parent = null; // TODO its shit: some joints are children and parents at the same time (circle)
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
	
	public void setTouched(boolean touched) {
		if (touched)
			m_paint = GameData.joint_touched_paint;
		else
			updateColor();
	}
	
	public void setOutOfBounds(boolean outOfBounds) {
		if (outOfBounds)
			m_paint = GameData.joint_drop_paint;
		else
			updateColor();
	}
	
	public void setUnactive(boolean unactive) {
		if (unactive)
			m_paint = GameData.joint_prev_frame_paint;
		else
			updateColor();
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

	public Joint getMyParent() {
		return m_parent;
	}
	
	public ArrayList<Joint> getMyChildren() {
		return m_childrenJoints;
	}
	
	public AbstractDrawingPrimitive getMyPrimitive() {
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
	
	public void draw(Canvas canvas) {
		canvas.drawCircle(m_point.x, m_point.y, GameData.joint_radius_visible, m_paint);
	}
	
	public void drawBlendingWithSuccessor(Canvas canvas, float t, Joint suc) {
		GameData.mixTwoColors(m_paint.getColor(), suc.m_paint.getColor(), 1 - t);
		canvas.drawCircle((1 - t) * m_point.x + t * suc.m_point.x, (1 - t) * m_point.y + t * suc.m_point.y, GameData.joint_radius_visible, GameData.blended_joint_paint);
	}
	
	public void drawBlendingWithNoPredecessor(Canvas canvas, float t) {
		GameData.mixTwoColors(Color.argb(0, 0, 0, 0), m_paint.getColor(), 1 - t);
		canvas.drawCircle(m_point.x, m_point.y, GameData.joint_radius_visible, GameData.blended_joint_paint);
	}
	
	public void drawBlendingWithNoSuccessor(Canvas canvas, float t) {
		GameData.mixTwoColors(Color.argb(0, 0, 0, 0), m_paint.getColor(), t);
		canvas.drawCircle(m_point.x, m_point.y, GameData.joint_radius_visible, GameData.blended_joint_paint);
	}
	
	public void removeChild(Joint ch) {
		m_childrenJoints.remove(ch);
		if (m_childrenJoints.isEmpty()) {
			isFreeJoint = true;
			isParentJoint = false;
		}
	}
}