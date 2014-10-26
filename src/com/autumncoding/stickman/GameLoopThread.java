package com.autumncoding.stickman;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.util.Log;

public class GameLoopThread extends Thread {
	private GameView playingTable;
	private boolean isRunning = false;
	static final long FPS = 30;
	static final long ticksPS = 1000 / FPS;
	
	public GameLoopThread(GameView view) {
        this.playingTable = view;
	}
	
	public void setRunning(boolean run) {
        isRunning = run;
	}
	
	
	 @SuppressLint("WrongCall")
	@Override
     public void run() {
         long startTime;
         long sleepTime;
         
         while (isRunning) {
        	 Canvas c = null;
        	 startTime = System.currentTimeMillis();
        	 
        	 try {
        		 c = playingTable.getHolder().lockCanvas();
        		 if (c != null) {
	        		 synchronized (playingTable.getHolder()) {
	        			 playingTable.postInvalidate();
	        		 }
        		 }
        	 } catch (Exception e) {
        		Log.i("Exeption in gameThread: ", e.toString());
        	 } finally {
        		 if (c != null) {
        			 playingTable.getHolder().unlockCanvasAndPost(c);
        		 }
        	 }
        	 
        	 sleepTime = ticksPS - (System.currentTimeMillis() - startTime);
        	 try {
        		 if (sleepTime > 0)
        			 sleep(sleepTime);
        		 else
        			 sleep(10);
        	 } catch (Exception e) {
        		 Log.i("Exeption: ", e.toString());
        	 }
         }
	 }
}