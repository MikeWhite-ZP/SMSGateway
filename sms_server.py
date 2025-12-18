from flask import Flask, request, jsonify
from datetime import datetime, timedelta
import queue
import secrets
import os

app = Flask(__name__)

connected_devices = {}
sms_queue = []
pending_messages = {}
device_tokens = {}

API_TOKEN = os.environ.get('SMS_API_TOKEN', secrets.token_urlsafe(32))
DEVICE_TOKEN = os.environ.get('DEVICE_TOKEN', secrets.token_urlsafe(32))

MESSAGE_TIMEOUT_SECONDS = 60

class DeviceConnection:
    def __init__(self, device_id, phone_number):
        self.device_id = device_id
        self.phone_number = phone_number
        self.last_seen = datetime.now()
        self.status = "online"

def verify_device_token(request):
    auth_header = request.headers.get('Authorization')
    return auth_header == f"Bearer {DEVICE_TOKEN}"

def get_available_message(device_id):
    now = datetime.now()
    for msg in sms_queue:
        if msg.get('in_progress'):
            assigned_at = datetime.fromisoformat(msg.get('assigned_at', now.isoformat()))
            if (now - assigned_at).total_seconds() > MESSAGE_TIMEOUT_SECONDS:
                msg['in_progress'] = False
                msg['assigned_to'] = None
            else:
                continue
        
        if msg.get('assigned_to') is None:
            msg['assigned_to'] = device_id
            msg['assigned_at'] = now.isoformat()
            msg['in_progress'] = True
            return msg
    return None

@app.route('/', methods=['GET'])
def index():
    return jsonify({
        "service": "SMS Gateway",
        "status": "running",
        "endpoints": {
            "health": "/health",
            "queue_status": "/queue/status",
            "send_sms": "POST /sms/send",
            "devices": "/devices"
        }
    })

@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({"status": "ok", "connected_devices": len(connected_devices)})

@app.route('/device/register', methods=['POST'])
def register_device():
    if not verify_device_token(request):
        return jsonify({"error": "Unauthorized - invalid device token"}), 401
    
    data = request.json
    device_id = data.get('device_id')
    phone_number = data.get('phone_number')
    
    if not device_id or not phone_number:
        return jsonify({"error": "Missing device_id or phone_number"}), 400
    
    connected_devices[device_id] = DeviceConnection(device_id, phone_number)
    
    return jsonify({
        "status": "registered",
        "device_id": device_id,
        "message": "Device registered successfully"
    })

@app.route('/device/poll', methods=['POST'])
def poll_messages():
    if not verify_device_token(request):
        return jsonify({"error": "Unauthorized - invalid device token"}), 401
    
    data = request.json
    device_id = data.get('device_id')
    
    if not device_id or device_id not in connected_devices:
        return jsonify({"error": "Device not registered"}), 401
    
    connected_devices[device_id].last_seen = datetime.now()
    
    messages = []
    msg = get_available_message(device_id)
    if msg:
        msg_id = msg.get('id')
        if not msg_id:
            msg_id = secrets.token_urlsafe(16)
            msg['id'] = msg_id
        pending_messages[msg_id] = msg
        messages.append({
            "id": msg_id,
            "to": msg["to"],
            "message": msg["message"]
        })
    
    return jsonify({"messages": messages})

@app.route('/device/status', methods=['POST'])
def update_status():
    if not verify_device_token(request):
        return jsonify({"error": "Unauthorized - invalid device token"}), 401
    
    data = request.json
    msg_id = data.get('message_id')
    status = data.get('status')
    
    if msg_id in pending_messages:
        pending_messages[msg_id]['status'] = status
        pending_messages[msg_id]['updated_at'] = datetime.now().isoformat()
        
        if status in ['sent', 'delivered']:
            sms_queue[:] = [m for m in sms_queue if m.get('id') != msg_id]
        elif status == 'failed':
            for m in sms_queue:
                if m.get('id') == msg_id:
                    m['assigned_to'] = None
                    m['in_progress'] = False
                    m['retry_count'] = m.get('retry_count', 0) + 1
                    break
    
    return jsonify({"status": "updated"})

@app.route('/sms/send', methods=['POST'])
def send_sms():
    auth_header = request.headers.get('Authorization')
    
    if auth_header != f"Bearer {API_TOKEN}":
        return jsonify({"error": "Unauthorized"}), 401
    
    data = request.json
    to_number = data.get('to')
    message = data.get('message')
    
    if not to_number or not message:
        return jsonify({"error": "Missing 'to' or 'message' field"}), 400
    
    if not connected_devices:
        return jsonify({"error": "No devices connected"}), 503
    
    sms_data = {
        "id": secrets.token_urlsafe(16),
        "to": to_number,
        "message": message,
        "created_at": datetime.now().isoformat(),
        "status": "queued",
        "assigned_to": None,
        "retry_count": 0
    }
    sms_queue.append(sms_data)
    
    return jsonify({
        "status": "queued",
        "message": "SMS queued for delivery",
        "id": sms_data["id"]
    })

@app.route('/sms/history', methods=['GET'])
def sms_history():
    auth_header = request.headers.get('Authorization')
    
    if auth_header != f"Bearer {API_TOKEN}":
        return jsonify({"error": "Unauthorized"}), 401
    
    history = []
    for msg_id, msg in pending_messages.items():
        history.append({
            "id": msg_id,
            "to": msg.get("to"),
            "message": msg.get("message"),
            "status": msg.get("status"),
            "created_at": msg.get("created_at"),
            "updated_at": msg.get("updated_at")
        })
    
    return jsonify({"messages": history})

@app.route('/devices', methods=['GET'])
def list_devices():
    auth_header = request.headers.get('Authorization')
    
    if auth_header != f"Bearer {API_TOKEN}":
        return jsonify({"error": "Unauthorized"}), 401
    
    devices = []
    for device_id, conn in connected_devices.items():
        devices.append({
            "device_id": device_id,
            "phone_number": conn.phone_number,
            "last_seen": conn.last_seen.isoformat(),
            "status": conn.status
        })
    return jsonify({"devices": devices})

@app.route('/queue/status', methods=['GET'])
def queue_status():
    return jsonify({
        "queue_size": len(sms_queue),
        "pending_messages": len(pending_messages),
        "connected_devices": len(connected_devices)
    })

if __name__ == '__main__':
    print(f"\n{'='*50}")
    print(f"SMS SERVER STARTED")
    print(f"{'='*50}")
    print(f"API Token: {API_TOKEN}")
    print(f"Device Token: {DEVICE_TOKEN}")
    print(f"Save these tokens!")
    print(f"{'='*50}\n")
    app.run(host='0.0.0.0', port=5000, debug=True)
