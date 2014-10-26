package com.autumncoding.stickman;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

public class Stick extends View implements DrawingPrimitive {

	private float x1;
	private float y1;
	private float x2;
	private float y2;
	private float stretch_line_x; // this point is between (x1, y1) and (x2, y2); It is near x1, y1
	private float stretch_line_y;
	
	private float length;
	private float angle;
	
	private Paint m_line_paint;
	private Paint m_joint1_paint;
	private Paint m_joint2_paint;
	private Paint m_stretch_line_paint;
	
	private boolean is_touched;
	private boolean is_menu_sample;
	
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
	
	ArrayList<Stick> child_sticks;
	Stick parent_stick;
	CentralJoint parent_joint;
	JOINTS rotateless_joint;
	StickTouches touch_state = StickTouches.NONE;
	
	
	public Stick(Context context) {
		super(context);

		x1 = 100;
		y1 = 100;
		length = 100;
		x2 = x1 + length * (float)Math.cos(angle);
		y2 = y1 + length * (float)Math.sin(angle);
		angle = 0;
		is_touched = false;
		m_line_paint = GameData.line_paint;
		m_joint1_paint = GameData.joint_paint;
		m_joint2_paint = GameData.joint_paint;
		m_stretch_line_paint = GameData.stretch_line_paint;
		rotateless_joint = JOINTS.NONE;
		child_sticks = new ArrayList<Stick>();
		parent_stick = null;
		parent_joint = null;
		stretch_line_x = x1 + GameData.stretch_line_length * (x2 - x1) / length;
    	stretch_line_y = y1 + GameData.stretch_line_length * (y2 - y1) / length;
	   // measure(100, 10);
	}
	
	public void copy(DrawingPrimitive primitive) {
		if (primitive == null)
			return;
		if (primitive.GetType() != PrimitiveType.STICK)
			return;
		
		Stick st = (Stick)primitive;
		angle = st.angle;
		x1 = st.x1;
		y1 = st.y1;
		x2 = st.x2;
		y2 = st.y2;
		length = st.length;
		parent_stick = null;
		parent_joint = null;
		m_line_paint = st.m_line_paint;
		m_joint1_paint = st.m_joint1_paint;
		m_joint2_paint = st.m_joint2_paint;
		is_touched = st.is_touched;
		touch_state = st.touch_state;
		stretch_line_x = st.stretch_line_x;
		stretch_line_y = st.stretch_line_y;
		m_stretch_line_paint = st.m_stretch_line_paint;
	}

	public void use_as_menu_sample(boolean as_menu_sample) {
		is_menu_sample = as_menu_sample;
	}
	
	public void rotate(float fi, float cx, float cy) {
		float new_x = (float) (cx + (x1 - cx) * Math.cos(fi) - (y1 - cy) * Math.sin(fi));
		float new_y = (float) (cy + (x1 - cx) * Math.sin(fi) + (y1 - cy) * Math.cos(fi));
		x1 = new_x;
		y1 = new_y;
		
		new_x = (float) (cx + (x2 - cx) * Math.cos(fi) - (y2 - cy) * Math.sin(fi));
		new_y = (float) (cy + (x2 - cx) * Math.sin(fi) + (y2 - cy) * Math.cos(fi));
		x2 = new_x;
		y2 = new_y;
		
		stretch_line_x = x1 + GameData.stretch_line_length * (x2 - x1) / length;
    	stretch_line_y = y1 + GameData.stretch_line_length * (y2 - y1) / length;
    	
		for (Stick st : child_sticks) { 
			st.rotate(fi, cx, cy);
		}
	}
	
	public void rotateAroundJoint1(float x, float y) {
		if (rotateless_joint == JOINTS.JOINT2)
			return;
		float dl = (float)Math.sqrt((x - x1) * (x - x1) + (y - y1) * (y - y1));
		if (dl == 0)
			return;
    	float cos_theta = ((x - x1) * (x2 - x1) + (y - y1) * (y2 - y1)) / (dl * length);
    	
    	if (cos_theta > 1.0f)
    		cos_theta = 1.0f;
    	else if (cos_theta < -1.0f)
    		cos_theta = -1.0f;
    	
    	float theta = (float)Math.acos(cos_theta);
    	
    	if ((x2 - x1) * (y - y1) - (x - x1) * (y2 - y1) < 0)
    		theta = -theta;
    	rotate(theta, x1, y1);
    	if (Float.isNaN(x1))
    	{
    		x1 = 0;
    		x2 = 0;
    		
    	}
	}
	
	public void rotateAroundJoint2(float new_x2, float new_y2) {
		float dl = (float)Math.sqrt((new_x2 - x2) * (new_x2 - x2) + (new_y2 - y2) * (new_y2 - y2));
		if (dl == 0)
			return;
    	float cos_theta = ((new_x2 - x2) * (x1 - x2) + (new_y2 - y2) * (y1 - y2)) / (dl * length);
    	if (cos_theta > 1.0f)
    		cos_theta = 1.0f;
    	else if (cos_theta < -1.0f)
    		cos_theta = -1.0f;
    	
    	float theta = (float)Math.acos(cos_theta);
    	
    	if ((x1 - x2) * (new_y2 - y2) - (new_x2 - x2) * (y1 - y2) < 0)
    		theta = -theta;
    	rotate(theta, x2, y2);
	}
	
	public void translate(float dx, float dy) {	
		x1 += dx;
		y1 += dy;
		x2 += dx;
		y2 += dy;
		
		stretch_line_x += dx;
    	stretch_line_y += dy;
    	
		for (Stick stick : child_sticks) {
			stick.translate(dx,  dy);
		}
	}
	
	public void scale(float new_x2, float new_y2) {
		if (parent_joint != null || parent_stick != null)
			return;
		
		float temp_length = (float) Math.sqrt((new_x2 - x2) * (new_x2 - x2) + (new_y2 - y2) * (new_y2 - y2));
		if (temp_length <= GameData.min_length)
			return;
		float dl = (float)Math.sqrt((new_x2 - x2) * (new_x2 - x2) + (new_y2 - y2) * (new_y2 - y2));
		if (dl == 0)
			return;
    	float cos_theta = ((new_x2 - x2) * (x1 - x2) + (new_y2 - y2) * (y1 - y2)) / (dl * length);
    	float theta = (float)Math.acos(cos_theta);
    	
    	if ((x1 - x2) * (new_y2 - y2) - (new_x2 - x2) * (y1 - y2) < 0)
    		theta = -theta;
    	rotate(theta, x2, y2);
    	
    	x1 = new_x2;
    	y1 = new_y2;
    	
    	stretch_line_x = x1 + GameData.stretch_line_length * (x2 - x1) / length;
    	stretch_line_y = y1 + GameData.stretch_line_length * (y2 - y1) / length;
    	length = temp_length;
	}
	
	@Override
	public void draw(Canvas canvas) { 
		canvas.drawLine(stretch_line_x, stretch_line_y, x2, y2, m_line_paint);
		canvas.drawLine(x1, y1, stretch_line_x, stretch_line_y, m_stretch_line_paint);
		canvas.drawCircle(x2, y2, GameData.joint_radius_visible, m_joint2_paint);
		//canvas.drawCircle(x1, y1, joint_radius_visible, m_joint1_paint);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		return super.onTouchEvent(event);
	}
	
	
	private StickTouches m_checkTouched(float touch_x, float touch_y) {
		float dx = x1 - touch_x;
		float dy = y1 - touch_y;
		boolean joint1_touched = (dx * dx + dy * dy <= GameData.joint_radius_touchable_square);
				
		dx = x2 - touch_x;
		dy = y2 - touch_y;
		boolean joint2_touched = false;
		
		if (!joint1_touched)
			joint2_touched = (dx * dx + dy * dy <= GameData.joint_radius_touchable_square);
		
		boolean stick_touched = false;
		if (!joint1_touched && !joint2_touched) {
			// let's find out if projection of point (touch_x, touch_y) gets into stick ((x1, y1); (x2, y2))
			float dot_product = (touch_x - x1) * (x2 - x1) + (touch_y - y1) * (y2 - y1);
			float touch_vector_length_squared = (touch_x - x1) * (touch_x - x1) + (touch_y - y1) * (touch_y - y1);
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
	
	public boolean checkTouched(float touch_x, float touch_y) {
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
		return (stick.y2 - y1) * (stick.y2 - y1) + (stick.x2 - x1) * (stick.x2 - x1);
	}
	
	
	public void addConnectedStick(Stick st) {
		if (!child_sticks.contains(st))
			child_sticks.add(st);
	}
	
	
	public void setNotConnected() {
		if (parent_joint != null) {
			parent_joint.removeConnectedStick(this);
			parent_joint = null;
		}
		
		if (parent_stick != null) {
			parent_stick.child_sticks.remove(this);
			parent_stick = null;
		}
		m_stretch_line_paint = GameData.stretch_line_paint;
	}
	
	public void setPosition(float _x1, float _y1, float _x2, float _y2) {
		x1 = _x1;
		y1 = _y1;
		x2 = _x2;
		y2 = _y2;
		length = (float) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
		stretch_line_x = x1 + GameData.stretch_line_length * (x2 - x1) / length;
    	stretch_line_y = y1 + GameData.stretch_line_length * (y2 - y1) / length;
	}
	
	public boolean isHigher(float y) {
		return (y1 < y && y2 < y);
	}

	@Override
	public PrimitiveType GetType() {
		return PrimitiveType.STICK;
	}

	@Override
	public float distTo(DrawingPrimitive primitive) {
		return primitive.getDistToMe(x1, y1);
	}

	@Override
	public void connectTo(DrawingPrimitive primitive) {
		switch (primitive.GetType()) {
		case JOINT: {
			CentralJoint joint = (CentralJoint)primitive;
			float dx = joint.getMyX() - x1;
			float dy = joint.getMyY() - y1;
			if (parent_joint == joint && dx == 0 && dy == 0)
				return;
			translate(dx, dy);
			parent_joint = joint;
			parent_stick = null;
			joint.addConnectedStick(this);
			m_stretch_line_paint = GameData.invisible_paint;
			break;
		}
		case STICK: {
			Stick stick = (Stick)primitive;
			float dx = stick.x2 - x1;
			float dy = stick.y2 - y1;
			if (parent_stick == stick && dy == 0 && dx == 0)
				return;
			translate(dx, dy);
			parent_stick = stick;
			parent_joint = null;
			parent_stick.addConnectedStick(this);
			m_stretch_line_paint = GameData.invisible_paint;
			break;
		}
		default:
			break;
		
		}
		
	}

	@Override
	public float getDistToMe(float x_from, float y_from) {
		return (x2 - x_from) * (x2 - x_from) + (y2 - y_from) * (y2 - y_from);
	}

	@Override
	public void applyMove(float new_x, float new_y, float prev_x, float prev_y) {
		switch (touch_state) {
		case JOINT1:
			scale(new_x, new_y);
			break;
		case JOINT2:
			rotateAroundJoint1(new_x, new_y);
			break;
		case STICK:
			translate(new_x - prev_x, new_y - prev_y);
			break;
		default:
			break;
		}
	}

	@Override
	public boolean checkScaleTouched(float touch_x, float touch_y) {
		StickTouches newTouchState = m_checkTouched(touch_x, touch_y);
		
		if (newTouchState == StickTouches.NONE)
			return false;
		
		return true;
	}
}
