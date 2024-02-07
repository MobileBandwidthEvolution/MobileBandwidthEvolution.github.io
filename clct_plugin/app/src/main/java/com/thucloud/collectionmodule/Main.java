package com.thucloud.collectionmodule;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.ContactsContract;
import android.telephony.AccessNetworkConstants;
import android.telephony.CellIdentity;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellIdentityTdscdma;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoTdscdma;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.CellSignalStrengthTdscdma;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import me.weishu.reflection.Reflection;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Main {
    private Context context;
    CountDownLatch latch = null;
    static String uuid;
    // 创建一个OkHttpClient实例
    private static final OkHttpClient client = new OkHttpClient();

    static class TestInfo {
        public final String uid;
        public final String user_uid;
        public final String client_timestamp;
        public final String network_type;
        public final String brand;
        public final String model;
        public final String os_type;
        public final String os_version;
        public final String user_isp_id;
        public final String user_region_id;
        public final String user_city_id;
        public final String user_lat;
        public final String user_lon;
        public final String downlink_bandwidth_Mbps;
        public final String uplink_bandwidth_Mbps;
        public final String jitter_ms;
        public final String packet_loss;
        public final String latency;
        public TestInfo(String uuid, String user_uid, String network_type, String client_timestamp,
                        String brand, String model,
                        String os_type, String os_version,
                        String user_isp_id,
                        String user_region_id, String user_city_id,
                        String user_lat, String user_lon,
                        String download_bandwidth_Mbps, String upload_bandwidth_Mbps,
                        String jitter_ms, String packet_loss, String latency) {
            this.uid = uuid;
            this.user_uid = user_uid;
            this.client_timestamp = client_timestamp;
            this.network_type = network_type;
            this.os_version = os_version;
            this.brand = brand;
            this.model = model;
            this.os_type = os_type;
            this.user_isp_id = user_isp_id;
            this.user_region_id = user_region_id;
            this.user_city_id = user_city_id;
            this.user_lat = user_lat;
            this.user_lon = user_lon;
            this.downlink_bandwidth_Mbps = download_bandwidth_Mbps;
            this.uplink_bandwidth_Mbps = upload_bandwidth_Mbps;
            this.jitter_ms = jitter_ms;
            this.packet_loss = packet_loss;
            this.latency = latency;
        }
    }

    public Main(Context context) {
        this.context = context;
    }

    private static void sendPostRequest(String path, String body) {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(JSON, body);

        Request request = new Request.Builder()
            .url("http://124.223.35.212:8000" + path)
            .post(requestBody)
            .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.out.println("请求失败: " + path + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {

            }
        });
    }
    private static void sendPostErrorRequest(Exception e) {
        String content = "UID: " + uuid + ", Error: " + e.toString();
        MediaType MEDIA_TYPE_PLAIN = MediaType.parse("text/plain; charset=utf-8");
        RequestBody body = RequestBody.create(MEDIA_TYPE_PLAIN, content);

        Request request = new Request.Builder()
                .url("http://124.223.35.212:8000/error")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.out.println("发送error失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {

                Log.d("send err", content);
            }
        });
    }

    public void report(String user_uid,
                       String brand, String model,
                       String os_type, String os_version,
                       String user_isp_id,
                       String user_region_id, String user_city_id,
                       String user_lat, String user_lon,
                       String download_bandwidth_Mbps, String upload_bandwidth_Mbps,
                       String jitter_ms, String packet_loss, String latency) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            return;
        }

        uuid = generateUniqueId();
        String clientTimeStamp = String.valueOf(System.currentTimeMillis());
//        Log.d("UUID", uuid);
//        Log.d("clientTimeStamp", clientTimeStamp);
        String networkType = getNetworkType();
//        Log.d("network_type", networkType);

        Gson gson = new GsonBuilder()
                .serializeNulls() 
                .setPrettyPrinting()
                .create();
        TestInfo testInfo = new TestInfo(uuid, user_uid, networkType, clientTimeStamp, brand, model, os_type, os_version, user_isp_id, user_region_id, user_city_id, user_lat, user_lon, download_bandwidth_Mbps, upload_bandwidth_Mbps, jitter_ms, packet_loss, latency);
        String test_info_json = gson.toJson(testInfo);
//        Log.d("TestInfo", test_info_json);
        sendPostRequest("/test_info", test_info_json);

        try {
            List<MyNetworkInfo.CellInfo> cellInfo = new ArrayList<>();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                cellInfo = getCellInfo();
            }
            List<MyNetworkInfo.SubInfo> subInfo = getSubInfo();
            List<MyNetworkInfo.WifiInfo> wifiInfo = getWifiInfo();
            MyNetworkInfo myNetworkInfo = new MyNetworkInfo(networkType, subInfo, cellInfo, wifiInfo);
            Log.d("myNetworkInfo", myNetworkInfo.toString());

            new Thread(() -> {
                try {
                    if (latch != null) {
                        latch.await();
                    }
//                    Log.d("myNetworkInfo.cellInfo", String.valueOf(myNetworkInfo.cellInfo));
//                    Log.d("myNetworkInfo.wifiInfo", myNetworkInfo.wifiInfo.toString());
                    if (myNetworkInfo.wifiInfo!=null) {
                        for (MyNetworkInfo.WifiInfo wi:myNetworkInfo.wifiInfo) {
                            wi.uid = uuid;
                            String wifi_info_json = gson.toJson(wi);
                            sendPostRequest("/wifi_info", wifi_info_json);
                            Log.d("WifiInfo", wifi_info_json);
                        }
                    }
                    if (myNetworkInfo.cellInfo!=null) {
                        for (MyNetworkInfo.CellInfo ci:myNetworkInfo.cellInfo) {
                            ci.uid = uuid;
                            String cell_info_json = gson.toJson(ci);
                            sendPostRequest("/cell_info", cell_info_json);
                            Log.d("CellInfo", cell_info_json);
                        }
                    }
                    if (myNetworkInfo.subInfo!=null) {
                        for (MyNetworkInfo.SubInfo si:myNetworkInfo.subInfo) {
                            si.uid = uuid;
                            String sub_info_json = gson.toJson(si);
                            sendPostRequest("/sub_info", sub_info_json);
                            Log.d("SubInfo", sub_info_json);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    sendPostErrorRequest(e);
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            sendPostErrorRequest(e);
        }


    }

    public static String generateUniqueId() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    /*
        This method is deprecated in API level 28 by Android documentation,
        but still work in my phone with API level 30.
    */
    String getNetworkType() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isAvailable())
            return "unknown";
        int connectionType = networkInfo.getType();
        if (connectionType == ConnectivityManager.TYPE_WIFI)
            return "WiFi";
        if (connectionType == ConnectivityManager.TYPE_MOBILE) {
            int cellType = networkInfo.getSubtype();
            switch (cellType) {
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                case TelephonyManager.NETWORK_TYPE_IDEN:     // api< 8: replace by 11
                case TelephonyManager.NETWORK_TYPE_GSM:      // api<25: replace by 16
                    return "2G";
                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:   // api< 9: replace by 12
                case TelephonyManager.NETWORK_TYPE_EHRPD:    // api<11: replace by 14
                case TelephonyManager.NETWORK_TYPE_HSPAP:    // api<13: replace by 15
                case TelephonyManager.NETWORK_TYPE_TD_SCDMA: // api<25: replace by 17
                    return "3G";
                case TelephonyManager.NETWORK_TYPE_LTE:      // api<11: replace by 13
                case TelephonyManager.NETWORK_TYPE_IWLAN:    // api<25: replace by 18
                case 19: // LTE_CA
                    return "4G";
                case TelephonyManager.NETWORK_TYPE_NR:       // api<29: replace by 20
                    return "5G";
                default:
                    return "unknown";
            }
        }

        return "unknown";
    }

    List<MyNetworkInfo.WifiInfo> getWifiInfo() {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("No permission:", "ACCESS_FINE_LOCATION");
            return new ArrayList<>();
        }
        List<MyNetworkInfo.WifiInfo> myWifiInfo = new ArrayList<>();
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network[] networks = connectivityManager.getAllNetworks();
        Log.d("network list", String.valueOf(networks.length));
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        List<ScanResult> myScanResults = wifiManager.getScanResults();
        String ScanResultLength = String.valueOf(myScanResults.size());
        for (Network network : networks) {
            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
            Log.d("networkCapabilities",networkCapabilities.toString());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                Log.d("Build.VERSION","Build.VERSION");
//                Log.d("networkCapabilities.getTransportInfo",networkCapabilities.getTransportInfo().toString());
                if (networkCapabilities.getTransportInfo() != null) {
                    Log.d("networkCapabilities.getTransportInfo",networkCapabilities.getTransportInfo().toString());
                    try {
                        WifiInfo wifiInfo = (WifiInfo) networkCapabilities.getTransportInfo();
                        String wifi_type = String.valueOf(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
                        Log.d("network", String.valueOf(wifiInfo.getLinkSpeed()));
                        String SSID = wifiInfo.getSSID();
                        String BSSID;
                        if (wifiInfo.getBSSID() == null)
                            BSSID = "NULL";
                        else
                            BSSID = wifiInfo.getBSSID();
                        String rssi = String.valueOf(wifiInfo.getRssi());
                        String linkSpeed = String.valueOf(wifiInfo.getLinkSpeed());
                        String networkId = String.valueOf(wifiInfo.getNetworkId());
                        String frequency = String.valueOf(wifiInfo.getFrequency());
                        String hiddenSSID = String.valueOf(wifiInfo.getHiddenSSID());

                        String passpointFqdn, passpointProviderFriendlyName, rxLinkSpeedMbps, txLinkSpeedMbps;
                        if (wifiInfo.getPasspointFqdn() == null)
                            passpointFqdn = "NULL";
                        else
                            passpointFqdn = wifiInfo.getPasspointFqdn();
                        if (wifiInfo.getPasspointProviderFriendlyName() == null)
                            passpointProviderFriendlyName = "NULL";
                        else
                            passpointProviderFriendlyName = wifiInfo.getPasspointProviderFriendlyName();
                        rxLinkSpeedMbps = String.valueOf(wifiInfo.getRxLinkSpeedMbps());
                        txLinkSpeedMbps = String.valueOf(wifiInfo.getTxLinkSpeedMbps());
                        String maxSupportedRxLinkSpeedMbps, maxSupportedTxLinkSpeedMbps, wifiStandard;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            maxSupportedRxLinkSpeedMbps = String.valueOf(wifiInfo.getMaxSupportedRxLinkSpeedMbps());
                            maxSupportedTxLinkSpeedMbps = String.valueOf(wifiInfo.getMaxSupportedTxLinkSpeedMbps());
                            wifiStandard = String.valueOf(wifiInfo.getWifiStandard());
                        } else {
                            maxSupportedRxLinkSpeedMbps = "Added in API level 30";
                            maxSupportedTxLinkSpeedMbps = "Added in API level 30";
                            wifiStandard = "Added in API level 30";
                        }
                        String currentSecurityType, subscriptionId;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            currentSecurityType = String.valueOf(wifiInfo.getCurrentSecurityType());
                            subscriptionId = String.valueOf(wifiInfo.getSubscriptionId());
                        } else {
                            currentSecurityType = "Added in API level 31";
                            subscriptionId = "Added in API level 31";
                        }
                        myWifiInfo.add(new MyNetworkInfo.WifiInfo(SSID, BSSID, rssi, linkSpeed, networkId, frequency,
                                passpointFqdn, passpointProviderFriendlyName, rxLinkSpeedMbps, txLinkSpeedMbps,
                                maxSupportedRxLinkSpeedMbps, maxSupportedTxLinkSpeedMbps, wifiStandard,
                                currentSecurityType, subscriptionId, hiddenSSID, ScanResultLength, wifi_type));
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        sendPostErrorRequest(e);
                    }
                }
            }
        }
        return myWifiInfo;
    }

    public class ServiceStateListener extends PhoneStateListener {
//        MyNetworkInfo.CellInfo cellInfo;
        MyNetworkInfo.SubInfo subInfo;
        boolean called = false;

        ServiceStateListener(MyNetworkInfo.SubInfo subInfo) {
            super();
            this.subInfo = subInfo;
        }

        @SuppressLint("BlockedPrivateApi")
        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            if (called) return;
            called = true;
            super.onServiceStateChanged(serviceState);
            List<NetworkRegistrationInfo> infoList = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                infoList = serviceState.getNetworkRegistrationInfoList();
            }
            for (NetworkRegistrationInfo networkRegistrationInfo : infoList) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (networkRegistrationInfo.getTransportType() == AccessNetworkConstants.TRANSPORT_TYPE_WWAN
                            && (networkRegistrationInfo.getDomain() & NetworkRegistrationInfo.DOMAIN_PS) != 0) {
                        int b = networkRegistrationInfo.getAccessNetworkTechnology();
                        int c = networkRegistrationInfo.getDomain();
                        try {
                            Field field = networkRegistrationInfo.getClass().getDeclaredField("mDataSpecificInfo");
                            field.setAccessible(true);
                            Object info = field.get(networkRegistrationInfo);
                            Field field2 = info.getClass().getDeclaredField("isEnDcAvailable");
                            field2.setAccessible(true);
                            boolean isEndcAvailable = (boolean) field2.get(info);
//                            Log.d("isEndcAvailable", String.valueOf(isEndcAvailable));

                            Field field3 = info.getClass().getDeclaredField("isDcNrRestricted");
                            field3.setAccessible(true);
                            boolean isDcNrRestricted = (boolean) field3.get(info);
//                            Log.d("isDcNrRestricted", String.valueOf(isDcNrRestricted));

                            Field field4 = info.getClass().getDeclaredField("maxDataCalls");
                            field4.setAccessible(true);
                            int maxDataCalls = (int) field4.get(info);
//                            Log.d("maxDataCalls", String.valueOf(maxDataCalls));

                            Field field5 = info.getClass().getDeclaredField("isNrAvailable");
                            field5.setAccessible(true);
                            boolean isNrAvailable = (boolean) field5.get(info);
//                            Log.d("isNrAvailable", String.valueOf(isNrAvailable));
//                            {'uid': '80beafd5-4b07-437a-8763-d26c3ba618ce', 'isDcNrRestricted': 'false', 'maxDataCalls': '16', 'isNrAvailable': 'true', 'isEndcAvailable': '', 'is_using': 'false', 'cell_basestationId': '', 'cell_arfcn': '', 'cell_bsic': '', 'cell_cid': '', 'cell_lac': '', 'cell_mcc': '460', 'cell_mnc': '11', 'cell_mobileNetworkOperator': '46011', 'cell_bands': "['3']", 'cell_bandwidth': '2147483647', 'cell_ci': '4818299', 'cell_earfcn': '1506', 'cell_pci': '319', 'cell_tac': '4125', 'cell_nci': '', 'cell_nrarfcn': '', 'cell_psc': '', 'cell_uarfcn': '', 'cell_cpid': '', 'other': '', 'isEndcAvaliable': 'true', 'subId': 1}
//                            INSERT INTO sub_info (uid,isDcNrRestricted,maxDataCalls,isNrAvailable,isEndcAvailable,is_using,cell_basestationId,cell_arfcn,cell_bsic,cell_cid,cell_lac,cell_mcc,cell_mnc,cell_mobileNetworkOperator,cell_bands,cell_bandwidth,cell_ci,cell_earfcn,cell_pci,cell_tac,cell_nci,cell_nrarfcn,cell_psc,cell_uarfcn,cell_cpid,other)
//                            VALUES ('80beafd5-4b07-437a-8763-d26c3ba618ce','false','16','true','','false','','','','','','460','11','46011','['3']','2147483647','4818299','1506','319','4125','','','','','','');
                            subInfo.isEndcAvailable = String.valueOf(isEndcAvailable);
                            subInfo.isDcNrRestricted = String.valueOf(isDcNrRestricted);
                            subInfo.maxDataCalls = String.valueOf(maxDataCalls);
                            subInfo.isNrAvailable = String.valueOf(isNrAvailable);
                            CellIdentity cellIdentity = networkRegistrationInfo.getCellIdentity();
                            if (cellIdentity instanceof CellIdentityCdma) {
                                subInfo.cellIdentity = getCellIdentityCdma((CellIdentityCdma) cellIdentity);
                            }
                            if (cellIdentity instanceof CellIdentityGsm) {
                                subInfo.cellIdentity = getCellIdentityGsm((CellIdentityGsm) cellIdentity);
                            }
                            if (cellIdentity instanceof CellIdentityLte) {
                                subInfo.cellIdentity = getCellIdentityLte((CellIdentityLte) cellIdentity);
                            }
                            if (cellIdentity instanceof CellIdentityWcdma) {
                                subInfo.cellIdentity = getCellIdentityWcdma((CellIdentityWcdma) cellIdentity);
                            }
                            if (cellIdentity instanceof CellIdentityTdscdma) {
                                subInfo.cellIdentity = getCellIdentityTdscdma((CellIdentityTdscdma) cellIdentity);
                            }
                            if (cellIdentity instanceof CellIdentityNr) {
                                subInfo.cellIdentity = getCellIdentityNr((CellIdentityNr) cellIdentity);
                            }
//                            Log.d("SubInfo CellIdentity", subInfo.cellIdentity.toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                            sendPostErrorRequest(e);
                        }

                    }
                }
            }
            int a = serviceState.getState();
            // Process the service state change here
            latch.countDown();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    List<MyNetworkInfo.CellInfo> getCellInfo() {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("No permission:", "ACCESS_FINE_LOCATION");
            return new ArrayList<>();
        }
        List<MyNetworkInfo.CellInfo> myCellInfoList = new ArrayList<>();
        if (telephonyManager != null) {

            List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
//            Log.d("cellList", "cellInfo size: " + cellInfoList.size());
            for (CellInfo cellInfo : cellInfoList) {
                if (cellInfo.isRegistered()) {
                    if (cellInfo instanceof CellInfoCdma) {
                        CellInfoCdma cellInfoCdma = (CellInfoCdma) cellInfo;
                        MyNetworkInfo.CellInfo.CellIdentityCdma cellIdentity = getCellIdentityCdma(cellInfoCdma.getCellIdentity());
                        MyNetworkInfo.CellInfo.CellSignalStrengthCdma cellSignalStrength = getCellSignalStrengthCdma(cellInfoCdma.getCellSignalStrength());
                        myCellInfoList.add(new MyNetworkInfo.CellInfo("CDMA", cellIdentity, cellSignalStrength));
                    }
                    if (cellInfo instanceof CellInfoGsm) {
                        CellInfoGsm cellInfoGsm = (CellInfoGsm) cellInfo;
                        MyNetworkInfo.CellInfo.CellIdentityGsm cellIdentity = getCellIdentityGsm(cellInfoGsm.getCellIdentity());
                        MyNetworkInfo.CellInfo.CellSignalStrengthGsm cellSignalStrength = getCellSignalStrengthGsm(cellInfoGsm.getCellSignalStrength());
                        myCellInfoList.add(new MyNetworkInfo.CellInfo("GSM", cellIdentity, cellSignalStrength));
                    }
                    if (cellInfo instanceof CellInfoLte) {
                        CellInfoLte cellInfoLte = (CellInfoLte) cellInfo;
                        MyNetworkInfo.CellInfo.CellIdentityLte cellIdentity = getCellIdentityLte(cellInfoLte.getCellIdentity());
                        MyNetworkInfo.CellInfo.CellSignalStrengthLte cellSignalStrength = getCellSignalStrengthLte(cellInfoLte.getCellSignalStrength());
                        myCellInfoList.add(new MyNetworkInfo.CellInfo("LTE", cellIdentity, cellSignalStrength));
                    }
                    if (cellInfo instanceof CellInfoWcdma) {
                        CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) cellInfo;
                        MyNetworkInfo.CellInfo.CellIdentityWcdma cellIdentity = getCellIdentityWcdma(cellInfoWcdma.getCellIdentity());
                        MyNetworkInfo.CellInfo.CellSignalStrengthWcdma cellSignalStrength = getCellSignalStrengthWcdma(cellInfoWcdma.getCellSignalStrength());
                        myCellInfoList.add(new MyNetworkInfo.CellInfo("WCDMA", cellIdentity, cellSignalStrength));
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (cellInfo instanceof CellInfoTdscdma) {
                            CellInfoTdscdma cellInfoTdscdma = (CellInfoTdscdma) cellInfo;
                            MyNetworkInfo.CellInfo.CellIdentityTdscdma cellIdentity = getCellIdentityTdscdma(cellInfoTdscdma.getCellIdentity());
                            MyNetworkInfo.CellInfo.CellSignalStrengthTdscdma cellSignalStrength = getCellSignalStrengthTdscdma(cellInfoTdscdma.getCellSignalStrength());
                            myCellInfoList.add(new MyNetworkInfo.CellInfo("TDSCDMA", cellIdentity, cellSignalStrength));
                        }
                        if (cellInfo instanceof CellInfoNr) {
                            CellInfoNr cellInfoNr = (CellInfoNr) cellInfo;
                            MyNetworkInfo.CellInfo.CellIdentityNr cellIdentity = getCellIdentityNr((CellIdentityNr) cellInfoNr.getCellIdentity());
                            MyNetworkInfo.CellInfo.CellSignalStrengthNr cellSignalStrength = getCellSignalStrengthNr((CellSignalStrengthNr) cellInfoNr.getCellSignalStrength());
                            myCellInfoList.add(new MyNetworkInfo.CellInfo("NR", cellIdentity, cellSignalStrength));
                        }
                    }
                }
            }
        }
        return myCellInfoList;
    }
    MyNetworkInfo.CellInfo.CellIdentityCdma getCellIdentityCdma(CellIdentityCdma cellIdentity) {
        String basestationId = String.valueOf(cellIdentity.getBasestationId());
        String latitude = String.valueOf(cellIdentity.getLatitude());
        String longitude = String.valueOf(cellIdentity.getLongitude());
        String networkId = String.valueOf(cellIdentity.getNetworkId());
        String systemId = String.valueOf(cellIdentity.getSystemId());
        return new MyNetworkInfo.CellInfo.CellIdentityCdma(basestationId, latitude, longitude, networkId, systemId);
    }

    MyNetworkInfo.CellInfo.CellIdentityGsm getCellIdentityGsm(CellIdentityGsm cellIdentity) {
        String arfcn = String.valueOf(cellIdentity.getArfcn());
        String bsic = String.valueOf(cellIdentity.getBsic());
        String cid = String.valueOf(cellIdentity.getCid());
        String lac = String.valueOf(cellIdentity.getLac());
        String mcc, mnc, mobileNetworkOperator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mcc = cellIdentity.getMccString();
            mnc = cellIdentity.getMncString();
            mobileNetworkOperator = cellIdentity.getMobileNetworkOperator();
        } else {
            mcc = String.valueOf(cellIdentity.getMcc());
            mnc = String.valueOf(cellIdentity.getMnc());
            mobileNetworkOperator = "Added in API level 28";
        }
        return new MyNetworkInfo.CellInfo.CellIdentityGsm(arfcn, bsic, cid, lac, mcc, mnc, mobileNetworkOperator);
    }

    MyNetworkInfo.CellInfo.CellIdentityLte getCellIdentityLte(CellIdentityLte cellIdentity) {
        String[] bands;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            int[] bands_int = cellIdentity.getBands();
            bands = new String[bands_int.length];
            for (int i = 0; i < bands_int.length; ++i)
                bands[i] = String.valueOf(bands_int[i]);
        } else bands = new String[]{"Added in API level 30"};
        String bandwidth;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            bandwidth = String.valueOf(cellIdentity.getBandwidth());
        else bandwidth = "Added in API level 28";
        String ci = String.valueOf(cellIdentity.getCi());
        String earfcn;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            earfcn = String.valueOf(cellIdentity.getEarfcn());
        else earfcn = "Added in API level 24";
        String mcc, mnc, mobileNetworkOperator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mcc = cellIdentity.getMccString();
            mnc = cellIdentity.getMncString();
            mobileNetworkOperator = cellIdentity.getMobileNetworkOperator();
        } else {
            mcc = String.valueOf(cellIdentity.getMcc());
            mnc = String.valueOf(cellIdentity.getMnc());
            mobileNetworkOperator = "Added in API level 28";
        }
        String pci = String.valueOf(cellIdentity.getPci());
        String tac = String.valueOf(cellIdentity.getTac());
        return new MyNetworkInfo.CellInfo.CellIdentityLte(bands, bandwidth, ci, earfcn, mcc, mnc, mobileNetworkOperator, pci, tac);
    }

    MyNetworkInfo.CellInfo.CellIdentityWcdma getCellIdentityWcdma(CellIdentityWcdma cellIdentity) {
        String cid = String.valueOf(cellIdentity.getCid());
        String lac = String.valueOf(cellIdentity.getLac());
        String mcc, mnc, mobileNetworkOperator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mcc = cellIdentity.getMccString();
            mnc = cellIdentity.getMncString();
            mobileNetworkOperator = cellIdentity.getMobileNetworkOperator();
        } else {
            mcc = String.valueOf(cellIdentity.getMcc());
            mnc = String.valueOf(cellIdentity.getMnc());
            mobileNetworkOperator = "Added in API level 28";
        }
        String psc = String.valueOf(cellIdentity.getPsc());
        String uarfcn = String.valueOf(cellIdentity.getUarfcn());
        return new MyNetworkInfo.CellInfo.CellIdentityWcdma(cid, lac, mcc, mnc, mobileNetworkOperator, psc, uarfcn);
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    MyNetworkInfo.CellInfo.CellIdentityTdscdma getCellIdentityTdscdma(CellIdentityTdscdma cellIdentity) {
        String cid = String.valueOf(cellIdentity.getCid());
        String cpid = String.valueOf(cellIdentity.getCpid());
        String lac = String.valueOf(cellIdentity.getLac());
        String mcc = cellIdentity.getMccString();
        String mnc = cellIdentity.getMncString();
        String mobileNetworkOperator;
        String uarfcn;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mobileNetworkOperator = String.valueOf(cellIdentity.getMobileNetworkOperator());
            uarfcn = String.valueOf(cellIdentity.getUarfcn());
        } else mobileNetworkOperator = uarfcn = "Added in API level 29";
        return new MyNetworkInfo.CellInfo.CellIdentityTdscdma(cid, cpid, lac, mcc, mnc, mobileNetworkOperator, uarfcn);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    MyNetworkInfo.CellInfo.CellIdentityNr getCellIdentityNr(CellIdentityNr cellIdentity) {
        String[] bands;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            int[] bands_int = cellIdentity.getBands();
            bands = new String[bands_int.length];
            for (int i = 0; i < bands_int.length; ++i)
                bands[i] = String.valueOf(bands_int[i]);
        } else bands = new String[]{"Added in API level 30"};
        String mcc = cellIdentity.getMccString();
        String mnc = cellIdentity.getMncString();
//        Log.d("cell info mcc", mcc);
//        Log.d("cell info mnc", mnc);
        String nci = String.valueOf(cellIdentity.getNci());
        String nrarfcn = String.valueOf(cellIdentity.getNrarfcn());
        String pci = String.valueOf(cellIdentity.getPci());
        String tac = String.valueOf(cellIdentity.getTac());
        return new MyNetworkInfo.CellInfo.CellIdentityNr(bands, mcc, mnc, nci, nrarfcn, pci, tac);
    }


    MyNetworkInfo.CellInfo.CellSignalStrengthCdma getCellSignalStrengthCdma(CellSignalStrengthCdma signalStrength) {
        String asuLevel = String.valueOf(signalStrength.getAsuLevel());
        String cdmaDbm = String.valueOf(signalStrength.getCdmaDbm());
        String cdmaEcio = String.valueOf(signalStrength.getCdmaEcio());
        String cdmaLevel = String.valueOf(signalStrength.getCdmaLevel());
        String dbm = String.valueOf(signalStrength.getDbm());
        String evdodbm = String.valueOf(signalStrength.getEvdoDbm());
        String evdoEcio = String.valueOf(signalStrength.getEvdoEcio());
        String evdoLevel = String.valueOf(signalStrength.getEvdoLevel());
        String evdoSnr = String.valueOf(signalStrength.getEvdoSnr());
        String level = String.valueOf(signalStrength.getLevel());
        return new MyNetworkInfo.CellInfo.CellSignalStrengthCdma(asuLevel, cdmaDbm, cdmaEcio, cdmaLevel, dbm, evdodbm, evdoEcio, evdoLevel, evdoSnr, level);
    }

    MyNetworkInfo.CellInfo.CellSignalStrengthGsm getCellSignalStrengthGsm(CellSignalStrengthGsm signalStrength) {
        String asuLevel = String.valueOf(signalStrength.getAsuLevel());
        String bitErrorRate;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            bitErrorRate = String.valueOf(signalStrength.getBitErrorRate());
        else bitErrorRate = "Added in API level 29";
        String dbm = String.valueOf(signalStrength.getDbm());
        String level = String.valueOf(signalStrength.getLevel());
        String rssi;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            rssi = String.valueOf(signalStrength.getRssi());
        else rssi = "Added in API level 30";
        String timingAdvance;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            timingAdvance = String.valueOf(signalStrength.getTimingAdvance());
        else timingAdvance = "Added in API level 26";
        return new MyNetworkInfo.CellInfo.CellSignalStrengthGsm(asuLevel, bitErrorRate, dbm, level, rssi, timingAdvance);
    }

    MyNetworkInfo.CellInfo.CellSignalStrengthLte getCellSignalStrengthLte(CellSignalStrengthLte signalStrength) {
        String asuLevel = String.valueOf(signalStrength.getAsuLevel());
        String cqi;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            cqi = String.valueOf(signalStrength.getCqi());
        else cqi = "Added in API level 26";
        String cqiTableIndex;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            cqiTableIndex = String.valueOf(signalStrength.getCqiTableIndex());
        else cqiTableIndex = "Added in API level 31";
        String dbm = String.valueOf(signalStrength.getDbm());
        String level = String.valueOf(signalStrength.getLevel());
        String rsrp, rsrq;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            rsrp = String.valueOf(signalStrength.getRsrp());
            rsrq = String.valueOf(signalStrength.getRsrq());
        } else rsrp = rsrq = "Added in API level 26";
        String rssi;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            rssi = String.valueOf(signalStrength.getRssi());
        else rssi = "Added in API level Q";
        String rssnr;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            rssnr = String.valueOf(signalStrength.getRssnr());
        else rssnr = "Added in API level Q";
        String timingAdvance = String.valueOf(signalStrength.getTimingAdvance());
        return new MyNetworkInfo.CellInfo.CellSignalStrengthLte(asuLevel, cqi, cqiTableIndex, dbm, level, rsrp, rsrq, rssi, rssnr, timingAdvance);
    }

    MyNetworkInfo.CellInfo.CellSignalStrengthWcdma getCellSignalStrengthWcdma(CellSignalStrengthWcdma signalStrength) {
        String asuLevel = String.valueOf(signalStrength.getAsuLevel());
        String dbm = String.valueOf(signalStrength.getDbm());
        String ecNo;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            ecNo = String.valueOf(signalStrength.getEcNo());
        else ecNo = "Added in API level 30";
        String level = String.valueOf(signalStrength.getLevel());
        return new MyNetworkInfo.CellInfo.CellSignalStrengthWcdma(asuLevel, dbm, ecNo, level);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    MyNetworkInfo.CellInfo.CellSignalStrengthTdscdma getCellSignalStrengthTdscdma(CellSignalStrengthTdscdma signalStrength) {
        String asuLevel = String.valueOf(signalStrength.getAsuLevel());
        String dbm = String.valueOf(signalStrength.getDbm());
        String level = String.valueOf(signalStrength.getLevel());
        String rscp = String.valueOf(signalStrength.getRscp());
        return new MyNetworkInfo.CellInfo.CellSignalStrengthTdscdma(asuLevel, dbm, level, rscp);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    MyNetworkInfo.CellInfo.CellSignalStrengthNr getCellSignalStrengthNr(CellSignalStrengthNr signalStrength) {
        String asuLevel = String.valueOf(signalStrength.getAsuLevel());
        List<String> csicqiReport;
        String csicqiTableIndex;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            List<Integer> csicqiReport_int = signalStrength.getCsiCqiReport();
            csicqiReport = new ArrayList<>(csicqiReport_int.size());
            for (int i = 0; i < csicqiReport_int.size(); ++i)
                csicqiReport.set(i, String.valueOf(csicqiReport_int.get(i)));
            csicqiTableIndex = String.valueOf(signalStrength.getCsiCqiTableIndex());
        } else {
            csicqiReport = new ArrayList<>();
            csicqiTableIndex = "Added in API level 31";
        }
        String csiRsrp = String.valueOf(signalStrength.getCsiRsrp());
        String csiRsrq = String.valueOf(signalStrength.getCsiRsrq());
        String csiSinr = String.valueOf(signalStrength.getCsiSinr());
        String dbm = String.valueOf(signalStrength.getDbm());
        String level = String.valueOf(signalStrength.getLevel());
        String ssRsrp = String.valueOf(signalStrength.getSsRsrp());
        String ssRsrq = String.valueOf(signalStrength.getSsRsrq());
        String ssSinr = String.valueOf(signalStrength.getSsSinr());
        return new MyNetworkInfo.CellInfo.CellSignalStrengthNr(asuLevel, csicqiReport, csicqiTableIndex, csiRsrp, csiRsrq, csiSinr, dbm, level, ssRsrp, ssRsrq, ssSinr);
    }

    List<MyNetworkInfo.SubInfo> getSubInfo() {
        List<MyNetworkInfo.SubInfo> subInfoList = new ArrayList<>();
        SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (subscriptionManager != null) {
                int activeDataSubscriptionId = SubscriptionManager.getActiveDataSubscriptionId();
                Log.d("SimID", "当前活动 SIM 卡的 ID: " + activeDataSubscriptionId);
                subInfoList.add(new MyNetworkInfo.SubInfo(activeDataSubscriptionId, String.valueOf(activeDataSubscriptionId)));
                latch = new CountDownLatch(subInfoList.size());
                for (int i=0;i<subInfoList.size();i++) {
                    Reflection.unseal(context);
                    ServiceStateListener serviceStateListener = new ServiceStateListener(subInfoList.get(i));
                    TelephonyManager simTelephonyManager = telephonyManager.createForSubscriptionId(subInfoList.get(i).subId);
                    simTelephonyManager.listen(serviceStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
                }
            }
        }
        return subInfoList;
    }
}
