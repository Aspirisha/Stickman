package com.autumncoding.stickman;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.autamncoding.stickman.R;
import com.autumncoding.stickman.Animation.AnimationState;
import com.autumncoding.stickman.TouchEventThread.CurrentDrawingState;

public class GameView extends SurfaceView {
	private GameData game_data;
	private static final int INVALID_POINTER_ID = -1;
	private int mActivePointerId = INVALID_POINTER_ID;
	private SurfaceHolder holder;
	private GameLoopThread gameLoopThread;
	private TouchEventThread touch_thread;
    
    private Paint m_pointsLinePaint = null;
    private Paint m_boundRectPaint = null;
    
    private AbstractDrawingPrimitive currently_touched_pimititve = null;
    private Bitmap m_menuBackground = null;
    private Path path = new Path();
    
    // for debug purposes
    final int d_drawsBetweenFpsRecount = 10;
    int d_drawsMade = 0;
    long d_timePassedBetweenFpsRecounts = 0;
    float d_fps = 0f;
    private Paint debug_paint;
    private ArrayList<MenuIcon> m_menuIcons = new ArrayList<MenuIcon>();
    private boolean canDraw = false;

    
    public GameView(Context context) {
    	super(context);
    	this.setBackgroundColor(Color.WHITE);
    	
    	for (int i = 0; i < GameData.numberOfMenuIcons; ++i)
    		m_menuIcons.add(new MenuIcon());
    	
    	game_data = GameData.getInstance();
    	
    	holder = getHolder();
    	debug_paint = GameData.debug_paint;
		
    	m_pointsLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    	m_pointsLinePaint.setStyle(Paint.Style.STROKE);
    	m_pointsLinePaint.setStrokeWidth(2);
    	m_pointsLinePaint.setColor(Color.RED);
    	
    	m_boundRectPaint = new Paint(m_pointsLinePaint);
    	m_boundRectPaint.setColor(Color.BLACK);
    	
    	setWillNotDraw(true);    	
    	
 
    	BitmapWorkerTask task = new BitmapWorkerTask(this);
        task.execute();
        
    	holder.addCallback(new SurfaceHolder.Callback() {
    		
    		@Override
    		public void surfaceDestroyed(SurfaceHolder holder) {
    			boolean retry = true;

    			gameLoopThread.setRunning(false);
    			touch_thread.setRunning(false);
    			while (retry) {
    				try {
    					gameLoopThread.join();
    					touch_thread.join();
    					canDraw = false;
    					retry = false;
    				} catch (InterruptedException e) {
    					// we will try it again and again...
    				}
    			}
    		}

    		@Override
    		public void surfaceCreated(SurfaceHolder holder) {
    			createNewThreads();
    			gameLoopThread.setRunning(true);
    			touch_thread.setRunning(true);
    			gameLoopThread.start(); 
    			touch_thread.start();
    			GameData.mainActivity.CountMetrics();
    			GameData.mainActivity.UpdateFramesInfo(Animation.getInstance().getCurrentFrameNumber(), 
						Animation.getInstance().getFramesNumber());
    			canDraw = true;
    		}

    		@Override
    		public void surfaceChanged(SurfaceHolder holder, int format,
    				int width, int height) {

    		}
    	});
	}

    private void createNewThreads() {
    	gameLoopThread = new GameLoopThread(this);
    	touch_thread = new TouchEventThread(this);
    	touch_thread.init(m_menuIcons);
    }
    
    @Override
    public boolean onTouchEvent(final MotionEvent ev) {    	
    	touch_thread.pushEvent(ev);
        return true;
    }
    
    private void drawMenu(Canvas canvas)
    {
    	canvas.drawBitmap(m_menuBackground, 0, 0, GameData.menuBitmapPaint);
    	for (MenuIcon icon : m_menuIcons)
    		icon.Draw(canvas);
    }

    public void debugDraw(Canvas canvas) {
        // debug info
        long prevTime = game_data.getPrevDrawingTime();
        game_data.writeDrawingTime();
        d_timePassedBetweenFpsRecounts += (game_data.getPrevDrawingTime() - prevTime);
        if (d_drawsBetweenFpsRecount == ++d_drawsMade) {
        	d_drawsMade = 0;
        	d_fps = d_drawsBetweenFpsRecount * 1000f / (float)d_timePassedBetweenFpsRecounts;
        	d_timePassedBetweenFpsRecounts = 0;
        }
        canvas.drawText("FPS: " + Float.toString(d_fps), 30, MainActivity.layout_height - 20, debug_paint);
        //canvas.drawText("angle: " + Float.toString(GameData.debugValue), 30, MainActivity.layout_height - 20, debug_paint);
        // end of debug info
    }
    
    @Override  
    public void onDraw(Canvas canvas) {   	
        canvas.save();
        debugDraw(canvas);
        
        if (canDraw) {	    
            canvas.drawRect(GameData.fieldRect, m_boundRectPaint);
	        synchronized (GameData.getLocker()) {	 
	            if (GameData.framesChanged) {
		        	GameData.framesChanged = false;
		        	GameData.mainActivity.setFramesSeekbarRange(Animation.getInstance().getFramesNumber());
		        	GameData.mainActivity.onCurrentframeChanged(Animation.getInstance().getCurrentFrameNumber() - 1);
					GameData.mainActivity.UpdateFramesInfo(Animation.getInstance().getCurrentFrameNumber(), 
							Animation.getInstance().getFramesNumber());
		        }
	        	
	        	Animation.getInstance().drawFrame(canvas);
		        
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
		        
		        m_pointsLinePaint.setColor(Color.GRAY);
		        canvas.drawPath(path, m_pointsLinePaint);
		        path.reset();
		        
	        	CurrentDrawingState ds = touch_thread.getCurrentDrawingState();
		        if (ds.m_isDrawingProcess) {
			        m_pointsLinePaint.setColor(Color.RED);
			        if (!ds.m_hasIntersection) {
			        	canvas.drawLine(ds.m_currentDrawingPoint.x, ds.m_currentDrawingPoint.y, 
			        			ds.m_startDrawingPoint.x, ds.m_startDrawingPoint.y, m_pointsLinePaint);
			        } else {
			        	canvas.drawCircle(ds.m_centre.x, ds.m_centre.y, ds.m_radius, m_pointsLinePaint);
			        }
		        }
		        
		        
		        if (m_menuBackground != null)
	            	drawMenu(canvas);
	        }
        }
        canvas.restore();
    }
    
    public AbstractDrawingPrimitive getTouchedPrimitive() {
    	return currently_touched_pimititve;
    }
 
    public void setTouchedPrimitive(AbstractDrawingPrimitive pr) {
    	currently_touched_pimititve = pr;
    }
    
    public void setMenuBitmaps(ArrayList<Bitmap> bitmapList) {
    	synchronized (GameData.getLocker()) {
	    	m_menuBackground = bitmapList.get(0);
	    	GameData.topMenuHeight = m_menuBackground.getHeight();
	    	
	    	for (int i = 0; i < GameData.numberOfMenuIcons; i++) 
	    		m_menuIcons.get(i).setActiveBitmap(bitmapList.get(i + 1));
	    	for (int i = 0; i < GameData.numberOfMenuIcons; i++)
	    		m_menuIcons.get(i).setTouchedBitmap(bitmapList.get(i + 1 + GameData.numberOfMenuIcons));
	    
	    	GameData.menuPencil = m_menuIcons.get(0);
	    	GameData.menuDrag = m_menuIcons.get(1);
	    	GameData.menuPrev = m_menuIcons.get(2);
	    	GameData.menuNext = m_menuIcons.get(3);
	    	GameData.menuNew = m_menuIcons.get(4);
	    	GameData.menuBin = m_menuIcons.get(5);
	    	GameData.menuPlay = m_menuIcons.get(6);
	    	
	    	GameData.menuPencil.setUnavailableBitmap(bitmapList.get(1 + 2 * GameData.numberOfMenuIcons));
	    	GameData.menuDrag.setUnavailableBitmap(bitmapList.get(2 + 2 * GameData.numberOfMenuIcons));
	    	GameData.menuPrev.setUnavailableBitmap(bitmapList.get(3 + 2 * GameData.numberOfMenuIcons));
	    	GameData.menuNext.setUnavailableBitmap(bitmapList.get(4 + 2 * GameData.numberOfMenuIcons));
	    	GameData.menuNew.setUnavailableBitmap(bitmapList.get(5 + 2 * GameData.numberOfMenuIcons));
	    	GameData.menuBin.setUnavailableBitmap(bitmapList.get(6 + 2 * GameData.numberOfMenuIcons));
	    	
	    	m_menuIcons.get(0).setTouched();
	    	m_menuIcons.get(2).setUnavailable();
	    	m_menuIcons.get(3).setUnavailable();
	    	GameData.setMenuBottom(m_menuBackground.getHeight());
	    	
	    	GameData.menuIcons = m_menuIcons;

	    	
	    	Bitmap b = bitmapList.get(1);
	    	while (!GameData.metricsSet) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    	}
	    	float dx = (MainActivity.layout_width - GameData.numberOfMenuIcons * b.getWidth()) / (float)GameData.numberOfMenuIcons;
	    	float left = dx / 2f;
	    	for (int i = 0; i < m_menuIcons.size(); i++) {
	    		m_menuIcons.get(i).setPosition(left, GameData.menuIconsTop);
	    		left += (b.getWidth() + dx);
	    	}
    	}
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
                	
            		if (GameData.saveToTemp) {
            			synchronized (GameData.getLocker()) {
            				try {
            					Animation.getInstance().loadFromFile("", true);
            				} catch (Exception e) {
            					e.printStackTrace();
            					Animation.getInstance().clear();
            				}
            			}
            		} else {
            			Animation.getInstance().clear();
            		}
                }
            }
        }
    

		@Override
		protected ArrayList<Bitmap> doInBackground(Void... params) {
			BitmapFactory.Options options = new BitmapFactory.Options();
	    	options.inJustDecodeBounds = false;
	    	
	    	ArrayList<Bitmap> output = new ArrayList<Bitmap>();
	    	output.add(BitmapFactory.decodeResource(getResources(), R.drawable.menu_back, options));
	    	
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.pencil, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.hand, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.prev, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.next, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.new_frame, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.bin, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.play, options));
	        
	        
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.pencil_touched, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.hand_touched, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.prev_touched, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.next_touched, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.new_frame_touched, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.bin_touched, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.stop_touched, options));
	        
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.pencil_unavailable, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.hand_unavailable, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.prev_unavailable, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.next_unavailable, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.new_frame_unavailable, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.bin_unavailable, options));
	        
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.stop, options));
	        output.add(BitmapFactory.decodeResource(getResources(), R.drawable.stop_touched, options));
	        
			return output;
		}
    }
    
    public void updateUpperMenu() {
	    if (Animation.getInstance().hasNextFrame())
			m_menuIcons.get(3).setAvailable();
		else
			m_menuIcons.get(3).setUnavailable();
	    if (Animation.getInstance().hasPrevFrame())
	    	m_menuIcons.get(2).setAvailable();
	    else
	    	m_menuIcons.get(2).setUnavailable();
    }
}

