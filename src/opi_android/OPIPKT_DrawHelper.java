package opi_android;

import java.util.Vector;

import org.achartengine.GraphicalView;

import opi.relax.opirelax.MainActivity;
import opi.relax.opirelax.R;
import opi.relax.opirelax.MainActivity.ConfigUce;

import android.app.ProgressDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

public class OPIPKT_DrawHelper {
	
	public boolean clean_z=false;
	
    public MainActivity main;
	// canvas
	public final static int DRAWADCMAX = 8000;
	public final static int DRAWADCMIN = -8000;
	public final static int SIGNALZOOMRATIOINIT = 1;
	public float nowy,beforey;
	public int counter;
	public float signalZoomRatio;
	public boolean firstdraw;

	// achartengine
	public boolean ACHRTALREADYSET;
	public GraphicalView view;
	public double timems; 
	public int left;
	
	
	//fft & score(small fft)
	public final static float FFTLEDWIDTH = 0.5f; // 1/4 of FFTPENWIDTH
	public final static double PI=3.14159265359;
	public final static int LIVEFFTDBGAIN = 8, LIVEFFTDBOFF = -2;
	public final static int NUMPATHFFT =512;
	public final static int FFTPENWIDTH =2;
	public int numScalefft = NUMPATHFFT/2;  //256 bins, half of 256Hz BW
	public final static int FFTSIGNALMAX = 254;
	public final static int FFTSIGNALMIN = 1;
	public int countsavedfft = NUMPATHFFT/2;
	double []imagey= new double[NUMPATHFFT];
	double []savedDataForFFT=new double[NUMPATHFFT];	
	double []calculatedFFT = new double[NUMPATHFFT*2];
	double []hannwindow= new double[NUMPATHFFT];
	double []temparrayforfft=new double[NUMPATHFFT];
	double re,im,ffttemp;
	double [] effected_by_ko_co_fft= new double[NUMPATHFFT/2+1];
	double [] increase_log = new double[NUMPATHFFT/2+1];
	public int countfftcolumn;
	public int numpathfftcol=50;  //will be initialize by Live_drawAll()
	public double fft_zoomrate; 
	boolean fft_is_ready;
    public double fftnowvalue;
    //for file mode
    public static int index_begin=0; //index, reference by x
    public static int changablemiddle=0; //delta
    public static float finaladcxposition=0;
    public int ScreenNumPack;
    public ProgressDialog pdialog;
    public boolean minscreen=false;
	public OPIPKT_DrawHelper(){
		super();
		reset();
	}

	public synchronized void reset()
	{
		ACHRTALREADYSET = false;
		nowy=0;
		timems=0;
		beforey=0;
		counter=0;
		fft_zoomrate=1;
		signalZoomRatio = (float) SIGNALZOOMRATIOINIT;
		firstdraw=true;
		countsavedfft=NUMPATHFFT/2;
		clean_z=false;
    	for(int z=0;z<NUMPATHFFT;z++)
    	{
    		imagey[z]=0;
    		hannwindow[z] = 0.5 * (1 - Math.cos(((2 * PI * z) / (NUMPATHFFT - 1))));  //form qt
    		savedDataForFFT[z] = 0;
    	}
    	
    	for(int i=0; i<=NUMPATHFFT/2; i++)
        {
            ffttemp = (i+1);
            increase_log[i]=10*Math.log10(ffttemp);//10db/dec equalization && draw segment length
        }
        countfftcolumn=0;
        fft_is_ready=false;  
	}


	public static  synchronized void cleansurfaceview(SurfaceHolder postureholder,SurfaceView posturesurface)
	{
				Canvas canvas = postureholder.lockCanvas(new Rect(0,0,posturesurface.getWidth(),posturesurface.getHeight()));
				canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); 
				postureholder.unlockCanvasAndPost(canvas);
				Canvas canvasa = postureholder.lockCanvas(new Rect(0,0,posturesurface.getWidth(),posturesurface.getHeight()));
				canvasa.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); 
				postureholder.unlockCanvasAndPost(canvasa);
	}
	
	public void drawfilefftRoutine(MainActivity main,int show_num_of_pack,Vector<Integer> signal,OPIPKT_DrawHelper dhp,SurfaceHolder sfh,SurfaceView sfv)
	{
		dhp.reset();
		int i=0,j=0;
		double [][]effected_by_ko_co_fft= new double [NUMPATHFFT][];
		for(i=0;i<NUMPATHFFT;i++)
		{
			effected_by_ko_co_fft[i] = new double[NUMPATHFFT/2+1];
		}
		//Toast.makeText(main.appVF.getContext(),"", Toast.LENGTH_LONG).show();
		int PACK_SAMPLERATEADC=OPIPKT_android.ADCLEN;
	    int countfft = 0, countadc=0 ;
	    float skipfft=1;
	    int counteffected = 0;
	    //to save the data for drawing
	    double []saveFFT=new double[OPIPKT_DrawHelper.NUMPATHFFT];//to save raw data,uncaculated to fft,just adc
	    int numpathfftcol = (int) main.OPIPKT_hp.screenwidth;
	    int countfftcolumn = 0;
	    int numScalefft=NUMPATHFFT/2; //256 bins, 128Hz BW
	    countadc = (show_num_of_pack+OPIPKT_Accel_Viewer.FRMSPERSEC)*PACK_SAMPLERATEADC +1; // +1 for let fft column the same as s
	    skipfft=(float) ((show_num_of_pack)*PACK_SAMPLERATEADC*2)/(float)NUMPATHFFT; //avoid truncation
	    skipfft=(skipfft-1)/(numpathfftcol); //skip factor
	    if(skipfft<=1) skipfft=1;
	    //fft
	    //from here
	    i=dhp.index_begin*OPIPKT_android.ADCLEN;
	    while (i< dhp.index_begin*OPIPKT_android.ADCLEN + countadc)  //for full FFT
	    {
	    		if(countfft>=0&&countfft<OPIPKT_DrawHelper.NUMPATHFFT)
	    		{
	    			  if(i>=signal.size())
	    		      {
	    				  saveFFT[countfft]=0; //firsttime shift
		  		          countfft++;
		  		          i++;
	    		      }
	    			  else
	    			  {
	    				  saveFFT[countfft]=signal.elementAt(i); //firsttime shift
	  		              countfft++;
	  		              i++;
	    			  }
		        }
		        else
		        {
		            //full sample
		            if(countfftcolumn>=0&&countfftcolumn<numpathfftcol)
		            {
		                countfftcolumn++;
		                for(j=0;j<OPIPKT_DrawHelper.NUMPATHFFT;j++) //add Hanning window
		                {
		                	saveFFT[j]=(float) (dhp.hannwindow[j]*saveFFT[j]); //add Hanning window
		                	dhp.imagey[j] = 0;
		                }
		                	dhp.calculatedFFT = OPIPKT_FFT.FFT(1,OPIPKT_helper.EXPNFFT,saveFFT, dhp.imagey);
		                //effected by fftk and fftco
		                for (j = 0; j < OPIPKT_DrawHelper.NUMPATHFFT/2 ; j ++) 
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
			    		    	effected_by_ko_co_fft[counteffected][j] =1;
			    		    else
			    		    	effected_by_ko_co_fft[counteffected][j] = OPIPKT_DrawHelper.LIVEFFTDBGAIN*(10*Math.log10(dhp.ffttemp*(j+1))-70- OPIPKT_DrawHelper.LIVEFFTDBOFF);    
			    		}//for(int j=0;j<NUMPATHFFTROW/2;J++) end
		                counteffected++;
		                if(skipfft>1) //with FFT skip
		                {
		                    countfft=0;
		                    i += (float) ((skipfft-2.0) * (OPIPKT_DrawHelper.NUMPATHFFT/2)); //skip forward, avoid truncation
		                    if(i>=countadc) break;
		                }
		                else // no skip
		                {
		                    countfft=OPIPKT_DrawHelper.NUMPATHFFT/2; //halfway
		                    for(j=0;j<countfft;j++)
		                        saveFFT[j]=saveFFT[(countfft+j)];
		                }
		                if(i>=signal.size())
		    		    {
		    				 saveFFT[countfft]=0; //firsttime shift
			  		         countfft++;
			  		         i++;
		    		    }
		                else
		                {
		                	saveFFT[countfft]=signal.elementAt(i); //1st new data point
			                countfft++;
			                i++;
		                }
		            }
		            else
		            { //screen full
		                break;
		            }//else end
		        } 
	    }//while end
	    drawfilefft(effected_by_ko_co_fft,dhp,sfh,sfv,countfftcolumn);	
	}
	
	public synchronized float[] HSVconverter(double fftnowvalue)
	{
		float[] pixelHSV = new float[3];
		double gg,rr,bb;
		gg= fftnowvalue;
        if(gg<32)
        {
            rr=255;
            bb=32+gg*3;
        }
        else if (gg<224)
        {
            rr=298-gg*4/3;
            bb=128;
        }
        else
        {
            rr=0;
            bb=gg*4-765;
        }
      //transform qt's hsl to java's hsv
        //h
        pixelHSV[0] = (float) rr;
        //v
        gg = bb*2;
        if(gg<255)
        gg = 255;
        else
        {
        	gg = gg - 255;
        	gg = 255 - gg;
        }
        pixelHSV[1] = (float) (gg/255);
        //v	 		 
        bb = bb*2;
        if(bb>255)
        bb=255;
        pixelHSV[2] = (float) (bb/255);
        
        return pixelHSV ;
	}
	public void drawfilefft(double [][] effected_by_ko_co_fft,OPIPKT_DrawHelper dhp,SurfaceHolder sfh,SurfaceView sfv,int countfftcolumn)
	{
		//draw file
		float FFTPEN=  (finaladcxposition/(float)(countfftcolumn));
		int i=0,j=0;
    	int tempy;
    	int begin = 0;
    	int button = sfv.getHeight();
    	float increase;
    	float fft_total;
    	tempy = button;
    	if(dhp.numScalefft>dhp.NUMPATHFFT/2)
    		dhp.numScalefft = dhp.NUMPATHFFT/2;
    	increase = (float)sfv.getHeight();
    	// add LOG scale increase here
    	fft_total = (float) (dhp.increase_log[dhp.numScalefft]-dhp.increase_log[1]); //start from 2=1Hz
    	increase=increase/fft_total;  //now normalized to display section
    	Canvas c = sfh.lockCanvas(null);
		Paint temppaint = new Paint();
		float[] pixelHSV = new float[3];
		Rect temprect ;
		for(i=0;i<countfftcolumn;i++)
		{
			tempy = sfv.getHeight();
			for( j=2;j<dhp.numScalefft;j++) 
	        {
	            //distribute the data
				dhp.fftnowvalue=effected_by_ko_co_fft[i][j];
				dhp.fftnowvalue=dhp.fftnowvalue*dhp.fft_zoomrate;
	            //clamp
	            if (dhp.fftnowvalue>OPIPKT_DrawHelper.FFTSIGNALMAX) dhp.fftnowvalue=FFTSIGNALMAX;
	            if (dhp.fftnowvalue<OPIPKT_DrawHelper.FFTSIGNALMIN) dhp.fftnowvalue=FFTSIGNALMIN;
	            //convert to pseudo-color
	            //draw
	            pixelHSV=HSVconverter(dhp.fftnowvalue);
	            if(i!=(countfftcolumn-1))
	            {
		            temprect =new Rect(
		            		(int)(begin+(i)*FFTPEN),
		            		(int)(button-(dhp.increase_log[j]-dhp.increase_log[1])*increase),
		            		(int)((begin+(i+1)*FFTPEN)),
		            		(int)(tempy));
	            }
	            else
	            {
		            temprect =new Rect(
		            		(int)(begin+(i)*FFTPEN),
		            		(int)(button-(dhp.increase_log[j]-dhp.increase_log[1])*increase),
		            		(minscreen==false)?sfv.getWidth():(int)finaladcxposition,
		            		(int)(tempy));
	            }
	    		temppaint.setColor(Color.HSVToColor(255, pixelHSV));
	    		c.drawRect(temprect, temppaint);
	            tempy=(int) (button-(dhp.increase_log[j]-dhp.increase_log[1])*increase);
	        }//for(int j=2;j<=numScalefft;j++) end
		}
    	sfh.unlockCanvasAndPost(c);
	}
	
	public static synchronized void drawfft(OPIPKT_DrawHelper dhp,SurfaceHolder sfh,SurfaceView sfv)
	{
		int j,tempy;
		int begin = 0;
		int button = sfv.getHeight();
		float increase;
		float fft_total;
		tempy = button;
		if(dhp.numScalefft>dhp.NUMPATHFFT/2)
			dhp.numScalefft = dhp.NUMPATHFFT/2;
		increase = (float)sfv.getHeight();
		// add LOG scale increase here
		fft_total = (float) (dhp.increase_log[dhp.numScalefft]-dhp.increase_log[1]); //start from 2=1Hz
		increase=increase/fft_total;  //now normalized to display section
		if(dhp.fft_is_ready) //choose one of the column to draw
	    {
			Canvas c = sfh.lockCanvas(new Rect( (int)(begin+(dhp.countfftcolumn-1)*FFTPENWIDTH)
												,0
												,(begin+(dhp.countfftcolumn)*FFTPENWIDTH)
												,button));
			Paint temppaint = new Paint();
			float[] pixelHSV = new float[3];
			for( j=2;j<dhp.numScalefft;j++) 
	        {
	            //distribute the data
				dhp.fftnowvalue=dhp.effected_by_ko_co_fft[j];
				dhp.fftnowvalue=dhp.fftnowvalue*dhp.fft_zoomrate;
	            //clamp
	            if (dhp.fftnowvalue>OPIPKT_DrawHelper.FFTSIGNALMAX) dhp.fftnowvalue=FFTSIGNALMAX;
	            if (dhp.fftnowvalue<OPIPKT_DrawHelper.FFTSIGNALMIN)  dhp.fftnowvalue=FFTSIGNALMIN;
	            //convert to pseudo-color
	            //draw
	            pixelHSV = dhp.HSVconverter(dhp.fftnowvalue);
	            
	            Rect temprect =new Rect(
	            		(int)(begin+(dhp.countfftcolumn-1)*FFTPENWIDTH),
	            		(int)(button-(dhp.increase_log[j]-dhp.increase_log[1])*increase),
	            		(int)((begin+(dhp.countfftcolumn)*FFTPENWIDTH)),
	            		(int)(tempy));
	    		temppaint.setColor(Color.HSVToColor(255, pixelHSV));
	    		c.drawRect(temprect, temppaint);
	            tempy=(int) (button-(dhp.increase_log[j]-dhp.increase_log[1])*increase);
	        }//for(int j=2;j<=numScalefft;j++) end
	        dhp.fft_is_ready=false;  //set false when drawing is done
	    	sfh.unlockCanvasAndPost(c);
	    }//if(fft_is_ready) end
		if(dhp.countfftcolumn==dhp.numpathfftcol)  //clean screen
		{
				dhp.clean_z=true;
				OPIPKT_DrawHelper.cleansurfaceview(sfh,sfv);
		}
	}
		
	public static synchronized void drawsmallfft(OPIPKT_DrawHelper dhp,int signal,SurfaceHolder sfh,SurfaceView sfv)
	{
		Canvas c = sfh.lockCanvas(new Rect( (int)(dhp.counter*FFTLEDWIDTH),0,(int)((dhp.counter+1)*FFTLEDWIDTH),sfv.getHeight()));
		Rect temprect = new Rect((int)(dhp.counter*FFTLEDWIDTH),0,(int)((dhp.counter+1)*FFTLEDWIDTH),sfv.getHeight());
		Paint temppaint = new Paint();
				switch(signal)
				{
				case OPIPKT_helper.LEDBLU:
					temppaint.setColor(Color.BLUE);
					break;
				case OPIPKT_helper.LEDGRN:
					temppaint.setColor(Color.GREEN);
					break;
				case OPIPKT_helper.LEDORG:
					temppaint.setColor(Color.YELLOW);
					break;
				case OPIPKT_helper.LEDRED:
					temppaint.setColor(Color.RED);
					break;
				default:
					temppaint.setColor(Color.BLACK);
					break;
				}
			c.drawRect(temprect, temppaint);
			sfh.unlockCanvasAndPost(c);
			dhp.counter++;
			if(dhp.counter*FFTLEDWIDTH>=sfv.getWidth())
			{
				dhp.counter=0;
				OPIPKT_DrawHelper.cleansurfaceview(sfh, sfv);
			}
	}
	

	public void FileDrawRoutine(boolean zoomined,MainActivity mainp,long record,int begin,int zoompack)
	{
		main = mainp;
		main.OPIPKT_edfreader.drawed=true;
		index_begin=begin;
		if(zoomined==false)
		{
			if((record*OPIPKT_EDF_Writer.EDFDRDURSEC)<=(main.ADCPACKAGEMAX/OPIPKT_Accel_Viewer.FRMSPERSEC))
			{
				minscreen=true;
				ScreenNumPack=main.ADCPACKAGEMAX;
			}
			else
			{
				minscreen=false;
				ScreenNumPack=(int) (record*OPIPKT_Accel_Viewer.FRMSPERSEC*OPIPKT_EDF_Writer.EDFDRDURSEC);
			}
		}
		else
		{
			minscreen=false;
			ScreenNumPack=zoompack;
		}
    	pdialog=new ProgressDialog(main.appVF.getContext());
    	pdialog.setCancelable(true);
    	pdialog.setMessage("Loading ....");
    	pdialog.show();
		new FileDrawRoutineAsync().execute((Void) null);
	}
	    
	/***
     * Class to configure Uce to get it ready for use. Done asynchronously so 
     * it doesn't block UI update
     * @return
     *     0, if successful
     *     < 0, if error, check below for specific code
     */
    public class FileDrawRoutineAsync extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... params) {
        	main.OPIPKT_drawadchp.reset();
    		main.OPIPKT_drawadchp.drawfilesignal(DRAWADCMAX,DRAWADCMIN,main.OPIPKT_drawadchp,ScreenNumPack,main.OPIPKT_edfreader.adcQV, main.fileadcholder, main.fileadcsurface,OPIPKT_android.ADCLEN);
    		main.OPIPKT_drawffthp.drawfilefftRoutine(main,ScreenNumPack,main.OPIPKT_edfreader.adcQV,main.OPIPKT_drawffthp,main.filefftholder,main.filefftsurface);
    		OPIPKT_DrawHelper.drawfileaccelview(main,ScreenNumPack);
        	return 0;
        }
        protected void onPostExecute(Integer result) {
        		pdialog.cancel();
        	}
    }
    
	
	public static void drawfileaccelview(MainActivity main,int maxpack)
	{
		
		main.OPIPKT_twv.setWandH(main.fileposturesurface.getWidth(),main.fileposturesurface.getHeight(),main.fileposturezsurface.getWidth(),main.fileposturezsurface.getHeight());
		main.OPIPKT_twv=OPIPKT_Accel_Viewer.filedisplayroutine(main.OPIPKT_twv, main.OPIPKT_edfreader.axQV, main.OPIPKT_edfreader.ayQV, main.OPIPKT_edfreader.azQV,finaladcxposition,index_begin,maxpack);
		Canvas csfh = main.filepostureholder.lockCanvas();
 		csfh.drawPath(main.OPIPKT_twv.myQPP, main.OPIPKT_twv.pcolor);
 		main.filepostureholder.unlockCanvasAndPost(csfh);
 		
 		Canvas csfhz = main.fileposturezholder.lockCanvas();
 		csfhz.drawPath(main.OPIPKT_twv.myQPP2, main.OPIPKT_twv.pcolor);
 		main.fileposturezholder.unlockCanvasAndPost(csfhz);
	}
	
	
	public  void drawfilesignal(int signalmax,int signalmin,OPIPKT_DrawHelper dhp,int maxpackagenum,Vector<Integer> signal,SurfaceHolder sfh,SurfaceView sfv,int samplenumperpack)
	{
		boolean  seperatestart,notenough;
		boolean FINALDRAW=false;
		float   begin=0;
		float nextvaluey;
		int i, reduction,j; //reduction factor to display fewer data
		float nowvaluey=0,beforevaluey,nowvaluex=0,beforevaluex;
		int show_num_of_seconds=(maxpackagenum/OPIPKT_Accel_Viewer.FRMSPERSEC);
		float samplerate=((float)samplenumperpack*OPIPKT_Accel_Viewer.FRMSPERSEC);
		float everywidthsignalincrease= (float)sfv.getWidth()/(float)show_num_of_seconds;
		float everysecondsignalincrease = (float)everywidthsignalincrease/samplerate;
		float screenmiddle = (float)sfv.getHeight()/2;
		float signalmiddle = ((float)signalmax+(float)signalmin)/2;
		float scalerate = (float)sfv.getHeight()/((float)signalmax-(float)signalmin);

		if(everysecondsignalincrease<0.25) reduction=(int) (0.25/everysecondsignalincrease); //max 4 values per pixel
	    else reduction=1;
	    if(samplerate>=256 && reduction>256) reduction=256;
	    else if(samplerate>=256 && reduction>128) reduction=128;
	    else if(samplerate>=256 && reduction>64) reduction=64;
	    else if(samplerate>=256 && reduction>32) reduction=32;
	    else if(samplerate>=16 && reduction>16) reduction=16;
	    else if(samplerate>=16 && reduction>8) reduction=8;
	    else if(samplerate>=4 && reduction>4) reduction=4;
	    else if(samplerate>=4 && reduction>2) reduction=2;
	    else if(samplerate>=4 && reduction>1) reduction=1;
	    else reduction=1;
	    everysecondsignalincrease=everysecondsignalincrease*reduction; //adjust by reduction factor
	    
	    Canvas c = sfh.lockCanvas();
		Paint pcolor = new Paint();
		pcolor.setStrokeWidth(1);
		pcolor.setPathEffect(null);
		pcolor.setColor(Color.RED);
		pcolor.setStyle(Paint.Style.STROKE); 
		Path signalpath=new Path(); 

		if(dhp.index_begin*OPIPKT_android.ADCLEN<signal.size())
	        beforevaluey=(float)signal.elementAt(index_begin*OPIPKT_android.ADCLEN)*signalZoomRatio;
	    else
	        return ;
	    beforevaluey=(screenmiddle+changablemiddle)-(beforevaluey-signalmiddle)*scalerate;
	    beforevaluey=clamp(beforevaluey,sfv.getHeight(),0);
	    beforevaluex=begin;
	    notenough=false;
	    
	    
	    for(i=0;i<show_num_of_seconds;i++)//how many seconds
	    {
	        if(index_begin*OPIPKT_android.ADCLEN+samplerate*i<signal.size())
	            nowvaluey=(float)signal.elementAt((int) (index_begin*OPIPKT_android.ADCLEN+samplerate*i))*signalZoomRatio;
	        else
	            break;
	        nowvaluey=(screenmiddle+changablemiddle)-(nowvaluey-signalmiddle)*scalerate;
	        nowvaluey=clamp(nowvaluey,sfv.getHeight(),0);
	        if(index_begin*OPIPKT_android.ADCLEN+samplerate*i+1<signal.size())
	            nextvaluey=(float)signal.elementAt((int) (index_begin*OPIPKT_android.ADCLEN+samplerate*i+1))*signalZoomRatio;
	        else
	            break;
	        nextvaluey=(screenmiddle+changablemiddle)-(nextvaluey-signalmiddle)*scalerate;
	        nextvaluey=clamp(nextvaluey,sfv.getHeight(),0);
	        nowvaluex=begin;
	        seperatestart=false;
	        for(j=0;j<samplerate/reduction;j++) //how many sample per second adj. for reduction
	        {
	            if(seperatestart)
	            {
	                if(index_begin*OPIPKT_android.ADCLEN+samplerate*i+j*reduction<signal.size()) //reduction
	                    nowvaluey=(float)signal.elementAt((int) (index_begin*OPIPKT_android.ADCLEN+samplerate*i+j*reduction))*signalZoomRatio; //reduction
	                else
	                	{
	                		FINALDRAW=true;
	                		break;
	                	}
	                nowvaluey=(screenmiddle+changablemiddle)-(nowvaluey-signalmiddle)*scalerate;
	                nowvaluey=clamp(nowvaluey,sfv.getHeight(),0);
	    	        if(index_begin*OPIPKT_android.ADCLEN+samplerate*i+j*reduction+1<signal.size()) //reduction
	                    nextvaluey=(float)signal.elementAt((int) (index_begin*OPIPKT_android.ADCLEN+samplerate*i+j*reduction+1))*signalZoomRatio; //reduction
	                nextvaluey=(screenmiddle+changablemiddle)-(nextvaluey-signalmiddle)*scalerate;
	                nextvaluey=clamp(nextvaluey,sfv.getHeight(),0);
	                nowvaluex=nowvaluex+everysecondsignalincrease; //reduction
	            }//if end
	            seperatestart=true;
	            signalpath.moveTo(beforevaluex,beforevaluey);
	            signalpath.lineTo(nowvaluex,nowvaluey);
	            beforevaluey=nowvaluey;
	            beforevaluex=nowvaluex;
	        }//samples end for end
	        if(FINALDRAW==true)
	        	break;
	        begin=everywidthsignalincrease*(i+1);
	    }//seconds end
	    if(index_begin*OPIPKT_android.ADCLEN+samplerate*show_num_of_seconds<signal.size())
	    {
	        nowvaluey=(float)signal.elementAt((int) (index_begin*OPIPKT_android.ADCLEN+samplerate*show_num_of_seconds))*signalZoomRatio;
	        nowvaluey=(screenmiddle+changablemiddle)-(nowvaluey-signalmiddle)*scalerate;
	        nowvaluey=clamp(nowvaluey,sfv.getHeight(),0);
	        signalpath.moveTo(beforevaluex,beforevaluey);
	        signalpath.lineTo(nowvaluex,nowvaluey);
	    }
	    else
	    {
	    	signalpath.moveTo(beforevaluex,beforevaluey);
	    	signalpath.lineTo(nowvaluex,nowvaluey);
	    }
		////////end
	    finaladcxposition = nowvaluex;
		signalpath.close();
		c.drawPath(signalpath, pcolor);
		sfh.unlockCanvasAndPost(c);
	}   
	    
	public static float clamp(float value,float max,float min)
	{
		if(value>max)
			value=max;
		else if(value<min)
			value=min;
		return value;
	}
	public static synchronized void drawsignal(int signalmax,int signalmin,OPIPKT_DrawHelper dhp,int maxpackagenum,int []signal,SurfaceHolder sfh,SurfaceView sfv,int samplenum)
	{
		float perwidth= (float)sfv.getWidth()/(float)maxpackagenum;
		float increase = (float)perwidth/(float)samplenum;
		float screenmiddle = (float)sfv.getHeight()/2;
		float signalmiddle = ((float)signalmax+(float)signalmin)/2;
		float scalerate = (float)sfv.getHeight()/((float)signalmax-(float)signalmin);

		Canvas c = sfh.lockCanvas(new Rect((int) (dhp.counter*increase),0,(int) (dhp.counter*increase+perwidth),sfv.getHeight()));
		Paint pcolor = new Paint();
		pcolor.setStrokeWidth(1);
		pcolor.setPathEffect(null);
		pcolor.setColor(Color.RED);
		pcolor.setStyle(Paint.Style.STROKE); 
		Path signalpath=new Path(); 
		/////////drawsignal
		for(int i=0;i<samplenum;i++)
		{
			dhp.counter++;
			if(((dhp.counter)*increase)>=(sfv.getWidth()))
			{
				signalpath.close();
				c.drawPath(signalpath, pcolor);
				sfh.unlockCanvasAndPost(c);
				signalpath.reset();
				dhp.counter=0;
				Canvas canvas = sfh.lockCanvas(new Rect(0,0,sfv.getWidth(),sfv.getHeight()));
				canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); 
				sfh.unlockCanvasAndPost(canvas);
				canvas = sfh.lockCanvas(new Rect(0,0,sfv.getWidth(),sfv.getHeight()));
				canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); 
				sfh.unlockCanvasAndPost(canvas);
				c = sfh.lockCanvas(new Rect(0,0,(int) (perwidth),sfv.getHeight()));
				dhp.counter=1;
			}
			if(dhp.firstdraw)
			{
				dhp.nowy = signal[i] * dhp.signalZoomRatio;
				dhp.beforey = dhp.nowy;
				dhp.firstdraw = false;
			}
			else
			{
				dhp.beforey = dhp.nowy;
				dhp.nowy = signal[i] * dhp.signalZoomRatio;
			}
			if(dhp.beforey>signalmax)
				dhp.beforey=signalmax; 
			if(dhp.beforey<signalmin)
				dhp.beforey=signalmin;
			if(dhp.nowy>signalmax)
				dhp.nowy=signalmax; 
			if(dhp.nowy<signalmin)
				dhp.nowy=signalmin;
			signalpath.moveTo((dhp.counter-1)*increase,screenmiddle-(dhp.beforey-signalmiddle)*scalerate);
			signalpath.lineTo(dhp.counter*increase,screenmiddle-(dhp.nowy-signalmiddle)*scalerate); 
		}
		////////end
		signalpath.close();
		c.drawPath(signalpath, pcolor);
		sfh.unlockCanvasAndPost(c);
	}
	
}
