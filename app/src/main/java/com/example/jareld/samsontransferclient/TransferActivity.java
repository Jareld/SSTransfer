package com.example.jareld.samsontransferclient;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

    private static final String TAG = "ClientActivity";
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
    private DeviceAdapter     mAdapter;
    private LinearLayout      mConnect_info_container;
    private TextView          mTv_connected_info_address;
    private TextView          mTv_connected_info_name;
    private FlikerProgressBar mFliker_pregress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        getSupportActionBar().hide();
        initView();
        initFilter();
        initReceiver();
        initEvent();
        initRxBus();
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
                                                         mTv_trans_file.setText("上一次文件传输完成，点击发送文件进行文件传输");
                                                     }

                                                     Log.d(TAG,
                                                           "run: mRcyc_devices" + (mRcyc_devices.getVisibility() == View.GONE));

                                                 }
                                             });


                                             break;
                                         case "doing":
                                             //  float per = (float) userEvent.getProgress() / (float) fileLength;
                                             final float finalPer = userEvent.getProgress();
                                             final int fileLengthMB = userEvent.getFileLengthMB();
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
                                                 Toast.makeText(getApplicationContext(),
                                                                "连接失败 , 请重新连接",
                                                                Toast.LENGTH_SHORT)
                                                      .show();
                                                 mIsFromCreateConnect = false;
                                             }
                                             isDoing = false;
                                             mTv_trans_file.setText("点击发送文件进行文件传输");

                                             mConnect_info_container.setVisibility(View.GONE);
                                             mRcyc_devices.setVisibility(View.VISIBLE);
                                             mIsConnect = false;
                                             Log.d(TAG, "call: 失去连接");
                                             beSelectDevicePosition = -1;
                                             break;
                                         case "connect_fail":

                                             Toast.makeText(getApplicationContext(),
                                                            "启动文件传输失败 ，请重新发送文件",
                                                            Toast.LENGTH_SHORT)
                                                  .show();
                                             isDoing = false;
                                             Log.d(TAG, "run: 通过connect_fail进入的reset模式");
                                             mFliker_pregress.reset();
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
                        mTv_connect_info.setText("搜索到设备:");
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
                                mTv_connected_info_name.setText("设备名称：" + wifiP2pDevice.deviceName);
                                mTv_connected_info_address.setText("设备地址：" + wifiP2pDevice.deviceAddress);
                                mIsConnect = true;
                                mTv_connect_info.setText("已经连接到设备：");
                                Toast.makeText(TransferActivity.this, "连接成功", Toast.LENGTH_SHORT)
                                     .show();
                                mIsFromCreateConnect = false;
                                isHandleFinished = true;
                                return;
                            }
                        }


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
        connectingProgressDialog = ProgressDialog.show(this, "连接设备", "连接中 :" + address, true, true,
                                                       // cancellable
                                                       new DialogInterface.OnCancelListener() {
                                                           @Override
                                                           public void onCancel(DialogInterface dialog)
                                                           {
                                                               mConnect_info_container.setVisibility(
                                                                       View.GONE);
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

                Toast.makeText(TransferActivity.this, "连接失败", Toast.LENGTH_SHORT)
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
        mFliker_pregress.setVisibility(View.GONE);
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
                    Toast.makeText(getApplicationContext(),
                                   "已经处于连接状态，请先断开连接再进行搜索",
                                   Toast.LENGTH_SHORT)
                         .show();
                    return;
                }


                if (discoverProgressDialog != null && discoverProgressDialog.isShowing()) {
                    discoverProgressDialog.dismiss();
                }
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
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/*;image/*");
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
                    Toast.makeText(getApplicationContext(),
                                   "正在传输文件，等待文件传输完成后再传输",
                                   Toast.LENGTH_SHORT)
                         .show();
                } else if (!mIsConnect) {
                    Toast.makeText(getApplicationContext(), "请先连接设备", Toast.LENGTH_SHORT)
                         .show();
                } else {
                    LogUtils.logInfo(TAG, "run", "进行到了这里");
                    Uri uri = data.getData();

                    Intent serviceIntent = new Intent(TransferActivity.this,
                                                      FileTransferService.class);

                    String realFilePath = getRealFilePath(this, uri);
                    LogUtils.logInfo(TAG, "run", "进行到了这里" + realFilePath);
                    mBeTransferFileName = realFilePath;
                    serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);

                    serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());

                    serviceIntent.putExtra(FileTransferService.REAL_FILE_PATH, realFilePath);


                    Log.d(TAG,
                          "onActivityResult:realFilePath " + realFilePath + "::uri.toString()" + Uri.parse(
                                  uri.toString())
                                                                                                    .toString());
                    serviceIntent.putExtra(FileTransferService.BE_CLICKED_DEVICE_NAME,
                                           mBeClickedDeviceName);

                    serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                                           mInfo.groupOwnerAddress.getHostAddress());

                    serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);

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
}