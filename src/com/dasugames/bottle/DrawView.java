package com.dasugames.bottle;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.media.SoundPool;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * This class handles all aspects of the drawable canvas
 * and the letter guide underneath it.
 * 
 * It is a monolithic, unmanageable class because it was
 * written years and years ago before I had a better idea
 * about what I was doing.
 * @author darren.sue
 *
 */
public class DrawView extends View {
	private Bitmap mBitmap;
	private Canvas mCanvas;
	private Paint mBitmapPaint;
	private Path mPath;
	private Paint fingerDraw;
	private Paint debugDraw;
	private Path rPath;
	private LetterGeometry currentLetter;
	public boolean isActive;
	private Handler fingerEnd;
	private Runnable fingerEndRunnable;
	private int viewWidth = 0;
	private int viewHeight = 0;
	int old_speed = 0;
	int old_volume = 999;
	SoundPool dragSound;
	int dragSoundIndex;
	int dragSoundStream;
	boolean is_drag_playing = false;
	
	public DrawView(Context context, AttributeSet attrs) {
		super(context,attrs);

		isActive = true;
		
		mBitmapPaint = new Paint(Paint.DITHER_FLAG);
		mPath = new Path();
		// resize the paint thingy
		
		fingerDraw = new Paint();
		fingerDraw.setColor(Color.BLACK);
		
		// Diameter will be a third of an inch, which seems tiny
		fingerDraw.setStrokeWidth(context.getResources().getDisplayMetrics().xdpi / 3);
		fingerDraw.setStyle(Paint.Style.STROKE);
		fingerDraw.setAntiAlias(true);
		fingerDraw.setStrokeJoin(Paint.Join.ROUND);
		fingerDraw.setStrokeCap(Paint.Cap.ROUND);
		rPath = new Path();
		debugDraw = new Paint();
		debugDraw.setColor(Color.RED);
		debugDraw.setStrokeWidth(3);
		debugDraw.setStyle(Paint.Style.STROKE);
		debugDraw.setStrokeJoin(Paint.Join.ROUND);
		debugDraw.setStrokeCap(Paint.Cap.ROUND);

		t_tolerance = context.getResources().getDisplayMetrics().xdpi / 15f;

		fingerEnd = new Handler();
		fingerEndRunnable = new Runnable() {
			public void run() {
				dragSound.setVolume(dragSoundStream, 0, 0);
			}
		};

		currentLetter = new LetterGeometry('a', 0, 0, 100, 100);

	}
	
	/**
	 * Resets the current view by reseting the background letter and clearing the sketchboard.
	 * @param nLetter
	 * @param n_upperLeftX
	 * @param n_upperLeftY
	 * @param n_lowerRightX
	 * @param n_lowerRightY
	 */
	public void reset(char nLetter,float n_upperLeftX, float n_upperLeftY, float n_lowerRightX , float n_lowerRightY) {
		
		// create a geometry
		currentLetter = new LetterGeometry(nLetter, n_upperLeftX,  n_upperLeftY,  n_lowerRightX ,  n_lowerRightY);

		// clears the board
		mBitmapPaint = new Paint(Paint.DITHER_FLAG);
		mBitmap = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(), Bitmap.Config.ARGB_4444);
		mCanvas = new Canvas(mBitmap);
		
	}
	
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mBitmap = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_4444);
		mCanvas = new Canvas(mBitmap);

		setViewWidth(w);
		setViewHeight(h);
		if (onFinishStageListener != null) {
			onFinishStageListener.onFinishChecked();
		}
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		canvas.drawColor(0x00000000);
		canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
		canvas.drawPath(mPath, fingerDraw);
		
		// Draw a debug line showing boundaries
		//currentLetter.debugDraw();
	}


	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		// prevent further interaction in the middle of a transition
		if (isActive == false){
			return true;
		}
		
		float x = event.getX();
		float y = event.getY();
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			touch_start(x,y);
			invalidate();
			break;
		case MotionEvent.ACTION_MOVE:
			touch_move(x,y);
			invalidate();
			break;
		case MotionEvent.ACTION_UP:
			touch_up();
			invalidate();
			break;
		}
		return true;
	}
	


	public void setSounds(SoundPool newDragSound, int newDragSoundIndex)
	{
		dragSoundIndex = newDragSoundIndex;
		dragSound = newDragSound;
	}
	
	private void playSound(int volume) {
		
		volume = Math.min(999, volume);
		
		if (is_drag_playing && dragSound != null) {
			dragSound.setVolume(dragSoundStream, volume / 1000f, volume / 1000f);
		} else if (dragSound != null) {
			dragSoundStream = dragSound.play(dragSoundIndex, volume / 1000f,
					volume / 1000f, 0, -1, 1);
			is_drag_playing = true;
		}
		
		fingerEnd.removeCallbacks(fingerEndRunnable);
		fingerEnd.postDelayed(fingerEndRunnable, 50);
	}

	private int speedToVolume(int speed) {
		final int proportional = 7;
		final int differential = 3;
		int new_volume = speed * proportional + differential * (speed - old_speed);
		old_speed = speed;
		old_volume = 2 * new_volume + old_volume / 3;
		return old_volume;
	}
	
	private void stopSound() {
		if (is_drag_playing && dragSound!= null) {
			dragSound.stop(dragSoundStream);
		}
		old_speed = 0;
		old_volume = 999;
		is_drag_playing = false;
		
	}
	
	private float t_tolerance = 5;
	private boolean lineDown = false;
	
	private void touch_start(float x,float y)
	{
		if (currentLetter.touch(new PointF(x,y)))
		{

			lineDown = true;
			line_start(x,y);
			line_move(x,y);
		}
			oldX = x;
			oldY = y;
			
		
	}
	
	private void touch_move(float x,float y)
	{
		int max_int = 0;
		int current_int = 0;
		
		// sets the number of times for the program to iterate
		int inter_dist = (int) Math.floor(Math.max(Math.abs(x-oldX),Math.abs(y-oldY))/t_tolerance);
		float x_traverse = (x-oldX) / inter_dist;
		float y_traverse = (y-oldY) / inter_dist;
		for (int i = 1; i <= inter_dist; i++)
		{
			float current_x = oldX + x_traverse * i;
			float current_y = oldY + y_traverse * i;
			if (currentLetter.touch(new PointF(current_x, current_y))) {

				if (lineDown) {
					line_move(current_x, current_y);
					current_int++;
					if (current_int > max_int)
						max_int = current_int;
				} else {
					line_start(current_x, current_y);
					current_int = 0;
				}
				lineDown = true;
				
			} else {
				current_int = 0;
				line_up();
				lineDown = false;

			}
			playSound(speedToVolume(max_int));
		}
		
		oldX = x;
		oldY = y;
	}
	private void touch_up()
	{
		line_up();
		lineDown = false;

		stopSound();
		if (onFinishStageListener!= null && currentLetter.is_complete())
		{
			onFinishStageListener.onFinishChecked();
		}
	}
	
	private float oldX,oldY;
	private void line_start(float x,float y)
	{
		
		mCanvas.drawPoint(x, y, fingerDraw);
		mPath.reset();
		mPath.moveTo(x, y);
	}
	private void line_move(float x,float y)
	{
		mPath.lineTo(x, y);

	}
	

	
	private void line_up()
	{

		//mPath.lineTo(oldX, oldY);
		mCanvas.drawPath(mPath, fingerDraw);
		mPath.reset();
	}
		
	/**
	 * 
	 * @author darren.sue
	 *
	 */
	private class LetterGeometry {

		private SpecialPolygon[] polygon_array;
		
		// These are the dimensions of the letter geometry
		// They are set by the dynamic sizing of the letter
		float upperLeftX = 0;
		float upperLeftY = 0;
		float lowerRightX = 100;
		float lowerRightY = 100;
		
		// These are arbitrary bounds used to declare letter geometry data
		// Part of the job here is to convert the data coordinates to the
		// screen coordinates
		float old_upperLeftX = 0;
		float old_upperLeftY = 0;
		float old_lowerRightX = 100;
		float old_lowerRightY = 100;
		
		private void set_bounds(float letter_upperLeftX,float letter_upperLeftY, float letter_lowerRightX,float letter_lowerRightY)
		{
			
			old_upperLeftX = 0;
			old_upperLeftY = 0;
			old_lowerRightX = 100;
			old_lowerRightY = 100;
			
			old_upperLeftX = letter_upperLeftX;
			old_upperLeftY = letter_upperLeftY;
			old_lowerRightX = letter_lowerRightX;
			old_lowerRightY = letter_lowerRightY;
			
		}
		/**
		 * This method is hard coded horribleness because I wrote it years ago before I knew what I was doing.
		 * It generates the polygonal geometry that will constitute the play area for the user.
		 * @param stageLetter
		 * @param n_upperLeftX
		 * @param n_upperLeftY
		 * @param n_lowerRightX
		 * @param n_lowerRightY
		 */
		LetterGeometry(char stageLetter, float n_upperLeftX, float n_upperLeftY, float n_lowerRightX , float n_lowerRightY) {
			
			upperLeftX = 0;
			upperLeftY = 0;
			lowerRightX = 100;
			lowerRightY = 100;
			
			upperLeftX = n_upperLeftX;
			upperLeftY = n_upperLeftY;
			lowerRightX = n_lowerRightX;
			lowerRightY = n_lowerRightY;
			
			
			if (stageLetter == 'A') {
				set_bounds(10,-20,90,120);

				final float[][] pointData = {{0,100},{30,100},{40,80},{60,80},{70,100},{100,100},{80,60},{70,30},{60,0},{40,0},{30,30},{20,60},
								{50,35},{55,55},{45,55}};

				final int [][] triangleData = {{1,2,3,12},{3,4,14,15},{4,5,6,7},{7,4,14},{7,8,13,14},{8,9,10,11,13},{11,12,15,13},{12,3,15}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),triangleData);
			} else if (stageLetter == 'B') {
				set_bounds(-5,-20,100,120);
				final float[][] pointData = {{0,0},{50,0},{80,10},{100,30},{85,45},
						{100,60},{100,80},{80,90},{50,95},{0,95},{0,75},{0,55},{0,40},{0,20},
						{35,20},{60,30},{35,40},
						{35,55},{65,65},{35,75}};

				final int [][] polygonData = {{1,2,15,14},{2,3,4,16,15},{4,5,17,16},{5,19,18,17},{5,6,7,19},
						{7,8,9,20,19},{9,10,11,20},{11,12,18,20},{12,13,17,18},{13,14,15,17}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'C') {
				set_bounds(-5,-20,100,120);
				final float[][] pointData = {{0,70},{0,30},{10,10},{50,0},{90,10},{100,40},{70,40},{50,20},
						{30,30},{50,75},{70,60},{100,60},{90,90},{50,100},{10,90},{30,70}};
				final int [][] polygonData = {{1,2,9,16},{2,3,8,9},{3,4,5,8},{5,6,7,8},{16,10,15,1},{10,13,14,15},{10,11,12,13}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'D') {
				set_bounds(-5,-25,105,125);
				final float[][] pointData = {{0,0},{30,0},{60,0},{80,10},{100,30},{100,70},{80,90},{60,100},{30,100},{0,100},{0,80},{0,20},
						{30,20},{65,25},{70,50},{65,70},{30,80}};
				final int [][] polygonData = {{1,2,13,12},{2,3,4,14,13},{4,5,6,7,16,14},{7,8,9,17,16},{9,10,11,17},{11,12,13,17}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'E') {
				set_bounds(-5,-25,105,125);
				final float[][] pointData = {{0,0},{40,0},{100,0},{100,20},{40,20},{40,40},{100,40},{100,60},
						{40,60},{40,80},{100,80},{100,100},{40,100},{0,100},{0,80},{0,60},{0,40},{0,20},
						{60,0},{60,20},{60,80},{60,100}};
				final int [][] polygonData = {{1,2,5,18},
						{2,3,4,5},
						{5,6,17,18},
						{6,9,16,17},
						{6,7,8,9},
						{9,10,15,16},
						{10,13,14,15},
						{10,11,12,13},
						{3,4,20,19},{11,12,22,21}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'F') {
				set_bounds(-5,-25,105,125);
				final float[][] pointData = {{0,0},{40,0},{100,0},{100,20},{40,20},{40,40},{100,40},{100,60},
						{40,60},{40,100},{0,100},{0,60},{0,40},{0,20},
						{60,0},{60,20},{0,75},{40,75}};
				final int [][] polygonData = {{1,2,5,14},
						{2,3,4,5},
						{5,6,13,14},
						{6,9,12,13},
						{6,7,8,9},
						{9,10,11,12},
						{3,4,16,15},{10,11,17,18}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'G') {
				set_bounds(-5,-20,105,120);
				final float[][] pointData = {{10,10},{50,0},{90,10},{100,35},{70,35},{50,20},{30,50},{50,75},
						{70,60},{45,60},{45,45},{100,45},{100,60},{100,85},{50,95},{10,85},{0,50},
						{70,45}};
				final int [][] polygonData = {{1,2,6,7,17},{2,3,4,5,6},{7,8,15,16,17},{8,9,13,14,15},{10,11,12,13},
						{9,10,11,18}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			}
			else if (stageLetter == 'H')
			{
				set_bounds(-10,-25,110,125);
				final float[][] pointData = {{0,0},{30,0},{30,40},{70,40},{70,0},{100,0},{100,40},{100,60},{100,100},
						{70,100},{70,60},{30,60},{30,100},{0,100},{0,60},{0,40},
						{0,25},{30,25},{70,25},{100,25},{0,75},{30,75},{70,75},{100,75}};
				final int[][] polygonData = {{1,2,3,16},{3,4,11,12},{4,5,6,7},{4,7,8,11},{8,9,10,11},
						{12,13,14,15},{3,12,15,16},
						{1,2,18,17},{5,6,20,19},{9,10,23,24},{13,14,21,22}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			}
			else if (stageLetter == 'I') {
				set_bounds(-25,-25,125,130);
				final float[][] pointData = {{0,0},{100,0},{100,25},{100,75},{100,100},
						{0,100},{0,75},{0,25}};
				final int [][] polygonData = {{1,2,3,8},{3,4,7,8},{4,5,6,7}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'J') {
				set_bounds(-5,-25,110,125);
				final float[][] pointData = {{100,0},{65,0},{65,25},{65,70},{50,80},{40,75},{30,65},{0,65},{0,90},{50,100},{100,90},{100,25}};

				final int [][] polygonData = {{1,2,3,12},{3,4,11,12},{4,5,10,11},{5,6,9,10},{6,7,8,9}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'K') {
				set_bounds(-10,-25,100,130);
				final float[][] pointData = {{0,0},{30,0},{30,40},{70,0},{100,0},{60,50},{100,100},{70,100},{30,60},{30,100},{0,100},{0,60},{0,40},{30,20},{0,20},{0,80},{30,80},
						{50,20},{80,20},{80,80},{50,80}};
				//final int [][] polygonData = {{1,2,3,4,5,6,7,8,9,10,11,12,13}};
				final int [][] polygonData = {{1,2,3,13},{3,6,9,12,13},{3,4,5,6},{6,7,8,9},{9,10,11,12},{1,2,14,15},{10,11,16,17},
						{4,5,19,18},{7,8,21,20}};

				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'L') {
				set_bounds(-10,-25,100,125);
				final float[][] pointData = {{0,0},{30,0},{30,25},{30,80},{100,80},{100,100},{0,100},{0,80},{0,25},{70,100},{70,80}};
				final int [][] polygonData = {{1,2,3,9},{3,4,8,9},{5,6,10,11},{11,10,7,8}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'M') {
				set_bounds(-10,-25,110,130);
				final float[][] pointData = {{0,0},{25,0},{40,35},{50,60},{60,35},{75,0},{100,0},{100,40},{100,100},
						{80,100},{80,40},{70,65},{60,100},{40,100},{30,65},{20,40},{20,100},{0,100},{0,40},
						{80,80},{100,80},{0,80},{20,80}};
				final int [][] polygonData = {{1,2,3,16,19},{3,4,15,16},{4,12,13,14,15},{4,5,11,12},{5,6,7,8,11},{8,9,10,11},{16,17,18,19},
						{9,10,20,21},{17,18,22,23}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'N') {
				set_bounds(-10,-25,110,130);
				final float[][] pointData = {{0,0},{25,0},{50,35},{75,65},{75,0},{100,0},{100,65},
						{100,100},{75,100},{50,65},{25,35},{25,100},{0,100},{0,35},
						{0,80},{25,80},{75,20},{100,20}};
				final int [][] polygonData = {{1,2,3,11,14},{3,4,10,11},{4,7,8,9,10},{4,5,6,7},{11,12,13,14},
						{15,16,12,13},{17,18,6,5}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'O') {
				set_bounds(-5,-25,105,125);
				final float[][] pointData = {{10,10},{50,0},{90,10},{100,50},{90,90},{50,100},{10,90},{0,50},
						{35,25},{50,20},{65,25},{70,50},{65,75},{50,80},{35,75},{30,50}};
				//final int [][] polygonData = {{1,2,3,4,5,6,7,8},{9,10,11,12,13,14,15,16}};
				final int [][] polygonData = {{1,2,10,9},{2,3,11,10},{3,4,12,11},{4,5,13,12},{5,6,14,13},{6,7,15,14},{7,8,16,15},{8,1,9,16}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			}
			else if (stageLetter == 'P')
			{
				set_bounds(-10,-25,100,130);
				final float[][] pointData = {{0,0},{30,0},{50,0},{85,10},{100,35},{85,60},{50,65},
						{30,65},{30,80},{30,100},{0,100},{0,80},{0,65},{0,45},{0,20},
						{30,20},{55,20},{65,35},{55,45},{30,45}};
				//final int [][] polygonData = {{1,2,3,4,5,6,7,8,9,10,11,12,13,14,15},{16,17,18,19,20}};
				final int [][] polygonData = {{1,2,16,15},{2,3,4,17,16},{4,5,18,17},{5,6,19,18},{6,7,8,20,19},
						{9,10,11,12},{8,9,12,13},{8,13,14,20},{14,15,16,20}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			}
			else if (stageLetter == 'Q')
			{			
				set_bounds(0,-25,105,125);
				final float[][] pointData = {{10,10},{50,0},{90,10},{100,50},{90,90},{50,100},{10,90},{0,50},
					{35,25},{50,20},{65,25},{70,50},{65,75},{50,80},{35,75},{30,50},
					{85,80},{70,90},{90,105},{105,95}};
				final int [][] polygonData = {{1,2,10,9},{2,3,11,10},{3,4,12,11},{4,5,13,12},{5,6,14,13},{6,7,15,14},{7,8,16,15},{8,1,9,16},
				{17,18,19,20}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			}
			else if (stageLetter == 'R')
			{
				set_bounds(-10,-25,100,130);
				final float[][] pointData = {{0,0},{30,0},{50,0},{85,10},{100,35},{75,50},{50,65},
						{30,65},{30,80},{30,100},{0,100},{0,80},{0,65},{0,45},{0,20},
						{30,20},{55,20},{65,35},{55,45},{30,45},
						{100,80},{100,100},{65,100},{65,80}};
				final int [][] polygonData = {{1,2,16,15},{2,3,4,17,16},{4,5,18,17},{5,6,19,18},{6,7,8,20,19},
						{9,10,11,12},{8,9,12,13},{8,13,14,20},{14,15,16,20},
						{6,7,24,21},{21,22,23,24}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			}
			else if (stageLetter == 'S')
			{
				set_bounds(-5,-25,105,125);
				final float[][] pointData = {{50,0},{95,5},{100,30},{70,30},{65,20},{50,20},{35,30},{90,50},{100,70},
						{95,90},{50,100},{5,90},{0,65},{25,65},{35,75},{50,80},{70,70},{10,45},{0,30},{5,5}};
				final int [][] polygonData = {{1,2,5,6},{2,3,4,5},{6,7,19,20,1},{7,8,9,17,18,19},{9,10,11,16,17},{11,12,15,16},{12,13,14,15}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			}
			else if (stageLetter == 'T')
			{
				set_bounds(0,-25,100,130);
				final float[][] pointData = {{0,0},{35,0},{65,0},{100,0},{100,20},{65,20},{65,80},{65,100},{35,100},{35,80},{35,20},{0,20}};
				final int [][] polygonData = {{1,2,11,12},{2,3,7,10},{3,4,5,6},{7,8,9,10}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'U') {
				set_bounds(-10,-25,110,125);
				final float[][] pointData = {{0,0},{25,0},{25,25},{25,65},{50,80},{75,65},{75,25},{75,0},
						{100,0},{100,25},{100,80},{90,90},{50,100},{10,90},{0,80},{0,25}};
				final int [][] polygonData = {{1,2,3,16},{3,4,15,16},{4,5,13,14,15},{5,6,11,12,13},{6,7,10,11},{7,8,9,10}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'V') {
				set_bounds(-0,-25,100,125);
				final float[][] pointData = {{0,0},{25,0},{50,70},{75,0},{100,0},{60,100},{40,100},
						{10,25},{35,25},{65,25},{90,25}};
				final int [][] polygonData = {{1,2,3,7},{3,6,7},{3,4,5,6},
						{1,2,9,8},{4,5,11,10}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'W') {
				set_bounds(-0,-25,100,125);
				final float[][] pointData = {{0,0},{20,0},{30,60},{40,0},{60,0},{70,60},{80,0},{100,0},
						{80,100},{60,100},{50,45},{40,100},{20,100},
						{5,25},{25,25},{75,25},{95,25}};
				final int [][] polygonData = {{1,2,3,13},{3,12,13},{3,4,11,12},{4,5,11},{5,6,10,11},{6,9,10},{6,7,8,9},
						{1,2,15,14},{7,8,17,16}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			}
			else if (stageLetter == 'X')
			{
				set_bounds(-0,-25,100,130);
				final float[][] pointData = {{0,0},{30,0},{50,35},{70,0},{100,0},{70,50},
						{100,100},{70,100},{50,65},{30,100},{0,100},{30,50},
						{10,20},{40,20},{60,20},{90,20},
						{10,80},{40,80},{60,80},{90,80}};
				final int [][] polygonData ={{1,2,3,12},{3,4,5,6},{6,7,8,9},{9,10,11,12},{3,6,9,12},
						{1,2,14,13},{4,5,16,15},{8,7,20,19},{11,10,18,17}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			}
			else if (stageLetter == 'Y')
			{
				set_bounds(-0,-25,100,130);
				final float[][] pointData = {{0,0},{30,0},{50,40},{70,0},{100,0},{65,65},
						{65,100},{35,100},{35,65},
						{10,25},{40,25},{60,25},{90,25},{35,75},{65,75}};
				final int [][] polygonData = {{1,2,3,9},{3,4,5,6},{3,6,7,8,9},
						{1,2,11,10},{4,5,13,12},{8,7,15,14}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			}
			else if (stageLetter == 'Z')
			{
				set_bounds(-0,-25,100,125);
				final float[][] pointData = {{0,0},{60,0},{100,0},{100,20},{40,80},{100,80},
						{100,100},{40,100},{0,100},{0,80},{60,20},{0,20},
						{30,0},{30,20},{70,80},{70,100}};
				final int [][] polygonData = {{1,2,11,12},{2,3,4,11},{4,5,10,11},{5,8,9,10},{5,6,7,8},
						{1,13,14,12},{6,7,16,15}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'a') {
				set_bounds(0,-55,100,130);
				final float[][] pointData = {{10,10},{50,0},{90,10},{100,50},{100,100},{50,100},{10,90},{0,65},
						{20,40},{60,40},{50,25},{40,35},{0,35},
						{35,65},{50,55},{60,55},{60,65},{45,75}};
				final int [][] polygonData = {{1,2,11,12,13},{2,3,4,10,11},{4,5,17,16},{5,6,18,17},{6,7,8,14,18},{8,9,15,14},{9,10,4,16,15}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'b') {
				set_bounds(-5,-15,100,125);
				final float[][] pointData = {{0,0},{35,0},{35,15},{35,30},{50,30},{90,35},{100,65},
						{90,90},{50,100},{0,100},{0,80},{0,50},{0,30},{0,15},
						{35,50},{50,45},{60,50},{60,80},{50,85},{35,80}};
				final int [][] polygonData = {{1,2,3,14},{3,4,13,14},{4,5,16,15,12,13},{5,6,17,16},{6,7,8,18,17},
						{8,9,19,18},{9,10,11,20,19},{11,12,15,20}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'c') {
				set_bounds(0,-55,100,130);
				final float[][] pointData = {{10,10},{50,0},{90,10},{100,40},{65,40},{50,30},{35,50},{50,70},{65,60},{100,60},{90,90},{50,100},{10,90},{0,50}};
				final int [][] polygonData = {{1,2,3,6},{3,4,5,6},{6,7,14,1},{7,8,13,14},{8,11,12,13},{8,9,10,11}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'd') {
				set_bounds(0,-15,105,125);
				final float[][] pointData = {{65,0},{100,0},{100,15},{100,30},{100,55},{100,75},{100,100},
						{50,100},{10,90},{0,65},{10,35},{50,30},{65,30},{65,15},
						{35,55},{50,45},{65,55},{65,75},{50,85},{35,75}};
				final int [][] polygonData ={{1,2,3,14},{3,4,13,14},{4,5,17,16,12},{5,6,18,17},{6,7,8,19,18},{8,9,20,19},
						{9,10,11,15,20},{11,12,16,15}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			}
			else if (stageLetter == 'e')
			{
				set_bounds(0,-55,100,130);
				final float [][] pointData = {{10,10},{50,0},{90,10},{100,60},{40,60},{55,75},{90,70},{95,90},{50,100},{10,90},{0,50},
						{45,25},{55,25},{65,40},{35,40}};
				final int [][] polygonData = {{1,2,3,13,12},{3,4,14,13},{4,5,11,15,14},{5,6,9,10,11},{6,7,8,9},{11,1,12,15}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'f') {
				set_bounds(0,-20,100,130);
				final float[][] pointData = {{0,30},{20,30},{20,10},{65,0},{100,0},{100,15},{75,20},{75,30},{100,30},{100,45},{75,45},
						{75,100},{25,100},{25,45},{0,45},
						{25,70},{75,70}};
				final int [][] polygonData = {{1,2,14,15},{2,3,4,7,8},{4,5,6,7},{8,9,10,11},{11,12,13,14},{2,8,11,14},{12,13,16,17}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'g') {
				set_bounds(0,-50,105,100);
				final float[][] pointData = {{10,5},{40,0},{100,0},{100,20},{100,50},{100,75},{90,95},
						{50,100},{5,95},{20,75},{50,80},{65,70},{30,70},{10,65},{0,50},{0,25},
						{35,25},{45,15},{65,20},{65,50},{45,55},{35,45}};
				final int [][] polygonData = {{2,3,4,19,18},{4,5,20,19},{5,6,12,20},{6,7,8,11,12},{8,9,10,11},{12,13,21,20},
						{13,14,15,22,21},{15,16,17,22},{16,1,2,18,17}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'h') {
				set_bounds(-5,-20,105,130);
				final float[][] pointData = {{0,0},{35,0},{35,15},{35,30},{50,30},{85,30},{100,50},{100,80},{100,100},{65,100},{65,80},
						{65,50},{50,45},{35,50},{35,80},{35,100},{0,100},{0,80},{0,50},{0,30},{0,15}};
				final int [][] polygonData = {{1,2,3,21},{3,4,20,21},{4,5,13,14,19,20},{5,6,7,12,13},{7,8,11,12},{8,9,10,11},{14,15,18,19},{15,16,17,18}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'i') {
				set_bounds(-20,-20,120,130);
				final float[][] pointData = {{0,0},{100,0},{100,15},{0,15},{0,30},{100,30},{100,100},{0,100},
						{100,50},{0,50},{0,80},{100,80}};
				final int [][] polygonData = {{1,2,3,4},{5,6,7,8},{5,6,9,10},{11,12,7,8}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'j') {
				set_bounds(10,-15,125,100);
				final float [][] pointData = {{35,0},{100,0},{100,85},{70,100},{0,100},
						{0,85},{35,80},
						{100,15},{35,15},{35,25},{100,25},
						{35,40},{100,40}};
				final int [][] polygonData = {{1,2,8,9},{10,11,13,12},{12,13,3,4,7},{4,5,6,7}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'k') {
				set_bounds(-10,-20,95,130);
				final float[][] pointData = {{0,0},{30,0},{30,20},{30,55},{55,30},{95,30},{60,65},
						{100,100},{60,100},{30,75},{30,85},{30,100},{0,100},{0,85},{0,75},{0,55},{0,20},
						{40,45},{75,50},{40,85},{80,80}};
				final int [][] polygonData = {{1,2,3,17},{3,4,16,17},{4,7,10,15,16},{4,5,6,7},{7,8,9,10},{10,12,13,15},{11,12,13,14},
						{5,6,19,18},{9,8,21,20}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'l') {
				set_bounds(-25,-15,120,125);
				final float[][] pointData = {{0,0},{100,0},{100,25},{100,75},{100,100},{0,100},{0,75},{0,25}};
				final int [][] polygonData = {{1,2,3,8},{3,4,7,8},{4,5,6,7}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'm') {
				set_bounds(-5,-65,105,140);
				final float[][] pointData = {{0,0},{30,0},{70,0},{90,0},{100,20},{100,70},{100,100},
						{80,100},{80,70},{80,30},{70,25},{60,30},{60,70},{60,100},{40,100},{40,70},
						{40,30},{30,25},{20,30},{20,70},{20,100},{0,100},{0,70},{0,30}};
				final int [][] polygonData = {{1,2,18,19,24},{2,3,11,12,17,18},{3,4,5,10,11},{5,6,9,10},{6,7,8,9},{12,13,16,17},{13,14,15,16},{19,20,23,24},{20,21,22,23}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'n') {
				set_bounds(-10,-65,105,140);
				final float[][] pointData = {{0,0},{50,0},{70,0},{90,10},{100,30},{100,70},{100,100},{65,100},{65,70},{65,30},{50,25},{35,30},
						{35,70},{35,100},{0,100},{0,70},{0,30}};
				final int [][] polygonData = {{1,2,11,12,17},{2,3,4,5,10,11},{5,6,9,10},{6,7,8,9},{12,13,16,17},{13,14,15,16}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'o') {
				set_bounds(0,-55,100,130);
				final float[][] pointData = {{50,0},{90,10},{100,50},{90,90},{50,100},{10,90},{0,50},{10,10},
						{35,30},{65,30},{65,70},{35,70}};
				final int [][] polygonData = {{1,2,10,9,8},{2,3,4,11,10},{4,5,6,12,11},{6,7,8,9,12}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'p') {
				set_bounds(-5,-50,105,105);
				final float[][] pointData = {{0,0},{50,0},{70,0},{100,20},{100,60},{60,80},{35,70},
						{35,80},{35,100},{0,100},{0,80},{0,55},{0,25},
						{35,25},{50,20},{65,25},{65,55},{35,55}};
				final int [][] polygonData = {{1,2,15,14,13},{2,3,4,16,15},{4,5,17,16},{5,6,7,18,17},{7,8,11,12,18},{8,9,10,11},
						{12,13,14,18}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'q') {
				set_bounds(105,-50,-5,105);
				final float[][] pointData = {{0,0},{50,0},{70,0},{100,20},{100,60},{60,80},{35,70},
						{35,80},{35,100},{0,100},{0,80},{0,55},{0,25},
						{35,25},{50,20},{65,25},{65,55},{35,55}};
				final int [][] polygonData = {{1,2,15,14,13},{2,3,4,16,15},{4,5,17,16},{5,6,7,18,17},{7,8,11,12,18},{8,9,10,11},
						{12,13,14,18}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'r') {
				set_bounds(-10,-65,100,140);
				final float[][] pointData = {{0,0},{45,0},{100,0},{100,25},{55,35},{55,70},{55,100},{0,100},{0,70}};
				final int [][] polygonData = {{1,2,5,6,9},{2,3,4,5},{6,7,8,9}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 's') {
				set_bounds(-5,-60,105,135);
				final float[][] pointData = {{30,0},{70,0},{100,20},{100,35},{65,35},{50,25},{35,35},{70,40},{100,60},{100,80},
						{70,100},{30,100},{0,80},{0,65},{35,65},{50,75},{65,65},{30,60},{0,40},{0,20}};
				final int [][] polygonData = {{1,2,6,7,19,20},{2,3,4,5,6},{7,8,9,17,18,19},{9,10,11,12,16,17},{12,13,14,15,16}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 't') {
				set_bounds(0,-35,100,130);
				final float[][] pointData = {{20,0},{70,0},{70,20},{100,20},{100,40},{70,40},{70,70},{100,70},
						{100,100},{50,100},{30,95},{20,70},{20,40},{0,40},{0,20},{20,20}};
				final int [][] polygonData = {{1,2,3,16},{3,4,5,6},{6,7,10,11,12,13},{7,8,9,10},{13,14,15,16},{3,6,13,16}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'u') {
				set_bounds(-5,-65,105,140);
				final float[][] pointData = {{0,0},{35,0},{35,25},{35,70},{50,80},{65,70},{65,25},{65,0},{100,0},
						{100,25},{100,100},{30,100},{10,90},{0,70},{0,25}};
				final int [][] polygonData = {{1,2,3,15},{3,4,14,15},{4,5,12,13,14},{5,6,11,12},{6,7,10,11},{7,8,9,10}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			}
			else if (stageLetter == 'v')
			{
				set_bounds(5,-65,95,140);
				final float[][] pointData = {{0,0},{35,0},{50,70},{65,0},{100,0},{60,100},{40,100},
						{10,30},{40,30},{60,30},{90,30}};
				final int [][] polygonData = {{1,2,3,7},{3,6,7},{3,4,5,6},{1,2,9,8},{4,5,11,10}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'w') {
				set_bounds(0,-65,100,140);
				final float[][] pointData = {{0,0},{20,0},{30,60},{40,0},{60,0},{70,60},{80,0},{100,0},{80,100},{60,100},{50,40},{40,100},{20,100},
						{5,30},{25,30},{75,30},{95,30}};
				final int [][] polygonData = {{1,2,3,13},{3,12,13},{3,4,11,12},{4,5,11},{5,6,10,11},{6,9,10},
						{6,7,8,9},{1,2,15,14},{7,8,17,16}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			}
			else if (stageLetter == 'x')
			{
				set_bounds(5,-65,95,140);
				final float[][] pointData = {{0,0},{35,0},{50,30},{65,0},{100,0},{70,50},
						{100,100},{65,100},{50,70},{35,100},{0,100},{30,50},
						{10,20},{45,20},{55,20},{90,20},{10,80},{45,80},{55,80},{90,80}};
				final int [][] polygonData = {{1,2,3,12},{3,4,5,6},{6,7,8,9},{9,10,11,12},{3,6,9,12},
						{1,2,14,13},{4,5,16,15},{7,8,19,20},{10,11,17,18}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'y') {
				set_bounds(5,-50,95,100);
				final float[][] pointData = {{0,0},{35,0},{50,45},{65,0},{100,0},{65,70},{50,100},{10,95},{15,80},{35,70},
						{15,30},{45,25},{55,25},{85,30}};
				final int [][] polygonData = {{1,2,3,10},{3,4,5,6},{3,6,7,10},{7,8,9,10},{1,2,12,11},{4,5,14,13}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			} else if (stageLetter == 'z') {
				set_bounds(0,-65,100,140);
				final float[][] pointData = {{0,0},{25,0},{55,0},{100,0},{100,20},{45,75},{75,75},{100,75},
						{100,100},{75,100},{45,100},{0,100},{0,80},{55,25},{25,25},{0,25}};
				final int [][] polygonData = {{1,2,15,16},{2,3,14,15},{3,4,5,14},{5,6,13,14},{6,11,12,13},{6,7,10,11},{7,8,9,10}};
				polygon_array = constructorHelper2(constructorHelper1(pointData),polygonData);
			}
	
		}
		
		// This just make writing the data for the letters a bit easier
		// The array verticeData needs to hold a length 2 array holding the data of the point
		private PointF[] constructorHelper1(float[][] verticeData) {
			PointF[] allPoints = new PointF[verticeData.length];
			// for each of the points described in the 
			for (int i = 0; i < verticeData.length;i++) {
				if (verticeData[i].length == 2) {
					// this is where I scale the points from a relative scale
					float scaled_pointX = (verticeData[i][0]-old_upperLeftX)/(old_upperLeftX-old_lowerRightX);
					float scaled_pointY = (verticeData[i][1]-old_upperLeftY)/(old_upperLeftY-old_lowerRightY);
					// I scale the points to the size of the new scale here
					allPoints[i] = new PointF((upperLeftX-lowerRightX)*scaled_pointX+upperLeftX,(upperLeftY-lowerRightY)*scaled_pointY+upperLeftY);
				}
				else
				{
					allPoints[i] = new PointF(0,0);
				}
			}
			return allPoints;
		}
		
		// Another helper
		// The first argument is the points used for the triangles.
		// The second argument is the indices of the points used to make up the triangles.
		private SpecialPolygon[] constructorHelper2(PointF [] pointArray, int[][] polygonVertices) {
			SpecialPolygon[] allPolygons = new SpecialPolygon[polygonVertices.length];
			PointF dummyPoints[] = new PointF[]{new PointF(0,0),new PointF(0,0),new PointF(0,0)};
			// for each of the triangles described in the triangleVertices array
			for (int i = 0; i < polygonVertices.length; i++)
			{
				if (polygonVertices[i].length >= 3)
				{
					PointF [] current_vertices = new PointF[polygonVertices[i].length];
					for (int j = 0; j < polygonVertices[i].length; j++)
					{
						current_vertices[j] = pointArray[polygonVertices[i][j]-1];
					}
					allPolygons[i] = new SpecialPolygon(current_vertices);
				}
				else
				{
					allPolygons[i] = new SpecialPolygon(dummyPoints);
				}
			}
			return allPolygons;
		}

		// this checks to see whether all the polygons are touched
		public boolean is_complete() {
			for (int i=0; i< polygon_array.length; i++)
			{
				if  (polygon_array[i].was_touched == false) return false;
			}
			return true;
		}

		
		public boolean touch(PointF t_point) {

			
			boolean single_touch = false;
			// in this case, I'll need to check every polygon even if one of them returns true
			// this is in the case that multiple polygons overlap
			for (int i=0; i< polygon_array.length; i++)
			{
				if  (polygon_array[i].touch(t_point) == true) single_touch = true;
			}
			return single_touch;
			
		}
		
		/**
		 * This function will outline the polygons in red lines.
		 * This is used to visually make sure that everything aligns
		 * and help with debugging if it does not.
		 */
		public void debugDraw() {
			for (SpecialPolygon current_polygon: polygon_array) {
				current_polygon.draw();
			}
		}
		

	}
	
	/**
	 * This is an inner class which describes a polygon. One simplification is that
	 * these polygons must always be concave so that we may break them down into
	 * component triangles more easily.
	 * @author darren.sue
	 *
	 */
	private class SpecialPolygon {
		private SpecialTriangle[] componentTriangles;
		private PointF[] vertex_array;
		public boolean was_touched = false;
		
		
		SpecialPolygon(PointF[] n_vertex_array) {
			vertex_array = n_vertex_array;
			componentTriangles = new SpecialTriangle[n_vertex_array.length - 2];
			for (int i = 0; i < n_vertex_array.length-2; i++) {
				componentTriangles[i] = new SpecialTriangle(n_vertex_array[0],n_vertex_array[i+1],n_vertex_array[i+2]);
			}	
		}
		
		public void draw() {
			rPath.reset();
			PointF tempPoint = vertex_array[vertex_array.length-1];
			rPath.moveTo(tempPoint.x,tempPoint.y);
			for (int i = 0; i < vertex_array.length ; i++)
			{
				tempPoint = vertex_array[i];
				rPath.lineTo(tempPoint.x,tempPoint.y);
			}
			mCanvas.drawPath(rPath, debugDraw);
			rPath.reset();
			invalidate();
		}
		
		/**
		 * Compares the coordinates of the argument point and the boundaries
		 * of the polygon marks the latter if the former is inside of it.
		 * Returns a boolean as well if the argument point is inside of
		 * the polygon.
		 * @param t_point
		 * @return
		 */
		public boolean touch(PointF t_point) {
			
			boolean any_triangle = false;
			for (int i = 0 ; i < componentTriangles.length ; i++) {
				if (componentTriangles[i].touch(t_point)) any_triangle = true;
				was_touched = true;
			}
			return any_triangle;
			
		}
		
	}

	/**
	 * These are the component triangle objects that make up the polygon object.
	 * @author darren.sue
	 *
	 */
	private class SpecialTriangle {
		private PointF vertex_one;
		private PointF vertex_two;
		private PointF vertex_three;

		SpecialTriangle(PointF n_vertex_one, PointF n_vertex_two, PointF n_vertex_three)
		{
			vertex_one = n_vertex_one;
			vertex_two = n_vertex_two;
			vertex_three = n_vertex_three;
		}
		

		/**
		 * Determines whether the given point lies within the three vertices of the
		 * triangle by converting the input point to the traingle's vector coordinates.
		 * @param t_point
		 * @return
		 */
		public boolean touch(PointF t_point) {
			PointF vectort = subtract(t_point,vertex_one);
			PointF vector1 = subtract(vertex_two,vertex_one);
			PointF vector2 = subtract(vertex_three,vertex_one);
			double u = (dotProduct(vector1,vector1)*dotProduct(vectort,vector2)
					-dotProduct(vector1,vector2)*dotProduct(vectort,vector1))
					/(dotProduct(vector2,vector2)*dotProduct(vector1,vector1)
							-dotProduct(vector2,vector1)*dotProduct(vector1,vector2));
			double v = (dotProduct(vector2,vector2)*dotProduct(vectort,vector1)
					-dotProduct(vector2,vector1)*dotProduct(vectort,vector2))
					/(dotProduct(vector2,vector2)*dotProduct(vector1,vector1)
							-dotProduct(vector2,vector1)*dotProduct(vector1,vector2));
			return (u>=0 && v>= 0 && u+v<=1);

		}

		private PointF subtract(PointF point1, PointF point2) {
			return new PointF(point1.x-point2.x,point1.y-point2.y);
		}

		private float dotProduct(PointF point1, PointF point2) {
			return point1.x*point2.x+point1.y*point2.y;
		}

	}
	
	OnFinishStageListener onFinishStageListener = null;
	public void setOnFinishStageListener(OnFinishStageListener listener) {
		onFinishStageListener = listener;
	}
	
	public interface OnFinishStageListener {
		public abstract void onFinishChecked();
	}
	
	OnFinishSetupListener onFinishSetupListener = null;
	public void setOnFinishSetupListener(OnFinishSetupListener listener) {
		onFinishSetupListener = listener;
	}
	
	public int getViewHeight() {
		return viewHeight;
	}


	public void setViewHeight(int viewHeight) {
		this.viewHeight = viewHeight;
	}

	public int getViewWidth() {
		return viewWidth;
	}


	public void setViewWidth(int viewWidth) {
		this.viewWidth = viewWidth;
	}

	public interface OnFinishSetupListener {
		public abstract void onSetupChecked();
	}


}
