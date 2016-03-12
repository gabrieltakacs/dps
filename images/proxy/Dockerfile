FROM alpine:3.3
MAINTAINER Gabriel Takacs <gtakacs@gtakacs.sk>

RUN apk update && apk upgrade && apk add curl bash wget

# Download and install Traefik
RUN wget -q https://github.com/containous/traefik/releases/download/v1.0.0-beta.211/traefik_linux-amd64 -o /traefik
RUN chmod 777 /traefik
ADD traefik.toml /

ENTRYPOINT ["/traefik"]