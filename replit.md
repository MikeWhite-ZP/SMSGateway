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
└── SETUP_GUIDE.md         # Complete setup instructions
```

## Running the Server

```bash
python sms_server.py
```

The server runs on port 5000 and displays an API token on startup.

## API Endpoints

- `GET /health` - Server health check
- `POST /sms/send` - Send SMS (requires Bearer token)
- `GET /devices` - List connected Android devices
- `GET /queue/status` - Queue statistics
- `GET /sms/history` - Message history (requires Bearer token)
- `POST /device/register` - Device registration (Android app)
- `POST /device/poll` - Poll for messages (Android app)
- `POST /device/status` - Update delivery status (Android app)

## Environment Variables

- `SMS_API_TOKEN` - Custom API token (auto-generated if not set)

## Recent Changes

- December 16, 2025: Initial project setup with Flask server, Android app, and Python client
