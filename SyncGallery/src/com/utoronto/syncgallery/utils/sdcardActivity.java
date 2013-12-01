package com.utoronto.syncgallery.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.utoronto.syncgallery.R;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class sdcardActivity extends ListActivity {
	    
		/** Initial step. */
	    private SQLiteDatabase db = null;
	    private Cursor cursor = null;
	    private List<Map<String, Object>> mData;
	    
	    sdfunction function = new sdfunction();
	    
	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        
	        function.initDatabase();
	        
	        mData = getData();

	        sdAdapter adapter = new sdAdapter(this);
	        setListAdapter(adapter);
	    }
	    
	    private List<Map<String, Object>> getData() {
	        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
	        
	        db = SQLiteDatabase.openOrCreateDatabase(config.f, null); 
	        Map<String, Object> map = new HashMap<String, Object>();
	        try {
	        	
	            cursor = db.query("test_listview", new String[]{}, null, null, null, null, null);
	            while(cursor.moveToNext()) {
	                String text = cursor.getString(cursor.getColumnIndex("text"));
	                map = new HashMap<String, Object>();
	                map.put("text", text);
	                list.add(map);
	            }
	        } catch (Exception e) {
	            // TODO: handle exception
	            Log.i("opendatabase", e.toString());
	        }
	        
	        return list;
	    }

	    @Override
	    protected void onListItemClick(ListView l, View v, int position, long id) {
	    	super.onListItemClick(l, v, position, id);
	        function.showDialog((String)mData.get(position).get("text"), this);
	        //Toast.makeText(MyListViewActivity.this, (String)mData.get(position).get("text"), Toast.LENGTH_LONG).show(); 
	    }
	    
	    public final class ViewHolder{
	            public ImageView img;
	            public TextView text;    
	    }
	    
	    public class sdAdapter extends BaseAdapter{

	        private LayoutInflater mInflater;
	        private List<Map<String, Object>> mData;
	        ViewHolder holder = null;
	        public sdAdapter(Context context){
	            this.mInflater = LayoutInflater.from(context);
	        }
	        	        
	        @Override
	        public int getCount() {
	            return mData.size();
	        }

	        @Override
	        public Object getItem(int arg0) {
	            return null;
	        }

	        @Override
	        public long getItemId(int arg0) {
	            return 0;
	        }

	        @Override
	        public View getView(int position, View convertView, ViewGroup parent) {
	            if (convertView == null) {
	                holder = new ViewHolder();  
	                
	                convertView = mInflater.inflate(R.layout.list_items, null);
	                holder.img = (ImageView)convertView.findViewById(R.id.file_abs_path);
	                holder.text = (TextView)convertView.findViewById(R.id.text);
	                convertView.setTag(holder);
	                
	            }else {
	                
	                holder = (ViewHolder)convertView.getTag();
	            }

	            holder.img.setBackgroundResource((Integer)mData.get(position).get("img"));
	            holder.text.setText((String)mData.get(position).get("text"));
	            
	            return convertView;
	        }
	    }
}
