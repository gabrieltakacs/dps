FROM davidcaste/alpine-tomcat:tomcat8

COPY config/logging.properties /opt/tomcat/conf/
COPY config/catalina.sh /opt/tomcat/bin/
COPY libs/*.jar /opt/tomcat/bin/

COPY app/proxy /proxy/
#RUN cd /proxy && ./gradlew war
RUN cp /proxy/build/libs/*.war /opt/tomcat/webapps/
RUN rm -r proxy

EXPOSE 8080
EXPOSE 2181

RUN chmod 777 /opt/tomcat/bin/catalina.sh
ADD run.sh /run.sh
RUN chmod 777 /run.sh
CMD ["/run.sh"]


