package com.autumncoding.stickman;

import java.util.ArrayList;
import java.util.LinkedList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

public class Circle implements DrawingPrimitive {
	private float x;
	private float y;
	private float x_stretch; // coordinates of point pulling which we can stretch the circle
	private float y_stretch;
	private float x_rotate; // joint on circle
	private float y_rotate;
	private float r;
	private float angle;
	
	/*********************** touch data *********************************************/
	private boolean is_touched;
	
	private Paint m_line_paint;
	private Paint m_joint_paint;
	private Paint m_stretch_line_paint;
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
		x_stretch = x - r;
		y_stretch = y;
		
		x_rotate = x + r;
		y_rotate = y;
		
		angle = 0;
		is_touched = false;
		childPrimitives = new ArrayList<DrawingPrimitive>();
		
		m_line_paint = GameData.line_paint;
		m_joint_paint = GameData.joint_paint;
		m_stretch_line_paint = GameData.stretch_line_paint;
		touch_state = CircleTouches.NONE;
		parentPrimitive = null;
	}
	
	private CircleTouches m_checkTouched(float touch_x, float touch_y) {
		float dx = x_rotate - touch_x;
		float dy = y_rotate - touch_y;
		CircleTouches newTouchState = CircleTouches.NONE;
		
		boolean rotate_point_touched = (dx * dx + dy * dy <= GameData.joint_radius_touchable_square);
		
		boolean stretch_point_touched = false;
		if (!rotate_point_touched) {
			dx = x_stretch - touch_x;
			dy = y_stretch - touch_y;
		    stretch_point_touched = (dx * dx + dy * dy <= GameData.joint_radius_touchable_square);
		}
		
		boolean circle_touched = false;
		if (!rotate_point_touched && !stretch_point_touched) {
			float dist = (x - touch_x) * (x - touch_x) + (y - touch_y) * (y - touch_y);
			float min_dist_in = (r - GameData.circle_touchable_dr) * (r - GameData.circle_touchable_dr);
			float max_dist_in = (r + GameData.circle_touchable_dr) * (r + GameData.circle_touchable_dr);
			circle_touched = (dist >= min_dist_in && dist <= max_dist_in);
		}
				
		if (rotate_point_touched) {
			newTouchState = CircleTouches.JOINT;
		}
		else if (stretch_point_touched) {
			m_stretch_line_paint = GameData.joint_touched_paint;
			newTouchState = CircleTouches.STRETCHER;
		}
		else if (circle_touched) {
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
		case STRETCHER:
			m_stretch_line_paint = GameData.joint_touched_paint;
			break;
		default:
			break;
		}
		return is_touched;
	}

	@Override
	public void draw(Canvas canvas) {
		canvas.drawCircle(x, y, r, m_line_paint);
		canvas.drawPoint(x_rotate, y_rotate, m_joint_paint);
		canvas.drawPoint(x_stretch, y_stretch, m_stretch_line_paint);
	}

	@Override
	public boolean isTouched() {
		return is_touched;
	}

	@Override
	public float distTo(DrawingPrimitive primitive) {
		return primitive.getDistToMe(x_stretch, y_stretch);
	}


	@Override
	public PrimitiveType GetType() {
		return PrimitiveType.CIRCLE;
	}
	
	public void rotate(float fi, float cx, float cy) {
		float new_x = (float) (cx + (x_rotate - cx) * Math.cos(fi) - (y_rotate - cy) * Math.sin(fi));
		float new_y = (float) (cy + (x_rotate - cx) * Math.sin(fi) + (y_rotate - cy) * Math.cos(fi));
		x_rotate = new_x;
		y_rotate = new_y;
		
		new_x = (float) (cx + (x_stretch - cx) * Math.cos(fi) - (y_stretch - cy) * Math.sin(fi));
		new_y = (float) (cy + (x_stretch - cx) * Math.sin(fi) + (y_stretch - cy) * Math.cos(fi));
		x_stretch = new_x;
		y_stretch = new_y;
		
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
		x_stretch += dx;
		y_stretch += dy;
		x_rotate += dx;
		y_rotate += dy;
		
    	
		for (DrawingPrimitive pr : childPrimitives) { 
			pr.translate(dx,  dy);
		}
	}
	
	public void setPosition(float _x, float _y, float _angle) {
		x = _x;
		y = _y;
		x_stretch = x - r;
		y_stretch = y;
		
		x_rotate = x + r;
		y_rotate = y;
		
		angle = _angle;
		rotate(angle, x, y);
	}

	@Override
	public float getDistToMe(float from_x, float from_y) {
		return (x_rotate - from_x) * (x_rotate - from_x) + (y_rotate - from_y) * (y_rotate - from_y);
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
		
		x_stretch = cir.x_stretch;
		y_stretch = cir.y_stretch;
		
		x_rotate = cir.x_rotate;
		y_rotate = cir.y_rotate;
		
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
		m_stretch_line_paint = GameData.stretch_line_paint;
		m_line_paint = GameData.line_paint;
	}
	
	public void stretch(float new_x, float new_y) {
		float new_r = (float)Math.sqrt((x - new_x) * (x - new_x) + (y - new_y) * (y - new_y));
		
		if (new_r > GameData.min_circle_radius && new_r < GameData.max_circle_radius) {
			r = new_r;
			x_stretch = (float) (x - r * Math.cos(angle));
			y_stretch = (float) (y - r * Math.sin(angle));
			
			x_rotate = (float) (x + r * Math.cos(angle));
			y_rotate = (float) (y + r * Math.sin(angle));
		}
	}
	
	public void applyMove(float new_x, float new_y, float prev_x, float prev_y, boolean isScaling) {
		switch (touch_state) {
		case CIRCLE:
			translate(new_x - prev_x, new_y - prev_y);
			break;
		case JOINT: {
			float len1 = (float) Math.sqrt((new_x - x_stretch) * (new_x - x_stretch) + (new_y - y_stretch) * (new_y - y_stretch));
			float len2 = (float) Math.sqrt((x_rotate - x_stretch) * (x_rotate - x_stretch) + (y_rotate - y_stretch) * (y_rotate - y_stretch));
			float cos_theta = (new_x - x_stretch) * (x_rotate - x_stretch) + (new_y - y_stretch) * (y_rotate - y_stretch) / (len1 * len2);
			
	    	if (cos_theta > 1.0f)
	    		cos_theta = 1.0f;
	    	else if (cos_theta < -1.0f)
	    		cos_theta = -1.0f;
	    	
			float theta = (float) Math.acos(cos_theta); // NB: using acos is not very cool just so: need to be sue fi <= 180
			rotate(theta, x_stretch, y_stretch);
			break;
		}
		case NONE:
			break;
		case STRETCHER:
			stretch(new_x, new_y);
			break;
		default:
			break;
		
		
		}
	}

	@Override
	public boolean tryConnection(LinkedList<DrawingPrimitive> neighbours) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ArrayList<Joint> getMyJoints() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean connectToParent(DrawingPrimitive primitive) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void ConnectToChild(DrawingPrimitive primitive, Joint myJoint,
			Joint primitiveJoint) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disconnectFromChild(DrawingPrimitive p) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disconnectFromParent() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setSubtreeVisited(boolean visited) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isVisited() {
		// TODO Auto-generated method stub
		return false;
	}
	
}
	
