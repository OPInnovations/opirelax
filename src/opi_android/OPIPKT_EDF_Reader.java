package opi_android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.Vector;

import opi.relax.opirelax.MainActivity;

import android.content.Context;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class OPIPKT_EDF_Reader {
	
	File f=null;
	BufferedReader reader=null;
	InputStreamReader read=null;
	public RandomAccessFile raf=null;
	//read file
	public int hdrBytes;
	public int numDataRecsp;
	public int dataRecDurp;
	public int numSignalsp;
	public Vector<String> labelSignalsQVp=new Vector<String>();
	public Vector<String> physDimQVp=new Vector<String>();
	public Vector<Integer> physMinQVp=new Vector<Integer>();
	public Vector<Integer> physMaxQVp=new Vector<Integer>();
	public Vector<Integer> digMinQVp=new Vector<Integer>();
	public Vector<Integer> digMaxQVp=new Vector<Integer>();
	public Vector<Integer> sampsPerDRQVp=new Vector<Integer>();
	private TextView tvRL;
    private TextView tvLevel;
	private TextView filetitle;
	private String filepath;
	public long wroteDRCt=0;
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
	public boolean drawed=false;
	public boolean getgesture=false;
	public boolean zoomined=false;
	
	public synchronized void initial(TextView tvRLp,TextView tvLevelp,TextView filetitlep)
	{
		tvRL=tvRLp;
		tvLevel=tvLevelp;
		filetitle=filetitlep;
	}
	public void setLvelText(String temp)
	{
		tvLevel.setText(temp);
	}
	public void setRLText(String temp)
	{
		tvRL.setText(temp);
	}
	public synchronized void reset()
	{
		drawed=false;
		getgesture=false;
		zoomined=false;
		tvRL.setText("Start Time:");
		tvLevel.setText("Recorded Time:");
		filetitle.setText("File:");
		filepath="";
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
	}
	
	
	public  int[] readRawData(int bytes,int samples,int sizes,RandomAccessFile raf)
	{
		int []temp=new int[sizes];
		byte []buff=new byte[bytes];
		for(int i=0;i<samples;i++)
		{
			try {
				if(raf.read(buff,0,bytes)!=bytes)
					temp[i]=-1;
				else
					temp[i]= OPIPKT_helper.edfDBytestToInt(buff);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return temp;
	}
	
	
	/***
	  * Read in an edf file that is generic opi data. Assumes data is in right format.
	  * Inputs:
	  *     all data will be appended to end of qvectors
	  * Returns:
	  *     non-negative number indicating number of data records read
	  *     -1, error
	  *     no get ts and annotation
	  */
	public  static long edfDread(OPIPKT_EDF_Reader OPIPKT_reader,RandomAccessFile raf,OPIPKT_Raw_File OPIPKT_rfio)
	{
		OPIPKT_reader.wroteDRCt=0;
		OPIPKT_reader.tagTextQV.clear();
		OPIPKT_reader.tagTS.clear();
		OPIPKT_reader.tsQV.clear();
		OPIPKT_reader.skpQV.clear();
		OPIPKT_reader.batQV.clear();
		OPIPKT_reader.adcQV.clear();
		OPIPKT_reader.tmpQV.clear();
		OPIPKT_reader.axQV.clear();
		OPIPKT_reader.ayQV.clear();
		OPIPKT_reader.azQV.clear();
		OPIPKT_reader.sqQV.clear();
		OPIPKT_reader.edQV.clear();
		int []tempadcs= new int[OPIPKT_android.ADCLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*OPIPKT_EDF_Writer.EDFDRDURSEC];
		int []tempaxs= new int[OPIPKT_android.ACCLEN/4*OPIPKT_Accel_Viewer.FRMSPERSEC*OPIPKT_EDF_Writer.EDFDRDURSEC];
		int []tempays= new int[OPIPKT_android.ACCLEN/4*OPIPKT_Accel_Viewer.FRMSPERSEC*OPIPKT_EDF_Writer.EDFDRDURSEC];
		int []tempazs= new int[OPIPKT_android.ACCLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*OPIPKT_EDF_Writer.EDFDRDURSEC];
		int []temptmps= new int[OPIPKT_android.TMPLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*OPIPKT_EDF_Writer.EDFDRDURSEC];
		int []tempsqs= new int[1*OPIPKT_Accel_Viewer.FRMSPERSEC*OPIPKT_EDF_Writer.EDFDRDURSEC];
		int []tempanns = new int[128];  //quint8
		int i, j, dataRecCt;
		int prevTS, tempTS;
	    Vector<String> annsQL=new Vector<String>();
	    Vector<String> annQL=new Vector<String>();
	    int newstep, azold, axold, ayold, az4old, ax2old, ay2old, ax3old, ay3old, ax4old, ay4old, az8old, az12old, az16old;
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


	    // Initialization
	    //prevTS = (startDT.toMSecsSinceEpoch()-QDateTime::fromString("20120928080000000","yyyyMMddhhmmsszzz").toMSecsSinceEpoch())*OPIPKT_android.UCERTCFREQ/1000;
	    //prevTS -= OPIPKT_android.ADCLEN*OPIPKT_android.UCERTCFREQ/OPIPKT_android.TSERTCFREQ;   // must make it previous TS
	    //if(prevTS < 0) prevTS = 0;
	    dataRecCt = 0;
	    
	    // Read in data until end
	    while(OPIPKT_reader.wroteDRCt<OPIPKT_reader.numDataRecsp) // read until no more data
	    {
	        // adc data 8192 = sampsPerDRQV.at(0)*2bytes
	    	tempadcs=OPIPKT_reader.readRawData(2,OPIPKT_reader.sampsPerDRQVp.elementAt(0),OPIPKT_android.ADCLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*OPIPKT_EDF_Writer.EDFDRDURSEC,raf);  //if(instrp->readRawData((char *)tempadcs, sampsPerDRQV.at(0)*2) < 0) break;  // not enough data
	    	tempaxs=OPIPKT_reader.readRawData(2,OPIPKT_reader.sampsPerDRQVp.elementAt(1),OPIPKT_android.ACCLEN/4*OPIPKT_Accel_Viewer.FRMSPERSEC*OPIPKT_EDF_Writer.EDFDRDURSEC,raf);//if(instrp->readRawData((char *)tempaxs, sampsPerDRQV.at(1)*2) < 0) break;
	    	tempays=OPIPKT_reader.readRawData(2,OPIPKT_reader.sampsPerDRQVp.elementAt(2),OPIPKT_android.ACCLEN/4*OPIPKT_Accel_Viewer.FRMSPERSEC*OPIPKT_EDF_Writer.EDFDRDURSEC,raf);//if(instrp->readRawData((char *)tempays, sampsPerDRQV.at(2)*2) < 0) break;
	    	tempazs=OPIPKT_reader.readRawData(2,OPIPKT_reader.sampsPerDRQVp.elementAt(3),OPIPKT_android.ACCLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*OPIPKT_EDF_Writer.EDFDRDURSEC,raf);//if(instrp->readRawData((char *)tempazs, sampsPerDRQV.at(3)*2) < 0) break;
	    	temptmps=OPIPKT_reader.readRawData(2,OPIPKT_reader.sampsPerDRQVp.elementAt(4),OPIPKT_android.TMPLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*OPIPKT_EDF_Writer.EDFDRDURSEC,raf);//if(instrp->readRawData((char *)temptmps, sampsPerDRQV.at(4)*2) < 0) break;
	    	tempsqs=OPIPKT_reader.readRawData(2,OPIPKT_reader.sampsPerDRQVp.elementAt(5),1*OPIPKT_Accel_Viewer.FRMSPERSEC*OPIPKT_EDF_Writer.EDFDRDURSEC,raf);//if(instrp->readRawData((char *)tempsqs, sampsPerDRQV.at(5)*2) < 0) break;
	    	tempanns=OPIPKT_reader.readRawData(2,OPIPKT_reader.sampsPerDRQVp.elementAt(6),128,raf);//if(instrp->readRawData((char *)tempanns, sampsPerDRQV.at(6)*2) < 0) break;

	        // put into qvectors because data record is complete
	        for(i = 0; i < 1*OPIPKT_Accel_Viewer.FRMSPERSEC*OPIPKT_EDF_Writer.EDFDRDURSEC; i++)
	        {
	            //tsQVp->append(prevTS+OPIPKT_android.ADCLEN*OPIPKT_android.UCERTCFREQ/OPIPKT_android.TSERTCFREQ);
	            //prevTS += OPIPKT_android.ADCLEN*OPIPKT_android.UCERTCFREQ/OPIPKT_android.TSERTCFREQ;
	            OPIPKT_reader.skpQV.addElement(new Integer(0));
	            OPIPKT_reader.batQV.addElement(new Integer(1));
	            for(j = 0; j < OPIPKT_android.ADCLEN; j++)
	            	OPIPKT_reader.adcQV.addElement(new Integer(tempadcs[i*OPIPKT_android.ADCLEN+j]));   
	            	
	            for(j = 0; j < OPIPKT_android.ACCLEN/4; j++)
	            {
	            	OPIPKT_reader.axQV.addElement(new Integer(tempaxs[i*OPIPKT_android.ACCLEN/4+j]));
	            	OPIPKT_reader.ayQV.addElement(new Integer(tempays[i*OPIPKT_android.ACCLEN/4+j]));
	            }
	            for(j = 0; j < OPIPKT_android.ACCLEN; j++)
	            	OPIPKT_reader.azQV.addElement(new Integer(tempazs[i*OPIPKT_android.ACCLEN+j]));
	            for(j = 0; j < OPIPKT_android.TMPLEN; j++)
	            	OPIPKT_reader.tmpQV.addElement(new Integer(temptmps[i*OPIPKT_android.TMPLEN+j]));
	            //OPIPKT_reader.sqQV.addElement(new Integer(calcAct(axQVp, ayQVp, azQVp, &newstep, &azold, &axold, &ayold, &az4old, &ax2old, &ay2old, &ax3old, &ay3old, &ax4old, &ay4old, &az8old, &az12old, &az16old)));
	            OPIPKT_reader.edQV.addElement(new Integer(0));
	        }
	        // take care of annotations
	        //annsQL = QString::fromAscii((const char *)tempanns,128).split(QChar(0),QString::SkipEmptyParts);
	        //qDebug()<<QChar(0)<<" "<<QChar(20);
	        /*
	        for(i = 0; i < annsQL.size(); i++)
	        {
	            annQL = annsQL.at(i).split(QChar(20),QString::SkipEmptyParts); // split each entry
	            if(annQL.size() < 2) continue; // no tag entries
	            // first parts is always the time
	            tempTS = (qint64) (annQL.at(0).toFloat()*UCERTCFREQ+(startDT.toMSecsSinceEpoch()-QDateTime::fromString("20120928080000000","yyyyMMddhhmmsszzz").toMSecsSinceEpoch())*UCERTCFREQ/1000);
	            for(j = 1; j < annQL.size(); j++)
	            {
	                annOnsetTSQVp->append(tempTS);
	                annTextQVp->append(annQL.at(j));
	            }
	        }
	        */
	        OPIPKT_reader.wroteDRCt++;
	    }
	    try {
			raf.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return OPIPKT_reader.wroteDRCt;
	}
	public  void setfullfilename(String name)
	{
		filetitle.setText("File:"+name);
		filepath=name;
	}
	
	/***
	  * Read EDF header information into pointed to qvectors.
	  * Returns:
	  *     <0, if error
	  *     0, if successful
	  *     in this version we don't read id
	  */
	public static int edfhdrread(OPIPKT_EDF_Reader OPIPKT_reader,RandomAccessFile raf)
	{
		int i, hdrBytes;
	    byte []buff8=new byte[8];
	    byte []buff80=new byte[80];
	    byte []buff16=new byte[16];
	    byte []buff32=new byte[32];
	    byte []buff44=new byte[44];
	    byte []buff4=new byte[4];
	    int tempint;
	    String str;
	    //8 ascii : version of this data format (0)
	    try {
			if(raf.read(buff8,0,8)!=8) return -1;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   
	    try {
			str = new String(buff8, "UTF-8");
			if(str.equalsIgnoreCase("0       ")!=true) return -1;
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	    
	    
	    //80 ascii : local patient identification
	    try {
			if(raf.read(buff80,0,80)!=80) return -2;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   
	    //*lpidp = QString::fromAscii(buff,80);

	    //80 ascii : local recording identification.
	    try {
			if(raf.read(buff80,0,80)!=80) return -3;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}    
	    //*lridp = QString::fromAscii(buff,80);

	    
	    //8 ascii : startdate of recording (dd.mm.yy),
	    //+8 ascii : starttime of recording (hh.mm.ss).
	    try {
			if(raf.read(buff16,0,16)!=16) return -4;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}      //if(instrp->readRawData(buff,16) < 0) return -1;
	    //*startDTp = QDateTime::fromString(QString::fromAscii(buff,16),"dd.MM.yyHH.mm.ss");
	    //if(*startDTp < QDateTime::fromString("19850101","yyyyMMdd")) // if yy=13 should be 2013
	    //*startDTp = startDTp->addYears(100);
	       
	      
	    //8 ascii : number of bytes in header record
	    try {
			if(raf.read(buff8,0,8)!=8) return -5;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   //if(instrp->readRawData(buff,8) < 0) return -1;
	    try {
			str = new String(buff8, "UTF-8");
			if(str.equalsIgnoreCase("2048    ")!=true) return -6;
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		OPIPKT_reader.hdrBytes =2048;  //hdrBytes = QString::fromAscii(buff,8).toInt();
	    
	   
	    //44 ascii : reserved
		try {
			if(raf.read(buff44,0,44)!=44) return -7;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			str = new String(buff44, "UTF-8");
			if(str.trim().equalsIgnoreCase("EDF+C")!=true) return -8;
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	    
	    //8 ascii : number of data records (-1 if unknown)
		try {
			if(raf.read(buff8,0,8)!=8) return -9;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		OPIPKT_reader.numDataRecsp = OPIPKT_helper.edfHeaderBytesToInt(buff8); //*numDataRecsp = QString::fromAscii(buff,8).toInt();

	   
	    //8 ascii : duration of a data record, in seconds
		try {
			if(raf.read(buff8,0,8)!=8) return -10;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		OPIPKT_reader.dataRecDurp=OPIPKT_helper.edfHeaderBytesToInt(buff8);	//*dataRecDurp = QString::fromAscii(buff,8).toInt();

	    
	    //4 ascii : number of signals (ns) in data record
		try {
			if(raf.read(buff4,0,4)!=4) return -11;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		OPIPKT_reader.numSignalsp=OPIPKT_helper.edfHeaderBytesToInt(buff4);	//*numSignalsp = QString::fromAscii(buff,4).toInt();
	    
	    
	    //ns * 16 ascii : ns * label
		OPIPKT_reader.labelSignalsQVp.clear();
	    for(i = 0; i < OPIPKT_reader.numSignalsp ; i++)
	    {
	    	try {
				if(raf.read(buff16,0,16)!=16) return -12;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	try {
				OPIPKT_reader.labelSignalsQVp.addElement(new String(buff16, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	    
	    
	    //ns * 80 ascii : ns * transducer type (e.g. AgAgCl electrode)
	    for(i = 0; i < OPIPKT_reader.numSignalsp; i++)
	    {
	    	try {
				if(raf.read(buff80,0,80)!=80) return -13;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        //transTypeQVp->append(QString::fromAscii(buff,80));
	    }

	    
	 
	    //ns * 8 ascii : ns * physical dimension (e.g. uV)
	    OPIPKT_reader.physDimQVp.clear();
	    for(i = 0; i < OPIPKT_reader.numSignalsp; i++)
	    {
	    	try {
				if(raf.read(buff8,0,8)!=8) return -14;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	try {
				OPIPKT_reader.physDimQVp.addElement(new String(buff8, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
	    }
	    
	    
	    
	    //ns * 8 ascii : ns * physical minimum (e.g. -500 or 34)
	    OPIPKT_reader.physMinQVp.clear();
	    for(i = 0; i < OPIPKT_reader.numSignalsp; i++)
	    {
	    	try {
				if(raf.read(buff8,0,8)!=8) return -15;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	OPIPKT_reader.physMinQVp.addElement(new Integer(OPIPKT_helper.edfHeaderBytesToInt(buff8)));//physMinQVp->append(QString::fromAscii(buff,8).toInt());
	    }

	    //ns * 8 ascii : ns * physical maximum (e.g. 500 or 40)
	    OPIPKT_reader.physMaxQVp.clear();
	    for(i = 0; i < OPIPKT_reader.numSignalsp; i++)
	    {
	    	try {
				if(raf.read(buff8,0,8)!=8) return -16;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	OPIPKT_reader.physMaxQVp.addElement(new Integer(OPIPKT_helper.edfHeaderBytesToInt(buff8)));//physMaxQVp->append(QString::fromAscii(buff,8).toInt());
	    }

	
	    //ns * 8 ascii : ns * digital minimum (e.g. -2048)
	    OPIPKT_reader.digMinQVp.clear();
	    for(i = 0; i < OPIPKT_reader.numSignalsp; i++)
	    {
	    	try {
				if(raf.read(buff8,0,8)!=8) return -17;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	OPIPKT_reader.digMinQVp.addElement(new Integer(OPIPKT_helper.edfHeaderBytesToInt(buff8)));// digMinQVp->append(QString::fromAscii(buff,8).toInt());
	    }

	 
	    //ns * 8 ascii : ns * digital maximum (e.g. 2047)
	    OPIPKT_reader.digMaxQVp.clear();
	    for(i = 0; i < OPIPKT_reader.numSignalsp; i++)
	    {
	    	try {
				if(raf.read(buff8,0,8)!=8) return -18;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	OPIPKT_reader.digMaxQVp.addElement(new Integer(OPIPKT_helper.edfHeaderBytesToInt(buff8)));//digMaxQVp->append(QString::fromAscii(buff,8).toInt());
	    }

	    
	    //ns * 80 ascii : ns * prefiltering (e.g. HP:0.1Hz LP:75Hz)
	    for(i = 0; i < OPIPKT_reader.numSignalsp; i++)
	    {
	    	try {
				if(raf.read(buff80,0,80)!=80) return -19;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        //prefiltQVp->append(QString::fromAscii(buff,8));
	    }

	    
	    //ns * 8 ascii : ns * nr of samples in each data record
	    OPIPKT_reader.sampsPerDRQVp.clear();
	    for(i = 0; i < OPIPKT_reader.numSignalsp; i++)
	    {
	    	try {
				if(raf.read(buff8,0,8)!=8) return -20;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	tempint=OPIPKT_helper.edfHeaderBytesToInt(buff8);
	    	OPIPKT_reader.sampsPerDRQVp.addElement(new Integer(tempint));//sampsPerDRQVp->append(QString::fromAscii(buff,8).toInt());
	    }

	    if(OPIPKT_reader.checksample(OPIPKT_reader.sampsPerDRQVp)==false)
	    	return -21;
	    
	    
	    //ns * 32 ascii : ns * reserved
	    for(i = 0; i < OPIPKT_reader.numSignalsp; i++)
	    {
	    	try {
				if(raf.read(buff32,0,32)!=32) return -22;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	    return 0;   // if got here, then read all of edf header
	}
	
	
	public  boolean checksample(Vector <Integer> sampsPerDRQV)
	{
		// check to make sure things are right
	    if((sampsPerDRQV.elementAt(0) != OPIPKT_android.ADCLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*OPIPKT_EDF_Writer.EDFDRDURSEC) ||
	            (sampsPerDRQV.elementAt(1) != OPIPKT_android.ACCLEN/4*OPIPKT_Accel_Viewer.FRMSPERSEC*OPIPKT_EDF_Writer.EDFDRDURSEC) ||
	            (sampsPerDRQV.elementAt(2) != OPIPKT_android.ACCLEN/4*OPIPKT_Accel_Viewer.FRMSPERSEC*OPIPKT_EDF_Writer.EDFDRDURSEC) ||
	            (sampsPerDRQV.elementAt(3) != OPIPKT_android.ACCLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*OPIPKT_EDF_Writer.EDFDRDURSEC) ||
	            (sampsPerDRQV.elementAt(4) != OPIPKT_android.TMPLEN*OPIPKT_Accel_Viewer.FRMSPERSEC*OPIPKT_EDF_Writer.EDFDRDURSEC) ||
	            (sampsPerDRQV.elementAt(5) != OPIPKT_android.ACCLEN/4*OPIPKT_Accel_Viewer.FRMSPERSEC*OPIPKT_EDF_Writer.EDFDRDURSEC) ||
	            ((sampsPerDRQV.elementAt(6) != 64) && (sampsPerDRQV.elementAt(6) != 30)))
	        return false;
	    else 
	    	return true;
	}
	public  boolean checkedfheader(ViewFlipper appVF,String fullname)
	{
		int temp=-1;
		f = new File(fullname);
		if(!f.exists())
		{
			Toast.makeText(appVF.getContext(),"file doesn't exist", Toast.LENGTH_LONG).show();	
			return false;
		}
		else
		{
			try {
				try {
					read = new InputStreamReader(
							new FileInputStream(f),"UTF-8");
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Toast.makeText(appVF.getContext(),"create streamreader failed", Toast.LENGTH_SHORT).show();
			}
			reader = new BufferedReader(read);
			try {
				reader.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				raf = new RandomAccessFile(fullname, "r");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				raf.seek(0);
				temp = OPIPKT_EDF_Reader.edfhdrread(this, raf);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			}
			
			if(temp==0)
			{
				return true;
			}
			else
			{
				Toast.makeText(appVF.getContext(),"file is invalid", Toast.LENGTH_LONG).show();	
				return false;
			}
		
		}
		
	}
