package com.autumncoding.stickman;

import java.util.ArrayList;
import java.util.LinkedList;

import com.autamncoding.stickman.R;
import com.autumncoding.stickman.TouchEventThread.TouchState;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;

public class GameData {
	public static LinkedList<AbstractDrawingPrimitive> drawing_queue;
	public static LinkedList<AbstractDrawingPrimitive> prevDrawingQueue = null;
    private static GameData instance = null;
    private static Object locker;// object for touch thread and ui thread synchronization
    private long prevDrawingTime = System.currentTimeMillis();
    
    public static final long FPS = 30;
    public static TouchState touchState = TouchState.DRAWING;
    //lengths
    public static final float min_dist_to_connect_square = 100;
    
    //menu lines
    public static float bottom_menu_y = 0;
    public static float bottom_menu_x1 = 0;
    public static float bottom_menu_x2 = 0;
    public static float top_menu_y = 0;
    public static float top_menu_x1 = 0;
    public static float top_menu_x2 = 0;
    public static float top_frames_text = 0;
    public static float left_frames_text = 0;
    
    public static final int maxPrimitivesNumber = 15; 
    
    public static Resources res;
    
    // visible sizes
    public static final float joint_radius_visible = 7;
    public static final float min_stick_length = 20;
    
    // touchable sizes
    public static float circle_touchable_dr; // +- so make it 0.5 of needed delta
    public static final float joint_radius_touchable = 10;
    public static final float joint_radius_touchable_square = joint_radius_touchable * joint_radius_touchable;
    public static final float max_circle_radius = 200;
    public static final float min_circle_radius = 10;
	public static final float stick_distance_touchable = 10;
	public static final float stick_distance_touchable_square = stick_distance_touchable * stick_distance_touchable;
    
    /*********** PAINTS ***************/
    // constant paints:
    public static Paint menu_line_paint;
    public static Paint menuBitmapPaint;
    public static Paint debug_paint;
    public static Paint textPaint;
    public static Paint line_prev_frame_paint;
    public static Paint joint_prev_frame_paint;
    
    // untouched paints:
	public static Paint line_paint;  // normal paint for stick line
	public static Paint root_joint_paint;
	public static Paint child_joint_paint;
	// touched paints:
	public static Paint line_drop_paint;  // paint used to show that element is going to be dropped to the bin
	public static Paint line_touched_paint;
	public static Paint joint_drop_paint; 
	public static Paint joint_touched_paint;
	
	public static Paint blended_line_paint;
	public static Paint blended_joint_paint;
	
	// menu info
	public static final int numberOfMenuIcons = 7;
	public static final float menuIconsTop = 4f;
	public static int topMenuHeight = 0;
	
	private static float menuBottom = 0;
	public static ArrayList<PointF> drawnPoints; 
	
	public static MenuIcon menuPencil = null;
	public static MenuIcon menuDrag = null;
	public static MenuIcon menuPrev = null;
	public static MenuIcon menuNext = null;
	public static MenuIcon menuNew = null;
	public static MenuIcon menuPlay = null;
	public static MenuIcon menuBin = null;
	public static ArrayList<MenuIcon> menuIcons;
	
	public static MainActivity mainActivity;
	public static int currentFrameIndex = 1; // for GameView, starting from 1
	public static volatile boolean metricsSet = false;
	public static volatile boolean framesChanged = false;
	
	public static void init(MainActivity m) {
		mainActivity = m;
		res = m.getResources();
		
		instance = new GameData();
    	
    	// touchable sizes init
    	circle_touchable_dr = 5;
    	
    	// paints init
    	line_paint = new Paint();
		line_paint.setColor(Color.BLACK);
		line_paint.setAntiAlias(true);
		line_paint.setDither(true);
		line_paint.setStrokeWidth(7f);
		line_paint.setStyle(Paint.Style.STROKE);
		line_paint.setStrokeJoin(Paint.Join.ROUND);
		line_paint.setStrokeCap(Paint.Cap.ROUND);
		
		textPaint = new Paint();
		textPaint.setColor(Color.GRAY);
		textPaint.setAntiAlias(true);
		textPaint.setStrokeWidth(1f);
		textPaint.setStyle(Paint.Style.STROKE);
		
		debug_paint = new Paint();
    	debug_paint.setColor(res.getColor(R.color.primitive_line));
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
		
    	blended_line_paint = new Paint(line_paint);
		
		// joints paints
		root_joint_paint = new Paint(line_paint);
		root_joint_paint.setColor(res.getColor(R.color.root_joint));
		root_joint_paint.setShadowLayer(2, 0, 0, 0x6A8F00);
		root_joint_paint.setStyle(Paint.Style.FILL);

		blended_joint_paint = new Paint(root_joint_paint);
		
		child_joint_paint = new Paint(root_joint_paint);
		child_joint_paint.setColor(res.getColor(R.color.child_joint));
		
		line_touched_paint = new Paint(line_paint);
		line_touched_paint.setColor(res.getColor(R.color.touched_element));
		joint_touched_paint = new Paint(child_joint_paint);
		joint_touched_paint.setColor(res.getColor(R.color.touched_element));
		
		
		line_prev_frame_paint = new Paint(line_paint);
		line_prev_frame_paint.setColor(res.getColor(R.color.prev_frame_element));
		joint_prev_frame_paint = new Paint(root_joint_paint);
		joint_prev_frame_paint.setColor(res.getColor(R.color.prev_frame_element));
		
		line_drop_paint = new Paint(line_paint);
		line_drop_paint.setColor(res.getColor(R.color.drop_color));
		joint_drop_paint = new Paint(child_joint_paint);
		joint_drop_paint.setColor(res.getColor(R.color.drop_color));
		
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
    
    public LinkedList<AbstractDrawingPrimitive> getDrawingQueue() {
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
    
    public static void setMetrics() {
    	float dy = 40; /// HARDCODE
    	float dx = 20; /// HARDCODE
    	
    	bottom_menu_y = MainActivity.layout_height - dy;
    	bottom_menu_x2 = MainActivity.layout_width - dx;
    	bottom_menu_x1 = dx;
    	
    	top_menu_y = dy;
    	top_menu_x2 = MainActivity.layout_width - dx;
    	top_menu_x1 = dx;
    	
		Resources res = mainActivity.getResources();
		
		textPaint.setTextSize(res.getDimension(R.dimen.gane_view_frames_text_font_size));
		textPaint.setColor(res.getColor(R.color.label_color));
		topMenuHeight = res.getDimensionPixelSize(R.dimen.game_view_menu_height);
		left_frames_text = res.getDimensionPixelSize(R.dimen.frames_text_position_left_padding);
		top_frames_text = topMenuHeight + res.getDimensionPixelSize(R.dimen.frames_text_position_top_padding);
		metricsSet = true;
    }

	public static float getMenuBottom() {
		return menuBottom;
	}

	public static void setMenuBottom(float menuBottom) {
		GameData.menuBottom = menuBottom;
	}
	
	public void setDrawingQueue(LinkedList<AbstractDrawingPrimitive> q) {
		drawing_queue = q;
	}
	
	public static int mixTwoColors(int color1, int color2, float amount)
	{
	    final byte ALPHA_CHANNEL = 24;
	    final byte RED_CHANNEL   = 16;
	    final byte GREEN_CHANNEL =  8;
	    final byte BLUE_CHANNEL  =  0;

	    final float inverseAmount = 1.0f - amount;

	    int a = ((int)(((float)(color1 >> ALPHA_CHANNEL & 0xff )*amount) +
	                   ((float)(color2 >> ALPHA_CHANNEL & 0xff )*inverseAmount))) & 0xff;
	    int r = ((int)(((float)(color1 >> RED_CHANNEL & 0xff )*amount) +
	                   ((float)(color2 >> RED_CHANNEL & 0xff )*inverseAmount))) & 0xff;
	    int g = ((int)(((float)(color1 >> GREEN_CHANNEL & 0xff )*amount) +
	                   ((float)(color2 >> GREEN_CHANNEL & 0xff )*inverseAmount))) & 0xff;
	    int b = ((int)(((float)(color1 & 0xff )*amount) +
	                   ((float)(color2 & 0xff )*inverseAmount))) & 0xff;

	    int newColor = a << ALPHA_CHANNEL | r << RED_CHANNEL | g << GREEN_CHANNEL | b << BLUE_CHANNEL;
	    
	    blended_line_paint.setColor(newColor);
	    blended_joint_paint.setColor(newColor);
	    
	    return newColor;
	}
}
