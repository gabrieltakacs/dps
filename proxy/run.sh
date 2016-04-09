#!/usr/bin/env bash

touch /rules.toml
/opt/tomcat/bin/catalina.sh start
chmod 777 /etc/traefik -R
./traefik