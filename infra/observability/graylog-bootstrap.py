import os, json, base64, time, urllib.request
auth=base64.b64encode(("admin:"+os.environ["GRAYLOG_ADMIN_PASSWORD"]).encode()).decode()
headers={"Authorization":"Basic "+auth,"X-Requested-By":"cinepass-bootstrap","Content-Type":"application/json"}
url="http://graylog:9000/api/system/inputs"
for attempt in range(180):
    try:
        with urllib.request.urlopen(urllib.request.Request(url,headers=headers),timeout=5) as response:
            existing=json.load(response).get("inputs",[])
        if not any(item.get("title")=="CinePass GELF UDP" for item in existing):
            body={"title":"CinePass GELF UDP","type":"org.graylog2.inputs.gelf.udp.GELFUDPInput","global":True,
                  "configuration":{"bind_address":"0.0.0.0","port":12201,"recv_buffer_size":1048576,
                  "number_worker_threads":2,"decompress_size_limit":8388608,"override_source":None}}
            with urllib.request.urlopen(urllib.request.Request(url,data=json.dumps(body).encode(),headers=headers),timeout=10): pass
        print("CinePass GELF UDP input ready")
        break
    except Exception:
        time.sleep(2)
else:
    raise SystemExit("Graylog not ready or authentication failed; check its health and configured credentials")
