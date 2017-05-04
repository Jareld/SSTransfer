package com.example.jareld.samsontransferclient.utils;

/*
 *  @项目名：  TestWifiDerect 
 *  @包名：    com.example.lyc2.testwifiderect.utils
 *  @文件名:   UserEvent
 *  @创建者:   LYC2
 *  @创建时间:  2016/12/5 10:48
 *  @描述：    TODO
 */
public class UserEvent {
    private static final String TAG = "UserEvent";

    private String name;
    private long   progress;
    private String fileName;

    public long getFileLengthMB() {
        return fileLengthMB;
    }

    public void setFileLengthMB(long fileLengthMB) {
        this.fileLengthMB = fileLengthMB;
    }

    private long fileLengthMB;
    public UserEvent(long l, String n) {
        name = n;
        progress = l;
    }

    public String getName() {
        return name;
    }

    public void setName(String n) {
        this.name = n;
    }

    public long getProgress() {
        return progress;
    }

    public void setProgress(long l) {
        this.progress = l;
    }

    public void setFileName(String fn) {
        this.fileName = fn;
    }
    public String getFileName(){
        return fileName;
    }
}
