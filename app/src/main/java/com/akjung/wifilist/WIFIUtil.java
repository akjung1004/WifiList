package com.akjung.wifilist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;


import java.util.ArrayList;
import java.util.List;

/**
 * WIFIUtil Util
 */
public class WIFIUtil {
    private Context mContext= null;
    private WifiManager mWifiManager = null;
    private List<ScanResult> mScanResultList; // ScanResult List
    private WIFIListGetInterface mWIFIListGetInterface;
    private int mNetWorkId = -1;

    public interface WIFIListGetInterface {
        void onResult(List<ScanResult> scanResultList);
        void connectWIFI(WifiInfo wifiInfo);
    }


    public WIFIUtil(Context context) {
        mContext = context;
        mScanResultList = new ArrayList<>();
        mWifiManager =  (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public void setScanResultList(WIFIListGetInterface WIFIListGetInterface) {
        this.mWIFIListGetInterface = WIFIListGetInterface;
    }

    /**
     * Receiver 등록  - 호출 후 unregisterReceiver() 호출 필요
     */
    public void registerReceiver() {
        //WIFI 목록 가져오는 receiver 등록
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mContext.registerReceiver(mScanReasultReceiver, intentFilter);
    }

    /**
     * Receiver 해제
     */
    public void unregisterReceiver() {
        mContext.unregisterReceiver(mScanReasultReceiver);
    }


    private BroadcastReceiver mScanReasultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) ||
                    action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION) ) {

                //WIFI ACCESS RESULT 조회
                mScanResultList = mWifiManager.getScanResults();
                if(mWIFIListGetInterface != null && mScanResultList.size() > 0) {
                    mWIFIListGetInterface.onResult(mScanResultList);
                }

                //WIFI 연결 체크를 위해 따로 처리
                if(action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                    mContext.sendBroadcast(new Intent("wifi.ON_NETWORK_STATE_CHANGED"));
                    if(mWIFIListGetInterface != null) {
                        NetworkInfo netInfo = intent.getParcelableExtra (WifiManager.EXTRA_NETWORK_INFO);
                        if (ConnectivityManager.TYPE_WIFI == netInfo.getType ()) {
                            WifiInfo info = mWifiManager.getConnectionInfo();
                            String ssid  = info.getSSID();
                            mNetWorkId = info.getNetworkId();
                            mWIFIListGetInterface.connectWIFI(info);

                        }
                    }
                }
            }
        }
    };

    /**
     * WifiManager get
     * @return WifiManager
     */
    public WifiManager getWifiManager() {
        return mWifiManager;
    }



}
