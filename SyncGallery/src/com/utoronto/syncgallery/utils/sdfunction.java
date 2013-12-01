package com.utoronto.syncgallery.utils;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class sdfunction {
    private SQLiteDatabase db;
    private int i;
    /**
     * create database folder and database 
     * and check the table is already exist to create new 
     */
    public void initDatabase(){
         
        if(!config.path.exists()){  
            config.path.mkdirs();
            Log.i("init", "create a database folder");  
        }   
        if(!config.f.exists()){      
            try{   
                config.f.createNewFile();  
                Log.i("init", "create a new database file");
            }catch(IOException e){   
                Log.i("init", e.toString());  
            }   
        } 
          
        try {
            if(tabIsExist("test_listview")==false){
                db.execSQL("create table test_listview(id integer primary key autoincrement," +
                        "text varchar(128))");
                for(i=0;i<10;i++){
                    db.execSQL("insert into test_listview(text)values('Auto insert value "+i+"')");
                }
                Log.i("init", "create a table test_listview");
            }
        } catch (Exception e) {
            // TODO: handle exception
            Log.i("init", e.toString());  
        }
    }
    /**
     * check the database is already exist
     * @param tabName
     * @return
     */
    public boolean tabIsExist(String tabName){
        boolean result = false;
        if(tabName == null){
                return false;
        }
        Cursor cursor = null;
        db = SQLiteDatabase.openOrCreateDatabase(config.f, null); 
        try {
            String sql = "select count(*) as c from sqlite_master where type ='table' " +
                        "and name ='"+tabName.trim()+"' ";
            cursor = db.rawQuery(sql, null);
            if(cursor.moveToNext()){
                int count = cursor.getInt(0);
                if(count>0){
                    result = true;
                }
            }
                
        } catch (Exception e) {
                // TODO: handle exception
        }                
        return result;
    }
    
    /**
     * define a dialog for show the message
     * @param mess
     * @param activity
     */
    public void showDialog(String mess,Activity activity){
      new AlertDialog.Builder(activity).setTitle("Message").setMessage(mess)
       .setNegativeButton("Confirm",new DialogInterface.OnClickListener()
       {
         public void onClick(DialogInterface dialog, int which)
         {          
         }
       })
       .show();
    }
}