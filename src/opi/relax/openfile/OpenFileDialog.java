package opi.relax.openfile;  
  
import java.io.File;  
import java.util.ArrayList;  
import java.util.HashMap;  
import java.util.List;  
import java.util.Map;  

import opi.relax.opirelax.R;
import android.app.Activity;  
import android.app.AlertDialog;  
import android.app.Dialog;  
import android.content.Context;  
import android.content.DialogInterface;
import android.os.Bundle;  
import android.os.Environment;
import android.view.View;  
import android.widget.AdapterView;  
import android.widget.ListView;  
import android.widget.SimpleAdapter;  
import android.widget.Toast;  
import android.widget.AdapterView.OnItemClickListener;  
  
public class OpenFileDialog {  
    public static String tag = "OpenFileDialog";  
    public static String sRoot = "/";   
    static public String sParent = "..";  
    static public String sFolder = ".";  
    static final public String sEmpty = "";  
    static final private String sOnErrorMsg = "No rights to access!";  
    
    public static Dialog createDialog(int id, Context context, String title, CallbackBundle callback, String suffix, Map<String, Integer> images){  
    	sRoot = Environment.getExternalStorageDirectory().getPath();
    	sRoot += "/OPI/";
    	File wallpaperDirectory = new File(sRoot);
		wallpaperDirectory.mkdirs();
		AlertDialog.Builder builder = new AlertDialog.Builder(context);  
        builder.setView(new FileSelectView(context, id, callback, suffix, images));  
        Dialog dialog = builder.create();  
        dialog.setTitle(title);  
        return dialog;  
    }  
      
    static class FileSelectView extends ListView implements OnItemClickListener{  
        private CallbackBundle callback = null;  
        private String path = sRoot;  
        private List<Map<String, Object>> list = null;  
        private int dialogid = 0;  
        public Context viewcontext;
        private String suffix = null;  
        private String finalpt="";
        private String finalfn="";
        private Map<String, Integer> imagemap = null;  
        public FileSelectView(Context context, int dialogid, CallbackBundle callback, String suffix, Map<String, Integer> images) {  
            super(context);  
            this.viewcontext=context;
            this.imagemap = images;  
            this.suffix = suffix==null?"":suffix.toLowerCase();  
            this.callback = callback;  
            this.dialogid = dialogid;  
            this.setOnItemClickListener(this);  
            refreshFileList();  
        }  
          
        private String getSuffix(String filename){  
            int dix = filename.lastIndexOf('.');  
            if(dix<0){  
                return "";  
            }  
            else{  
                return filename.substring(dix+1);  
            }  
        }  
          
        private int getImageId(String s){  
            if(imagemap == null){  
                return 0;  
            }  
            else if(imagemap.containsKey(s)){  
                return imagemap.get(s);  
            }  
            else if(imagemap.containsKey(sEmpty)){  
                return imagemap.get(sEmpty);  
            }  
            else {  
                return 0;  
            }  
        }  
          
        private int refreshFileList()  
        {  
            // refresh  
            File[] files = null;  
            try{  
                files = new File(path).listFiles();  
            }  
            catch(Exception e){  
                files = null;  
            }  
            if(files==null){  
                Toast.makeText(getContext(), sOnErrorMsg,Toast.LENGTH_SHORT).show();  
                return -1;  
            }  
            if(list != null){  
                list.clear();  
            }  
            else{  
                list = new ArrayList<Map<String, Object>>(files.length);  
            }  
              
            ArrayList<Map<String, Object>> lfolders = new ArrayList<Map<String, Object>>();  
            ArrayList<Map<String, Object>> lfiles = new ArrayList<Map<String, Object>>();  
              
            if(!this.path.equals(sRoot)){  
                Map<String, Object> map = new HashMap<String, Object>();  
                map.put("name", sRoot);  
                map.put("path", sRoot);  
                map.put("img", getImageId(sRoot));  
                list.add(map);  
                  
                map = new HashMap<String, Object>();  
                map.put("name", sParent);  
                map.put("path", path);  
                map.put("img", getImageId(sParent));  
                list.add(map);  
            }  
              
            for(File file: files)  
            {  
                if(file.isDirectory() && file.listFiles()!=null){   
                    Map<String, Object> map = new HashMap<String, Object>();  
                    map.put("name", file.getName());  
                    map.put("path", file.getPath());  
                    map.put("img", getImageId(sFolder));  
                    lfolders.add(map);  
                }  
                else if(file.isFile()){  
                    String sf = getSuffix(file.getName()).toLowerCase();  
                    if(suffix == null || suffix.length()==0 || (sf.length()>0 && suffix.indexOf("."+sf+";")>=0)){  
                        Map<String, Object> map = new HashMap<String, Object>();  
                        map.put("name", file.getName());  
                        map.put("path", file.getPath());  
                        map.put("img", getImageId(sf));  
                        lfiles.add(map);  
                    }  
                }    
            }  
              
            list.addAll(lfolders); 
            list.addAll(lfiles);  
            SimpleAdapter adapter = new SimpleAdapter(getContext(), list, R.layout.filedialogitem, new String[]{"img", "name", "path"}, new int[]{R.id.filedialogitem_img, R.id.filedialogitem_name, R.id.filedialogitem_path});  
            this.setAdapter(adapter); 
            return files.length;  
        }  
        
        public synchronized void  checkdialog(boolean result)
        {
        	
        	if(result==true)//yes
        	{
                Bundle bundle = new Bundle();  
                bundle.putString("path", finalpt);  
                bundle.putString("name", finalfn); 
                this.callback.callback(bundle);    
                ((Activity)getContext()).dismissDialog(this.dialogid); 
                return;  
        	}
        	else
        	{
        		SimpleAdapter adapter = new SimpleAdapter(getContext(), list, R.layout.filedialogitem, new String[]{"img", "name", "path"}, new int[]{R.id.filedialogitem_img, R.id.filedialogitem_name, R.id.filedialogitem_path});  
                this.setAdapter(adapter); 
        	}
        }
    
        @Override  
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {  
            //
            finalpt = (String) list.get(position).get("path");  
            finalfn = (String) list.get(position).get("name");  
            if(finalfn.equals(sRoot) || finalfn.equals(sParent)){   
                File fl = new File(finalpt);  
                String ppt = fl.getParent();  
                if(ppt != null){  
                    // ªðback 
                    path = ppt;  
                }  
                else{  
                    // root 
                    path = sRoot;  
                }  
            }  
            else{  
                File fl = new File(finalpt);  
                if(fl.isFile()){  
                    // if file
                	new AlertDialog.Builder(this.viewcontext)  
                	.setMessage("Open "+finalfn+" ?")  
                	.setCancelable(false)  
                	.setPositiveButton("Yes", new DialogInterface.OnClickListener() {  
                	    public void onClick(DialogInterface dialog, int which)   
                	    {  
                	    	  // Perform Your Task Here--When Yes Is Pressed. 
                	    	   checkdialog(true);
                	           dialog.cancel();  
                	    }  
                	})    
                	.setNegativeButton("No", new DialogInterface.OnClickListener() {  
                	      public void onClick(DialogInterface dialog, int which)   
                	      {  
                	    	  // Perform Your Task Here--When No is pressed  
                	    	 checkdialog(false);
                	         dialog.cancel();  
                	      }  
                	}).show();  
                }  
                else if(fl.isDirectory()){  
                    path = finalpt;  
                }  
            }  
            this.refreshFileList();  
        }  
    }  
}  