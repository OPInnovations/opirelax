package opi_android;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import opi.relax.opirelax.MainActivity;

import android.os.Environment;



public class OPIPKT_Raw_File {
	public  BufferedWriter out=null;
	public FileWriter filewriter=null;
	public String FilePath;
	
	public OPIPKT_Raw_File(){
		super();
		FilePath = Environment.getExternalStorageDirectory().getPath();
		FilePath += "/OPI/";
		// create a File object for the parent directory
		File wallpaperDirectory = new File(FilePath);
		// have the object build the directory structure, if needed.
		wallpaperDirectory.mkdirs();
		out=null;
		filewriter=null;
	}
	

	 
	public boolean SetFileName(String name)
	{
		try {
			filewriter = new FileWriter(FilePath+name);
			out = new BufferedWriter(filewriter);
			if(filewriter==null)
				return false;
			if(out==null)
				return false;
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return false;
		}
		return true;
	}
	
	public boolean WriteStringToFile(String stringdata)
	{
		try {
			 out.write(stringdata);
		     return true;	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean CloseFile()
	{
    	try {
    		if(out!=null)
    		{
    			out.flush();
    			out.close();
    		}
		    return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} 
	}
}
