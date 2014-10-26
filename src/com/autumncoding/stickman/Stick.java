package com.autumncoding.stickman;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;

public class Stick extends View implements DrawingPrimitive {

	private Vector2DF p1;
	private Vector2DF p2;
	
	private float length;
	private float angle;
	
	private Paint m_line_paint;
	private Paint m_joint1_paint;
	private Paint m_joint2_paint;
	
	/*********************** touch data *********************************************/
	private boolean is_touched;	
	
	enum StickTouches {
		JOINT1, 
		JOINT2,
		STICK,
		NONE
	}
	
	enum JOINTS {
		JOINT1,
		JOINT2,
		BOTH, 
		NONE
	}
	
	private ArrayList<DrawingPrimitive> childrenPrimitives;
	private DrawingPrimitive parentPrimitive = null;
	private JOINTS rotateless_joint;
	private StickTouches touch_state = StickTouches.NONE;
	
	public Stick(Context context) {
		super(context);
		p1 = new Vector2DF();
		p2 = new Vector2DF();
		
		p1.x = 100;
		p1.y = 100;
		length = 100;
		p2.x = p1.x + length * (float)Math.cos(angle);
		p2.y = p1.y + length * (float)Math.sin(angle);
		angle = 0;
		is_touched = false;
		m_line_paint = GameData.line_paint;
		m_joint1_paint = GameData.joint_paint;
		m_joint2_paint = GameData.joint_paint;
		rotateless_joint = JOINTS.NONE;
		childrenPrimitives = new ArrayList<DrawingPrimitive>();
	}
	
	public void copy(DrawingPrimitive primitive) {
		if (primitive == null)
			return;
		if (primitive.GetType() != PrimitiveType.STICK)
			return;
		
		Stick st = (Stick)primitive;
		angle = st.angle;
		p1.x = st.p1.x;
		p1.y = st.p1.y;
		p2.x = st.p2.x;
		p2.y = st.p2.y;
		length = st.length;
		parentPrimitive = null;
		m_line_paint = st.m_line_paint;
		m_joint1_paint = st.m_joint1_paint;
		m_joint2_paint = st.m_joint2_paint;
		is_touched = st.is_touched;
		touch_state = st.touch_state;
	}
	
	public void rotate(float fi, float cx, float cy) {
		float new_x = (float) (cx + (p1.x - cx) * Math.cos(fi) - (p1.y - cy) * Math.sin(fi));
		float new_y = (float) (cy + (p1.x - cx) * Math.sin(fi) + (p1.y - cy) * Math.cos(fi));
		p1.x = new_x;
		p1.y = new_y;
		
		new_x = (float) (cx + (p2.x - cx) * Math.cos(fi) - (p2.y - cy) * Math.sin(fi));
		new_y = (float) (cy + (p2.x - cx) * Math.sin(fi) + (p2.y - cy) * Math.cos(fi));
		p2.x = new_x;
		p2.y = new_y;
    	
		for (DrawingPrimitive pr : childrenPrimitives) { 
			pr.rotate(fi, cx, cy);
		}
	}
	
	public void rotateAroundJoint1(float x, float y) {
		if (rotateless_joint == JOINTS.JOINT2)
			return;
		float dl = (float)Math.sqrt((x - p1.x) * (x - p1.x) + (y - p1.y) * (y - p1.y));
		if (dl == 0)
			return;
    	float cos_theta = ((x - p1.x) * (p2.x - p1.x) + (y - p1.y) * (p2.y - p1.y)) / (dl * length);
    	
    	if (cos_theta > 1.0f)
    		cos_theta = 1.0f;
    	else if (cos_theta < -1.0f)
    		cos_theta = -1.0f;
    	
    	float theta = (float)Math.acos(cos_theta);
    	
    	if ((p2.x - p1.x) * (y - p1.y) - (x - p1.x) * (p2.y - p1.y) < 0)
    		theta = -theta;
    	rotate(theta, p1.x, p1.y);
    	if (Float.isNaN(p1.x))
    	{
    		p1.x = 0;
    		p2.x = 0;
    		
    	}
	}
	
	public void rotateAroundJoint2(float new_x2, float new_y2) {
		float dl = (float)Math.sqrt((new_x2 - p2.x) * (new_x2 - p2.x) + (new_y2 - p2.y) * (new_y2 - p2.y));
		if (dl == 0)
			return;
    	float cos_theta = ((new_x2 - p2.x) * (p1.x - p2.x) + (new_y2 - p2.y) * (p1.y - p2.y)) / (dl * length);
    	if (cos_theta > 1.0f)
    		cos_theta = 1.0f;
    	else if (cos_theta < -1.0f)
    		cos_theta = -1.0f;
    	
    	float theta = (float)Math.acos(cos_theta);
    	
    	if ((p1.x - p2.x) * (new_y2 - p2.y) - (new_x2 - p2.x) * (p1.y - p2.y) < 0)
    		theta = -theta;
    	rotate(theta, p2.x, p2.y);
	}
	
	public void translate(float dx, float dy) {	
		p1.x += dx;
		p1.y += dy;
		p2.x += dx;
		p2.y += dy;
    	
		for (DrawingPrimitive pr : childrenPrimitives) {
			pr.translate(dx,  dy);
		}
	}
	
	public void scale(float cx, float cy, float rate) {
		if (parentPrimitive != null)
			return;
		
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
		
    	length = temp_length;
	}
	
	@Override
	public void draw(Canvas canvas) { 
		canvas.drawLine(p1.x, p2.x, p2.x, p2.y, m_line_paint);
		canvas.drawCircle(p2.x, p2.y, GameData.joint_radius_visible, m_joint2_paint);
		canvas.drawCircle(p1.x, p1.y, GameData.joint_radius_visible, m_joint1_paint);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		return super.onTouchEvent(event);
	}
	
	
	private StickTouches m_checkTouched(float touch_x, float touch_y) {
		float dx = p1.x - touch_x;
		float dy = p1.y - touch_y;
		boolean joint1_touched = (dx * dx + dy * dy <= GameData.joint_radius_touchable_square);
				
		dx = p2.x - touch_x;
		dy = p2.y - touch_y;
		boolean joint2_touched = false;
		
		if (!joint1_touched)
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
		touch_state = m_checkTouched(touch_x, touch_y);
		
		is_touched = true;
		m_joint1_paint = GameData.joint_paint;
		m_joint2_paint = GameData.joint_paint;
		m_line_paint = GameData.line_paint;
		
		switch (touch_state) {
		case JOINT1:
			m_joint1_paint = GameData.joint_touched_paint;
			break;
		case JOINT2:
			m_joint2_paint = GameData.joint_touched_paint;
			break;
		case NONE:
			is_touched = false;
			break;
		case STICK:
			m_line_paint = GameData.line_touched_paint;
			break;
		default:
			break;
		}
		
		return is_touched;
	}
	
	public boolean isTouched() {
		return is_touched;
	}
	
	public void setUntouched() {
		if (!is_touched)
			return;
		
		is_touched = false;
		touch_state = StickTouches.NONE;
		m_joint1_paint = GameData.joint_paint;
		m_joint2_paint = GameData.joint_paint;
		m_line_paint = GameData.line_paint;
	}
	
	public StickTouches getTouchState() {
		return touch_state;
	}
	
	public float distTo(Stick stick) {
		return (stick.p2.y - p1.y) * (stick.p2.y - p1.y) + (stick.p2.x - p1.x) * (stick.p2.x - p1.x);
	}
	
	
	public void addChild(DrawingPrimitive p) {
		if (!childrenPrimitives.contains(p))
			childrenPrimitives.add(p);
	}
	
	
	public void setNotConnected() {	
		if (parentPrimitive != null) {
			parentPrimitive.removeChild(this);
			parentPrimitive = null;
		}
	}
	
	public void setPosition(float _x1, float _y1, float _x2, float _y2) {
		p1.x = _x1;
		p1.y = _y1;
		p2.x = _x2;
		p2.y = _y2;
		length = (float) Math.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y));
	}
	
	public boolean isHigher(float y) {
		return (p1.y < y && p2.y < y);
	}

	@Override
	public PrimitiveType GetType() {
		return PrimitiveType.STICK;
	}

	@Override
	public float distTo(DrawingPrimitive primitive) {
		return primitive.getDistToMe(p1.x, p1.y);
	}

	@Override
	public void connectTo(DrawingPrimitive primitive) {
		switch (primitive.GetType()) {
		case JOINT: {
			CentralJoint joint = (CentralJoint)primitive;
			Vector2DF e = new Vector2DF(joint.getMyX(), joint.getMyY());
			float len1 = Vector2DF.sub(e, p1).getLength();
			float len2 = Vector2DF.sub(e, p2).getLength();
			
			float dx;
			float dy;
			if (len2 < len1) {
				dx = joint.getMyX() - p2.x;
				dy = joint.getMyY() - p2.y;
			} else {
				dx = joint.getMyX() - p1.x;
				dy = joint.getMyY() - p1.y;
			}
			if (parentPrimitive == joint && dx == 0 && dy == 0)
				return;
			
			translate(dx, dy);
			break;
		}
		case STICK: {
			Stick stick = (Stick)primitive;
			float len1 = Vector2DF.sub(stick.p1, p1).getLength();
			float len2 = Vector2DF.sub(stick.p2, p1).getLength();
			float len3 = Vector2DF.sub(stick.p1, p2).getLength();
			float len4 = Vector2DF.sub(stick.p2, p2).getLength();
			
			Vector2DF dv = null;
			if (len1 <= len2 && len1 <= len3 && len1 <= len4) {
				dv = Vector2DF.sub(stick.p1, p1);
			} else if (len2 <= len1 && len2 <= len3 && len2 <= len4) {
				dv = Vector2DF.sub(stick.p2, p1);
			} else if (len3 <= len2 && len3 <= len2 && len3 <= len4) {
				dv = Vector2DF.sub(stick.p1, p2);
			} else {
				dv = Vector2DF.sub(stick.p2, p2);
			}
			
			if (parentPrimitive == stick && dv.y == 0 && dv.x == 0)
				return;
			translate(dv.x, dv.y);
			break;
		}
		default:
			break;
		
		}
		
		parentPrimitive = primitive;
		parentPrimitive.addChild(this);
	}

	@Override
	public float getDistToMe(float x_from, float y_from) {
		return (p2.x - x_from) * (p2.x - x_from) + (p2.y - y_from) * (p2.y - y_from);
	}

	@Override
	public void applyMove(float new_x, float new_y, float prev_x, float prev_y, boolean isScaling) {
		switch (touch_state) {
		case JOINT1:
			if (!isScaling)
				rotateAroundJoint2(new_x, new_y);
			else {
				Vector2DF v = new Vector2DF(new_x, new_y);
				float newLen = Vector2DF.sub(v, p2).getLength();
				scale(p2.x, p2.y, newLen / length);
			}
			break;
		case JOINT2:
			if (!isScaling)
				rotateAroundJoint1(new_x, new_y);
			else {
				Vector2DF v = new Vector2DF(new_x, new_y);
				float newLen = Vector2DF.sub(v, p1).getLength();
				scale(p1.x, p1.y, newLen / length);
			}
			break;
		case STICK:
			translate(new_x - prev_x, new_y - prev_y);
			break;
		default:
			break;
		}
	}

	@Override
	public void removeChild(DrawingPrimitive p) {
		childrenPrimitives.remove(p);
	}

}
