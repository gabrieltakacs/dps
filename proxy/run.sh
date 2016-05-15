#!/usr/bin/env bash

 
sleep 5
zabbix_agentd -c /etc/zabbix/zabbix_agentd.conf
touch /rules.toml
/opt/tomcat/bin/catalina.sh start
chmod 777 /etc/traefik -R
./traefik
