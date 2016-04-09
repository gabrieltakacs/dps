FROM davidcaste/alpine-tomcat:tomcat8

COPY app/dynamo /dynamo/
RUN cd /dynamo && ./gradlew war
RUN cp /dynamo/build/libs/*.war /opt/tomcat/webapps/
RUN rm -r dynamo
RUN export CATALINA_OPTS="-server -Dfile.encoding=UTF-8"

EXPOSE 2181

ADD run.sh /run.sh
RUN chmod 777 /run.sh
CMD ["/run.sh"]


