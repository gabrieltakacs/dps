#!/usr/bin/env bash
cp /dynamo/build/libs/*.war /opt/tomcat/webapps/
/opt/tomcat/bin/catalina.sh start
touch /var/log/keepalive
tail -f /var/log/keepalive