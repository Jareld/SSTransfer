package com.example.jareld.samsontransferclient;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

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
    private long mLength;


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
            String fileUri = intent.getExtras()
                                   .getString(EXTRAS_FILE_PATH);

            String host = intent.getExtras()
                                .getString(EXTRAS_GROUP_OWNER_ADDRESS);
            String realPath = intent.getExtras()
                                    .getString(REAL_FILE_PATH);

            String beClickName = intent.getExtras()
                                       .getString(BE_CLICKED_DEVICE_NAME);
            Socket socket = new Socket();
            try {
                socket.setReuseAddress(true);
            } catch (SocketException e) {
                e.printStackTrace();
            }
            int port = intent.getExtras()
                             .getInt(EXTRAS_GROUP_OWNER_PORT);

            try {
                socket.setSendBufferSize(1024 * 1024 );
                socket.bind(null);
                InetSocketAddress inetSocketAddress = new InetSocketAddress(host, 10086);
                socket.connect(inetSocketAddress, SOCKET_TIMEOUT);

                LogUtils.logInfo(TAG,
                                 "onHandleIntent",
                                 "socket.getSendBufferSize()=" + socket.getSendBufferSize() + "socket.getReceiveBufferSize()=" + socket.getReceiveBufferSize());
                // socket.setSendBufferSize(1024*1024);
                /*returns an output stream to write data into this socket*/

                OutputStream stream = socket.getOutputStream();



                ContentResolver cr = context.getContentResolver();
                InputStream     is = null;
                try {
                    File file = new File(Uri.parse(fileUri)
                                            .toString());

                    mLength = file.length();



                    RxBus.getInstance()
                         .post(new UserEvent(mLength, "before"));

                    is = cr.openInputStream(Uri.parse(fileUri));
                    int available = is.available();
                    int xiegang         = realPath.lastIndexOf("/");
                    realPath = realPath.substring(xiegang + 1);
                    stream.write(("<filepath>" +available+"-=-="+ realPath  + "<//filepath>").getBytes());
                } catch (FileNotFoundException e) {
                    Log.d("xyz", e.toString());
                    LogUtils.logException(TAG, "在客戶端發送打開文件的時候出錯了" + e.toString());
                }
                byte buf[] = new byte[1024 * 1024 ];
                FileServerAsyncTask.copyFileClient(is, stream, buf ,socket , mLength);
                Log.d("xyz", "Client: Data written");
            } catch (IOException e) {
                Log.e("xyz", e.getMessage());
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

}
