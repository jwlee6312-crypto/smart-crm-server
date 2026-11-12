import urllib.request
import json

url = 'http://localhost:8090/api/crm/asterisk/script/save'
data = json.dumps([{'id': '01_welcome', 'script_text': '안녕하세요 씨알엠뱅크입니다'}]).encode('utf-8')
req = urllib.request.Request(url, data=data, headers={'Content-Type': 'application/json'}, method='POST')

try:
    with urllib.request.urlopen(req) as response:
        print("Response status:", response.status)
        print("Response body:", response.read().decode())
except Exception as e:
    print("Error:", e)
