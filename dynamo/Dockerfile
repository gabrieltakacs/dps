FROM davidcaste/alpine-tomcat:tomcat8

#CMD ["cp","/dynamo/build/libs/*.war","/opt/tomcat/webapps/"]
#CMD [“/opt/tomcat/bin/catalina.sh”, “start”]

# In order to keep container running all the time
#RUN touch /var/log/keepalive
#CMD ["tail", "-f", "/var/log/keepalive"]

COPY app/dynamo /dynamo/
RUN cd /dynamo && ./gradlew war
RUN cp /dynamo/build/libs/*.war /opt/tomcat/webapps/
RUN /opt/tomcat/bin/catalina.sh start

ADD run.sh /run.sh
RUN chmod 777 /run.sh
CMD ["/run.sh"]


