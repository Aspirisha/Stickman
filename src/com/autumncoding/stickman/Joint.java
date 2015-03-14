package com.autumncoding.stickman;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import com.autamncoding.stickman.R;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Join;

/**
 * This class represents joints that connect different drawing primitives. 
 * @author Andy
 *
 */
public class Joint implements Serializable {
	private enum JointState {
		CENTRAL,
		FREE,
		CHILD
	}
	
	private static final long serialVersionUID = 1L;
	private boolean isParentJoint = false;
	private boolean isChildJoint = false;
	private boolean isFreeJoint = true;
	private transient Joint m_parent;
	private transient ArrayList<Joint> m_childrenJoints;
	private transient AbstractDrawingPrimitive m_primitive; // primitive, to which this joint belongs
	private transient long glowStartTime;
	private transient int glowColor1;
	private transient int glowColor2;
	private Vector2DF m_point;
	private Paint m_paint;
	private boolean m_isCentral = false;
	private JointState m_state = JointState.FREE;
	private boolean m_isTouched = false;
	private boolean m_isGlowing = false;
	
	private void writeObject(java.io.ObjectOutputStream  stream) throws IOException {
		stream.writeBoolean(isParentJoint);
		stream.writeBoolean(isChildJoint);
		stream.writeBoolean(isFreeJoint);
		stream.writeBoolean(m_isCentral);
		stream.writeBoolean(m_isGlowing);
		stream.writeObject(m_point);
	}
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		isParentJoint = stream.readBoolean();
		isChildJoint = stream.readBoolean();
		isFreeJoint = stream.readBoolean();
		m_isCentral = stream.readBoolean();
		m_isGlowing = stream.readBoolean();
		m_point = (Vector2DF) stream.readObject();
		m_parent = null;
		m_childrenJoints = new ArrayList<Joint>();
		m_primitive = null;
		
		if (m_isCentral)
			m_paint = GameData.root_joint_paint;
		else
			m_paint = GameData.child_joint_paint;
	}
	
	public Joint(AbstractDrawingPrimitive pr, Vector2DF p) {
		m_childrenJoints = new ArrayList<Joint>();
		m_primitive = pr;
		m_point = new Vector2DF(p);
		m_paint = GameData.child_joint_paint;
	}
	
	public void updateColor() {
		if (!m_primitive.hasParent() && m_isCentral) {
			m_paint = GameData.root_joint_paint;
		}
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
		if (touched != m_isTouched) {
			m_isTouched = touched;
			if (touched)
				m_paint = GameData.joint_touched_paint;
			else
				updateColor();
		}
	}
	
	public void setInvisible() {
		m_paint = GameData.invisible_paint;
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
	
	public void translate(float dx, float dy) {
		m_point.x += dx;
		m_point.y += dy;
	}
	
	public void draw(Canvas canvas) {
		if (!m_isGlowing)
			canvas.drawCircle(m_point.x, m_point.y, GameData.joint_radius_visible, m_paint);
		else {
			float t = (float) Math.abs(Math.cos((System.currentTimeMillis() - glowStartTime) * 2 * Math.PI / 1000));
			GameData.mixTwoColors(glowColor1, glowColor2, t);
			canvas.drawCircle(m_point.x, m_point.y, GameData.joint_radius_visible, GameData.blended_joint_paint);
		}
	}
	
	public void drawBlendingWithSuccessor(Canvas canvas, Joint suc, float t, float interp_x, float interp_y) {
		GameData.mixTwoColors(m_paint.getColor(), suc.m_paint.getColor(), 1 - t);
		canvas.drawCircle(interp_x, interp_y, GameData.joint_radius_visible, GameData.blended_joint_paint);
	}
	
	public void drawBlendingWithNoPredecessor(Canvas canvas, float t) {
		GameData.mixTwoColors(Color.argb(0, 0, 0, 0), m_paint.getColor(), 1 - t);
		canvas.drawCircle(m_point.x, m_point.y, GameData.joint_radius_visible, GameData.blended_joint_paint);
	}
	
	public void drawBlendingWithNoSuccessor(Canvas canvas, float t) {
		GameData.mixTwoColors(Color.argb(0, 0, 0, 0), m_paint.getColor(), t);
		canvas.drawCircle(m_point.x, m_point.y, GameData.joint_radius_visible, GameData.blended_joint_paint);
	}
	
	public void setCentral(boolean b) {
		m_isCentral = b;
	}
	
	public void removeChild(Joint ch) {
		m_childrenJoints.remove(ch);
		if (m_childrenJoints.isEmpty()) {
			isFreeJoint = true;
			isParentJoint = false;
		}
	}
	
	public void startGlowing(int Color1, int Color2) {
		if (!m_isGlowing) {
			m_isGlowing = true;
			glowStartTime = System.currentTimeMillis();
			glowColor1 = Color1;
			glowColor2 = Color2;
		}
	}
	
	public void stopGlowing() {
		m_isGlowing = false;
	}
}