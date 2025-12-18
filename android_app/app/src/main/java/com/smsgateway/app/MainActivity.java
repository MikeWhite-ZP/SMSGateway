package com.smsgateway.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 123;
    private EditText serverUrlInput;
    private EditText deviceTokenInput;
    private Button connectButton;
    private TextView statusText;
    private TextView smsCountText;
    private Handler handler;
    private boolean isConnected = false;
    private String serverUrl = "";
    private String deviceToken = "";
    private String deviceId = "";
    private String phoneNumber = "";
    private int smsSentCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serverUrlInput = findViewById(R.id.serverUrlInput);
        deviceTokenInput = findViewById(R.id.deviceTokenInput);
        connectButton = findViewById(R.id.connectButton);
        statusText = findViewById(R.id.statusText);
        smsCountText = findViewById(R.id.smsCountText);
        handler = new Handler(Looper.getMainLooper());

        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        connectButton.setOnClickListener(v -> {
            if (!isConnected) {
                checkPermissionsAndConnect();
            } else {
                disconnect();
            }
        });

        updateUI();
    }

    private void checkPermissionsAndConnect() {
        ArrayList<String> permissionsNeeded = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.SEND_SMS);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_PHONE_NUMBERS);
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            connect();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                connect();
            } else {
                Toast.makeText(this, "SMS permission required", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void connect() {
        serverUrl = serverUrlInput.getText().toString().trim();
        deviceToken = deviceTokenInput.getText().toString().trim();
        
        if (serverUrl.isEmpty()) {
            Toast.makeText(this, "Please enter server URL", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (deviceToken.isEmpty()) {
            Toast.makeText(this, "Please enter device token", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
            serverUrl = "http://" + serverUrl;
        }

        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED) {
                phoneNumber = tm.getLine1Number();
                if (phoneNumber == null || phoneNumber.isEmpty()) {
                    phoneNumber = "Unknown";
                }
            } else {
                phoneNumber = "Unknown";
            }
        } catch (Exception e) {
            phoneNumber = "Unknown";
        }

        registerDevice();
    }

    private void disconnect() {
        isConnected = false;
        handler.removeCallbacksAndMessages(null);
        updateUI();
    }

    private void registerDevice() {
        new Thread(() -> {
            try {
                URL url = new URL(serverUrl + "/device/register");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + deviceToken);
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("device_id", deviceId);
                json.put("phone_number", phoneNumber);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes());
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    runOnUiThread(() -> {
                        isConnected = true;
                        updateUI();
                        startPolling();
                        Toast.makeText(this, "Connected successfully", Toast.LENGTH_SHORT).show();
                    });
                } else if (responseCode == 401) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Invalid device token", Toast.LENGTH_LONG).show();
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Connection failed: " + responseCode, Toast.LENGTH_LONG).show();
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void startPolling() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isConnected) {
                    pollMessages();
                    handler.postDelayed(this, 5000);
                }
            }
        }, 5000);
    }

    private void pollMessages() {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(serverUrl + "/device/poll");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + deviceToken);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("device_id", deviceId);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes());
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject responseJson = new JSONObject(response.toString());
                    if (responseJson.has("messages")) {
                        JSONArray messages = responseJson.getJSONArray("messages");
                        for (int i = 0; i < messages.length(); i++) {
                            JSONObject msg = messages.getJSONObject(i);
                            String msgId = msg.getString("id");
                            String to = msg.getString("to");
                            String message = msg.getString("message");
                            sendSMS(msgId, to, message);
                        }
                    }
                } else if (responseCode == 401 || responseCode == 403) {
                    runOnUiThread(() -> {
                        isConnected = false;
                        handler.removeCallbacksAndMessages(null);
                        updateUI();
                        Toast.makeText(this, "Authentication failed - please reconnect", Toast.LENGTH_LONG).show();
                    });
                } else if (responseCode >= 500) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Server error: " + responseCode, Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Connection error - retrying...", Toast.LENGTH_SHORT).show();
                });
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    private void sendSMS(String msgId, String to, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            
            if (message.length() > 160) {
                ArrayList<String> parts = smsManager.divideMessage(message);
                smsManager.sendMultipartTextMessage(to, null, parts, null, null);
            } else {
                smsManager.sendTextMessage(to, null, message, null, null);
            }

            smsSentCount++;
            runOnUiThread(() -> updateUI());
            reportStatus(msgId, "sent");

        } catch (Exception e) {
            reportStatus(msgId, "failed");
            e.printStackTrace();
        }
    }

    private void reportStatus(String msgId, String status) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(serverUrl + "/device/status");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + deviceToken);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("message_id", msgId);
                json.put("status", status);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes());
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == 401 || responseCode == 403) {
                    runOnUiThread(() -> {
                        isConnected = false;
                        handler.removeCallbacksAndMessages(null);
                        updateUI();
                        Toast.makeText(this, "Authentication failed - please reconnect", Toast.LENGTH_LONG).show();
                    });
                } else if (responseCode >= 500) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to report status: " + responseCode, Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error reporting status", Toast.LENGTH_SHORT).show();
                });
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    private void updateUI() {
        if (isConnected) {
            statusText.setText("Status: Connected");
            statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            connectButton.setText("Disconnect");
            serverUrlInput.setEnabled(false);
            deviceTokenInput.setEnabled(false);
        } else {
            statusText.setText("Status: Disconnected");
            statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            connectButton.setText("Connect");
            serverUrlInput.setEnabled(true);
            deviceTokenInput.setEnabled(true);
        }
        smsCountText.setText("SMS Sent: " + smsSentCount);
    }
}
