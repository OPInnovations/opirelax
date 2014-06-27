package opi_android;

import java.util.Vector;

import opi.relax.opirelax.MainActivity;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


public class OPIPKT_Accel_Viewer {
	public Paint pcolor = new Paint();
	
	public static final int ACTOFFSET =0;	 // offset of activity data when written to file
	public static final int ACTGAIN =1;	 // gain of activity data when written to file
	public static final int FASTACTWEIGHT =3;	 // divider of fast activity
	public static final int SLOWACTWEIGHT =24;	 // divider of slow activity in activity cal.
	public static final int SFASTACTWEIGHT =1;	 // divider of super fast activity in activity calc.
	public static final int LIVEPVSHOWSECMAX =1200;	 //PostureViewer shows 20min then clear
	public static final int FRMSPERSEC =4;	 // number of truesense data frames per second
	public static final int PVZSCALE = 768;	 // 640 for +/-1.25G, 768 for +/-1.5G PostureViewer scaling of z axis
	
	
	float posx,posy,oldposx,oldposy;
	boolean firstdraw;
	float x_calm,y_calm,z_calm;
	long lowcount,medcount,highcount;
	long intensecount,totalcount,newstep;
	int azold,axold,az4old,ayold,ax2old,ay2old,ax3old,ay3old,ax4old,ay4old,az8old,az12old,az16old,stepcount;
	float windowxscale,windowyscale;
	public int viewwidth,viewheight;
	public int viewzwidth,viewzheight;
	public Vector<Integer> accxqvect = new Vector<Integer>();
	public Vector<Integer> accyqvect = new Vector<Integer>();
	public Vector<Integer> acczqvect = new Vector<Integer>();
	public Vector<Integer> acczplane = new Vector<Integer>();
	public Vector<Integer> acczqvectoriginal = new Vector<Integer>();
	public Vector<Integer> activityqvect = new Vector<Integer>();
	public Vector<Long> activitylow = new Vector<Long>();
	public Vector<Long> totalcountsave = new Vector<Long>();
	public Vector<Long> stepcountsave = new Vector<Long>();
	public Vector<Long> activitymed = new Vector<Long>();
    public Vector<Long> activityhigh = new Vector<Long>();
    public Vector<Long> activityintense = new Vector<Long>();
	public Path myQPP=new Path(); 
	public Path myQPP2=new Path();
    public OPIPKT_Accel_Viewer() {
		pcolor.setStrokeWidth(1);
		pcolor.setPathEffect(null);
		pcolor.setColor(Color.RED);
		pcolor.setStyle(Paint.Style.STROKE); 
		reset();
	}
	
    public synchronized void reset()
    {
		firstdraw=true;
	    x_calm=0;
	    y_calm=0;
	    z_calm=0;
	    lowcount = 0;
	    medcount = 0;
	    highcount = 0;
	    intensecount = 0;
	    totalcount = 0;
	    newstep = 1;//init
	    azold = 0;
	    axold = 0;
	    ayold = 0;
	    az4old = 0;
	    ax2old = 0;
	    ay2old = 0;
	    ax3old = 0;
	    ay3old = 0;
	    ax4old = 0;
	    ay4old = 0;
	    az8old = 0;
	    az12old = 0;
	    az16old = 0;
	    stepcount = 0;
	    accxqvect.clear();
	    accyqvect.clear();
	    acczqvect.clear();
	    acczplane.clear();
	    acczqvectoriginal.clear();
	    activityqvect.clear();
	    activitylow.clear();
	    totalcountsave.clear();
	    stepcountsave.clear();
	    activitymed.clear();
	    activityhigh.clear();
	    activityintense.clear();
    }
    public synchronized void  setWandH(int width,int height,int widthz,int heightz)
    {
		viewwidth=width;
		viewheight=height;
		viewzwidth=widthz;
		viewzheight=heightz;
	    windowxscale = ((float)(70000))/((float)(height)); //fixed range 70000
	    windowyscale = ((float)(50000))/((float)(height)); //fixed range 50000
    }
	public static synchronized  int calcAct(OPIPKT_Accel_Viewer twv)
	{
	    int i,j, fastdx, fastdy, fastdz, slowdx, slowdy, slowdz, sfastdz;
	    int fastAct, slowAct, sfastAct, activ, z4sum, x4mean, y4mean, z16mean, xsum, ysum;
	    int retval;
	    slowdx = 0; //init
	    slowdy = 0; //init
	    slowdz = 0; //init
	    fastdz = 0; //init
	    sfastAct = 0;
	    z4sum = 0; //init
	    x4mean = 0;
	    y4mean = 0;
	    z16mean = 0;
	    xsum = 0;
	    ysum = 0;
	    j = twv.acczqvectoriginal.size(); //max index
	    sfastdz = (int) (twv.acczqvectoriginal.elementAt(j-4) - twv.azold); //current packet
	    sfastAct += (sfastdz*sfastdz);
	    sfastdz = (twv.acczqvectoriginal.elementAt(j-3)-twv.acczqvectoriginal.elementAt(j-4)); //current packet
	    sfastAct += (sfastdz*sfastdz);
	    sfastdz = (twv.acczqvectoriginal.elementAt(j-2)-twv.acczqvectoriginal.elementAt(j-3)); //current packet
	    sfastAct += (sfastdz*sfastdz);
	    sfastdz = (twv.acczqvectoriginal.elementAt(j-1)-twv.acczqvectoriginal.elementAt(j-2)); //current packet
	    sfastAct += (sfastdz*sfastdz);
	    xsum = twv.accxqvect.elementAt(j/4-1); //current packet
	    ysum = twv.accyqvect.elementAt(j/4-1); //current packet
	    fastdx = (int) (xsum - twv.axold); //current vs. last packet
	    fastdy = (int) (ysum - twv.ayold); //current vs. last packet
	    z4sum += (twv.acczqvectoriginal.elementAt(j-1));
	    z4sum += (twv.acczqvectoriginal.elementAt(j-2));
	    z4sum += (twv.acczqvectoriginal.elementAt(j-3));
	    z4sum += (twv.acczqvectoriginal.elementAt(j-4));
	    fastdz = (int) ((z4sum - twv.az4old)/4); //average of 4
	    fastAct = fastdx*fastdx + fastdy*fastdy + fastdz*fastdz; //sum of square
	    slowdz = (int) ((z4sum - twv.az16old)/16); //average of 4, 4 packets apart
	    x4mean = (int) ((twv.axold + twv.ax2old + twv.ax3old + twv.ax4old)/4); //mean of 4 packets
	    y4mean = (int) ((twv.ayold + twv.ay2old + twv.ay3old + twv.ay4old)/4); //mean of 4 packets
	    z16mean = (int) ((twv.az4old+ twv.az8old+ twv.az12old+ twv.az16old)/16); //mean of 4 packets
	    slowdx = (int) ((xsum - twv.ax4old)/4); //4 packets apart
	    slowdy = (int) ((ysum - twv.ay4old)/4); //4 packets apart
	    slowAct = slowdx*slowdx + slowdy*slowdy + slowdz*slowdz; //sum of square
	    if(slowAct<=257000) slowAct=0; //noise reduction
	    if(fastAct<=257000) fastAct=0; //noise reduction
	    if(sfastAct<=257000) sfastAct=0; //noise reduction
	    activ = (OPIPKT_Accel_Viewer.ACTOFFSET + (OPIPKT_Accel_Viewer.ACTGAIN*(fastAct/OPIPKT_Accel_Viewer.FASTACTWEIGHT + slowAct/OPIPKT_Accel_Viewer.SLOWACTWEIGHT + sfastAct/OPIPKT_Accel_Viewer.SFASTACTWEIGHT))); // activ must be qint16
	    if(activ<=1000000) activ=1000000; //set noise floor 256*256*4=256K
	    activ = (int) (6553.6*(Math.log10(activ)-6.0));  //log10 50dB dynamic range
	    if(activ > 32767) activ = 32767;    // clipping, since return value is qint16
	    if(activ < 0) activ = 0; // set floor
	    if(j<16 && activ>6553.6) activ=(int) 6553.6; //block initial spike >10db
	    //check step using zero-crossing: newstepp=-1(neg domain), +1(pos domain), +2(pos transition detected=>add 1 step)
	    for(i=4; i>0; i--)
	    {
	        if((twv.acczqvectoriginal.elementAt(j-i)-xsum+ysum - z16mean+x4mean-y4mean)> 3000) //positive with hysteresis
	        {
	            if((twv.newstep)==-1) twv.newstep=2; //advance 1 step
	            else if(twv.newstep==-2) twv.newstep=3; //advance 2 steps
	        }
	        else if((twv.acczqvectoriginal.elementAt(j-i)-xsum+ysum - z16mean+x4mean-y4mean) < -3000) //negative with hysteresis
	        {
	            if((twv.newstep)==1) twv.newstep=-1; //negative
	            else if(twv.newstep==2) twv.newstep=-2; //negative
	        }
	    }
	    twv.azold = twv.acczqvectoriginal.elementAt(j-1); //new value
	    twv.az16old = twv.az12old; //new value
	    twv.az12old = twv.az8old; //new value
	    twv.az8old= twv.az4old; //new value
	    twv.az4old = z4sum; //new value
	    twv.ax4old = twv.ax3old; //new value
	    twv.ax3old = twv.ax2old; //new value
	    twv.ax2old = twv.axold; //new value
	    twv.axold = xsum; //new value
	    twv.ay4old = twv.ay3old; //new value
	    twv.ay3old = twv.ay2old; //new value
	    twv.ay2old = twv.ayold; //new value
	    twv.ayold= ysum; //new value
	    retval =  activ;
	    return retval; //no motion <5db; slow(low) 5~10db; walk(mid) 10~20db; run(hi) 20~28db; shake(intense) >28db;
	}
	public static  OPIPKT_Accel_Viewer filedisplayroutine(OPIPKT_Accel_Viewer twv,Vector<Integer> signalx,
			Vector<Integer> signaly,Vector<Integer> signalz,float finalfftxposition,int index_begin,int maxpack)
	{
		int i,j;
		float zXDelta;
		float zzz;
		int z_new_data_average;
		Vector<Integer> acczaverage = new Vector<Integer>();
		twv.reset();
		twv.myQPP.rewind();
		twv.myQPP.reset();
		twv.myQPP2.rewind();
		twv.myQPP2.reset();
		
		z_new_data_average = 0;

		for(j=index_begin*OPIPKT_android.ACCLEN;j< (index_begin+1)*OPIPKT_android.ACCLEN;j++)
		{
			if(j<signalz.size())
			{
				zzz = signalz.elementAt(j);
				if(zzz>32767) zzz-=65536;
				z_new_data_average+= (zzz/OPIPKT_android.ACCLEN); //prevent overflow in qint16
			}
		}
		acczaverage.addElement(new Integer(z_new_data_average));
	    // first data as starting point for 2D draw
		if(index_begin<signalx.size())
		calnewpos(twv,signalx.elementAt(index_begin),signaly.elementAt(index_begin),z_new_data_average);
		twv.posy=OPIPKT_DrawHelper.clamp(twv.posy,twv.viewheight,0);
	    twv.myQPP.moveTo(twv.posx, twv.posy);
	    
	    for(i = index_begin+1; i < index_begin+maxpack; i++)
	    {
	    	z_new_data_average = 0;
	    	for(j=0;j< OPIPKT_android.ACCLEN;j++)
	   		{
	    		if(OPIPKT_android.ACCLEN*i+j<signalz.size())
	    		{
		    		zzz = signalz.elementAt(OPIPKT_android.ACCLEN*i+j);
		    		if(zzz>32767) zzz-=65536;
		    		z_new_data_average+= (zzz/OPIPKT_android.ACCLEN); //prevent overflow in qint16
	    		}
	    	}
	    	 acczaverage.addElement(new Integer(z_new_data_average));
	    	 if(i<signalx.size())
	    	 calnewpos(twv,signalx.elementAt(i),signaly.elementAt(i),z_new_data_average);
	    	 twv.posy=OPIPKT_DrawHelper.clamp(twv.posy,twv.viewheight,0);
	    	 twv.myQPP.lineTo(twv.posx, twv.posy);
	    	 twv.myQPP.moveTo(twv.posx, twv.posy);
	    }
		
	    float xDelta= ((float)finalfftxposition/((float) acczaverage.size()-1));
	    // draw the Z data
	    float Znowy,Zprey;
	    for(i = 1; i < acczaverage.size(); i++)
	    {
	    	Znowy= (float)twv.viewzheight/2-(float) acczaverage.elementAt(i)/(float)OPIPKT_Accel_Viewer.PVZSCALE;
	    	Znowy=OPIPKT_DrawHelper.clamp(Znowy,twv.viewzheight,0);
	    	Zprey= (float)twv.viewzheight/2-(float) acczaverage.elementAt(i-1)/(float)OPIPKT_Accel_Viewer.PVZSCALE;
	    	Zprey=OPIPKT_DrawHelper.clamp(Zprey,twv.viewzheight,0);
	    	twv.myQPP2.moveTo(xDelta*(i-1),Zprey);
		    twv.myQPP2.lineTo(xDelta*(i),Znowy);
	    }
	    return twv;
	}
	
	
	public  static synchronized void displayroutine(OPIPKT_Accel_Viewer twv,OPIPKT_Data_Converter opipkt_dt,SurfaceHolder sfh,SurfaceView sfv)
	{
		int i,removeXCt;
		int acttemp;
		float zXDelta;
		twv.myQPP.reset();
		twv.myQPP2.reset();
		twv.appendaccxyz(opipkt_dt);
		for(i = 0;i<OPIPKT_android.ACCLEN;i++)
		{
			twv.acczqvectoriginal.addElement(new Integer(opipkt_dt.zvalue[i]));
		}
		acttemp = calcAct(twv);
		
		if(twv.newstep>1) //update stepcount here
	    {
			twv.stepcount++; //add 1 step
	        if(twv.newstep>2) twv.stepcount++; //add 2 steps
	        twv.newstep=1;
	    }
	    else if(twv.newstep<-1)
	    {
	    	twv.stepcount++; //add 1 step
	    	twv.newstep=-1;
	    }
		
	    activitydistribu(twv.actscale(acttemp),twv);    

	    if(twv.accxqvect.size() < 2) return; // don't draw anything here, since not even two points
	    // first data as starting point for 2D draw
	    calnewpos(twv,twv.accxqvect.elementAt(0),twv.accyqvect.elementAt(0),twv.acczqvect.elementAt(0));
	    twv.myQPP.moveTo(twv.posx, twv.posy);
	    for(i = 1; i < twv.accxqvect.size(); i++)
	    {
	    	 calnewpos(twv,twv.accxqvect.elementAt(i),twv.accyqvect.elementAt(i),twv.acczqvect.elementAt(i));
	    	 twv.myQPP.lineTo(twv.posx, twv.posy);
	    	 twv.myQPP.moveTo(twv.posx, twv.posy);
	    }
		
	    // draw the Z data
	    //zXDelta = ((float) LIVEPVSHOWSECMAX*FRMSPERSEC)/(float)twv.viewzwidth;   // figure out how many data points to take
	    for(i = 1; i < twv.acczplane.size(); i++)
	    {
	    	twv.myQPP2.moveTo((int)(((float)OPIPKT_DrawHelper.FFTPENWIDTH/(float)FRMSPERSEC)*(i-1)), (float)twv.viewzheight/2-(float)twv.acczplane.elementAt(i-1)/(float)OPIPKT_Accel_Viewer.PVZSCALE);
	    	twv.myQPP2.lineTo((int)(((float)OPIPKT_DrawHelper.FFTPENWIDTH/(float)FRMSPERSEC)*(i)), (float)twv.viewzheight/2-(float)twv.acczplane.elementAt(i)/(float)OPIPKT_Accel_Viewer.PVZSCALE);
	    }
	    
	    // clear data that exceeds maximum
	    if(twv.accxqvect.size() > OPIPKT_Accel_Viewer.LIVEPVSHOWSECMAX*OPIPKT_Accel_Viewer.FRMSPERSEC)
	    {
	    	/*
	        removeXCt = twv.accxqvect.size() - OPIPKT_Accel_Viewer.LIVEPVSHOWSECMAX*OPIPKT_Accel_Viewer.FRMSPERSEC;  
	        for(i=0;i<removeXCt;i++)
	        {
	        	twv.accxqvect.removeElementAt(0);
	        	twv.accyqvect.removeElementAt(0);
	        	twv.acczqvect.removeElementAt(0);
	        }
	        for(i=0;i< removeXCt*OPIPKT_android.ACCLEN;i++)
	        	twv.acczqvectoriginal.removeElementAt(0);
	        */
	        twv.accxqvect.clear();
	        twv.accyqvect.clear();
	        twv.acczqvect.clear();
	        twv.acczqvectoriginal.clear();
	        OPIPKT_DrawHelper.cleansurfaceview(sfh,sfv);
	    }
	}
	
	public synchronized void cleanZplane(SurfaceHolder sfh,SurfaceView sfv)
	{
		OPIPKT_DrawHelper.cleansurfaceview(sfh,sfv);
		acczplane.clear();
	}
	static private synchronized void calnewpos(OPIPKT_Accel_Viewer twv,int newposx,int newposy,int newposz)
	{
		float xxx,yyy; //prevent qint16 overflow
	    xxx=(float)newposx;
	    xxx+=(float)newposy;
	    xxx=(xxx-twv.x_calm-twv.y_calm)/twv.windowxscale;
	    yyy=newposz;
	    yyy=(yyy-twv.z_calm)/twv.windowyscale;
	    (twv.posx)=(float)twv.viewwidth/2+xxx;        //right left
	    if ((twv.posx)<0) (twv.posx)=0;
	    else if ((twv.posx)>twv.viewwidth) (twv.posx)=twv.viewwidth;
	    
	    (twv.posy)=(float)twv.viewheight/2-yyy; //up down
	    if ((twv.posy)<0) (twv.posy)=0;
	    else if ((twv.posy)>(twv.viewheight)) (twv.posy)=(twv.viewheight);
	}
	static private synchronized void activitydistribu(double act ,OPIPKT_Accel_Viewer twv)
	{
		 (twv.totalcount)++;
		    if(act>=6&&act<=10) //set 6 to 10 for low activity: slow moving
		    {
		        (twv.lowcount)++;
		    }
		    else if(act>10&&act<=20) //set 10 to 20 for mid activity: walking
		    {
		        (twv.medcount)++;
		    }
		    else if(act>20&&act<=28) //set 20 to 28 for hi activity: running
		    {
		        (twv.highcount)++;
		    }
		    else if(act>28) //set 28 for intense activity: shaking
		    {
		        (twv.intensecount)++;
		    }
	}
	private synchronized double actscale(int act)
	{
		 double scale= (double)50/(double)32767; //convert to db from 50db fullrange
		 return (act*scale);
	}
	private synchronized void appendaccxyz(OPIPKT_Data_Converter opipkt_dt)
	{
		int j;
		float zzz;
		int z_new_data_average;
		accxqvect.addElement(new Integer(opipkt_dt.xvalue));
		accyqvect.addElement(new Integer(opipkt_dt.yvalue));
		z_new_data_average = 0;
		for(j=0;j< OPIPKT_android.ACCLEN;j++)
		{
			zzz = opipkt_dt.zvalue[j];
			if(zzz>32767) zzz-=65536;
			z_new_data_average+= (zzz/OPIPKT_android.ACCLEN); //prevent overflow in qint16
		}
		acczqvect.addElement(new Integer(z_new_data_average));
		acczplane.addElement(new Integer(z_new_data_average));
	}
}
