# Aktualny stav
* Mame vytvorene vsetky containery s potrebnymi vecami
* Mame nakodene vlastne jednoduche proxy
* Mame nakodene "dynamo", ktore dokaze prijimat requesty, ale nic s nimi zatial nerobi
* Mame rozbehane elstash

# Aktualne problemy

# Todo:
* Otestovat logovanie
* Spravit, aby dynamo vedelo preposielat requesty na ine dynama
* Zistit, ci to musi bezat na dvoch separatnych PCs, alebo ci sa ako virtualka mysli docker container
* Vsetko skontrolovat a pripravit na odovzdavanie

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
* spustenie traefiku inak, píše inú chybu
*  /./traefik --zookeeper --zookeeper.endpoint 172.17.0.3:2181

# dynamo
* dynamo beží na http://127.0.0.1:8080/dynamo-0.1/
* stiahnuť:

    intellij (lepšie ultimate)

    Java 8 sdk

    http://groovy-lang.org/download.html

    https://grails.org/download.html

* otvoriť intellij - open dps project
* súbor .bashrc v home adresári - pridať:

GRAILS_HOME=/home/miro/grails-3.1.4

export GRAILS_HOME

PATH=$PATH:$GRAILS_HOME/bin

