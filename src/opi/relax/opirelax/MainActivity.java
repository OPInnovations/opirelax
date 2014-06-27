package opi.relax.opirelax;
import java.util.HashMap;
import java.util.Map;
import opi.relax.opirelax.R;
import opi.relax.openfile.CallbackBundle;
import opi.relax.openfile.OpenFileDialog;

import android.os.Bundle;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.view.Menu;
import opi_android.*;
import android.graphics.*;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.view.SurfaceHolder;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MainActivity extends Activity {
	MainActivity main=this;
	public Toast ToastMessage=null;
	//OPIPKT 
	public  OPIPKT_FFT OPIPKT_fft;
	public  OPIPKT_EDF_Reader OPIPKT_edfreader;
	public  OPIPKT_EDF_Writer OPIPKT_edfwriter;
	public  OPIPKT_struct [] OPIPKT_t;
	public  OPIPKT_android   OPIPKT_usb;
	public  OPIPKT_Data_Converter OPIPKT_DT;
	public  OPIPKT_Raw_File OPIPKT_rfio;
	public  OPIPKT_helper OPIPKT_hp;
	public  OPIPKT_DrawHelper OPIPKT_drawadchp;
	public  OPIPKT_DrawHelper OPIPKT_drawffthp;
	public  OPIPKT_DrawHelper OPIPKT_drawsmffthp;
	public  OPIPKT_Accel_Viewer OPIPKT_twv;
	//FOR OPIPKT USB
	public UsbManager manager;
	public UsbDevice mdevice;
	public  int usbPermissionGranted = -1;	// not sure if usbPermission is Granted yet
    public static final String ACTION_USB_PERMISSION = "opi.relax.opirelax.USB_PERMISSION";
    public static  PendingIntent mPermissionIntent;
    //FOR DRAWING SIGNAL
    public LinearLayout LL;
    //Live Mode
    public SurfaceView adcsurface;
	public SurfaceHolder adcholder;
	public SurfaceView smfftsurface;
	public SurfaceHolder smfftholder;
	public SurfaceView fftsurface;
	public SurfaceHolder fftholder;
	public SurfaceView posturesurface;
	public SurfaceHolder postureholder;
	public SurfaceView posturezsurface;
	public SurfaceHolder posturezholder;
    //local
	public static final int USBTESTTIMES=50;
	public static final int OPENFILEDIALOGID = 0; 
	public static final int TASKREFRESHPERIOD = 30; // 30 milliseconds
	public static final int TOUCHSCREENTHRESHOLD = 60; // 60pix
	public static final int USBCHECKSECOND=600;  //10min 
	public static final int SHOWMESSAGEDURATION=3000; //3s
	public static final int EVERYPACKAGEDURATION=1000; //1s
	public static final int ADCPACKAGEMAX= OPIPKT_Accel_Viewer.FRMSPERSEC * 20;   //1s per 4 packages,in this case we show 5s for adc
	public int Live_Button_State=0;
	public boolean Live_Is_Recording=false;
	public float touch_x, touch_y; //to save the position of user's hand
	public float touch_x2, touch_y2; //to save the position of user's hand
	public int zoomxSB=-1;
	public int final_x;
	public float distanceX;
	public int pagecount=0; //to save the page position
	public boolean lockScreen=false;   //to lock screen when config in page 0
	public int losewirelesscount=0; //to count how many times we lose package
	public String  RAWFILENAME = "OPIRELAX.txt"; 
	public boolean bigscreen=false; //Check the screen size
	public long button_start_time;
	public long button_end_time=0;
	public static final int Touch_DEBOUNCESHRESHOLD=2000; //2000ms
	public static final int Button_DEBOUNCESHRESHOLD=1000; //1000ms
	public boolean Button_StateOK=false;
	public long Touch_debouncetime=0; 
	public  boolean Live_Drawed=false;
	public Handler mHandler = new Handler();//to count the time
	//GUI Components
	//Live Mode
	public TextView tvRL;
    public TextView tvLevel;
    public TextView tvStartdate;
	public Button bt_live;
	public SeekBar SB;
	//Configure Mode
	public ProgressBar configPB;
	public TextView configTV;
	//Transient
	public GifRun w;	
	public ViewFlipper appVF;
	public SurfaceView gifsurface;
	//File Mode
	public Button bt_file;
	public LinearLayout fileLL;
    public SurfaceView fileadcsurface;
	public SurfaceHolder fileadcholder;
	public SurfaceView filesmfftsurface;
	public SurfaceHolder filesmfftholder;
	public SurfaceView filefftsurface;
	public SurfaceHolder filefftholder;
	public SurfaceView fileposturesurface;
	public SurfaceHolder filepostureholder;
	public SurfaceView fileposturezsurface;
	public SurfaceHolder fileposturezholder;
	public boolean onePointer=false;
	public boolean twoPointer=false;
	public long wroteDRCt;
	public int new_begin_index=0;  //reference by accx
	public int total_pack=0;
	public String EDFfilepath="";
	public boolean zoomdirectmore;
	private WakeLock wakeLock;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	requestWindowFeature(Window.FEATURE_NO_TITLE);   //GET WHOLE WINDOW DISPLAY
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
        WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        OPIPKT_helper.Local_reset(this);
        OPIPKT_helper.OPI_reset(this);
        OPIPKT_drawffthp.main=this;
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK,"");    
        wakeLock.acquire();
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
      	Live_Configure();
        SB.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
        	@Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
            	tvLevel.setText("LEVEL:"+Integer.toString(arg1));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
        });
        
        //File Mode Button Touch listener
        bt_file.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public synchronized boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				button_start_time=System.currentTimeMillis();
				if((OPIPKT_helper.button_debounce(button_start_time, button_end_time,Button_DEBOUNCESHRESHOLD)==true)
					&&(Button_StateOK==false))
				{
					Button_StateOK=true;
					if(lockScreen==false)
					{
						bt_file.setBackgroundResource(R.drawable.emptylock);
						lockScreen=true;
					}
					else
					{
						bt_file.setBackgroundResource(R.drawable.empty);
						lockScreen=false;
						OPIPKT_edfreader.getgesture=false;
					}
				}
				return false;
			}
		});	
        
        //File Mode Button click listener
        bt_file.setOnClickListener(new View.OnClickListener() {
          	@Override
 			   public void onClick(View v) {
                
          		if((Button_StateOK==true))
          		{
          			if(lockScreen==true)
              		{
          				 clean_File_Screen();
              			 OPIPKT_edfreader.reset(); 
              			 //NEED UPDATA FILE FOLDER
                  		 showDialog(OPENFILEDIALOGID);
              		}
          			Button_StateOK=false;
          			button_end_time=System.currentTimeMillis();
          		}
          	  }
        });
        
        
        //Live Mode Button Touch listener
        bt_live.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public synchronized boolean onTouch(View v, MotionEvent event) {
				button_start_time=System.currentTimeMillis();
				// TODO Auto-generated method stub
				if((OPIPKT_helper.button_debounce(button_start_time, button_end_time,Button_DEBOUNCESHRESHOLD)==true)
						&&((Live_Button_State!=OPIPKT_helper.LIVEBUTTONTRANSIENT))
						&&(Button_StateOK==false))
				{
					Button_StateOK=true;
					OPIPKT_helper.changeLiveButtonState(main, OPIPKT_hp, OPIPKT_helper.LIVEBUTTONTRANSIENT);	
				}
				return false;
			}
		});	
        
        //Live Mode Button click listener
        bt_live.setOnClickListener(new View.OnClickListener() {
          	@Override
 			   public synchronized void onClick(View v) {
          		if((Button_StateOK==true))
				{	
          			click_Live_Button_Function();
					Button_StateOK=false;
				}
          	  }
        });
    }
    
    
    
    
	 
    public Runnable  UpdateData = new Runnable() {
    	@Override
    	public synchronized void run() {
    		if(Live_Is_Recording==true)
    		{
    			// initialize data in case there are errors later
    			OPIPKT_t[0].payload[OPIPKT_android.DSNLEN+OPIPKT_android.TSLEN+6+OPIPKT_android.FWVLEN+1+OPIPKT_android.PDNLISTLEN+1] = 0;
    			if(OPIPKT_usb.opiuce_getwltsdata(OPIPKT_t[0])==1) 
        		{
        			losewirelesscount=0;
        			if(OPIPKT_DT.opipkt_dataprocess(OPIPKT_t[0],OPIPKT_edfwriter)==true)
            	    {
            		   OPIPKT_helper.changeLiveButtonState(main, OPIPKT_hp, OPIPKT_helper.LIVEBUTTONRECORDING);	
            	       Live_drawAll();
            	       if(OPIPKT_edfwriter.writingfunOK==true)
            	       OPIPKT_edfwriter.checkEDF();
            	       Live_Check_RawFile();
            	    }
                }// if get data end  
        		else
        		{
        			Miss_Wireless_Data();
    			}
    			mHandler.postDelayed(UpdateData, TASKREFRESHPERIOD); // update for next
    		}//if button is on end
    }};
     

     
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	appLivEend();
    }
    
    @Override
    public void onBackPressed()
    {
       appLivEend();
       super.onBackPressed();
    }
     
    public void finish() {
    	android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
        System.exit(0);
    	super.finish();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    

    
    @Override  
    protected Dialog onCreateDialog(int id) {  
        if(id==OPENFILEDIALOGID){   //open edf file folder
            Map<String, Integer> images = new HashMap<String, Integer>();  
            images.put(OpenFileDialog.sRoot, R.drawable.disk);  
            images.put(OpenFileDialog.sParent, R.drawable.goback);    
            images.put(OpenFileDialog.sFolder, R.drawable.folder); 
            images.put("edf", R.drawable.edfpic);   
            images.put(OpenFileDialog.sEmpty, R.drawable.empty);  
            Dialog dialog = OpenFileDialog.createDialog(id, this, "Open file", new CallbackBundle() {  
                @Override  
                public void callback(Bundle bundle) {  
                    EDFfilepath = bundle.getString("path");  
                    if((OPIPKT_edfreader.checkedfheader(appVF,EDFfilepath)==true)&&(EDFfilepath!="")) //file is correct
                    {
                    	clean_File_Screen();
             			OPIPKT_rfio.SetFileName(RAWFILENAME);
             			OPIPKT_edfreader.setfullfilename(EDFfilepath);
             			wroteDRCt=OPIPKT_edfreader.edfDread(OPIPKT_edfreader,OPIPKT_edfreader.raf,OPIPKT_rfio);
             			OPIPKT_edfreader.setLvelText("Recorded Time:"+Float.toString(wroteDRCt*OPIPKT_EDF_Writer.EDFDRDURSEC)+"s");
                 	    OPIPKT_edfreader.setRLText("Start Time:"+Integer.toString(0)+"s");
                 	    OPIPKT_edfreader.getgesture=true;
             			OPIPKT_edfreader.zoomined=false;
             			new_begin_index=0;
             			onePointer=false;
             			twoPointer=false;
             			OPIPKT_drawadchp.FileDrawRoutine(false,main,wroteDRCt,new_begin_index,0);
             			OPIPKT_rfio.CloseFile();
                    }
                    else
                    {
                    	clean_File_Screen();
                    	OPIPKT_edfreader.reset();
                    	bt_file.setBackgroundResource(R.drawable.empty);
						lockScreen=false;
						OPIPKT_edfreader.getgesture=false;
						Button_StateOK=false;
	          			button_end_time=System.currentTimeMillis();
                    }
                }  
            },   
            ".edf;",  
            images);  
            return dialog;  
        }  
        else if(id==OPIPKT_EDF_Writer.checkEDFDialogId) //can't write edf header correctly
        {
        		Dialog dialog = OPIPKT_EDF_Writer.createDialog(id,this, "Confirm", new CallbackBundle() {  
                @Override  
                public void callback(Bundle bundle) {  
                	OPIPKT_edfwriter.leave = bundle.getBoolean("LEAVE"); 
                	if(OPIPKT_edfwriter.leave==true)
                  	{
        		 			Live_Is_Recording=true;
                			click_Live_Button_Function();
                  	}
                  	else
                  	{
                  			Live_Is_Recording=true;
                  			UpdateData.run();
                  	}
                }  
            });
            return dialog;  
        }
        return null;  
    }  
    
    private synchronized void appLivEend()
    {
    	if(Live_Is_Recording==true)
		{
			Live_Button_OnToOff();	
		}
	    for(int i=0;i<USBTESTTIMES;i++)//set ont mode
        {
        	if( OPIPKT_usb.opiuce_offmode()==0)
        		break;
        }
   		OPIPKT_usb.opi_closeuce_com();
 		finish();
    }
    
    public synchronized void Live_Configure()
	{
    	usbPermissionGranted=-1;
      	if(checkopiusb()==0)
      		usbPermissionGranted=1;
		if(usbPermissionGranted!=1)
    	{
    		configTV.setText("No device connected");
    		OPIPKT_helper.changepage(this,true);
    	}
    	else
    	{
    		configTV.setText("Configuring...");
    		configPB.setIndeterminate(true);
        	new ConfigUce().execute((Void) null);
    	}	
	}
    
    /***
     * Class to configure Uce to get it ready for use. Done asynchronously so 
     * it doesn't block UI update
     * @return
     *     0, if successful
     *     < 0, if error, check below for specific code
     */
    public class ConfigUce extends AsyncTask<Void, Void, Integer> {
        @Override
        protected Integer doInBackground(Void... params) {
        	lockScreen=true;
        	return OPIPKT_helper.pairconfig(main);
        }
        protected void onPostExecute(Integer result) {
        		lockScreen=false;
        		if(result==0)
        		{
        			w = new  GifRun();
					w.LoadGiff(gifsurface,getResources().openRawResource(R.drawable.opigif));
					appVF.showNext();
					pagecount++;
	        		configPB.setIndeterminate(false);
        		}
        		else
        		{
        			pagecount=0;
					ToastMessage=Toast.makeText(appVF.getContext(),"Error:"+Integer.toString(result), Toast.LENGTH_LONG);
					ToastMessage.show();
        			configTV.setText("Previous=File                                          	Next=CONFIG");
	        		configPB.setIndeterminate(false);
        		}
        	}
    }
    


    public synchronized boolean onTouchEvent(MotionEvent event) {
    		   float x = event.getX(0);
               float y = event.getY(0);
               float x2;
               float y2;
               float distanceX;
               if(OPIPKT_edfreader.getgesture==true) //edf reader lock
               {
            	   int pointerCounter=event.getPointerCount();
            	   if(pointerCounter==1)
            	   {
            		   switch(event.getAction())
                       {
                       case MotionEvent.ACTION_DOWN:
                    	   this.onePointer=true;
                           this.touch_x = x;
                           this.touch_y = y;
                           this.zoomxSB = -1;
                           break;
                       case MotionEvent.ACTION_MOVE:
                    	   if(this.onePointer==true&&OPIPKT_edfreader.zoomined==false&&((System.currentTimeMillis()-Touch_debouncetime)>Touch_DEBOUNCESHRESHOLD/3))
                    	   {
                    		   Canvas cadc,cz;
                    		   int beginx,endx;
                        	   if(this.zoomxSB==-1)
                        	     {
                        		   if(x<this.touch_x)
                        		   {	
                        			   this.zoomdirectmore=false;
                        			   beginx=(int) x;
                        			   this.final_x=(int)x;
                        			   endx=(int) this.touch_x;
                        			   this.zoomxSB = beginx;
                        			   cadc = fileadcholder.lockCanvas(new Rect(beginx,0,endx,fileadcsurface.getHeight()));
                            		   cz  = fileposturezholder.lockCanvas(new Rect(beginx,0,endx,fileposturezsurface.getHeight()));
                            		   //adc
                                	   Paint adcpaint = new Paint();
                                	   Rect adcrect =new Rect(beginx,0,endx,fileadcsurface.getHeight());
                                	   adcpaint.setColor(Color.argb(100, 255, 255, 0));
                                       cadc.drawRect(adcrect, adcpaint);
                                	   fileadcholder.unlockCanvasAndPost(cadc);
                                	   //z
                    	    		   Paint zpaint = new Paint();
                       	    		   Rect zrect =new Rect(beginx,0,endx,fileposturezsurface.getHeight());
                    	    		   zpaint.setColor(Color.argb(100, 255, 255, 0));
                    	    		   cz.drawRect(zrect, zpaint);
                                	   fileposturezholder.unlockCanvasAndPost(cz);
                        		   }
                        		   else if(x>this.touch_x)
                        		   {
                        			   this.zoomdirectmore=true;
                        			   beginx=(int) this.touch_x;
                        			   endx=(int) x;
                        			   this.final_x=(int)x;
                        			   this.zoomxSB = endx;
                        			   cadc = fileadcholder.lockCanvas(new Rect(beginx,0,endx,fileadcsurface.getHeight()));
                            		   cz  = fileposturezholder.lockCanvas(new Rect(beginx,0,endx,fileposturezsurface.getHeight()));
                            		   //adc
                                	   Paint adcpaint = new Paint();
                                	   Rect adcrect =new Rect(beginx,0,endx,fileadcsurface.getHeight());
                                	   adcpaint.setColor(Color.argb(100, 255, 255, 0));
                                	   cadc.drawRect(adcrect, adcpaint);
                                	   fileadcholder.unlockCanvasAndPost(cadc);
                                	   //z
                    	    		   Paint zpaint = new Paint();
                       	    		   Rect zrect =new Rect(beginx,0,endx,fileposturezsurface.getHeight());
                    	    		   zpaint.setColor(Color.argb(100, 255, 255, 0));
                    	    		   cz.drawRect(zrect, zpaint);
                                	   fileposturezholder.unlockCanvasAndPost(cz);
                        		   }
                        	     }
                        	   else
                        	   	{
                        		   if(x<this.zoomxSB&&(this.zoomdirectmore==false))
                        		   {
                        			   		   beginx=(int) x;
                                 			   this.final_x=(int)x;
                                			   endx=this.zoomxSB;
                                			   this.zoomxSB = beginx;
                                			   cadc = fileadcholder.lockCanvas(new Rect(beginx,0,endx,fileadcsurface.getHeight()));
                                    		   cz  = fileposturezholder.lockCanvas(new Rect(beginx,0,endx,fileposturezsurface.getHeight())); 
                                    		   //adc
                                        	   Paint adcpaint = new Paint();
                                        	   Rect adcrect =new Rect(beginx,0,endx,fileadcsurface.getHeight());
                                        	   adcpaint.setColor(Color.argb(100, 255, 255, 0));
                                        	   cadc.drawRect(adcrect, adcpaint);
                                        	   fileadcholder.unlockCanvasAndPost(cadc);
                                        	   //z
                            	    		   Paint zpaint = new Paint();
                               	    		   Rect zrect =new Rect(beginx,0,endx,fileposturezsurface.getHeight());
                            	    		   zpaint.setColor(Color.argb(100, 255, 255, 0));
                            	    		   cz.drawRect(zrect, zpaint);
                                        	   fileposturezholder.unlockCanvasAndPost(cz);
                        		   }
                        		   else if(x>this.zoomxSB&&(this.zoomdirectmore==true))
                        		   {
                        			   	   beginx=this.zoomxSB;
                            			   endx=(int) x;
                            			   this.final_x=(int)x;
                            			   this.zoomxSB = endx;
                            			   cadc = fileadcholder.lockCanvas(new Rect(beginx,0,endx,fileadcsurface.getHeight()));
                                		   cz  = fileposturezholder.lockCanvas(new Rect(beginx,0,endx,fileposturezsurface.getHeight())); 
                                		   //adc
                                    	   Paint adcpaint = new Paint();
                                    	   Rect adcrect =new Rect(beginx,0,endx,fileadcsurface.getHeight());
                                    	   adcpaint.setColor(Color.argb(100, 255, 255, 0));
                                    	   cadc.drawRect(adcrect, adcpaint);
                                    	   fileadcholder.unlockCanvasAndPost(cadc);
                        	    		   //z
                        	    		   Paint zpaint = new Paint();
                           	    		   Rect zrect =new Rect(beginx,0,endx,fileposturezsurface.getHeight());
                        	    		   zpaint.setColor(Color.argb(100, 255, 255, 0));
                        	    		   cz.drawRect(zrect, zpaint);
                                    	   fileposturezholder.unlockCanvasAndPost(cz);
                        		   }
                        	   	}  
                    	   }
                    	   break;
                       case MotionEvent.ACTION_UP:
                    	   if(this.onePointer==true&&((System.currentTimeMillis()-Touch_debouncetime)>Touch_DEBOUNCESHRESHOLD/3)&&OPIPKT_edfreader.zoomined==true)
                    	   {
                    		   if(x - this.touch_x > TOUCHSCREENTHRESHOLD)
                               {
                    			   	   this.onePointer=false;
                            		   if(ToastMessage!=null)
                                   	    ToastMessage.cancel();
                            		   if(OPIPKT_edfreader.zoomined==true)
                            		   {
                            			   int tempsave = new_begin_index;
                            			   new_begin_index-=total_pack;
                            			   if(new_begin_index>=0)
                            			   {
                            				   clean_File_Screen();
                            				   String message;
                                        	   
                                        	   message = "Display page:"+Float.toString((float)(total_pack)/(float)OPIPKT_Accel_Viewer.FRMSPERSEC)+"s  ";
                                        	   message = message+"Start Time:"+Float.toString((float)new_begin_index/(float)OPIPKT_Accel_Viewer.FRMSPERSEC)+"s  ";
                                        	   message = message+"End Time:"+Float.toString((float)(new_begin_index+total_pack)/(float)OPIPKT_Accel_Viewer.FRMSPERSEC)+"s";
                                        	   
                                        	   
                                        	   OPIPKT_edfreader.setRLText(message);
                                        	   OPIPKT_drawadchp.FileDrawRoutine(true,main,wroteDRCt,new_begin_index,total_pack);
                                    		   Touch_debouncetime=System.currentTimeMillis();
                            			   }
                            			   else
                            			   {
                            				   new_begin_index=0;
                            				   clean_File_Screen();
                            				   String message;
                                        	   
                                        	   message = "Display page:"+Float.toString((float)(total_pack)/(float)OPIPKT_Accel_Viewer.FRMSPERSEC)+"s  ";
                                        	   message = message+"Start Time:"+Float.toString((float)new_begin_index/(float)OPIPKT_Accel_Viewer.FRMSPERSEC)+"s  ";
                                        	   message = message+"End Time:"+Float.toString((float)(new_begin_index+total_pack)/(float)OPIPKT_Accel_Viewer.FRMSPERSEC)+"s";
                                        	   
                                        	   
                                        	   OPIPKT_edfreader.setRLText(message);
                                        	   OPIPKT_drawadchp.FileDrawRoutine(true,main,wroteDRCt,new_begin_index,total_pack);
                                    		   Touch_debouncetime=System.currentTimeMillis();
                            				   //Toast.makeText(appVF.getContext(),"Previous Page Not Enough "+Float.toString((float)(total_pack)/(float)OPIPKT_Accel_Viewer.FRMSPERSEC)+"s", Toast.LENGTH_SHORT).show();
                            				   //new_begin_index=tempsave;
                            			   } 
                            		   }
                               }
                               else if(x - this.touch_x < -1*TOUCHSCREENTHRESHOLD)
                               {
                            	       this.onePointer=false;
                            		   if(ToastMessage!=null)
                                   	    ToastMessage.cancel();
                            		   if(OPIPKT_edfreader.zoomined==true)
                            		   {
                            			   int tempsave = new_begin_index;
                            			   new_begin_index+=total_pack;
                            			   if(new_begin_index+total_pack<OPIPKT_edfreader.axQV.size())
                            			   {
                            				   clean_File_Screen();
                            				   String message;
                                        	   
                            				   message = "Display page:"+Float.toString((float)(total_pack)/(float)OPIPKT_Accel_Viewer.FRMSPERSEC)+"s  ";
                                        	   message = message+"Start Time:"+Float.toString((float)new_begin_index/(float)OPIPKT_Accel_Viewer.FRMSPERSEC)+"s  ";
                                        	   message = message+"End Time:"+Float.toString((float)(new_begin_index+total_pack)/(float)OPIPKT_Accel_Viewer.FRMSPERSEC)+"s";
                                        	   
                                        	   
                                        	   OPIPKT_edfreader.setRLText(message);
                            				   OPIPKT_drawadchp.FileDrawRoutine(true,main,wroteDRCt,new_begin_index,total_pack);
                                    		   Touch_debouncetime=System.currentTimeMillis();
                            			   }
                            			   else if(new_begin_index<OPIPKT_edfreader.axQV.size())
                            			   {
                            				   new_begin_index = OPIPKT_edfreader.axQV.size()- total_pack;
                            				   clean_File_Screen();
                            				   String message;
                                        	   message = "Display page:"+Float.toString((float)(total_pack)/(float)OPIPKT_Accel_Viewer.FRMSPERSEC)+"s  ";
                                        	   message = message+"Start Time:"+Float.toString((float)new_begin_index/(float)OPIPKT_Accel_Viewer.FRMSPERSEC)+"s  ";
                                        	   message = message+"End Time:"+Float.toString((float)(new_begin_index+total_pack)/(float)OPIPKT_Accel_Viewer.FRMSPERSEC)+"s";
                                        	   
                                        	   OPIPKT_edfreader.setRLText(message);
                            				   OPIPKT_drawadchp.FileDrawRoutine(true,main,wroteDRCt,new_begin_index,OPIPKT_edfreader.axQV.size()-new_begin_index);
                                    		   Touch_debouncetime=System.currentTimeMillis();
                            			   }
                            			   else
                            			   {
                            				   Toast.makeText(appVF.getContext(),"Out of file size", Toast.LENGTH_SHORT).show();
                            				   new_begin_index = tempsave;
                            			   }
                            		   }	 
                               }
                    	   }
                    	   else if(this.onePointer==true&&((System.currentTimeMillis()-Touch_debouncetime)>Touch_DEBOUNCESHRESHOLD/3)&&OPIPKT_edfreader.zoomined==false)
                    	   {
                        		   OPIPKT_edfreader.zoomined=true;
                        		   bt_file.setBackgroundResource(R.drawable.emptylockzoomin);
                            	   clean_File_Screen();
                            	   float beginx,endx;
                            	   if(this.touch_x<this.final_x)
                            	   {
                            		   beginx=this.touch_x;
                            		   endx=this.final_x;
                            	   }
                            	   else
                            	   {
                            		   beginx=this.final_x;
                            		   endx=this.touch_x;
                            	   }
                            	   new_begin_index=(int) ((beginx/(float)fileadcsurface.getWidth())*(wroteDRCt*OPIPKT_EDF_Writer.EDFDRDURSEC*OPIPKT_Accel_Viewer.FRMSPERSEC));
                            	   new_begin_index-=1;
                            	   total_pack=(int) (((float)(endx-beginx)/(float)fileadcsurface.getWidth())*(wroteDRCt*OPIPKT_EDF_Writer.EDFDRDURSEC*OPIPKT_Accel_Viewer.FRMSPERSEC));
                            	   total_pack+=OPIPKT_Accel_Viewer.FRMSPERSEC;
                            	   float total_float_sec = (float)(total_pack)/(float)OPIPKT_Accel_Viewer.FRMSPERSEC;
                            	   int total_int_sec = (int)(total_float_sec/10)*10+10;
                            	   total_pack=total_int_sec*OPIPKT_Accel_Viewer.FRMSPERSEC;
                            	   if(new_begin_index<0)
                            		   new_begin_index=0;
                            	   int sec_begin_int = (int) ((float)new_begin_index/(float)OPIPKT_Accel_Viewer.FRMSPERSEC);
                            	   new_begin_index = (int)(sec_begin_int*OPIPKT_Accel_Viewer.FRMSPERSEC);
                            	   String message;
                            	   message = "Display page:"+Float.toString((float)(total_pack)/(float)OPIPKT_Accel_Viewer.FRMSPERSEC)+"s  ";
                            	   message = message+"Start Time:"+Float.toString((float)new_begin_index/(float)OPIPKT_Accel_Viewer.FRMSPERSEC)+"s  ";
                            	   message = message+"End Time:"+Float.toString((float)(new_begin_index+total_pack)/(float)OPIPKT_Accel_Viewer.FRMSPERSEC)+"s";
                            	   
                            	   OPIPKT_edfreader.setRLText(message);
                            	   OPIPKT_drawadchp.FileDrawRoutine(true,main,wroteDRCt,new_begin_index,total_pack);
                        		   Touch_debouncetime=System.currentTimeMillis();
                    	   }
                           break;
                       }
            	   }
            	   else if(pointerCounter==2)
            	   {
            		   this.onePointer=false;
            		   x2=event.getX(1);
            		   y2=event.getY(1);
            		   if(this.twoPointer==false&&((System.currentTimeMillis()-Touch_debouncetime)>Touch_DEBOUNCESHRESHOLD/2))
            		   {
                           this.twoPointer=true;
                           this.touch_x = event.getX(0);
                           this.touch_y = event.getY(0);
                           this.touch_x2 = event.getX(1);
                           this.touch_y2 = event.getY(1);
                           this.distanceX=this.touch_x2-this.touch_x;
                           if(this.distanceX<0)
                        	   this.distanceX=-1*this.distanceX;
                       }
            		   else if(this.twoPointer==true)
            		   {
            			   if(((x - this.touch_x > TOUCHSCREENTHRESHOLD/4)&&(x2 - this.touch_x2 < -1*TOUCHSCREENTHRESHOLD/4))||
            			   		((x - this.touch_x < -1*TOUCHSCREENTHRESHOLD/4)&&(x2 - this.touch_x2 > TOUCHSCREENTHRESHOLD/4)))
                           {
                				   distanceX=x2-x;
                                   if(distanceX<0)
                                	   distanceX=-1*distanceX;
                                   this.twoPointer=false;
                				   if(ToastMessage!=null)
                                  	    ToastMessage.cancel();
                                   if(distanceX<=this.distanceX)
                                   {
                                	   if(OPIPKT_edfreader.zoomined==true)
                                	   {
                                   	    OPIPKT_edfreader.zoomined=false;
                                   	    bt_file.setBackgroundResource(R.drawable.emptylock);
                                   	    clean_File_Screen();
                                   		OPIPKT_edfreader.setRLText("Start Time:"+Float.toString(0.0f)+"s");
                                   		new_begin_index=0;
                                		OPIPKT_drawadchp.FileDrawRoutine(false,main,wroteDRCt,new_begin_index,0);
                                		Touch_debouncetime=System.currentTimeMillis();
                                	   }
                                   }
                           		   
                           }
            		   }
            	   }
               }
               else //normal gesture
               {
            	   switch(event.getAction())
                   {
                   case MotionEvent.ACTION_DOWN:
                       this.touch_x = x;
                       this.touch_y = y;
                       break;
                   case MotionEvent.ACTION_UP:
                   	if((Live_Button_State==OPIPKT_helper.LIVEBUTTONNOTRECORDING)
                   			&&lockScreen==false)
                   	{
                       if(x - this.touch_x > TOUCHSCREENTHRESHOLD)
                       {
                    	   if((System.currentTimeMillis()-Touch_debouncetime)>Touch_DEBOUNCESHRESHOLD)
                    	   {
                    		   clean_File_Screen();
                    		   Touch_debouncetime=System.currentTimeMillis();
                    		   if(ToastMessage!=null)
                           	    ToastMessage.cancel();
                               OPIPKT_helper.changepage(this,true);
                    	   }
                       }
                       else if(x - this.touch_x < -1*TOUCHSCREENTHRESHOLD)
                       {
                    	   if((System.currentTimeMillis()-Touch_debouncetime)>Touch_DEBOUNCESHRESHOLD)
                    	   {
                    		   clean_File_Screen();
                    		   Touch_debouncetime=System.currentTimeMillis();
                    		   if(ToastMessage!=null)
                           	    ToastMessage.cancel();
                           	   OPIPKT_helper.changepage(this,false);
                    	   }
                       }
                   	}
                       break;
                   }
               }
               appVF.getCurrentView().postInvalidate();
           return super.onTouchEvent(event);
    }

    public synchronized void clean_Live_Screen()
    {
    	if(Live_Drawed==true)
    	{
    		if(bigscreen==true)
	        {
		 	 OPIPKT_DrawHelper.cleansurfaceview(adcholder, adcsurface);
		 	 OPIPKT_DrawHelper.cleansurfaceview(fftholder, fftsurface);
	        }
		    OPIPKT_DrawHelper.cleansurfaceview(smfftholder, smfftsurface);
		 	OPIPKT_DrawHelper.cleansurfaceview(postureholder, posturesurface);
		 	OPIPKT_DrawHelper.cleansurfaceview(posturezholder, posturezsurface);
		 	if(bigscreen==true)
	        {
		 	 OPIPKT_DrawHelper.cleansurfaceview(adcholder, adcsurface);
		 	 OPIPKT_DrawHelper.cleansurfaceview(fftholder, fftsurface);
	        }
		    OPIPKT_DrawHelper.cleansurfaceview(smfftholder, smfftsurface);
		 	OPIPKT_DrawHelper.cleansurfaceview(postureholder, posturesurface);
		 	OPIPKT_DrawHelper.cleansurfaceview(posturezholder, posturezsurface);
		 	Live_Drawed=false;
    	}	
    }
    
    public synchronized void clean_File_Screen()
    {
    	if(OPIPKT_edfreader.drawed==true)
    	{
    		if(bigscreen==true)
	        {
		 	 OPIPKT_DrawHelper.cleansurfaceview(fileadcholder, fileadcsurface);
		 	 OPIPKT_DrawHelper.cleansurfaceview(filefftholder, filefftsurface);
	        }
		    OPIPKT_DrawHelper.cleansurfaceview(filesmfftholder, filesmfftsurface);
		 	OPIPKT_DrawHelper.cleansurfaceview(filepostureholder, fileposturesurface);
		 	OPIPKT_DrawHelper.cleansurfaceview(fileposturezholder, fileposturezsurface);
		 	if(bigscreen==true)
	        {
		 	 OPIPKT_DrawHelper.cleansurfaceview(fileadcholder, fileadcsurface);
		 	 OPIPKT_DrawHelper.cleansurfaceview(filefftholder, filefftsurface);
	        }
		    OPIPKT_DrawHelper.cleansurfaceview(filesmfftholder, filesmfftsurface);
		 	OPIPKT_DrawHelper.cleansurfaceview(filepostureholder, fileposturesurface);
		 	OPIPKT_DrawHelper.cleansurfaceview(fileposturezholder, fileposturezsurface);
		 	OPIPKT_edfreader.drawed=false;
    	}
    }
    
    
    private synchronized void Live_Button_OnToOff()
    {
    	  String temp;
    	  Live_Is_Recording=false;
    	  temp = OPIPKT_edfwriter.CloseFile(this);
    	  OPIPKT_rfio.CloseFile();
	     	SB.setVisibility(0);
			SB.setEnabled(true);	
	 		if((Button_StateOK==true))
	    	{
	    		button_end_time=System.currentTimeMillis();
	    	}
			OPIPKT_helper.changeLiveButtonState(main, OPIPKT_hp, OPIPKT_helper.LIVEBUTTONNOTRECORDING);
			ToastMessage=Toast.makeText(appVF.getContext(),temp, Toast.LENGTH_SHORT);
			ToastMessage.show();
    }
    
    private synchronized void Live_Button_OffToOn()
    {
    		int STARTCHECK=-1;
			OPIPKT_helper.OPI_default(this);
			clean_Live_Screen();
			clean_File_Screen();
			checkopiusb();
		 	for(int i=0;i<USBTESTTIMES;i++)
		 	{
		 		if(OPIPKT_hp.opistart(OPIPKT_t[0],OPIPKT_usb,OPIPKT_fft,SB.getProgress())==0)
		 		{
		 			STARTCHECK=0;
		 			break;
				}
		 	}
		 	if(STARTCHECK==0)
			{
			  for(int i=0;i<USBTESTTIMES;i++)
              if(OPIPKT_hp.opigetrelaxdata(OPIPKT_t[0],OPIPKT_usb,tvRL)==0) break;
         
              for(int i=0;i<USBTESTTIMES;i++)
              if(OPIPKT_usb.opiuce_resetrelaxdata()==0) break;
              
			  OPIPKT_twv.setWandH(posturesurface.getWidth(),posturesurface.getHeight(),posturezsurface.getWidth(),posturezsurface.getHeight());
			  SB.setVisibility(4);
			  SB.setEnabled(false);
			  OPIPKT_rfio.SetFileName(RAWFILENAME);
			  if(OPIPKT_edfwriter.buildfile(OPIPKT_edfreader,appVF,this)==true) //return true, if writing fun is ok
			  {
				  clean_Live_Screen();
		    	  OPIPKT_helper.changeLiveButtonState(main, OPIPKT_hp, OPIPKT_helper.LIVEBUTTONRECORDING);
			 	  Live_Is_Recording=true;
			 	  if((Button_StateOK==true))
			    	{
			 		  button_end_time=System.currentTimeMillis();
			    	}
				  UpdateData.run();
			  }
			  else 
			  {
				  if((Button_StateOK==true))
			      {
			 		  button_end_time=System.currentTimeMillis();
			      }
				  showDialog(OPIPKT_EDF_Writer.checkEDFDialogId);
			  } 
			}
			else
			{
				ToastMessage=Toast.makeText(appVF.getContext(),"Please configure again", Toast.LENGTH_SHORT);
				ToastMessage.show();
		 		Live_Is_Recording=true;
				click_Live_Button_Function();
			}
    }
    
    private synchronized void Live_drawAll()
    {
    	if(Live_Drawed==false)
    	{
    		Live_Drawed=true;
    	}
    	if(OPIPKT_DT.sampQual <= 3) // take only data with adequate signal quality 0= CRC=1, CRC=0 1=noC; 2=<20C; 3=>20C;
 	    {   
    		if(OPIPKT_usb.opiuce_getwltsefft(OPIPKT_t[1])==0) 
    		{
    			OPIPKT_DT.convertFFT(OPIPKT_t[1]);
            	OPIPKT_DrawHelper.drawsmallfft(OPIPKT_drawsmffthp, OPIPKT_fft.calsmallfft(OPIPKT_DT,tvRL),smfftholder, smfftsurface);
            }
            else
            {
            	OPIPKT_DrawHelper.drawsmallfft(OPIPKT_drawsmffthp,-1,smfftholder, smfftsurface); //DUMMY
            }
    	}
    	if(bigscreen==true)
		{
			OPIPKT_DrawHelper.drawsignal(OPIPKT_DrawHelper.DRAWADCMAX,OPIPKT_DrawHelper.DRAWADCMIN,OPIPKT_drawadchp, ADCPACKAGEMAX,OPIPKT_DT.adcvalue, adcholder, adcsurface,OPIPKT_DT.adclength);
			OPIPKT_helper.calculatefft(OPIPKT_drawffthp,OPIPKT_DT.adcvalue,OPIPKT_DT.adclength);
			OPIPKT_drawffthp.numpathfftcol = fftsurface.getWidth()/OPIPKT_DrawHelper.FFTPENWIDTH;
			OPIPKT_DrawHelper.drawfft(OPIPKT_drawffthp, fftholder, fftsurface);
			if(OPIPKT_drawffthp.clean_z==true)
			{
				OPIPKT_DrawHelper.cleansurfaceview(posturezholder,posturezsurface);
				OPIPKT_drawffthp.clean_z=false;
				OPIPKT_twv.acczplane.clear();
				OPIPKT_twv.myQPP2.reset();
			}
		}
    	
    	OPIPKT_twv.setWandH(posturesurface.getWidth(),posturesurface.getHeight(),posturezsurface.getWidth(),posturezsurface.getHeight());
 		OPIPKT_Accel_Viewer.displayroutine(OPIPKT_twv,OPIPKT_DT,postureholder,posturesurface);

		Canvas csfh = postureholder.lockCanvas(new Rect( 0,0, posturesurface.getWidth(),posturesurface.getHeight()));
 		csfh.drawPath(OPIPKT_twv.myQPP, OPIPKT_twv.pcolor);
 		postureholder.unlockCanvasAndPost(csfh);

 		//OPIPKT_DrawHelper.cleansurfaceview(posturezholder,posturezsurface);
		Canvas csfhz = posturezholder.lockCanvas(new Rect( 0,0, posturezsurface.getWidth(),posturezsurface.getHeight()));
 		csfhz.drawPath(OPIPKT_twv.myQPP2, OPIPKT_twv.pcolor);
 		posturezholder.unlockCanvasAndPost(csfhz);
    }
    
	 private synchronized void Live_Check_RawFile()
	 {
	    	//OPIPKT_rfio.WriteStringToFile(Long.toString(System.currentTimeMillis())+"\n");
	 	    for(int i=0;i<6;i++)
	 	    	OPIPKT_rfio.WriteStringToFile("frmTS:["+Integer.toString(i)+"]="
	 	    +Integer.toBinaryString(OPIPKT_t[0].payload[OPIPKT_android.WFRMHDRLEN-1+i])+"\n");
	 	    
	 	    //OPIPKT_rfio.WriteStringToFile("sampQual:"+Integer.toString(OPIPKT_DT.sampQual)+"\n");
	 		//OPIPKT_rfio.WriteStringToFile("length:"+Integer.toString(OPIPKT_DT.length)+"\n");
	 		//for(int i=0;i<OPIPKT_DT.adclength;i++)
	 		//OPIPKT_rfio.WriteStringToFile("ADC["+Integer.toString(i)+"]:"+Integer.toString(OPIPKT_DT.adcvalue[i])+"\n");
	 	    OPIPKT_rfio.WriteStringToFile("X:"+Integer.toString(OPIPKT_DT.xvalue)+"\n");
	 	    OPIPKT_rfio.WriteStringToFile("Y:"+Integer.toString(OPIPKT_DT.yvalue)+"\n");
	 		//OPIPKT_rfio.WriteStringToFile("Z[0]:"+Integer.toString(OPIPKT_DT.zvalue[0])+"\n");
	 	    OPIPKT_rfio.WriteStringToFile("Temp:"+Integer.toString(OPIPKT_DT.tempvalue)+"\n");
	 	    for(int i=0;i<6;i++)
	 	    	OPIPKT_rfio.WriteStringToFile("time"+Long.toString(System.currentTimeMillis()));
	 	
	 }
	 
	 
	 /***
	  * if time out then finish edf file
	  * then tell user what happens
	  */
	 private  void Miss_Wireless_Data()
     {
    	 	losewirelesscount++;
    	 	if(losewirelesscount>=(EVERYPACKAGEDURATION/TASKREFRESHPERIOD))
    	 		OPIPKT_helper.changeLiveButtonState(main, OPIPKT_hp, OPIPKT_helper.LIVEBUTTONWARNNING);
    	 	
    	 	if(losewirelesscount==(USBCHECKSECOND*1000/TASKREFRESHPERIOD))  
			{
				if(checkopiusb()!=0)
				{
					losewirelesscount=0;
					ToastMessage=Toast.makeText(appVF.getContext(),"USB is not ok. Finishing edf file...", Toast.LENGTH_LONG);
					ToastMessage.show();
				}
				else
				{
					losewirelesscount=0;
					ToastMessage=Toast.makeText(appVF.getContext(),"USB is ok, but system can't get data. Finishing edf file...", Toast.LENGTH_LONG);
					ToastMessage.show();
				}
				OPIPKT_helper.changeLiveButtonState(main, OPIPKT_hp, OPIPKT_helper.LIVEBUTTONTRANSIENT);	
				click_Live_Button_Function();
			}
    	 	else if(((losewirelesscount%(SHOWMESSAGEDURATION/TASKREFRESHPERIOD))==0)||
    	 			(losewirelesscount==(EVERYPACKAGEDURATION/TASKREFRESHPERIOD))) 
    	 	{
    	 		String message="";
    	 		int countsec=((USBCHECKSECOND*1000/TASKREFRESHPERIOD)-losewirelesscount)*TASKREFRESHPERIOD/1000;
    	 		if(countsec!=0)
    	 		{
    	 			message="System still can't get data. Stop Live-Mode in "+Integer.toString(countsec)+"s";
    	 		}
    	 		else
    	 		{
    	 			message="shutting down live mode";
    	 		}
    	 		ToastMessage=Toast.makeText(appVF.getContext(),message, Toast.LENGTH_SHORT);
	 			ToastMessage.show();
	 			checkopiusb();
    	 	}
    	 	else if(losewirelesscount%(1000/TASKREFRESHPERIOD)==0) //every seconds
    	 	{
    	 		checkopiusb();
    	 	}
    	 	
    	 		
     }
	 
	 /***
	  * return 0 if success
	  * @return
	  */
	 private synchronized int checkopiusb()
	 {
		 int checkusb=-1;
			UsbManager managercheck = (UsbManager) getSystemService(Context.USB_SERVICE);
			for(UsbDevice device :  managercheck.getDeviceList().values()) 
			{ 
				if((device.getProductId()==OPIPKT_android.OPIUCPID)&&(device.getVendorId()==OPIPKT_android.OPIUCVID))
				{
					if(!managercheck.hasPermission(device))
					{
						managercheck.requestPermission(device,mPermissionIntent);
					} 
					else 
					{
						checkusb = 0;
						mdevice = device;		
						manager = managercheck;
						OPIPKT_usb.checkDevice(manager,mdevice);
					}						    				    					
					break;
				}
			}//for end
			return checkusb;
	 }
	 
	 private synchronized void click_Live_Button_Function()
	    {
	    	if(Live_Is_Recording==false)  //if flag is false, then switch to on
			{
	    		Live_Button_OffToOn();	
	  		}
			else if(Live_Is_Recording==true)
			{
				Live_Button_OnToOff();
			}
	    }

}
