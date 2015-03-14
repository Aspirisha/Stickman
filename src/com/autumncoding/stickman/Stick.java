package com.autumncoding.stickman;

import java.io.IOException;
import java.io.Serializable;

import com.autamncoding.stickman.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Stick extends AbstractDrawingPrimitive implements Serializable {
	private static final long serialVersionUID = -9064457764417918381L;
	private Vector2DF p1;
	private Vector2DF p2;
	
	private float length;
	private float angle;
	
	private transient Paint m_line_paint;
	
	/*********************** touch data *********************************************/
	enum StickTouches {
		JOINT1, 
		JOINT2,
		STICK,
		NONE
	}	
	
	private transient StickTouches m_touchState = StickTouches.NONE;
	
	public Stick(Context context) {
		super(context);
		
		length = 100;
		angle = 0;
		p1 = new Vector2DF(100, 100);
		p2 = new Vector2DF(200, 100);

		m_line_paint = GameData.line_paint;
		
		joints.add(new Joint(this, p1));
		joints.add(new Joint(this, p2));
		joints.get(0).setCentral(true);
		m_rotationCentre = joints.get(0);
		rotJoint = m_rotationCentre;
		m_primitiveCentre = new Joint(this, new Vector2DF((p1.x + p2.x) / 2, (p1.y + p2.y) / 2));
		m_primitiveCentre.setInvisible();
		updateJointColors();
	}
	
	public Stick(Stick st) { // TODO maybe it should be private?
		super(st);
		p1 = new Vector2DF(st.p1);
		p2 = new Vector2DF(st.p2);
		length = st.length;
		angle = st.angle;
		m_line_paint = GameData.line_paint;
		joints.add(new Joint(this, p1));
		joints.add(new Joint(this, p2));
		joints.get(0).setCentral(true);
		m_rotationCentre = joints.get(0);
		rotJoint = m_rotationCentre;
		m_primitiveCentre = new Joint(this, new Vector2DF((p1.x + p2.x) / 2, (p1.y + p2.y) / 2));
		m_primitiveCentre.setInvisible();
		updateJointColors();
	}
	
	@Override
	public void rotate(float fi, float cx, float cy, boolean rotateChildren) {
		float new_x = (float) (cx + (p1.x - cx) * Math.cos(fi) - (p1.y - cy) * Math.sin(fi));
		float new_y = (float) (cy + (p1.x - cx) * Math.sin(fi) + (p1.y - cy) * Math.cos(fi));
		p1.x = new_x;
		p1.y = new_y;
		
		
		new_x = (float) (cx + (p2.x - cx) * Math.cos(fi) - (p2.y - cy) * Math.sin(fi));
		new_y = (float) (cy + (p2.x - cx) * Math.sin(fi) + (p2.y - cy) * Math.cos(fi));
		p2.x = new_x;
		p2.y = new_y;
    	
		angle += fi;
		
		joints.get(0).setMyPoint(p1);
		joints.get(1).setMyPoint(p2);
		m_primitiveCentre.setMyPoint(Vector2DF.ave(p1, p2));
		if (rotateChildren) {
			for (Connection con : m_childrenConnections) {
				con.primitive.rotate(fi, cx, cy, true);
			}
		}
	}

	@Override
	public void translate(float dx, float dy) {	
		p1.x += dx;
		p1.y += dy;
		p2.x += dx;
		p2.y += dy;
    	
		joints.get(0).setMyPoint(p1);
		joints.get(1).setMyPoint(p2);
		m_primitiveCentre.translate(dx, dy);
		
		for (Connection con : m_childrenConnections) {
			con.primitive.translate(dx, dy);
		}
	}
	
	@Override
	public void scale(float cx, float cy, float rate) {	
		super.scale(cx, cy, rate);
		
		float temp_length = length * rate;
		if (temp_length < GameData.min_stick_length)
		{
			temp_length = GameData.min_stick_length;
			rate = temp_length / length;
		}
		
		p1.x = cx + rate * (p1.x - cx);
		p1.y = cy + rate * (p1.y - cy);
		p2.x = cx + rate * (p2.x - cx);
		p2.y = cy + rate * (p2.y - cy);
		
		joints.get(0).setMyPoint(p1);
		joints.get(1).setMyPoint(p2);
		
		
		m_primitiveCentre.setMyPoint(Vector2DF.ave(p1, p2));
    	length = temp_length;
	}
	
	@Override
	public void draw(Canvas canvas) { 
		float dx = GameData.joint_radius_visible * (p2.x - p1.x) / length;
		float dy = GameData.joint_radius_visible * (p2.y - p1.y) / length;
		
		canvas.drawLine(p1.x + dx, p1.y + dy, p2.x - dx, p2.y - dy, m_line_paint);
		for (Joint j : joints)
			j.draw(canvas);
		m_primitiveCentre.draw(canvas);
	}
	
	private StickTouches m_checkTouched(float touch_x, float touch_y) {
		float dx = p1.x - touch_x;
		float dy = p1.y - touch_y;
		boolean joint1_touched = (dx * dx + dy * dy <= GameData.joint_radius_touchable_square);
		if (joints.get(0).isChild())
			joint1_touched = false;
		
		dx = p2.x - touch_x;
		dy = p2.y - touch_y;
		boolean joint2_touched = false;
		
		if (!joint1_touched && !joints.get(1).isChild())
			joint2_touched = (dx * dx + dy * dy <= GameData.joint_radius_touchable_square);
		
		boolean stick_touched = false;
		if (!joint1_touched && !joint2_touched) {
			// let's find out if projection of point (touch_x, touch_y) gets into stick ((p1.x, p1.y); (p2.x, p2.y))
			float dot_product = (touch_x - p1.x) * (p2.x - p1.x) + (touch_y - p1.y) * (p2.y - p1.y);
			float touch_vector_length_squared = (touch_x - p1.x) * (touch_x - p1.x) + (touch_y - p1.y) * (touch_y - p1.y);
			float touch_vector_projection = dot_product / length;
			if (touch_vector_projection >= 0 && touch_vector_projection <= length) { 
				float dist = touch_vector_length_squared - touch_vector_projection * touch_vector_projection;
				stick_touched = (dist <= GameData.stick_distance_touchable_square);
			}
		}
		
		StickTouches newTouchState = StickTouches.NONE;
		if (joint1_touched) {
			newTouchState = StickTouches.JOINT1;
		}
		else if (joint2_touched) {
			newTouchState = StickTouches.JOINT2;
		}
		else if (stick_touched) {
			newTouchState = StickTouches.STICK;
		}
		
		
		return newTouchState;
	}
	
	public boolean checkTouch(float touch_x, float touch_y) {
		m_touchState = m_checkTouched(touch_x, touch_y);
		
		m_isTouched = true;
		m_line_paint = GameData.line_paint;
		for (Joint j : joints) {
			j.setTouched(false);
		}
		
		switch (m_touchState) {
		case JOINT1:
			// TODO set glowing
			rotJoint = (joints.get(0) != m_rotationCentre ? m_rotationCentre : m_primitiveCentre);
			rotJoint.startGlowing(GameData.res.getColor(R.color.glow_color1), GameData.res.getColor(R.color.glow_color2));
			joints.get(0).setTouched(true);
			break;
		case JOINT2:
			rotJoint = (joints.get(1) != m_rotationCentre ? m_rotationCentre : m_primitiveCentre);
			rotJoint.startGlowing(GameData.res.getColor(R.color.glow_color1), GameData.res.getColor(R.color.glow_color2));
			joints.get(1).setTouched(true);
			break;
		case NONE:
			m_isTouched = false;
			break;
		case STICK:
			m_line_paint = GameData.line_touched_paint;
			break;
		default:
			break;
		}
		
		return m_isTouched;
	}
	
	public boolean isTouched() {
		return m_isTouched;
	}
	
	public void setUntouched() {
		if (!m_isTouched)
			return;
		
		m_isTouched = false;
		rotJoint.stopGlowing();
		m_touchState = StickTouches.NONE;
		if (!m_isOutOfBounds) {
			for (Joint j : joints)
				j.setTouched(false);
			m_line_paint = GameData.line_paint;
		}
	}
	
	public StickTouches getTouchState() {
		return m_touchState;
	}
	
	public void setPosition(float _x1, float _y1, float _x2, float _y2) {
		p1.x = _x1;
		p1.y = _y1;
		p2.x = _x2;
		p2.y = _y2;
		
		joints.get(0).setMyPoint(p1);
		joints.get(1).setMyPoint(p2);
		
		length = (float) Math.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y));
		if (length < GameData.min_stick_length) {
			p2.x = p1.x + 20;
		}
		
		float cos_theta = (_x2 - _x1) / length;
    	if (cos_theta > 1.0f)
    		cos_theta = 1.0f;
    	else if (cos_theta < -1.0f)
    		cos_theta = -1.0f;
    	
    	angle = (float)Math.acos(cos_theta);
    	if (_y2 - _y1 < 0)
    		angle = -angle;
	}
	
	public boolean isHigher(float y) {
		return (p1.y < y && p2.y < y);
	}

	@Override
	public PrimitiveType GetType() {
		return PrimitiveType.STICK;
	}

	@Override
	public float distTo(AbstractDrawingPrimitive primitive) {
		return Math.min(primitive.getDistToMe(p1.x, p1.y), primitive.getDistToMe(p2.x, p2.y));
	}

	@Override
	public float getDistToMe(float x_from, float y_from) {
		float d1 = (p2.x - x_from) * (p2.x - x_from) + (p2.y - y_from) * (p2.y - y_from);
		float d2 = (p1.x - x_from) * (p1.x - x_from) + (p1.y - y_from) * (p1.y - y_from);
		
		return Math.min(d1, d2);
	}

	@Override
	public void applyMove(float new_x, float new_y, float prev_x, float prev_y, boolean isScaling) {
		switch (m_touchState) {
		case JOINT1:
			if (!isScaling || !isScalable)
				rotateAroundRotationCentre(joints.get(0), new_x, new_y);
			else {
				Vector2DF v = new Vector2DF(new_x, new_y);
				float newLen = Vector2DF.sub(v, p2).getLength();
				scale(p2.x, p2.y, newLen / length);
			}
			checkOutOfBounds();
			break;
		case JOINT2:
			if (!isScaling || !isScalable)
				rotateAroundRotationCentre(joints.get(1), new_x, new_y);
			else {
				Vector2DF v = new Vector2DF(new_x, new_y);
				float newLen = Vector2DF.sub(v, p1).getLength();
				scale(p1.x, p1.y, newLen / length);
			}
			checkOutOfBounds();
			break;
		case STICK: {
			float dx = new_x - prev_x;
			float dy = new_y - prev_y;
			if (dx * dx + dy * dy <= GameData.min_dist_to_connect_square && hasParent)
				return;
			translate(dx, dy);
			disconnectFromParent();
			checkOutOfBounds();
			break;
		}
		default:
			break;
		}
		
		
	}
	
	
	public void checkOutOfBounds() {		
		boolean newOutOfBoundsState = !GameData.fieldRect.contains(p1.x - GameData.joint_radius_visible, p1.y - GameData.joint_radius_visible, 
			p1.x + GameData.joint_radius_visible, p1.y + GameData.joint_radius_visible);
		
		newOutOfBoundsState |= !GameData.fieldRect.contains(p2.x - GameData.joint_radius_visible, p2.y - GameData.joint_radius_visible, 
				p2.x + GameData.joint_radius_visible, p2.y + GameData.joint_radius_visible);
		
		if (newOutOfBoundsState != m_isOutOfBounds) {
			m_isOutOfBounds = newOutOfBoundsState;
			if (m_isOutOfBounds) {
				m_line_paint = GameData.line_drop_paint;
				for (Joint j : joints)
					j.setOutOfBounds(true);
			} else {
				m_line_paint = GameData.line_paint;
				for (Joint j : joints)
					j.setOutOfBounds(false);
				switch (m_touchState) {
				case JOINT1:
					joints.get(0).setTouched(true);
					break;
				case JOINT2:
					joints.get(1).setTouched(true);
					break;
				case STICK:
					m_line_paint = GameData.line_touched_paint;
					break;
				default:
					break;
				
				}
			}
		}
		super.checkOutOfBounds();
	}

	@Override
	public void setTransitiveFields(Context context) {
		super.setTransitiveFields(context);
		m_line_paint = GameData.line_paint;
		m_touchState = StickTouches.NONE;
	}

	@Override
	public AbstractDrawingPrimitive getCopy() {
		Stick stick = new Stick(this);
		return stick;
	}

	@Override
	public void setActiveColour() {
		for (Joint j : joints)
			j.setUnactive(false);
		m_line_paint = GameData.line_paint;
	}

	@Override
	public void setUnactiveColour() {
		for (Joint j : joints)
			j.setUnactive(true);
		m_line_paint = GameData.line_prev_frame_paint;
	}

	private void writeObject(java.io.ObjectOutputStream  stream) throws IOException {
		stream.writeObject(p1);
		stream.writeObject(p2);
		stream.writeFloat(length);
		stream.writeFloat(angle);
		
	}
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		p1 = (Vector2DF) stream.readObject();
		p2 = (Vector2DF) stream.readObject();
		length = stream.readFloat();
		angle = stream.readFloat();
	}
	
	@Override
	public void drawBlendingWithSuccessor(Canvas canvas, float t) {
		if (m_successor != null) {
			Stick suc = (Stick)m_successor;
			GameData.mixTwoColors(m_line_paint.getColor(), suc.m_line_paint.getColor(), 1 - t);
			
			float fi = (suc.angle - angle) * t + angle;
			float dr_x = (suc.p1.x - p1.x) * t;
			float dr_y = (suc.p1.y - p1.y) * t;
			float len = (1 - t) * length + t * suc.length;
			
			float x1 = p1.x + dr_x;
			float y1 = p1.y + dr_y;
			float x2 = (float) (x1 + len * Math.cos(fi));
			float y2 = (float) (y1 + len * Math.sin(fi));
			
			float dx = GameData.joint_radius_visible * (x2 - x1) / len;
			float dy = GameData.joint_radius_visible * (y2 - y1) / len;
			
			canvas.drawLine(x1 + dx, y1 + dy, x2 - dx, y2 - dy, GameData.blended_line_paint);
			joints.get(0).drawBlendingWithSuccessor(canvas, suc.joints.get(0), t, x1, y1);
			joints.get(1).drawBlendingWithSuccessor(canvas, suc.joints.get(1), t, x2, y2);
		} else {
			drawBlendingWithNoSuccessor(canvas, t);
		}
	}

	@Override
	public void drawBlendingWithNoPredecessor(Canvas canvas, float t) {
		GameData.mixTwoColors(Color.argb(0, 0, 0, 0), m_line_paint.getColor(), 1 - t);
		float dx = GameData.joint_radius_visible * (p2.x - p1.x) / length;
		float dy = GameData.joint_radius_visible * (p2.y - p1.y) / length;
		
		canvas.drawLine(p1.x + dx, p1.y + dy, p2.x - dx, p2.y - dy, GameData.blended_line_paint);
		joints.get(0).drawBlendingWithNoPredecessor(canvas, t);
		joints.get(1).drawBlendingWithNoPredecessor(canvas, t);
	}
	
	@Override
	public void drawBlendingWithNoSuccessor(Canvas canvas, float t) {
		GameData.mixTwoColors(Color.argb(0, 0, 0, 0), m_line_paint.getColor(), t);
		canvas.drawLine(p1.x, p1.y, p2.x, p2.y, GameData.blended_line_paint);
		joints.get(0).drawBlendingWithNoSuccessor(canvas, t);
		joints.get(1).drawBlendingWithNoSuccessor(canvas, t);
	}

	@Override
	public Joint getTouchedJoint() {
		switch (m_touchState) {
		case JOINT1:
			return joints.get(0);
		case JOINT2:
			return joints.get(1);
		default:
			return null;
		}
	}

}
