package org.udoo.androidadkdemobidirect;

import me.palazzetti.adktoolkit.AdkManager;
import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends Activity{

//	private static final String TAG = "UDOO_AndroidADKFULL";	 
	
	private AdkManager mAdkManager;
	
	private ToggleButton buttonLED;
	private TextView distance;
	
	private AdkReadTask mAdkReadTask;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mAdkManager = new AdkManager((UsbManager) getSystemService(Context.USB_SERVICE));
		
//		register a BroadcastReceiver to catch UsbManager.ACTION_USB_ACCESSORY_DETACHED action
		registerReceiver(mAdkManager.getUsbReceiver(), mAdkManager.getDetachedFilter());
		
		buttonLED = (ToggleButton) findViewById(R.id.toggleButtonLED);
		distance  = (TextView) findViewById(R.id.textView_distance);
		
	}

	@Override
	public void onResume() {
		super.onResume();
		mAdkManager.open();
		
		mAdkReadTask = new AdkReadTask();
		mAdkReadTask.execute();
	}
	
	@Override
	public void onPause() {
		super.onPause();	
		mAdkManager.close();
		
		mAdkReadTask.pause();
		mAdkReadTask = null;
	}
 
	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mAdkManager.getUsbReceiver());
	}
	
	// ToggleButton method - send message to SAM3X
	public void blinkLED(View v){
		if (buttonLED.isChecked()) { 
			// writeSerial() allows you to write a single char or a String object.
			mAdkManager.writeSerial("1");
		} else {
			mAdkManager.writeSerial("0"); 
		}
	}
	
	/* 
	 * We put the readSerial() method in an AsyncTask to run the 
	 * continuous read task out of the UI main thread
	 */
	private class AdkReadTask extends AsyncTask<Void, String, Void> {

		private boolean running = true;
			
		public void pause(){
			running = false;
		}
		 
	    protected Void doInBackground(Void... params) {
//	    	Log.i("ADK demo bi", "start adkreadtask");
	    	while(running) {
	    		publishProgress(mAdkManager.readSerial()) ;
	     	}
	    	return null;
	    }

	    protected void onProgressUpdate(String... progress) {
	    	distance.setText((int)progress[0].charAt(0) + " cm");
//	    	Log.i(TAG, "received: " + (int)progress[0].charAt(0));
	    }  
	}

}
