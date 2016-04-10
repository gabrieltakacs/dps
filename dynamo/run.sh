#!/usr/bin/env bash
/usr/sbin/zabbix_agentd -c /etc/zabbix/zabbix_agentd.conf
/opt/tomcat/bin/catalina.sh start
syslogd
${FILEBEAT_HOME}/filebeat
touch /var/log/keepalive
tail -f /var/log/keepalive