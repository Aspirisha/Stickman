package com.autumncoding.stickman;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;

public class MenuIcon {
	enum IconState {
		ACTIVE,
		TOUCHED,
		UNAVAILABLE
	}
	private Bitmap m_activeBitmap = null;
	private Bitmap m_touchedBitmap = null;
	private Bitmap m_unavailableBitmap = null;
	private Bitmap m_currentBitmap = null;
	private boolean m_isTouched = false;
	private RectF m_rect = null; 
	private IconState m_state = IconState.ACTIVE;
	
	public MenuIcon() {
		
	}
	
	public void setActiveBitmap(Bitmap bmp) {
		m_activeBitmap = bmp;
		m_currentBitmap = m_activeBitmap;
	}
	
	public void setTouchedBitmap(Bitmap bmp) {
		m_touchedBitmap = bmp;
	}
	
	public void setUnavailableBitmap(Bitmap bmp) {
		m_unavailableBitmap = bmp;
	}
	
	public void setPosition(float x, float y) {
		if (m_activeBitmap == null)
			return;
		m_rect = new RectF(x, y, x + m_activeBitmap.getWidth(), y + m_activeBitmap.getHeight());
	}
	
	public boolean checkTouchedAndSet(float x, float y) {
		if (m_state == IconState.UNAVAILABLE) 
			return false;
		
		if (m_rect.contains(x, y)) {
			m_state = IconState.TOUCHED;
			m_currentBitmap = m_touchedBitmap;
			m_isTouched = true;
			return true;
		}
		m_state = IconState.ACTIVE;
		m_currentBitmap = m_activeBitmap;
		return false;
	}
	
	public boolean checkTouchedNoSet(float x, float y) {
		if (m_state == IconState.UNAVAILABLE) 
			return false;
		
		if (m_rect.contains(x, y)) {
			m_state = IconState.TOUCHED;
			m_currentBitmap = m_touchedBitmap;
			m_isTouched = true;
			return true;
		}
		return false;
	}
	
	public void setActive() {
		m_state = IconState.ACTIVE;
		m_currentBitmap = m_activeBitmap;
	}
	
	public void setUnavailable() {
		m_state = IconState.UNAVAILABLE;
		m_currentBitmap = m_unavailableBitmap;
	}
	
	public void setUntouched() {
		if (m_state == IconState.UNAVAILABLE)
			return;
		m_state = IconState.ACTIVE;
		m_currentBitmap = m_activeBitmap;
	}
	
	public void setTouched() {
		m_state = IconState.TOUCHED;
		m_currentBitmap = m_touchedBitmap;
	}
	
	public void setAvailable() {
		if (m_state == IconState.UNAVAILABLE) {
			m_state = IconState.ACTIVE;
			m_currentBitmap = m_activeBitmap;
		}
	}
	
	public void Draw(Canvas canvas) {
		canvas.drawBitmap(m_currentBitmap, m_rect.left, m_rect.top, GameData.menuBitmapPaint);
	}
}
