package opi_android;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import opi.relax.opirelax.MainActivity;
import opi.relax.opirelax.R;
import android.graphics.PixelFormat;
import android.util.DisplayMetrics;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;


public class OPIPKT_helper {
		//LIVE BUTTON
		public final static int LIVEBUTTONNOTRECORDING=0;
		public final static int LIVEBUTTONRECORDING=1;
		public final static int LIVEBUTTONTRANSIENT=2;
		public final static int LIVEBUTTONWARNNING=-1;
		//PAGE
		public final static int PAGE_CONFIGURE=0;
		public final static int PAGE_LIVEMODE=1;
		public final static int PAGE_FILEMODE=2;
		public static final int PAGEMAX = 2; 
		public static final int PAGEMIN = 0;
		// Relax parameters
		// use below as reference and calc/scale to RLEVEL
	    public final static int COMRETRYTIME=10;
		public final static int DEFAULTRLEVEL =  2; //0 EASY  to 10 HARD
		public final static int THX = 1200; //EXCLUSION: 2000 for ReLax APP; 300 for AttN APP
		public final static int THM =  260; //M
		public final static int THGM =  60; //1G
		public final static int THBM =  55; //1B 1x
		public final static int OFFL=  300; //LF 1x
		public final static int OFFM =  60; //all 4 the same
		public final static int RLSCOREMINPKTS= 32; // must get 8"=32 packets to write file/show score
		public final static int LEDNON= 0;
		public final static int LEDBLU= 1;
		public final static int LEDGRN= 2;
		public final static int LEDORG= 3;
		public final static int LEDRED= 4;
		public final static int SCREENHEIGH= 800;  //BIG is 800 
		public final static int EXPNFFT =9; //2^EXPNFFT=NUMPATHFFT
		MainActivity main;
		public int mytsepdn;
		public int tsedsn;
		public int resMinInd;
		public int ucusdStatus;
		public int [] resWLScan= new int[OPIPKT_android.ZBWLCHANCT];
		public  int bestzbChan; 
		public static double screenheigth,screenwidth;
		public boolean configuredflag;
		public  boolean  configFailFlg;
	public OPIPKT_helper(MainActivity mainp){
		super();
		reset();
		main=mainp;
	}
	
	/***
	 * if the change state doesn't equal to the current state then change the pic
	 * @param main
	 * @param OPIPKT_hp
	 * @param state
	 */
	public static synchronized void changeLiveButtonState(MainActivity main,OPIPKT_helper OPIPKT_hp,int state)
	{
		if(state!=main.Live_Button_State) //if not equal then change
		{
			main.Live_Button_State=state;
			if(main.Live_Button_State==OPIPKT_hp.LIVEBUTTONWARNNING)
			{
				main.bt_live.setBackgroundResource(R.drawable.livewarnning);
			}
			else if(main.Live_Button_State==OPIPKT_hp.LIVEBUTTONNOTRECORDING)
			{
				main.bt_live.setBackgroundResource(R.drawable.livenotrecording);
			}
			else if(main.Live_Button_State==OPIPKT_hp.LIVEBUTTONRECORDING)
			{
				main.bt_live.setBackgroundResource(R.drawable.liverecording);
			}	
			else if(main.Live_Button_State==OPIPKT_hp.LIVEBUTTONTRANSIENT)
			{
				main.bt_live.setBackgroundResource(R.drawable.livetransient);
			}	
		}
	}
	 
	public static synchronized boolean button_debounce(long starttime,long endtime,int shreshold)
	{
		if((starttime-endtime)>shreshold)
			return true;
		else 
			return false;
	}
	
	public static synchronized void changepage(MainActivity main,boolean hand_L_to_R)
    {
		int pagefrom=main.pagecount;
		main.button_end_time=0;
		main.Button_StateOK=false;
    	if(hand_L_to_R==true) //previous
    	{
			main.appVF.showPrevious();
			main.pagecount--;
			if(main.pagecount<PAGEMIN)
			{
				main.pagecount = PAGEMAX;
			}
			else if(main.pagecount==PAGE_CONFIGURE)
			{
				main.appVF.showPrevious();
				main.pagecount = PAGEMAX;
			}		
    	}
    	else //next
    	{
    		main.appVF.showNext();
    		main.pagecount++;
			if(main.pagecount > PAGEMAX)
			{
				main.appVF.showPrevious();
				main.appVF.showPrevious();
				main.pagecount=PAGE_LIVEMODE;
			}
			else if(main.pagecount==PAGE_CONFIGURE)
			{
				main.appVF.showNext();
				main.pagecount = PAGE_LIVEMODE;
			}
    	}
		if(main.pagecount!=PAGE_CONFIGURE)	
     	{
    		if(main.pagecount==PAGE_LIVEMODE)
    		{
    			main.appVF.showPrevious();
    			main.pagecount=PAGE_CONFIGURE;
    			main.Live_Configure();
    		}
    		else if(main.pagecount==PAGE_FILEMODE)
    		{
    			if(pagefrom==PAGE_LIVEMODE)
    			{
    				main.clean_Live_Screen();
    				for(int i=0;i<main.USBTESTTIMES;i++)//set ont mode
    				{
    				   if(main.OPIPKT_usb.opiuce_offmode()==0)
    				     break;
    				}
    			}
    			main.OPIPKT_edfreader.reset();
    		}
    	}
		
    }
	
	/***
	 * return -1 can't open com
	 * return -2 can't onmode
	 * return -3 can't getrelax
	 * return <-10 from checkconfig
	 * @param main
	 * @return
	 */
	public static synchronized int pairconfig(MainActivity main)
    {
		main.tvRL.setText("RL:0.0");
    	int checkconfig=-1;
    	int checkdevice=-1;
    	int getstatus=-1;
    	int wirelesspdn = -1;
    	int statuspdn = -1;
    	int getwireless=-1;
    	int testtime=10;
        main.SB.setProgress(OPIPKT_helper.DEFAULTRLEVEL);
        for(int i=0;i<testtime;i++)
        { 
        	if(main.OPIPKT_usb.checkDevice(main.manager, main.mdevice)==true)
        	{
        		if(main.OPIPKT_usb.opi_openuce_com()==0)
        		{
        			checkdevice=0;
        			break;
        		}
        	}
        }

        if(checkdevice==0) 
        {
        	  for(int i=0;i<testtime;i++)//set ont mode
              {
        		   checkdevice=main.OPIPKT_usb.opiuce_onmode();
        		   if(checkdevice==0)
              		break;
              }
        	  if(checkdevice!=0)
        		  return -2;
        	  for(int i=0;i<testtime;i++) //get rlevel
              {
        		  checkdevice=main.OPIPKT_usb.opiuce_getrelaxparams(main.OPIPKT_t[0]);
        		  if(checkdevice==0)
              	  {
              		 	// figure out what level is based on thm
              			int tempui16 = 0;
              			tempui16 +=(main.OPIPKT_t[0].payload[4]& 0x000000FF);
              			tempui16 += ((main.OPIPKT_t[0].payload[3]& 0x000000FF) << 8); // thm
              			float rlevelCalc = (float) ((1.7-((float) tempui16)/((float) OPIPKT_helper.THM))*10);
              			main.SB.setProgress((int) (rlevelCalc+0.5));
              			break;
              	  }
              }
        	  if(checkdevice!=0)
        		  return -3;
        	  
        	  
        	  for(int i=0;i<testtime*5;i++)//get wireless
        	  {
        		  main.OPIPKT_t[0].payload[OPIPKT_android.DSNLEN+OPIPKT_android.TSLEN+6+OPIPKT_android.FWVLEN+1+OPIPKT_android.PDNLISTLEN+1] = 0;
        		  if(main.OPIPKT_usb.opiuce_getwltsdata(main.OPIPKT_t[0])==1)
        	      {
        			  	wirelesspdn=main.OPIPKT_t[0].payload[7]& 0x000000FF;
        			  	getwireless=1;
        			  	break;
        		  }
              }
        	  
			  for(int i=0;i<testtime;i++)//get status
              {
               	if(main.OPIPKT_usb.opiuce_status(main.OPIPKT_t[0])==0)
                {
                	getstatus=0;
                	statuspdn = (main.OPIPKT_t[0].payload[OPIPKT_android.DSNLEN+OPIPKT_android.TSLEN+6+OPIPKT_android.FWVLEN+1]& 0x000000FF);
                	break;
           		}
              }
			  
			  //check paired or not
        	  if((getwireless==1)&&
        		 (getstatus==0)&&
        	     (statuspdn == wirelesspdn))  //paired before
        	  {
        		
        		  main.OPIPKT_edfwriter.pdnnum =  statuspdn;
        		  checkconfig=0;
        	  }
        	  else  //not paired before
        	  {
            	 for(int i=0;i<testtime;i++)
                 {
            		checkconfig=main.OPIPKT_hp.opiconfig(main.OPIPKT_t[0], main.OPIPKT_usb);
            		if(checkconfig==0)
            		{
            			main.OPIPKT_edfwriter.pdnnum = main.OPIPKT_hp.mytsepdn;
                     	break;
            		}	
                 }
        	  }
        	  
        	  if(checkconfig==0)
         	  {
         		 	main.configTV.setVisibility(0);
         		    for(int i=0;i<testtime;i++)
 	                {
 	                	if(main.OPIPKT_hp.opigetrelaxdata(main.OPIPKT_t[0],main.OPIPKT_usb,main.tvRL)==0)
 	                		break;
 	                }
 	                for(int i=0;i<testtime;i++)
 	                {
 	                	if(main.OPIPKT_usb.opiuce_resetrelaxdata()==0)
 	                		break;
 	                }	
         	  }
        }
        else
        {
        	checkconfig=-1;
        }
        return checkconfig;
    }
	
	public static synchronized void OPI_default(MainActivity main)
 	{
		main.OPIPKT_drawadchp.reset();
		main.OPIPKT_drawffthp.reset();
		main.OPIPKT_drawsmffthp.reset();
		main.OPIPKT_DT.reset();
		main.OPIPKT_twv.reset();
		main.OPIPKT_hp.reset();
		main.OPIPKT_fft.reset();
 	}
	
	public static synchronized void OPI_reset(MainActivity main)
   	{   
		main.OPIPKT_edfreader=new OPIPKT_EDF_Reader();
		main.OPIPKT_edfreader.initial((TextView)main.findViewById(R.id.bio3TextView2)
    			   , (TextView)main.findViewById(R.id.bio3textView1)
    			   , (TextView)main.findViewById(R.id.bio3filetitle));
		main.OPIPKT_edfwriter = new OPIPKT_EDF_Writer();
		main.OPIPKT_hp = new OPIPKT_helper(main);
		main.OPIPKT_usb = new OPIPKT_android();
		main.OPIPKT_rfio = new OPIPKT_Raw_File();
		main.OPIPKT_drawadchp = new OPIPKT_DrawHelper();
		main.OPIPKT_drawffthp = new OPIPKT_DrawHelper();
		main.OPIPKT_drawsmffthp = new OPIPKT_DrawHelper();
		main.OPIPKT_DT= new OPIPKT_Data_Converter();
		main.OPIPKT_t=new OPIPKT_struct[2];
		main.OPIPKT_t[0] = new OPIPKT_struct();
		main.OPIPKT_t[1] = new OPIPKT_struct();
		main.OPIPKT_twv = new OPIPKT_Accel_Viewer(); 
		main.OPIPKT_fft = new OPIPKT_FFT();
   	}
	
	public static synchronized void Local_reset(MainActivity main)
    {

 	   main.usbPermissionGranted = -1;
 	   main.pagecount=0;
 	   main.Button_StateOK=false;
 	   main.button_end_time=0;
 	   main.button_start_time=0;
 	   main.Touch_debouncetime=0;
 	   main.lockScreen=false;
 	   main.Live_Is_Recording=false;
 	   main.gifsurface = (SurfaceView) main.findViewById(R.id.gifsurfaceView);
 	   main.gifsurface.setZOrderOnTop(true);
 	   main.gifsurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);
 	   main.bt_live = (Button) main.findViewById(R.id.button1);
 	   main.bt_live.setAlpha(1f);
 	   main.bt_file = (Button) main.findViewById(R.id.bio3button1);
 	   main.bt_file.setAlpha(1f);
 	   main.tvStartdate = (TextView) main.findViewById(R.id.textView1);
 	   main.tvStartdate.setText("Start date/time:");
 	   main.tvLevel = (TextView) main.findViewById(R.id.TextView2);
 	   main.tvLevel.setText("LEVEL:2");
 	   main.tvRL = (TextView) main.findViewById(R.id.textView3);
 	   main.tvRL.setText("RL:0.0");
 	   main.LL = (LinearLayout) main.findViewById(R.id.LLayout);
 	   main.LL.setOrientation(LinearLayout.VERTICAL);
 	   main.fileLL = (LinearLayout) main.findViewById(R.id.bio3LLayout);
 	   main.fileLL.setOrientation(LinearLayout.VERTICAL);
 	   main.SB = (SeekBar) main.findViewById(R.id.seekBar1);
 	   main.appVF = (ViewFlipper) main.findViewById(R.id.appVF);
 	   main.appVF.setInAnimation(main, R.anim.fadein);
 	   main.appVF.setOutAnimation(main, R.anim.fadeout);
 	   main.configTV = (TextView) main.findViewById(R.id.configTV);
 	   main.configPB = (ProgressBar) main.findViewById(R.id.configPB);
       DisplayMetrics dm = new DisplayMetrics();
       main.getWindowManager().getDefaultDisplay().getMetrics(dm);
       screenwidth = dm.widthPixels;
       screenheigth = dm.heightPixels;
       if(screenheigth>=OPIPKT_helper.SCREENHEIGH) main.bigscreen=true;
       else main.bigscreen=false;
       OPIPKT_helper.setSurfaceLayout(main,main.bigscreen);	
       OPIPKT_helper.setFileSurfaceLayout(main,main.bigscreen);
    }

	public static synchronized void setFileSurfaceLayout(MainActivity main,Boolean bigscreen)
	{
		if(bigscreen==true)
       	{
       	 //adc
       	 LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
       	 params1.height = 75;
   	     params1.width  = -2;
   	     params1.weight = (float) 0.3;
   	     main.fileadcsurface =new SurfaceView(main);
   	     main.fileadcsurface.setZOrderOnTop(true);
   	     main.fileadcsurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);
   	     main.fileadcsurface.setId(1);
   	     main.fileadcsurface.setLayoutParams(params1);
   		 main.fileLL.addView(main.fileadcsurface);
   		 main.fileadcholder = main.fileadcsurface.getHolder(); 
   		 //smfft
   		 LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
       	 params2.height = 10;
   	     params2.width  = -2;
   	     params2.weight = (float) 0.04;
   	     main.filesmfftsurface =new SurfaceView(main);
   	     main.filesmfftsurface.setZOrderOnTop(true);
   	     main.filesmfftsurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);
   	     main.filesmfftsurface.setId(2);
   	     main.filesmfftsurface.setLayoutParams(params2);
   		 main.fileLL.addView(main.filesmfftsurface);
   		 main.filesmfftholder = main.filesmfftsurface.getHolder();
   		 //fft
   		 LinearLayout.LayoutParams params3 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
       	 params3.height = 90;
   	     params3.width  = -2;
   	     params3.weight = (float) 0.36;
   	     main.filefftsurface =new SurfaceView(main);
   	     main.filefftsurface.setZOrderOnTop(true);
   	     main.filefftsurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);
   	     main.filefftsurface.setId(3);
   	     main.filefftsurface.setLayoutParams(params3);
   		 main.fileLL.addView(main.filefftsurface);
   		 main.filefftholder = main.filefftsurface.getHolder();
   		 //postureviewer
   		 LinearLayout.LayoutParams params4 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
       	 params4.height = 50;
   	     params4.width  = -2;
   	     params4.weight = (float) 0.2;
   	     main.fileposturesurface =new SurfaceView(main);
   	     main.fileposturesurface.setZOrderOnTop(true);
   	     main.fileposturesurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);
   	     main.fileposturesurface.setId(4);
   	     main.fileposturesurface.setLayoutParams(params4);
   		 main.fileLL.addView(main.fileposturesurface);
   		 main.filepostureholder = main.fileposturesurface.getHolder();
   		 //posturezviewer
   		 LinearLayout.LayoutParams params5 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
       	 params5.height = 25;
   	     params5.width  = -2;
   	     params5.weight = (float) 0.1;
   	     main.fileposturezsurface =new SurfaceView(main);
   	     main.fileposturezsurface.setZOrderOnTop(true);
   	     main.fileposturezsurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);
   	     main.fileposturezsurface.setId(5);
   	     main.fileposturezsurface.setLayoutParams(params5);
   		 main.fileLL.addView(main.fileposturezsurface);
   		 main.fileposturezholder = main.fileposturezsurface.getHolder();
       	}
       	else
       	{
       	
      		 //smfft
      		 LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
          	 params2.height = 25;
      	     params2.width  = -2;
      	     params2.weight = (float) 0.1;
      	     main.filesmfftsurface =new SurfaceView(main);
      	     main.filesmfftsurface.setZOrderOnTop(true);
      	     main.filesmfftsurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);
      	     main.filesmfftsurface.setId(1);
      	     main.filesmfftsurface.setLayoutParams(params2);
      		 main.fileLL.addView(main.filesmfftsurface);
      		 main.filesmfftholder = main.filesmfftsurface.getHolder();
      		 //postureviewer
      		 LinearLayout.LayoutParams params4 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
          	 params4.height = 125;
      	     params4.width  = -2;
      	     params4.weight = (float) 0.5;
      	     main.fileposturesurface =new SurfaceView(main);
      	     main.fileposturesurface.setZOrderOnTop(true);
      	     main.fileposturesurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);
      	     main.fileposturesurface.setId(2);
      	     main.fileposturesurface.setLayoutParams(params4);
      		 main.fileLL.addView(main.fileposturesurface);
      		 main.filepostureholder = main.fileposturesurface.getHolder();
      		 //posturezviewer
      		 LinearLayout.LayoutParams params5 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
          	 params5.height = 100;
      	     params5.width  = -2;
      	     params5.weight = (float) 0.4;
      	     main.fileposturezsurface =new SurfaceView(main);
      	     main.fileposturezsurface.setZOrderOnTop(true);
      	     main.fileposturezsurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);
      	     main.fileposturezsurface.setId(3);
      	     main.fileposturezsurface.setLayoutParams(params5);
      		 main.fileLL.addView(main.fileposturezsurface);
      		 main.fileposturezholder = main.fileposturezsurface.getHolder();
       	}
	}
	
	public static synchronized void setSurfaceLayout(MainActivity main,Boolean bigscreen)
	{
		if(bigscreen==true)
       	{
       	 //adc
       	 LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
       	 params1.height = 75;
   	     params1.width  = -2;
   	     params1.weight = (float) 0.3;
   	     main.adcsurface =new SurfaceView(main);
   	     main.adcsurface.setZOrderOnTop(true);
   	     main.adcsurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);
   	     main.adcsurface.setId(1);
   	     main.adcsurface.setLayoutParams(params1);
   		 main.LL.addView(main.adcsurface);
   		 main.adcholder = main.adcsurface.getHolder(); 
   		 //smfft
   		 LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
       	 params2.height = 10;
   	     params2.width  = -2;
   	     params2.weight = (float) 0.04;
   	     main.smfftsurface =new SurfaceView(main);
   	     main.smfftsurface.setZOrderOnTop(true);
   	     main.smfftsurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);
   	     main.smfftsurface.setId(2);
   	     main.smfftsurface.setLayoutParams(params2);
   		 main.LL.addView(main.smfftsurface);
   		 main.smfftholder = main.smfftsurface.getHolder();
   		 //fft
   		 LinearLayout.LayoutParams params3 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
       	 params3.height = 90;
   	     params3.width  = -2;
   	     params3.weight = (float) 0.36;
   	     main.fftsurface =new SurfaceView(main);
   	     main.fftsurface.setZOrderOnTop(true);
   	     main.fftsurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);
   	     main.fftsurface.setId(3);
   	     main.fftsurface.setLayoutParams(params3);
   		 main.LL.addView(main.fftsurface);
   		 main.fftholder = main.fftsurface.getHolder();
   		 //postureviewer
   		 LinearLayout.LayoutParams params4 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
       	 params4.height = 50;
   	     params4.width  = -2;
   	     params4.weight = (float) 0.2;
   	     main.posturesurface =new SurfaceView(main);
   	     main.posturesurface.setZOrderOnTop(true);
   	     main.posturesurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);
   	     main.posturesurface.setId(4);
   	     main.posturesurface.setLayoutParams(params4);
   		 main.LL.addView(main.posturesurface);
   		 main.postureholder = main.posturesurface.getHolder();
   		 //posturezviewer
   		 LinearLayout.LayoutParams params5 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
       	 params5.height = 25;
   	     params5.width  = -2;
   	     params5.weight = (float) 0.1;
   	     main.posturezsurface =new SurfaceView(main);
   	     main.posturezsurface.setZOrderOnTop(true);
   	     main.posturezsurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);
   	     main.posturezsurface.setId(5);
   	     main.posturezsurface.setLayoutParams(params5);
   		 main.LL.addView(main.posturezsurface);
   		 main.posturezholder = main.posturezsurface.getHolder();
       	}
       	else
       	{
       	
      		 //smfft
      		 LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
          	 params2.height = 25;
      	     params2.width  = -2;
      	     params2.weight = (float) 0.1;
      	     main.smfftsurface =new SurfaceView(main);
      	     main.smfftsurface.setZOrderOnTop(true);
      	     main.smfftsurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);
      	     main.smfftsurface.setId(1);
      	     main.smfftsurface.setLayoutParams(params2);
      		 main.LL.addView(main.smfftsurface);
      		 main.smfftholder = main.smfftsurface.getHolder();
      		 //postureviewer
      		 LinearLayout.LayoutParams params4 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
          	 params4.height = 125;
      	     params4.width  = -2;
      	     params4.weight = (float) 0.5;
      	     main.posturesurface =new SurfaceView(main);
      	     main.posturesurface.setZOrderOnTop(true);
      	     main.posturesurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);
      	     main.posturesurface.setId(2);
      	     main.posturesurface.setLayoutParams(params4);
      		 main.LL.addView(main.posturesurface);
      		 main.postureholder = main.posturesurface.getHolder();
      		 //posturezviewer
      		 LinearLayout.LayoutParams params5 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
          	 params5.height = 100;
      	     params5.width  = -2;
      	     params5.weight = (float) 0.4;
      	     main.posturezsurface =new SurfaceView(main);
      	     main.posturezsurface.setZOrderOnTop(true);
      	     main.posturezsurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);
      	     main.posturezsurface.setId(3);
      	     main.posturezsurface.setLayoutParams(params5);
      		 main.LL.addView(main.posturezsurface);
      		 main.posturezholder = main.posturezsurface.getHolder();
       	}
	}
	

	
	public synchronized void reset()
	{
		configFailFlg=false;
		configuredflag=false;
		
	}
	public static synchronized void calculatefft(OPIPKT_DrawHelper dhp,int []signal,int adclength)
	{
			//save data	
		    for(int k=0;k<adclength;k++)
			{
		    	dhp.savedDataForFFT[dhp.countsavedfft]= (double)signal[k];
		    	//do fft if data length is enough
		    	if(dhp.countsavedfft==(OPIPKT_DrawHelper.NUMPATHFFT-1))
		    	{	
		    		int allsize = dhp.countsavedfft;
		    		for(int i=0;i<OPIPKT_DrawHelper.NUMPATHFFT;i++)
		 			{
		    			 dhp.temparrayforfft[i] = dhp.savedDataForFFT[i] * dhp.hannwindow[i];  //learn form qt
		    			 dhp.imagey[i] = 0;
		    			 dhp.countsavedfft--;
		 			}
		    		// fft
		    		dhp.calculatedFFT =OPIPKT_FFT.FFT(1,EXPNFFT, dhp.temparrayforfft, dhp.imagey);
		    		dhp.fft_is_ready=true;
		    		if(dhp.countfftcolumn<dhp.numpathfftcol)
		    			dhp.countfftcolumn++;
		    		else
		    		{
		    			dhp.countfftcolumn=1;
		    		}
		        	for (int j = 0; j < OPIPKT_DrawHelper.NUMPATHFFT/2 ; j ++) 
		    		{
		    		    //real
		        		dhp.calculatedFFT[j]=dhp.calculatedFFT[j]*OPIPKT_DrawHelper.NUMPATHFFT;
		    		    dhp.re = dhp.calculatedFFT[j]*dhp.calculatedFFT[j];
		    		    //image
		    		    dhp.calculatedFFT[j+OPIPKT_DrawHelper.NUMPATHFFT]=dhp.calculatedFFT[j+OPIPKT_DrawHelper.NUMPATHFFT]*OPIPKT_DrawHelper.NUMPATHFFT;
		    		    dhp.im = dhp.calculatedFFT[j+OPIPKT_DrawHelper.NUMPATHFFT]*dhp.calculatedFFT[j+OPIPKT_DrawHelper.NUMPATHFFT];
		    		    //magnitude
		    		    dhp.ffttemp = dhp.re+dhp.im;	
		    		    if(dhp.ffttemp<=0) // can't log 0 or negative numbers
		    		    dhp.effected_by_ko_co_fft[j] =1;
		    		    else
		    		    dhp.effected_by_ko_co_fft[j] = OPIPKT_DrawHelper.LIVEFFTDBGAIN*(10*Math.log10(dhp.ffttemp*(j+1))-70- OPIPKT_DrawHelper.LIVEFFTDBOFF);    
		    		}
			        	for(int z=OPIPKT_DrawHelper.NUMPATHFFT/2;z<allsize;z++)
			        	{
			        		dhp.savedDataForFFT[z-OPIPKT_DrawHelper.NUMPATHFFT/2] =  dhp.temparrayforfft[z]; //dhp.savedDataForFFT[z];
			        		
			        	}
			        	dhp.countsavedfft=OPIPKT_DrawHelper.NUMPATHFFT/2-1;
		    	}//if end
		    	dhp.countsavedfft++;
			}
		        	
}
	
	
	
	
	public static synchronized void opiwait(long millis)
	{
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	
	public synchronized int opigetrelaxdata(OPIPKT_struct ucOpipkt,OPIPKT_android OPIPKT_usb,TextView tv)
	{
		
		OPIPKT_struct relax1Opipkt = new OPIPKT_struct();
		long startTS;
		int relSt_accum,relSt_pktCt;
		long startQDT;
		String refDT;
		// assumes port is open already
	    if(OPIPKT_usb.opiuce_status(ucOpipkt)!=0)
	    {
	        OPIPKT_helper.opiwait(COMRETRYTIME);
		    if(OPIPKT_usb.opiuce_status(ucOpipkt)!=0)
	        {
	            return -1; // get out if this didn't open
	        }
	    }
	    mytsepdn = ucOpipkt.payload[OPIPKT_android.DSNLEN+OPIPKT_android.TSLEN+6+OPIPKT_android.FWVLEN+1]&0x000000FF;
	    if(OPIPKT_usb.opiuce_getrelaxdata(relax1Opipkt)!=0)
	    {
	    	OPIPKT_helper.opiwait(COMRETRYTIME);
	    	 if(OPIPKT_usb.opiuce_getrelaxdata(relax1Opipkt)!=0)
	        {
	            return -1; // get out if this didn't open
	        }
	    }
	    
	    startTS=0;
	    for(int i=0;i<6;i++)
	    	startTS = (startTS<<8) | relax1Opipkt.payload[1+i];
	    //startQDT = DateTime.fromMSecsSinceEpoch(startTS*1000/UCERTCFREQ+refDT.toMSecsSinceEpoch());
	    relSt_accum=0;
	    relSt_accum =  (relSt_accum<<8) | relax1Opipkt.payload[7];
	    relSt_accum =  (relSt_accum<<8) | relax1Opipkt.payload[8];
	    relSt_accum =  (relSt_accum<<8) | relax1Opipkt.payload[9];
	    relSt_accum =  (relSt_accum<<8) | relax1Opipkt.payload[10];
	    
	    relSt_pktCt = 0;
	    relSt_pktCt = (relSt_pktCt<<8) | relax1Opipkt.payload[11];
	    relSt_pktCt = (relSt_pktCt<<8) | relax1Opipkt.payload[12];
	    relSt_pktCt = (relSt_pktCt<<8) | relax1Opipkt.payload[13];
	    relSt_pktCt = (relSt_pktCt<<8) | relax1Opipkt.payload[14];
	    if(relSt_pktCt < RLSCOREMINPKTS) 
	    	return -1;
	    tv.setText("RL:"+Integer.toString(relSt_accum/relSt_pktCt) +"."+Integer.toString((relSt_accum*100)/relSt_pktCt-(relSt_accum/relSt_pktCt)*100));
		return 0;
	}
	public  synchronized int opistart(OPIPKT_struct opipkt,OPIPKT_android OPIPKT_usb,OPIPKT_FFT OPIPKT_fft,int vofrlevel)
	{
		if(OPIPKT_usb.opi_openuce_com()!=0)   // open com port, should open unless device was removed
        {
            if(OPIPKT_usb.opi_openuce_com()!=0)
            return -1; // get out if this didn't open
        }
        if(this.setUCETime(OPIPKT_usb)!=0)    // make sure time is right before starting
        {
            if(this.setUCETime(OPIPKT_usb)!=0) 
            return -1; // get out if this didn't open
        }
        OPIPKT_usb.opiuce_settsertc();  // propagate time set to tse
        if(OPIPKT_usb.opiuce_status(opipkt)!=0)
        {
        	if(OPIPKT_usb.opiuce_status(opipkt)!=0)
            return -1; // get out if this didn't open
        }

        if((opipkt.payload[20]&0xff) == 0xFF)    // if not paired, then get out
        {
        	if((opipkt.payload[20]&0xff) == 0xFF)
            return -1; // get out if this didn't open
        }
        OPIPKT_fft.tsepdn = opipkt.payload[20]; // only one

        // calculate algorithm parameters
        OPIPKT_fft.rlevel = vofrlevel;
        OPIPKT_fft.thxx = OPIPKT_helper.THX;
        OPIPKT_fft.thmm = OPIPKT_helper.THM;
        OPIPKT_fft.thgm = OPIPKT_helper.THGM;
        OPIPKT_fft.thbm = OPIPKT_helper.THBM;
        OPIPKT_fft.offll = OPIPKT_helper.OFFL;
        OPIPKT_fft.offm = OPIPKT_helper.OFFM;

        //use rlevel 0~10 to calculate parameters: AttN invert from ReLax
        OPIPKT_fft.rlsm = (float) (1.7 - ((float)OPIPKT_fft.rlevel)/10); //2.43X 0.7~1.7 for thm
        OPIPKT_fft.rls1 = (float) (1.5 - ((float)OPIPKT_fft.rlevel)/12.5); //2.14X  0.7~1.5
        OPIPKT_fft.rls2 = (float) (OPIPKT_fft.rls1 * 1.5); //2*rls1
        OPIPKT_fft.rls3 = (float) (OPIPKT_fft.rls1 * 2.4); //3*rls1
        OPIPKT_fft.thx = OPIPKT_fft.thxx; //M exclusion threshold
        OPIPKT_fft.thm = (int) (OPIPKT_fft.thmm*OPIPKT_fft.rlsm);
        OPIPKT_fft.offl = (int) (OPIPKT_fft.offll*OPIPKT_fft.rlsm);
        OPIPKT_fft.th3gm=(int) (OPIPKT_fft.thgm*OPIPKT_fft.rls3);
        OPIPKT_fft.th3bm=(int) (OPIPKT_fft.thbm*OPIPKT_fft.rls3);
        OPIPKT_fft.th2gm=(int) (OPIPKT_fft.thgm*OPIPKT_fft.rls2);
        OPIPKT_fft.th2bm=(int) (OPIPKT_fft.thbm*OPIPKT_fft.rls2);
        OPIPKT_fft.th1gm=(int) (OPIPKT_fft.thgm*OPIPKT_fft.rls1);
        OPIPKT_fft.th1bm=(int) (OPIPKT_fft.thbm*OPIPKT_fft.rls1);
        if( OPIPKT_usb.opiuce_setrelaxparams( OPIPKT_fft.thx, OPIPKT_fft.thm, OPIPKT_fft.offl, OPIPKT_fft.th3gm, OPIPKT_fft.th3bm, OPIPKT_fft.th2gm, OPIPKT_fft.th2bm, OPIPKT_fft.th1gm, OPIPKT_fft.th1bm, OPIPKT_fft.offm)!=0)
        {
        	int count=0;
            for(count=0;count<50;count++)
            {
            	if(OPIPKT_usb.opiuce_setrelaxparams( OPIPKT_fft.thx, OPIPKT_fft.thm, OPIPKT_fft.offl, OPIPKT_fft.th3gm, OPIPKT_fft.th3bm, OPIPKT_fft.th2gm, OPIPKT_fft.th2bm, OPIPKT_fft.th1gm, OPIPKT_fft.th1bm, OPIPKT_fft.offm)==0)
            		break;
            	OPIPKT_helper.opiwait(500);
            }
            if(count==50)
            return -1;// get out if this didn't open
        }
        if(OPIPKT_usb.opiuce_getrelaxparams(opipkt)!=0)
        {
        	int count=0;
            for(count=0;count<50;count++)
            {
            	if(OPIPKT_usb.opiuce_getrelaxparams(opipkt)==0)
            		break;
            	OPIPKT_helper.opiwait(500);
            }
        	if(count==50)
            return -1; // get out if this didn't open
        }
        OPIPKT_fft.thx=0;
        OPIPKT_fft.thx = OPIPKT_fft.thx+ (opipkt.payload[2]& 0x000000FF);
        OPIPKT_fft.thx = OPIPKT_fft.thx+ ((opipkt.payload[1]& 0x000000FF) <<8);
        OPIPKT_fft.thm=0;
        OPIPKT_fft.thm= OPIPKT_fft.thm+ (opipkt.payload[4]& 0x000000FF);
        OPIPKT_fft.thm = OPIPKT_fft.thm+ ((opipkt.payload[3]& 0x000000FF) <<8) ;
        OPIPKT_fft.offl=0;
        OPIPKT_fft.offl= OPIPKT_fft.offl+ (opipkt.payload[6]& 0x000000FF);
        OPIPKT_fft.offl = OPIPKT_fft.offl+ ((opipkt.payload[5]& 0x000000FF) <<8) ;
        OPIPKT_fft.th3gm=0;
        OPIPKT_fft.th3gm = OPIPKT_fft.th3gm+ (opipkt.payload[8]& 0x000000FF);
        OPIPKT_fft.th3gm = OPIPKT_fft.th3gm+ ((opipkt.payload[7]& 0x000000FF) <<8) ;
        OPIPKT_fft.th3bm=0;
        OPIPKT_fft.th3bm = OPIPKT_fft.th3bm  +(opipkt.payload[10]& 0x000000FF);
        OPIPKT_fft.th3bm = OPIPKT_fft.th3bm  + ((opipkt.payload[9]& 0x000000FF) <<8);
        OPIPKT_fft.th2gm=0;
        OPIPKT_fft.th2gm=OPIPKT_fft.th2gm+( opipkt.payload[12]& 0x000000FF);
        OPIPKT_fft.th2gm=OPIPKT_fft.th2gm+((opipkt.payload[11]& 0x000000FF) <<8);
        OPIPKT_fft.th2bm=0;
        OPIPKT_fft.th2bm=OPIPKT_fft.th2bm +( opipkt.payload[14]& 0x000000FF);
        OPIPKT_fft.th2bm = OPIPKT_fft.th2bm +((opipkt.payload[13]& 0x000000FF) <<8);
        OPIPKT_fft.th1gm=0;
        OPIPKT_fft.th1gm=OPIPKT_fft.th1gm+( opipkt.payload[16]& 0x000000FF);
        OPIPKT_fft.th1gm =OPIPKT_fft.th1gm+((opipkt.payload[15]& 0x000000FF) <<8) ;
        OPIPKT_fft.th1bm=0;
        OPIPKT_fft.th1bm=OPIPKT_fft.th1bm + (opipkt.payload[18]& 0x000000FF);
        OPIPKT_fft.th1bm =OPIPKT_fft.th1bm + ((opipkt.payload[17]& 0x000000FF) <<8);
        OPIPKT_fft.offm=0;
        OPIPKT_fft.offm=OPIPKT_fft.offm+(opipkt.payload[20]& 0x000000FF);
        OPIPKT_fft.offm =OPIPKT_fft.offm+((opipkt.payload[19]& 0x000000FF) <<8) ;
        return 0;
	}
	
	public  synchronized int opiconfig(OPIPKT_struct opipkt,OPIPKT_android OPIPKT_usb)
	{
		 //start to config
	     int i=0;
	     resMinInd =0;
	     int check=-1;
	     // initialize data in case there are errors later
	     opipkt.payload[OPIPKT_android.DSNLEN
	                    +OPIPKT_android.TSLEN
	                    +6+OPIPKT_android.FWVLEN
	                    +1+OPIPKT_android.PDNLISTLEN+1] = 0;
	     
	     configFailFlg = false;
		 if(OPIPKT_usb.opi_openuce_com()!=0){ 
			  configFailFlg = true;
			  return -10;
		 }
			 
		 if(OPIPKT_usb.opiuce_onmode()!=0)
		 {
			opiwait(COMRETRYTIME);
			if(OPIPKT_usb.opiuce_onmode()!=0)//try again
			{
				configFailFlg = true;
				 return -11;
			}
		 }
			 
			 
		 opiwait(1100); // wait so that ts has a chance to startup qt 198
		      
		   if(OPIPKT_usb.opiuce_evcaperase()!=0)
				  configFailFlg = true;
		      
			  if(OPIPKT_usb.opiuce_status(opipkt)!=0)
			  {
				  if(OPIPKT_usb.opiuce_status(opipkt)!=0)
				  {
		    	      configFailFlg = true;
		    	      return -12;
				  }
			  }
			  
		      ucusdStatus = (opipkt.payload[OPIPKT_android.DSNLEN
		                                    +OPIPKT_android.TSLEN
		                                    +6+OPIPKT_android.FWVLEN
		                                    +1+OPIPKT_android.PDNLISTLEN+1])& 0x000000FF;//qt 214
		      
		      
		      if(((ucusdStatus & 0x01)&0xff)==0x01) // if ts in
		      {
		    	  // erase settings
		    	  if(OPIPKT_usb.opiuce_forgettsesettings(0)!=0)//218
		    	  {
		    		  opiwait(COMRETRYTIME); // 
		    		  if(OPIPKT_usb.opiuce_forgettsesettings(0)!=0)
		    		  {
		    			   configFailFlg = true;
		    			   return -13;
		    		  }
		    	  }
		    	  
		    	  // default tse to regular settings
		    	  if(OPIPKT_usb.opiuce_tsestatus(opipkt)!=0)//qt 230
		    	  {
		    		  configFailFlg = true;
		    		  return -14;
		    	  }
		    	  
		    	  // pdn can never be 255, will cause problems with controller
		    	  mytsepdn = (opipkt.payload[1+OPIPKT_android.DSNLEN+5+OPIPKT_android.FWVLEN]&0x000000FF);
		    	  
		    	  if(mytsepdn==255)
                  {
                      tsedsn = (0x000000FF&opipkt.payload[1]);
                     
                      if(OPIPKT_usb.opiuce_settsepdn((int) (tsedsn%255))!=0)
                      {
                    	  opiwait(COMRETRYTIME); // 
                    	  if(OPIPKT_usb.opiuce_settsepdn((int) (tsedsn%255))!=0)
                          {
                    		  configFailFlg = true;
                    		  return -15;
                          }
                      }
                  }
		    	  
		    	 
		    	  if(OPIPKT_usb.opiuce_settserfmode(1)!=0) // default RF mode //255
                  {
		    		  opiwait(COMRETRYTIME); // 
		    		  if(OPIPKT_usb.opiuce_settserfmode(1)!=0) // default RF mode
                      {
		    			  configFailFlg = true;
		    			  return -16;
                      }
                  }
		    	  
		    	  if(OPIPKT_usb.opiuce_settserftxpwr(7)!=0) // default RF TX pwr //qt 266
                  {
		    		  opiwait(COMRETRYTIME); // 
		    		  if(OPIPKT_usb.opiuce_settserftxpwr(7)!=0) // default RF TX pwr
                      {
		    			  configFailFlg = true;
		    			  return -17;
                      }
                  }
		    	  
		    	  
		    	  if(OPIPKT_usb.opiuce_settsemmwrite(0)!=0) // default memory module write state //qt 277
                  {
		    		  opiwait(COMRETRYTIME); // 
		    		  if(OPIPKT_usb.opiuce_settsemmwrite(0)!=0)  // default memory module write state
                      {
		    			  configFailFlg = true;
		    			  return -18;
                      }
                  }
		    	  
		    	  opiwait(COMRETRYTIME); 
		    	  
		    	 for( i = 0; i < OPIPKT_android.ZBWLCHANCT; i++)
                 {
                    if(OPIPKT_usb.opiuce_setzbchan(i+11)!=0)
                    {
                        opiwait(COMRETRYTIME); //
                        if(OPIPKT_usb.opiuce_setzbchan(i+11)!=0)
                        {
                          configFailFlg = true;
                          return -19;
                        }
                    }
                    resWLScan[i] = maxWLMeasure100(OPIPKT_usb);
                 }
		    	 
		    
                  resMinInd = 0;  //qt 308
                  for(i = 0; i < OPIPKT_android.ZBWLCHANCT; i++)
                  {
                      if((resWLScan[i]) >= 0) // measure must have been successful
                      {
                          resMinInd = i;
                          break;
                      }
                  }
                  for(; i < OPIPKT_android.ZBWLCHANCT; i++)
                  {
                      if(((resWLScan[i]) >= 0) && ((resWLScan[i]) <= (resWLScan[resMinInd])))
                      {
                          resMinInd = i;
                      }
                  }
                  
                  bestzbChan =  (resMinInd + 11);
                  
               // set zigbee channel
                  if(OPIPKT_usb.opiuce_setzbchan(bestzbChan)!=0)
                  {
                	  opiwait(COMRETRYTIME); //
                      if(OPIPKT_usb.opiuce_setzbchan(bestzbChan)!=0)
                      {
                    	  configFailFlg = true;
                    	  return -20;
                      }
                  }
                  
                  if(OPIPKT_usb.opiuce_settsezbchan(bestzbChan)!=0)
                  {
                	  opiwait(COMRETRYTIME); //
                      if(OPIPKT_usb.opiuce_settsezbchan( bestzbChan)!=0)
                      {
                    	  configFailFlg = true;
                    	  return -21;
                      }
                  }
               // remember tse in uce
                  if(OPIPKT_usb.opiuce_copytsesettings(0)!=0)
                  {
                	  opiwait(COMRETRYTIME); //
                      if(OPIPKT_usb.opiuce_copytsesettings(0)!=0)
                      {
                    	  configFailFlg = true;
                    	  return -22;
                      }
                  }                
                  //OPIPKT_usb.opi_closeuce_com();  //qt 373
		      }
		      else   //if ucusdStatus==0
		      {
		    	 configFailFlg=true;
		    	 return -23;
		      }
		      return 0;
	}
	
	/***
	  *	Set the Controller time to current time.
	  * Assumes the comport has already been opened by SDK.
	  *	Inputs:
	  *		comportptr, pointer to handle
	  *	Returns:
	  *      0, if successful
	  *      -1, if error
	  */
	public synchronized int setUCETime(OPIPKT_android   OPIPKT_usb)
	{
	    long ucSetTS=0,ucRefEpochMSecs;
	    long refDT= Long.valueOf(13487904*100000);
	    int timeStamp[]= new int[6];
	    ucRefEpochMSecs = System.currentTimeMillis()- refDT;
	    ucSetTS = ucRefEpochMSecs*OPIPKT_android.UCERTCFREQ/1000;
	    // Set Timestamp, need to correct and put correct date time in
	    timeStamp[0] =   (int) ((ucSetTS>>40)&0x000000FF);
	    timeStamp[1] =  (int) ((ucSetTS>>32)&0x000000FF);
	    timeStamp[2] =  (int) ((ucSetTS>>24)&0x000000FF);
	    timeStamp[3] =  (int) ((ucSetTS>>16)&0x000000FF);
	    timeStamp[4] =  (int) ((ucSetTS>>8)&0x000000FF);
	    timeStamp[5] =  (int) ((ucSetTS>>0)&0x000000FF);
	    if(OPIPKT_usb.opiuce_setpktts(timeStamp)!=0) return -1;
	    else return 0;
	}
	
	/***
	  *	Use the Controller to take 100 measurements of current ZigBee Channel
	  *  returning the maximum peak Energy Detected.
	  *  Assumes the comport has already been opened by SDK.
	  *	Inputs:
	  *		comportptr, pointer to handle
	  *	Returns:
	  *      non-negative integer representing maximum peak Energy Detected
	  *      -1, if error
	  */
	public synchronized int maxWLMeasure100(OPIPKT_android   OPIPKT_usb)
	{
		OPIPKT_struct opipkt = new OPIPKT_struct();
		 int i;
		 int maxED;
		 int temp;
	    maxED = 0;
	    for(i = 0; i < 100; i++)
	    {
	    	opiwait(COMRETRYTIME); 
	    	temp = OPIPKT_usb.opiuce_wlmeasure(opipkt);
	        if(temp!=0)
	        {
	            return (-1*i);
	        }
	        if( (0x000000FF&opipkt.payload[2]) > (maxED)) 
	        	maxED = (0x000000FF&opipkt.payload[2]);
	    }
	    return (maxED);
	}
	
	public static synchronized int edfDBytestToInt(byte [] buff)
	{
		int temp=0;
		for(int i=buff.length-1;i>=0;i--)
		{
			temp = (temp<<8) | (buff[i]&0xFF);
		}
		if(temp>32767) temp=temp-65536;
		return temp;
	}
	
	public static synchronized int edfHeaderBytesToInt(byte[] a)
	{
		int value=0;
		int notspacecount=0;
		for(int i=a.length-1;i>=0;i--)
		{
			if((a[i]!=32)&&(a[i]>45))
			{
				value = (int) (value + (a[i]-48)*Math.pow(10, notspacecount));
				notspacecount++;
			}
			else if(a[i]==45)
			{
				value = value*-1;
			}
		}
		return value;
	}
	
	
	
	public static synchronized byte[] leftJustified_byte(char[] original,int width,char fill)
	{
		byte[] output=new byte[width];
		for(int i=0;i<width;i++)
		{
			if(i<original.length)
				output[i]=(byte) original[i];
			else
				output[i]=(byte) fill;
		}
		return output;
	}
	

	public static synchronized byte[] int16tobyte(int a)
	{
		byte [] inttemp  = new byte[2];
		inttemp[1]= ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(a).get(1);
		inttemp[0]= ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(a).get(0);
		return inttemp;
	}
}
