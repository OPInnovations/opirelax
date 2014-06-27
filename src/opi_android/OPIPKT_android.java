package opi_android;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

public class OPIPKT_android {
	
	public OPIPKT_android(){
		super();
		androidusb_reset();
	}
	
	public void androidusb_reset()
	{
		con =null;
		epIN =null;
		epOUT =null;
		mDevice=null;
		usbIf=null;
		manager=null;
		comPort = 0;  
	}
	/***
	  * Definitions
	  */

	public final static int ZBWLCHANCT=16;
	public final static byte SAMPQUALMASK=0x03;
	public final static int TSEFRMLEN=145;
	public final static int UCERTCFREQ = 4096;
	public final static int DSNLEN=5;	// Device Serial Number length in bytes
	public final static int TSLEN=6	;// Device Serial Number length in bytes
	public final static int FWVLEN=2 ;	// Firmware version length in bytes
	public final static int PDNLISTLEN=8;	// Number of PDNs that can be paired
	public final static int PDNINFOLEN=12;	// Length of info bytes for each PDN stored in UC
	public final static int WLFRMHDRLEN=2	;// Length of the wireless frame header in bytes
	public final static int WFRMHDRLEN=2 ;  // Length of the wired frame header in bytes
	public final static int  TSFRMLEN=145 ;// Usual length of the wired frame with truesense data in bytes
	public final static int EVFRMLEN=9  ; // Length of an wired frame with single event in bytes
	public final static int OPIUCDSTLEN=128 ;// Number of bytes in a UC status packet including dataCode
	public final static int ADCLEN=64;	// Usual number of ADC samples
	public final static int TMPLEN=1 ;  // Length of temperature data in bytes
	public final static int ACCDLEN=6 ;  // Length of accelerometer data in bytes (1 x, 1 y, 4 z)
	public final static int ACCLEN = 4; // Length of z-axis accelerometer data in bytes
	public final static int OPIHDRLEN=512 ;// Number of bytes at the beginning of OPI file for header
	public final static byte SYNCBYTE=0x33	;// Byte value for sync bytes
	public final static int UCRTCFREQ=4096 ;   // Frequency of real-time clock on Unicon
	public final static int TSRTCFREQ=12   ;  // Frequency of real-time clock on TrueSense	
	public final static int OPITIMEOUT = 25; //ms
	public final static int CHECKTIMES = 20;
	public final static int OPIUCPID=9220;
	public final static int OPIUCVID=1003;
	public final static int TSERTCFREQ = 256 ;    // Frequency of real-time clock on TrueSense
	public final static int UCEFFTLEN = 32 ; // FFT size done on UCE divide by 2 since symmetrical
	private static final int USB_RECIP_INTERFACE = 0x01;	// copied from usb serial for android
	private static final int USB_RT_ACM = UsbConstants.USB_TYPE_CLASS | USB_RECIP_INTERFACE;
	private static final int SET_LINE_CODING = 0x20; // USB CDC 1.1 section 6.2
	private static final int GET_LINE_CODING = 0x21;
	private static final int SET_CONTROL_LINE_STATE = 0x22;
	private static final int SEND_BREAK = 0x23;
	
	UsbDeviceConnection con ;
	UsbEndpoint epIN ;
	UsbEndpoint epOUT ;
	UsbDevice mDevice;
	UsbInterface usbIf;
    UsbManager manager;
    public int comPort;  //0 for null 1 for open usb successfully



    /***
     *	Sets the plugged in truesense RF Tx Timeout.
     *	Inputs:
     *		comportptr, pointer to comport assigned to UC
     *		rftxtimeout, value to write, 0 is never, 1 is 30min, 2 is 1hr
     *	Returns:
     *		0 if successful
     *		-1 if error
     */
   public synchronized int opiuce_settserftxtimeout(int rftxtimeout)
   {
	   OPIPKT_struct opipkt = new OPIPKT_struct();
	   opipkt.dataCode = (byte)0x20;
       opipkt.payload[0] = (byte)0x10;
       opipkt.payload[1] = (byte) (rftxtimeout & 0xFF);
       opipkt.length = 2;
       if (opiuce_put_com(opipkt) != 0) return -1;
       if (opiuce_get_com(opipkt) != 0) return -1;
       if ((opipkt.dataCode&0xff) == 0x40) 		return 0;
       else return -1;
   }
   
    /***
     *	Sets the plugged in truesense real time counter to the same
     *	as the unified controller (unicon or UC).
     *	Inputs:
     *		comportptr, pointer to comport assigned to UC
     *	Returns:
     *		0 if successful
     *		-1 if error
     */
   public synchronized int opiuce_settsertc()
   {
	   OPIPKT_struct opipkt = new OPIPKT_struct();

       opipkt.dataCode = (byte)0x20;
       opipkt.payload[0] = (byte)0x11;
       opipkt.length = 1;
       if (opiuce_put_com(opipkt) != 0) return -1;
       if (opiuce_get_com(opipkt) != 0) return -1;
       if ((opipkt.dataCode&0xff) == 0x40) 		return 0;
       else return -1;
   }
   
    /***
     *	Sets the timer in the unicon
     *	Inputs:
     *		comportptr, pointer to comport assigned to UC
     *		timeStamp, array of 6 bytes with MSB first representing
     *			internal 48bit timer
     *	Returns:
     *		0 if successful
     *		-1 if error
     */
   public synchronized int opiuce_setpktts(int [] timeStamp)
   {
       int i;
       OPIPKT_struct opipkt = new OPIPKT_struct();

       opipkt.dataCode =(byte) 0x20;
       opipkt.payload[0] = (byte)0x0F;
       for (i = 0; i < TSLEN; i++)
           opipkt.payload[1+i] = (byte) (timeStamp[i]&0xFF);
       opipkt.length = 1+TSLEN;
       if (opiuce_put_com(opipkt) != 0) return -1;
       if (opiuce_get_com(opipkt) != 0) return -1;
       if ((opipkt.dataCode&0xff) == 0x40) 		return 0;
       else return -1;
   }
   
   
    /***
     *	Read out captured events/tags. Format specified in wired frame definition.
     *	Inputs:
     *		comportptr, pointer to comport assigned to UC
     *      	pktptr, pointer to packet that will be modified to have the event/tag data
     *	Returns:
     *		0 if successful
     *		-1 if error
     */
   public synchronized int opiuce_evcapread( OPIPKT_struct pktptr)
   {
       pktptr.dataCode = (byte)0x10;
       pktptr.payload[0] = (byte)0x20;
       pktptr.length = 1;
       if (opiuce_put_com(pktptr) != 0) return -1;
       if (opiuce_get_com(pktptr) != 0) return -1;
       if (((pktptr.dataCode&0xff) == 0x10) && ((pktptr.payload[0]&0xff) == 0x21)) return 0;
       else return -1;
   }
   
    /***
     *	Start chip erase of memory module, does not check if done.
     *	Use a separate function to check if chip erase is done.
     *	Inputs:
     *		comportptr, pointer to comport assigned to UC
     *	Returns:
     *		0 if successful
     *		-1 if error
     */
   public synchronized int opiuce_mmerasest()
    {
	 OPIPKT_struct opipkt = new OPIPKT_struct();

    opipkt.dataCode = (byte)0x2A;
    opipkt.payload[0] = (byte)0x08;
    opipkt.length = 1;
    if (opiuce_put_com(opipkt) != 0) return -1;
    else return 0;
    }
    
    /***
     *	Check if end of erase of memory module, does not start
     *	Inputs:
     *		comportptr, pointer to comport assigned to UC
     *	Returns:
     *		0 if end of memory erase
     *		1 if no reply
     *      	-1 if error,
     */
   public synchronized int opiuce_mmeraseend()
   {
       OPIPKT_struct opipkt = new OPIPKT_struct();
       int res;

       res = opiuce_get_com(opipkt);
       if (res == -1) return 1;
       else if(res != 0) return -1;
       if ((opipkt.dataCode&0xff) == 0x40) return 0;
       else return -1;
   }
   
    /***
     *	Get 5 packets of data as specified by packet number from memory module.
     *	The packet number specifies the first packet of the 5 packets. Function
     *	checks the packet number and makes sure it is between 0 and 327679,
     *	inclusive, since these are the valid numbers.
     *	Starting packet number will always be rounded down to the nearest
     *	multiple of 5.
     *	Format of this frame is specified in the wired frame definition.
     *	Inputs:
     *		comportptr, pointer to comport assigned to UC
     *      	pktNumber, an integer specifying the starting packet number
     *      		in memory (valid values are 0-2^18)
     *      	pktptr, pointer to packet that will be updated with data
     *	Returns:
     *		0 if data read out
     *		-1 if error
     */
  public  synchronized int opiuce_get5mmtsdata( int pktNumber, OPIPKT_struct pktptr)
   {
       if ((pktNumber < 0) || (pktNumber > 327679)) return -1;
       pktNumber = (pktNumber/5)*5;    // rounding down to nearest multiple of 5
       pktptr.dataCode = (byte)0x2A;
       pktptr.payload[0] = (byte)0x00;
       pktptr.payload[1] = (byte) ((pktNumber >> 16) & 0xFF);
       pktptr.payload[2] = (byte) ((pktNumber >> 8) & 0xFF);
       pktptr.payload[3] = (byte) (pktNumber & 0xFF);
       pktptr.length = 4;
       if (opiuce_put_com(pktptr) != 0) return -1;
       if (opiuce_get_com(pktptr) != 0) return -1;
       if (((pktptr.dataCode&0xff) == 0x2A) && ((pktptr.payload[0]&0xff) == 0x02))
           return 0;
       else return -1;
   }
   
    /***
     *	Turn module off through unified controller
     *	Inputs:
     *		comportptr, pointer to comport assigned to UC
     *	Returns:
     *		0 if successful
     *		-1 if error
     */
    public synchronized  int opiuce_turnmodoff()
    {
	 OPIPKT_struct opipkt = new OPIPKT_struct();

    opipkt.dataCode = (byte)0x20;
    opipkt.payload[0] = (byte)0x20;
    opipkt.length = 1;
    if (opiuce_put_com(opipkt) != 0) return -1;
    if (opiuce_get_com(opipkt) != 0) return -1;
    if ((opipkt.dataCode&0xff) == 0x40) 		return 0;
    else return -1;
    }


    /***
     *	Turn module on through unified controller
     *	Inputs:
     *		comportptr, pointer to comport assigned to UC
     *	Returns:
     *		0 if successful
     *		-1 if error
     */
   public synchronized int opiuce_turnmodon()
   {
	   OPIPKT_struct opipkt = new OPIPKT_struct();

       opipkt.dataCode = (byte)0x20;
       opipkt.payload[0] = (byte)0x21;
       opipkt.length = 1;
       if (opiuce_put_com(opipkt) != 0) return -1;
       if (opiuce_get_com(opipkt) != 0) return -1;
       if ((opipkt.dataCode&0xff) == 0x40) 		return 0;
       else return -1;
   }

    /***
     *	Shutdown unified controller, will disconnect USB
     *	Inputs:
     *		comportptr, pointer to comport assigned to UC
     *	Returns:
     *		0 if successful
     *		-1 if error
     */
    public synchronized int opiuce_shutdown()
    {
    	OPIPKT_struct opipkt = new OPIPKT_struct();

    	opipkt.dataCode = (byte)0x13;
    	opipkt.length = 0;
    	if (opiuce_put_com(opipkt) != 0) return -1;
    	else return 0;
    }


    /***
     *	Reset the variables related to relax state in controller.
     * Will set the start timestamp to current timestamp and Set
     * to zero the accumulated score and number of packets used.
     *	Inputs:
     *		comportptr, pointer to comport assigned to UC
     *	Returns:
     *		0 if ok
     *		-1 if error
     */
   public synchronized int opiuce_resetrelaxdata()
   {
	   OPIPKT_struct opipkt = new OPIPKT_struct();

       opipkt.dataCode = (byte)0x14;
       opipkt.payload[0] = (byte)0x02;
       opipkt.length = 1;
       if(opiuce_put_com(opipkt) != 0) return -1;
       if(opiuce_get_com(opipkt) != 0) return -1;
       if((opipkt.dataCode&0xff) == 0x40)   return 0;
       else return -1;
   }
   
    /***
     *	Get calculated the last 64point 16bit FFT data. The FFT is based on
     * the last packet data retrieved with opiuce_getwltsedata. Since
     * FFTs of real data give symmetrical results, only 32 points are
     * given.
     *	Format of this frame is specified in the wired frame definition.
     *	Inputs:
     *		comportptr, pointer to comport assigned to UC
     *     pktptr, pointer to packet that will be updated with data
     *	Returns:
     *		0 if data read out
     *		-1 if error
     */
   public synchronized int opiuce_getwltsefft( OPIPKT_struct pktptr)
   {
       pktptr.dataCode = (byte)0x14;
       pktptr.payload[0] =(byte) 0x22;
       pktptr.length = 1;
       if(opiuce_put_com(pktptr) != 0) return -1;
       if(opiuce_get_com(pktptr) != 0) return -1;
       if(((pktptr.dataCode&0xff) == 0x14) && ((pktptr.payload[0]&0xff) == 0x23))
           return 0;
       else return -1;
   }
   
   
    /***
     *	Get the parameters used in calculating the Relax state.
     *	Format of this frame is specified in the wired frame definition.
     *	Inputs:
     *		comportptr, pointer to comport assigned to UC
     *     pktptr, pointer to packet that will be updated with data
     *	Returns:
     *		0 if data read out
     *		-1 if error
     */
   public synchronized int opiuce_setrelaxparams(int thx, int thm, int offl, int th3gm, int th3bm, int th2gm, int th2bm, int th1gm, int th1bm, int offm)
   {
	   OPIPKT_struct opipkt = new OPIPKT_struct();

       opipkt.dataCode = (byte)0x14;
       opipkt.payload[0] = (byte)0x06;
       opipkt.length = 21;
       opipkt.payload[1] = (byte) ((thx >>8 )&0xff);
       opipkt.payload[2] = (byte) ((thx >>0 )&0xff);
       opipkt.payload[3] = (byte) ((thm >>8 )&0xff);
       opipkt.payload[4] = (byte) ((thm >>0 )&0xff);
       opipkt.payload[5] = (byte) ((offl >>8 )&0xff);
       opipkt.payload[6] = (byte) ((offl >>0 )&0xff);
       opipkt.payload[7] = (byte) ((th3gm >>8 )&0xff);
       opipkt.payload[8] = (byte) ((th3gm >>0 )&0xff);
       opipkt.payload[9] = (byte) ((th3bm >>8 )&0xff);
       opipkt.payload[10] = (byte) ((th3bm >>0 )&0xff);
       opipkt.payload[11] = (byte) ((th2gm >>8 )&0xff);
       opipkt.payload[12] = (byte) ((th2gm >>0 )&0xff);
       opipkt.payload[13] = (byte)  (( th2bm >>8 )&0xff);
       opipkt.payload[14] = (byte)  (( th2bm >>0 )&0xff);
       opipkt.payload[15] = (byte) (( th1gm >>8 )&0xff);
       opipkt.payload[16] = (byte) (( th1gm >>0 )&0xff);
       opipkt.payload[17] = (byte)  (( th1bm >>8 )&0xff);
       opipkt.payload[18] = (byte)  (( th1bm >>0 )&0xff);
       opipkt.payload[19] = (byte)  (( offm >>8 )&0xff);
       opipkt.payload[20] = (byte)  (( offm >>0 )&0xff);

       if(opiuce_put_com(opipkt) != 0) return -1;
       if(opiuce_get_com(opipkt) != 0) return -1;
       if((opipkt.dataCode&0xff) == 0x40)   return 0;
       else return -1;
   }
   
    /***
     *	Set controller to off-state where it constantly turns off
     * plugged in truesense.
     *	Inputs:
     *		comportptr, pointer to comport assigned to UC
     *	Returns:
     *		0 if ok
     *		-1 if error
     */
   public synchronized int opiuce_offmode()
   {
	   OPIPKT_struct opipkt = new OPIPKT_struct();

       opipkt.dataCode = (byte)0x20;
       opipkt.payload[0] =(byte) 0x22;
       opipkt.length = 1;
       if(opiuce_put_com(opipkt) != 0) return -1;
       if(opiuce_get_com(opipkt) != 0) return -1;
       if((opipkt.dataCode&0xff) == 0x40)   return 0;
       else return -1;
   }

   
   
    /***
     *	Get the relax state data: start time, accumulated score, and packets used.
     *	Format of this frame is specified in the wired frame definition.
     *	Inputs:
     *		comportptr, pointer to comport assigned to UC
     *     pktptr, pointer to packet that will be updated with data
     *	Returns:
     *		0 if data read out
     *		-1 if error
     */
   public synchronized int opiuce_getrelaxdata(OPIPKT_struct pktptr)
   {
       pktptr.dataCode = (byte)0x14;
       pktptr.payload[0] = (byte)0x00;
       pktptr.length = 1;
       if(opiuce_put_com(pktptr) != 0) return -1;
       if(opiuce_get_com(pktptr) != 0) return -1;
       if(((pktptr.dataCode&0xff) == 0x14) && ((pktptr.payload[0]&0xff) == 0x01))
           return 0;
       else return -1;
   }
   
    
    /***
     *	Get the parameters used in calculating the Relax state.
     *	Format of this frame is specified in the wired frame definition.
     *	Inputs:
     *		comportptr, pointer to comport assigned to UC
     *     pktptr, pointer to packet that will be updated with data
     *	Returns:
     *		0 if data read out
     *		-1 if error
     */
   public synchronized int opiuce_getrelaxparams(OPIPKT_struct pktptr)
   {
       pktptr.dataCode = (byte)0x14;
       pktptr.payload[0] = (byte)0x04;
       pktptr.length = 1;
       if(opiuce_put_com(pktptr) != 0) return -1;
       if(opiuce_get_com(pktptr) != 0) return -2;
       if(((pktptr.dataCode&0xff) == 0x14) && ((pktptr.payload[0]&0xff) == 0x05))
           return 0;
       else return -3;
   }

   
   
    /***
     *	Copies the settings of plugged in module to specified slot
     *	(0 thru 7 valid)
     *	Inputs:
     *		comportptr, pointer to comport assigned to UC
     *		pdnslot, int value of pdn slot
     *	Returns:
     *		0 if successful
     *		-1 if error
     */
   public synchronized int opiuce_copytsesettings( int pdnslot)
   {
	   OPIPKT_struct opipkt = new OPIPKT_struct();

       opipkt.dataCode =(byte) 0x20;
       opipkt.payload[0] = (byte)0x09;
       opipkt.payload[1] = (byte) (pdnslot & 0xFF);
       opipkt.length = 2;
       if (opiuce_put_com(opipkt) != 0) return -1;
       if (opiuce_get_com(opipkt) != 0) return -1;
       if ((opipkt.dataCode&0xff) == 0x40) 		return 0;
       else return -1;
   }
   
   
    /***
     *	Assigns the plugged in truesense ZigBee Channel to use. Valid
     *	values are 11-26.
     *	Inputs:
     *		comportptr, pointer to comport assigned to UC
     *		zbChan, the ZigBee Channel to be assigned to module
     *	Returns:
     *		0 if successful
     *		-1 if error
     */
   public synchronized int opiuce_settsezbchan(int zbChan)
   {
	   OPIPKT_struct opipkt = new OPIPKT_struct();
       opipkt.dataCode =(byte) 0x20;
       opipkt.payload[0] = (byte)0x03;
       opipkt.payload[1] = (byte) (zbChan & 0xFF);
       opipkt.length = 2;
       if (opiuce_put_com(opipkt) != 0) return -1;
       if (opiuce_get_com(opipkt) != 0) return -1;
       if ((opipkt.dataCode&0xff) == 0x40) 		return 0;
       else return -1;
   }
   
    /***
     *	Measure ZigBee-like signal presence and Energy Detection on current
     *	unicon channel
     *	Inputs:
     *		comportptr, pointer to comport assigned to UC
     *      	pktptr, pointer to packet that will be modified to have the measurement data
     *	Returns:
     *		0 if successful
     *		-1 if error
     */
   public synchronized int opiuce_wlmeasure(OPIPKT_struct pktptr)
   {
       pktptr.dataCode = (byte)0x10;
       pktptr.payload[0] =(byte) 0x10;
       pktptr.length = 1;
       if (opiuce_put_com(pktptr) != 0) return -10;
       int temp = opiuce_get_com(pktptr);
       if ( temp!= 0) return temp;
       if (((pktptr.dataCode&0xff) == 0x10) && (pktptr.length == 3)) return 0;
       else return -30;
   }
   
    
    /***
     *	Sets the unicon zigbee channel. Valid values are 11-26.
     *	Inputs:
     *		comportptr, pointer to comport assigned to UC
     *		zbchan, ZigBee channel number 11-26
     *	Returns:
     *		0 if successful
     *		-1 if error
     */
   public synchronized int opiuce_setzbchan( int zbChan)
   {
	   OPIPKT_struct opipkt = new OPIPKT_struct();

       opipkt.dataCode = (byte)0x20;
       opipkt.payload[0] = (byte)0x08;
       opipkt.payload[1] = (byte) (zbChan & 0xFF);
       opipkt.length = 2;
       if (opiuce_put_com(opipkt) != 0) return -1;
       if (opiuce_get_com(opipkt) != 0) return -1;
       if ((opipkt.dataCode&0xff) == 0x40) 		return 0;
       else return -1;
   }

   
   
    /***
     *	Sets the plugged in truesense Memory Module Write Feature.
     *	True value means truesense will try to write to memory module.
     *	False value means truesense will NOT try to write.
     *	Inputs:
     *		comportptr, pointer to comport assigned to UC
     *		mmwrite, false or true
     *	Returns:
     *		0 if successful
     *		-1 if error
     */
  public synchronized int opiuce_settsemmwrite( int mmwrite)
   {
	  OPIPKT_struct opipkt = new OPIPKT_struct();

       opipkt.dataCode = (byte)0x20;
       opipkt.payload[0] = (byte)0x06;
       opipkt.payload[1] = (byte) (mmwrite & 0xFF);
       opipkt.length = 2;
       if (opiuce_put_com(opipkt) != 0) return -1;
       if (opiuce_get_com(opipkt) != 0) return -1;
       if ((opipkt.dataCode&0xff) == 0x40) 		return 0;
       else return -1;
   }
   
   
    /***
     *	Sets the plugged in truesense RF TxPwr:
     * 0 -
     *	Inputs:
     *		comportptr, pointer to comport assigned to UC
     *		rftxpwr, mapped value to pwr
     *	Returns:
     *		0 if successful
     *		-1 if error
     */
   public synchronized int opiuce_settserftxpwr(int rftxpwr)
   {
	   OPIPKT_struct opipkt = new OPIPKT_struct();

       opipkt.dataCode = (byte)0x20;
       opipkt.payload[0] = (byte)0x05;
       opipkt.payload[1] = (byte) (rftxpwr & 0x0F);
       opipkt.length = 2;
       if (opiuce_put_com(opipkt) != 0) return -1;
       if (opiuce_get_com(opipkt) != 0) return -1;
       if ((opipkt.dataCode&0xff) == 0x40)        return 0;
       else return -1;
   }

   
    /***
     *	Sets the plugged in truesense RF Mode:
     *     0x00 - RF and double tap&timeout off
     *	0x01 - RF on and double tap&timeout off
     *	0x02 - RF off and double tap&timeout on
     *	0x03 - RF on and double tap&timeout on
     *	Inputs:
     *		comportptr, pointer to comport assigned to UC
     *		rfmode, mode settings in bit positions
     *	Returns:
     *		0 if successful
     *		-1 if error
     */
   public synchronized int opiuce_settserfmode(int rfmode)
   {
	   OPIPKT_struct opipkt = new OPIPKT_struct();

       opipkt.dataCode = (byte)0x20;
       opipkt.payload[0] = (byte)0x04;
       opipkt.payload[1] = (byte) (rfmode & 0xFF);
       opipkt.length = 2;
       if (opiuce_put_com(opipkt) != 0) return -1;
       if (opiuce_get_com(opipkt) != 0) return -1;
       if ((opipkt.dataCode&0xff) == 0x40)        return 0;
       else return -1;
   }

   
   
    /***
     *	Assigns the plugged in truesense pdn (Paired Device Number).
     *	Valid values are 0-254. 255 is reserved for unknown device.
     *	Inputs:
     *		comportptr, pointer to comport assigned to UC
     *		pdn, the PDN to be assigned
     *	Returns:
     *		0 if successful
     *		-1 if error
     */
   public synchronized int opiuce_settsepdn(int pdn)
   {
	   OPIPKT_struct opipkt = new OPIPKT_struct();

       opipkt.dataCode = (byte)0x20;
       opipkt.payload[0] = (byte)0x02;
       opipkt.payload[1] = (byte) (pdn & 0xFF);
       opipkt.length = 2;
       if (opiuce_put_com(opipkt) != 0) return -1;
       if (opiuce_get_com(opipkt) != 0) return -1;
       if ((opipkt.dataCode&0xff) == 0x40) 		return 0;
       else return -1;
   }
    
    /***
     *	Gets the plugged in truesense status: DSN, RTC, FirmWare Version,
     *		Paired Device Number (PDN), and other parameters
     *	Inputs:
     *		comportptr, pointer to comport assigned to UC
     *		pktptr, pointer to the packet where status will be put
     *	Returns:
     *		0 if successful
     *		-1 if error
     */
   public synchronized int opiuce_tsestatus(OPIPKT_struct pktptr)
   {
       pktptr.dataCode = (byte)0x20;
       pktptr.payload[0] = (byte)0x00;
       pktptr.length = 1;
       if (opiuce_put_com(pktptr) != 0) return -1;
       if (opiuce_get_com(pktptr) != 0) return -1;
       if (!(((pktptr.dataCode&0xff) == 0x20) && (pktptr.length == 19)))
           return -1;
       else
           return 0;
   }
   
    /***
     *	Forget the settings of plugged in module to specified slot (0 thru 7 valid)
     *	Inputs:
     *		comportptr, pointer to comport assigned to UC
     *		pdnslot, int value of pdn slot
     *	Returns:
     *		0 if successful
     *		-1 if error
     */
   public synchronized int opiuce_forgettsesettings(int pdnslot)
   {
	   OPIPKT_struct opipkt = new OPIPKT_struct();

       opipkt.dataCode = (byte)0x20;
       opipkt.payload[0] = (byte)0x0A;
       opipkt.payload[1] = (byte) (pdnslot & 0xFF);
       opipkt.length = 2;
       if (opiuce_put_com(opipkt) != 0) return -1;
       if (opiuce_get_com(opipkt) != 0) return -1;
       if ((opipkt.dataCode&0xff) == 0x40) 		return 0;
       else return -1;
   }
   
    /***
     *	Erase captured events
     *	Inputs:
     *		comportptr, pointer to comport assigned to UC
     *	Returns:
     *		0 if successful
     *		<0 if error
     */
    public synchronized int opiuce_evcaperase()
    {
        OPIPKT_struct opipkt = new OPIPKT_struct();
        opipkt.dataCode = (byte)0x10;
        opipkt.payload[0] = (byte)0x2F;
        opipkt.length = 1;
        if (opiuce_put_com(opipkt) != 0) return -1;
        if (opiuce_get_com(opipkt) != 0) return -1;
        if ((opipkt.dataCode&0xff) == 0x40) 		return 0;
        else return -1;
    }
    
    /***
     *	Set controller to on-state where it will turn on
     * plugged in truesense.
     *	Inputs:
     *		comportptr, pointer to comport assigned to UC
     *	Returns:
     *		0 if ok
     *		<0 if error
     */
    
    public synchronized int opiuce_onmode()
    {
    	OPIPKT_struct opipkt = new OPIPKT_struct();
    	
    	opipkt.dataCode = (byte)0x20;
        opipkt.payload[0] = (byte)0x23;
        opipkt.length = 1;
        if(opiuce_put_com(opipkt) != 0) return -1;
        if(opiuce_get_com(opipkt) != 0) return -1;
        if((opipkt.dataCode&0xff) == 0x40)   return 0;
        else return -1;
    }
    /***
	  *	Get an OPI packet from the com port,
	  *	Inputs:
	  *		pktptr, pointer to the packet
	  *		comportptr, pointer to handle
	  *	Returns:
	  *		code:
	  *			0	// valid packet
	  *			1	// invalid packet
	 // *		2	// null packet
	  * 	    3   // length in packet is different to lengthcheck (high+low)
	  *			-1 	// couldn't get packet in 20 tries
	  *        -2 -3 //can't get syncbyte
	  */
	public synchronized int opiuce_get_com(OPIPKT_struct pktptr){
		if(con==null)
			return -1;
		int i = 0 , j = 0;
		long pktChksm=0;
		long calcChksm = 0;
		byte [] buf = new byte[65541];
		
	    int debug=0;
	    for(j = 0;j<20;j++)//read one
	    {
	    	debug=0;
	    	debug=con.bulkTransfer(epIN, buf, 65541, OPITIMEOUT); 
	    	if(debug>0)
	    		break;
	    }
	    if(j==20)
	    	return -1;
	    else
	    {	
	    	if(buf[0]!=SYNCBYTE) return -2;
	    	if(buf[1]!=SYNCBYTE) return -3;
	    	
	    	pktptr.length=0;
	    	pktptr.length = pktptr.length +((buf[2]& 0xFF)<<8);
	    	pktptr.length = pktptr.length - 1;
	    	pktptr.length = pktptr.length + (buf[3]& 0xFF) ;
	    			 		

	    	if(pktptr.length==-1) return 20;
	    	if(pktptr.length!=(debug-7)) return 30;
	    	
	    	pktptr.dataCode = (byte) (buf[4]);
	    	calcChksm=0;
	    	calcChksm =  calcChksm  +  (buf[4]& 0xFF);
	    	
	    	for(i=0;i<pktptr.length;i++) 
	    	{
	    		pktptr.payload[i] = buf[5+i];	    	
	    		calcChksm = calcChksm + (buf[5+i]& 0xFF);
	    	}
	    	 
	    	pktChksm=0;
	    	pktChksm = pktChksm  + (buf[5+pktptr.length+1]&0xFF);
	        pktChksm = pktChksm  + (((buf[5+pktptr.length]&0xFF)<<8));
	        if (pktChksm != calcChksm) return 10;
	         else return 0;
	    }
	}
	/***
	  *	Request wireless truesense data from unicon (received from truesense).
	  *	Data format is specified in wired frame definition document.
	  *	Inputs:
	  *		comportptr, pointer to comport assigned to UC
	  *		pktptr, pointer to packet that will have the data, if any
	  *	Returns:
	  *		1 if received data
	  *		0 if no data
	  *		-1 if error
	  */
	public synchronized int opiuce_getwltsdata(OPIPKT_struct pktptr){
		pktptr.setdataCode((byte)(0x10));
		pktptr.setpayload((byte)(0x00),0);
		pktptr.setlength(1);
		if(this.opiuce_put_com(pktptr)!= 0) return -1;
		if(this.opiuce_get_com(pktptr) !=0) return -1;
		if((pktptr.getdataCode()&0XFF) == 0x40) return 0;
		else if((pktptr.getdataCode()&0XFF) == 0x01) return 1;
		return -1;
	}
	/***
	  *	Put an OPI packet to the com port,
	  *	Inputs:
	  *		pktptr, pointer to the packet
	  *		comportptr, pointer to handle
	  *	Returns:
	  *		code:
	  *			0	// successful
	  *			-1	// error
	  */
	public synchronized int opiuce_put_com(OPIPKT_struct pktptr){
		if(con==null)
			return -1;
		int i,calcChksm = 0,debug=0,j;
		byte [] sendbuf= new byte[5+pktptr.getlength()+2];

		sendbuf[0] = SYNCBYTE;
		sendbuf[1] = SYNCBYTE;
		sendbuf[2] = (byte) ((0xff)& (((pktptr.getlength()+1) >>8)));
		sendbuf[3] = (byte) ((0xff)& ((pktptr.getlength()+1)));
		sendbuf[4] = (byte) ((pktptr.getdataCode()));
		calcChksm =0;
		calcChksm = calcChksm + (pktptr.getdataCode()&0xFF);
		
		for(i = 0;i< pktptr.getlength();i++)
			calcChksm = calcChksm + (pktptr.getpayload(i)&0xFF);
		
		pktptr.setpayload((byte)((calcChksm>>8)&0xff), i);
		pktptr.setpayload((byte)(calcChksm&0xff), i+1);
		
		
		for(j=0;j<=i+1;j++)
			sendbuf[5+j] = (pktptr.getpayload(j));
		
		debug=con.bulkTransfer(epOUT, sendbuf,5+pktptr.getlength()+2, OPITIMEOUT);
		
		if(debug!=5+pktptr.getlength()+2)
			return -1;
		else
			return 0;
	}
	
	
	public synchronized void opi_closeuce_com(){
		if(con!=null)
		{
	     	if(opiuce_resetrelaxdata()!=0)
	     	{
	     		OPIPKT_helper.opiwait(200);
	     		opiuce_resetrelaxdata();
	     	}
	     	comPort = 0;
			this.con.close();
		}
	}
	/***
	  *	Gets the plugged in UC status: current counter value, FirmWave Version,
	  *	Mode, uSD status, charger status, associated Devices and related info
	  *	Inputs:
	  *		comportptr, pointer to comport assigned to UC
	  *		pktptr, pointer to the packet where status will be put
	  *	Returns:
	  *		0 if successful
	  *		<0 if error
	  */
	public synchronized int opiuce_status(OPIPKT_struct pktptr){
		pktptr.setdataCode((byte)(0x10));
		pktptr.setpayload((byte)(0x01), 0);
		pktptr.setlength(1);
		int temp=0;
		
		if(this.opiuce_put_com(pktptr)!=0) 
		return -10;
		
		temp = this.opiuce_get_com(pktptr);
		if(temp!=0) return temp;

		if (!((pktptr.getdataCode()&0XFF) == 0x10))
		  return -20;
		else if(! (pktptr.getlength() == 127))
	      return -30;
		
	    else return 0;

	}
    
    /**
     *  Check and setup device. Assumes permission has already been granted to use device
    **/
	public synchronized boolean checkDevice(UsbManager managerp, UsbDevice device){
		this.opi_closeuce_com();
        manager = managerp;
        mDevice = device;
		con = manager.openDevice(mDevice);
		usbIf = mDevice.getInterface(1);
		usbIf.getEndpointCount();
		con.claimInterface(usbIf, true);
		
		// setup, acm control message
		byte[] msg = { (byte) 0x00, (byte) 0xC2, (byte) 0x01, (byte) 0x00, // baudrate in hex
				(byte) 0x00, // stop bits=1
				(byte) 0x00, // parity = none
				(byte) 0x08, // data bits				
		};
		
		if(con.controlTransfer(USB_RT_ACM, SET_CONTROL_LINE_STATE, 0, 0, null, 0, OPITIMEOUT)<0)
			return false;
		if(con.controlTransfer(USB_RT_ACM, SET_LINE_CODING, 0, 0, msg, msg.length, OPITIMEOUT)<0)
			return false;
		
		for (int i = 0; i < usbIf.getEndpointCount(); i++) {
			if (usbIf.getEndpoint(i).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
				if (usbIf.getEndpoint(i).getDirection() == UsbConstants.USB_DIR_IN)
					epIN = usbIf.getEndpoint(i);
				else
					epOUT = usbIf.getEndpoint(i);
			}
		}    	
		return true;
    }
	
	/***
	  *	Finds the OPI device if it is connected.
	  *	Can't assume opened comport conforms to OPI packet protocol, so need to
	  *	check each byte if written/received and if correct value
	  *	Inputs:
	  *		comportptr, for assigning the resource when found
	  *	Returns:
	  *		0 if successful
	  *		<0 if not successful
	 * @throws IOException 
	  */
		public synchronized int opi_openuce_com(){
		int j = 0,pktLen = 0;
		byte [] recBuf = new byte [256];
		int pktChksm;
		long calcChksm;
		byte [] tempuint8 = new byte [500];
		int tempint16 = 0;
		int debug=0,countr=0;
		
		comPort = 0;
		if(con==null)
			return -1;
		
		debug=con.bulkTransfer(epOUT, new byte [] {SYNCBYTE,SYNCBYTE,0x00,0x02,0x10,0x01,0x00,0x11}, 8, OPITIMEOUT); // chksm low byte
        if(debug!=8)
			return -2;
		
        for(countr = 0 ; countr <CHECKTIMES;countr++)
        {
        	debug=0;
        	debug=con.bulkTransfer(epIN, tempuint8, 500, OPITIMEOUT); // chksm low byte
        	if((debug==134))
        	{
              if(tempuint8[0]==SYNCBYTE&&tempuint8[1]==SYNCBYTE)
        	  break;
        	}
        }
        
        if(countr==CHECKTIMES)
        	{
        	  con.close();
        	  return -3;
        	}
        
        tempint16=0;
        tempint16 += tempuint8[3]&0xFF;
        tempint16 += ((tempuint8[2]&0xFF)<<8) ;
        
        if((tempint16!=OPIUCDSTLEN))
        {
        	con.close();
        	return -4;
        }
        calcChksm = 0;
	    pktLen = tempuint8[3]&0xFF;
	    if(pktLen<0)
	    	pktLen = -1*pktLen;
	    
	    for (j = 0; j < pktLen; j++)
        {
	    	recBuf[j] = tempuint8[4+j];
            calcChksm = calcChksm + (recBuf[j]&0xFF);
        }
	    if (j < pktLen)
        {
        	con.close();
	    	return -5;	
        }
	    pktChksm = ((tempuint8[4+pktLen]&0xFF)<<8);
	    pktChksm = pktChksm + (tempuint8[4+pktLen+1]&0xFF);
	    if(pktChksm!=calcChksm){
	    	con.close();
	    	return -6;	
	    }
	    
	    //OPIUCE
	    //79 80 73 85 67 69
	    if(!((recBuf[12]&0x000000FF) == 79))//O
	    {
	    	con.close();
	    	return -7;	
	    }
	    if(!((recBuf[13]&0x000000FF) == 80))//P
	    {
	    	con.close();
	    	return -8;	
	    }
	    if(!((recBuf[14]&0x000000FF) == 73))//I
	    {
	    	con.close();
	    	return -8;	
	    }
	    if(!((recBuf[15]&0x000000FF) == 85))//U
	    {
	    	con.close();
	    	return -9;	
	    }
	    if(!((recBuf[16]&0x000000FF) == 67))//C
	    {
	    	con.close();
	    	return -10;	
	    }
	    if(!((recBuf[17]&0x000000FF) == 69))//E
	    {
	    	con.close();
	    	return -11;	
	    }
	    
	    comPort = 1;
	    return 0;
	    }
}
