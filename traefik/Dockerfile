FROM alpine:3.3

RUN apk update && apk upgrade && apk add curl bash wget vim iptables

RUN cd / && wget http://github.com/containous/traefik/releases/download/v1.0.0-beta.247/traefik --quiet && chmod +x traefik
EXPOSE 80
EXPOSE 2181

RUN mkdir /etc/traefik

#ENTRYPOINT ["/traefik"]
#CMD ["--configFile=/etc/treaefik/traefik.toml"]
RUN touch /var/log/keepalive
CMD ["tail", "-f", "/var/log/keepalive"]
