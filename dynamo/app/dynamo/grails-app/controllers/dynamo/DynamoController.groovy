package dynamo

import grails.converters.JSON
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseException
import groovyx.net.http.Method
import org.apache.curator.x.discovery.ServiceInstance
import org.grails.web.json.JSONElement

class DynamoController {

    def index() {
        render DynamoParams.myNumber;
    }

    static def rest(String baseUrl, String path, query, method = Method.GET) {
        try {
            def ret = null
            def http = new HTTPBuilder(baseUrl)
            http.request(method, ContentType.TEXT) {
                uri.path = path
                uri.query = query
                headers.'User-Agent' = 'Mozilla/5.0 Ubuntu/8.10 Firefox/3.0.4'
                response.success = { resp, reader ->
                    ret = reader.getText()
                    //TODO remove later
                    println 'Response data: -----';println ret;println '--------------------'
                }
            }
            return ret
        } catch (HttpResponseException ex) {
            ex.printStackTrace()
            return null
        } catch (ConnectException ex) {
            ex.printStackTrace()
            return null
        }
    }

    def postData() {
        String key = params.key;
        def url = "http://127.0.0.1:8080"
        def path = "/api/v1.0/get"
        def query = [ key: params.key ]
        JSONElement a = JSON.parse(rest(url, path, query));

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
            log.debug("postData - accepting request: "+params);
        } else {
            Map obj = new LinkedHashMap()
            obj.put("status", "redirect");
            response.status = 200
            log.debug("postData - redirecting request: "+params);
            render obj as JSON
        }
    }

    def getData() {
        String key = params.key;
        int hash = KeyValue.calculateHash(key);
        println(getServers(hash));
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

    boolean belongHash(int hash) {
        return (DynamoParams.myNumber <= hash && DynamoParams.nextNumber > hash);
    }

    List<ServiceInstance> getServers(int hash) {
        List<ServiceInstance> instances = new ArrayList<>();
        List<ServiceInstance> all = Zookeeper.serviceProvider.allInstances;
        for (int i=0; i<all.size(); i++) {
            ServiceInstance instance = all.get(i);
            int instanceKey = Integer.parseInt(instance.payload as String);
            if(instanceKey >= hash) {
                instances.add(instance);
                if(instances.size() >= DynamoParams.replicas) {
                    break;
                }
            }
        }
        if(instances.size() < DynamoParams.replicas && all.size() > instances.size()) {
            int i=0;
            while(instances.size() < Math.min(DynamoParams.replicas, all.size())) {
                instances.add(all.get(i));
                i++;
            }
        }
        return instances;
    }
}
