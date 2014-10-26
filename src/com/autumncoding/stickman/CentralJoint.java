package com.autumncoding.stickman;

import java.util.ArrayList;

import com.autumncoding.stickman.Stick.StickTouches;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class CentralJoint extends View implements DrawingPrimitive {
	private static Paint joint_paint;
	private static Paint joint_touched_paint;
	private static float joint_radius_touchable;
	private static float joint_radius_touchable_square;
	private static float joint_radius_visible;
	
	private Paint m_paint; 
	private ArrayList<Stick> connected_sticks;
	private boolean is_touched;
	private float x;
	private float y;
	static {
		joint_radius_touchable = 10;
		joint_radius_visible = 5;
		joint_radius_touchable_square = joint_radius_touchable * joint_radius_touchable;
		
		joint_paint = new Paint();
		joint_paint.setAntiAlias(true);
		joint_paint.setDither(true);
		joint_paint.setColor(Color.GREEN);
		joint_paint.setStrokeWidth(7f);
		joint_paint.setStyle(Paint.Style.STROKE);
		joint_paint.setStrokeJoin(Paint.Join.ROUND);
		joint_paint.setStrokeCap(Paint.Cap.ROUND);
		
		joint_touched_paint = new Paint(joint_paint);
		joint_touched_paint.setColor(Color.RED);
	}
	
	public CentralJoint(Context context) {
		super(context);
		m_paint = joint_paint;
		connected_sticks = new ArrayList<Stick>();
		is_touched = false;
		x = 0;
		y = 0;
	}

	public void translate(float dx, float dy) {	
		x += dx;
		y += dy;
		
		for (int i = 0; i < connected_sticks.size(); i++) {
			connected_sticks.get(i).translate(dx, dy);
		}
	}
	
	@Override
	public void draw(Canvas canvas) { 
		canvas.drawCircle(x, y, joint_radius_visible, m_paint);
	}
	
	public boolean checkTouched(float touch_x, float touch_y) {
		float dx = x - touch_x;
		float dy = y - touch_y;
		is_touched = (dx * dx + dy * dy <= joint_radius_touchable_square);
		
		if (is_touched)
			m_paint = joint_touched_paint;
		return is_touched;
	}
	
	public boolean isTouched() {
		return is_touched;
	}
	
	public void setUntouched() {
		if (!is_touched)
			return;
		
		is_touched = false;
		m_paint = joint_paint;
	}
	
	public void setPosition(float new_x, float new_y) {
		x = new_x;
		y = new_y;
	}
	
	float getMyY() {
		return y;
	}
	
	float getMyX() {
		return x;
	}
	
	public void addConnectedStick(Stick st) {
		if (!connected_sticks.contains(st))
			connected_sticks.add(st);
	}
	
	public void removeConnectedStick(Stick st) {
		if (connected_sticks.contains(st))
			connected_sticks.remove(st);
	}

	@Override
	public PrimitiveType GetType() {
		return PrimitiveType.JOINT;
	}

	@Override
	public float distTo(DrawingPrimitive primitive) {
		float distance = 0;
		switch (primitive.GetType()) {
		case JOINT: {
			CentralJoint joint = (CentralJoint)primitive;
			distance = (joint.x - x) * (joint.x - x) + (joint.y - y) * (joint.y - y);
			break;
		}
		case STICK:
			break;
		default:
			break;
		
		}
		return 0;
	}

	@Override
	public void connectTo(DrawingPrimitive primitive) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rotate(float fi, float cx, float cy) {
		return;
	}

	@Override
	public float getDistToMe(float x_from, float y_from) {
		return (x - x_from) * (x - x_from) + (y - y_from) * (y - y_from);
	}

	@Override
	public void applyMove(float new_x, float new_y, float prev_x, float prev_y) {
		translate(new_x - prev_x, new_y - prev_y);
		
	}

	@Override
	public void setNotConnected() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void copy(DrawingPrimitive p) {
		if (p == null)
			return;
		if (p.GetType() != PrimitiveType.JOINT)
			return;
		
		CentralJoint joint = (CentralJoint)p;
		x = joint.x;
		y = joint.y;
		is_touched = joint.is_touched;
		m_paint = joint.m_paint;
	}

	@Override
	public boolean checkScaleTouched(float touch_x, float touch_y) { // no sense
		return false;
	}
}
