package com.example.jareld.samsontransferclient;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

public class MainActivity
        extends AppCompatActivity
{
    private static final int STARTACTIVITY             = 1;
    private static final int MY_WRITE_EXTERNAL_STORAGE = 2;
    private ImageView mIv_logo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);
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
        AnimationSet animationSet = new AnimationSet(false);

        RotateAnimation rotateAnimation =new RotateAnimation(0 , 360 ,
                                                             Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setDuration(2000);

        animationSet.addAnimation(rotateAnimation);
        ScaleAnimation scaleAnimation = new ScaleAnimation(0 , 1 , 0 , 1 , Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(2000);

        animationSet.addAnimation(scaleAnimation);
        animationSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {


                mHandler.sendEmptyMessageDelayed(STARTACTIVITY , 1000);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mIv_logo.startAnimation(animationSet);
    }


    private void initEvent() {

    }
}
