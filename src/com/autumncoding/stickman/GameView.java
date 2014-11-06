package com.autumncoding.stickman;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;

import com.autamncoding.stickman.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;



public class GameView extends SurfaceView {
	private GameData game_data;
	private static final int INVALID_POINTER_ID = -1;
	private int mActivePointerId = INVALID_POINTER_ID;
	private SurfaceHolder holder;
	private GameLoopThread gameLoopThread;
	private TouchEventThread touch_thread;
	
    private float translate_x = 0;
    private float translate_y = 0;
    private Paint menu_line_paint;
    private Paint m_menuBitmapPaint;
    
    // share with touch thread: get from shared storage
    private Stick menu_stick;
    private Circle menu_circle;
    
    private LinkedList<DrawingPrimitive> drawing_queue;
    private Paint m_pointsLinePaint = null;
    
    private DrawingPrimitive currently_touched_pimititve = null;
    private ArrayList<Bitmap> m_menuBitmapsUntouched = null;
    private ArrayList<Bitmap> m_menuBitmapsTouched = null;
    private Bitmap m_menuBackground = null;
    
    // for debug purposes
    final int d_drawsBetweenFpsRecount = 10;
    int d_drawsMade = 0;
    long d_timePassedBetweenFpsRecounts = 0;
    float d_fps = 0f;
    private Paint debug_paint;
    
    public GameView(Context context) {
    	super(context);
    	this.setBackgroundColor(Color.WHITE);
    	
    	Animation.getInstance().setContext(context);
    	game_data = GameData.getInstance();
    	game_data.init(this);
    	gameLoopThread = new GameLoopThread(this);
    	
    	holder = getHolder();
    	debug_paint = GameData.debug_paint;
		
    	menu_line_paint = GameData.menu_line_paint;
    	m_pointsLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    	m_pointsLinePaint.setStyle(Paint.Style.STROKE);
    	m_pointsLinePaint.setStrokeWidth(2);
    	m_pointsLinePaint.setColor(Color.BLACK);
    	
    	menu_stick = game_data.getMenuStick();
    	menu_circle = game_data.getMenuCircle();
    	
    	drawing_queue = game_data.getDrawingQueue();
    	touch_thread = new TouchEventThread(this);
    	touch_thread.init();
    	
    	setWillNotDraw(true);    	
    	m_menuBitmapPaint = new Paint();
    	
    	BitmapWorkerTask task = new BitmapWorkerTask(this);
        task.execute();
        
    	holder.addCallback(new SurfaceHolder.Callback() {
    		
    		@Override
    		public void surfaceDestroyed(SurfaceHolder holder) {
    			boolean retry = true;
    			/*set the flag to false */
    			gameLoopThread.setRunning(false);
    			while (retry) {
    				try {
    					gameLoopThread.join();
    					retry = false;
    				} catch (InterruptedException e) {
    					// we will try it again and again...
    				}
    			}
    		}

    		@Override
    		public void surfaceCreated(SurfaceHolder holder) {
    			gameLoopThread.setRunning(true);
    			touch_thread.setRunning(true);
    			gameLoopThread.start(); 
    			touch_thread.start();
    		}

    		@Override
    		public void surfaceChanged(SurfaceHolder holder, int format,
    				int width, int height) {

    		}
    	});
	}

    public void setMetrics() {
    	menu_stick.setPosition(MainActivity.layout_width - 140, MainActivity.layout_height - 20, MainActivity.layout_width - 40, MainActivity.layout_height - 20);
    	menu_circle.setPosition(MainActivity.layout_width - 180, MainActivity.layout_height - 20, 10, 0);
    	game_data.setMetrics();
    }
    
    @Override
    public boolean onTouchEvent(final MotionEvent ev) {    	
    	touch_thread.pushEvent(ev);
        return true;
    }
    
    private void drawMenu(Canvas canvas)
    {
    	canvas.drawBitmap(m_menuBackground, 0, 0, m_menuBitmapPaint);
    	Bitmap b = m_menuBitmapsTouched.get(0);
    	float dx = (MainActivity.layout_width - m_menuBitmapsUntouched.size() * b.getWidth()) / (float)m_menuBitmapsUntouched.size();
    	float left = dx / 2f;
    	boolean menuTouches[] = GameData.getMenuTouchState(); 
    	for (int i = 0; i < m_menuBitmapsUntouched.size(); i++) {
    		if (menuTouches[i])
    			b = m_menuBitmapsTouched.get(i);
    		else
    			b = m_menuBitmapsUntouched.get(i);
    		canvas.drawBitmap(b, left, GameData.menuIconsTop, m_menuBitmapPaint);
    		left += (b.getWidth() + dx);
    	}
    }
    
    @Override  
    public void onDraw(Canvas canvas) {   	
        canvas.save();
        
        // debug info
        long prevTime = game_data.getPrevDrawingTime();
        game_data.writeDrawingTime();
        d_timePassedBetweenFpsRecounts += (game_data.getPrevDrawingTime() - prevTime);
        if (d_drawsBetweenFpsRecount == ++d_drawsMade) {
        	d_drawsMade = 0;
        	d_fps = d_drawsBetweenFpsRecount * 1000f / (float)d_timePassedBetweenFpsRecounts;
        	d_timePassedBetweenFpsRecounts = 0;
        }
        canvas.drawText("FPS: " + Float.toString(d_fps), 30, 470, debug_paint);
        
        // bottom menu 
        canvas.drawLine(game_data.bottom_menu_x1, game_data.bottom_menu_y, game_data.bottom_menu_x2, game_data.bottom_menu_y, menu_line_paint);
        
        // top menu
        canvas.drawLine(game_data.top_menu_x1, game_data.top_menu_y, game_data.top_menu_x2, game_data.top_menu_y, menu_line_paint);
        
        synchronized (game_data.getLocker()) {
        	if (m_menuBackground != null)
            	drawMenu(canvas);
	        menu_stick.draw(canvas);
	        menu_circle.draw(canvas);
	        // finally draw all joints
	        for (DrawingPrimitive v : drawing_queue)
	        	v.draw(canvas);
	        
	        Path path = new Path();
	        boolean first = true;
	        for(PointF point : GameData.drawnPoints){
	            if(first){
	                first = false;
	                path.moveTo(point.x, point.y);
	            }
	            else{
	                path.lineTo(point.x, point.y);
	            }
	        }
	        
	        canvas.drawPath(path, m_pointsLinePaint);
        }
        canvas.restore();
    }
    
    
    private boolean inMenu(Stick stick) {
    	int dy = 40;
    	return (!stick.isHigher(MainActivity.layout_height - dy));
    }
    
    public DrawingPrimitive getTouchedPrimitive() {
    	return currently_touched_pimititve;
    }
 
    public void setTouchedPrimitive(DrawingPrimitive pr) {
    	currently_touched_pimititve = pr;
    }
    
    public void setMenuBitmaps(ArrayList<Bitmap> bitmapList)
    {
    	m_menuBackground = bitmapList.get(0);
    	m_menuBitmapsUntouched = new ArrayList<Bitmap>();
    	m_menuBitmapsTouched = new ArrayList<Bitmap>();
    	for (int i = 0; i < GameData.numberOfMenuIcons; i++)
    		m_menuBitmapsUntouched.add(bitmapList.get(i + 1));
    	for (int i = 0; i < GameData.numberOfMenuIcons; i++)
    		m_menuBitmapsTouched.add(bitmapList.get(i + 1 + GameData.numberOfMenuIcons));
    	
    	WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
    	Display display = wm.getDefaultDisplay();
    	float width = display.getWidth();
    	
    	GameData.setMenuBottom(m_menuBackground.getHeight());
    	touch_thread.SetMenuRects(width, m_menuBitmapsUntouched.get(0).getWidth(), m_menuBitmapsUntouched.get(0).getHeight());
    }
    
    class BitmapWorkerTask extends AsyncTask<Void, Void, ArrayList<Bitmap>> {
        private final WeakReference<GameView> imageViewReference;

        public BitmapWorkerTask(GameView gameView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<GameView>(gameView);
        }

      
        // Once complete, see if ImageView is still around and set bitmap.
        protected void onPostExecute(ArrayList<Bitmap> bitmapList) {
            if (imageViewReference != null) {
                final GameView gameView = imageViewReference.get();
                if (gameView != null) {
                	gameView.setMenuBitmaps(bitmapList);
                }
            }
        }
    

		@Override
		protected ArrayList<Bitmap> doInBackground(Void... params) {
			BitmapFactory.Options options = new BitmapFactory.Options();
	    	options.inJustDecodeBounds = false;
	    	
	    	ArrayList<Bitmap> output = new ArrayList<Bitmap>();
	    	output.add(BitmapFactory.decodeResource(getResources(), R.drawable.menu_back, options));
	    	
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.save, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.open, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.pencil, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.hand, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.prev, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.next, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.new_frame, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.play, options));
	        
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.save_touched, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.open_touched, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.pencil_touched, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.hand_touched, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.prev_touched, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.next_touched, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.new_frame_touched, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.play_touched, options));
			return output;
		}
    }
    
    public void setDrawingQueue(LinkedList<DrawingPrimitive> q) {
    	drawing_queue = q;
    }
}

