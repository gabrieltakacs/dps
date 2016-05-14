# Docker

## Docker intercontainer networking
* http://blog.sequenceiq.com/blog/2014/08/12/docker-networking/

## Commands
* Build: docker-compose build
* Start: docker-compose up -d (foregroud: bez prepinaca -d)
* Stop:  docker-compose stop
* Pripojenie sa na container (napr. na zookeeper): docker exec -it dps-zookeeper bash

# Traefik
* Install directory: /
* Configuration: /etc/traefik/traefik.toml
* Zatial ho spustame manualne
* Run command: cd / && ./traefik --configFile=/etc/traefik/traefik.toml

# Zookeeper
* Install directory: /opt/zookeeper
* Configuration: /opt/zookeeper/conf/zoo.cfg
* Bezi na porte 2181
* Spusta sa automaticky pri spusteni containera

##
* spustenie s viacerími inštanciami dynama:
* docker-compose scale dynamo=3
* docker-compose up -d
* Delete all containers
* docker rm $(docker ps -a -q)


