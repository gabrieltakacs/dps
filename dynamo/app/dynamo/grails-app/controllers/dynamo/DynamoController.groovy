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

    static def rest(String baseUrl, String path, query, Method method = Method.GET) {
        try {
            def ret = null
            def http = new HTTPBuilder(baseUrl)
            http.getClient().getParams().setParameter("http.connection.timeout", new Integer(1000))
            http.getClient().getParams().setParameter("http.socket.timeout", new Integer(1000))

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
            ex.printStackTrace()
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
                        println(vectorclock as JSON)
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
            query.put("value", params.value);
            query.put("vectorclock", params.vectorclock);
            query.put("redirected", false);
            String resp = rest(url, path, query, Method.POST)
            if(resp == null) {
                response.status = 500
                Map obj = new LinkedHashMap();
                obj.put("status", "error");
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

        //som zodpovedný za správu
        if (contains(instances)) {
            //nie som koordinátor
            if (params.redirected == "true") {
                Map obj = new LinkedHashMap();
                log.debug("getData - received redirected response: " + params);
                KeyValue kv = KeyValue.findByKey(key);
                if (kv?.value != null) {
                    obj.put("status", "success");
                    obj.put("value", kv.value);
                } else {
                    obj.put("status", "no data");
                }
                response.status = 200
                render obj as JSON
            } else {
                //pošle požiadavku serverom, pre ktoré je určená (zistí podla hashu klúča
                Set<String> set = new HashSet<>();//values, môžu sa lýšiť
                List<String> debugList = new ArrayList<>();
                int success = 0;
                for (ServiceInstance i : instances) {
                    //som jeden z príjemcov
                    if (InetAddress.getLocalHost().getHostAddress().equals(i.address)) {
                        log.debug("getData - getting data: " + params);
                        KeyValue kv = KeyValue.findByKey(key);
                        String s = "[" + i.payload + "]" + " " + i.address + " (this)";
                        if (kv?.value != null) {
                            set.add(kv.value);
                            success++;
                            s += " " + kv.value;
                        } else {
                            s += " no data";
                        }
                        debugList.add(s);
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
                        String s = "[" + i.payload + "]" + " " + i.address;
                        if (a?.status == "success" && a?.value != null) {
                            set.add(a.value);
                            success++;
                            s += " " + a.value;
                        } else {
                            s += " no data";
                        }
                        debugList.add(s);
                    }
                }
                Map obj = new LinkedHashMap();
                obj.put("key", params.key);
                obj.put("hash", hash);
                obj.put("status", "success: " + success);
                obj.put("values", set);
                obj.put("debug-values", debugList);
                response.status = 200
                render obj as JSON
            }
        } else {
            String url = "http://"+instances.get(0).address+":"+instances.get(0).port;
            log.debug("getData - resending to coordinator: "+url);
            String path = "/api/v1.0/get"
            Map query = new HashMap();
            query.put("key", params.key);
            query.put("redirected", true);
            String resp = rest(url, path, query)
            if(resp == null) {
                response.status = 500
                Map obj = new LinkedHashMap();
                obj.put("status", "error");
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
        render KeyValue.findAllByHashBetween(from, to) as JSON
    }

    def getAll() {
        if(params.redirected == "true") {
            render KeyValue.findAll() as JSON
        } else {
            //pošle požiadavku serverom, pre ktoré je určená (zistí podla hashu klúča
            List<ServiceInstance> instances = Zookeeper.serviceProvider.allInstances
            Map<String, List> dataMap = new LinkedHashMap<>();
            for(ServiceInstance i:instances) {
                if (InetAddress.getLocalHost().getHostAddress().equals(i.address)) {

                    dataMap.put(i.address.toString(), KeyValue.findAll());
                } else {
                    String url = "http://"+i.address+":"+i.port;
                    log.debug("postData - resending to: "+url);
                    String path = "/api/v1.0/getAll"
                    Map query = new HashMap();
                    query.put("redirected", true);
                    def resp = rest(url, path, query)
                    if(!resp) continue
                    JSONElement a = JSON.parse(resp);
                    def list = a
                    dataMap.put(i.address.toString(), list);
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

}
