package com.example.permissionapp;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.util.Log;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 数据收集服务
 * 仅在用户明确授权的情况下收集数据
 */
public class DataCollectionService extends Service {
    
    private static final String TAG = "DataCollectionService";
    private static final String DATA_FILE = "collected_data.json";
    
    // 数据收集状态
    private Map<String, Boolean> collectionStatus = new HashMap<>();
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "数据收集服务启动");
        
        // 检查用户授权状态
        if (intent != null && intent.hasExtra("permission_granted")) {
            String permission = intent.getStringExtra("permission");
            boolean granted = intent.getBooleanExtra("permission_granted", false);
            
            if (granted) {
                collectData(permission);
            }
        }
        
        return START_NOT_STICKY;
    }
    
    /**
     * 根据权限收集数据
     */
    private void collectData(String permission) {
        switch (permission) {
            case "android.permission.READ_CONTACTS":
                collectContactsData();
                break;
            case "android.permission.ACCESS_FINE_LOCATION":
                collectLocationData();
                break;
            case "android.permission.CAMERA":
                // 相机权限通常不收集数据，仅用于实时功能
                Log.d(TAG, "相机权限 - 不收集数据，仅用于实时功能");
                break;
            case "android.permission.READ_EXTERNAL_STORAGE":
                collectStorageData();
                break;
            case "android.permission.RECORD_AUDIO":
                // 麦克风权限通常不收集数据，仅用于实时功能
                Log.d(TAG, "麦克风权限 - 不收集数据，仅用于实时功能");
                break;
        }
    }
    
    /**
     * 收集通讯录数据（仅收集数量，不收集具体信息）
     */
    private void collectContactsData() {
        try {
            Cursor cursor = getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                new String[]{ContactsContract.Contacts._ID},
                null, null, null
            );
            
            if (cursor != null) {
                int contactCount = cursor.getCount();
                cursor.close();
                
                // 只记录联系人数量，不收集具体信息
                Map<String, Object> data = new HashMap<>();
                data.put("permission", "READ_CONTACTS");
                data.put("data_type", "contact_count");
                data.put("value", contactCount);
                data.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
                data.put("user_consent", true);
                
                saveDataToFile(data);
                Log.d(TAG, "收集通讯录数据: 联系人数量 = " + contactCount);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "没有通讯录权限: " + e.getMessage());
        }
    }
    
    /**
     * 收集位置数据（仅收集大致位置，不精确追踪）
     */
    private void collectLocationData() {
        try {
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            
            if (locationManager != null && 
                checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                
                if (location != null) {
                    // 只记录大致位置（精确到城市级别），不精确追踪
                    Map<String, Object> data = new HashMap<>();
                    data.put("permission", "ACCESS_FINE_LOCATION");
                    data.put("data_type", "approximate_location");
                    data.put("latitude_rounded", Math.round(location.getLatitude() * 100.0) / 100.0);
                    data.put("longitude_rounded", Math.round(location.getLongitude() * 100.0) / 100.0);
                    data.put("accuracy", location.getAccuracy());
                    data.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
                    data.put("user_consent", true);
                    
                    saveDataToFile(data);
                    Log.d(TAG, "收集位置数据: 大致位置已记录");
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "没有位置权限: " + e.getMessage());
        }
    }
    
    /**
     * 收集存储数据（仅收集应用数据信息）
     */
    private void collectStorageData() {
        try {
            File appDir = getApplicationContext().getFilesDir();
            long appDataSize = getDirectorySize(appDir);
            
            Map<String, Object> data = new HashMap<>();
            data.put("permission", "READ_EXTERNAL_STORAGE");
            data.put("data_type", "app_storage_info");
            data.put("app_data_size_bytes", appDataSize);
            data.put("app_data_size_mb", appDataSize / (1024 * 1024));
            data.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            data.put("user_consent", true);
            
            saveDataToFile(data);
            Log.d(TAG, "收集存储数据: 应用数据大小 = " + (appDataSize / (1024 * 1024)) + " MB");
        } catch (Exception e) {
            Log.e(TAG, "收集存储数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 计算目录大小
     */
    private long getDirectorySize(File directory) {
        long size = 0;
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    } else if (file.isDirectory()) {
                        size += getDirectorySize(file);
                    }
                }
            }
        }
        return size;
    }
    
    /**
     * 保存数据到文件
     */
    private void saveDataToFile(Map<String, Object> data) {
        try {
            File dataFile = new File(getApplicationContext().getFilesDir(), DATA_FILE);
            
            // 创建JSON格式的数据
            StringBuilder jsonData = new StringBuilder();
            jsonData.append("{\n");
            jsonData.append("  \"permission\": \"").append(data.get("permission")).append("\",\n");
            jsonData.append("  \"data_type\": \"").append(data.get("data_type")).append("\",\n");
            
            // 添加具体数据
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                if (!key.equals("permission") && !key.equals("data_type")) {
                    if (value instanceof String) {
                        jsonData.append("  \"").append(key).append("\": \"").append(value).append("\",\n");
                    } else {
                        jsonData.append("  \"").append(key).append("\": ").append(value).append(",\n");
                    }
                }
            }
            
            // 移除最后一个逗号
            if (jsonData.charAt(jsonData.length() - 2) == ',') {
                jsonData.setLength(jsonData.length() - 2);
                jsonData.append("\n");
            }
            
            jsonData.append("}\n");
            
            // 写入文件
            FileWriter writer = new FileWriter(dataFile, true); // 追加模式
            writer.write(jsonData.toString());
            writer.write("\n"); // 添加分隔符
            writer.close();
            
            Log.d(TAG, "数据已保存到文件: " + dataFile.getAbsolutePath());
            
        } catch (IOException e) {
            Log.e(TAG, "保存数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取收集的数据
     */
    public static String getCollectedData(android.content.Context context) {
        try {
            File dataFile = new File(context.getFilesDir(), DATA_FILE);
            if (dataFile.exists()) {
                java.util.Scanner scanner = new java.util.Scanner(dataFile);
                StringBuilder data = new StringBuilder();
                while (scanner.hasNextLine()) {
                    data.append(scanner.nextLine()).append("\n");
                }
                scanner.close();
                return data.toString();
            }
        } catch (IOException e) {
            Log.e(TAG, "读取数据失败: " + e.getMessage());
        }
        return "暂无收集的数据";
    }
    
    /**
     * 清除收集的数据
     */
    public static void clearCollectedData(android.content.Context context) {
        try {
            File dataFile = new File(context.getFilesDir(), DATA_FILE);
            if (dataFile.exists()) {
                dataFile.delete();
                Log.d(TAG, "收集的数据已清除");
            }
        } catch (Exception e) {
            Log.e(TAG, "清除数据失败: " + e.getMessage());
        }
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "数据收集服务已停止");
    }
} 