package com.autumncoding.stickman;

import android.graphics.Canvas;

public interface DrawingPrimitive  {
	public boolean checkTouched(float touch_x, float touch_y);
	public void draw(Canvas canvas);
	public boolean isTouched();
	public float distTo(DrawingPrimitive primitive);
	public void connectTo(DrawingPrimitive primitive);
	public void rotate(float fi, float cx, float cy);
	public void translate(float dx, float dy);
	public float getDistToMe(float from_x, float from_y);
	public void setUntouched();
	
	enum PrimitiveType {
		STICK,
		CIRCLE,
		JOINT
	};
	
	PrimitiveType GetType();
}
