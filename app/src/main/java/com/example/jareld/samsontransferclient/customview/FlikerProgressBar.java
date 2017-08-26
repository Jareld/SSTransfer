package com.example.jareld.samsontransferclient.customview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.example.jareld.samsontransferclient.R ;



/*
 *  @项目名：  TestSocket 
 *  @包名：    com.example.jareld.testsocket
 *  @文件名:   FlikerProgressBar
 *  @创建者:   LYC2
 *  @创建时间:  2017/4/14 17:26
 *  @描述：    TODO
 */
public class FlikerProgressBar
        extends View
        implements Runnable

{
    private static final String             TAG      = "FlikerProgressBar";
    private              PorterDuffXfermode xfermode = new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP);

    private int DEFAULT_HEIGHT_DP = 35;

    private int borderWidth;
    private int needWidth  = 0;
    private int needHeight = 0;

    private float maxProgress = 100f;

    private Paint textPaint;

    private Paint bgPaint;

    private Paint pgPaint;

    private String progressText;

    private Rect textRect;

    private RectF bgRectf;

    /**
     * 左右来回移动的滑块
     */
    private Bitmap flikerBitmap;

    /**
     * 滑块移动最左边位置，作用是控制移动
     */
    private float flickerLeft;

    /**
     * 进度条 bitmap ，包含滑块
     */
    private Bitmap pgBitmap;

    private Canvas pgCanvas;

    /**
     * 当前进度
     */
    private float progress;

    private boolean isFinish;

    private boolean isStop;

    /**
     * 下载中颜色
     */
    private int loadingColor;

    /**
     * 暂停时颜色
     */
    private int stopColor;

    /**
     * 进度文本、边框、进度条颜色
     */
    private int progressColor;

    private int textSize;

    private int radius;

    private Thread thread;

    BitmapShader bitmapShader;
    private int mInitWidth = 0;

    public FlikerProgressBar(Context context) {
        this(context, null, 0);
    }

    public FlikerProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlikerProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Log.d(TAG, "FlikerProgressBar: 走这个生命周期");
        initAttrs(attrs);
    }

    private void initAttrs(AttributeSet attrs) {
        TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.FlikerProgressBar);
        try {
            textSize = (int) ta.getDimension(R.styleable.FlikerProgressBar_textSize, 12);
            loadingColor = ta.getColor(R.styleable.FlikerProgressBar_loadingColor,
                                       Color.parseColor("#40c4ff"));
            stopColor = ta.getColor(R.styleable.FlikerProgressBar_stopColor,
                                    Color.parseColor("#ff9800"));
            radius = (int) ta.getDimension(R.styleable.FlikerProgressBar_radius, 0);
            borderWidth = (int) ta.getDimension(R.styleable.FlikerProgressBar_borderWidth, 1);
        } finally {
            ta.recycle();
        }
    }

    private void init() {
        Log.d(TAG, "init: yixia");
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        bgPaint.setStyle(Paint.Style.STROKE);
        bgPaint.setStrokeWidth(borderWidth);

        pgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pgPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(textSize);

        textRect = new Rect();
        bgRectf = new RectF(borderWidth,
                            borderWidth,
                            getMeasuredWidth() - borderWidth,
                            getMeasuredHeight() - borderWidth);

        if (isStop) {
            progressColor = stopColor;
        } else {
            progressColor = loadingColor;
        }

        flikerBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.flicker);
        mInitWidth = -flikerBitmap.getWidth();
        flickerLeft = mInitWidth;

        initPgBimap();
    }


    private void initPgBimap() {
        Log.d(TAG, "initPgBimap: 赋值之前的" +needWidth +"::" +needHeight);
        if (needWidth == 0 && needHeight == 0) {
            needWidth = getMeasuredWidth() - borderWidth;
            needHeight = getMeasuredHeight() - borderWidth;
        }

        if(needWidth == 536 && needHeight == 48){
            needWidth = getMeasuredWidth() - borderWidth;
            needHeight = getMeasuredHeight() - borderWidth;
        }

        if(needHeight<= 0 || needWidth <=0){
             needWidth = 536;
             needHeight = 48;
        }
        Log.d(TAG, "initPgBimap: needWidth" +needWidth +"::" +needHeight);
        pgBitmap = Bitmap.createBitmap(needWidth, needHeight, Bitmap.Config.ARGB_8888);
        pgCanvas = new Canvas(pgBitmap);
        thread = new Thread(this);
        thread.start();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.d(TAG, "onMeasure: ");
        int widthSpecSize  = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        int height         = 0;
        switch (heightSpecMode) {
            case MeasureSpec.AT_MOST:
                height = dp2px(DEFAULT_HEIGHT_DP);
                break;
            case MeasureSpec.EXACTLY:
            case MeasureSpec.UNSPECIFIED:
                height = heightSpecSize;
                break;
        }
        setMeasuredDimension(widthSpecSize, height);

        if (pgBitmap == null) {
            init();
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //背景
        drawBackGround(canvas);

        //进度
        drawProgress(canvas);

        //进度text
        drawProgressText(canvas);

        //变色处理
        drawColorProgressText(canvas);
    }

    /**
     * 边框
     * @param canvas
     */
    private void drawBackGround(Canvas canvas) {
        bgPaint.setColor(progressColor);
        //left、top、right、bottom不要贴着控件边，否则border只有一半绘制在控件内,导致圆角处线条显粗
        canvas.drawRoundRect(bgRectf, radius, radius, bgPaint);
    }

    /**
     * 进度
     */
    private void drawProgress(Canvas canvas) {
        pgPaint.setColor(progressColor);

        float right = (progress / maxProgress) * getMeasuredWidth();
        pgCanvas.save(Canvas.CLIP_SAVE_FLAG);
        pgCanvas.clipRect(0, 0, right, getMeasuredHeight());
        pgCanvas.drawColor(progressColor);
        pgCanvas.restore();

        if (!isStop) {
            pgPaint.setXfermode(xfermode);
            pgCanvas.drawBitmap(flikerBitmap, flickerLeft, 0, pgPaint);
            pgPaint.setXfermode(null);
        }

        //控制显示区域
        bitmapShader = new BitmapShader(pgBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        pgPaint.setShader(bitmapShader);
        canvas.drawRoundRect(bgRectf, radius, radius, pgPaint);
    }

    /**
     * 进度提示文本
     * @param canvas
     */
    private void drawProgressText(Canvas canvas) {
        textPaint.setColor(progressColor);
        progressText = getProgressText();
        textPaint.getTextBounds(progressText, 0, progressText.length(), textRect);
        int   tWidth      = textRect.width();
        int   tHeight     = textRect.height();
        float xCoordinate = (getMeasuredWidth() - tWidth) / 2;
        float yCoordinate = (getMeasuredHeight() + tHeight) / 2;
        canvas.drawText(progressText, xCoordinate, yCoordinate, textPaint);
    }

    /**
     * 变色处理
     * @param canvas
     */
    private void drawColorProgressText(Canvas canvas) {
        textPaint.setColor(Color.WHITE);
        int   tWidth        = textRect.width();
        int   tHeight       = textRect.height();
        float xCoordinate   = (getMeasuredWidth() - tWidth) / 2;
        float yCoordinate   = (getMeasuredHeight() + tHeight) / 2;
        float progressWidth = (progress / maxProgress) * getMeasuredWidth();
        if (progressWidth > xCoordinate) {
            canvas.save(Canvas.CLIP_SAVE_FLAG);
            float right = Math.min(progressWidth, xCoordinate + tWidth * 1.1f);
            canvas.clipRect(xCoordinate, 0, right, getMeasuredHeight());
            canvas.drawText(progressText, xCoordinate, yCoordinate, textPaint);
            canvas.restore();
        }
    }

    public void setProgress(float progress) {
        if (!isStop) {
            if (progress < maxProgress) {
                this.progress = progress;
            } else {
                this.progress = maxProgress;
                finishLoad();
            }
            invalidate();
        }
    }

    public void setStop(boolean stop) {
        isStop = stop;
        if (isStop) {
            progressColor = stopColor;
            if (thread != null) {
                boolean isSleep = true;
                while (isSleep){
                    Thread.State state = thread.getState();
                    if(state == Thread.State.TIMED_WAITING){
                    }else{
                        thread.interrupt();
                        isSleep = false;
                    }

                }
            }
        } else {
            progressColor = loadingColor;
            thread = new Thread(this);
            thread.start();
        }
        invalidate();
    }

    public void finishLoad() {
        isFinish = true;
        if (!isStop) {
            setStop(true);
        }
    }

    public void toggle() {
        if (!isFinish) {
            if (isStop) {
                setStop(false);
            } else {
                setStop(true);
            }
        }
    }

    @Override
    public void run() {
        int width = flikerBitmap.getWidth();
        try {
            while (!isStop && !thread.isInterrupted()) {
                flickerLeft += dp2px(5);
                float progressWidth = (progress / maxProgress) * getMeasuredWidth();
                if (flickerLeft >= progressWidth) {
                    flickerLeft = -width;
                }
                postInvalidate();
                 if(!thread.isInterrupted()){
                Thread.sleep(20);

                 }


            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 重置
     */
    public void reset() {
        Log.d(TAG, "reset: 进入reset模式");
        setStop(true);
        progress = 0;
        isFinish = false;
        isStop = false;
        progressColor = loadingColor;
        progressText = "";
        if (mInitWidth != 0) {
            flickerLeft = mInitWidth;
        } else if (flikerBitmap != null) {
            flickerLeft = -flikerBitmap.getWidth();
        } else {
            flikerBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.flicker);
            mInitWidth = -flikerBitmap.getWidth();
        }
        initPgBimap();
    }

    public float getProgress() {
        return progress;
    }

    public boolean isStop() {
        return isStop;
    }

    public boolean isFinish() {
        return isFinish;
    }

    private String getProgressText() {
        String text = "";
        if (!isFinish) {
            if (!isStop) {
                if(progress == 0.0f){
                    //中文   等待发送文件
                    //   text = "Waiting to send file";
                   text = "等待发送文件";
                } else if (progress == 0.1f){
                    //中文   启动文件传输
                    //   text = "Start file transfer...";
                   text = "启动文件传输...";
                }else {
                     //中文  传输中
                    //             text = "Transfering" + progress + "%";
                   text = "传输中" + progress + "%";

                }  } else {
                //中文  继续
                //             text = "Continue";
               text = "继续";
            }
        } else {
            //中文 传输完成
            //        text = "Transmission is complete";
          text = "传输完成";
        }

        return text;
    }

    private int dp2px(int dp) {
        float density = getContext().getResources()
                                    .getDisplayMetrics().density;
        return (int) (dp * density);
    }
}
