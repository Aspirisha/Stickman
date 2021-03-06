package com.autumncoding.stickman;

import java.util.ArrayList;
import java.util.LinkedList;

import org.apache.http.message.LineParser;

import android.content.res.Resources;
import android.graphics.BlurMaskFilter;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import com.autamncoding.stickman.R;
import com.autumncoding.stickman.TouchEventThread.TouchState;

public class GameData {
	public static LinkedList<AbstractDrawingPrimitive> drawing_queue;
	public static LinkedList<AbstractDrawingPrimitive> prevDrawingQueue = null;
    private static GameData instance = null;
    private static Object locker;// object for touch thread and ui thread synchronization
    private long prevDrawingTime = System.currentTimeMillis();
    
    public static boolean isDebug = false;
    public static final long FPS = 30; // FPS for real drawing
    public static final int maxAnimationFps = 30;
    public static TouchState touchState = TouchState.DRAWING;
    //lengths
    public static float min_dist_to_connect_square = 100;
    
    //menu lines
    public static float bottom_menu_y = 0;
    public static float bottom_menu_x1 = 0;
    public static float bottom_menu_x2 = 0;
    public static float top_menu_y = 0;
    public static float top_menu_x1 = 0;
    public static float top_menu_x2 = 0;
    public static float top_frames_text = 0;
    public static float left_frames_text = 0;
    
    public static int maxPrimitivesNumber = 15; 
    public static final int absoluteMaxOfObjects = 30; 
    
    public static Resources res;
    
    public static RectF fieldRect = null;
    
    // visible sizes
    public static float joint_radius_visible = 8;
    public static float min_stick_length = 30;
    public static int rectDeltaX = 5;
    public static int rectDeltaY = 5;
    
    // touchable sizes
    public static float circle_touchable_dr; // +- so make it 0.5 of needed delta
    public static float joint_radius_touchable = 10;
    public static float joint_radius_touchable_square = joint_radius_touchable * joint_radius_touchable;
    public static float max_circle_radius = 200;
    public static float min_circle_radius = 15;
	public static float stick_distance_touchable = 10;
	public static float stick_distance_touchable_square = stick_distance_touchable * stick_distance_touchable;
    
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
	public static Paint root_line_paint;
	public static Paint root_joint_paint;
	public static Paint joint_paint_free;
	public static Paint joint_connected_paint;
	public static Paint invisible_paint;
	public static Paint blured_paint;
	public static Paint glowingLinePaint;
	
	// touched paints:
	public static Paint line_drop_paint;  // paint used to show that element is going to be dropped to the bin
	public static Paint line_touched_paint;
	public static Paint joint_drop_paint; 
	public static Paint joint_touched_paint;
	
	public static Paint blended_line_paint;
	public static Paint blended_joint_paint;
	
	// menu info
	public static final int numberOfMenuIcons = 8;
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
	public static MenuIcon menuControl = null;
	public static ArrayList<MenuIcon> menuIcons;
	
	public static MainActivity mainActivity;
	public static int currentFrameIndex = 1; // for GameView, starting from 1
	public static volatile boolean metricsSet = false;
	public static volatile boolean framesChanged = false;
	public static int helpToastDurationPerLetter = 100;
	public static long intervalBetweenSameToasts = 30000L;
	
	// settings info
	public static boolean saveToTemp = true;
	public static String lang = "English"; 
	public static boolean playInLoop = false;
	public static boolean enableInterpolation = true;
	public static boolean showPopupHints = true;
	public static float debugValue = 0; // TODO delete
	public static String animFolder = "/animations";
	
	// line glowing
	public static float glowingFrequency = 0.001f;
	public static int lineGlowingColor1 = 0xAA093D0C;
	public static int lineGlowingColor2 = 0xAA12DD1F;
	
	public static void init(MainActivity m) {
		mainActivity = m;
		res = m.getResources();
		
		instance = new GameData();
    	
    	// touchable sizes init
    	circle_touchable_dr = 5;
    	
    	// paints init
    	line_paint = new Paint();
		line_paint.setColor(res.getColor(R.color.primitive_line));
		line_paint.setAntiAlias(true);
		line_paint.setDither(true);
		line_paint.setStrokeWidth(7f);
		line_paint.setStyle(Paint.Style.STROKE);
		line_paint.setStrokeJoin(Paint.Join.ROUND);
		line_paint.setStrokeCap(Paint.Cap.ROUND);
		
		root_line_paint = new Paint(line_paint);
		root_line_paint.setColor(res.getColor(R.color.root_primitive));
		
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
    	glowingLinePaint = new Paint(line_paint);
    	glowingLinePaint.setStrokeWidth(12f);
    	
		// joints paints
		root_joint_paint = new Paint(line_paint);
		root_joint_paint.setColor(res.getColor(R.color.root_joint));
		root_joint_paint.setShadowLayer(2, 0, 0, 0x6A8F00);
		root_joint_paint.setStyle(Paint.Style.FILL);

		blended_joint_paint = new Paint(root_joint_paint);
		
		joint_paint_free = new Paint(root_joint_paint);
		joint_paint_free.setColor(res.getColor(R.color.child_joint));
		
		blured_paint = new Paint(joint_paint_free);
		blured_paint.setColor(Color.argb(235, 74, 138, 255));
		blured_paint.setStrokeWidth(30f);
		blured_paint.setMaskFilter(new BlurMaskFilter(15, BlurMaskFilter.Blur.NORMAL)); 
		
		line_touched_paint = new Paint(line_paint);
		line_touched_paint.setColor(res.getColor(R.color.touched_element));
		
		joint_touched_paint = new Paint(joint_paint_free);
		joint_touched_paint.setColor(res.getColor(R.color.touched_element));
		
		joint_connected_paint = new Paint(joint_paint_free);
		joint_connected_paint.setColor(res.getColor(R.color.connected_joint));
		
		line_prev_frame_paint = new Paint(line_paint);
		line_prev_frame_paint.setColor(res.getColor(R.color.prev_frame_element));
		joint_prev_frame_paint = new Paint(root_joint_paint);
		joint_prev_frame_paint.setColor(res.getColor(R.color.prev_frame_element));
		
		line_drop_paint = new Paint(line_paint);
		line_drop_paint.setColor(res.getColor(R.color.drop_color));
		joint_drop_paint = new Paint(joint_paint_free);
		joint_drop_paint.setColor(res.getColor(R.color.drop_color));
		
		drawnPoints = new ArrayList<PointF>();
		
    	menuBitmapPaint = new Paint();
    	
    	invisible_paint = new Paint();
    	invisible_paint.setColor(Color.argb(0, 0, 0, 0));
    	lineGlowingColor1 = res.getColor(R.color.touched_element);
    	lineGlowingColor2 = res.getColor(R.color.line_glowing_color_2);
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
    	float dx = 20; /// HARDCODE
    	int Yoffset = 20;
    	
    	Drawable d = res.getDrawable(R.drawable.menu_back);
    	int h = d.getIntrinsicHeight();
    	
    	top_menu_y = h; 
    	bottom_menu_y = MainActivity.layout_height - top_menu_y;
    	bottom_menu_x2 = MainActivity.layout_width - dx;
    	bottom_menu_x1 = dx;
    	
    	top_menu_x2 = MainActivity.layout_width - dx;
    	top_menu_x1 = dx;
    	
    	rectDeltaX = (int) res.getDimension(R.dimen.field_delta_x);
    	rectDeltaY = (int) res.getDimension(R.dimen.field_delta_y);
    	fieldRect = new RectF(rectDeltaX, top_menu_y + rectDeltaY + Yoffset, MainActivity.layout_width - rectDeltaX, MainActivity.layout_height - rectDeltaY);
		Resources res = mainActivity.getResources();
		
		textPaint.setTextSize(res.getDimension(R.dimen.gane_view_frames_text_font_size));
		textPaint.setColor(res.getColor(R.color.label_color));
		topMenuHeight = res.getDimensionPixelSize(R.dimen.game_view_menu_height);
		left_frames_text = res.getDimensionPixelSize(R.dimen.frames_text_position_left_padding);
		top_frames_text = topMenuHeight + res.getDimensionPixelSize(R.dimen.frames_text_position_top_padding);
		
		joint_radius_visible = res.getDimension(R.dimen.visible_joint_r);
		joint_radius_touchable = res.getDimension(R.dimen.touchable_joint_r);
	    min_stick_length = res.getDimension(R.dimen.min_stick_length);
	    
	    // touchable sizes
	    circle_touchable_dr = res.getDimension(R.dimen.circle_dist_touchable); // +- so make it 0.5 of needed delta
	    joint_radius_touchable_square = joint_radius_touchable * joint_radius_touchable;
	    max_circle_radius = res.getDimension(R.dimen.max_circle_radius);
	    min_circle_radius = res.getDimension(R.dimen.min_circle_radius);
		stick_distance_touchable = res.getDimension(R.dimen.stick_dist_touchable);
		stick_distance_touchable_square = stick_distance_touchable * stick_distance_touchable;
		
		line_paint.setStrokeWidth(res.getDimension(R.dimen.line_width));
		root_line_paint.setStrokeWidth(res.getDimension(R.dimen.line_width));
		line_drop_paint.setStrokeWidth(res.getDimension(R.dimen.line_width));
		line_prev_frame_paint.setStrokeWidth(res.getDimension(R.dimen.line_width));
		line_touched_paint.setStrokeWidth(res.getDimension(R.dimen.line_width));
		blended_line_paint.setStrokeWidth(res.getDimension(R.dimen.line_width));
		glowingLinePaint.setStrokeWidth(res.getDimension(R.dimen.glowing_line_width));
		min_dist_to_connect_square = res.getDimension(R.dimen.min_dist_to_connect_square);
		
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
