FROM davidcaste/alpine-tomcat:tomcat8

COPY app/dynamo /dynamo/
RUN cd /dynamo && ./gradlew war
RUN cp /dynamo/build/libs/*.war /opt/tomcat/webapps/
RUN rm -r dynamo
RUN export CATALINA_OPTS="-server -Dfile.encoding=UTF-8"

EXPOSE 2181

# Install utils
RUN apk update && apk upgrade && apk add curl bash wget vim

# Install filebeat to send logs to logstash (https://github.com/bargenson/docker-filebeat)
ENV     FILEBEAT_VERSION 1.2.1
ENV     FILEBEAT_URL https://download.elastic.co/beats/filebeat/filebeat-${FILEBEAT_VERSION}-x86_64.tar.gz
ENV     FILEBEAT_HOME /opt/filebeat-${FILEBEAT_VERSION}-x86_64/

RUN apk add --update python curl && \
            wget -q "https://circle-artifacts.com/gh/andyshinn/alpine-pkg-glibc/6/artifacts/0/home/ubuntu/alpine-pkg-glibc/packages/x86_64/glibc-2.21-r2.apk" \
                 "https://circle-artifacts.com/gh/andyshinn/alpine-pkg-glibc/6/artifacts/0/home/ubuntu/alpine-pkg-glibc/packages/x86_64/glibc-bin-2.21-r2.apk" && \
            apk add --allow-untrusted glibc-2.21-r2.apk glibc-bin-2.21-r2.apk && \
            /usr/glibc/usr/bin/ldconfig /lib /usr/glibc/usr/lib
RUN cd /opt/ && curl -sL ${FILEBEAT_URL} | tar xz -C .
ADD ./config/filebeat.yml ${FILEBEAT_HOME}

# Install Zabbix-agent
RUN addgroup -g 2000 -S zabbix \
    && adduser -u 2000 -G zabbix -S zabbix \
    && apk add --update zabbix-agent jq bash python socat curl wget openssl \
    && rm -rf /var/cache/apk/* \
    && mkdir /etc/zabbix/zabbix_agentd.conf.d
ADD ./config/zabbix.conf /etc/zabbix/zabbix_agentd.conf

ADD run.sh /run.sh
RUN chmod 777 /run.sh
CMD ["/run.sh"]