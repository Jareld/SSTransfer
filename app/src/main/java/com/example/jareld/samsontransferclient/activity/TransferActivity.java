package com.example.jareld.samsontransferclient.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jareld.samsontransferclient.R;
import com.example.jareld.samsontransferclient.adapter.DeviceAdapter;
import com.example.jareld.samsontransferclient.customview.FlikerProgressBar;
import com.example.jareld.samsontransferclient.utils.LogUtils;
import com.example.jareld.samsontransferclient.utils.RxBus;
import com.example.jareld.samsontransferclient.utils.UserEvent;
import com.example.jareld.samsontransferclient.wifip2p.FileTransferService;
import com.example.jareld.samsontransferclient.wifip2p.WifiDerectBroadcastReceiver;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import rx.Subscription;
import rx.functions.Action1;

public class TransferActivity
        extends AppCompatActivity
        implements View.OnClickListener
{
android.hardware.Camera mCamera;

    private static final String TAG                      = "ClientActivity";
    private static final int    MY_READ_EXTERNAL_STORAGE = 100;
    private RecyclerView                          mRcyc_devices;
    private Button                                mBtn_stop_connect;
    private Button                                mBtn_search;
    private IntentFilter                          mFilter;
    private WifiP2pManager                        mManager;
    private WifiP2pManager.Channel                mChannel;
    private WifiDerectBroadcastReceiver           mReceiver;
    private WifiP2pManager.PeerListListener       mPeerListListener;
    private WifiP2pManager.ConnectionInfoListener mConnectionInfoListener;
    private ArrayList<HashMap<String, String>>    mPeerLists;
    private WifiP2pInfo                           mInfo;
    private Button                                mBtn_send_file;
    private boolean mIsConnect = false;
    private TextView mTv_trans_file;
    private ProgressDialog discoverProgressDialog   = null;
    private ProgressDialog connectingProgressDialog = null;
    private boolean        mIsFromCreateConnect     = false;
    private boolean        isServer                 = false;
    private static String                    mBeClickedDeviceName;
    private        Subscription              mSubscription;
    private        Collection<WifiP2pDevice> mDeviceList;
    private        String                    mBeTransferFileName;
    private boolean isDoing = false;
    private TextView mTv_connect_info;
    private Button   mBtn_connect_device;
    private int beSelectDevicePosition = -1;
    private DeviceAdapter mAdapter;
    private LinearLayout  mConnect_info_container;
    private TextView      mTv_connected_info_address;
    private TextView      mTv_connected_info_name;

    private FlikerProgressBar mFliker_pregress;
    private static final int     MISS_PROGRESS = 1;
    private              Handler mHandler      = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MISS_PROGRESS:
                    mFliker_pregress.setVisibility(View.GONE);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        getSupportActionBar().hide();
        initView();
        initData();
        initFilter();
        initReceiver();
        initEvent();
        initRxBus();
        checKPermission();
    }

    private void checKPermission() {


    }

    private void initData() {
        Log.d(TAG, "checKStoragePermission: ");
        if (ContextCompat.checkSelfPermission(this,
                                              Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                                              new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,},
                                              MY_READ_EXTERNAL_STORAGE);

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //处理回调
        switch (requestCode) {
            case MY_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {


                } else {
                    //这里是不允许的时候
                    finish();
                }
                break;
        }

    }

    private void initRxBus() {

        mSubscription = RxBus.getInstance()
                             .toObserverable(UserEvent.class)
                             .subscribe(new Action1<UserEvent>() {
                                 @Override
                                 public void call(UserEvent userEvent) {
                                     long fileLength = 0;
                                     switch (userEvent.getName()) {
                                         case "before":

                                             fileLength = userEvent.getProgress();
                                             final long finalFileLength = fileLength;
                                             handler.post(new Runnable() {
                                                 @Override
                                                 public void run() {
                                                     if (mTv_trans_file.getVisibility() == View.VISIBLE) {
                                                         mTv_trans_file.setVisibility(View.GONE);
                                                     }

                                                 }
                                             });
                                             break;
                                         case "after":
                                             final long fileleth = userEvent.getProgress();
                                             isDoing = false;
                                             handler.post(new Runnable() {
                                                 @Override
                                                 public void run() {
                                                     Log.d(TAG, "run: 通过after进入的reset模式");

                                                     if (mFliker_pregress.getVisibility() == View.VISIBLE) {

                                                         mFliker_pregress.reset();
                                                         mFliker_pregress.setVisibility(View.GONE);
                                                     }
                                                     if (mTv_trans_file.getVisibility() == View.GONE) {
                                                         mTv_trans_file.setVisibility(View.VISIBLE);
                                                          //中文 上一次文件传输完成，点击发送文件再进行文件传输
                                                 //        mTv_trans_file.setText(
                                                 //                "The last time the file transfer is complete, click to send the file and then transfer the file");
                                                         mTv_trans_file.setText(
                                                                 "上一次文件传输完成，点击发送文件再进行文件传输");
                                                     }

                                                     Log.d(TAG,
                                                           "run: mRcyc_devices" + (mRcyc_devices.getVisibility() == View.GONE));

                                                 }
                                             });


                                             break;
                                         case "doing":
                                             //  float per = (float) userEvent.getProgress() / (float) fileLength;
                                             final float finalPer = userEvent.getProgress();
                                             final long fileLengthMB = userEvent.getFileLengthMB();

                                             isDoing = true;
                                             handler.post(new Runnable() {
                                                 @Override
                                                 public void run() {
                                                     float      progress = (finalPer * 100 / fileLengthMB);
                                                     BigDecimal b        = new BigDecimal(progress);
                                                     progress = b.setScale(1,
                                                                           BigDecimal.ROUND_HALF_UP)
                                                                 .floatValue();
                                                     mFliker_pregress.setProgress(progress);

                                                 }
                                             });
                                             break;
                                         case "disconnect":
                                             if (connectingProgressDialog != null && connectingProgressDialog.isShowing()) {
                                                 connectingProgressDialog.dismiss();
                                             }
                                             if (mIsFromCreateConnect) {
                                                 Log.d(TAG, "call: 这里是来自客户端的申请连接  然后把圈圈给miss掉");
                                                 //中文   连接失败 , 请重新连接
//                                                 Toast.makeText(getApplicationContext(),
//                                                                "The connection failed, please reconnect",
//                                                                Toast.LENGTH_SHORT)
//                                                      .show();
                                                 Toast.makeText(getApplicationContext(),
                                                                "连接失败 , 请重新连接",
                                                                Toast.LENGTH_SHORT)
                                                      .show();
                                                 mIsFromCreateConnect = false;
                                             }
                                             isDoing = false;
                                             //中文 点击发送文件进行文件传输
                                           //  mTv_trans_file.setText("Click to send the file for file transfer");
                                             mTv_trans_file.setText("点击发送文件进行文件传输");
                                             mConnect_info_container.setVisibility(View.GONE);
                                             mRcyc_devices.setVisibility(View.VISIBLE);
                                             mIsConnect = false;
                                             Log.d(TAG, "call: 失去连接");
                                             beSelectDevicePosition = -1;
                                             break;
                                         case "connect_fail":
                                                //中文  启动文件传输失败 ，请重新发送文件
//                                             Toast.makeText(getApplicationContext(),
//                                                            "Start file transfer failed, please resend the file",
//                                                            Toast.LENGTH_SHORT)
//                                                  .show();
                                             Toast.makeText(getApplicationContext(),
                                                            "启动文件传输失败 ，请重新发送文件",
                                                            Toast.LENGTH_SHORT)
                                                  .show();
                                             isDoing = false;
                                             Log.d(TAG, "run: 通过connect_fail进入的reset模式");
                                             handler.post(new Runnable() {
                                                 @Override
                                                 public void run() {
                                                     mFliker_pregress.reset();

                                                 }
                                             });
                                             break;
                                     }

                                 }

                             });

    }

    private boolean isHandleFinished = true;

    private void initReceiver() {

        mManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mPeerLists = new ArrayList<HashMap<String, String>>();
        //这个是申请到列表后的回调
        mPeerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peers) {
                LogUtils.logInfo(TAG,
                                 "onPeersAvailable",
                                 "::" + peers.getDeviceList()
                                             .size());

                if (mPeerLists != null) {
                    mPeerLists.clear();
                }


                if (discoverProgressDialog != null && discoverProgressDialog.isShowing()) {
                    discoverProgressDialog.dismiss();
                }

                //这个是申请到列表后的回调
                mDeviceList = peers.getDeviceList();
                for (WifiP2pDevice wifiP2pDevice : mDeviceList) {

                    HashMap<String, String> map = new HashMap<>();

                    map.put("name", wifiP2pDevice.deviceName);

                    map.put("address", wifiP2pDevice.deviceAddress);

                    mPeerLists.add(map);
                }
                if (mPeerLists != null && mPeerLists.size() != 0) {
                    if (mIsConnect) {
                        //已经连接了
                    } else {
                        mTv_connect_info.setVisibility(View.VISIBLE);
                        mRcyc_devices.setVisibility(View.VISIBLE);
                        //  mTv_connect_info.setText("The device being searched for:");
                        mTv_connect_info.setText("搜索到设备:");

                        //中文  搜索到设备:
                    }
                } else if (mPeerLists != null && mPeerLists.size() == 0) {
                    mTv_connect_info.setVisibility(View.GONE);
                }
                if (mAdapter != null) {
                    ArrayList<HashMap<String, String>> list = mAdapter.getList();
                    if (list.equals(mPeerLists)) {
                        Log.d(TAG, "onPeersAvailable: 完全一样");
                    }
                }
                if (beSelectDevicePosition == -1) {
                    beSelectDevicePosition = 0;
                } else if (beSelectDevicePosition > mPeerLists.size() - 1) {
                    beSelectDevicePosition = 0;
                }
                mAdapter = new DeviceAdapter(mPeerLists,
                                             isServer,
                                             getResources(),
                                             beSelectDevicePosition);
                mRcyc_devices.setAdapter(mAdapter);
                Log.d(TAG, "onPeersAvailable:beSelectDevicePosition = " + beSelectDevicePosition);

                mRcyc_devices.setLayoutManager(new LinearLayoutManager(TransferActivity.this));
                mAdapter.setOnItemButtonClickConnectListener(new DeviceAdapter.ItemButtonClickConnectListener() {
                    @Override
                    public void onItemButtonClickConnectListener(int position) {
                        //条目被点击的时候  需要连接
                        //                        LogUtils.logInfo(TAG,
                        //                                         "onItemButtonClickConnectListener",
                        //                                         mPeerLists.get(position)
                        //                                                   .get("name") + mPeerLists.get(position)
                        //                                                                            .get("address"));
                        //
                        //                        mBeClickedDeviceName = mPeerLists.get(position)
                        //                                                         .get("name");
                        //
                        //
                        //                        createConnet(mPeerLists.get(position)
                        //                                               .get("name"),
                        //                                     mPeerLists.get(position)
                        //                                               .get("address"));
                        if (beSelectDevicePosition == -1) {
                            //说明第一次选
                            if (mAdapter.getIvArr() != null && mAdapter.getIvArr()
                                                                       .get(position) != null)
                            {
                                mAdapter.getIvArr()
                                        .get(position)
                                        .setImageBitmap(BitmapFactory.decodeResource(getResources(),
                                                                                     R.mipmap.dagou));
                                beSelectDevicePosition = position;
                            }
                        } else {
                            //把之前的干掉
                            if (mAdapter.getIvArr() != null && mAdapter.getIvArr()
                                                                       .get(beSelectDevicePosition) != null)
                            {
                                if (beSelectDevicePosition == position) {
                                    //默认不动
                                } else {
                                    mAdapter.getIvArr()
                                            .get(beSelectDevicePosition)
                                            .setImageBitmap(null);
                                    mAdapter.getIvArr()
                                            .get(position)
                                            .setImageBitmap(BitmapFactory.decodeResource(
                                                    getResources(),
                                                    R.mipmap.dagou));
                                    beSelectDevicePosition = position;
                                }
                            }
                            //重新复制
                        }
                    }
                });
            }
        };
      String taskA=  this.getApplicationInfo().taskAffinity;
        //这是申请连接后的回调
        mConnectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                //这是申请连接后的回调
                Log.d(TAG, "createConnet: 第三步 连接的回调 ");
                mInfo = info;
                LogUtils.logInfo(TAG,
                                 "onConnectionInfoAvailable",
                                 mInfo.groupOwnerAddress + "info.isGroupOwner" + info.isGroupOwner + "::");
                ///192.168.49.1info.isGroupOwner/flase
                //这是申请连接后的回调  说明已经连接上了

                if (info.groupFormed && info.isGroupOwner) {
                    //说明是服务端
                    LogUtils.logInfo(TAG, "onConnectionInfoAvailable", "说明是服务器  接受数据");

                } else if (info.groupFormed) {
                    //说明是客户端
                    LogUtils.logInfo(TAG,
                                     "onConnectionInfoAvailable",
                                     "说明是客户端  发送数据" + isHandleFinished);
                    Log.d(TAG, "createConnet: 第四部 说明是客户端 ");

                    if (connectingProgressDialog != null && connectingProgressDialog.isShowing()) {
                        connectingProgressDialog.dismiss();
                    }
                    Log.d(TAG, "createConnet: 第五步  让dialog消失掉");

                    if (mIsFromCreateConnect) {
                        Log.d(TAG, "createConnet: 第六步 进行一个判断");

                        isHandleFinished = false;
                        for (WifiP2pDevice wifiP2pDevice : mDeviceList) {
                            if (wifiP2pDevice.status == WifiP2pDevice.CONNECTED) {
                                Log.d(TAG,
                                      "onConnectionInfoAvailable: 这个wifidevice连接上了" + wifiP2pDevice.deviceName);
                                mConnect_info_container.setVisibility(View.VISIBLE);
                                mRcyc_devices.setVisibility(View.GONE);
                                //中文     设备名称：
                              //  mTv_connected_info_name.setText("Device name：" + wifiP2pDevice.deviceName);
                                mTv_connected_info_name.setText("设备名称：" + wifiP2pDevice.deviceName);
                                //中文     设备地址：
                               // mTv_connected_info_address.setText("Device address：" + wifiP2pDevice.deviceAddress);
                                mTv_connected_info_address.setText("设备地址：" + wifiP2pDevice.deviceAddress);
                                mIsConnect = true;
                                //中文  已经连接到设备：
                                //  mTv_connect_info.setText("Has been connected to the device：");
                                mTv_connect_info.setText("已经连接到设备：");

                                //中文 连接成功
//                                Toast.makeText(TransferActivity.this, "Connect Success", Toast.LENGTH_SHORT)
//                                     .show();
                                Toast.makeText(TransferActivity.this, "连接成功", Toast.LENGTH_SHORT)
                                     .show();
                                mIsFromCreateConnect = false;
                                isHandleFinished = true;
                                return;
                            }
                        }

                        //中文  ： 连接失败 ，请重新连接
//                        Toast.makeText(TransferActivity.this, "The connection failed, please reconnect", Toast.LENGTH_SHORT)
//                             .show();
                        Toast.makeText(TransferActivity.this, "连接失败 ，请重新连接", Toast.LENGTH_SHORT)
                             .show();

                        mIsFromCreateConnect = false;
                        isHandleFinished = true;
                    }

                    //发送一条信息过去
                    // TODO: 2016/12/15 等待一个发送信息的socket告诉 服务器 连接的是什么


                }


            }
        };

        mReceiver = new WifiDerectBroadcastReceiver(mManager,
                                                    mChannel,
                                                    this,
                                                    mPeerListListener,
                                                    mConnectionInfoListener);


    }

    private void createConnet(String name, final String address) {


        mIsFromCreateConnect = true;

        Log.d(TAG, "createConnet: 第一步进来");
        if (connectingProgressDialog != null && connectingProgressDialog.isShowing()) {
            connectingProgressDialog.dismiss();
        }
        //中文  连接设备   连接中 :
        connectingProgressDialog = ProgressDialog.show(this, "连接设备", "连接中 :" + address, true, true,
                                                       // cancellable
                                                       new DialogInterface.OnCancelListener() {
                                                           @Override
                                                           public void onCancel(DialogInterface dialog)
                                                           {
                                                               mConnect_info_container.setVisibility(
                                                                       View.GONE);
                                                               //中文 点击发送文件进行文件传输
                                                               // mTv_trans_file.setText("Click to send the file for file transfer");
                                                                mTv_trans_file.setText("点击发送文件进行文件传输");
                                                               mRcyc_devices.setVisibility(View.VISIBLE);
                                                               mManager.stopPeerDiscovery(mChannel,
                                                                                          new WifiP2pManager.ActionListener() {
                                                                                              @Override
                                                                                              public void onSuccess() {

                                                                                              }

                                                                                              @Override
                                                                                              public void onFailure(
                                                                                                      int reason)
                                                                                              {

                                                                                              }
                                                                                          });

                                                               mManager.removeGroup(mChannel,
                                                                                    new WifiP2pManager.ActionListener() {
                                                                                        @Override
                                                                                        public void onSuccess() {
                                                                                            Log.d(TAG,
                                                                                                  "onSuccess: 移除成功1");
                                                                                        }

                                                                                        @Override

                                                                                        public void onFailure(
                                                                                                int reason)
                                                                                        {
                                                                                            Log.d(TAG,
                                                                                                  "onFailure: 移除失败1");
                                                                                        }
                                                                                    });
                                                           }
                                                       });
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = address;
        config.wps.setup = WpsInfo.PBC;
        config.groupOwnerIntent = 0;
        Log.d(TAG, "createConnet: 第二部 进行连接" + name + "::" + config.deviceAddress);

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                LogUtils.logInfo(TAG, "createConnet: ", "连接成功了");

            }

            @Override
            public void onFailure(int reason) {
                LogUtils.logInfo(TAG, "createConnet: ", "连接失败了");
//中文  连接失败
//                Toast.makeText(TransferActivity.this, "The connection failed ", Toast.LENGTH_SHORT)
//                     .show();
                Toast.makeText(TransferActivity.this, "连接失败 ", Toast.LENGTH_SHORT)
                     .show();
            }
        });


    }

    private void initFilter() {
        mFilter = new IntentFilter();
        mFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

    }

    private void initEvent() {
        mBtn_stop_connect.setOnClickListener(this);
        mBtn_search.setOnClickListener(this);
        mBtn_send_file.setOnClickListener(this);
        mBtn_connect_device.setOnClickListener(this);


    }

    private void initView() {

        mRcyc_devices = (RecyclerView) findViewById(R.id.devices);
        mBtn_search = (Button) findViewById(R.id.search_device);
        mBtn_connect_device = (Button) findViewById(R.id.connect_device);
        mBtn_stop_connect = (Button) findViewById(R.id.stop_connect);
        mBtn_send_file = (Button) findViewById(R.id.send_file);

        //  mIv_test_camera = (ImageView) findViewById(R.id.iv_test_camera);
        mTv_trans_file = (TextView) findViewById(R.id.tv_trans_file_client);
        mTv_trans_file.setSelected(true);
        mTv_connect_info = (TextView) findViewById(R.id.connect_info);
        mConnect_info_container = (LinearLayout) findViewById(R.id.ll_has_connected);
        mTv_connected_info_name = (TextView) findViewById(R.id.ll_has_connected_tv_name);
        mTv_connected_info_address = (TextView) findViewById(R.id.ll_has_connected_tv_address);
        mTv_connected_info_name.setSelected(true);
        mTv_connected_info_address.setSelected(true);

        mFliker_pregress = (FlikerProgressBar) findViewById(R.id.fliker_progress);
        mHandler.sendEmptyMessageDelayed(MISS_PROGRESS, 50);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mFilter);


    }

    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);

        mBtn_stop_connect.performClick();

        mManager.cancelConnect(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int reason) {

            }
        });

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.search_device:
                if (mIsConnect) {
                    //正在连接
                    //中文 已经处于连接状态，请先断开连接再进行搜索
//                    Toast.makeText(getApplicationContext(),
//                                   "Is already connected, please disconnect and then search",
//                                   Toast.LENGTH_SHORT)
//                         .show();
                    Toast.makeText(getApplicationContext(),
                                   "已经处于连接状态，请先断开连接再进行搜索",
                                   Toast.LENGTH_SHORT)
                         .show();
                    return;
                }


                if (discoverProgressDialog != null && discoverProgressDialog.isShowing()) {
                    discoverProgressDialog.dismiss();
                }
                //中文  搜索设备 ，  搜索中......
                discoverProgressDialog = ProgressDialog.show(this, "搜索设备", "搜索中......:", true, true,
                                                             // cancellable
                                                             new DialogInterface.OnCancelListener() {
                                                                 @Override
                                                                 public void onCancel(
                                                                         DialogInterface dialog)
                                                                 {
                                                                 }
                                                             });

                //start search device it can call wifireceiver :WIFI_P2P_DISCOVERY_CHANGED_ACTION
                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onFailure(int reason) {

                    }
                });

                break;
            case R.id.connect_device:
                if (mIsConnect) {
                    //正在连接
                    //中文  已经处于连接状态，请先断开连接
//                    Toast.makeText(getApplicationContext(), "Is already connected. Please disconnect", Toast.LENGTH_SHORT)
//                         .show();
                    Toast.makeText(getApplicationContext(), "已经处于连接状态，请先断开连接", Toast.LENGTH_SHORT)
                         .show();
                    return;
                }

                Log.d(TAG, "onClick: " + beSelectDevicePosition);
                if (mPeerLists != null && mPeerLists.size() != 0) {
                    String name = mPeerLists.get(beSelectDevicePosition)
                                            .get("name");
                    String address = mPeerLists.get(beSelectDevicePosition)
                                               .get("address");
                    createConnet(name, address);
                    Log.d(TAG,
                          "onClick: 连接设备" + beSelectDevicePosition + "::" + name + "::" + address);
                } else {
                    //中文  还未搜索到设备
//                    Toast.makeText(getApplicationContext(), "No device has been searched", Toast.LENGTH_SHORT)
//                         .show();
                    Toast.makeText(getApplicationContext(), "还未搜索到设备", Toast.LENGTH_SHORT)
                         .show();
                }
                break;
            case R.id.stop_connect:
                Log.d(TAG, "onClick: disconnec");
                Log.d(TAG, "run: 通过点击 stopconnect进入的reset模式");

                mFliker_pregress.reset();
                mConnect_info_container.setVisibility(View.GONE);
                if (mTv_trans_file.getVisibility() == View.GONE) {
                    mTv_trans_file.setVisibility(View.VISIBLE);
                    //中文  点击发送文件进行文件传输
                  //  mTv_trans_file.setText("Click to send the file for file transfer");
                    mTv_trans_file.setText("点击发送文件进行文件传输");
                }
                mRcyc_devices.setVisibility(View.VISIBLE);
                beSelectDevicePosition = -1;
                isDoing = false;
                mManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onFailure(int reason) {

                    }
                });

                mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "onSuccess: 移除成功1");
                    }

                    @Override

                    public void onFailure(int reason) {
                        Log.d(TAG, "onFailure: 移除失败1");
                    }
                });


                break;

            case R.id.send_file:
                Intent intent = new Intent(this,PicVideoSelectorActivity.class);


                ArrayList picPaths = new ArrayList<String>();
                ArrayList videoPaths = new ArrayList<String>();
                ContentResolver contentResolver = getApplicationContext().getContentResolver();
                Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                StringBuilder where = new StringBuilder();
                where.append(MediaStore.Video.Media.TITLE + " != ''");

                String[] projection = new String[]{MediaStore.Video.Media.TITLE,
                                                   MediaStore.Video.Media.DATA};
                where.append(" AND " + MediaStore.Video.Media.DATA + " LIKE '%" + "DCIM" + "%'");
                final Cursor cursor = contentResolver.query(uri,
                                                            projection,
                                                            where.toString(),
                                                            null,
                                                            MediaStore.Video.Media.DEFAULT_SORT_ORDER);
                if (cursor == null) {

                    return;
                }
                if (cursor.moveToFirst()) {
                    do {
                        String path  = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                        videoPaths.add(path);
                    } while (cursor.moveToNext());
                }


                Uri uri_dcim = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                StringBuilder where_dcim = new StringBuilder();
                where_dcim.append(MediaStore.Images.Media.TITLE + " != ''");
                String[] projection_dcim = new String[]{MediaStore.Images.Media.TITLE,
                                                        MediaStore.Images.Media.DATA};
                where_dcim.append(" AND " + MediaStore.Images.Media.DATA + " LIKE '%" + "DCIM" + "%'");
                final Cursor cursor_dcim = contentResolver.query(uri_dcim,
                                                                 projection_dcim,
                                                                 where_dcim.toString(),
                                                                 null,
                                                                 MediaStore.Images.Media.DEFAULT_SORT_ORDER);
                if (cursor_dcim == null) {

                    return;
                }
                if (cursor_dcim.moveToFirst()) {
                    do {
                        String path = cursor_dcim.getString(cursor_dcim.getColumnIndexOrThrow(
                                MediaStore.Images.Media.DATA));
                        picPaths.add(path);
                    } while (cursor_dcim.moveToNext());
                }
                intent.putExtra("pics" , picPaths);
                intent.putExtra("videos" , videoPaths);
                startActivityForResult(intent, 20);

                break;

            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        LogUtils.logInfo(TAG, "run", "进行到了这里requestCode");
        if (data == null) {
            return;
        }
        switch (requestCode) {
            case 20:
                super.onActivityResult(requestCode, resultCode, data);

                if (isDoing) {
                    //中文 ：正在传输文件，等待文件传输完成后再传输
//                    Toast.makeText(getApplicationContext(),
//                                   "Is transferring files, waiting for the file transfer to complete and then transfer",
//                                   Toast.LENGTH_SHORT)
//                         .show();
                    Toast.makeText(getApplicationContext(),
                                   "正在传输文件，等待文件传输完成后再传输",
                                   Toast.LENGTH_SHORT)
                         .show();
                } else if (!mIsConnect) {

//中文  请先连接设备
//                    Toast.makeText(getApplicationContext(), "Please connect the device first", Toast.LENGTH_SHORT)
//                         .show();
                    Toast.makeText(getApplicationContext(), "请先连接设备", Toast.LENGTH_SHORT)
                         .show();
                } else {
                    LogUtils.logInfo(TAG, "run", "进行到了这里");
                    ArrayList<String> selected_pv = data.getStringArrayListExtra("selected_pv");

                    Intent serviceIntent = new Intent(TransferActivity.this,
                                                      FileTransferService.class);

                    mBeTransferFileName = selected_pv.size()+"::";

                    serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);

                    serviceIntent.putExtra(FileTransferService.REAL_FILE_PATH , selected_pv);

                    serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                                           mInfo.groupOwnerAddress.getHostAddress());

                    serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 10086);

                    TransferActivity.this.startService(serviceIntent);
                    if (mFliker_pregress.getVisibility() == View.GONE) {
                        mFliker_pregress.setVisibility(View.VISIBLE);
                        mFliker_pregress.setProgress(0.1f);
                    }
                }
                break;
            default:
                break;
        }
    }

    Handler  handler  = new Handler();
    Runnable runnable = new Runnable() {

        @Override
        public void run() {
            // handler自带方法实现定时器
            try {
                handler.postDelayed(this, 1000 * 5);

                mBtn_search.performClick();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                LogUtils.logInfo(TAG, "run()", "定时器出了问题");
            }
        }
    };

    public static String getRealFilePath(final Context context, final Uri uri) {
        if (null == uri) { return null; }
        final String scheme = uri.getScheme();
        String       data   = null;
        if (scheme == null) { data = uri.getPath(); } else if (ContentResolver.SCHEME_FILE.equals(
                scheme))
        {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = context.getContentResolver()
                                   .query(uri,
                                          new String[]{MediaStore.Video.Media.DISPLAY_NAME},
                                          null,
                                          null,
                                          null);
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME);
                    if (index > -1) {
                        data = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        }
        return data;
    }

    public String getFileAbsolutePath(Context context, Uri fileUri) {
        if (context == null || fileUri == null) { return null; }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(
                context,
                fileUri))
        {
            if (isExternalStorageDocument(fileUri)) {
                String   docId = DocumentsContract.getDocumentId(fileUri);
                String[] split = docId.split(":");
                String   type  = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if (isDownloadsDocument(fileUri)) {
                String id         = DocumentsContract.getDocumentId(fileUri);
                Uri    contentUri = ContentUris.withAppendedId(Uri.parse(
                        "content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(fileUri)) {
                String   docId      = DocumentsContract.getDocumentId(fileUri);
                String[] split      = docId.split(":");
                String   type       = split[0];
                Uri      contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                String   selection     = MediaStore.Images.Media._ID + "=?";
                String[] selectionArgs = new String[]{split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } // MediaStore (and general)
        else if ("content".equalsIgnoreCase(fileUri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(fileUri)) { return fileUri.getLastPathSegment(); }
            return getDataColumn(context, fileUri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(fileUri.getScheme())) {
            return fileUri.getPath();
        }
        return null;
    }

    public String getDataColumn(Context context,
                                Uri uri,
                                String selection,
                                String[] selectionArgs)
    {
        Cursor   cursor     = null;
        String[] projection = {MediaStore.Images.Media.DATA};
        try {
            cursor = context.getContentResolver()
                            .query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null) { cursor.close(); }
        }
        return null;
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
}
