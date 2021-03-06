package dynamo

import grails.converters.JSON
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.apache.curator.x.discovery.ServiceInstance
import org.grails.web.json.JSONElement

class DynamoController {

    def index() {
        render DynamoParams.myNumber;
    }

    static def rest(String baseUrl, String path, query, Method method = Method.GET, Integer timeout = new Integer(1000)) {
        try {
            def ret = null
            def http = new HTTPBuilder(baseUrl)
            http.getClient().getParams().setParameter("http.connection.timeout", timeout)
            http.getClient().getParams().setParameter("http.socket.timeout", timeout)

            http.request(method, ContentType.TEXT) {
                uri.path = path
                uri.query = query

                headers.'User-Agent' = 'Mozilla/5.0 Ubuntu/8.10 Firefox/3.0.4'
                response.success = { resp, reader ->
                    ret = reader.getText()
                }
            }
            return ret
        } catch (Exception ex) {
            println(ex.message)
           // ex.printStackTrace()
            return null
        }
    }

    def postData() {
        String key = params.key as String;
        int hash = KeyValue.calculateHash(key);
        List<ServiceInstance> instances = Zookeeper.getResponsibleServers(hash);
        int quorum = DynamoParams.writeQuorum
        if (params.quorum) {
            quorum = params.quorum as Integer
        }
        Map vectorclock = new HashMap();
        if(params.vectorclock) {
            vectorclock = new JsonSlurper().parseText(params.vectorclock)
        }
        //som zodpovedný za správu
        if (contains(instances)) {
            //nie som koordinátor
            if(params.redirected == "true") {
                Map obj = new LinkedHashMap();
                log.debug("postData - received redirected response: "+params);
                KeyValue.saveImpl(key, params.value as String, hash, vectorclock)
                obj.put("status", "success")
                response.status = 200
                render obj as JSON
            } else {
                //som koordinátor - pošle požiadavku ostatným serverom
                int last = 0;
                if(vectorclock?.containsKey(DynamoParams.myNumber as String)) {
                    last = vectorclock.get(DynamoParams.myNumber as String) as Integer
                }
                last++;
                vectorclock.put(DynamoParams.myNumber as String, last)
                int success = 0;
                for(ServiceInstance i:instances) {
                    if (InetAddress.getLocalHost().getHostAddress().equals(i.address)) {
                        log.debug("postData - storing data: "+params);
                        if (KeyValue.saveImpl(key, params.value as String, hash, vectorclock)) {
                            success++;
                        } else {
                            Map obj = new LinkedHashMap();
                            obj.put("status", "error - old data");
                            response.status = 200
                            render obj as JSON
                            return
                        }
                    } else { //prepošle ďalej
                        String url = "http://"+i.address+":"+i.port;
                        log.debug("postData - resending to: "+url);
                        String path = "/api/v1.0/post"
                        Map query = new LinkedHashMap();
                        query.put("key", params.key);
                        query.put("value", params.value);
                        query.put("vectorclock", vectorclock as JSON);
                        query.put("redirected", true);
                        String resp = rest(url, path, query, Method.POST)
                        if(resp == null) continue
                        JSONElement a = JSON.parse(resp);
                        log.debug("postData - received response: "+a);
                        if(a?.status == "success") {
                            success++;
                        }
                    }
                }
                Map obj = new LinkedHashMap();
                if(success >= quorum) {
                    obj.put("status", "success: "+success);
                    obj.put("hash", Integer.toString(hash));
                } else {
                    obj.put("status", "quorum not met");
                }
                response.status = 200
                render obj as JSON
            }
        } else {
            String url = "http://"+instances.get(0).address+":"+instances.get(0).port;
            log.debug("postData - resending to coordinator: "+url);
            String path = "/api/v1.0/post"
            Map query = new LinkedHashMap();
            query.put("key", params.key);
            query.put("quorum", params.quorum);
            query.put("value", params.value);
            query.put("vectorclock", params.vectorclock);
            String resp = rest(url, path, query, Method.POST, new Integer(5000))
            if(resp == null) {
                response.status = 500
                Map obj = new LinkedHashMap();
                obj.put("status", "error - no response from coordinator");
                render obj as JSON
                return
            }
            render resp as String
        }
    }

    def getData() {
        String key = params.key;
        int hash = KeyValue.calculateHash(key);
        List<ServiceInstance> instances = Zookeeper.getResponsibleServers(hash);
        int quorum = DynamoParams.readQuorum
        if (params.quorum) {
            quorum = params.quorum as Integer
        }

        //som zodpovedný za správu
        if (contains(instances)) {
            //nie som koordinátor
            if (params.redirected == "true") {
                log.debug("getData - received redirected response: " + params);
                def kv = KeyValue.findAllByKey(key);
                response.status = 200
                render kv as JSON
            } else {
                //pošle požiadavku serverom, pre ktoré je určená
                List list = new ArrayList();
                int success = 0;
                for (ServiceInstance i : instances) {
                    //som jeden z príjemcov
                    if (InetAddress.getLocalHost().getHostAddress().equals(i.address)) {//ja
                        log.debug("getData - getting data: " + params);
                        list.addAll(KeyValue.findAllByKey(key))
                        if(!list.empty) {
                            success++;
                        }
                    } else { //prepošle ďalej
                        String url = "http://" + i.address + ":" + i.port;
                        log.debug("postData - resending to: " + url);
                        String path = "/api/v1.0/get"
                        Map query = new HashMap();
                        query.put("key", params.key);
                        query.put("redirected", true);
                        String resp = rest(url, path, query)
                        if (resp == null) continue
                        JSONElement a = JSON.parse(resp);
                        log.debug("getData - received response:");
                        if (!a.empty) {
                            list.addAll(a)
                            success++;
                        }
                    }
                }
                Map obj = new LinkedHashMap()
                if(success >= quorum) {
                    obj.put("status", "success: "+success);
                    obj.put("hash", Integer.toString(hash));
                    obj.put("values", list);
                } else {
                    obj.put("status", "quorum not met");
                }
                response.status = 200
                def json = obj as JSON
                json.prettyPrint = true
                json.render response
            }
        } else {
            String url = "http://"+instances.get(0).address+":"+instances.get(0).port;
            log.debug("getData - resending to coordinator: "+url);
            String path = "/api/v1.0/get"
            Map query = new HashMap();
            query.put("key", params.key);
            String resp = rest(url, path, query, Method.GET, new Integer(5000))
            if(resp == null) {
                response.status = 500
                Map obj = new LinkedHashMap();
                obj.put("status", "error - no response from coordinator");
                render obj as JSON
                return
            }
            render resp as String;
        }
    }

    def deleteData() {
        String key = params.key;
        int hash = KeyValue.calculateHash(key);
        List<ServiceInstance> instances = Zookeeper.getResponsibleServers(hash);

        //som zodpovedný za správu
        if (contains(instances)) {
            //nie som koordinátor
            if (params.redirected == "true") {
                log.debug("deleteData - received redirected response: " + params);
                KeyValue.executeUpdate("delete KeyValue c where c.key = :key", [key: key])
                Map obj = new LinkedHashMap();
                obj.put("status", "success");
                response.status = 200
                render obj as JSON
            } else {
                //pošle požiadavku serverom, pre ktoré je určená
                int success = 0;
                for (ServiceInstance i : instances) {
                    //som jeden z príjemcov
                    if (InetAddress.getLocalHost().getHostAddress().equals(i.address)) {
                        log.debug("deleteData - deleting data: " + params);
                        KeyValue.executeUpdate("delete KeyValue c where c.key = :key", [key: key])
                        success++;
                    } else { //prepošle ďalej
                        String url = "http://" + i.address + ":" + i.port;
                        log.debug("deleteData - resending to: " + url);
                        String path = "/api/v1.0/delete"
                        Map query = new HashMap();
                        query.put("key", params.key);
                        query.put("redirected", true);
                        String resp = rest(url, path, query, Method.DELETE)
                        if (resp == null) continue
                        JSONElement a = JSON.parse(resp);
                        log.debug("deleteData - received response:");
                        if (a?.status == "success") {
                            success++;
                        }
                    }
                }
                Map obj = new LinkedHashMap()
                obj.put("status", "success: " + success);
                response.status = 200
                render obj as JSON
            }
        } else {
            String url = "http://"+instances.get(0).address+":"+instances.get(0).port;
            log.debug("getData - resending to coordinator: "+url);
            String path = "/api/v1.0/delete"
            Map query = new HashMap();
            query.put("key", params.key);
            String resp = rest(url, path, query, Method.DELETE, new Integer(5000))
            if(resp == null) {
                response.status = 500
                Map obj = new LinkedHashMap();
                obj.put("status", "error - no response from coordinator");
                render obj as JSON
                return
            }
            render resp as String;
        }
    }

    def getRange() {
        if(!params.from || !params.to) {
            render "missing parameters from & to"
        }
        Integer from = params.from as Integer
        Integer to = params.to as Integer
        if(from > to) {
            render KeyValue.findAllByHashBetweenOrHashBetween(0, to, from, DynamoParams.maxClockNumber-1) as JSON
        } else {
            render KeyValue.findAllByHashBetween(from, to) as JSON
        }
    }

    def getAll() {
        int[] m = getCurrentRange();
        if(params.redirected == "true") {
            if(m[0] > m[1]) {
                render KeyValue.findAllByHashBetweenOrHashBetween(0, m[1], m[0], DynamoParams.maxClockNumber-1) as JSON
            } else {
                render KeyValue.findAllByHashBetween(m[0], m[1]) as JSON
            }
        } else {
            //pošle požiadavku serverom, pre ktoré je určená (zistí podla hashu klúča
            List<ServiceInstance> instances = Zookeeper.serviceProvider.allInstances
            Map<String, List> dataMap = new LinkedHashMap<>();
            for(ServiceInstance i:instances) {
                if (InetAddress.getLocalHost().getHostAddress().equals(i.address)) {
                    if(m[0] > m[1]) {
                        dataMap.put(i.address.toString(),KeyValue.findAllByHashBetweenOrHashBetween(0, m[1], m[0], DynamoParams.maxClockNumber-1));
                    } else {
                        dataMap.put(i.address.toString(),KeyValue.findAllByHashBetween(m[0], m[1]));
                    }
                } else {
                    String url = "http://"+i.address+":"+i.port;
                    log.debug("postData - resending to: "+url);
                    String path = "/api/v1.0/getAll"
                    Map query = new HashMap();
                    query.put("redirected", true);
                    def resp = rest(url, path, query)
                    if(!resp) continue
                    JSONElement a = JSON.parse(resp);
                    dataMap.put(i.address.toString(), a);
                }
            }
            def json = dataMap as JSON
            json.prettyPrint = true
            json.render response
        }
    }

    def clear() {
        KeyValue.executeUpdate('delete from KeyValue')
        render "cleared"
    }

    private static boolean contains(List<ServiceInstance> instances) {
        for(ServiceInstance i:instances) {
            if (InetAddress.getLocalHost().getHostAddress().equals(i.address)) {
                return true;
            }
        }
        return false;
    }

    public static int[] getCurrentRange() {
        int min = DynamoParams.myNumber;
        int max = (DynamoParams.myNumber - 1 + DynamoParams.maxClockNumber) % DynamoParams.maxClockNumber;
        int count = 0;
        List<ServiceInstance> all = Zookeeper.getSortedServices();
        for (int i=all.size()-1; i>=0; i--) {
            ServiceInstance instance = all.get(i);
            int instanceKey = Integer.parseInt(instance.payload as String);
            if(instanceKey < DynamoParams.myNumber) {
                min = instanceKey;
                count++;
                if(count >= DynamoParams.replicas) {
                    break;
                }
            }
        }
        if(count < DynamoParams.replicas && all.size() > count) {
            int i=all.size()-1;
            while(count < Math.min(DynamoParams.replicas, all.size())) {
                int key = Integer.parseInt(all.get(i).payload as String)
                min = key;
                i--;
                count++;
            }
        }
        return [min , max];
    }
}
