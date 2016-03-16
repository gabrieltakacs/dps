FROM anapsix/alpine-java

# In order to keep container running all the time
RUN touch /var/log/keepalive
CMD ["tail", "-f", "/var/log/keepalive"]