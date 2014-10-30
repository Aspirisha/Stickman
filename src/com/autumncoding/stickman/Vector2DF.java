package com.autumncoding.stickman;

public class Vector2DF {
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
	
	static float distSquare(Vector2DF v1, Vector2DF v2) {
		return ((v1.x - v2.x) * (v1.x - v2.x) + (v1.y - v2.y) * (v1.y - v2.y));
	}
	
	static float dist(Vector2DF v1, Vector2DF v2) {
		return (float) Math.sqrt((v1.x - v2.x) * (v1.x - v2.x) + (v1.y - v2.y) * (v1.y - v2.y));
	}
	
}
