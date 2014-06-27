package opi_android;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Vector;
import java.util.TimeZone;

import opi.relax.openfile.CallbackBundle;
import opi.relax.openfile.OpenFileDialog;
import opi.relax.opirelax.MainActivity;
import opi.relax.opirelax.R;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;
import android.widget.ViewFlipper;
	
public class OPIPKT_EDF_Writer{
	//write file
	public boolean writingfunOK; //define whether the writing function is ok or not?
	public boolean leave; //if user want to leave, then true
	OutputStreamWriter write=null;
	File f=null;
	BufferedWriter writer=null;
	RandomAccessFile raf=null;
	public static final int checkEDFDialogId=1;
	public static final int EDFDRDURSEC =8;	 // number of truesense data frames per second
	public static final int MAXMISSFRMS =15000;	 // Maximum missing frames to insert in edf >1Hr=14400
	public int pdnnum=-1;
	public String FilePath;
	public String localPatientID="X X X X";
	public String localRecordID="X X";
	public String filename="";
	private Date currentDate;
	public long wroteDRCt=0;
	public long firstFrmTS=0;
	long currentmillis=0;
	public long firstmillis=0;
	public long lastmillism=0;
	public Vector<Long> tagTS=new Vector<Long>();
	public Vector<String> tagTextQV=new Vector<String>();
	public Vector<Long> tsQV=new Vector<Long>();
	public Vector<Integer> skpQV=new Vector<Integer>();
	public Vector<Integer> batQV=new Vector<Integer>();
	public Vector<Integer> adcQV=new Vector<Integer>();
	public Vector<Integer> tmpQV=new Vector<Integer>();
	public Vector<Integer> axQV=new Vector<Integer>();
	public Vector<Integer> ayQV=new Vector<Integer>();
	public Vector<Integer> azQV=new Vector<Integer>();
	public Vector<Integer> sqQV=new Vector<Integer>();
	public Vector<Integer> edQV=new Vector<Integer>();
	
	//Async
	private boolean canwrite=false;
	public Vector<Long> tsQVAsync=new Vector<Long>();
	public Vector<Integer> skpQVAsync=new Vector<Integer>();
	public Vector<Integer> batQVAsync=new Vector<Integer>();
	public Vector<Integer> adcQVAsync=new Vector<Integer>();
	public Vector<Integer> tmpQVAsync=new Vector<Integer>();
	public Vector<Integer> axQVAsync=new Vector<Integer>();
	public Vector<Integer> ayQVAsync=new Vector<Integer>();
	public Vector<Integer> azQVAsync=new Vector<Integer>();
	public Vector<Integer> sqQVAsync=new Vector<Integer>();
	public Vector<Integer> edQVAsync=new Vector<Integer>();
	
	/***
	 * a constructor with file path search
	 */
	public OPIPKT_EDF_Writer()
	{
		super();
		FilePath = Environment.getExternalStorageDirectory().getPath();
		FilePath += "/OPI/";
		File wallpaperDirectory = new File(FilePath);
		wallpaperDirectory.mkdirs();
		wroteDRCt=0;
		tagTextQV.clear();
		tagTS.clear();
		tsQV.clear();
		skpQV.clear();
		batQV.clear();
		adcQV.clear();
		tmpQV.clear();
		axQV.clear();
		ayQV.clear();
		azQV.clear();
		sqQV.clear();
		edQV.clear();
		canwrite=true;
	}

	/***
	 * write header file
	 * @param lpidp
	 * @param lridp
	 * @param numDataRecs
	 */
	public static synchronized void edfEhdropiwrite(String lpidp, String lridp,long numDataRecs,RandomAccessFile raf,Date currentDate)
	{
		String tempstr;
	  	try {
	  		tempstr="0       ";
	  		raf.write(tempstr.getBytes(),0,8); // edf version of data format
	  		
		  	raf.write(OPIPKT_helper.leftJustified_byte(lpidp.toCharArray(),80,' '),0,80);// local patient identification
		  	
		  	raf.write(OPIPKT_helper.leftJustified_byte(lridp.toCharArray(),80,' '),0,80); // local recording identification
		  	
		  	SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyHH.mm.ss");
		  	tempstr = sdf.format(currentDate);
		  	raf.write(tempstr.getBytes(),0,16);// startdate and starttime
		  	
		  	tempstr="2048    ";
			raf.write(tempstr.getBytes(),0,8);  // number of header bytes (256+7signals*256)
			
			tempstr="EDF+C";
			raf.write(OPIPKT_helper.leftJustified_byte(tempstr.toCharArray(),44,' '),0,44);  // format type (reserved)
			
			tempstr=Long.toString(numDataRecs);
			raf.write(OPIPKT_helper.leftJustified_byte(tempstr.toCharArray(),8,' '),0,8);// number of data records
			
			tempstr="8       ";
			raf.write(tempstr.getBytes(),0,8);  // duration of a data record in seconds
			
			tempstr="7   ";
			raf.write(tempstr.getBytes(),0,4);  // duration of a data record in seconds
			
			// signal labels
			tempstr="ADC             ";
			raf.write(tempstr.getBytes(),0,16);// maybe change if know type later on
			
			tempstr="Accel. X-axis   ";
			raf.write(tempstr.getBytes(),0,16);
			
			tempstr="Accel. Y-axis   ";
			raf.write(tempstr.getBytes(),0,16);
			
			tempstr="Accel. Z-axis   ";
			raf.write(tempstr.getBytes(),0,16);
			
			tempstr="Temperature     ";
			raf.write(tempstr.getBytes(),0,16);
			
			tempstr="Activity        ";
			raf.write(tempstr.getBytes(),0,16);
			
			tempstr="EDF Annotations ";
			raf.write(tempstr.getBytes(),0,16);
			
			
			// transducer type
			tempstr=" ";
			raf.write(OPIPKT_helper.leftJustified_byte(tempstr.toCharArray(),80,' '),0,80);
			raf.write(OPIPKT_helper.leftJustified_byte(tempstr.toCharArray(),80,' '),0,80);
			raf.write(OPIPKT_helper.leftJustified_byte(tempstr.toCharArray(),80,' '),0,80);
			raf.write(OPIPKT_helper.leftJustified_byte(tempstr.toCharArray(),80,' '),0,80);
			raf.write(OPIPKT_helper.leftJustified_byte(tempstr.toCharArray(),80,' '),0,80);
			raf.write(OPIPKT_helper.leftJustified_byte(tempstr.toCharArray(),80,' '),0,80);
			raf.write(OPIPKT_helper.leftJustified_byte(tempstr.toCharArray(),80,' '),0,80);
			
			
			// physical dimensions
			tempstr="uV      ";
			raf.write(tempstr.getBytes(),0,8);
			tempstr="g       ";
			raf.write(tempstr.getBytes(),0,8);
			raf.write(tempstr.getBytes(),0,8);
			raf.write(tempstr.getBytes(),0,8);
			tempstr="degreeC ";
			raf.write(tempstr.getBytes(),0,8);
			tempstr="dB      ";
			raf.write(tempstr.getBytes(),0,8);
			tempstr="        ";
			raf.write(tempstr.getBytes(),0,8);
			
			
		    // physical mins and maxs
			tempstr="-800    ";
			raf.write(tempstr.getBytes(),0,8);
			tempstr="-2      ";
			raf.write(tempstr.getBytes(),0,8);
			raf.write(tempstr.getBytes(),0,8);
			raf.write(tempstr.getBytes(),0,8);
			tempstr="-47     ";
			raf.write(tempstr.getBytes(),0,8);
			tempstr="-50     ";
			raf.write(tempstr.getBytes(),0,8);
			tempstr="-1      ";
			raf.write(tempstr.getBytes(),0,8);
			tempstr="800     ";
			raf.write(tempstr.getBytes(),0,8);
			tempstr="2       ";
			raf.write(tempstr.getBytes(),0,8);
			raf.write(tempstr.getBytes(),0,8);
			raf.write(tempstr.getBytes(),0,8);
			tempstr="241     ";
			raf.write(tempstr.getBytes(),0,8);
			tempstr="50      ";
			raf.write(tempstr.getBytes(),0,8);
			tempstr="1       ";
			raf.write(tempstr.getBytes(),0,8);
			
			
			
			// digital mins and maxs
			tempstr="-20480  ";
			raf.write(tempstr.getBytes(),0,8);
			tempstr="-32768  ";
			raf.write(tempstr.getBytes(),0,8);
			raf.write(tempstr.getBytes(),0,8);
			raf.write(tempstr.getBytes(),0,8);
			tempstr="0       ";
			raf.write(tempstr.getBytes(),0,8);
			tempstr="-32768  ";
			raf.write(tempstr.getBytes(),0,8);
			raf.write(tempstr.getBytes(),0,8);
			tempstr="20480   ";
			raf.write(tempstr.getBytes(),0,8);
			tempstr="32767   ";
			raf.write(tempstr.getBytes(),0,8);
			raf.write(tempstr.getBytes(),0,8);
			raf.write(tempstr.getBytes(),0,8);
			tempstr="4080    ";
			raf.write(tempstr.getBytes(),0,8);
			tempstr="32767   ";
			raf.write(tempstr.getBytes(),0,8);
			raf.write(tempstr.getBytes(),0,8);
			
			
			// prefiltering
			tempstr=" ";
			raf.write(OPIPKT_helper.leftJustified_byte(tempstr.toCharArray(),80,' '),0,80);
			raf.write(OPIPKT_helper.leftJustified_byte(tempstr.toCharArray(),80,' '),0,80);
			raf.write(OPIPKT_helper.leftJustified_byte(tempstr.toCharArray(),80,' '),0,80);
			raf.write(OPIPKT_helper.leftJustified_byte(tempstr.toCharArray(),80,' '),0,80);
			raf.write(OPIPKT_helper.leftJustified_byte(tempstr.toCharArray(),80,' '),0,80);
			raf.write(OPIPKT_helper.leftJustified_byte(tempstr.toCharArray(),80,' '),0,80);
			raf.write(OPIPKT_helper.leftJustified_byte(tempstr.toCharArray(),80,' '),0,80);
			
			
			
			// number of samples in each data record (8s)
			tempstr="2048    ";
			raf.write(tempstr.getBytes(),0,8);
			tempstr="32      ";
			raf.write(tempstr.getBytes(),0,8);
			raf.write(tempstr.getBytes(),0,8);
			tempstr="128     ";
			raf.write(tempstr.getBytes(),0,8);
			tempstr="32      ";
			raf.write(tempstr.getBytes(),0,8);
			raf.write(tempstr.getBytes(),0,8);
			tempstr="30      ";
			raf.write(tempstr.getBytes(),0,8);
			
		    // reserved fields
			tempstr=" ";
			raf.write(OPIPKT_helper.leftJustified_byte(tempstr.toCharArray(),32,' '),0,32);
			raf.write(OPIPKT_helper.leftJustified_byte(tempstr.toCharArray(),32,' '),0,32);
			raf.write(OPIPKT_helper.leftJustified_byte(tempstr.toCharArray(),32,' '),0,32);
			raf.write(OPIPKT_helper.leftJustified_byte(tempstr.toCharArray(),32,' '),0,32);
			raf.write(OPIPKT_helper.leftJustified_byte(tempstr.toCharArray(),32,' '),0,32);
			raf.write(OPIPKT_helper.leftJustified_byte(tempstr.toCharArray(),32,' '),0,32);
			raf.write(OPIPKT_helper.leftJustified_byte(tempstr.toCharArray(),32,' '),0,32);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/***
	 * build file everytime when user create a new live mode observation
	 * return true, if writing fun is ok
	 */
	public synchronized boolean buildfile(OPIPKT_EDF_Reader OPIPKT_reader,ViewFlipper appVF,MainActivity main)
	{
		wroteDRCt=0;
		tagTextQV.clear();
		tagTS.clear();
		tsQV.clear();
		skpQV.clear();
		batQV.clear();
		adcQV.clear();
		tmpQV.clear();
		axQV.clear();
		ayQV.clear();
		azQV.clear();
		sqQV.clear();
		edQV.clear();
		canwrite=true;
		localPatientID="X X X X";
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
		currentDate = new Date();
		String currentDateandTime;
		currentDateandTime = sdf.format(currentDate);
		main.tvStartdate.setText("Start date/time:"+currentDateandTime);
		filename = "E"+currentDateandTime+"_"+Integer.toString(pdnnum)+".edf";
		localRecordID = String.format("X X %S_OPITSE%03d",localUTCOffset(),pdnnum);
		SimpleDateFormat stQDT = new SimpleDateFormat("yyyy");
		localRecordID= "-"+ stQDT.format(currentDate)+" "+localRecordID;
		stQDT = new SimpleDateFormat("MMM");
		localRecordID=stQDT.format(currentDate).toUpperCase()+localRecordID;
		stQDT = new SimpleDateFormat("dd");
		localRecordID="Startdate "+stQDT.format(currentDate)+"-"+localRecordID;
		firstFrmTS=System.currentTimeMillis();
		f = new File(FilePath+filename);
		if(!f.exists())
			try {
				f.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();			
			}
		try {
			write = new OutputStreamWriter(
					new FileOutputStream(f),"UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		writer = new BufferedWriter(write);
		try {
			writer.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			raf = new RandomAccessFile(FilePath+filename, "rw");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			raf.seek(0);
			OPIPKT_EDF_Writer.edfEhdropiwrite(localPatientID,localRecordID ,-1,raf,currentDate);
			raf.close();
			raf = new RandomAccessFile(FilePath+filename, "rw");
			int temp;
			temp = OPIPKT_EDF_Reader.edfhdrread(OPIPKT_reader, raf);
			raf.close();
			leave=false;
			if(temp!=0)
			{
				 main.ToastMessage=Toast.makeText(appVF.getContext(),"Error:"+Integer.toString(temp), Toast.LENGTH_SHORT);
				 main.ToastMessage.show();
				writingfunOK=false;
			}
			else
			{
				writingfunOK=true;
				raf = new RandomAccessFile(FilePath+filename, "rw");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return writingfunOK;
	}
	
		public static Dialog createDialog(int id, Context context, String title, CallbackBundle callbackp){  
		 AlertDialog.Builder builder = new AlertDialog.Builder(context);
		 final CallbackBundle callback = callbackp;
		 builder.setTitle("Confirm");
   	     builder.setMessage("System can't record data correctly, continue or not?");
   	     builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
   	    	 public void onClick(DialogInterface dialog, int which) {
   	            // Do nothing but close the dialog
   	    		Bundle bundle = new Bundle();  
                bundle.putBoolean("LEAVE",false);
                callback.callback(bundle);
   	    		dialog.dismiss();
   	        }});
   	     	builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
   	     	@Override
   	        public void onClick(DialogInterface dialog, int which) {
   	            // Do nothing
   	     		Bundle bundle = new Bundle();  
   	     		bundle.putBoolean("LEAVE",true);
   	     		callback.callback(bundle);
   	            dialog.dismiss();
   	        }
   	     	});  
	        Dialog dialog = builder.create();  
	        dialog.setTitle(title);  
	        return dialog;  
	    }
	/***
	 * write edf file header, and initialize some data
	 */
	public synchronized void reset()
	{
		double beginOffFrms,beginOffFrmsj;
		long beginOffFrmsi;
		 // write EDF header, will need to come back later to adjust number of data records
		 // which are unknown (-1) at this point
		 // also pass the dataType and use the write header if need to write events
		 OPIPKT_EDF_Writer.edfEhdropiwrite(localPatientID,localRecordID ,-1,raf,currentDate);	
		 firstmillis=firstFrmTS;
		 //tagTextQV.insertElementAt(new String("DataStart"),0);
		 //tagTS.insertElementAt(new Long(firstFrmTS),0);
		 currentmillis=System.currentTimeMillis();
		 beginOffFrms = ((double) (currentmillis % 1000))*OPIPKT_android.TSERTCFREQ/1000/OPIPKT_android.ADCLEN;
		 beginOffFrmsi = (long) (beginOffFrms);
		 beginOffFrmsj = beginOffFrms-((double) beginOffFrmsi);
		 // put in default values for beginning in constant frame units
		 for(long j=beginOffFrmsi; j > 0; j--)
		 {
			 tsQV.insertElementAt(new Long(tsQV.firstElement()-OPIPKT_android.ADCLEN/OPIPKT_android.TSERTCFREQ*OPIPKT_android.UCERTCFREQ*beginOffFrmsi), 0);
			 skpQV.insertElementAt(new Integer(0),0);
			 batQV.insertElementAt(new Integer(1),0);
			 sqQV.insertElementAt(new Integer(0),0);
			 edQV.insertElementAt(new Integer(0),0);
		     for(int k = 0; k < OPIPKT_android.ADCLEN; k++)
		    	 adcQV.insertElementAt(new Integer(0),0);
		     for(int k = 0; k < OPIPKT_android.TMPLEN; k++)
		    	 tmpQV.insertElementAt(new Integer(1024),0);

		     for(int k = 0; k < OPIPKT_android.ACCLEN/4; k++)
		     {
		    	 axQV.insertElementAt(new Integer(0),0);
		    	 ayQV.insertElementAt(new Integer(0),0);
		     }
		     for(int k = 0; k < OPIPKT_android.ACCLEN; k++)
		    	 azQV.insertElementAt(new Integer(0),0);
		 }
		 // add less than a frame data
		 if(beginOffFrmsj > 0.5)
		 {
			 tsQV.insertElementAt(new Long(tsQV.firstElement()-OPIPKT_android.ADCLEN/OPIPKT_android.TSERTCFREQ*OPIPKT_android.UCERTCFREQ*(beginOffFrmsi+1)), 0);
			 skpQV.insertElementAt(new Integer(0),0);
			 batQV.insertElementAt(new Integer(1),0);
			 sqQV.insertElementAt(new Integer(0),0);
			 edQV.insertElementAt(new Integer(0),0);
			 tmpQV.insertElementAt(new Integer(1024),0);
			 axQV.insertElementAt(new Integer(0),0);
			 ayQV.insertElementAt(new Integer(0),0);
		 }
		 for(int k = 0; k < ((int) (beginOffFrmsj*OPIPKT_android.ADCLEN)); k++)
			 adcQV.insertElementAt(new Integer(0),0);
		 for(int k = 0; k < ((int) (beginOffFrmsj*OPIPKT_android.ACCLEN)); k++)
			 azQV.insertElementAt(new Integer(0),0);
		 // fix up tag times by adding
		 Long tempsave;
		 for(int k = 0; k < tagTS.size(); k++)
		 {
			 tempsave = tagTS.elementAt(k);
			 tagTS.removeElementAt(k);
			 tagTS.insertElementAt(new Long(tempsave+((long) (beginOffFrms*OPIPKT_android.ADCLEN/OPIPKT_android.TSERTCFREQ*OPIPKT_android.UCERTCFREQ))), k);
		 }
	}
	
	
	
	
	
	/***
	  * Need to know beforedhand knowledge how data is formatted in different streams
	  * Will alter temperature stream for averaging so need that flag
	  * Returns number of data records written according to opi EDF format
	  */
	private class  edfEwriteAsync extends AsyncTask<Void, Void, Integer>{
		@Override
        protected Integer doInBackground(Void... params) {
			canwrite=false;
			boolean noMore;    // indicate if no more data to write into data records
		    long i, j, dataRecordCt;
		    String tempQS;
		    // Write EDF in data record segments
		    noMore = false;
		    dataRecordCt = 0;
		    for(j = 0; j < tmpQVAsync.size()/(OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC); j++)
		    {
		        // make sure there is enough data for another data record, otherwise get out
		        if((((j+1)*OPIPKT_android.ADCLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC-1) > adcQVAsync.size()) ||
		                (((j+1)*OPIPKT_android.TMPLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC-1) > tmpQVAsync.size()) ||
		                (((j+1)*OPIPKT_android.ACCLEN/4*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC-1) > axQVAsync.size()) ||
		                (((j+1)*OPIPKT_android.ACCLEN/4*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC-1) > ayQVAsync.size()) ||
		                (((j+1)*OPIPKT_android.ACCLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC-1) > azQVAsync.size()) ||
		                (((j+1)*1*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC-1) > sqQVAsync.size()))
		            break;  
		        for(i = 0; i < OPIPKT_android.ADCLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC; i++)
					try {
							raf.write(OPIPKT_helper.int16tobyte(adcQVAsync.elementAt((int) (j*OPIPKT_android.ADCLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC+i))));
							
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	   
		        for(i = 0; i < OPIPKT_android.ACCLEN/4*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC; i++)
					try {
						raf.write(OPIPKT_helper.int16tobyte(axQVAsync.elementAt((int) (j*OPIPKT_android.ACCLEN/4*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC+i))));
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		        	   
		        for(i = 0; i < OPIPKT_android.ACCLEN/4*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC; i++)
					try {
						raf.write(OPIPKT_helper.int16tobyte(ayQVAsync.elementAt((int) (j*OPIPKT_android.ACCLEN/4*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC+i))));
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		        	    
		        for(i = 0; i < OPIPKT_android.ACCLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC; i++)
					try {
						raf.write(OPIPKT_helper.int16tobyte(azQVAsync.elementAt((int) (j*OPIPKT_android.ACCLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC+i))));
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		            
		        for(i = 0; i < OPIPKT_android.TMPLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC; i++)
					try {
						raf.write(OPIPKT_helper.int16tobyte(tmpQVAsync.elementAt((int) (j*OPIPKT_android.TMPLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC+i))));
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		            
		        for(i = 0; i < 1*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC; i++)
					try {
						raf.write(OPIPKT_helper.int16tobyte(sqQVAsync.elementAt((int) (j*1*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC+i))));
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		        
		        // EDF Annotations, max of 2 annotations per data record, each with 52 total chars
		  
		        char a=(char)20;
		        char b=(char)0;
		        String tempQBAString = "+"+Integer.toString((int) ((dataRecordCt+ wroteDRCt)*EDFDRDURSEC))+a+a+b;
		     
		        for(i = 0; i < 2; i++)
		        {
		            if((tagTS.size() > 0) && (tagTS.elementAt(0) <= (tsQVAsync.elementAt((int) (j*1*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC)) + 1*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC*OPIPKT_android.ADCLEN*OPIPKT_android.UCERTCFREQ/OPIPKT_android.TSERTCFREQ)))
		            {
		                tempQS = tagTextQV.elementAt(0);
		                if(tempQS.length()>14)
		                tempQS = tempQS.substring(0, 14); // limit text to 14 charcters
		                tempQBAString = tempQBAString+"+"+Float.toString(((float)tagTS.elementAt(0)-(float)firstFrmTS)/1000)+a+tempQS+a+b;
		                tagTS.removeElementAt(0);
		                tagTextQV.removeElementAt(0);
		            }
		        }
		  
		        try {
					raf.write(OPIPKT_helper.leftJustified_byte(tempQBAString.toCharArray(),60,b),0,60);
			    } catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		        dataRecordCt++;
		    }
		    wroteDRCt += dataRecordCt;
		    return 1;
		}
		
		protected void onPostExecute(Integer result) {
			if(result==1)
			{
				canwrite=true;
			}
		}
	} 
	
	
	private synchronized void edfEwriteSync()
	{
		boolean noMore;    // indicate if no more data to write into data records
	    long i, j, dataRecordCt;
	    String tempQS;
	    // Write EDF in data record segments
	    noMore = false;
	    dataRecordCt = 0;
	    for(j = 0; j < tmpQV.size()/(OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC); j++)
	    {
	        // make sure there is enough data for another data record, otherwise get out
	        if((((j+1)*OPIPKT_android.ADCLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC-1) > adcQV.size()) ||
	                (((j+1)*OPIPKT_android.TMPLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC-1) > tmpQV.size()) ||
	                (((j+1)*OPIPKT_android.ACCLEN/4*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC-1) > axQV.size()) ||
	                (((j+1)*OPIPKT_android.ACCLEN/4*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC-1) > ayQV.size()) ||
	                (((j+1)*OPIPKT_android.ACCLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC-1) > azQV.size()) ||
	                (((j+1)*1*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC-1) > sqQV.size()))
	            break;  
	        for(i = 0; i < OPIPKT_android.ADCLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC; i++)
				try {
						raf.write(OPIPKT_helper.int16tobyte(adcQV.elementAt((int) (j*OPIPKT_android.ADCLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC+i))));
						
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
   
	        for(i = 0; i < OPIPKT_android.ACCLEN/4*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC; i++)
				try {
					raf.write(OPIPKT_helper.int16tobyte(axQV.elementAt((int) (j*OPIPKT_android.ACCLEN/4*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC+i))));
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        	   
	        for(i = 0; i < OPIPKT_android.ACCLEN/4*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC; i++)
				try {
					raf.write(OPIPKT_helper.int16tobyte(ayQV.elementAt((int) (j*OPIPKT_android.ACCLEN/4*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC+i))));
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        	    
	        for(i = 0; i < OPIPKT_android.ACCLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC; i++)
				try {
					raf.write(OPIPKT_helper.int16tobyte(azQV.elementAt((int) (j*OPIPKT_android.ACCLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC+i))));
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	            
	        for(i = 0; i < OPIPKT_android.TMPLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC; i++)
				try {
					raf.write(OPIPKT_helper.int16tobyte(tmpQV.elementAt((int) (j*OPIPKT_android.TMPLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC+i))));
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	            
	        for(i = 0; i < 1*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC; i++)
				try {
					raf.write(OPIPKT_helper.int16tobyte(sqQV.elementAt((int) (j*1*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC+i))));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        
	        // EDF Annotations, max of 2 annotations per data record, each with 52 total chars
	        char a=(char)20;
	        char b=(char)0;
	        String tempQBAString = "+"+Integer.toString((int) ((dataRecordCt+ wroteDRCt)*EDFDRDURSEC))+a+a+b;
	     
	        for(i = 0; i < 2; i++)
	        {
	            if((tagTS.size() > 0) && (tagTS.elementAt(0) <= (tsQV.elementAt((int) (j*1*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC)) + 1*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC*OPIPKT_android.ADCLEN*OPIPKT_android.UCERTCFREQ/OPIPKT_android.TSERTCFREQ)))
	            {
	            	tempQS = tagTextQV.elementAt(0);
	                if(tempQS.length()>14)
	                tempQS = tempQS.substring(0, 14); // limit text to 14 charcters
	                tempQBAString = tempQBAString+"+"+Float.toString((lastmillism-firstmillis)/1000)+a+tempQS+a+b;
	                tagTS.removeElementAt(0);
	                tagTextQV.removeElementAt(0);
	            }
	        }
	  
	        try {
				raf.write(OPIPKT_helper.leftJustified_byte(tempQBAString.toCharArray(),60,b),0,60);
	        } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        dataRecordCt++;
	    }
	    wroteDRCt += dataRecordCt;
	}
	
	/***
	 * check data is enough or not , if it is enough then write a data record
	 */
	@SuppressWarnings("unchecked")
	public synchronized void checkEDF()
	{
		if(adcQV.size()>OPIPKT_android.ADCLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC )
		{
			procQV();
			if(wroteDRCt==0)
			{
				reset();
			}
			// write to file, add any tags that are within the timestamp ranges
            if(f != null)
            {   
            	//Async
            	for(int i=0;i<50;i++)
            	{
            		if(canwrite==false)
            			OPIPKT_helper.opiwait(500);
            		else
            			break;
            	}
            	tsQVAsync=(Vector<Long>) tsQV.clone();
            	skpQVAsync=(Vector<Integer>) skpQV.clone();
                batQVAsync=(Vector<Integer>) batQV.clone();
            	adcQVAsync=(Vector<Integer>) adcQV.clone();
            	tmpQVAsync=(Vector<Integer>) tmpQV.clone();
            	axQVAsync=(Vector<Integer>) axQV.clone();
            	ayQVAsync=(Vector<Integer>) ayQV.clone();
            	azQVAsync=(Vector<Integer>) azQV.clone();
            	sqQVAsync=(Vector<Integer>) sqQV.clone();
            	edQVAsync=(Vector<Integer>) edQV.clone();
            	new edfEwriteAsync().execute((Void) null);
                //clip off the parts it wrote
                for(int k=0;k<1*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC;k++)
                {
                	tsQV.removeElementAt(0);
                	skpQV.removeElementAt(0);
                	batQV.removeElementAt(0);
                	sqQV.removeElementAt(0);
                	edQV.removeElementAt(0);
                }
                for(int k=0;k<OPIPKT_android.ADCLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC;k++)
                	adcQV.removeElementAt(0);
                for(int k=0;k<OPIPKT_android.TMPLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC;k++)
                	tmpQV.removeElementAt(0);
                for(int k=0;k<OPIPKT_android.ACCLEN/4*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC;k++)
                {
                	axQV.removeElementAt(0);
                	ayQV.removeElementAt(0);
                }
                for(int k=0;k<OPIPKT_android.ACCLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC;k++)
                	azQV.removeElementAt(0);
            }
		}
	}
	
	
	// Returns a QString in format of "UTC+HH:mm" with the offset of local time
	// from UTC contained in +HH:mm. This function depends on the system time.
	// granularity is 15 minutes
	public synchronized String localUTCOffset()
	{
	    String retQS;
	    retQS= "UTC"+getCurrentTimezoneOffset();
	    return retQS;
	}
	
	
	@SuppressLint("DefaultLocale") public static synchronized String getCurrentTimezoneOffset() {
		TimeZone tz = TimeZone.getDefault();  
	    Calendar cal = GregorianCalendar.getInstance(tz);
	    long offsetInMillis = tz.getOffset(cal.getTimeInMillis());
	    String offset = String.format("%02d:%02d", Math.abs(offsetInMillis / 3600000), Math.abs((offsetInMillis / 60000) % 60));
	    offset = (offsetInMillis >= 0 ? "+" : "-") + offset;
	    return offset;
	}
	
	@SuppressLint("UseValueOf") public synchronized void procQV()
	{
		int i, j;
	    int currADCIndex;  // need to keep track since sometimes there is 62 data (usually 64)
	    double cntr2FrmRatioSet, delFrmCt;
	    int missFrmCt;
	    long tempsave;
	    // Set the counter to frame ratio
	    cntr2FrmRatioSet = ((double) (OPIPKT_android.ADCLEN*OPIPKT_android.UCERTCFREQ))/((double) (OPIPKT_android.TSERTCFREQ));
	    // process all the frames backwards since inserting data into vector
	    if(skpQV.lastElement() == 0)
	        currADCIndex = adcQV.size()- OPIPKT_android.ADCLEN;
	    else    // skipped sample
	        currADCIndex = adcQV.size()-(OPIPKT_android.ADCLEN-2);
	    for(i = tsQV.size()-1; i > 0; i--)
	    {
	        delFrmCt = ((double) (tsQV.elementAt(i)-tsQV.elementAt(i-1)))/cntr2FrmRatioSet;
	        missFrmCt = ((int) (delFrmCt + 0.5))-1;	// for rounding
	        if((missFrmCt > 0) && (missFrmCt < MAXMISSFRMS))  // only fill positive frames and that less than a number
	        // qvector should only be filled with monotonically increasing time
	        {
	        	//insert missing timeslots in array
	        	for(int k=0;k<missFrmCt;k++)
	        	tsQV.insertElementAt(new Long(tsQV.elementAt(i-1)+((int)cntr2FrmRatioSet)), i);
	       
	        	for(j = 1; j < missFrmCt; j++)
	            {
	        		tempsave=tsQV.elementAt(i+j-1);
	        		tsQV.removeElementAt(i+j);
	                tsQV.insertElementAt(new Long(tempsave+((long)cntr2FrmRatioSet)),i+j);
	            }
	        	for(int k=0;k<missFrmCt;k++)
	        	{
	        		skpQV.insertElementAt(new Integer(0), i);
	        		batQV.insertElementAt(new Integer(1), i);
	        		tmpQV.insertElementAt(new Integer(tmpQV.elementAt(i-1)),i);
	        		axQV.insertElementAt(new Integer(axQV.elementAt(i-1)),i);
	        		ayQV.insertElementAt(new Integer(ayQV.elementAt(i-1)),i);
	        		sqQV.insertElementAt(new Integer(-4000),i);//4 for missing pkt
	        		edQV.insertElementAt(new Integer(0), i);
	        	}
	        	for(int k=0;k< missFrmCt*OPIPKT_android.ADCLEN;k++)
	        		adcQV.insertElementAt(new Integer(0), currADCIndex);
	        	for(int k=0;k<missFrmCt*OPIPKT_android.ACCLEN;k++)
	        		azQV.insertElementAt(new Integer(azQV.elementAt(i*OPIPKT_android.ACCLEN-1)), i*OPIPKT_android.ACCLEN);
	        }
	        else if(missFrmCt != 0)
	        {
	        	//Toast.makeText(ct,"invalid missing frame count to add, ignoring", Toast.LENGTH_SHORT).show();
	        }
	        if(skpQV.elementAt(i-1) == 0)
	            currADCIndex -= OPIPKT_android.ADCLEN;
	        else
	            currADCIndex -= (OPIPKT_android.ADCLEN-2);
	    }
	    if(i > 0)
	    {
	    	for(int k=0;k<i;k++)
	    	{
	    		tsQV.removeElementAt(0);
	    		skpQV.removeElementAt(0);
	    		batQV.removeElementAt(0);
	    		axQV.removeElementAt(0);
	    		ayQV.removeElementAt(0);
	    		tmpQV.removeElementAt(0);
	    		sqQV.removeElementAt(0);
	    		edQV.removeElementAt(0);
	    	}
	    	for(int k=0;k<currADCIndex;k++)
	    		adcQV.removeElementAt(0);
	    	for(int k=0;k<i*OPIPKT_android.ACCLEN;k++)
	    		azQV.removeElementAt(0);
	    }
	}
	
	public synchronized void deleteFile(String filename,MainActivity main)
	{
		main.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + FilePath)));
		File file = new File(filename);
		file.delete();
		Uri mediaUri = Uri.parse("file://"+filename);
		Intent mediaIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, mediaUri);
		main.sendBroadcast(mediaIntent);
	}
	public synchronized String CloseFile(MainActivity main)
	{
		    main.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + FilePath)));
			Uri mediaUri = Uri.parse("file://"+FilePath+filename);
			Intent mediaIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, mediaUri);
			main.sendBroadcast(mediaIntent);
			String filemessage="";
    		if((writer!=null)&&(writingfunOK==true))	
    		{
    			currentmillis=System.currentTimeMillis();
            	lastmillism=currentmillis;
    			for(int i=0;i<50;i++)
    			{
    				if(canwrite==false)
    					 OPIPKT_helper.opiwait(1000);
    				else
    					break;
    			}
    			
    			if(wroteDRCt==0)
    			{
    				try {
						raf.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    				deleteFile(FilePath+filename,main);
    				filemessage="Data record is not enough!";
    			}
    			else
    			{
    				// write tail of data, filling up datarecord so can be written
                    if(tsQV.size() > 0)
                    {
                    	double beginOffFrms = ((double) (currentmillis % 1000))*OPIPKT_android.TSERTCFREQ/1000/OPIPKT_android.ADCLEN;
                        //tagTS.addElement(new Long((long) (tsQV.elementAt(tsQV.size()-1)+OPIPKT_android.ADCLEN*OPIPKT_android.UCERTCFREQ/OPIPKT_android.TSERTCFREQ+OPIPKT_android.ADCLEN*OPIPKT_android.UCERTCFREQ*beginOffFrms/OPIPKT_android.TSERTCFREQ)));
                        //tagTextQV.addElement(new String("DataEnd"));
                    	// put in default values for last record
                        while(tsQV.size() < 1*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC)
                        {
                        	tsQV.addElement(new Long(tsQV.lastElement()+OPIPKT_android.ADCLEN*OPIPKT_android.UCERTCFREQ/OPIPKT_android.TSERTCFREQ));
                            skpQV.addElement(new Integer(0));
                            batQV.addElement(new Integer(1));
                            sqQV.addElement(new Integer(0));
                            edQV.addElement(new Integer(0));
                        }
                        while(adcQV.size() < OPIPKT_android.ADCLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC)
                            adcQV.addElement(new Integer(0));
                        while(tmpQV.size() < OPIPKT_android.TMPLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC)
                            tmpQV.addElement(new Integer(25));   
                        while(axQV.size() < OPIPKT_android.ACCLEN/4*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC)
                        {
                            axQV.addElement(new Integer(0));
                            ayQV.addElement(new Integer(0));
                        }
                        while(azQV.size() < OPIPKT_android.ACCLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*EDFDRDURSEC)
                            azQV.addElement(new Integer(0));
                        procQV();
                        // write to file, add any tags that are within the timestamp ranges
                        edfEwriteSync();
                    }
    				try {
    					raf.seek(0);
    					OPIPKT_EDF_Writer.edfEhdropiwrite(localPatientID, localRecordID,wroteDRCt,raf,currentDate);
    					filemessage=Long.toString(wroteDRCt) +" data recards in edf file";
    					raf.close();
    				} catch (IOException e1) {
    					// TODO Auto-generated catch block
    					e1.printStackTrace();
    				}
    				main.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + FilePath)));
    				Uri mediaUrib = Uri.parse("file://"+FilePath+filename);
    				Intent mediaIntentb = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, mediaUrib);
    				main.sendBroadcast(mediaIntentb);
    			}
                wroteDRCt = 0;
    		}
		    return filemessage;
	}
}
