package com.example.jareld.samsontransferclient;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;


/*
 *  @项目名：  TestWifiDerect 
 *  @包名：    com.example.lyc2.testwifiderect.broadcastreceiver
 *  @文件名:   WifiDerectBroadcastReceiver
 *  @创建者:   LYC2
 *  @创建时间:  2016/11/24 11:47
 *  @描述：    TODO
 */
public class WifiDerectBroadcastReceiver
        extends BroadcastReceiver
{
    private static final String TAG = "WifiDerectBroadcasr";
    private WifiP2pManager                        mManager;
    private WifiP2pManager.Channel                mChannel;
    private WifiP2pManager.PeerListListener       mPeerListListener;
    private WifiP2pManager.ConnectionInfoListener mInfoListener;
    private static boolean isConnect = false;

    public WifiDerectBroadcastReceiver() {}

    public WifiDerectBroadcastReceiver(WifiP2pManager manager,
                                       WifiP2pManager.Channel channel,
                                       Activity activity,
                                       WifiP2pManager.PeerListListener peerListListener,
                                       WifiP2pManager.ConnectionInfoListener infoListener)
    {
        this.mManager = manager;
        this.mChannel = channel;
        this.mPeerListListener = peerListListener;
        this.mInfoListener = infoListener;


    }

    @Override
    public void onReceive(Context context, Intent intent) {


        String action = intent.getAction();
        switch (action) {

            case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                //is wifi enable?
                Log.d(TAG, "onReceive: WIFI_P2P_STATE_CHANGED_ACTION");


                break;
            case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                //search p2p_peer ，call when peer change(find or lose)
                Log.d(TAG, "onReceive: WIFI_P2P_PEERS_CHANGED_ACTION");

                mManager.requestPeers(mChannel, mPeerListListener);

                break;
            case WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION:
                Log.d(TAG, "onReceive: WIFI_P2P_DISCOVERY_CHANGED_ACTION");

                int State = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1);
                //这是一个wifi derecr搜索开启或者关闭的状态
                if (State == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED) {
                    //start discover

                } else if (State == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED) {
                    //stop discover

                }
                break;
            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                Log.d(TAG, "onReceive: WIFI_P2P_CONNECTION_CHANGED_ACTION");

                // is wifi_p2p connet

                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                WifiP2pInfo wifiP2pInfo = (WifiP2pInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);

                WifiP2pGroup wifiP2pGroup = (WifiP2pGroup) intent.getParcelableExtra(
                        WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
                Log.d(TAG, "onReceive:1111 " +networkInfo.getState() +"::"+networkInfo.isConnected()+"::"+wifiP2pInfo.isGroupOwner);

                if (networkInfo.isConnected()) {
                    //wifi_p2p has connect
                    mManager.requestConnectionInfo(mChannel, mInfoListener);
                    isConnect = true;
                    Log.d(TAG, "onReceive: 连接成功");
                } else {
                    isConnect = false;
                    UserEvent userEvent = new UserEvent(0, "disconnect");
                    RxBus.getInstance()
                         .post(userEvent);
                    Log.d(TAG, "onReceive: 失去连接");
                }
            case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                //  TODO: 2016/11/24
                Log.d(TAG, "onReceive: createConnet  WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");

                break;
        }

    }

}
