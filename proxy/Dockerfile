FROM davidcaste/alpine-tomcat:tomcat8

RUN apk update && apk upgrade && apk add curl bash wget vim iptables

RUN cd / && wget http://github.com/containous/traefik/releases/download/v1.0.0-beta.254/traefik --quiet && chmod +x traefik
EXPOSE 8081
EXPOSE 2181
EXPOSE 3181

RUN mkdir /etc/traefik

COPY app/proxy /proxy/
#RUN cd /proxy && ./gradlew war
RUN cp /proxy/build/libs/*.war /opt/tomcat/webapps/
RUN rm -r proxy
RUN export CATALINA_OPTS="-server -Dfile.encoding=UTF-8"

# Install Zabbix-agent
RUN addgroup -g 2000 -S zabbix \
    && adduser -u 2000 -G zabbix -S zabbix \
    && apk add --update zabbix-agent jq bash python socat curl wget openssl \
    && rm -rf /var/cache/apk/* \
    && mkdir /etc/zabbix/zabbix_agentd.conf.d
ADD config/zabbix.conf /etc/zabbix/zabbix_agentd.conf

ADD run.sh /run.sh
RUN chmod 777 /run.sh
CMD ["/run.sh"]


