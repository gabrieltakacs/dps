package dynamo

import grails.converters.JSON
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.RetryNTimes
import org.apache.curator.x.discovery.ServiceDiscovery
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder
import org.apache.curator.x.discovery.ServiceInstance
import org.apache.curator.x.discovery.ServiceProvider

class MainController{

    static ServiceProvider serviceProvider
    static {
        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient("zookeeper:2181", new RetryNTimes(5, 1000))
        curatorFramework.start()
        ServiceDiscovery<Void> serviceDiscovery = ServiceDiscoveryBuilder
                .builder(Void)
                .basePath("dynamoProxy")
                .client(curatorFramework).build()
        serviceDiscovery.start()
        serviceProvider = serviceDiscovery
                .serviceProviderBuilder()
                .serviceName("servers")
                .build()
        serviceProvider.start();
    }

    def index() {
    }

    def number() {
        render DynamoParams.myNumber;
    }

    def info() {
        log.info("request - server info");
        Map obj = new LinkedHashMap()
        obj.put("server IP", InetAddress.getLocalHost().getHostAddress());
        obj.put("server id", DynamoParams.getMyNumber().toString());
        int i=0;
        for(ServiceInstance s:serviceProvider.allInstances) {
            if(!InetAddress.getLocalHost().getHostAddress().equals(s.address)) {
                i++;
                obj.put("neighbour "+i+" IP", s.address);

                String address = s.buildUriSpec()
                URL url = (address + "/number").toURL();
                log.info("sending request to "+url);
                try {
                    obj.put("neighbour "+i+" ID", url.getText([connectTimeout: 1000, readTimeout: 1000]));
                } catch (Exception e) {
                    obj.put("neighbour "+i+" ID", "unknown");
                    log.error("ERROR",e);
                }
            }
        }
        def json = obj as JSON
        json.prettyPrint = true
        json.render response
    }

    def postData() {
        String key = params.key;
        int hash = KeyValue.calculateHash(key);
        if(belongHash(hash)) {
            KeyValue kv = KeyValue.findOrCreateByKey(key);
            kv.setValue(params.value);
            kv.save(flush: true, failOnError: true);
            Map obj = new LinkedHashMap();
            obj.put("status", "success");
            obj.put("hash", Integer.toString(hash));
            response.status = 200;
            render obj as JSON;
        } else {
            //redirect
            /*
            RestBuilder rest = new RestBuilder();
            def resp = rest.get("http://${hostname}/oauth/token") {
                auth(clientId, clientSecret)
                accept("application/json")
                contentType("application/x-www-form-urlencoded")
            }
            def json = resp.json*/
            Map obj = new LinkedHashMap()
            obj.put("status", "redirect");
            response.status = 200
            render obj as JSON
        }
    }

    boolean belongHash(int hash) {
        return (DynamoParams.myNumber <= hash && DynamoParams.nextNumber > hash);
    }

    def getData() {
        String key = params.key;
        int hash = KeyValue.calculateHash(key);
        if(belongHash(hash)) {
            KeyValue kv = KeyValue.findByKey(key);
            Map obj = new LinkedHashMap();

            if(kv != null) {
                obj.put("status", "success");
                obj.put("key", key);
                obj.put("hash", Integer.toString(hash));
                obj.put("value", kv.value);
            } else {
                obj.put("status", "key not found");
            }
            render obj as JSON;
        } else {
            //redirect
            Map obj = new LinkedHashMap()
            obj.put("status", "redirect");
            render obj as JSON
        }
    }

    def getHash() {
        if(params.key) {
            render KeyValue.getHash(params.key);
        } else {
            render "";
        }
    }
}