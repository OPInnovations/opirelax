package opi_android;


import java.util.Vector;
import android.content.Context;



public class OPIPKT_Data_Converter {
	
	public OPIPKT_Data_Converter(){
		super();
		adcvalue = new int[OPIPKT_android.ADCLEN];
		zvalue = new int[OPIPKT_android.ACCLEN];
		uceFFTData = new int[OPIPKT_android.UCEFFTLEN];
	}
	
	//to calact
	public Vector<Integer> acczqvectoriginal = new Vector<Integer>();
	int azold,axold,az4old,ayold,ax2old,ay2old,ax3old,ay3old,ax4old,ay4old,az8old,az12old,az16old,stepcount;
	public Vector<Integer> accxqvect = new Vector<Integer>();
	public Vector<Integer> accyqvect = new Vector<Integer>();
	public Vector<Integer> acczqvect = new Vector<Integer>();
	long intensecount,totalcount,newstep;
	
	//ADC
	public static final int ADCPHYMIN=-800;	
	public static final int ADCPHYMAX=800;
	public static final int ADCDIGMIN=-20480;	
	public static final int ADCDIGMAX=20480;
	//X
	public static final int XPHYMIN=-2;	
	public static final int XPHYMAX=2;
	public static final int XDIGMIN=-32768;	
	public static final int XDIGMAX=32767;
	//Y
	public static final int YPHYMIN=-2;	
	public static final int YPHYMAX=2;
	public static final int YDIGMIN=-32768;	
	public static final int YDIGMAX=32767;
	//Z
	public static final int ZPHYMIN=-2;	
	public static final int ZPHYMAX=2;
	public static final int ZDIGMIN=-32768;	
	public static final int ZDIGMAX=32767;
	//TEMP
	public static final int TPHYMIN=-47;	
	public static final int TPHYMAX=241;
	public static final int TDIGMIN=0;	
	public static final int TDIGMAX=4080;	
	
    //save opi data
	public  int  skp,bat;
    public  long frmTS,prevFrmTS;
    public  int length;
    public  int ed;
    public  int xvalue;
    public  int yvalue;
    public  int zvalue[];
    public  int uceFFTData[];
    public  int tempvalue;
    public  long tsQV;
    public  int adcvalue[],sampQual;
    public  int adctemp,adclength;
    public  int sqQV;
    
    public synchronized void reset()
    {
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
	    acczqvectoriginal.clear();
    }
    /***
	  *	Get an OPI packet from the com port,
	  *	Inputs:
	  *		pktptr, pointer to the packet
	  *	Returns:
	  *		code:
	  *		false present timestamp is small than previous timestamp
	  *     true process successfully
	  */
   public synchronized boolean opipkt_dataprocess(OPIPKT_struct pktptr,OPIPKT_EDF_Writer edf)
   {	
	    long val = 0;
	    for (int i = 0; i < 6; i++)
	    {
	       val = (val << 8) + (pktptr.payload[OPIPKT_android.WFRMHDRLEN-1+i] & 0xff);
	    }
	    frmTS = System.currentTimeMillis();  
        prevFrmTS = frmTS;
        adctemp = (((pktptr.payload[1+OPIPKT_android.TSLEN+OPIPKT_android.WLFRMHDRLEN])<<8) 
	    		+ (pktptr.payload[1+OPIPKT_android.TSLEN+OPIPKT_android.WLFRMHDRLEN+1]));
        if(adctemp>32767) adctemp-=65536;
        sampQual =  0|(adctemp&OPIPKT_android.SAMPQUALMASK);
        adctemp =   (adctemp&(0xFFFFFFFC));
        if(adctemp>32767) adctemp-=65536;
        length = pktptr.length;
        adcvalue[0] = adctemp;
        tsQV = frmTS;
        skp = 0 ;
 	    skp = 0|pktptr.payload[OPIPKT_android.WFRMHDRLEN+OPIPKT_android.TSLEN];
 	    skp = skp>>7;
        bat = 0 ;
        bat = 0|(pktptr.payload[OPIPKT_android.WFRMHDRLEN+OPIPKT_android.TSLEN+1]&0xff&0x01);
        edf.tsQV.addElement(new Long(frmTS));
        edf.skpQV.addElement(new Integer(skp));
        edf.batQV.addElement(new Integer(bat));
        edf.adcQV.addElement(new Integer(adctemp));
       if(length == OPIPKT_android.TSEFRMLEN) // full 64 adc samples here
       {
    	   for(int i = 1; i < (OPIPKT_android.ADCLEN); i++) // already got first sample
           {
    		adctemp = (((pktptr.payload[OPIPKT_android.WFRMHDRLEN-1+OPIPKT_android.TSLEN+OPIPKT_android.WLFRMHDRLEN+2*i])<<8) 
          				+ (pktptr.payload[OPIPKT_android.WFRMHDRLEN-1+OPIPKT_android.TSLEN+OPIPKT_android.WLFRMHDRLEN+2*i+1]));
          	   if(adctemp>32767) adctemp-=65536;
       		adcvalue[i] = adctemp;
       		edf.adcQV.addElement(new Integer(adctemp));
           }
           adclength = OPIPKT_android.ADCLEN;
        xvalue =0;
        xvalue = (xvalue<<8) | pktptr.payload[OPIPKT_android.WFRMHDRLEN-1+OPIPKT_android.TSLEN+OPIPKT_android.WLFRMHDRLEN+2*(OPIPKT_android.ADCLEN)+OPIPKT_android.TMPLEN];
        xvalue = (xvalue<<8);
       	if(xvalue>32767) xvalue-=65536;
       	yvalue = 0;
       	yvalue = (yvalue<<8) | pktptr.payload[OPIPKT_android.WFRMHDRLEN-1+OPIPKT_android.TSLEN+OPIPKT_android.WLFRMHDRLEN+2*(OPIPKT_android.ADCLEN)+OPIPKT_android.TMPLEN+1];
       	yvalue = (yvalue<<8);
       	if(yvalue>32767) yvalue-=65536;
       	for(int i = 0; i < (OPIPKT_android.ACCLEN); i++) 
       	{
       	   zvalue[i]=0;
       	   zvalue[i]=(zvalue[i]<<8)|pktptr.payload[OPIPKT_android.WFRMHDRLEN-1+OPIPKT_android.TSLEN+OPIPKT_android.WLFRMHDRLEN+2*(OPIPKT_android.ADCLEN)+OPIPKT_android.TMPLEN+2+i];
       	   zvalue[i]=(zvalue[i]<<8);
           if(zvalue[i]>32767) zvalue[i]-=65536;
           edf.azQV.addElement(new Integer(zvalue[i]));
       	}
       	tempvalue=0;
       	tempvalue=pktptr.payload[OPIPKT_android.WFRMHDRLEN-1+OPIPKT_android.TSLEN+OPIPKT_android.WLFRMHDRLEN+2*(OPIPKT_android.ADCLEN)]&0xff;
       	tempvalue=(tempvalue<<4);
    	if(tempvalue >32767) tempvalue -=65536;
       	ed=0;
       	ed=0|pktptr.payload[OPIPKT_android.WFRMHDRLEN-1+OPIPKT_android.TSLEN+OPIPKT_android.WLFRMHDRLEN+2*(OPIPKT_android.ADCLEN)+OPIPKT_android.TMPLEN+OPIPKT_android.ACCDLEN];
       	if(ed >32767) ed-=65536;
       	edf.tmpQV.addElement(new Integer(tempvalue));
        edf.axQV.addElement(new Integer(xvalue));
        edf.ayQV.addElement(new Integer(yvalue));
        edf.edQV.addElement(new Integer(ed));
       }
       else if(length == (OPIPKT_android.TSEFRMLEN-4)) // full 64 adc samples here
       {
    	    for(int i = 1; i < (OPIPKT_android.ADCLEN-2); i++) // already got first sample
            {
        		adctemp = (((pktptr.payload[1+OPIPKT_android.TSLEN+OPIPKT_android.WLFRMHDRLEN+2*i])<<8) 
        				+ (pktptr.payload[1+OPIPKT_android.TSLEN+OPIPKT_android.WLFRMHDRLEN+2*i+1]));
        		if(adctemp>32767) adctemp-=65536;
        		adcvalue[i] = adctemp;
        		edf.adcQV.addElement(new Integer(adctemp));
            }
            adclength = OPIPKT_android.ADCLEN-2;
      	xvalue=0;
      	xvalue=0|pktptr.payload[OPIPKT_android.WFRMHDRLEN-1+OPIPKT_android.TSLEN+OPIPKT_android.WLFRMHDRLEN+2*(OPIPKT_android.ADCLEN-2)+OPIPKT_android.TMPLEN];
      	xvalue=(xvalue<<8);
      	if(xvalue>32767) xvalue-=65536;
      	yvalue=0;
      	yvalue=0|pktptr.payload[OPIPKT_android.WFRMHDRLEN-1+OPIPKT_android.TSLEN+OPIPKT_android.WLFRMHDRLEN+2*(OPIPKT_android.ADCLEN-2)+OPIPKT_android.TMPLEN+1];
      	yvalue=(yvalue<<8);
      	if(yvalue>32767) yvalue-=65536;
      	for(int i = 0; i < OPIPKT_android.ACCLEN; i++) 
      	{
      		zvalue[i]=0;
      		zvalue[i]=0|pktptr.payload[OPIPKT_android.WFRMHDRLEN-1+OPIPKT_android.TSLEN+OPIPKT_android.WLFRMHDRLEN+2*(OPIPKT_android.ADCLEN-2)+OPIPKT_android.TMPLEN+2+i];
      		zvalue[i]=(zvalue[i]<<8);
      		if(zvalue[i]>32767) zvalue[i]-=65536;
      		edf.azQV.addElement(new Integer(zvalue[i]));
      	}
      	tempvalue=0;
      	tempvalue= pktptr.payload[OPIPKT_android.WFRMHDRLEN-1+OPIPKT_android.TSLEN+OPIPKT_android.WLFRMHDRLEN+2*(OPIPKT_android.ADCLEN-2)]&0xff;
      	tempvalue=(tempvalue<<4);
      	if(tempvalue >32767) tempvalue -=65536;
      	ed=0;
      	ed = 0|pktptr.payload[OPIPKT_android.WFRMHDRLEN-1+OPIPKT_android.TSLEN+OPIPKT_android.WLFRMHDRLEN+2*(OPIPKT_android.ADCLEN-2)+OPIPKT_android.TMPLEN+OPIPKT_android.ACCDLEN];
    	if(ed >32767) ed-=65536;
      	edf.tmpQV.addElement(new Integer(tempvalue));
        edf.axQV.addElement(new Integer(xvalue));
        edf.ayQV.addElement(new Integer(yvalue));
        edf.edQV.addElement(new Integer(ed));
       }
       if(sampQual>0)
       {
    	   sqQV=-1000*sampQual;
       }
       else
       {
    	    appendaccxyz();
   			for(int i = 0;i<OPIPKT_android.ACCLEN;i++)
   			{
   				acczqvectoriginal.addElement(new Integer(zvalue[i]));
   			}
    	    sqQV=calcAct();
       }
	   edf.sqQV.addElement(new Integer(sqQV));
   	return true;
   }
   
   	private synchronized void appendaccxyz()
	{
		int j;
		float zzz;
		int z_new_data_average;
		accxqvect.addElement(new Integer(xvalue));
		accyqvect.addElement(new Integer(yvalue));
		z_new_data_average = 0;
		for(j=0;j< OPIPKT_android.ACCLEN;j++)
		{
			zzz = zvalue[j];
			if(zzz>32767) zzz-=65536;
			z_new_data_average+= (zzz/OPIPKT_android.ACCLEN); //prevent overflow in qint16
		}
		acczqvect.addElement(new Integer(z_new_data_average));
	}
   
   public synchronized boolean convertFFT(OPIPKT_struct pktptr)
   {
	   for(int i = 0; i < OPIPKT_android.UCEFFTLEN; i++)
       {
           uceFFTData[i]=0;
           uceFFTData[i]=(uceFFTData[i]<<8)|pktptr.payload[1+2*i];
           uceFFTData[i]=(uceFFTData[i]<<8)|pktptr.payload[1+2*i+1];
       }
	   return true;
   }
	private static synchronized float opi_calculate(int rawdata,int phymin,int phymax,int digmin,int digmax){
		return (((float) (phymax-phymin)/(float)(digmax-digmin))*(rawdata-digmin)+phymin);
	}
	public static synchronized float opi_adc_raw_to_float(int rawdata){
		return opi_calculate(rawdata,ADCPHYMIN,ADCPHYMAX,ADCDIGMIN,ADCDIGMAX);
	}
	public static synchronized float opi_x_raw_to_float(int rawdata){
		return opi_calculate(rawdata,XPHYMIN,XPHYMAX,XDIGMIN,XDIGMAX);
	}
	public static synchronized float opi_y_raw_to_float(int rawdata){
		return opi_calculate(rawdata,YPHYMIN,YPHYMAX,YDIGMIN,YDIGMAX);
	}
	public static synchronized float opi_z_raw_to_float(int rawdata){
		return opi_calculate(rawdata,ZPHYMIN,ZPHYMAX,ZDIGMIN,ZDIGMAX);
	}
	public static synchronized float opi_temp_raw_to_float(int rawdata){
		return opi_calculate(rawdata,TPHYMIN,TPHYMAX,TDIGMIN,TDIGMAX);
	}
	
	public synchronized int calcAct()
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
	    j = acczqvectoriginal.size(); //max index
	    sfastdz = (int) (acczqvectoriginal.elementAt(j-4) - azold); //current packet
	    sfastAct += (sfastdz*sfastdz);
	    sfastdz = (acczqvectoriginal.elementAt(j-3)-acczqvectoriginal.elementAt(j-4)); //current packet
	    sfastAct += (sfastdz*sfastdz);
	    sfastdz = (acczqvectoriginal.elementAt(j-2)-acczqvectoriginal.elementAt(j-3)); //current packet
	    sfastAct += (sfastdz*sfastdz);
	    sfastdz = (acczqvectoriginal.elementAt(j-1)-acczqvectoriginal.elementAt(j-2)); //current packet
	    sfastAct += (sfastdz*sfastdz);
	    xsum = accxqvect.elementAt(j/4-1); //current packet
	    ysum = accyqvect.elementAt(j/4-1); //current packet
	    fastdx = (int) (xsum - axold); //current vs. last packet
	    fastdy = (int) (ysum - ayold); //current vs. last packet
	    z4sum += (acczqvectoriginal.elementAt(j-1));
	    z4sum += (acczqvectoriginal.elementAt(j-2));
	    z4sum += (acczqvectoriginal.elementAt(j-3));
	    z4sum += (acczqvectoriginal.elementAt(j-4));
	    fastdz = (int) ((z4sum - az4old)/4); //average of 4
	    fastAct = fastdx*fastdx + fastdy*fastdy + fastdz*fastdz; //sum of square
	    slowdz = (int) ((z4sum - az16old)/16); //average of 4, 4 packets apart
	    x4mean = (int) ((axold + ax2old + ax3old + ax4old)/4); //mean of 4 packets
	    y4mean = (int) ((ayold + ay2old + ay3old + ay4old)/4); //mean of 4 packets
	    z16mean = (int) ((az4old+ az8old+ az12old+ az16old)/16); //mean of 4 packets
	    slowdx = (int) ((xsum - ax4old)/4); //4 packets apart
	    slowdy = (int) ((ysum - ay4old)/4); //4 packets apart
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
	        if((acczqvectoriginal.elementAt(j-i)-xsum+ysum - z16mean+x4mean-y4mean)> 3000) //positive with hysteresis
	        {
	            if((newstep)==-1) newstep=2; //advance 1 step
	            else if(newstep==-2) newstep=3; //advance 2 steps
	        }
	        else if((acczqvectoriginal.elementAt(j-i)-xsum+ysum - z16mean+x4mean-y4mean) < -3000) //negative with hysteresis
	        {
	            if((newstep)==1) newstep=-1; //negative
	            else if(newstep==2) newstep=-2; //negative
	        }
	    }
	    azold = acczqvectoriginal.elementAt(j-1); //new value
	    az16old = az12old; //new value
	    az12old = az8old; //new value
	    az8old= az4old; //new value
	    az4old = z4sum; //new value
	    ax4old = ax3old; //new value
	    ax3old = ax2old; //new value
	    ax2old = axold; //new value
	    axold = xsum; //new value
	    ay4old = ay3old; //new value
	    ay3old = ay2old; //new value
	    ay2old = ayold; //new value
	    ayold= ysum; //new value

	    retval =  activ;
	    return retval; //no motion <5db; slow(low) 5~10db; walk(mid) 10~20db; run(hi) 20~28db; shake(intense) >28db;
	}
}
