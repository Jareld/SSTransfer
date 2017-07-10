package com.example.jareld.samsontransferclient.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.example.jareld.samsontransferclient.R;
import com.example.jareld.samsontransferclient.customview.MyAnimationDrawable;


public class MainActivity
        extends AppCompatActivity
{
    private static final int STARTACTIVITY             = 1;
    private static final int MY_WRITE_EXTERNAL_STORAGE = 2;
    private ImageView mIv_logo;
    private View mLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        getWindow().getDecorView()
                   .setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        mLayout = getLayoutInflater().from(this)
                                     .inflate(R.layout.activity_main, null);
        this.getWindow()
            .setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                      WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(mLayout);
        initView();
        initData();
        initEvent();
    }
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STARTACTIVITY:
                    Intent intent = new Intent(MainActivity.this, TransferActivity.class);
                    startActivity(intent);
                    finish();

                    break;
            }

            super.handleMessage(msg);
        }
    };

    private void initView() {
        mIv_logo = (ImageView) findViewById(R.id.iv_logo);

    }

    private void initData() {
//        AnimationSet animationSet = new AnimationSet(false);
//
//        RotateAnimation rotateAnimation =new RotateAnimation(0 , 360 ,
//                                                             Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
//        rotateAnimation.setDuration(2000);
//
//        animationSet.addAnimation(rotateAnimation);
//        ScaleAnimation scaleAnimation = new ScaleAnimation(0 , 1 , 0 , 1 , Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
//        scaleAnimation.setDuration(2000);
//
//        animationSet.addAnimation(scaleAnimation);
//        animationSet.setAnimationListener(new Animation.AnimationListener() {
//            @Override
//            public void onAnimationStart(Animation animation) {
//
//            }
//
//            @Override
//            public void onAnimationEnd(Animation animation) {
//
//
//                mHandler.sendEmptyMessageDelayed(STARTACTIVITY , 1000);
//            }
//
//            @Override
//            public void onAnimationRepeat(Animation animation) {
//
//            }
//        });
        MyAnimationDrawable.animateRawManuallyFromXML(R.drawable.logo_animlist,
                                                      mIv_logo, new Runnable() {

                    @Override
                    public void run() {
                        // TODO onStart
                        // 动画开始时回调

                    }
                }, new Runnable() {

                    @Override
                    public void run() {
                        // TODO onComplete
                     mHandler.sendEmptyMessageDelayed(STARTACTIVITY ,1500);

                    }
                });


    }


    private void initEvent() {

    }
}
