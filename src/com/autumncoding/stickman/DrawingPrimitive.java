package com.autumncoding.stickman;

import android.graphics.Canvas;

public interface DrawingPrimitive  {
	public boolean checkTouched(float touch_x, float touch_y);
	public boolean checkScaleTouched(float touch_x, float touch_y);
	public void draw(Canvas canvas);
	public boolean isTouched();
	public float distTo(DrawingPrimitive primitive);
	public void connectTo(DrawingPrimitive primitive);
	public void applyMove(float new_x, float new_y, float prev_x, float prev_y);
	public void rotate(float fi, float cx, float cy);
	public void translate(float dx, float dy);
	public float getDistToMe(float from_x, float from_y);
	public void setUntouched();
	public void setNotConnected();
	public void copy(DrawingPrimitive p);
	
	enum PrimitiveType {
		STICK,
		CIRCLE,
		JOINT
	};
	
	PrimitiveType GetType();
}
