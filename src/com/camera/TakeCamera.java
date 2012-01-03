package com.camera;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

public class TakeCamera extends Activity implements SurfaceHolder.Callback
{
	private String TAG = "TakeCamera";

	private Camera mCamera01;
	
	private Timer timer;
	private TextView tsec;
	private EditText sec;

	private AlertDialog.Builder builder;
	private static final int MENU_EXIT = Menu.FIRST;

	private SurfaceView mSurfaceView01;
	private SurfaceHolder mSurfaceHolder01;
	  
	private boolean bIfPreview = false;
	private String strCaptureFilePath = Environment.getExternalStorageDirectory() + "/";
	
    private static volatile AtomicBoolean processing = new AtomicBoolean(false);

	private int take_sec;
		
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

	    timer = new Timer();

        //Checking Status
        if (!CheckInternet(3))
        {
        	openOptionsDialog("connetion error by Internet");
        }
        else
        {
            //顯示輸入IP的windows
              AlertDialog.Builder alert = new AlertDialog.Builder(this);

              alert.setTitle("input data");
              alert.setMessage("請輸入拍攝秒數(s)");
              ScrollView sv = new ScrollView(this);
              LinearLayout ll = new LinearLayout(this);
              ll.setOrientation(LinearLayout.VERTICAL);
              sv.addView(ll);

              tsec = new TextView(this);
              tsec.setText("sec: ");
              sec = new EditText(this);      
              sec.setText("10");
              ll.addView(tsec);
              ll.addView(sec);

              // Set an EditText view to get user input 
              alert.setView(sv);
              
              alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	              public void onClick(DialogInterface dialog, int whichButton) 
	              {
	            	  take_sec = Integer.valueOf(sec.getText().toString());
	                  timer.schedule(new DateTask(), take_sec * 1000, take_sec * 1000);                        
	              }
              });
              
              alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int whichButton) 
                  {
                	  take_sec = 10;
                      timer.schedule(new DateTask(), 0, take_sec * 1000);                        
                      SwitchCamera();
                  }
               });
            
                alert.show();
           	SwitchCamera();
        }
    }
    
	public boolean onCreateOptionsMenu(Menu menu)
	{
	    super.onCreateOptionsMenu(menu);
	    
	    menu.add(0 , MENU_EXIT, 1 , "Exit").setIcon(R.drawable.exit)
	    .setAlphabeticShortcut('E');
	    
	    return true;  
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		    switch (item.getItemId())
		    { 		      
		          case MENU_EXIT:
		        	  finish();
		          break ;
		    }
		      return true ;
	}

    
    public void SwitchCamera()
    {
		 if(!checkSDCard())
		 {
			 openOptionsDialog("no SDCard");
		 }
		 else
		 {
		     Log.i(TAG,"SwtichCamera");

		     mSurfaceView01 = (SurfaceView) findViewById(R.id.mSurfaceView1);
			  mSurfaceHolder01 = mSurfaceView01.getHolder();
			  mSurfaceHolder01.addCallback(this);
			    
			  mSurfaceHolder01.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			  bIfPreview = false;
		 }
    	
    }
    
    private boolean CheckInternet(int retry)
    {
    	boolean has = false;
    	for (int i=0; i<=retry; i++)
    	{
    		has = HaveInternet();
    		if (has == true) break;    		
    	}
    	
		return has;
    }
    
    private boolean HaveInternet()
    {
	     boolean result = false;
	     
	     ConnectivityManager connManager = (ConnectivityManager) 
	                                getSystemService(Context.CONNECTIVITY_SERVICE); 
	      
	     NetworkInfo info = connManager.getActiveNetworkInfo();
	     
	     if (info == null || !info.isConnected())
	     {
	    	 result = false;
	     }
	     else 
	     {
		     if (!info.isAvailable())
		     {
		    	 result =false;
		     }
		     else
		     {
		    	 result = true;
		     }
     }
    
     return result;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
	    if (keyCode == KeyEvent.KEYCODE_BACK)
	    {
                builder.setMessage("Are you exit?");
                builder.setCancelable(false);
	               
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                     public void onClick(DialogInterface dialog, int id)
	                    {
	                     finish();
	                    }
	                });
	               
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                     public void onClick(DialogInterface dialog, int id)
	                    {
	                    }
	                });
	                
	              AlertDialog alert = builder.create();
	              alert.show();
	              
	              return false;
	    }
	    else if (keyCode == KeyEvent.KEYCODE_MENU)
	    {
	    	return super.onKeyDown(keyCode, event);
	    }
	
	    return super.onKeyDown(keyCode, event);
     
    }
    
	 private void initCamera()
	 {
	    if(!bIfPreview)
	 	  mCamera01 = Camera.open();
	    
	    if (mCamera01 != null && !bIfPreview)
	    {
	      Log.i(TAG, "inside the camera");
	      
	      Camera.Parameters parameters = mCamera01.getParameters();
	      parameters.setPictureFormat(PixelFormat.JPEG);
	      
	      //parameters.setPreviewSize(360, 240);
	      //parameters.setPictureSize(360, 240);
	      mCamera01.setParameters(parameters);

	      try
	      {
	    	  mCamera01.setPreviewDisplay(mSurfaceHolder01);
	    	  mCamera01.startPreview();
	      }
	      catch (Exception X)
	      {  
	    	  mCamera01.release();
	    	  mCamera01 = null;            
	      }
	      
	      bIfPreview = true;
	    }
	  }

	  public void takePicture() 
	  {
	      if(checkSDCard())
	      {
	    	    if (mCamera01 != null && bIfPreview) 
	    	    {
	    	      mCamera01.takePicture(shutterCallback, rawCallback, jpegCallback);
	    	      bIfPreview = false;
	    	    }
	      }
	  }
	  
	  private void resetCamera()
	  {
	    if (mCamera01 != null)
	    {
	        mCamera01.stopPreview();
	        mCamera01.release();
	        mCamera01 = null;
	        bIfPreview = false;
	    }
	  }
	   
	  private ShutterCallback shutterCallback = new ShutterCallback() 
	  { 
	    public void onShutter() 
	    { 
	      // Shutter has closed 
	    } 
	  }; 
	   
	  private PictureCallback rawCallback = new PictureCallback() 
	  { 
	    public void onPictureTaken(byte[] _data, Camera _camera) 
	    { 
	      // TODO Handle RAW image data 
	    } 
	  }; 
	  
	  private PictureCallback jpegCallback = new PictureCallback() 
	  {
	    public void onPictureTaken(byte[] _data, Camera _camera)
	    {
	      // TODO Handle JPEG image data	      
	      Bitmap bm = BitmapFactory.decodeByteArray(_data, 0, _data.length);
	      String filename = System.currentTimeMillis() +  ".jpg";
	      String pathfile = strCaptureFilePath + filename;    	    
	      File myCaptureFile = new File(pathfile);
	      
	      try
	      {
	        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(myCaptureFile));
	        bm.compress(Bitmap.CompressFormat.JPEG, 50, bos);
	        
	        bos.flush();	        
	        bos.close();
	        
	        uploadfile p = new uploadfile(filename);
	        p.start();
	        
	        resetCamera();        
	        initCamera();
	      }
	      catch (Exception e)
	      {
	        Log.e(TAG, e.getMessage());
	      }
	    }
	  };
	  
	  private void delFile(String strFileName)
	  {
	    try
	    {
	      File myFile = new File(strFileName);
	      if(myFile.exists())
	      {
	        myFile.delete();
	      }
	    }
	    catch (Exception e)
	    {
	      Log.e(TAG, e.toString());
	      e.printStackTrace();
	    }
	  }
	  
	  public class DateTask extends TimerTask {
		    public void run() 
		    {
		        if (!processing.compareAndSet(false, true)) return;

		        takePicture();
		        
		        processing.set(false);     
		    }
	  }
	  
	  private boolean checkSDCard()
	  {
	    if(android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
	    {
	      return true;
	    }
	    else
	    {
	      return false;
	    }
	  }
	  
	  @Override
	  public void surfaceChanged(SurfaceHolder surfaceholder, int format, int w, int h)
	  {
	    // TODO Auto-generated method stub
	    Log.i(TAG, "Surface Changed");
	  }
	  
	  @Override
	  public void surfaceCreated(SurfaceHolder surfaceholder)
	  {
	    // TODO Auto-generated method stub
		  	Log.i(TAG, "surfaceCreated");
	    	//mCamera01 = Camera.open();
		  	resetCamera(); 
		  	initCamera();
	   }
	  
	  @Override
	  public void surfaceDestroyed(SurfaceHolder surfaceholder)
	  {
	    // TODO Auto-generated method stub
		  if (mCamera01 != null)
		  {			  
		    try
		    {
		    	mCamera01.stopPreview();
		    	mCamera01.release();
		    	mCamera01 = null;
		    	bIfPreview = false;
		       //(strCaptureFilePath);
		    }
		    catch(Exception e)
		    {
		      e.printStackTrace();
		    }
		  }
		  
		  Log.i(TAG, "Surface Destroyed");
	  }
	  
	  public class uploadfile extends Thread 
	  {
		  String strfile;
		  
		  uploadfile(String strfilename)
		  {
			  strfile = strfilename;
		  }
		  
		  public void run() 
		  {
			    //Send Picture file
			    File file = new File(strCaptureFilePath + strfile);
    	        FTPClient client = new FTPClient();
    	        FileInputStream fis = null;
			    
	    
			    if (file.exists())
			    {
        	        try {
        	            client.connect("ftp.myweb.hinet.net");
        	            client.login("a85056250", "2oliouoi");


        	            if (client.isConnected() == true)
        	            {
	        	            //
	        	            // Create an InputStream of the file to be uploaded
	        	            //
	        	            fis = new FileInputStream(strCaptureFilePath + strfile);
	
	        	            //
	        	            // Store file to server
	        	            //
	        	            client.setFileType(FTP.BINARY_FILE_TYPE);

	        	            client.storeFile(strfile, fis);
	        	            client.logout();
        	            }     
	        	        } catch (IOException e) {
	        	            e.printStackTrace();
	        	        }			    	  
			    	
		              //delete
		              delFile(strCaptureFilePath + strfile);
			    }
		    	
		  }
	  }	  
	  
	  
	private void openOptionsDialog(String info)
	{
	    new AlertDialog.Builder(this)
	    .setTitle("message")
	    .setMessage(info)
	    .setPositiveButton("OK",
	        new DialogInterface.OnClickListener()
	        {
	         public void onClick(DialogInterface dialoginterface, int i)
	         {
	            	finish();
	         }
	        }
	        )
	    .show();
	}
	
}