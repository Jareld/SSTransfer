package com.example.jareld.samsontransferclient.wifip2p;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.example.jareld.samsontransferclient.utils.LogUtils;
import com.example.jareld.samsontransferclient.utils.RxBus;
import com.example.jareld.samsontransferclient.utils.UserEvent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

/*
 *  @项目名：  TestWifiDerect 
 *  @包名：    com.example.lyc2.testwifiderect.service
 *  @文件名:   FileTransferService
 *  @创建者:   LYC2
 *  @创建时间:  2016/11/25 11:13
 *  @描述：    TODO
 */
public class FileTransferService
        extends IntentService
{
    private static final String TAG                        = "FileTransferService";
    private static final int    SOCKET_TIMEOUT             = 5000;
    public static final  String ACTION_SEND_FILE           = "com.example.android.wifidirect.SEND_FILE";
    public static final  String EXTRAS_FILE_PATH           = "sf_file_url";
    public static final  String REAL_FILE_PATH             = "sf_file_real_path";
    public static final  String BE_CLICKED_DEVICE_NAME     = "be_clicked_device_name";
    public static final  String EXTRAS_GROUP_OWNER_ADDRESS = "sf_go_host";
    public static final  String EXTRAS_GROUP_OWNER_PORT    = "sf_go_port";
    private              long   mLength                    = 0;
    private long              mAvailable;
    private ArrayList<String> mStringArrayListExtra;
    public long hasSendFileLength = 0;

    public FileTransferService() {
        super("FileTransferService");
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *

     * @param name Used to name the worker thread, important only for debugging.
     */
    public FileTransferService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {


        LogUtils.logInfo(TAG, "HY测试", "FileTransferService：onHandleIntent: ");
        Context context = getApplicationContext();
        if (intent.getAction()
                  .equals(ACTION_SEND_FILE))
        {


            String host = intent.getExtras()
                                .getString(EXTRAS_GROUP_OWNER_ADDRESS);
            int port = intent.getExtras()
                             .getInt(EXTRAS_GROUP_OWNER_PORT);
            mStringArrayListExtra = intent.getStringArrayListExtra(REAL_FILE_PATH);

            Socket socket = new Socket();
            try {
                socket.setReuseAddress(true);
            } catch (SocketException e) {
                e.printStackTrace();
            }


            try {
                socket.setSendBufferSize(1024 * 1024);
                socket.bind(null);
                InetSocketAddress inetSocketAddress = new InetSocketAddress(host, port);
                socket.connect(inetSocketAddress, SOCKET_TIMEOUT);

                LogUtils.logInfo(TAG,
                                 "onHandleIntent",
                                 "socket.getSendBufferSize()=" + socket.getSendBufferSize() + "socket.getReceiveBufferSize()=" + socket.getReceiveBufferSize());

                OutputStream stream = socket.getOutputStream();

                ContentResolver cr = context.getContentResolver();
                InputStream     is = null;
                try {
                    for (int i = 0; i < mStringArrayListExtra.size(); i++) {
                        File file = new File(mStringArrayListExtra.get(i));
                        mLength += file.length();
                    }
                    RxBus.getInstance()
                         .post(new UserEvent(mLength, "before"));
                    //换一种 模式  先看要传输几个文件，每个文件的大小和文件名
                    String realPath;
                    String info  = "";
                    byte   buf[] = new byte[1024 * 1024];
                    for (int i = 0; i < mStringArrayListExtra.size(); i++) {
                        File file   = new File(mStringArrayListExtra.get(i));
                        long length = file.length();
                        int xiegang = mStringArrayListExtra.get(i)
                                                           .lastIndexOf("/");
                        realPath = mStringArrayListExtra.get(i)
                                                        .substring(xiegang + 1);
                        if (i == mStringArrayListExtra.size() - 1) {
                            info += length + "-=-=" + realPath;
                            info = "<filepath>" + mStringArrayListExtra.size() + "-=-=" + info + "<//filepath>";
                        } else {
                            info += length + "-=-=" + realPath + "-=-=";
                        }
                        //                        stream.write(("<filepath>" + length + "-=-=" + realPath + "<//filepath>").getBytes());
                        //                        copyOneFile(is, stream, buf, socket, mLength);
                        //                        SystemClock.sleep(100);
                        //                        Log.d(TAG,
                        //                              "onHandleIntent: 复制传输的次数" + length + "::" + realPath + "::" + mLength + "::" + hasSendFileLength);
                    }
                    stream.write(info.getBytes());
                    for(int i = 0 ; i < mStringArrayListExtra.size(); i++){
                        File file   = new File(mStringArrayListExtra.get(i));
                        is = cr.openInputStream(Uri.fromFile(file));
                        copyOneFile(is, stream, buf, socket, mLength);
                    }

                    Log.d(TAG, "onHandleIntent: info = " + info);
                    RxBus.getInstance()
                         .post(new UserEvent((mLength / 1024 / 1024), "after"));
                    stream.close();
                    socket.close();


                } catch (Exception e) {
                    Log.d("xyz", e.toString());
                    LogUtils.logException(TAG, "在客戶端發送打開文件的時候出錯了" + e.toString());
                }
                // copyFileClient(is, stream, buf , socket , mLength);
            } catch (IOException e) {

                LogUtils.logException(TAG, "在客戶端的時候傳輸寫入的時候可能出現的錯誤 IOEx的錯誤" + e.toString());
                UserEvent userEvent = new UserEvent(0, "connect_fail");
                RxBus.getInstance()
                     .post(userEvent);

            } finally {
                Log.d(TAG, "onHandleIntent: finally");
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            Log.d(TAG, "onHandleIntent: 关闭");
                            socket.close();
                            //客户端发送信息
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }

        }
    }

    public void copyOneFile(InputStream inputStream,
                            OutputStream out,
                            byte[] buf,
                            Socket socket,
                            long fileLength)
    {
        int len;

        long      fileMB    = fileLength / 1024;
        UserEvent userEvent = new UserEvent(0, "doing");
        userEvent.setFileLengthMB(fileMB);
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
                out.flush();
                hasSendFileLength += len;
                userEvent.setProgress(hasSendFileLength / 1024);
                RxBus.getInstance()
                     .post(userEvent);
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public boolean copyFileClient(InputStream inputStream,
                                  OutputStream out,
                                  byte[] buf,
                                  Socket socket,
                                  long fileLength)
    {
        int  len;
        long length = 0;
        try {
            long available = inputStream.available();
            long fileMB    = (available / 1024);
            if (fileMB == 0) {
                fileMB = fileLength / 1024;
            }
            UserEvent userEvent = new UserEvent(0, "doing");
            userEvent.setFileLengthMB(fileMB);
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
                //输出端要fluch
                //输入端不要flush
                out.flush();
                length += len;
                userEvent.setProgress(length / 1024);
                RxBus.getInstance()
                     .post(userEvent);
                LogUtils.logInfo(TAG, "HYcopyFile: ", "百分比" + length + "::" + fileLength);
            }
            Log.d(TAG, "copyFileClient: 百分比之后");
            RxBus.getInstance()
                 .post(new UserEvent((length / 1024 / 1024), "after"));

            out.close();
            inputStream.close();
            socket.close();

        } catch (IOException e) {
            LogUtils.logException(TAG, "这里是客户端 不断写入的时候出现的错误" + e.toString());
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return false;
        }

        return true;
    }
}
