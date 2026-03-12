#!/bin/bash

echo "SERVER_NAME=$SERVER_NAME"

echo "Patch"
cp -rf /opt/SmartFoxServer_2X_Patch/"$GCS_PATH"* /opt/SmartFoxServer_2X

# Giải thích xmlstarlet:
# xmlstarlet ed -L -P : sửa value (ed), ko tạo file mới (-L), giữa formatting như cũ (-P)
# -u <node_path>: node cần sửa
# -v <new_value>: giá trị cần sửa
# -s <node_path> -t elem -n <new_node_name> -v <new_value>: thêm node mới
# //<node_name> : tìm node trong toàn bộ file
# /<node_name> : tìm node ở root level

echo "Update server config"

IFS=',' read -r ADMIN_LOGIN ADMIN_PASS <<< "$CREDENTIALS"

xmlstarlet ed -L -P \
  -u "//serverSettings/ipFilter/maxConnectionsPerAddress" -v "$MAX_CONNECTIONS_PER_IP" \
  -u "//serverSettings/webServer/isActive" -v "true" \
  -u "//serverSettings/webServer/isXForwardedForActive" -v "true" \
  -u "//serverSettings/serverName" -v "$SERVER_NAME" \
  -u "//serverSettings/licenseCode" -v "$LICENSE_CODE" \
  -s "//serverSettings/ipFilter[not(maxConnectionsPerAddress)]" -t elem -n maxConnectionsPerAddress -v "$MAX_CONNECTIONS_PER_IP" \
  -s "//serverSettings/webServer[not(isActive)]" -t elem -n isActive -v "true" \
  -s "//serverSettings/webServer[not(isXForwardedForActive)]" -t elem -n isXForwardedForActive -v "true" \
  -s "//serverSettings[not(serverName)]" -t elem -n serverName -v "$SERVER_NAME" \
  -s "//serverSettings[not(licenseCode)]" -t elem -n licenseCode -v "$LICENSE_CODE" \
  -d "//serverSettings/remoteAdmin/administrators/adminUser" \
  -s "//serverSettings/remoteAdmin/administrators" -t elem -n adminUser -v "" \
  -s "//serverSettings/remoteAdmin/administrators/adminUser" -t elem -n login -v "$ADMIN_LOGIN" \
  -s "//serverSettings/remoteAdmin/administrators/adminUser" -t elem -n password -v "$ADMIN_PASS" \
  -s "//serverSettings/remoteAdmin/administrators/adminUser" -t elem -n allowHalt -v "true" \
  -s "//serverSettings/remoteAdmin/administrators/adminUser" -t elem -n disabledModules -v "" \
  config/server.xml


echo "Update apache config"
xmlstarlet ed -L -P \
    -u "//Connector[@name='sfs-http']/@port" -v "$HTTP_PORT" \
    -u "//Connector[@name='sfs-https']/@port" -v "$HTTPS_PORT" \
    -u //Connector/@maxConnections -v "$MAX_CCU" \
    lib/apache-tomcat/conf/server.xml

if [ "$IS_PVP_SERVER" -ne 1 ]; then
    echo "Not a pvp server"
    rm /opt/SmartFoxServer_2X/SFS2X/zones/PVPZone.zone.xml
else
    echo "Update pvp zone config"
    xmlstarlet ed -L -P \
        -u /zone/maxUsers -v "$MAX_CCU" \
        zones/PVPZone.zone.xml
    echo "Disable nagle algorithm"
    xmlstarlet ed -L -P \
        -u /tcpNoDelay -v true \
        config/core.xml
fi

if [ "$IS_GAME_SERVER" -ne 1 ]; then
    echo "Not a game server"
    rm /opt/SmartFoxServer_2X/SFS2X/zones/BomberGameZone.zone.xml
    
else
    echo "Update main zone config"
    xmlstarlet ed -L -P \
        -u /zone/maxUsers -v "$MAX_CCU" \
        zones/BomberGameZone.zone.xml
fi

echo "Disable Tomcat JAR scanning"
xmlstarlet ed -L \
    -s "/Context[not(JarScanner)]" -t elem -n JarScanner -v "" \
    -i "//JarScanner[not(@scanClassPath)]" -t attr -n scanClassPath -v "false" \
    -i "//JarScanner[not(@scanManifest)]" -t attr -n scanManifest -v "false" \
    lib/apache-tomcat/conf/context.xml

echo "Start server"

if [ "$IS_DEBUG" -eq 1 ]; then
  # https://smartfoxserver.com/blog/how-to-debug-your-extensions/
  echo "Run debug mode"
  sh sfs2x_debug.sh
else
  sh sfs2x.sh
fi