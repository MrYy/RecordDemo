package com.example.recorddemo;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;



import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Camera.CameraInfo;
import android.media.CamcorderProfile;
import android.media.CameraProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
@SuppressLint("NewApi")
public  class RecordActivity extends Activity implements SensorEventListener,OnClickListener,SurfaceHolder.Callback {
	private SensorManager sensorManager;
	private TextView showTextView;
	private Sensor gyroscopeSensor;
	private static final float NS2S = 1.0f / 1000000000.0f;
	private float timestamp;
	private float angle[] = new float[3];
	private ProgressBar pb;
	private int status=0;
	private Button btn_start;
	private static String TAG="RecordActivity";
	private Button btn_stop;
	private Ex ex;
	float angley=0;
	float anglez=0;
	float anglex=0;
	boolean flag=false;
	double rAnglex=0;
	double rAngley=0;
	double rAnglez=0;
	Handler handler=new Handler(){
		@Override
		public void handleMessage(Message msg){
			if(msg.what==1){
				pb.setProgress(status);
			}
			if(msg.what==2){
				Toast.makeText(RecordActivity.this,"拍摄完成",Toast.LENGTH_SHORT).show();
			}
		}
	};
	public static final int MEDIA_TYPE_VIDEO = 2;
	private Camera mCamera;
	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;
	private MediaRecorder mMediaRecorder;
	private boolean isReording=false;//是否正在录像
	private Button bt_start;
	private File videoFile;
	private File videoDir;
	private boolean isRecording;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "debug1");
		setContentView(R.layout.activity_record);
		Log.d(TAG, "debug2");
		pb=(ProgressBar)findViewById(R.id.progressBar_record_progress);
		pb.setVisibility(View.VISIBLE);
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		bt_start=(Button) findViewById(R.id.start);
		bt_start.setOnClickListener(this);
		mCamera=getCameraInstance();
		mSurfaceView=(SurfaceView) findViewById(R.id.SurfaceView);
		mSurfaceHolder=mSurfaceView.getHolder();
		mSurfaceHolder.addCallback(this);
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		ex=new Ex();
		ex.init();
		new Thread(new Runnable(){
			@Override
			public void run() {
				while(true){
					if(flag){
						ex.inputData(angley);
						rAngley=ex.filter();
		//				Log.i("估测数据y",Double.toString(rAngley));

					}
				}
				
			}
			
		}).start();
		
	}
	public void init(){
		status=0;
		angle[0]=0;
		angle[1]=0;
		angle[2]=0;
		pb.setProgress(status);
		
	}
	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
			if (timestamp != 0) {
				final float dT = (event.timestamp - timestamp) * NS2S;
				angle[0] += event.values[0] * dT;
				angle[1] += event.values[1] * dT;
				angle[2] += event.values[2] * dT;
				anglex = (float) Math.toDegrees(angle[0]);
				angley= (float) Math.toDegrees(angle[1]);
				anglez = (float) Math.toDegrees(angle[2]);
				flag=true;
			//	tv_angle.setText("x  "+rAnglex+"\n"+"y  "+rAngley+"\n"+"z  "+rAnglez+"\n");
				// Log.d("角度","anglex------------>" + anglex);
			//	Log.d("角度", "angley------------>" + angley);
				
				if(status<100){
					status=Math.abs((int)(((float)rAngley/360)*100));
			//		Log.d("进度",Integer.toString(status));
					handler.sendEmptyMessage(1);
				}else{
					sensorManager.unregisterListener(this);
					handler.sendEmptyMessage(2);
					mMediaRecorder.stop();// stop the recording
					releaseMediaRecorder();// release the MediaRecorder object
					mCamera.lock();// take camera access back from MediaRecorder
					bt_start.setText("Capture");
					isRecording=false;
					flag=false;
					init();
				}
				// Log.d("角度","anglez------------>" + anglez);
			}
			// Log.d("TAMESTAMP",Long.toString(event.timestamp));
			timestamp = event.timestamp;
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		if(holder.getSurface()==null){
			return;
		}
		mCamera.stopPreview();
		try {
			mCamera.setPreviewDisplay(holder);
			setCameraDisplayOrientation(this, 0, mCamera);
			
			mCamera.startPreview();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.d("Surface Changed", e.getMessage());
		}
		
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		
		if(mCamera==null){
			mCamera=getCameraInstance();
		}
		try {
			// The Surface has been created, now tell the camera where to draw the preview.
			mCamera.setPreviewDisplay(holder);
			mCamera.startPreview();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.d("surfaceCreated", "error setting camera preview" + e.getMessage());
		}
		
	}


	private Camera getCameraInstance() {
		// TODO Auto-generated method stub
		Camera c=null;
		try {
			c=Camera.open();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.d("Open Camera", "failed");
		}
		return c;
	}
	
	private boolean prepareVideoRecorder(){
		if (mCamera == null) {
			mCamera=getCameraInstance();
		}
		mMediaRecorder=new MediaRecorder();
		// Step 1: Unlock and set camera to MediaRecorder
		
		mCamera.unlock();
		mMediaRecorder.setCamera(mCamera);
		// Step 2: Set sources
		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		// Step 3: Set a CamcorderProfile (requires API Level 8 or higher instead of setting format and encoding)
		mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
		// Step 4: Set output file
		mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());
		// Step 5: Set the preview output
		mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
		 // Step 6: Prepare configured MediaRecorder
		try {
			mMediaRecorder.prepare();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.d("TAG", "IllegalStateException preparing MediaRecorder: " + e.getMessage());
			releaseMediaRecorder();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.d("TAG", "IOException preparing MediaRecorder: " + e.getMessage());
			e.printStackTrace();
			releaseMediaRecorder();
		}	
		return true;	
	}
	
	@SuppressLint("NewApi")
	public static void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}
		int rotationDegrees;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			rotationDegrees = (info.orientation + degrees) % 360;
			rotationDegrees = (360 - rotationDegrees) % 360; // compensate the mirror
		} else {
			rotationDegrees = (info.orientation - degrees + 360) % 360;
		}
		camera.setDisplayOrientation(rotationDegrees);
	}
	
	private void releaseMediaRecorder(){
		if(mMediaRecorder!=null){
			mMediaRecorder.reset();// clear recorder configuration
			mMediaRecorder.release();// release the recorder object
			mMediaRecorder=null;
			mCamera.lock();// lock camera for later use
		}
	}
	
	private void releaseCamera(){
		if(mCamera!=null){
			mCamera.release();// release the camera for other applications
			mCamera=null;
		}
	}
	private static File getOutputMediaFile(int type){
		File mediaStorageDir=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+"/"+"RecordVideo");
		if(!mediaStorageDir.exists()){
			if(!mediaStorageDir.mkdirs()){
				Log.d("getOutputMediaFile", "failed to create directory");
				return null;
			}
		}
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		File mediaFile;
		if(type == MEDIA_TYPE_VIDEO){
			mediaFile = new File(mediaStorageDir.getPath() + 
					File.separator + "VID_" + timeStamp + ".mp4");
		} else {
			return null;
		}
		return mediaFile;	
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		releaseMediaRecorder();
		releaseCamera();
	}

	
	protected void onPause(){
		super.onPause();
		sensorManager.unregisterListener(this);
		flag=false;
		releaseMediaRecorder();// if you are using MediaRecorder, release it first
		releaseCamera();// release the camera immediately on pause event
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.start:
			if(isRecording){
				// stop recording and release camera
				mMediaRecorder.stop();// stop the recording
				releaseMediaRecorder();// release the MediaRecorder object
				mCamera.lock();// take camera access back from MediaRecorder
				bt_start.setText("Capture");
				isRecording=false;
				sensorManager.unregisterListener(this);
				flag=false;
				init();
			}else{
				
				// initialize video camera
				if(prepareVideoRecorder()){
					mMediaRecorder.start();
					bt_start.setText("Stop");
					isRecording=true;
					init();
					sensorManager.registerListener(this, gyroscopeSensor,
							SensorManager.SENSOR_DELAY_GAME);
				}else{
					releaseMediaRecorder();
				}
			}
		}
		
	}
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

}
