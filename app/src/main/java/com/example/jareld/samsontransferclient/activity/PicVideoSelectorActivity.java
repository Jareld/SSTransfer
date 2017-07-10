package com.example.jareld.samsontransferclient.activity;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.jareld.samsontransferclient.R;
import com.example.jareld.samsontransferclient.adapter.PicVideoAdpter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class PicVideoSelectorActivity
        extends AppCompatActivity
        implements View.OnClickListener
{

    private static final String TAG = "PicVideo";
    private RecyclerView   mRecyclerView;
    private PicVideoAdpter mPicVideoAdpter;
    private Button         mBtn_confirm;
    private TextView mTv_send_info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_pic_video_selector);
        initView();
        initData();
        initEvent();
    }


    private void initEvent() {
        mBtn_confirm.setOnClickListener(this);
    }

    private void initData() {
        Intent intent = getIntent();
        ArrayList<String> pics = intent.getStringArrayListExtra("pics");
        ArrayList<String> videos = intent.getStringArrayListExtra("videos");
        //获取到数据之后添加到一个结合里面进行排序
        ArrayList<String>  pic_video_paths = new ArrayList<>();
        for(int i = 0 ; i < pics.size() ; i++){
            pic_video_paths.add(pics.get(i));
        }
        for(int i = 0 ; i < videos.size() ; i++){

            pic_video_paths.add(videos.get(i));
        }

        ArrayList<File> arrayList = new ArrayList<>();
        for(int i = 0 ; i < pic_video_paths.size(); i++){
            File file = new File(pic_video_paths.get(i));
            arrayList.add(file);
        }
        Collections.sort(arrayList, new FileComparator());
        mPicVideoAdpter = new PicVideoAdpter(this , arrayList);
        mRecyclerView.setAdapter(mPicVideoAdpter);



    }

    private void initView() {


        mRecyclerView = (RecyclerView) findViewById(R.id.select_recyclerView);
        mBtn_confirm = (Button) findViewById(R.id.btn_confirm);
        mTv_send_info = (TextView) findViewById(R.id.tv_send_file);
        GridLayoutManager manager      =new GridLayoutManager(this, 4,GridLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(manager);

    }





    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_confirm:
                ArrayList<String> selectedPV = mPicVideoAdpter.getSelectedPV();
                if(selectedPV.size()==0){
                    Log.d(TAG, "onClick: 没有一个");
                }else{

                    Intent intent = new Intent();
                    intent.putExtra("selected_pv" , selectedPV);
                    setResult(20 , intent);
                    finish();

                }

                break;
            default:
                break;
        }

    }

    private class FileComparator
            implements Comparator<File>
    {
        @Override
        public int compare(File file, File t1) {
            if (file.lastModified() >= t1.lastModified()) {
                return -1;
            } else {
                return 1;
            }
        }

    }
    public class SpaceItemDecoration extends RecyclerView.ItemDecoration {
        int mSpace ;

        /**
         * @param space 传入的值，其单位视为dp
         */
        public SpaceItemDecoration(int space) {
            this.mSpace = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int itemCount = mPicVideoAdpter.getItemCount();
            int pos = parent.getChildAdapterPosition(view);

            outRect.left = 0;
            outRect.top = 0;
            outRect.bottom = 0;


            if (pos != (itemCount -1)) {
                outRect.right = mSpace;
            } else {
                outRect.right = 0;
            }
        }
    }

    public void setSendInfo(String text){
        //中文 发送文件
       // mTv_send_info.setText("Send FiLE" + text);
        mTv_send_info.setText("发送文件" + text);
    }
}
