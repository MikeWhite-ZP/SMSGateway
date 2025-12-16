# SMS Gateway System

A complete SMS gateway to replace Twilio by sending SMS messages directly from an Android phone's SIM card.

## Overview

This system consists of:
1. **SMS Server** - Python Flask API that manages SMS queue
2. **Android App** - Sends SMS using phone's SIM card
3. **Python Client** - Easy integration with booking systems

## Project Structure

```
/
├── sms_server.py          # Main Flask API server
├── sms_gateway_client.py  # Python client library
├── android_app/           # Android app source code
│   └── app/src/main/
│       ├── java/...       # MainActivity.java
│       ├── res/layout/    # UI layout
│       └── AndroidManifest.xml
├── SETUP_GUIDE.md         # Complete setup instructions
└── replit.md              # Project documentation
```

## Running the Server

```bash
python sms_server.py
```

The server runs on port 5000 and displays two tokens on startup:
- **API Token** - For booking system integration
- **Device Token** - For Android app authentication

## API Endpoints

### Public Endpoints
- `GET /health` - Server health check
- `GET /queue/status` - Queue statistics

### Booking System Endpoints (requires API Token)
- `POST /sms/send` - Send SMS
- `GET /sms/history` - Message history
- `GET /devices` - List connected devices

### Device Endpoints (requires Device Token)
- `POST /device/register` - Device registration
- `POST /device/poll` - Poll for messages
- `POST /device/status` - Update delivery status

## Security Features

- API Token authentication for booking system endpoints
- Device Token authentication for Android app endpoints
- Single-delivery guarantee with in_progress flag
- Timeout-based message reassignment (60 seconds)
- Retry handling for failed messages

## Environment Variables

- `SMS_API_TOKEN` - Custom API token (auto-generated if not set)
- `DEVICE_TOKEN` - Custom device token (auto-generated if not set)

## Recent Changes

- December 16, 2025: Initial project setup with Flask server, Android app, and Python client
- December 16, 2025: Added device token authentication for security
- December 16, 2025: Fixed duplicate delivery issue with in_progress flag
