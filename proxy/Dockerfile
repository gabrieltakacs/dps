FROM davidcaste/alpine-tomcat:tomcat8

COPY app/proxy /proxy/
RUN cd /proxy && ./gradlew war
RUN cp /proxy/build/libs/*.war /opt/tomcat/webapps/
RUN rm -r proxy

EXPOSE 2181

ADD run.sh /run.sh
RUN chmod 777 /run.sh
CMD ["/run.sh"]


