package com.orangelabs.rcs.whiteboard;

import java.nio.ByteBuffer;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Vibrator;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Draw photo UI
 * 
 * @author Jean-Marc AUFFRET
 */
public class DrawPhoto implements ShakeMotionListener {
	private static final String TAG = "whiteboard";

	/**
	 * Default backgroung color
	 */
	private static int DEFAULT_BACKGROUND_COLOR = 0xFFFFFFFF;

	/**
	 * Default pen color
	 */
	private static int DEFAULT_PEN_COLOR = 0xFF0000FF;

	/**
	 * Default pen width
	 */
	private static int DEFAULT_PEN_WIDTH = 5;

	/**
	 * Screen metrics
	 */
	private DisplayMetrics metrics = new DisplayMetrics();

	/**
	 * View
	 */
	private MyView view;
	private MultimediaSessionView activity;
	
	/**
	 * Painter
	 */
    private Paint painter;

    /**
     * Pen width
     */
    private int penWidth = DEFAULT_PEN_WIDTH;
    
    /**
     * Share motion detector
     */
    private ShakeMotionDetector shakeDetector = null;  
    
    public DrawPhoto(MultimediaSessionView act) {
    	activity=act;
		// Always on window
    	act.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // Set the portrait mode
    	act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);        

        // Set the title
    	act.setTitle(R.string.app_name);
        
        // Get default screen metrics
    	act.getWindowManager().getDefaultDisplay().getMetrics(metrics);        	

	    // Create the view
        view = new MyView(act);
        act.setContentView(view);
        painter = new Paint();
        painter.setAntiAlias(true);
        painter.setDither(true);
        painter.setColor(DEFAULT_PEN_COLOR);
        painter.setStyle(Paint.Style.STROKE);
        painter.setStrokeJoin(Paint.Join.ROUND);
        painter.setStrokeCap(Paint.Cap.ROUND);
        painter.setStrokeWidth(penWidth);

        // Clear the view by default
        view.clear();

        // Create shake detector 
        shakeDetector = new ShakeMotionDetector(act);
        shakeDetector.addListener(this);
        shakeDetector.start();
        
        // Display toast
        displayToast();
        
        //dynamicmenu items
        activity.currentTask=activity.CURRENTTASK_DRAWPHOTO; //DrawPhoto
    }
       
    void onDestroy() {
    	// Remove shake detector
        if (shakeDetector != null) {
	    	shakeDetector.stop();
	    	shakeDetector.removeListener();
        }
    }
    
	/**
	 * Display a custom toast
	 */
	public void displayToast() {
		LayoutInflater inflater = activity.getLayoutInflater();
		View layout = inflater.inflate(R.layout.shake_toast_layout,
				(ViewGroup)activity.findViewById(R.id.csh_draw_toast_layout_root));
		TextView text = (TextView)layout.findViewById(R.id.csh_draw_toast_text);
		text.setText(activity.getString(R.string.label_shake_to_erase));
		
		Toast toast = new Toast(activity);
		toast.setGravity(Gravity.TOP, 0, 20);
		toast.setDuration(Toast.LENGTH_LONG);
		toast.setView(layout);
		toast.show();
	}    
    
    public void colorChanged(int color) {
        painter.setColor(color);
    }

    public class MyView extends View {
        private static final float TOUCH_TOLERANCE = 4;

        private Bitmap  mBitmap;
        private Canvas  mCanvas;
        private Path    mPath;
        private float mX, mY;
        
        public MyView(Context c) {
            super(c);
            
            mBitmap = Bitmap.createBitmap(metrics.widthPixels, metrics.heightPixels, Bitmap.Config.RGB_565);
            mCanvas = new Canvas(mBitmap);
            mPath = new Path();
        }

        @Override
        protected void onDraw(Canvas canvas) {
        	// Draw the default background color
            canvas.drawColor(DEFAULT_BACKGROUND_COLOR);

            // Draw draw
        	canvas.drawBitmap(mBitmap, 0, 0, painter);
            canvas.drawPath(mPath, painter);
        }
        
        private void touch_start(float x, float y) {
            mPath.reset();
            mPath.moveTo(x, y);
            mX = x;
            mY = y;
        }

        private void touch_move(float x, float y) {
            float dx = Math.abs(x - mX);
            float dy = Math.abs(y - mY);
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
                mX = x;
                mY = y;
            }
        }
        
        private void touch_up() {
            mPath.lineTo(mX, mY);
            
            // commit the path to our offscreen
            mCanvas.drawPath(mPath, painter);

            // kill this so we don't double draw
            mPath.reset();
        }
        
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touch_start(x, y);
                    invalidate();
                    activity.send("ts("+x+","+y+")");
                    break;
                case MotionEvent.ACTION_MOVE:
                    touch_move(x, y);
                    invalidate();
                    activity.send("tm("+x+","+y+")");
                    break;
                case MotionEvent.ACTION_UP:
                    touch_up();
                    invalidate();
                    activity.send("tu("+x+","+y+")");
                    break;
            }

            return true;
        }
        
        public void clear() {
            if (mCanvas != null) {
            	int current = painter.getColor();
            	painter.setColor(DEFAULT_BACKGROUND_COLOR);
                mCanvas.drawPaint(painter);
                invalidate();
                if (current == DEFAULT_BACKGROUND_COLOR) {
                	painter.setColor(DEFAULT_PEN_COLOR);
                } else {
                	painter.setColor(current);
                }
            }
        }        
        
        public void setBackgroundBitmap(Bitmap bmp) {
            double xScale = (double)bmp.getWidth() / (double)metrics.widthPixels;
            double yScale = (double)bmp.getHeight() / (double)metrics.heightPixels;
            double scale = Math.max(xScale, yScale);
            int w, h;
            if (scale != 0) {
                w = (int)((double)bmp.getWidth() / (double)scale);
                h = (int)((double)bmp.getHeight() / (double)scale);
            } else {
	            w = bmp.getWidth();
	            h = bmp.getHeight();
            }        	
        	Rect src = new Rect(0, 0, bmp.getWidth(), bmp.getHeight());
            Rect dst = new Rect(0, 0, w, h);
        	mCanvas.drawBitmap(bmp, src, dst, painter);
            mCanvas.drawPath(mPath, painter);
        }        
    }

    /**
	 * Share event detected
	 */
	public void onShakeDetected() {
		// Make a vibration
        Vibrator vibrator = (Vibrator)activity.getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {500L, 200L, 500L};
        vibrator.vibrate(pattern, -1);

		// Erase screen
        view.clear();
        activity.send("cl("+0.0+","+0.0+")"); //un-used arg just for a unique format
	}
	
	public void remoteCmd(String txt) {
		try {
			int quote1=-1;
			int quote2=-1;
			int comma=-1;
			String cmd=txt.substring(0, 2);
			
			boolean float_argument=false;
			float x=Float.parseFloat(txt.substring(quote1+1, comma));
			float y=Float.parseFloat(txt.substring(comma+1,quote2));
			float_argument=true;
			
			if (float_argument && cmd.equals("ts")) { //touch start			
	            view.touch_start(x, y);
	            view.invalidate();
			} else if (float_argument && cmd.equals("tm")) { //touch move
	            view.touch_move(x, y);
	            view.invalidate();	
			} else if (cmd.equals("tu")) { //touch up
				view.touch_up();
	            view.invalidate();
			} else if (cmd.equals("cl")) { //clear the whiteboard
				view.clear();
				view.invalidate();
			}
			cmd_state=new String(cmd); //update cmd_state only if cmd if valid
		} catch (Throwable e) {
    		Log.d(TAG, "Draw failed", e);
		}
	}
}
