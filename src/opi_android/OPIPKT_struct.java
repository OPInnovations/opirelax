package opi_android;

public class OPIPKT_struct {
	public byte dataCode;
	public int length;
	public byte  [] payload = new byte [1024];
	
	public synchronized byte getdataCode(){
		return dataCode;
	}
	public synchronized int getlength(){
		return length;
	}
	public synchronized byte[] getpayload(){
		return payload;
	}
	public synchronized byte getpayload(int index){
		return payload[index];
	}
	public synchronized void setdataCode(byte d){
		dataCode = d;
	}
	public synchronized void setlength(int l){
		length = l;
	}
	public synchronized void setpayload(byte [] p){
		for(int i=0;i<p.length;i++)
		payload[i] = p[i];
	}
	public synchronized void setpayload(byte p,int index){
		payload[index] = p;
	}
}
