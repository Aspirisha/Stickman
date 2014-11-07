package com.autumncoding.stickman;

import java.util.ArrayList;
import java.util.LinkedList;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;

public class GameData {
	public static LinkedList<DrawingPrimitive> drawing_queue;
	public static LinkedList<DrawingPrimitive> prevDrawingQueue = null;
	public static Context context = null;
    private boolean is_inited = false;
    private static GameData instance = null;
    private static Object locker;// object for touch thread and ui thread synchronization
    private long prevDrawingTime = System.currentTimeMillis();
    
    public static final long FPS = 30;
    
    //lengths
    public static final float min_dist_to_connect_square = 100;
    
    //menu lines
    public float bottom_menu_y = 0;
    public float bottom_menu_x1 = 0;
    public float bottom_menu_x2 = 0;
    public float top_menu_y = 0;
    public float top_menu_x1 = 0;
    public float top_menu_x2 = 0;
    
    public static final int maxPrimitivesNumber = 15; 
    
    
    // visible sizes
    public static final float joint_radius_visible = 7;
    public static final float min_stick_length = 20;
    
    // touchable sizes
    public static final float circle_touchable_dr; // +- so make it 0.5 of needed delta
    public static final float joint_radius_touchable = 10;
    public static final float joint_radius_touchable_square = joint_radius_touchable * joint_radius_touchable;
    public static final float max_circle_radius = 200;
    public static final float min_circle_radius = 10;
	public static final float stick_distance_touchable = 10;
	public static final float stick_distance_touchable_square = stick_distance_touchable * stick_distance_touchable;
    
    /*********** PAINTS ***************/
    // constant paints:
    public static Paint menu_line_paint;
    public static final Paint menuBitmapPaint;
    public static Paint debug_paint;
    public static Paint prev_frame_line_paint;
    public static Paint prev_frame_joint_paint;
    
    // untouched paints:
	public static final Paint line_paint;  // normal paint for stick line
	public static Paint joint_paint;
	public static Paint invisible_paint;
	// touched paints:
	public static final Paint drop_line_paint;  // paint used to show that element is going to be dropped to the bin
	public static final Paint drop_joint_paint;
	public static Paint joint_touched_paint;
	public static Paint line_touched_paint;
	
	
	// menu info
	public static final int numberOfMenuIcons = 6;
	public static final float menuIconsTop = 4f;
	public static int topMenuHeight = 0;
	
	private static float menuBottom = 0;
	public static ArrayList<PointF> drawnPoints; 
	
    static {
    	instance = new GameData();
    	
    	// touchable sizes init
    	circle_touchable_dr = 5;
    	
    	// paints init
    	line_paint = new Paint();
		line_paint.setColor(Color.BLACK);
		line_paint.setAntiAlias(true);
		line_paint.setDither(true);
		line_paint.setColor(Color.BLACK);
		line_paint.setStrokeWidth(7f);
		line_paint.setStyle(Paint.Style.STROKE);
		line_paint.setStrokeJoin(Paint.Join.ROUND);
		line_paint.setStrokeCap(Paint.Cap.ROUND);
		
		prev_frame_line_paint = new Paint(line_paint);
		prev_frame_line_paint.setColor(Color.argb(220, 200, 200, 200));
		
		debug_paint = new Paint();
    	debug_paint.setColor(Color.BLACK);
    	debug_paint.setAntiAlias(true);
    	debug_paint.setDither(true);
    	debug_paint.setStrokeWidth(2f);
    	debug_paint.setStyle(Paint.Style.STROKE);
    	debug_paint.setStrokeJoin(Paint.Join.ROUND);
    	debug_paint.setStrokeCap(Paint.Cap.ROUND);
    	debug_paint.setTextSize(16);
		
    	menu_line_paint = new Paint();
    	menu_line_paint.setColor(Color.BLACK);
    	menu_line_paint.setStrokeJoin(Paint.Join.ROUND);
    	menu_line_paint.setStrokeCap(Paint.Cap.ROUND);
    	menu_line_paint.setAntiAlias(true);
    	menu_line_paint.setDither(true);
    	menu_line_paint.setStrokeWidth(3f);
		
		line_touched_paint = new Paint(line_paint);
		line_touched_paint.setColor(Color.GREEN);
		
		joint_paint = new Paint(line_paint);
		joint_paint.setColor(Color.argb(220, 235, 216, 16));
		joint_paint.setShadowLayer(2, 0, 0, 0x6A8F00);
		joint_paint.setStyle(Paint.Style.FILL);
		
		joint_touched_paint = new Paint(joint_paint);
		joint_touched_paint.setColor(Color.argb(220, 16, 216, 235));
		
		prev_frame_joint_paint = new Paint(joint_paint);
		prev_frame_joint_paint.setColor(Color.argb(220, 200, 200, 200));
		
		drop_line_paint = new Paint(line_paint);
		drop_line_paint.setColor(Color.RED);
		
		drop_joint_paint = new Paint(joint_paint);
		drop_line_paint.setColor(Color.RED);
		
		invisible_paint = new Paint(joint_paint);
		invisible_paint.setColor(Color.TRANSPARENT);
		
		drawnPoints = new ArrayList<PointF>();
		
    	menuBitmapPaint = new Paint();
    }
    
    private GameData() {
    	drawing_queue = Animation.getInstance().getFrame(0);
    	locker = new Object();
    }
    
    public static GameData getInstance() {
    	return instance;
    }
    
    public static Object getLocker() {
    	return locker;
    }
    
    public LinkedList<DrawingPrimitive> getDrawingQueue() {
    	return drawing_queue;
    }
    
    public synchronized long getPrevDrawingTime() {
    	return prevDrawingTime;
    }
    
    public synchronized void writeDrawingTime() {
    	prevDrawingTime = System.currentTimeMillis();
    }
    
    public synchronized long timePassedSinceDrawing() {
    	return System.currentTimeMillis() - prevDrawingTime;
    }
    
    public synchronized void setMetrics() {
    	float dy = 40; /// HARDCODE
    	float dx = 20; /// HARDCODE
    	
    	bottom_menu_y = MainActivity.layout_height - dy;
    	bottom_menu_x2 = MainActivity.layout_width - dx;
    	bottom_menu_x1 = dx;
    	
    	top_menu_y = dy;
    	top_menu_x2 = MainActivity.layout_width - dx;
    	top_menu_x1 = dx;
    }

	public static float getMenuBottom() {
		return menuBottom;
	}

	public static void setMenuBottom(float menuBottom) {
		GameData.menuBottom = menuBottom;
	}
	
	public void setDrawingQueue(LinkedList<DrawingPrimitive> q) {
		drawing_queue = q;
	}
}
