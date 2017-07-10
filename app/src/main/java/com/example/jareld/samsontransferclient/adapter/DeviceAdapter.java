package com.example.jareld.samsontransferclient.adapter;

import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.preference.SwitchPreference;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.jareld.samsontransferclient.R;

import java.util.ArrayList;
import java.util.HashMap;

/*
 *  @项目名：  TestWifiDerect 
 *  @包名：    com.example.lyc2.testwifiderect.adapter
 *  @文件名:   DeviceAdapter
 *  @创建者:   LYC2
 *  @创建时间:  2016/11/24 11:46
 *  @描述：    TODO
 */
public class DeviceAdapter
        extends RecyclerView.Adapter<DeviceAdapter.MyHolder>
{
    private boolean mIsServer;
    private ArrayList<ImageView> iv_arr = new ArrayList<>();
    private Resources mResources;

    public DeviceAdapter(ArrayList<HashMap<String, String>> peerLists,
                         boolean isServer,
                         Resources resources)
    {

        this.mList = peerLists;
        this.mIsServer = isServer;
        this.mResources = resources;
    }

    public DeviceAdapter(ArrayList<HashMap<String, String>> peerLists,
                         boolean isServer,
                         Resources resources,
                         int beSelectDevicePosition)
    {
        this(peerLists , isServer , resources);
        this.beSelectedPosition = beSelectDevicePosition;

    }

    public ArrayList<HashMap<String, String>> getList(){
        return mList;
    }
    //来一个点击的回调
    public interface ItemButtonClickConnectListener {
        void onItemButtonClickConnectListener(int position);
    }

    private ItemButtonClickConnectListener mItemButtonClickConnectListener;
    private int beSelectedPosition = -1;

    public void setOnItemButtonClickConnectListener(ItemButtonClickConnectListener i) {
        this.mItemButtonClickConnectListener = i;
    }


    private ArrayList<HashMap<String, String>> mList;

    public DeviceAdapter() {}

    public DeviceAdapter(ArrayList<HashMap<String, String>> list, boolean isServer) {
        super();
        this.mList = list;
        this.mIsServer = isServer;
    }

    @Override
    public DeviceAdapter.MyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        View     view     = inflater.inflate(R.layout.recycle_item, parent, false);
        MyHolder myHolder = new MyHolder(view);
        return myHolder;
    }

    @Override
    public void onBindViewHolder(final DeviceAdapter.MyHolder holder, final int position) {
        SwitchPreference switchPreference;
        // 中文  设备名称
       // holder.mTv_name.setText("Device name：" + mList.get(position)
       //                                        .get("name"));
        holder.mTv_name.setText("设备名称：" + mList.get(position)
                                                      .get("name"));
        // 中文  设备地址
//        holder.mTv_address.setText("Device address：" + mList.get(position)
//                                                  .get("address"));
        holder.mTv_address.setText("设备地址：" + mList.get(position)
                                                         .get("address"));
        holder.mTv_name.setSelected(true);
        holder.mTv_address.setSelected(true);
        if(position == beSelectedPosition){
            holder.mBtn_connect.setImageBitmap(BitmapFactory.decodeResource(mResources , R.mipmap.dagou));
        }

        iv_arr.add(holder.mBtn_connect);
        if (mItemButtonClickConnectListener != null) {
            holder.mContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (!mIsServer) {
                        mItemButtonClickConnectListener.onItemButtonClickConnectListener(position);
                    }

                }
            });
            holder.mBtn_connect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.mContainer.performClick();
                }
            });
        }

    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public class MyHolder
            extends RecyclerView.ViewHolder
    {
        public TextView  mTv_name;
        public TextView  mTv_address;
        public ImageView mBtn_connect;
        public LinearLayout mContainer;
        public MyHolder(View itemView) {
            super(itemView);
            mTv_name = (TextView) itemView.findViewById(R.id.tv_name);
            mTv_address = (TextView) itemView.findViewById(R.id.tv_address);
            mBtn_connect = (ImageView) itemView.findViewById(R.id.item_connect);
            mContainer = (LinearLayout) itemView.findViewById(R.id.item_container);

        }
    }

    public void resetDeivce() {
        beSelectedPosition = -1;
    }

    public ArrayList<ImageView> getIvArr() {
        if (iv_arr != null) {
            return iv_arr;
        }else{
            return null;
        }
    }
}
