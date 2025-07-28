package com.example.permissionapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    
    // 权限常量
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] PERMISSIONS = {
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO
    };
    
    // 权限说明
    private static final Map<String, String> PERMISSION_DESCRIPTIONS = new HashMap<String, String>() {{
        put(Manifest.permission.READ_CONTACTS, "通讯录权限 - 用于同步联系人到云端");
        put(Manifest.permission.ACCESS_FINE_LOCATION, "位置权限 - 用于提供基于位置的服务");
        put(Manifest.permission.CAMERA, "相机权限 - 用于扫描二维码和拍照");
        put(Manifest.permission.READ_EXTERNAL_STORAGE, "存储权限 - 用于保存应用数据");
        put(Manifest.permission.RECORD_AUDIO, "麦克风权限 - 用于语音通话和语音输入");
    }};
    
    // 权限状态
    private Map<String, Boolean> permissionStatus = new HashMap<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeViews();
        checkCurrentPermissions();
    }
    
    private void initializeViews() {
        // 设置标题
        TextView titleText = findViewById(R.id.title_text);
        titleText.setText("权限管理");
        
        // 设置说明文字
        TextView infoText = findViewById(R.id.info_text);
        infoText.setText("请仔细阅读每个权限的用途说明，然后选择是否授权。\n您可以随时在设置中撤销这些权限。");
        
        // 权限按钮
        setupPermissionButtons();
        
        // 底部按钮
        setupBottomButtons();
    }
    
    private void setupPermissionButtons() {
        // 通讯录权限
        Button contactsButton = findViewById(R.id.contacts_button);
        contactsButton.setOnClickListener(v -> requestPermission(Manifest.permission.READ_CONTACTS, "通讯录权限"));
        
        // 位置权限
        Button locationButton = findViewById(R.id.location_button);
        locationButton.setOnClickListener(v -> requestPermission(Manifest.permission.ACCESS_FINE_LOCATION, "位置权限"));
        
        // 相机权限
        Button cameraButton = findViewById(R.id.camera_button);
        cameraButton.setOnClickListener(v -> requestPermission(Manifest.permission.CAMERA, "相机权限"));
        
        // 存储权限
        Button storageButton = findViewById(R.id.storage_button);
        storageButton.setOnClickListener(v -> requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, "存储权限"));
        
        // 麦克风权限
        Button microphoneButton = findViewById(R.id.microphone_button);
        microphoneButton.setOnClickListener(v -> requestPermission(Manifest.permission.RECORD_AUDIO, "麦克风权限"));
    }
    
    private void setupBottomButtons() {
        // 全部授权按钮
        Button grantAllButton = findViewById(R.id.grant_all_button);
        grantAllButton.setOnClickListener(v -> grantAllPermissions());
        
        // 拒绝全部按钮
        Button denyAllButton = findViewById(R.id.deny_all_button);
        denyAllButton.setOnClickListener(v -> denyAllPermissions());
        
        // 查看设置按钮
        Button settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(v -> openAppSettings());
        
        // 查看数据按钮
        Button viewDataButton = findViewById(R.id.view_data_button);
        viewDataButton.setOnClickListener(v -> showCollectedData());
    }
    
    private void checkCurrentPermissions() {
        for (String permission : PERMISSIONS) {
            boolean granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
            permissionStatus.put(permission, granted);
            updatePermissionButton(permission, granted);
        }
    }
    
    private void updatePermissionButton(String permission, boolean granted) {
        int buttonId = getButtonIdForPermission(permission);
        if (buttonId != 0) {
            Button button = findViewById(buttonId);
            button.setText(granted ? "已授权" : "未授权");
            button.setBackgroundColor(granted ? getResources().getColor(android.R.color.holo_green_light) : 
                                              getResources().getColor(android.R.color.holo_red_light));
        }
    }
    
    private int getButtonIdForPermission(String permission) {
        switch (permission) {
            case Manifest.permission.READ_CONTACTS:
                return R.id.contacts_button;
            case Manifest.permission.ACCESS_FINE_LOCATION:
                return R.id.location_button;
            case Manifest.permission.CAMERA:
                return R.id.camera_button;
            case Manifest.permission.READ_EXTERNAL_STORAGE:
                return R.id.storage_button;
            case Manifest.permission.RECORD_AUDIO:
                return R.id.microphone_button;
            default:
                return 0;
        }
    }
    
    private void requestPermission(String permission, String permissionName) {
        // 检查是否已有权限
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            showPermissionDialog(permission, permissionName, "您已经授权了" + permissionName + "。\n\n您可以在设置中撤销此权限。");
            return;
        }
        
        // 显示权限说明对话框
        showPermissionExplanationDialog(permission, permissionName);
    }
    
    private void showPermissionExplanationDialog(String permission, String permissionName) {
        String description = PERMISSION_DESCRIPTIONS.get(permission);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(permissionName + "说明")
               .setMessage(description + "\n\n" +
                          "• 我们只会用于声明目的\n" +
                          "• 数据会加密存储\n" +
                          "• 不会分享给第三方\n" +
                          "• 您可以随时撤销\n\n" +
                          "是否授权此权限？")
               .setPositiveButton("授权", (dialog, which) -> {
                   // 请求系统权限
                   ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQUEST_CODE);
               })
               .setNegativeButton("拒绝", (dialog, which) -> {
                   showPermissionDialog(permission, permissionName, "您已拒绝" + permissionName + "。\n\n相关功能将无法使用。\n您可以稍后在设置中重新授权。");
               })
               .setNeutralButton("稍后询问", (dialog, which) -> {
                   Toast.makeText(this, "我们稍后会再次询问此权限", Toast.LENGTH_SHORT).show();
               })
               .show();
    }
    
    private void showPermissionDialog(String permission, String permissionName, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(permissionName)
               .setMessage(message)
               .setPositiveButton("确定", null)
               .show();
    }
    
    private void grantAllPermissions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("授权所有权限")
               .setMessage("确定要授权所有权限吗？\n\n" +
                          "这将允许应用访问：\n" +
                          "• 您的通讯录\n" +
                          "• 您的位置信息\n" +
                          "• 您的相机\n" +
                          "• 您的存储空间\n" +
                          "• 您的麦克风\n\n" +
                          "您可以随时在设置中撤销这些权限。")
               .setPositiveButton("确定", (dialog, which) -> {
                   ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE);
               })
               .setNegativeButton("取消", null)
               .show();
    }
    
    private void denyAllPermissions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("拒绝所有权限")
               .setMessage("确定要拒绝所有权限吗？\n\n" +
                          "这将限制应用的功能：\n" +
                          "• 无法同步联系人\n" +
                          "• 无法提供位置服务\n" +
                          "• 无法使用相机功能\n" +
                          "• 无法保存文件\n" +
                          "• 无法使用语音功能\n\n" +
                          "您可以稍后在设置中重新授权。")
               .setPositiveButton("确定", (dialog, which) -> {
                   Toast.makeText(this, "所有权限已拒绝", Toast.LENGTH_SHORT).show();
               })
               .setNegativeButton("取消", null)
               .show();
    }
    
    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }
    
    private void showCollectedData() {
        StringBuilder data = new StringBuilder();
        data.append("=== 收集的数据 ===\n\n");
        
        for (String permission : PERMISSIONS) {
            String permissionName = getPermissionDisplayName(permission);
            boolean granted = permissionStatus.getOrDefault(permission, false);
            data.append(permissionName).append(": ").append(granted ? "已授权" : "未授权").append("\n");
            
            if (granted) {
                data.append("  用途: ").append(PERMISSION_DESCRIPTIONS.get(permission)).append("\n");
                data.append("  数据保留: 根据用途而定\n");
            }
            data.append("\n");
        }
        
        data.append("=== 隐私保护 ===\n\n");
        data.append("• 所有数据都经过加密存储\n");
        data.append("• 不会分享给第三方\n");
        data.append("• 您可以随时撤销权限\n");
        data.append("• 数据保留时间有限\n");
        data.append("• 符合GDPR等隐私法规\n");
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("收集的数据")
               .setMessage(data.toString())
               .setPositiveButton("确定", null)
               .show();
    }
    
    private String getPermissionDisplayName(String permission) {
        switch (permission) {
            case Manifest.permission.READ_CONTACTS:
                return "通讯录权限";
            case Manifest.permission.ACCESS_FINE_LOCATION:
                return "位置权限";
            case Manifest.permission.CAMERA:
                return "相机权限";
            case Manifest.permission.READ_EXTERNAL_STORAGE:
                return "存储权限";
            case Manifest.permission.RECORD_AUDIO:
                return "麦克风权限";
            default:
                return permission;
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                permissionStatus.put(permission, granted);
                updatePermissionButton(permission, granted);
                
                String permissionName = getPermissionDisplayName(permission);
                if (granted) {
                    Toast.makeText(this, permissionName + "已授权", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, permissionName + "被拒绝", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
} 