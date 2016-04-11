package dynamo

import grails.converters.JSON
import org.apache.curator.x.discovery.ServiceInstance

class MainController{

    def index() {
       render(view: "/index", model: [serverInfo: getInfo()])
    }

    def number() {
        render DynamoParams.myNumber;
    }

    private getInfo = { ->
        Map obj = new LinkedHashMap()
        obj.put("server IP", InetAddress.getLocalHost().getHostAddress());
        obj.put("server id", DynamoParams.getMyNumber().toString());
        int i = 0;
        for (ServiceInstance s : Zookeeper.serviceProvider.allInstances) {
            if (!InetAddress.getLocalHost().getHostAddress().equals(s.address)) {
                i++;
                obj.put("neighbour " + i + " IP", s.address);
                obj.put("neighbour " + i + " ID", s.payload);
            }
        }
        return obj;
    }

    def info() {
        log.info("request - server info");
        Map obj = getInfo();
        def json = obj as JSON
        json.prettyPrint = true
        json.render response
    }

    def getHash() {
        if(params.key) {
            render KeyValue.calculateHash(params.key);
        } else {
            render "";
        }
    }

}