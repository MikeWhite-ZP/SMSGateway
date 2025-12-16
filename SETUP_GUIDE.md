# SMS Gateway Setup Guide

Complete guide to replace Twilio with your own Android phone SMS system.

## System Overview

This system consists of:
1. **SMS Server** - Python Flask API that manages SMS queue
2. **Android App** - Sends SMS using phone's SIM card
3. **Booking System Integration** - Python client to send SMS

## How It Works

1. Your booking system sends SMS request to the Flask server via API
2. The server queues the message
3. The Android app polls the server every 5 seconds for new messages
4. When messages are found, the app sends them using the phone's SIM card
5. Delivery status is reported back to the server

## Requirements

### Server Requirements
- Python 3.7+
- Flask
- Internet connection

### Android Phone Requirements
- Android 5.0 (API 21) or higher
- Active SIM card with SMS capability
- Internet connection (WiFi or mobile data)
- SMS permissions enabled

## Installation Steps

### Step 1: Setup SMS Server

The server is already running. Note the API token displayed in the console output.

### Step 2: Build Android App

1. **Install Android Studio** from https://developer.android.com/studio

2. **Open the project:**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the `android_app` folder

3. **Build APK:**
   - Build > Build Bundle(s) / APK(s) > Build APK(s)
   - APK will be in `app/build/outputs/apk/debug/`

4. **Install on Android phone:**
   - Transfer APK to phone
   - Enable "Install from Unknown Sources" in settings
   - Install the APK

### Step 3: Configure Android App

1. **Open the app** on your Android phone

2. **Grant permissions:**
   - Allow SMS permission when prompted
   - Allow phone state permission

3. **Enter server URL:**
   - Use your server's public URL (e.g., from Replit)
   - Format: `https://your-repl-name.replit.app`

4. **Enter Device Token:**
   - Copy the "Device Token" from the server console output
   - This authenticates your phone with the server

5. **Press "Connect"**
   - App will register with server
   - Status should show "Connected"

6. **Keep app running:**
   - App must stay open to send SMS
   - Consider disabling battery optimization for this app

### Step 4: Integrate with Booking System

```python
from sms_gateway_client import SMSGateway

sms = SMSGateway(
    server_url="https://your-server-url",
    api_token="YOUR_API_TOKEN"
)

# Send SMS
result = sms.send_sms(
    to_number="+1234567890",
    message="Your booking is confirmed!"
)
print(result)
```

## Usage Examples

### Send Booking Confirmation
```python
sms.send_sms(
    to_number="+1234567890",
    message="Hi John! Your haircut appointment is confirmed for Dec 17 at 2:00 PM."
)
```

### Send Reminder
```python
sms.send_sms(
    to_number="+1234567890",
    message="Reminder: Your appointment is in 1 hour. See you soon!"
)
```

## Monitoring

### Check Server Status
```python
status = sms.get_server_status()
print(status)  # {'status': 'ok', 'connected_devices': 1}
```

### List Connected Devices
```python
devices = sms.get_connected_devices()
print(devices)
```

### Get SMS History
```python
history = sms.get_sms_history()
print(history)
```

## Troubleshooting

### Android App Won't Connect
- Check server is running
- Verify server URL format (include https://)
- Ensure phone can reach server

### SMS Not Sending
- Check SMS permission granted
- Verify SIM card has SMS capability
- Check phone has signal
- Verify phone number format (international format with +)

### Server Returns 503 Error
- No devices connected
- Restart Android app and reconnect

### API Returns 401 Unauthorized
- Check API token is correct
- Verify Authorization header format: `Bearer YOUR_TOKEN`

## Cost Savings

Compared to Twilio:
- **Twilio**: $0.0079 per SMS (US)
- **This System**: Only SIM card costs (typically $10-30/month unlimited)

For 1000 SMS/month:
- Twilio: ~$8/month
- This system: ~$20/month (unlimited)

**Break-even point**: ~2,500 SMS/month

## SMS Limits

Check your carrier's SMS limits:
- Most carriers: 100-200 SMS/hour
- Daily limits: 500-1000 SMS/day
- Exceeding limits may flag account as spam

## Security Recommendations

1. Use HTTPS in production
2. Store API token in environment variables
3. Monitor usage to prevent abuse
