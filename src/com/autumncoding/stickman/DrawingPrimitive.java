package com.autumncoding.stickman;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedList;

import android.graphics.Canvas;

public interface DrawingPrimitive  {
	enum VisitColor {
		WHITE, 
		GRAY,
		BLACK
	}
	
	public boolean checkTouch(float touch_x, float touch_y);
	public void draw(Canvas canvas);
	public boolean isTouched();
	public float distTo(DrawingPrimitive primitive);
	public void connectTo(DrawingPrimitive primitive);
	public void applyMove(float new_x, float new_y, float prev_x, float prev_y, boolean isScaling);
	public void rotate(float fi, float cx, float cy);
	public void translate(float dx, float dy);
	public float getDistToMe(float from_x, float from_y);
	public void setUntouched();
	public void setNotConnected();
	public void copy(DrawingPrimitive p);
	public void removeConnection(DrawingPrimitive p);
	public void addConnection(DrawingPrimitive p);
	public boolean tryConnection(LinkedList<DrawingPrimitive> neighbours);
	public float getDepth();
	public void setDepth(float d);
	public boolean isLeafInPrimitiveTree();
	public void setVisitColor(VisitColor color);
	public VisitColor getVisitColor();
	public ArrayList<DrawingPrimitive> getConnectedPrimitives();
	public void addChild(DrawingPrimitive ch);
	public void addParent(DrawingPrimitive ch);
	enum PrimitiveType {
		STICK,
		CIRCLE,
		JOINT
	};
	PrimitiveType GetType();
}
