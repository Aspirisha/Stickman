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
		FREE,
		PARENT,
		CHILD
	}
	
	private static final long serialVersionUID = 1L;
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
		stream.writeObject(m_state);
		stream.writeBoolean(m_isCentral);
		stream.writeBoolean(m_isGlowing);
		stream.writeObject(m_point);
	}
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		m_state = (JointState) stream.readObject();
		m_isCentral = stream.readBoolean();
		m_isGlowing = stream.readBoolean();
		m_point = (Vector2DF) stream.readObject();
		m_parent = null;
		m_childrenJoints = new ArrayList<Joint>();
		m_primitive = null;
		
		if (m_isCentral)
			m_paint = GameData.root_joint_paint;
		else
			m_paint = GameData.joint_paint_free;
	}
	
	// !!! Consumes point as-is
	public Joint(AbstractDrawingPrimitive pr, Vector2DF p) {
		m_childrenJoints = new ArrayList<Joint>();
		m_primitive = pr;
		m_point = p;
		m_paint = GameData.joint_paint_free;
	}
	
	public void updateColor() {
		switch (m_state) {
		case CHILD:
			m_paint = GameData.invisible_paint;
			break;
		case FREE:
			m_paint = GameData.joint_paint_free;
			break;
		case PARENT:
			m_paint = GameData.joint_connected_paint;
			break;
		}
		
		if (m_isCentral)
			m_paint = GameData.root_joint_paint;
	}
	
	public void setMyPrimitive(AbstractDrawingPrimitive pr) {
		m_primitive = pr;
	}
	/**
	 * Sets current joint to be free, i.e. it's not connected to anything, and so
	 * it is not a child joint, nor a parent joint.
	 */
	public void setFree() {
		m_state = JointState.FREE;
		m_parent = null;
		m_childrenJoints.clear();
		
		if (!m_isCentral)
			m_paint = GameData.joint_paint_free;
	}

	/**
	 * Sets current joint to be parent of joint (newChild).
	 * @param newChild new child of this joint
	 */
	public void addChild(Joint newChild) {
		if (!m_childrenJoints.contains(newChild)) {
			m_childrenJoints.add(newChild);
			m_state = JointState.PARENT;
			m_parent = null; 
			m_paint = GameData.joint_connected_paint;
		}
	}
	
	/**
	 * Sets current joint to be child of joint (newParent)
	 * @param newParent new parent of this joint
	 */
	public void connectToParent(Joint newParent) {
		m_state = JointState.CHILD;
		m_parent = newParent;
		m_childrenJoints.clear(); 
		m_isCentral = false;
		setInvisible();
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
		return (m_state == JointState.PARENT);
	}
	
	public boolean isFree() {
		return (m_state == JointState.FREE);
	}
	
	public boolean isChild() {
		return (m_state == JointState.CHILD);
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
	
	public void setMyPoint(float x, float y) {
		m_point.x = x;
		m_point.y = y;
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
		canvas.drawCircle(m_point.x, m_point.y, GameData.joint_radius_visible, m_paint);
		if (m_isGlowing) {
			float t = (float) Math.abs(Math.cos((System.currentTimeMillis() - glowStartTime) * 2 * Math.PI / 1000));
			GameData.mixTwoColors(glowColor1, glowColor2, t);
			GameData.blured_paint.setColor(GameData.blended_joint_paint.getColor());
			canvas.drawCircle(m_point.x, m_point.y, GameData.joint_radius_visible * 1.5f, GameData.blended_joint_paint);
			canvas.drawCircle(m_point.x, m_point.y, GameData.joint_radius_visible * 1.5f, GameData.blured_paint);
		}
	}
	
	public void drawBlendingWithSuccessor(Canvas canvas, float interp_x, float interp_y) {
		canvas.drawCircle(interp_x, interp_y, GameData.joint_radius_visible, GameData.joint_paint_free);
	}
	
	public void drawBlendingWithNoSuccessor(Canvas canvas, float t) {
		GameData.mixTwoColors(Color.argb(0, 0, 0, 0), GameData.joint_paint_free.getColor(), t);
		canvas.drawCircle(m_point.x, m_point.y, GameData.joint_radius_visible, GameData.blended_joint_paint);
	}
	
	public void setCentral(boolean b) {
		m_isCentral = b;
	}
	
	public void removeChild(Joint ch) {
		m_childrenJoints.remove(ch);
		if (m_childrenJoints.isEmpty()) {
			setFree();
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