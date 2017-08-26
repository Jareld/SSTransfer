package com.example.jareld.samsontransferclient.adapter;

/*
 *  @项目名：  SamsonTransfer 
 *  @包名：    com.example.jareld.samsontransferclient.adapter
 *  @文件名:   PicVideoAdpter
 *  @创建者:   lyc-2
 *  @创建时间:  2017/5/3 16:36
 *  @描述：    TODO
 */

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.jareld.samsontransferclient.R;
import com.example.jareld.samsontransferclient.activity.PicVideoSelectorActivity;

import java.io.File;
import java.util.ArrayList;

public class PicVideoAdpter
        extends RecyclerView.Adapter<PicVideoAdpter.MyHolder>
{

    private static final String TAG = "jiazai";
    private ArrayList<File> picsVideosPath;
    private Context            mContext;
    private ArrayList<Boolean> mBooleens;

    public PicVideoAdpter() {

    }

    public PicVideoAdpter(Activity context, ArrayList<File> picsVideos) {
        mContext = context;
        picsVideosPath = picsVideos;
        mBooleens = new ArrayList<>();
        for (int i = 0; i < picsVideosPath.size(); i++) {
            mBooleens.add(false);
        }
    }

    @Override
    public PicVideoAdpter.MyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View           view     = inflater.inflate(R.layout.picvideo_selector_item, parent, false);
        MyHolder       myHolder = new MyHolder(view);
        return myHolder;
    }

    @Override
    public void onBindViewHolder(final PicVideoAdpter.MyHolder holder,  int position) {
        final int final_postin = position;
        Glide.with(mContext)
             .load(picsVideosPath.get(position))
             .sizeMultiplier(0.5f)
             .centerCrop()
             .placeholder(R.mipmap.ic_launcher)
             .diskCacheStrategy(DiskCacheStrategy.RESULT)
             .into(holder.mIv_main);

        File   file      = picsVideosPath.get(position);
        String name      = file.getName();
        int    i         = name.lastIndexOf(".");
        String substring = name.substring(i + 1);
        if (substring.equals("mp4")) {
            holder.mIv_video.setVisibility(View.VISIBLE);
            holder.mIv_video.setImageAlpha(150);
        } else {
            holder.mIv_video.setVisibility(View.GONE);
        }
        Log.d(TAG, "onBindViewHolder: " + position + "::" + mBooleens.get(position));
        if(mBooleens.get(position)){
            holder.mIv_dagou.setVisibility(View.VISIBLE);
        }else{
            holder.mIv_dagou.setVisibility(View.GONE);

        }


        holder.mIv_main.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.mIv_dagou.getVisibility() == View.GONE) {
                    //目前是消失的 就让他出现
                    holder.mIv_dagou.setVisibility(View.VISIBLE);
                    mBooleens.set(final_postin, true);

                } else {
                    //目前是出现的  就让他消失
                    holder.mIv_dagou.setVisibility(View.GONE);
                    mBooleens.set(final_postin, false);

                }
                long              file_length = 0;
                ArrayList<String> arrayList   = new ArrayList<>();
                for (int i = 0; i < mBooleens.size(); i++) {
                    if (mBooleens.get(i)) {
                        arrayList.add(picsVideosPath.get(i)
                                                    .getAbsolutePath());
                        file_length += picsVideosPath.get(i)
                                                     .length();
                        Log.d("hy", "onClick: " + i +"::");
                    }
                }
                file_length /= 1024 * 1024;
        //中文  ："（选中" + arrayList.size() + "个文件，共"+file_length +"MB）"

                //        ((PicVideoSelectorActivity) mContext).setSendInfo("（Select " + arrayList.size() + " files，a total of "+file_length +"MB）");
            ((PicVideoSelectorActivity) mContext).setSendInfo("（选中 " + arrayList.size() + " 个文件，共 "+file_length +"MB）");

            }
        });
    }

    @Override
    public int getItemCount() {
        return picsVideosPath.size();
    }

    public class MyHolder
            extends RecyclerView.ViewHolder
    {

        public ImageView mIv_main;
        public ImageView mIv_biankuang;
        public ImageView mIv_dagou;
        public ImageView mIv_video;

        public MyHolder(View itemView) {
            super(itemView);
            mIv_main = (ImageView) itemView.findViewById(R.id.iv_main);
            mIv_biankuang = (ImageView) itemView.findViewById(R.id.iv_biankuang);
            mIv_dagou = (ImageView) itemView.findViewById(R.id.iv_dagou);
            mIv_video = (ImageView) itemView.findViewById(R.id.video_info);

        }
    }

    public ArrayList<String> getSelectedPV() {
        ArrayList<String> arrayList = new ArrayList<>();
        for (int i = 0; i < mBooleens.size(); i++) {
            if (mBooleens.get(i)) {
                arrayList.add(picsVideosPath.get(i)
                                            .getAbsolutePath());
            }
        }
        return arrayList;
    }
}
