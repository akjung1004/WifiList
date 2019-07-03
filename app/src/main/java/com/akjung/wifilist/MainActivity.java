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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListView = findViewById(R.id.wifiList);

        mWIFIUtil = new WIFIUtil(this);
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
                            Toast.makeText(MainActivity.this, wifiInfo.getSSID(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

            mAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, mListDatalist);
            mListView.setAdapter(mAdapter);


            //리스트뷰의 아이템을 클릭시 해당 아이템의 문자열을 가져오기 위한 처리
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


    private void connectWifi(int position) {
        //setMobileDataEnabled(this, false);

        ScanResult scanResult = mWIFIList.get(position);
        if("[ESS]".equals(scanResult.capabilities)) {
            open(scanResult);
        }
    }

    private void setMobileDataEnabled(Context context, boolean enabled) {
        final ConnectivityManager conman =
       (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            final Class conmanClass = Class.forName(conman.getClass().getName());
            final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
            iConnectivityManagerField.setAccessible(true);
            final Object iConnectivityManager = iConnectivityManagerField.get(conman);
            final Class iConnectivityManagerClass = Class.forName(
                    iConnectivityManager.getClass().getName());
            final Method setMobileDataEnabledMethod = iConnectivityManagerClass
                    .getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
            setMobileDataEnabledMethod.setAccessible(true);

            setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private boolean isMobileDataEnabledFromLollipop(Context context) {
        boolean state = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            state = Settings.Global.getInt(context.getContentResolver(), "mobile_data", 0) == 1;
        }
        return state;
    }

    private void open(ScanResult scanResult) {

        WifiConfiguration wfc = new WifiConfiguration();
        wfc.SSID = "\"".concat(scanResult.SSID).concat("\"");
//        wfc.status = WifiConfiguration.Status.DISABLED;
//        wfc.priority = 40;
//        wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
//        wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
//        wfc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
//        wfc.allowedAuthAlgorithms.clear();
//        wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
//        wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
//        wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
//        wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
//        wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);


        int networkId = mWIFIUtil.getWifiManager().addNetwork(wfc);
        Toast.makeText(MainActivity.this, "networkId =" + networkId, Toast.LENGTH_SHORT).show();
        if (networkId != -1) {

            mWIFIUtil.getWifiManager().enableNetwork(networkId, true);

        } else {
            int Id = checkPreviousConfiguration(scanResult.SSID);
            {
                mWIFIUtil.getWifiManager().enableNetwork(networkId, true);
            }
            Toast.makeText(MainActivity.this, networkId+" remove =" + Id, Toast.LENGTH_SHORT).show();
        }





    }

    public int checkPreviousConfiguration(String ssid) {
        ssid = ssid.replace("\"", "");
        List<WifiConfiguration> configs = mWIFIUtil.getWifiManager().getConfiguredNetworks();
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
