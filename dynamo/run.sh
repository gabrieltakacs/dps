#!/usr/bin/env bash

/opt/tomcat/bin/catalina.sh start
syslogd
${FILEBEAT_HOME}/filebeat
touch /var/log/keepalive
tail -f /var/log/keepalive