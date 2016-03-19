package com.example.bottle;
import com.example.bottle.DrawView.OnFinishStageListener;
import com.example.bottle.R;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Typeface;

public class MainActivity extends Activity {
	LanguageContent currentStage;
	int letterIter;
	DrawView paintView;
	TextView background_text;
	TextView conclusion_text;
	TextView header_text_back;
	TextView conclusion_background; // to make it consistant with everything else...
	RelativeLayout main_view;
	String next_letters[];
	float paint_width;
	float paint_height;
	MediaPlayer current_music;
	Handler waitAnimation;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		// I don't actually need to initialize the text values,
		// but I will anyways
		highlight_text = "";
		header_text_back = (TextView) findViewById(R.id.headTextBack);
		header_text_back.setText("");
		header_text_back.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
		conclusion_text = (TextView) findViewById(R.id.conclusionText);
		//conclusion_text.setText("");
		conclusion_background = (TextView) findViewById(R.id.conclusionBackground);
		conclusion_text.setVisibility(View.INVISIBLE);
		conclusion_background.setVisibility(View.INVISIBLE);
		background_text = (TextView) findViewById(R.id.imageText);
		background_text.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
		background_text.setText("");
	
		paintView = (DrawView) findViewById(R.id.paintView);
		currentStage = new LanguageContent();
		currentStage.resetContent();
		next_letters = new String[]{"",""};
		
		
		
		//Display display = getWindowManager().getDefaultDisplay();
		
		paint_width = (float) paintView.viewWidth;
		paint_height = (float) paintView.viewHeight;
		

		// this will hopefully set the listener
		paintView.setOnFinishStageListener(new OnFinishStageListener()
		{
			@Override
			public void onFinishChecked()
			{
				nextLetter();
			}
		});
		
		myFirstSoundPool = new SoundPool(1,AudioManager.STREAM_MUSIC, 0);
		dragSoundIndex = myFirstSoundPool.load(getApplicationContext(), R.raw.noise_sound, 1);
		
		paintView.setSounds(myFirstSoundPool,dragSoundIndex);
		paintView.dragSound = myFirstSoundPool;
		paintView.dragSoundIndex = dragSoundIndex;
		
		waitAnimation = new Handler();
		}
	SoundPool myFirstSoundPool;
	int dragSoundIndex;
	
	/*
	@Override
	public void onBackPressed() {
	    // Do your stuff....
		finish();
	}
	*/
	
	@Override
	protected void onStart()
	{
		super.onStart();
		
	}
	

	
    @Override
    protected void onPause(){
       super.onPause();
       if (current_music != null) current_music.release();
    }
	
	@Override
	protected void onStop()
	{	
		super.onStop();
        finish();
	}
	
	private void playChord()
	{
		if (current_music != null) current_music.release();
		int new_chord = R.raw.chord01;
		current_music = MediaPlayer.create(this, new_chord);
		current_music.start(); // no need to call prepare(); create() does that for you
	}
	
	private void playEndChord()
	{
		if (current_music != null) current_music.release();
		int new_chord = R.raw.chord08;
		current_music = MediaPlayer.create(this, new_chord);
		current_music.start(); // no need to call prepare(); create() does that for you
	}
	
	// Called when you want a completely new stage
	private void resetStage()
	{
		
		// resets current stage

		// probably needs to completely destroy old view here
		// actually, maybe not test first and see for yourself...
		conclusion_text.setVisibility(View.INVISIBLE);
		conclusion_background.setVisibility(View.INVISIBLE);
		highlight_text = "";
		next_letters = new String[]{"",""};
		currentStage.resetContent();
		
		
		header_text_back.setText(currentStage.currentStageWords);
		header_text_back.setTextColor(0xffcccccc);

		
		nextLetter2();
	
		
	}
	
	
	private void concludeStageResponse()
	{
		// this is where we decide what the response text ought to be set as
		conclusion_text.setText(currentStage.currentConclusionWords);
		
		conclusion_background.setVisibility(View.VISIBLE);
		conclusion_background.bringToFront();
		// this is where the response animation does its thing.
		
		//conclusion_text.setVisibility(View.INVISIBLE);
		waitAnimation.postDelayed(new Runnable(){
			public void run()
			{
				Animation response_text_animation = AnimationUtils.loadAnimation(getApplicationContext(),
						R.anim.conclude_show_animation);
			conclusion_text.setVisibility(View.VISIBLE);
			conclusion_text.bringToFront();
			conclusion_text.startAnimation(response_text_animation);

			}
		}, 500);

		Animation response_background_animation = AnimationUtils.loadAnimation(getApplicationContext(),
				R.anim.conclude_show_animation);
		conclusion_background.startAnimation(response_background_animation);
		
		waitAnimation.postDelayed(new Runnable(){
			  public void run()
			  {
				conclusion_text.clearAnimation();
				conclusion_background.clearAnimation();
			    resetStage();
			  }
			}

			, 6000);
		
		waitAnimation.postDelayed(new Runnable(){
			  public void run()
			  {
				playEndChord();
			  }
			}

			, 4000);
		/*
		new Handler().postDelayed(new Runnable(){
			public void run()
			{
				Animation response_text_animation = AnimationUtils.loadAnimation(getApplicationContext(),
						R.anim.conclude_show_animation);
			conclusion_text.setVisibility(View.VISIBLE);
			conclusion_text.bringToFront();
			conclusion_text.startAnimation(response_text_animation);

			}
		}, 500);

		Animation response_background_animation = AnimationUtils.loadAnimation(getApplicationContext(),
				R.anim.conclude_show_animation);
		conclusion_background.startAnimation(response_background_animation);
		
		new Handler().postDelayed(new Runnable(){
			  public void run()
			  {
				conclusion_text.clearAnimation();
				conclusion_background.clearAnimation();
			    resetStage();
			  }
			}

			, 6000);
		
		new Handler().postDelayed(new Runnable(){
			  public void run()
			  {
				playEndChord();
			  }
			}

			, 4000);
			*/
	}
	
	private void concludeStage() {
		playChord();

		// Create an animation that
		//Spannable WordtoSpan = new SpannableString(currentStage.currentStageWords);        
		//WordtoSpan.setSpan(new ForegroundColorSpan(0xEE000000), 0, highlight_length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		header_text_back.setText(currentStage.currentStageWords);
		header_text_back.setTextColor(0xEE000000);
		AlphaAnimation anim = new AlphaAnimation(0.5f, 1.0f);
		anim.setDuration(300);
		header_text_back.startAnimation(anim);
		// only show conclusion 1/3 of the time
		// TODO change this back
		if (2.0 < Math.random() * 3) {
			// what if we want to show the conclusion text
			concludeStageResponse();
		} else {
			// what if we want to simply pause and go to the next stage?
			new Handler().postDelayed(new Runnable(){
				  public void run()
				  {
				    resetStage();
				  }
				}
				, 3000);
		}
	}
	
	// TODO fix this
	@SuppressLint("NewApi")
	private void nextLetter2(){
		// Remember, the first of the next letters is the junk
		// the second of the letters is the actual content
		next_letters = currentStage.next();
		
	
		
		//final float initialSize = background_text.getTextSize();
		float initialSize = 100;
		//Rect text_measure = new Rect();
		//Paint temp_paint = new Paint();
		//temp_paint.setTextAlign(Align.LEFT);
		//temp_paint.setTypeface(Typeface.DEFAULT_BOLD);
		//temp_paint.setTextSize(100);
		//temp_paint.getTextBounds(next_letters[1], 0, 1, text_measure);
		
		
		// destroy and create the background text due to the fact that
		// the text box resizing doesn't always work as I want it to
		RelativeLayout temp_layout =(RelativeLayout)findViewById(R.id.mainView);
		//background_text.clearAnimation();
		temp_layout.removeView(background_text);
		temp_layout.forceLayout();
		// I may have to remake the whole background text thing here
		background_text = new TextView(MainActivity.this);
		background_text.setTextSize(TypedValue.COMPLEX_UNIT_PX, initialSize);
		background_text.setTextColor(0xBB000000);
		//background_text.setAlpha((float) 0.3);
		background_text.setAlpha((float) 0.2);
		background_text.setText(next_letters[1]);
		background_text.setLayoutParams(new TableRow.LayoutParams(
	            LayoutParams.FILL_PARENT,
	            LayoutParams.FILL_PARENT));
		background_text.setGravity(Gravity.CENTER);
		background_text.setIncludeFontPadding(false);
		background_text.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
		// I may have to remake the whole background text thing here
		//background_text.setBackgroundColor(Color.BLACK);
		
		temp_layout.addView(background_text);
		background_text.requestLayout();
		temp_layout.requestLayout();
		//header_text_back.bringToFront();		
		paintView.bringToFront();
		background_text.setVisibility(View.VISIBLE);
		background_text.requestLayout();
		background_text.measure(0,0);
		background_text.setTextSize(100);
		background_text.requestLayout();
		paint_width = (float) paintView.viewWidth;
		paint_height = (float) paintView.viewHeight;		
		
		
		float text_width = (float) background_text.getMeasuredWidth();
		//float text_height = (float) (((float) text_measure.height() )* 2.3);
		float text_height = (float) background_text.getMeasuredHeight();
		
		
		// Scale the text so that it fits well the view
		float heightFactor = (float) paint_height/text_height;
		float widthFactor = (float) paint_width/text_width;
		float scale_factor;
		
		
		if (heightFactor<widthFactor) scale_factor = heightFactor;
		else scale_factor = widthFactor;
		//scale_factor = widthFactor;
		background_text.setTextSize(TypedValue.COMPLEX_UNIT_PX, scale_factor*initialSize);
		float n_text_width = text_width * scale_factor;
		float n_text_height = text_height * scale_factor;
		
		// figure out the upper and lower corners of the text
		float upper_left_x = (paint_width-n_text_width)/2;
		float upper_left_y = (paint_height-n_text_height)/2;
		float lower_right_x = (paint_width+n_text_width)/2;
		float lower_right_y = (paint_height+n_text_height)/2;
		
		// somehow needs to scale the paintview object
		paintView.clearAnimation();
		paintView.isActive = true;
		paintView.setVisibility(View.VISIBLE);
		paintView.reset(next_letters[1].charAt(0),upper_left_x,upper_left_y,lower_right_x,lower_right_y);
		
	}
	String highlight_text;
	private void nextLetter()
	{
		//Animation drawing_animation = AnimationUtils.loadAnimation(getApplicationContext(),
		//		R.anim.vanish_animation);

		//Animation letter_animation  = AnimationUtils.loadAnimation(getApplicationContext(),
        //        R.anim.letter_complete_animation);
	
		new Handler().postDelayed(new Runnable(){
			  public void run()
			  {
				  highlight_text = highlight_text + next_letters[0] + next_letters[1];
				  int highlight_length;
					if (currentStage.hasNext()) highlight_length = highlight_text.length();
					else highlight_length = header_text_back.getText().length();				  
					Spannable WordtoSpan = new SpannableString(currentStage.currentStageWords);        
					WordtoSpan.setSpan(new ForegroundColorSpan(0x88000000), 0, highlight_length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					header_text_back.setText(WordtoSpan);
					background_text.setText("");
					paintView.setVisibility(View.INVISIBLE);
					if (currentStage.hasNext())
					{
						
						nextLetter2();
					}
					else
					{
						concludeStage();
					}
			  }
			}
			, 200);
		//paintView.startAnimation(drawing_animation);
		paintView.isActive = false;
		//background_text.setAlpha(1.0f);
		//background_text.startAnimation(letter_animation);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
