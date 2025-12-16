import requests
from typing import Optional, Dict, Any

class SMSGateway:
    def __init__(self, server_url: str, api_token: str):
        self.server_url = server_url.rstrip('/')
        self.api_token = api_token
        self.headers = {
            "Authorization": f"Bearer {api_token}",
            "Content-Type": "application/json"
        }
    
    def send_sms(self, to_number: str, message: str) -> Dict[str, Any]:
        response = requests.post(
            f"{self.server_url}/sms/send",
            headers=self.headers,
            json={"to": to_number, "message": message}
        )
        return response.json()
    
    def get_server_status(self) -> Dict[str, Any]:
        response = requests.get(f"{self.server_url}/health")
        return response.json()
    
    def get_connected_devices(self) -> Dict[str, Any]:
        response = requests.get(f"{self.server_url}/devices")
        return response.json()
    
    def get_queue_status(self) -> Dict[str, Any]:
        response = requests.get(f"{self.server_url}/queue/status")
        return response.json()
    
    def get_sms_history(self) -> Dict[str, Any]:
        response = requests.get(
            f"{self.server_url}/sms/history",
            headers=self.headers
        )
        return response.json()


if __name__ == "__main__":
    print("SMS Gateway Client Library")
    print("=" * 40)
    print("\nUsage Example:")
    print("-" * 40)
    print("""
from sms_gateway_client import SMSGateway

sms = SMSGateway(
    server_url="http://YOUR_SERVER_IP:5000",
    api_token="YOUR_API_TOKEN"
)

result = sms.send_sms(
    to_number="+1234567890",
    message="Your booking is confirmed!"
)
print(result)

status = sms.get_server_status()
print(status)

devices = sms.get_connected_devices()
print(devices)
""")
