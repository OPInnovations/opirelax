// Created by JeffMeJones@gmail.com
package opi_android;


import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;


public class GifRun implements Runnable, Callback {

	
	public Bitmap bmb;
	public GIFDecode decode;
	public int ind;
	public int gifCount;
	public SurfaceHolder mSurfaceHolder ;
	public SurfaceView mSurfaceView ;
	public boolean surfaceExists;
	int counter;
	
	public void LoadGiff(SurfaceView v,InputStream input)
	{		
		//InputStream Raw= context.getResources().openRawResource(R.drawable.image001);
		mSurfaceView = v;
	       mSurfaceHolder = v.getHolder();
	       mSurfaceHolder.addCallback(this);
	       decode = new GIFDecode();
	       decode.read(input);
	       ind = 0;
	       counter=0;
			// decode.
			gifCount = decode.getFrameCount();
			bmb = decode.getFrame(0);
			surfaceExists=true;
			Thread t = new Thread(this);
			t.start();	
	}
	
	public void run() 
	{
		if (surfaceExists) {
			for(int i=0;i<gifCount;i++)
			{
				try {
					
					Canvas rCanvas = mSurfaceHolder.lockCanvas();
					rCanvas.drawBitmap(bmb, rCanvas.getWidth()/4, rCanvas.getHeight()/4, new Paint());
					//ImageView im = (ImageView) findViewById(R.id.imageView1);
					//im.setImageBitmap(bmb);
					mSurfaceHolder.unlockCanvasAndPost(rCanvas);
					bmb = decode.next();
					counter++;
					if(counter==gifCount)
					{
						counter=0;
						Thread.sleep(1000);
						OPIPKT_DrawHelper.cleansurfaceview(mSurfaceHolder,mSurfaceView);
					}
					else
					Thread.sleep(100);
					} catch (Exception ex) {
				}		
			}
		}
		
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) 
	{
		
		
		
	}

	public void surfaceCreated(SurfaceHolder holder) 
	{
		
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		
		surfaceExists=false;
	}
	
}
