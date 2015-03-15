package com.autumncoding.stickman;

import java.io.IOException;
import java.util.ArrayList;

import com.autamncoding.stickman.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Circle extends AbstractDrawingPrimitive {
	private static final long serialVersionUID = 5019188822441365318L;
	private Vector2DF m_centre;
	private Vector2DF m_jointPoint;
	private float m_radius;
	private float angle = 0;
	
	/*********************** touch data *********************************************/
	
	enum CircleTouches {
		JOINT, 
		CIRCLE,
		NONE
	}
	
	private transient CircleTouches m_touchState = CircleTouches.NONE;
	
	public Circle(Context context) {
		super(context);
		m_centre = new Vector2DF(100, 100);
		m_radius = 10;
		
		m_jointPoint = new Vector2DF(m_centre.x + m_radius, m_centre.y);
		joints.add(new Joint(this, m_jointPoint));
		joints.get(0).setCentral(true);	
		m_primitiveCentre = new Joint(this, m_centre);
		m_primitiveCentre.setInvisible();
		updateJointColors();
		m_rotationCentre = joints.get(0);
		rotJoint = m_rotationCentre;
	}
	
	public Circle(Circle cir) {
		super(cir);
		m_centre = new Vector2DF(cir.m_centre);
		m_jointPoint = new Vector2DF(cir.m_jointPoint);
		m_radius = cir.m_radius;
		joints.add(new Joint(this, m_jointPoint));
		joints.get(0).setCentral(true);
		m_primitiveCentre = new Joint(this, m_centre);
		m_primitiveCentre.setInvisible();
		updateJointColors();
		m_rotationCentre = joints.get(0);
		rotJoint = m_rotationCentre;
		angle = cir.angle;
	}
	
	private CircleTouches m_checkTouched(float touch_x, float touch_y) {
		float dx = m_jointPoint.x - touch_x;
		float dy = m_jointPoint.y - touch_y;

		CircleTouches newTouchState = CircleTouches.NONE;
		boolean joint_touched = (dx * dx + dy * dy <= GameData.joint_radius_touchable_square);
		if (joints.get(0).isChild() && joint_touched) {
			newTouchState = CircleTouches.NONE;
			return newTouchState;
		}
		
		boolean circle_touched = false;
		if (!joint_touched) {
			float dist = (m_centre.x - touch_x) * (m_centre.x - touch_x) + (m_centre.y - touch_y) * (m_centre.y - touch_y);
			float min_dist_in = (m_radius - GameData.circle_touchable_dr) * (m_radius - GameData.circle_touchable_dr);
			float max_dist_in = (m_radius + GameData.circle_touchable_dr) * (m_radius + GameData.circle_touchable_dr);
			circle_touched = (dist >= min_dist_in && dist <= max_dist_in);
		}
				
		if (joint_touched) {
			newTouchState = CircleTouches.JOINT;
		}
		else if (circle_touched) {
			newTouchState = CircleTouches.CIRCLE;
		}
		
		
		return newTouchState;
	}
	
	@Override
	public boolean checkTouch(float touch_x, float touch_y) {
		m_touchState = m_checkTouched(touch_x, touch_y);
		
		m_isTouched = true;
		m_line_paint = (hasParent ? GameData.line_paint : GameData.root_line_paint);
		joints.get(0).setTouched(false);
		
		switch (m_touchState) {
		case CIRCLE:
			m_line_paint = GameData.line_touched_paint;
			break;
		case JOINT:
			rotJoint = (joints.get(0) != m_rotationCentre ? m_rotationCentre : m_primitiveCentre);
			rotJoint.startGlowing(GameData.res.getColor(R.color.glow_color1), GameData.res.getColor(R.color.glow_color2));
			joints.get(0).setTouched(true);
			break;
		case NONE:
			m_isTouched = false;
			break;
		default:
			break;
		}
		return m_isTouched;
	}

	@Override
	public void drawLine(Canvas canvas) {
		canvas.drawCircle(m_centre.x, m_centre.y, m_radius, m_line_paint);
	}

	@Override
	public boolean isTouched() {
		return m_isTouched;
	}

	@Override
	public float distTo(AbstractDrawingPrimitive primitive) {
		return primitive.getDistToMe(m_jointPoint.x, m_jointPoint.y);
	}


	@Override
	public PrimitiveType GetType() {
		return PrimitiveType.CIRCLE;
	}
	
	@Override
	public void rotate(float fi, float cx, float cy, boolean rotateChildren) {
		float new_x = (float) (cx + (m_jointPoint.x - cx) * Math.cos(fi) - (m_jointPoint.y - cy) * Math.sin(fi));
		float new_y = (float) (cy + (m_jointPoint.x - cx) * Math.sin(fi) + (m_jointPoint.y - cy) * Math.cos(fi));
		m_jointPoint.x = new_x;
		m_jointPoint.y = new_y;
		
		new_x = (float) (cx + (m_centre.x - cx) * Math.cos(fi) - (m_centre.y - cy) * Math.sin(fi));
		new_y = (float) (cy + (m_centre.x - cx) * Math.sin(fi) + (m_centre.y - cy) * Math.cos(fi));
		m_centre.x = new_x;
		m_centre.y = new_y;
    	
		joints.get(0).setMyPoint(m_jointPoint);
		m_primitiveCentre.setMyPoint(m_centre);
		angle += fi;
		
		/*if (angle > Math.PI)
			angle = (float) (2*Math.PI - angle);
		else if (angle < -Math.PI)
			angle = (float) (2*Math.PI + angle);*/
		if (rotateChildren) {
			for (Connection con : m_childrenConnections) {
				con.primitive.rotate(fi, cx, cy, true);
			}
		}
	}
	
	public void translate(float dx, float dy) {	
		m_centre.x += dx;
		m_centre.y += dy;
		m_jointPoint.x += dx;
		m_jointPoint.y += dy;
		
		joints.get(0).setMyPoint(m_jointPoint);
    	m_primitiveCentre.translate(dx, dy);
    	
		for (Connection con : m_childrenConnections) {
			con.primitive.translate(dx, dy);
		}
	}
	
	public void setPosition(float _x, float _y, float joint_x, float joint_y, float _r) {
		m_centre.x = _x;
		m_centre.y = _y;
		m_primitiveCentre.setMyPoint(m_centre);
		m_radius = _r;
		
		if (m_radius < GameData.min_circle_radius)
			m_radius = GameData.min_circle_radius;
		m_jointPoint.x = joint_x;
		m_jointPoint.y = joint_y;
		
		float cos_theta = (joint_x - _x) / _r;
    	if (cos_theta > 1.0f)
    		cos_theta = 1.0f;
    	else if (cos_theta < -1.0f)
    		cos_theta = -1.0f;
    	
    	angle = (float)Math.acos(cos_theta);
    	if (joint_y - _y < 0)
    		angle = -angle;
		
		joints.get(0).setMyPoint(m_jointPoint);
	}

	@Override
	public float getDistToMe(float from_x, float from_y) {
		return (m_jointPoint.x - from_x) * (m_jointPoint.x - from_x) + (m_jointPoint.y - from_y) * (m_jointPoint.y - from_y);
	}

	@Override
	public void setUntouched() {
		if (!m_isTouched)
			return;
		
		rotJoint.stopGlowing();
		m_isTouched = false;
		m_touchState = CircleTouches.NONE;
		
		if (!m_isOutOfBounds) {
			joints.get(0).setTouched(false);
			m_line_paint = (hasParent ? GameData.line_paint : GameData.root_line_paint);
		}
	}
	
	@Override
	public void applyMove(float new_x, float new_y, float prev_x, float prev_y, boolean isScaling) {
		switch (m_touchState) {
		case CIRCLE: {
			float dx = new_x - prev_x;
			float dy = new_y - prev_y;
			if (dx * dx + dy * dy <= GameData.min_dist_to_connect_square && hasParent)
				return;
			disconnectFromParent();
			translate(dx, dy);
			checkOutOfBounds();
			break;
		}
		case JOINT: {
			if (!isScaling || !isScalable) {
				float len1 = (float) Math.sqrt((new_x - m_centre.x) * (new_x - m_centre.x) + (new_y - m_centre.y) * (new_y - m_centre.y));
				float len2 = m_radius;
				float cos_theta = ((new_x - m_centre.x) * (m_jointPoint.x - m_centre.x) + (new_y - m_centre.y) * (m_jointPoint.y - m_centre.y)) / (len1 * len2);
				
		    	if (cos_theta > 1.0f)
		    		cos_theta = 1.0f;
		    	else if (cos_theta < -1.0f)
		    		cos_theta = -1.0f;
		    	
				float theta = (float) Math.acos(cos_theta); // NB: using acos is not very cool just so: need to be sue fi <= 180
				if ((m_jointPoint.x - m_centre.x) * (new_y - m_centre.y) - (new_x - m_centre.x) * (m_jointPoint.y - m_centre.y) < 0)
		    		theta = -theta;

				rotateAroundRotationCentre(joints.get(0), new_x, new_y);
			} else {
				Vector2DF v = new Vector2DF(new_x, new_y);
				float newLen = Vector2DF.dist(v, m_centre);
				scale(m_centre.x, m_centre.y, newLen / m_radius);
			}
			checkOutOfBounds();
			break;
		}
		case NONE:
			break;
		default:
			break;
		}
	}

	@Override
	public void scale(float cx, float cy, float rate) {
		super.scale(cx, cy, rate);
		
		float temp_r = m_radius * rate;
		if (temp_r < GameData.min_stick_length)
		{
			temp_r = GameData.min_circle_radius;
			rate = temp_r / m_radius;
		}
		
		m_jointPoint.x = cx + rate * (m_jointPoint.x - cx);
		m_jointPoint.y = cy + rate * (m_jointPoint.y - cy);
		
		joints.get(0).setMyPoint(m_jointPoint);
		
    	m_radius = temp_r;
	}
	
	public void checkOutOfBounds() {	
		boolean newOutOfBoundsState = !GameData.fieldRect.contains(m_centre.x - m_radius, m_centre.y - m_radius, m_centre.x + m_radius, m_centre.y + m_radius);
		newOutOfBoundsState |= !GameData.fieldRect.contains(m_jointPoint.x - GameData.joint_radius_visible, m_jointPoint.y - GameData.joint_radius_visible, 
				m_jointPoint.x + GameData.joint_radius_visible, m_jointPoint.y + GameData.joint_radius_visible);
		
		if (newOutOfBoundsState != m_isOutOfBounds) {
			m_isOutOfBounds = newOutOfBoundsState;
			if (m_isOutOfBounds) {
				m_line_paint = GameData.line_drop_paint;
				joints.get(0).setOutOfBounds(true);
			} else {
				m_line_paint = (hasParent ? GameData.line_paint : GameData.root_line_paint);
				joints.get(0).setOutOfBounds(false);
				switch (m_touchState) {
				case JOINT:
					joints.get(0).setTouched(true);
					break;
				case CIRCLE:
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
	public void setTransientFields(Context context) {
		super.setTransientFields(context);
		m_touchState = CircleTouches.NONE;
	}

	@Override
	public AbstractDrawingPrimitive getCopy() {
		Circle circle = new Circle(this);
		return circle;
	}
	
	@Override
	public void setActiveColour() {
		joints.get(0).updateColor();
		m_line_paint = (hasParent ? GameData.line_paint : GameData.root_line_paint);
	}

	@Override
	public void setUnactiveColour() {
		joints.get(0).setUnactive(true);
		m_line_paint = GameData.line_prev_frame_paint;
	}

	private void writeObject(java.io.ObjectOutputStream  stream) throws IOException {
		stream.writeObject(m_centre);
		stream.writeObject(m_jointPoint);
		stream.writeFloat(m_radius);		
	}
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		m_centre = (Vector2DF) stream.readObject();
		m_jointPoint = (Vector2DF) stream.readObject();
		m_radius = stream.readFloat();
	}

	@Override
	public void drawLineBlendingWithSuccessor(Canvas canvas, float t) {
		if (m_successor != null) {
			Circle suc = (Circle)m_successor;
			canvas.drawCircle((1 - t) * m_centre.x + t * suc.m_centre.x, 
					(1 - t) * m_centre.y + t * suc.m_centre.y, (1 - t) * m_radius + t * suc.m_radius, GameData.root_line_paint);
		} else {
			drawLineBlendingWithNoSuccessor(canvas, t);
		}
	}
	
	@Override
	public void drawJointsBlendingWithSuccessor(Canvas canvas, float t) {
		if (m_successor != null) {
			Circle suc = (Circle)m_successor;
			float x = (suc.m_jointPoint.x - m_jointPoint.x) * t + m_jointPoint.x;
			float y = (suc.m_jointPoint.y - m_jointPoint.y) * t + m_jointPoint.y;
			joints.get(0).drawBlendingWithSuccessor(canvas, x, y);
		} else {
			drawJointsBlendingWithNoSuccessor(canvas, t);
		}
	}

	@Override
	public void drawLineBlendingWithNoSuccessor(Canvas canvas, float t) {
		GameData.mixTwoColors(Color.argb(0, 0, 0, 0), GameData.root_line_paint.getColor(), t);
		canvas.drawCircle(m_centre.x, m_centre.y, m_radius, GameData.blended_line_paint);
	}
	
	@Override
	public void drawJointsBlendingWithNoSuccessor(Canvas canvas, float t) {
		joints.get(0).drawBlendingWithNoSuccessor(canvas, t);
	}
	
	@Override
	public Joint getTouchedJoint() {
		switch (m_touchState) {
		case JOINT:
			return joints.get(0);
		default:
			return null;
		}
	}
	
}
	
