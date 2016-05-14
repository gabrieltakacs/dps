FROM kibana:latest

RUN apt-get update && apt-get install -y netcat vim

COPY entrypoint.sh /tmp/entrypoint.sh
COPY config /opt/kibana/config
RUN chmod 777 -R /opt/kibana
RUN chmod +x /tmp/entrypoint.sh

RUN kibana plugin --install elastic/sense

CMD ["/tmp/entrypoint.sh"]
