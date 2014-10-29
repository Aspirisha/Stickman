package com.autumncoding.stickman;

import java.util.ArrayList;
import java.util.LinkedList;

import com.autumncoding.stickman.Stick.StickTouches;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

public class Circle implements DrawingPrimitive {
	private float x;
	private float y;
	private float x_joint; // joint on circle
	private float y_joint;
	private float r;
	private float angle;
	
	/*********************** touch data *********************************************/
	private boolean is_touched;
	
	private Paint m_line_paint;
	private Paint m_joint_paint;
	private DrawingPrimitive parentPrimitive;
	private ArrayList<DrawingPrimitive> childPrimitives;
	
	
	enum CircleTouches {
		JOINT, 
		STRETCHER,
		CIRCLE,
		NONE
	}
	
	private CircleTouches touch_state;
	
	public Circle(Context context) {
		x = 100;
		y = 100;
		r = 10;
		
		x_joint = x + r;
		y_joint = y;
		
		angle = 0;
		is_touched = false;
		childPrimitives = new ArrayList<DrawingPrimitive>();
		
		m_line_paint = GameData.line_paint;
		m_joint_paint = GameData.joint_paint;
		touch_state = CircleTouches.NONE;
		parentPrimitive = null;
	}
	
	private CircleTouches m_checkTouched(float touch_x, float touch_y) {
		float dx = x_joint - touch_x;
		float dy = y_joint - touch_y;
		CircleTouches newTouchState = CircleTouches.NONE;
		
		boolean joint_point_touched = (dx * dx + dy * dy <= GameData.joint_radius_touchable_square);
		
		boolean circle_touched = false;
		if (!joint_point_touched) {
			float dist = (x - touch_x) * (x - touch_x) + (y - touch_y) * (y - touch_y);
			float min_dist_in = (r - GameData.circle_touchable_dr) * (r - GameData.circle_touchable_dr);
			float max_dist_in = (r + GameData.circle_touchable_dr) * (r + GameData.circle_touchable_dr);
			circle_touched = (dist >= min_dist_in && dist <= max_dist_in);
		}
				
		if (joint_point_touched) {
			newTouchState = CircleTouches.JOINT;
		} else if (circle_touched) {
			newTouchState = CircleTouches.CIRCLE;
		}
		
		return newTouchState;
	}
	
	@Override
	public boolean checkTouch(float touch_x, float touch_y) {
		touch_state = m_checkTouched(touch_x, touch_y);
		
		is_touched = true;
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
			is_touched = false;
			break;
		default:
			break;
		}
		return is_touched;
	}

	@Override
	public void draw(Canvas canvas) {
		canvas.drawCircle(x, y, r, m_line_paint);
		canvas.drawCircle(x_joint, y_joint, GameData.joint_radius_visible, m_joint_paint);
	}

	@Override
	public boolean isTouched() {
		return is_touched;
	}

	@Override
	public float distTo(DrawingPrimitive primitive) {
		return primitive.getDistToMe(x_joint, y_joint);
	}

	@Override
	public void connectTo(DrawingPrimitive primitive) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public PrimitiveType GetType() {
		return PrimitiveType.CIRCLE;
	}
	
	public void rotate(float fi, float cx, float cy) {
		float new_x = (float) (cx + (x_joint - cx) * Math.cos(fi) - (y_joint - cy) * Math.sin(fi));
		float new_y = (float) (cy + (x_joint - cx) * Math.sin(fi) + (y_joint - cy) * Math.cos(fi));
		x_joint = new_x;
		y_joint = new_y;
		
		new_x = (float) (cx + (x - cx) * Math.cos(fi) - (y - cy) * Math.sin(fi));
		new_y = (float) (cy + (x - cx) * Math.sin(fi) + (y - cy) * Math.cos(fi));
		x = new_x;
		y = new_y;
    	
		for (DrawingPrimitive pr : childPrimitives) { 
			pr.rotate(fi, cx, cy);
		}
	}
	
	public void translate(float dx, float dy) {	
		x += dx;
		y += dy;
		x_joint += dx;
		y_joint += dy;
		
    	
		for (DrawingPrimitive pr : childPrimitives) { 
			pr.translate(dx,  dy);
		}
	}
	
	public void setPosition(float _x, float _y, float _angle) {
		x = _x;
		y = _y;
		
		x_joint = x + r;
		y_joint = y;
		
		angle = _angle;
		rotate(angle, x, y);
	}

	@Override
	public float getDistToMe(float from_x, float from_y) {
		return (x_joint - from_x) * (x_joint - from_x) + (y_joint - from_y) * (y_joint - from_y);
	}
	
	public void copy(DrawingPrimitive primitive) {
		if (primitive == null)
			return;
		
		if (primitive.GetType() != PrimitiveType.CIRCLE)
			return;
		Circle cir = (Circle)primitive;
		
		x = cir.x;
		y = cir.y;
		r = cir.r;
		
		x_joint = cir.x_joint;
		y_joint = cir.y_joint;
		
		angle = cir.angle;
		is_touched = cir.is_touched;

		touch_state = cir.touch_state;
	}

	@Override
	public void setUntouched() {
		if (!is_touched)
			return;
		
		is_touched = false;
		touch_state = CircleTouches.NONE;
		m_joint_paint = GameData.joint_paint;
		m_line_paint = GameData.line_paint;
	}
	
	public void stretch(float new_x, float new_y) {
		float new_r = (float)Math.sqrt((x - new_x) * (x - new_x) + (y - new_y) * (y - new_y));
		
		if (new_r > GameData.min_circle_radius && new_r < GameData.max_circle_radius) {
			r = new_r;
			
			x_joint = (float) (x + r * Math.cos(angle));
			y_joint = (float) (y + r * Math.sin(angle));
		}
	}
	
	public void applyMove(float new_x, float new_y, float prev_x, float prev_y, boolean isScaling) {
		switch (touch_state) {
		case CIRCLE:
			translate(new_x - prev_x, new_y - prev_y);
			break;
		case JOINT: {
			if (isScaling) {
				float newRadius = (float) Math.sqrt((x - new_x) * (x - new_x) + (y - new_y) * (y - new_y));
				scale(x, y, newRadius / r);
			} else {
				float len1 = (float) Math.sqrt((new_x - x) * (new_x - x) + (new_y - y) * (new_y - y));
				float len2 = (float) Math.sqrt((prev_x - x) * (prev_x - x) + (prev_y - y) * (prev_y - y));
				float cos_theta = ((new_x - x) * (prev_x - x) + (new_y - y) * (prev_y - y)) / (len1 * len2);
				
		    	if (cos_theta > 1.0f)
		    		cos_theta = 1.0f;
		    	else if (cos_theta < -1.0f)
		    		cos_theta = -1.0f;
		    	
				float theta = (float) Math.acos(cos_theta); // NB: using acos is not very cool just so: need to be sue fi <= 180
				if ((new_x - x) * (prev_y - y) - (prev_x - x) * (new_y - y) > 0)
					theta = -theta;
				rotate(theta, x, y);
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
	public void setNotConnected() {
		// TODO Auto-generated method stub
		
	}
	
	public void scale(float cx, float cy, float ratio) {
		if (parentPrimitive != null)
			return;
		
		float temp_radius = r * ratio;
		if (temp_radius < GameData.min_circle_radius)
		{
			temp_radius = GameData.min_circle_radius;
			ratio = temp_radius / r;
		}
		
		x = cx + ratio * (x - cx);
		y = cy + ratio * (y - cy);
		
		x_joint = cx + ratio * (x_joint - cx);
		y_joint = cy + ratio * (y_joint - cy);
    	r = temp_radius;
	}

	@Override
	public void removeConnection(DrawingPrimitive p) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addConnection(DrawingPrimitive p) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean tryConnection(LinkedList<DrawingPrimitive> neighbours) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public float getDepth() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setDepth(float d) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isLeafInPrimitiveTree() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setVisitColor(VisitColor color) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public VisitColor getVisitColor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<DrawingPrimitive> getConnectedPrimitives() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addChild(DrawingPrimitive ch) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addParent(DrawingPrimitive ch) {
		// TODO Auto-generated method stub
		
	}

	
}
	
