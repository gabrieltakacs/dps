# Aktualny stav
* Mame vytvorene vsetky containery s potrebnymi vecami

# Aktualne problemy
* Nevieme prepojit traefik so zookeeper. Vyzera to byt na chybu vo firewalle, ktory tam ani neviem ci mame :) Skusal som nainstalovat iptables, ale nejako to nefici.

# TO-DO list (poradie je dolezite!)
* Prepojenie traefik so zookeeper
* Spravit zakladny node - implementaciu dynamo, zakladny REST klient. Zaklad by mozno mohol spravit Miro, on ma skusenosti s Javou a REST klientom v Jave.
* Prepojit dynamo so zookeeper - keyword "zookeeper-client". Dynamo sa musi registrovat u zookeepera.
* Nakonfigurovat zookeeper a traefik tak, aby traefik vedel, na ktory node posielat requesty.
* Rozbehat zakladne logovanie

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
* docker-compose scale dynamo=5 up -d


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

