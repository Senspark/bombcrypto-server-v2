#!/bin/bash
# Kill the SFS Java process directly. The entrypoint loop will restart it.
pkill -9 -f 'com.smartfoxserver.v2.Main' 2>/dev/null

# Wait for the Java process to fully exit and release ports
while pgrep -f 'com.smartfoxserver.v2.Main' > /dev/null 2>&1; do
  sleep 0.5
done
echo "SFS process killed, restarting..."
