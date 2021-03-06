package com.example.nlogdemo;

import java.util.Map;

import com.baidu.nlog.NLog;
import com.example.nlogdemo.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class FullscreenActivity extends Activity {
	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean TOGGLE_ON_CLICK = true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;
	
	@Override
	protected void onPause() {
		super.onPause();
		NLog.follow(this);
		reinfo();
	}

	@Override
	protected void onResume() {
		super.onResume();
		NLog.follow(this);
		reinfo();
	}
	
	private String deviceId;
	
	private void reinfo() {
		
		final EditText editText01 = (EditText)findViewById(R.id.EditText01);
		if (editText01 == null) {
			return;
		}
		editText01.setText(
				String.format("deviceId = %s\nsessionId = %s\nsessionSeq = %s",
						deviceId, NLog.getSessionId(), NLog.getSessionSeq()
				)
		);
		
	}
	
	private static Context applicationContext = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		TelephonyManager tm = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
        deviceId = tm.getDeviceId();
        
        if (applicationContext == null) { // 初始化 // 注意onCreate可能被调用多次，这个判断是必须的
        	applicationContext = this.getApplicationContext();
        	NLog.cmd("wenku.send", "event", "a=", 1);
        	NLog.init(this, 
        			"childPackages=", "com.baidu.wenku",
        			"debug=", true,
        			"ruleUrl=", "http://hunter.duapp.com/nlog/demo.rule.js", // 策略文件存放地址
        			"ruleExpires=", "5", // 策略文件失效时间
	    			"onCreateSession=", new NLog.EventListener() { // 重新创建session时触发
						@Override
						public void onHandler(Map<String, Object> map) {
							NLog.cmd("pv.send", "appview");
						}
					},
					"onDestorySession=", new NLog.EventListener() {
						
						@Override
						public void onHandler(Map<String, Object> map) {
							// TODO Auto-generated method stub
							
						}
					},
					"onViewClose=", new NLog.EventListener() {
						
						@Override
						public void onHandler(Map<String, Object> map) {
							// TODO Auto-generated method stub
							
						}
					}
			);
        	
			NLog.cmd("pv.start",
					"postUrl=", "http://hunter.duapp.com/command/?command=nlog-post&channel=demo",
	        		"protocolParameter=", NLog.buildMap( // 字段简写
	        				"ht=", null, // 不传送hitType
	        				"time=", "t" // time -> t 简写
    				),
					"cuid=", deviceId
			);
        }
		NLog.follow(this);

		setContentView(R.layout.activity_fullscreen);

		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		final View contentView = findViewById(R.id.fullscreen_content);
		

		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, contentView,
				HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider
				.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
					// Cached values.
					int mControlsHeight;
					int mShortAnimTime;

					@Override
					@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
					public void onVisibilityChange(boolean visible) {
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
							// If the ViewPropertyAnimator API is available
							// (Honeycomb MR2 and later), use it to animate the
							// in-layout UI controls at the bottom of the
							// screen.
							if (mControlsHeight == 0) {
								mControlsHeight = controlsView.getHeight();
							}
							if (mShortAnimTime == 0) {
								mShortAnimTime = getResources().getInteger(
										android.R.integer.config_shortAnimTime);
							}
							controlsView
									.animate()
									.translationY(visible ? 0 : mControlsHeight)
									.setDuration(mShortAnimTime);
						} else {
							// If the ViewPropertyAnimator APIs aren't
							// available, simply show or hide the in-layout UI
							// controls.
							controlsView.setVisibility(visible ? View.VISIBLE
									: View.GONE);
						}

						if (visible && AUTO_HIDE) {
							// Schedule a hide().
							delayedHide(AUTO_HIDE_DELAY_MILLIS);
						}
					}
				});

		// Set up the user interaction to manually show or hide the system UI.
		contentView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (TOGGLE_ON_CLICK) {
					mSystemUiHider.toggle();
				} else {
					mSystemUiHider.show();
				}
			}
		});

		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		findViewById(R.id.dummy_button).setOnTouchListener(
				mDelayHideTouchListener);
		//*
		findViewById(R.id.total_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent();
				intent.setClass(FullscreenActivity.this, MainActivity.class);

				FullscreenActivity.this.startActivity(intent);
			}
		});
		//*/
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide(100);
	}

	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			if (AUTO_HIDE) {
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
			}
			return false;
		}
	};

	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			mSystemUiHider.hide();
		}
	};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}
}
