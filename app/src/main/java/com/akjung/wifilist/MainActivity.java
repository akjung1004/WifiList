package com.akjung.wifilist;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private int REQUEST_PERMISION_PERMISSION_CODE = 1001;
    private String TAG = "TAG";

    private ListView mListView;
    private WIFIUtil mWIFIUtil;
    private List<ScanResult> mWIFIList;
    private List<String> mListDatalist = new ArrayList<>();
    ArrayAdapter<String> mAdapter = null;

    private TextView mInfoText;

    private String mCurrentSSID;
    private WifiManager mWifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mInfoText = findViewById(R.id.infoText);
        mListView = findViewById(R.id.wifiList);

        mWIFIUtil = new WIFIUtil(this);

        mWifiManager = mWIFIUtil.getWifiManager();

        requestPermission();
    }

    private void requestPermission() {
        //권한 체크
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_PERMISION_PERMISSION_CODE);
        } else {

            mWIFIUtil.registerReceiver();
            mWIFIUtil.setScanResultList(new WIFIUtil.WIFIListGetInterface() {
                @Override
                public void onResult(List<ScanResult> scanResultList) {
                    if(mWIFIList == null || mWIFIList.size() == 0) {
                        mWIFIList = scanResultList;
                        mListDatalist.clear();
                        for(int i=0; i<mWIFIList.size(); i++) {
                            ScanResult scanResult = mWIFIList.get(i);
                            Log.d(TAG,"+++++++++++++++++++++++  index = 0 " + i);
                            Log.d(TAG,"scanResult.capabilities = " + scanResult.capabilities);
                            Log.d(TAG,"scanResult.SSID = " + scanResult.SSID);
                            int level = WifiManager.calculateSignalLevel(scanResult.level, 101);
                            mListDatalist.add(scanResult.SSID + "   " + scanResult.capabilities + "  " + level);
                        }
                        mAdapter.notifyDataSetInvalidated();
                    }
                }

                @Override
                public void connectWIFI(final WifiInfo wifiInfo) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mInfoText.append(wifiInfo.getSSID()+"\n");
                            Toast.makeText(MainActivity.this, wifiInfo.getSSID(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

            mAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, mListDatalist);
            mListView.setAdapter(mAdapter);

            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> adapterView,
                                        View view, int position, long id) {
                    connectWifi(position);
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISION_PERMISSION_CODE &&
                (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            requestPermission();
        }  else {
            Toast.makeText(MainActivity.this, "No!! Permission!!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    public void clickRemoveNetwork(View view) {

        mWifiManager.disconnect();
        //app이 생성한 WIFI는 삭제 가능
        final List<WifiConfiguration> configurations = mWifiManager.getConfiguredNetworks();
        for(final WifiConfiguration config : configurations) {
            if(config.SSID.equals(mCurrentSSID)) {
                mWifiManager.removeNetwork(config.networkId);
            }
        }
    }

    private void connectWifi(int position) {

        ScanResult scanResult = mWIFIList.get(position);

        mCurrentSSID = scanResult.SSID;
        mInfoText.append("set :" + mCurrentSSID );
        if("[ESS]".equals(scanResult.capabilities)) {
            open(scanResult);
        }  else if(scanResult.capabilities.contains("WPA2")){
            wpa2(scanResult);
        }
    }


    private int getMaxConfigurationPriority(final WifiManager wifiManager) {
        final List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
        int maxPriority = 0;
        for(final WifiConfiguration config : configurations) {
            if(config.priority > maxPriority)
                maxPriority = config.priority;
        }

        return maxPriority;
    }

    private void open(ScanResult scanResult) {

        WifiConfiguration wfc = new WifiConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            wfc.isHomeProviderNetwork = true;
        }
        wfc.SSID = "\"".concat(scanResult.SSID).concat("\"");
        wfc.status = WifiConfiguration.Status.DISABLED;
        wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        wfc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);

        wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        wfc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);

        wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
        wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);



        int networkId = mWifiManager.addNetwork(wfc);
        Toast.makeText(MainActivity.this, "networkId =" + networkId, Toast.LENGTH_SHORT).show();
        if (networkId != -1) {

            mWifiManager.enableNetwork(networkId, true);

        } else {
            int Id = checkPreviousConfiguration(scanResult.SSID);
            {
                mWifiManager.enableNetwork(networkId, true);
            }
            Toast.makeText(MainActivity.this, networkId+" remove =" + Id, Toast.LENGTH_SHORT).show();
        }
    }


    private void wpa2(ScanResult scanResult) {
        String password = "dddddddddd";

        WifiConfiguration wfc = new WifiConfiguration();
        wfc.SSID = "\"".concat(scanResult.SSID).concat("\"");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            wfc.isHomeProviderNetwork = true;
        }
        wfc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        wfc.preSharedKey = "\"".concat(password).concat("\"");


        int networkId = mWifiManager.addNetwork(wfc);
        Toast.makeText(MainActivity.this, "networkId =" + networkId, Toast.LENGTH_SHORT).show();
        if (networkId != -1) {
            
            mWifiManager.enableNetwork(networkId, true);

        }

    }


    public int checkPreviousConfiguration(String ssid) {
        ssid = ssid.replace("\"", "");
        List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
        for(WifiConfiguration config : configs) {
           String configSSID = config.SSID.replace("\"", "");
            if(configSSID.compareTo(ssid) == 0) {

                return config.networkId;
            }
        }


        return -1;
    }



    @Override
    protected void onDestroy() {
        mWIFIUtil.unregisterReceiver();
        super.onDestroy();
    }
}
