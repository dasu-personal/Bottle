package com.example.bottle;

import com.example.bottle.R;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;

/**
 * This is a very simple activity to show a black screen as
 * a title / splash screen before transitioning to the main
 * activity after a few seconds.
 * @author darren.sue
 *
 */
public class SplashScreen extends Activity {

	MediaPlayer current_music;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
		new Handler().postDelayed(new Runnable(){
			  public void run()
			  {
				  if (current_music != null) current_music.release();
					
			        Intent a = new Intent(SplashScreen.this, MainActivity.class);
			        a.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			        startActivity(a);
			        SplashScreen.this.finish();
			  }
			}
			, 6000);
		playChord();

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
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    @Override
    protected void onPause(){
       super.onPause();
       if (current_music != null) current_music.release();
    }
    

    
    @Override
    public void onStart()
    {
    	super.onStart();
    }
    
    
}
