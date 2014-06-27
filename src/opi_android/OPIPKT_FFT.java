package opi_android;

import android.widget.TextView;


public  class OPIPKT_FFT {

	public static final int FFTKO=8; // set initial gain multiplier 8=32db 6=43db dynamic range
	public static final int FFTCO=-2;  // (-)fftco value 1 => -70-1=-71 db for 1024FFT (-70db built-in)
	//for small fft
	public  int offl,offm,stRelax_accum,stRelax_pktCt,thm,th3gm,th2gm,th1gm,th2bm,th1bm,th3bm,stRelax_prev;
	public int tsepdn,rlevel,thxx,thmm,thgm,thbm,offll,thx;
	public float rlsm,rls1,rls2,rls3;
	public int lCalc,bCalc,gCalc,mCalc,gmCalc,bmCalc;
	public int stRelax_now;
	
	public void reset()
	{
		rlevel=0;
		offl=0;
		offm=0;
		stRelax_accum=0;
		stRelax_pktCt=0;
		thm=0;
		th3gm=0;
		th2gm=0;
		th1gm=0;
		th2bm=0;
		th1bm=0;
		th3bm=0;
		stRelax_prev=0;
		tsepdn=0;
		rlevel=0;
		thxx=0;
		thmm=0;
		thgm=0;
		thbm=0;
		offll=0;
		thx=0;
		rlsm=0;
		rls1=0;
		rls2=0;
		rls3=0;
		lCalc=0;
		bCalc=0;
		gCalc=0;
		mCalc=0;
		gmCalc=0;
		bmCalc=0;
		stRelax_now=0;
	}
	
	public int calsmallfft(OPIPKT_Data_Converter OPIPKT_DT,TextView tv)
	{
		// calculations (NO averaging: M dynamic range too wide)
        lCalc = (OPIPKT_DT.uceFFTData[1] + OPIPKT_DT.uceFFTData[2] +  //1x
        		OPIPKT_DT.uceFFTData[3]);
        bCalc = (OPIPKT_DT.uceFFTData[4] + OPIPKT_DT.uceFFTData[5] +  //1x
        		OPIPKT_DT.uceFFTData[6] + OPIPKT_DT.uceFFTData[7] +
                 OPIPKT_DT.uceFFTData[8] + OPIPKT_DT.uceFFTData[9] + OPIPKT_DT.uceFFTData[10]);
        gCalc = (OPIPKT_DT.uceFFTData[17] + OPIPKT_DT.uceFFTData[18] +  // 1x
                 OPIPKT_DT.uceFFTData[19] + OPIPKT_DT.uceFFTData[20] +
                 OPIPKT_DT.uceFFTData[21] + OPIPKT_DT.uceFFTData[22] +
                 OPIPKT_DT.uceFFTData[23] + OPIPKT_DT.uceFFTData[24]);
        mCalc = (OPIPKT_DT.uceFFTData[25] + OPIPKT_DT.uceFFTData[26] +  //1x
                 OPIPKT_DT.uceFFTData[27] + OPIPKT_DT.uceFFTData[28] +
                 OPIPKT_DT.uceFFTData[29] + OPIPKT_DT.uceFFTData[30] + OPIPKT_DT.uceFFTData[31]);
        if(gCalc>mCalc) gmCalc = (gCalc) - mCalc;
        else gmCalc = 0;
        if(bCalc>mCalc*2) bmCalc = (bCalc) - mCalc*2;
        else bmCalc = 0;
        if(mCalc >= this.thx) stRelax_now = OPIPKT_helper.LEDNON;      //exclude if HF spike absolute
        else if((lCalc > this.offl) && (lCalc > mCalc)) stRelax_now = OPIPKT_helper.LEDNON; //exclude if LF spike absolute
        else if(mCalc >= this.thm || (mCalc > (this.thm/2) && gmCalc<=0)) //absolute or relative
        {
            stRelax_now = OPIPKT_helper.LEDRED;
        }
        else if((gmCalc > this.th3gm) || gCalc > (this.offm+this.th3gm) ) //absolute or relative
        {
            stRelax_now = OPIPKT_helper.LEDRED;
        }
        else if((gmCalc > this.th2gm) || gCalc > (this.offm+this.th2gm) || (bmCalc > this.th2bm) || bCalc > (this.offm*2+this.th2bm)) //absolute or relative
        {
            stRelax_now = OPIPKT_helper.LEDORG;
        }
        else if((gmCalc > this.th1gm) || gCalc > (this.offm+this.th1gm) || (bmCalc > this.th1bm) || bCalc > (this.offm*2+this.th1bm)) //absolute or relative
        {
            stRelax_now = OPIPKT_helper.LEDGRN;
        }
        else
        {
            stRelax_now = OPIPKT_helper.LEDBLU;
        }
        
        {
            //ledDataToDrawQQ.enqueue(stRelax_now);
        	this.stRelax_prev = stRelax_now;
        }
        if(stRelax_now!=0) // only add if nonzero
        {
        	this.stRelax_accum += stRelax_now;
        	this.stRelax_pktCt++;
        }
        
        if(this.stRelax_pktCt>0) 
        	tv.setText("RL:"+Integer.toString(stRelax_accum/stRelax_pktCt)+"."+Integer.toString((stRelax_accum*100)/stRelax_pktCt-(stRelax_accum/stRelax_pktCt)*100));
        return stRelax_now;
	}
	/*
	   This computes an in-place complex-to-complex FFT
	   x and y are the real and imaginary arrays of 2^m points.
	   dir =  1 gives forward transform
	   dir = -1 gives reverse transform
	*/
	public static double []  FFT(int dir,long m,double []x,double []y)
	{
	   int n,i,i1,j,k,i2,l,l1,l2;
	   double c1,c2,tx,ty,t1,t2,u1,u2,z;

	   /* Calculate the number of points */
	   n = 1;
	   for (i=0;i<m;i++)
	      n *= 2;

	   /* Do the bit reversal */
	   i2 = n >> 1;
	   j = 0;
	   for (i=0;i<n-1;i++) {
	      if (i < j) {
	         tx = x[i];
	         ty = y[i];
	         x[i] = x[j];
	         y[i] = y[j];
	         x[j] = tx;
	         y[j] = ty;
	      }
	      k = i2;
	      while (k <= j) {
	         j -= k;
	         k >>= 1;
	      }
	      j += k;
	   }

	   /* Compute the FFT */
	   c1 = (float) -1.0;
	   c2 = (float) 0.0;
	   l2 = 1;
	   for (l=0;l<m;l++) {
	      l1 = l2;
	      l2 <<= 1;
	      u1 = (float) 1.0;
	      u2 = (float) 0.0;
	      for (j=0;j<l1;j++) {
	         for (i=j;i<n;i+=l2) {
	            i1 = i + l1;
	            t1 = u1 * x[i1] - u2 * y[i1];
	            t2 = u1 * y[i1] + u2 * x[i1];
	            x[i1] = x[i] - t1;
	            y[i1] = y[i] - t2;
	            x[i] += t1;
	            y[i] += t2;
	         }
	         z =  u1 * c1 - u2 * c2;
	         u2 = u1 * c2 + u2 * c1;
	         u1 = z;
	      }
	      c2 = (float) Math.sqrt((1.0 - c1) / 2.0);
	      if (dir == 1)
	         c2 = -c2;
	      c1 = (float) Math.sqrt((1.0 + c1) / 2.0);
	   }

	   /* Scaling for forward transform */
	   if (dir == 1) {
	      for (i=0;i<n;i++) {
	         x[i] /= n;
	         y[i] /= n;
	      }
	   }
	   
	   double newarray[] = new double[2*n];
	   for (i=0;i<n;i++) {
		   newarray[i] = x[i];
	      }
	   for (i=0;i<n;i++) {
		   newarray[n+i] = y[i];
	      }
	   return  newarray;
	}
}


