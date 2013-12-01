package com.utoronto.syncgallery.adapter;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.utoronto.syncgallery.R;

public class sdAdapter extends BaseAdapter{

    private LayoutInflater mInflater;
    private List<Map<String, Object>> mData;
    ViewHolder holder = null;
    public sdAdapter(Context context){
        this.mInflater = LayoutInflater.from(context);
    }
    
    public final class ViewHolder{
        public ImageView img;
        public TextView text;    
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
            
//            convertView = mInflater.inflate(R.layout.sdcard, null);
//            holder.img = (ImageView)convertView.findViewById(R.id.img);
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