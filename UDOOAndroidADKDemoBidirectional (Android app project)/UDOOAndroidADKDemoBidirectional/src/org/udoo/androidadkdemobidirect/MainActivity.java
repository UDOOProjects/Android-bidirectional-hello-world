package org.udoo.androidadkdemobidirect;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.udoo.androidadkdemobidirect.R;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements Runnable{

	private static final String TAG = "UDOO_AndroidADKFULL";	 
	private static final String ACTION_USB_PERMISSION = "org.udoo.androidadkdemobidirect.action.USB_PERMISSION";
 
	private UsbManager mUsbManager;
	private PendingIntent mPermissionIntent;
	private boolean mPermissionRequestPending;
	
	UsbAccessory mAccessory;
	ParcelFileDescriptor mFileDescriptor;
	FileInputStream mInputStream;
	FileOutputStream mOutputStream;
	
	private ToggleButton buttonLED;
	private static TextView distance;
	private boolean running = false;
	
	// Receive the USB attached intent 
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbAccessory accessory = (UsbAccessory)intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
					if (intent.getBooleanExtra(
						UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						openAccessory(accessory);
					} else {
						Log.d(TAG, "permission denied for accessory "+ accessory);
					}
					mPermissionRequestPending = false;
				}
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				UsbAccessory accessory = (UsbAccessory)intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
				if (accessory != null && accessory.equals(mAccessory)) {
					closeAccessory();
				}
			}
		}
	};
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver, filter);
 
		if (getLastNonConfigurationInstance() != null) {
			mAccessory = (UsbAccessory) getLastNonConfigurationInstance();
			openAccessory(mAccessory);
		}
		
		setContentView(R.layout.activity_main);
		buttonLED = (ToggleButton) findViewById(R.id.toggleButtonLED);
		distance = (TextView) findViewById(R.id.textView_distance);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public Object onRetainNonConfigurationInstance() {
		if (mAccessory != null) {
			return mAccessory;
		} else {
			return super.onRetainNonConfigurationInstance();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
 
		if (mInputStream != null && mOutputStream != null) {
			return;
		}
		//open the accessory from the accessory list
		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
		UsbAccessory accessory = (accessories == null ? null : accessories[0]);
		if (accessory != null) {
			if (mUsbManager.hasPermission(accessory)) {
				openAccessory(accessory);
			} else {
				synchronized (mUsbReceiver) {
					if (!mPermissionRequestPending) {
						mUsbManager.requestPermission(accessory,mPermissionIntent);
						mPermissionRequestPending = true;
					}
				}
			}
		} else {
			Log.d(TAG, "mAccessory is null");
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();	
	}
 
	@Override
	public void onDestroy() {
		closeAccessory();
		unregisterReceiver(mUsbReceiver);
		super.onDestroy();
	}
	
	// open the accessory and open the input and output stream from the descriptor
	// start also the thread that reads from Arduino
	private void openAccessory(UsbAccessory accessory) {
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		if (mFileDescriptor != null) {
			mAccessory = accessory;
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			
			Thread thread = new Thread(null, this, "UDOO_ADK_readfrom");
			thread.start();
			
			Toast.makeText(getApplicationContext(), "Accessory connected", Toast.LENGTH_SHORT).show();
			Log.i(TAG, "openaccessory");
		} 
		else {
			Toast.makeText(getApplicationContext(), "Accessory not connected", Toast.LENGTH_SHORT).show();
		}
	}
	
	// close the accessory
	private void closeAccessory() {
		Log.i(TAG, "closeaccessory");
		try {
			if (mFileDescriptor != null) {
				mFileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			mFileDescriptor = null;
			mAccessory = null;
			running = false;
		}
	}
	
	// ToggleButton method - send message to SAM3X
	public void blinkLED(View v){
		if (mAccessory != null) {
			byte[] message = new byte[1];
			if (buttonLED.isChecked()) { 
				message[0] = (byte)1;
			} else {
				message[0] = (byte)0; 
			}	
			if (mOutputStream != null) {
				try {
					mOutputStream.write(message);
				} catch (IOException e) {
					Log.e(TAG, "write failed", e);
				}
			}
		} else {
			Toast.makeText(getApplicationContext(),
		               "Accessory not connected", 
		               Toast.LENGTH_SHORT).show();
		}
	}
	 
	
	private Message m;
	
	// Thread for read data from SAM3X
	public void run() {
		int ret = 0;
		byte[] buffer = new byte[4];
		running = true;
		
		while (running) {
			try {
				ret = mInputStream.read(buffer);
			} catch (IOException e) {
				break;
			}
			m = Message.obtain(mHandler);
			
			if (ret != 0) {
				m.arg1 = unsignedByteToInt(buffer[0]);
				ret = 0;
			}			
			mHandler.sendMessage(m);		
		}
	}
	
	private int unsignedByteToInt(byte b){
		return b & 0xFF;
	}
	
	static Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			distance.setText(msg.arg1 + " cm ");			
		}
	};

}
