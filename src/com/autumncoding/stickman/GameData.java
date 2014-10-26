package com.autumncoding.stickman;

import java.util.LinkedList;

import android.graphics.Color;
import android.graphics.Paint;

public class GameData {
	private LinkedList<DrawingPrimitive> drawing_queue;
	private CentralJoint menu_central_joint = null;
    private Stick menu_stick = null;
    private Circle menu_circle = null;
    private boolean is_inited = false;
    private static GameData instance = null;
    private Object locker;// object for touch thread and ui thread synchronization
    private long prevDrawingTime = System.currentTimeMillis();
    
    //menu lines
    public float bottom_menu_y = 0;
    public float bottom_menu_x1 = 0;
    public float bottom_menu_x2 = 0;
    public float top_menu_y = 0;
    public float top_menu_x1 = 0;
    public float top_menu_x2 = 0;
    
    
    // touchable sizes
    public static final float circle_touchable_dr; // +- so make it 0.5 of needed delta
    public static final float joint_radius_touchable = 10;
    public static final float joint_radius_touchable_square = joint_radius_touchable * joint_radius_touchable;
    public static final float max_circle_radius = 200;
    public static final float min_circle_radius = 10;
    
    /*********** PAINTS ***************/
    // untouched paints:
	public static Paint line_paint;  // normal paint for stick line
	public static Paint joint_paint;
	public static Paint joint_touched_paint;
	public static Paint line_touched_paint;
	// touched paints:
	public static Paint drop_paint;  // paint used to show that element is going to be dropped to the bin
	public static Paint stretch_line_paint;
	public static Paint invisible_paint;
	
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
		
		line_touched_paint = new Paint(line_paint);
		line_touched_paint.setColor(Color.GREEN);
		
		joint_paint = new Paint(line_paint);
		joint_paint.setColor(Color.argb(220, 235, 216, 16));
		joint_paint.setShadowLayer(2, 0, 0, 0x6A8F00);
		joint_paint.setStyle(Paint.Style.FILL);
		
		joint_touched_paint = new Paint(joint_paint);
		joint_touched_paint.setColor(Color.argb(220, 16, 216, 235));
		
		drop_paint = new Paint(joint_paint);
		drop_paint.setColor(Color.GRAY);
		
		stretch_line_paint = new Paint(joint_paint);
		stretch_line_paint.setColor(Color.RED);
		
		invisible_paint = new Paint(joint_paint);
		invisible_paint.setColor(Color.TRANSPARENT);
    }
    
    private GameData() {
    	drawing_queue = new LinkedList<DrawingPrimitive>();
    	locker = new Object();
    }
    
    public static GameData getInstance() {
    	return instance;
    }
    
    public synchronized int init(GameView playing_table) {
    	if (is_inited)
    		return 0;
    	
    	menu_stick = new Stick(playing_table.getContext());
    	menu_central_joint = new CentralJoint(playing_table.getContext());
    	menu_circle = new Circle(playing_table.getContext());
    	is_inited = true;
    	
    	return 1;
    }
    
    public Object getLocker() {
    	return locker;
    }
    
    public synchronized Stick getMenuStick() {
    	return menu_stick;
    }
    
    public synchronized Circle getMenuCircle() {
    	return menu_circle;
    }
    
    public LinkedList<DrawingPrimitive> getDrawingQueue() {
    	return drawing_queue;
    }
    
    public synchronized CentralJoint getMenuCentralJoint() {
    	return menu_central_joint;
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
}
