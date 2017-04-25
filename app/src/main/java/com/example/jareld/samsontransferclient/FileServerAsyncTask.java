package com.example.jareld.samsontransferclient;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/*
 *  @项目名：  TestWifiDerect 
 *  @包名：    com.example.lyc2.testwifiderect.task
 *  @文件名:   FileServerAsyncTask
 *  @创建者:   LYC2
 *  @创建时间:  2016/11/25 11:40
 *  @描述：    TODO
 */
public class FileServerAsyncTask
        extends AsyncTask<Void, Void, String>
{
    private static final String TAG = "FileServerAsyncTask";


    private        Socket mClient;
    private static String substring;
    private static String nPath;

    /*
       * (non-Javadoc)
       *
       * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
       */

    @Override
    protected void onPostExecute(String result) {

    }

    /*
     * (non-Javadoc)
     *
     * @see android.os.AsyncTask#onPreExecute()
     */
    @Override
    protected void onPreExecute() {

    }

    @Override
    protected String doInBackground(Void... params) {
        ServerSocket serverSocket = null;
        try {

            //开始前的时间1480063883631
            Log.i("xyz", "file doinback");

            serverSocket = new ServerSocket(10086);
            serverSocket.setReceiveBufferSize(1024 * 1024 * 10);
            serverSocket.setReuseAddress(true);
            UserEvent userEvent = new UserEvent(0, "waitingConnect");
            RxBus.getInstance()
                 .post(userEvent);

            Log.i("xyz", "file doinback serverSocket.accept() 之前");

            mClient = serverSocket.accept();
            Log.i("xyz", "file doinback serverSocket.accept() 之后");

            mClient.setReuseAddress(true);
            mClient.setReceiveBufferSize(1024 * 1024 * 10);
            LogUtils.logInfo(TAG,
                             "doInBackground",
                             "serverSocket.getReceiveBufferSize()=" + serverSocket.getReceiveBufferSize() +
                                     "client.getReceiveBufferSize()=" +
                                     mClient.getReceiveBufferSize() +
                                     "client.getSendBufferSize()=" + mClient.getSendBufferSize());




                /*Returns an input stream to read data from this socket*/
            InputStream inputstream = mClient.getInputStream();

            LogUtils.logInfo(TAG, "doInBackground", "开始前的时间" + +System.currentTimeMillis());

            byte buf[] = new byte[1024 * 1024];

            //服务器接受数据
            String filePath = copyFileServer(inputstream, null, buf);


            LogUtils.logInfo(TAG,
                             "doInBackground",
                             "结束后的时间" + System.currentTimeMillis() + "::" + nPath);


            //开始前的时间1480059430591
            //结束后前的时1480059694635
            //264044    4.4fenzhong
            //401635803  383mb


            try {
                Log.d(TAG, "doInBackground: 进入关闭之前");
                if(serverSocket!=null && !serverSocket.isClosed()){
                    Log.d(TAG, "doInBackground:看有没有关");
                    serverSocket.close();}
                if(mClient!=null && !mClient.isClosed()){
                    //看有没有关
                    mClient.close();}
            } catch (IOException e) {

                e.printStackTrace();
            }
            //告诉服务器接受完成  并且 重新开启一次接受任务
            userEvent.setName("hasReceived");
            userEvent.setFileName(filePath);
            RxBus.getInstance()
                 .post(userEvent);
            return filePath;

        } catch (IOException e) {
            LogUtils.logException(TAG, "在服務端接受数据的时候可能导致的IO错误" + e.toString());
            return null;
        }finally {
            Log.d(TAG, "doInBackground: 最终样式");
        }

    }

    public static boolean   copyFileClient(InputStream inputStream, OutputStream out, byte[] buf ,Socket socket , long fileLength) {
        int  len;
        long length = 0;
        try {
            long available = inputStream.available();
            //1480396906677开始时间
            //1480396972009   401635803
            long fileMB =   ( available / 1024 );
            if(fileMB == 0){
                fileMB = fileLength / 1024 ;
            }
            Log.d(TAG, "copyFileClient: 到了transfer_file_start之前" +fileMB);
            RxBus.getInstance().post(new UserEvent(fileMB , "transfer_file_start"));
            UserEvent userEvent = new UserEvent(0, "doing");
            userEvent.setFileLengthMB(fileMB);
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
                //输出端要fluch
                //输入端不要flush
                out.flush();
                length += len;
                userEvent.setProgress(length / 1024 );
                RxBus.getInstance()
                     .post(userEvent);

                LogUtils.logInfo(TAG, "HYcopyFile: ", "百分比" + length + "::" +fileLength);

            }
            Log.d(TAG, "copyFileClient: 百分比之后");
            RxBus.getInstance()
                 .post(new UserEvent((length/1024/1024), "after"));

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

    public  String copyFileServer(InputStream inputStream, OutputStream out, byte[] buf) {


        int  len;
        long length = 0;
        RxBus.getInstance()
             .post(new UserEvent(0, "serverReceiving"));
        try {
            //1480396906677开始时间
            //1480396972009   401635803

            //在第一次的时候完成切割
            long   fileTime = System.currentTimeMillis();
            String sFile    = String.valueOf(fileTime);
            String path     = Environment.getExternalStorageDirectory() + "/" + "VrecxPlayer/Wifi-Tranfer/";
            File   f        = null;





                if ((len = inputStream.read(buf)) != -1) {

                    String filepath = new String(buf , "UTF-8");

                    int    first    = filepath.indexOf("<filepath>");

                    int    last     = filepath.indexOf("<//filepath>");




                    Log.d(TAG, "copyFileServer: " + first + "::" + last +"::"+ len+"::"+filepath);
                    substring = filepath.substring(first + 10, last);
                    Log.d(TAG, "copyFileServer: "+substring+ "::FILEPATH");


                    nPath = "WFD-" + substring;


                    LogUtils.logInfo(TAG,
                                     "copyFileServer",
                                     "first=" + first + "last" + last + "substring" + "::" + nPath);

                    f = new File(path + nPath);
                    if (f.exists()) {
                        //如果已经存在了
                        //那么就会出现同名的情况，就要做修改
                        f = handleSameName(path, nPath, 1);

                    } else {
                        File dirs = new File(f.getParent());

                        if (!dirs.exists()) { dirs.mkdirs(); }

                        f.createNewFile();
                    }


                    out = new FileOutputStream(f);
                    Log.d(TAG, "copyFileServer: 穿件out");
                if((len - last - 12 ) == 0){
                    Log.d(TAG, "copyFileServer: 如果刚刚好一波是名字");
                }else{
                    byte[]  newByTE = new byte[ (len - last - 12)];

                    for( int i = 0 ; i <(len - last - 12) ; i++){
                        newByTE[ i ] = buf [ (last + 12 + i) ];
                    }
//                    String shengyuString = filepath.substring(last + 12, len);
//                    byte[] bytes         = shengyuString.getBytes("UTF-8");
                    // out.write(buf , last+12 , len);
                  // byte[] bytes         = shengyuString.getBytes();
                   out.write(newByTE , 0 , (len - last - 12));
                    out.flush();
                }
                    Log.d(TAG, "copyFileServer: if判断完成");
                }

              //  LogUtils.logInfo(TAG, "HYcopyFile:服务器接受 ", "百分比" + length + "::" + len + "::");
                int changdu = 0;
                while ((len = inputStream.read(buf)) != -1) {
                    out.write(buf, 0, len);
                     changdu += len;
                    LogUtils.prinfLog("changdu = "+ changdu);
                    out.flush();
                }

            LogUtils.prinfLog("changdu = "+ changdu);
            LogUtils.logInfo(TAG, "获取到传过来的文件的文件名", substring);
            out.close();
            inputStream.close();
        } catch (IOException e) {
            LogUtils.logException(TAG, "这里是服务端 不断写入的时候出现的错误" + e.toString());
            return "";
        } return nPath;
    }

    private static File handleSameName(String dirPath, String filePath, int i) {
        //说明nPath已经存在了
        Log.d(TAG, "handleSameName: " + filePath);
        String[] split = filePath.split("\\.");
        Log.d(TAG, "handleSameName: " + split.length + split[0]);
        String filePathPre = split[0] + "(" + i + ")";
        String newFilePath = filePathPre + "." + split[1];
        File   file        = new File(dirPath + newFilePath);
        if (file.exists()) {
            //如果文件已经存在 那么就要 继续调用下一个方法
            i++;
            return handleSameName(dirPath, filePath, i);
        } else {
            //如果不存在  说明是可以的
            try {
                //创建一个file
                file.createNewFile();
                nPath = newFilePath;
                return file;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;

    }

}