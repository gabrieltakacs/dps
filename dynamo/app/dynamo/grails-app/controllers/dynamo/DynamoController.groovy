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

    static def rest(String baseUrl, String path, query, Method method = Method.GET) {
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
                    //println 'Response data: -----';println ret;println '--------------------'
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
        int hash = KeyValue.calculateHash(key);

        //spracuje a neposiela ďalej
        if(params.redirected == "true") {
            Map obj = new LinkedHashMap();
            log.debug("postData - received redirected response: "+params);
            //TODO check či má naozaj zapísať - podla hashu či je jeden z príjemcov
            KeyValue kv = KeyValue.findOrCreateByKey(key);
            kv.setValue(params.value);
            kv.save(flush: true, failOnError: true);

            obj.put("status", "success");
            response.status = 200
            render obj as JSON
        } else {
            //pošle požiadavku serverom, pre ktoré je určená (zistí podla hashu klúča
            List<ServiceInstance> instances = getServers(hash);
            int success = 0;
            for(ServiceInstance i:instances) {
                //som jeden z príjemcov
                if (InetAddress.getLocalHost().getHostAddress().equals(i.address)) {
                    log.debug("postData - storing data: "+params);
                    KeyValue kv = KeyValue.findOrCreateByKey(key);
                    kv.setValue(params.value);
                    kv.save(flush: true, failOnError: true);
                    success++;
                } else { //prepošle ďalej
                    //TODO paralelne + synchronizovať
                    String url = "http://"+i.address+":"+i.port;
                    log.debug("postData - resending to: "+url);
                    String path = "/api/v1.0/post"
                    Map query = new LinkedHashMap();
                    query.put("key", params.key);
                    query.put("value", params.value);
                    query.put("redirected", true);
                    JSONElement a = JSON.parse(rest(url, path, query, Method.POST));
                    log.debug("postData - received response: "+a);
                    if(a?.status == "success") {
                        success++;
                    }
                }
            }
            Map obj = new LinkedHashMap();
            obj.put("status", "success: "+success);
            obj.put("hash", Integer.toString(hash));

            response.status = 200
            render obj as JSON
        }
    }

    def getData() {
        String key = params.key;
        int hash = KeyValue.calculateHash(key);

        //spracuje a neposiela ďalej
        if(params.redirected == "true") {
            Map obj = new LinkedHashMap();
            log.debug("getData - received redirected response: "+params);
            KeyValue kv = KeyValue.findByKey(key);
            if(kv?.value != null) {
                obj.put("status", "success");
                obj.put("value", kv.value);
            } else {
                obj.put("status", "no data");
            }
            response.status = 200
            render obj as JSON
        } else {
            //pošle požiadavku serverom, pre ktoré je určená (zistí podla hashu klúča
            List<ServiceInstance> instances = getServers(hash);
            Set<String> set = new HashSet<>();//values, môžu sa lýšiť
            List<String> debugList = new ArrayList<>();
            int success = 0;
            for(ServiceInstance i:instances) {
                //som jeden z príjemcov
                if (InetAddress.getLocalHost().getHostAddress().equals(i.address)) {
                    log.debug("getData - getting data: "+params);
                    KeyValue kv = KeyValue.findByKey(key);
                    String s = "["+i.payload+"]"+" "+i.address+" (this)";
                    if(kv?.value != null) {
                        set.add(kv.value);
                        success++;
                        s+=" "+kv.value;
                    } else {
                        s+=" no data";
                    }
                    debugList.add(s);
                } else { //prepošle ďalej
                    //TODO paralelne + synchronizovať
                    String url = "http://"+i.address+":"+i.port;
                    log.debug("postData - resending to: "+url);
                    String path = "/api/v1.0/get"
                    Map query = new HashMap();
                    query.put("key", params.key);
                    query.put("redirected", true);
                    JSONElement a = JSON.parse(rest(url, path, query));
                    log.debug("getData - received response:");
                    String s = "["+i.payload+"]"+" "+i.address;
                    if(a?.status == "success" && a?.value != null) {
                        set.add(a.value);
                        success++;
                        s+=" "+a.value;
                    } else {
                        s+=" no data";
                    }
                    debugList.add(s);
                }
            }
            Map obj = new LinkedHashMap();
            obj.put("key", params.key);
            obj.put("hash", hash);
            obj.put("status", "success: "+success);
            obj.put("values", set);
            obj.put("debug-values", debugList);
            response.status = 200
            render obj as JSON
        }
    }

    static Comparator<ServiceInstance> comparator = new Comparator<ServiceInstance>() {
        @Override
        int compare(ServiceInstance o1, ServiceInstance o2) {
            return Integer.compare(Integer.parseInt(o1.payload as String), Integer.parseInt(o2.payload as String))
        }
    }

    List<ServiceInstance> getServers(int hash) {
        List<ServiceInstance> instances = new ArrayList<>();
        List<ServiceInstance> all = new ArrayList<ServiceInstance>(Zookeeper.serviceProvider.allInstances);
        all.sort(comparator);
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
