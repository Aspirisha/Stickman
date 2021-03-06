package com.autumncoding.stickman;

import java.io.Serializable;

public class Vector2DF implements Serializable {
	private static final long serialVersionUID = 3955268686748801150L;
	public float x;
	public float y;
		
	Vector2DF() {
		x = 0;
		y = 0;
	}
	
	Vector2DF(float _x, float _y) {
		x = _x;
		y = _y;
	}
	
	Vector2DF(Vector2DF p) {
		x = p.x;
		y = p.y;
	}
	
	public void scale(float cx, float cy, float rate) {
		x = cx + rate * (x - cx);
		y = cy + rate * (y - cy);
	}
	
	public float getLength() {
		return (float) Math.sqrt(x * x + y * y);
	}
	
	public float getSquaredLength() {
		return mul(this, this);
	}
	
	static Vector2DF sub(Vector2DF v1, Vector2DF v2) {
		Vector2DF result = new Vector2DF();
		result.x = v1.x - v2.x;
		result.y = v1.y - v2.y;
		return result;
	}
	
	static Vector2DF add(Vector2DF v1, Vector2DF v2) {
		Vector2DF result = new Vector2DF();
		result.x = v1.x + v2.x;
		result.y = v1.y + v2.y;
		return result;
	}
	
	static float mul(Vector2DF v1, Vector2DF v2) {
		float result = v1.x * v2.x + v1.y * v2.y;
		return result;
	}
	
	static Vector2DF mul(float c, Vector2DF v) {
		Vector2DF result = new Vector2DF();
		v.x *= c;
		v.y *= c;
		return result;
	}
	
	static Vector2DF mul(Vector2DF v, float c) {
		Vector2DF result = new Vector2DF();
		v.x *= c;
		v.y *= c;
		return result;
	}
	
	static Vector2DF ave(Vector2DF v1, Vector2DF v2) {
		Vector2DF result = new Vector2DF();
		result.x = (v1.x + v2.x) / 2;
		result.y = (v1.y + v2.y) / 2;
		
		return result;
	}
	
	static float distSquare(Vector2DF v1, Vector2DF v2) {
		return ((v1.x - v2.x) * (v1.x - v2.x) + (v1.y - v2.y) * (v1.y - v2.y));
	}
	
	static float distSquare(Vector2DF v1, float x, float y) {
		return (v1.x - x) * (v1.x - x) + (v1.y - y) * (v1.y - y);
	}
	
	static float distSquare(float x, float y, Vector2DF v2) {
		return (v2.x - x) * (v2.x - x) + (v2.y - y) * (v2.y - y);
	}
	
	static float dist(Vector2DF v1, Vector2DF v2) {
		return (float) Math.sqrt((v1.x - v2.x) * (v1.x - v2.x) + (v1.y - v2.y) * (v1.y - v2.y));
	}
	
	static float dist(Vector2DF v1, float x, float y) {
		return (float) Math.sqrt((v1.x - x) * (v1.x - x) + (v1.y - y) * (v1.y - y));
	}
	
	static float dist(float x, float y, Vector2DF v2) {
		return (float) Math.sqrt((x - v2.x) * (x - v2.x) + (y - v2.y) * (y - v2.y));
	}
	
}
