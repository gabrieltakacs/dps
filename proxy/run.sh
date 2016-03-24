#!/usr/bin/env bash

/opt/tomcat/bin/catalina.sh start
touch /var/log/keepalive
tail -f /var/log/keepalive