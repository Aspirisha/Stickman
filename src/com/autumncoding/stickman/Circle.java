package com.autumncoding.stickman;

import java.util.ArrayList;
import java.util.LinkedList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

public class Circle extends AbstractDrawingPrimitive {
	private Vector2DF m_centre;
	private Vector2DF m_jointPoint;
	private float r;
	private float angle;
	
	/*********************** touch data *********************************************/
	
	private Paint m_line_paint;
	private Paint m_joint_paint;
	
	
	enum CircleTouches {
		JOINT, 
		CIRCLE,
		NONE
	}
	
	private CircleTouches touch_state;
	
	public Circle(Context context) {
		super(context);
		m_centre = new Vector2DF(100, 100);
		r = 10;
		
		joints = new ArrayList<Joint>(1);
		
		m_jointPoint = new Vector2DF(m_centre.x + r, m_centre.y);
		joints.add(new Joint(this, m_jointPoint));
		
		angle = 0;
		
		m_line_paint = GameData.line_paint;
		m_joint_paint = GameData.joint_paint;
		touch_state = CircleTouches.NONE;
	}
	
	private CircleTouches m_checkTouched(float touch_x, float touch_y) {
		float dx = m_jointPoint.x - touch_x;
		float dy = m_jointPoint.y - touch_y;

		CircleTouches newTouchState = CircleTouches.NONE;
		
		boolean joint_touched = (dx * dx + dy * dy <= GameData.joint_radius_touchable_square);
		
		boolean circle_touched = false;
		if (!joint_touched) {
			float dist = (m_centre.x - touch_x) * (m_centre.x - touch_x) + (m_centre.y - touch_y) * (m_centre.y - touch_y);
			float min_dist_in = (r - GameData.circle_touchable_dr) * (r - GameData.circle_touchable_dr);
			float max_dist_in = (r + GameData.circle_touchable_dr) * (r + GameData.circle_touchable_dr);
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
		touch_state = m_checkTouched(touch_x, touch_y);
		
		m_isTouched = true;
		m_joint_paint = GameData.joint_paint;
		m_line_paint = GameData.line_paint;
		
		switch (touch_state) {
		case CIRCLE:
			m_line_paint = GameData.line_touched_paint;
			break;
		case JOINT:
			m_joint_paint = GameData.joint_touched_paint;
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
	public void draw(Canvas canvas) {
		canvas.drawCircle(m_centre.x, m_centre.y, r, m_line_paint);
		canvas.drawCircle(m_jointPoint.x, m_jointPoint.y, GameData.joint_radius_visible, m_joint_paint);
	}

	@Override
	public boolean isTouched() {
		return m_isTouched;
	}

	@Override
	public float distTo(DrawingPrimitive primitive) {
		return primitive.getDistToMe(m_jointPoint.x, m_jointPoint.y);
	}


	@Override
	public PrimitiveType GetType() {
		return PrimitiveType.CIRCLE;
	}
	
	public void rotate(float fi, float cx, float cy) {
		float new_x = (float) (cx + (m_jointPoint.x - cx) * Math.cos(fi) - (m_jointPoint.y - cy) * Math.sin(fi));
		float new_y = (float) (cy + (m_jointPoint.x - cx) * Math.sin(fi) + (m_jointPoint.y - cy) * Math.cos(fi));
		m_jointPoint.x = new_x;
		m_jointPoint.y = new_y;
		
		new_x = (float) (cx + (m_centre.x - cx) * Math.cos(fi) - (m_centre.y - cy) * Math.sin(fi));
		new_y = (float) (cy + (m_centre.x - cx) * Math.sin(fi) + (m_centre.y - cy) * Math.cos(fi));
		m_centre.x = new_x;
		m_centre.y = new_y;
    	
		joints.get(0).setMyPoint(m_jointPoint);
		
		/*for (DrawingPrimitive pr : childPrimitives) { 
			pr.rotate(fi, cx, cy);
		}*/
	}
	
	public void translate(float dx, float dy) {	
		m_centre.x += dx;
		m_centre.y += dy;
		m_jointPoint.x += dx;
		m_jointPoint.y += dy;
		
		joints.get(0).setMyPoint(m_jointPoint);
    	
		/*for (DrawingPrimitive pr : childPrimitives) { 
			pr.translate(dx,  dy);
		}*/
	}
	
	public void setPosition(float _x, float _y, float _angle) {
		m_centre.x = _x;
		m_centre.y = _y;
		
		m_jointPoint.x = m_centre.x + r;
		m_jointPoint.y = m_centre.y;
		
		joints.get(0).setMyPoint(m_jointPoint);
		
		angle = _angle;
		rotate(angle, m_centre.x, m_centre.y);
	}

	@Override
	public float getDistToMe(float from_x, float from_y) {
		return (m_jointPoint.x - from_x) * (m_jointPoint.x - from_x) + (m_centre.y - from_y) * (m_centre.y - from_y);
	}
	
	public void copy(DrawingPrimitive primitive) {
		if (primitive == null)
			return;
		
		if (primitive.GetType() != PrimitiveType.CIRCLE)
			return;
		Circle cir = (Circle)primitive;
		
		m_centre.x = cir.m_centre.x;
		m_centre.y = cir.m_centre.y;
		r = cir.r;
		
		m_jointPoint.x = cir.m_jointPoint.x;
		m_jointPoint.y = cir.m_jointPoint.y;
		m_centre.y = cir.m_centre.y;
		
		angle = cir.angle;
		m_isTouched = cir.m_isTouched;

		touch_state = cir.touch_state;
	}

	@Override
	public void setUntouched() {
		if (!m_isTouched)
			return;
		
		m_isTouched = false;
		touch_state = CircleTouches.NONE;
		m_joint_paint = GameData.joint_paint;
		m_line_paint = GameData.line_paint;
	}
	
	public void stretch(float new_x, float new_y) {
		float new_r = (float)Math.sqrt((m_centre.x - new_x) * (m_centre.x - new_x) + (m_centre.y - new_y) * (m_centre.y - new_y));
		
		if (new_r > GameData.min_circle_radius && new_r < GameData.max_circle_radius) {
			r = new_r;
			
			m_jointPoint.x = (float) (m_centre.x + r * Math.cos(angle));
			m_centre.y = (float) (m_centre.y + r * Math.sin(angle));
		}
	}
	
	public void applyMove(float new_x, float new_y, float prev_x, float prev_y, boolean isScaling) {
		switch (touch_state) {
		case CIRCLE:
			translate(new_x - prev_x, new_y - prev_y);
			break;
		case JOINT: {
			if (!isScaling || !isScalable) {
				float len1 = (float) Math.sqrt((new_x - m_centre.x) * (new_x - m_centre.x) + (new_y - m_centre.y) * (new_y - m_centre.y));
				float len2 = (float) Math.sqrt((m_jointPoint.x - m_centre.x) * (m_jointPoint.x - m_centre.x) + (m_centre.y - m_centre.y) * (m_centre.y - m_centre.y));
				float cos_theta = (new_x - m_centre.x) * (m_jointPoint.x - m_centre.x) + (new_y - m_centre.y) * (m_jointPoint.y - m_centre.y) / (len1 * len2);
				
		    	if (cos_theta > 1.0f)
		    		cos_theta = 1.0f;
		    	else if (cos_theta < -1.0f)
		    		cos_theta = -1.0f;
		    	
				float theta = (float) Math.acos(cos_theta); // NB: using acos is not very cool just so: need to be sue fi <= 180
				rotate(theta, m_centre.x, m_centre.y);
			} else {
				Vector2DF v = new Vector2DF(new_x, new_y);
				float newLen = Vector2DF.dist(v, m_centre);
				scale(m_centre.x, m_centre.y, newLen / r);
			}
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
		float temp_r = r * rate;
		if (temp_r < GameData.min_stick_length)
		{
			temp_r = GameData.min_circle_radius;
			rate = temp_r / r;
		}
		
		m_jointPoint.x = cx + rate * (m_jointPoint.x - cx);
		m_jointPoint.y = cy + rate * (m_jointPoint.y - cy);
		
		joints.get(0).setMyPoint(m_jointPoint);
		
    	r = temp_r;
	}
}
	
