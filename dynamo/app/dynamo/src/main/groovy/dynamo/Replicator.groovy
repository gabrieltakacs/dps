package dynamo

import grails.converters.JSON
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.apache.curator.framework.recipes.locks.InterProcessMutex
import org.apache.curator.x.discovery.ServiceInstance
import org.grails.web.json.JSONObject

class Replicator {
    private static final Replicator replicator = new Replicator();

    public static Replicator getInstance() {
        return replicator;
    }

    public synchronized void process() {
       // InterProcessMutex mutex = new InterProcessMutex(Zookeeper.getCuratorFramework(), "/lock");
       // println("Replicator - accquiring lock")
       // mutex.acquire()
        println("Replicator - lock acquired")
        checkData();
       // mutex.release();
        println("Replicator - lock released")
    }

    private static void checkData() {
        //vypočítanie rozsahu, ktorý máme uložený
        println("Replicator - checkData")
        List<ServiceInstance> last = DynamoParams.getLastInstanceList();
        int[] range = null;
        boolean init;
        if(last == null) {
            println("Replicator - no saved data");
            init = true;
        } else {
            println("Replicator - getting range of saved data");
            range = getCurrentRange();
            println("Replicator - detected stored range: ["+range[0]+"]->["+range[1]+"]");
            init = false;
        }
        List<ServiceInstance> list = Zookeeper.getSortedServices();
        List<Integer> ranges = getRequiredRange(list)
        println("Required range: "+ranges);
        List<Integer[]> needed = filterRanges(ranges, range);
        println("Filtered ranges: "+needed);
        for(Integer[] array: needed) {
            downloadRange(array, init)
        }
        DynamoParams.setLastInstanceList(list);
    }

    //vráti rozsahy, ktoré potrebuje stiahnuť
    //stav - hotovo
    private static List<int[]> filterRanges(List<Integer> ranges, int[] stored) {
        List<int[]> result = new ArrayList<Integer[]>();
        int to = (DynamoParams.myNumber - 1 + DynamoParams.maxClockNumber) % DynamoParams.maxClockNumber;
        for(int i=0;i<ranges.size();i++) {
            int from = ranges.get(i);
            int[] range = new int[2];
            println("Debug: processing range ["+from+"]->["+to+"]");
            if(stored == null) {
                range[0] = from;
                range[1] = to;
                result.add(range);
                println("Debug: cannot filter");
            } else {
                int f_to = to;
                int f_from = from;
                int min = stored[0], max = stored[1];
                if(max < min && f_to < f_from) {
                    max+= DynamoParams.maxClockNumber;
                    f_to+= DynamoParams.maxClockNumber;
                } else if(max < min) {
                    max+= DynamoParams.maxClockNumber;
                    f_to+= DynamoParams.maxClockNumber;
                    f_from+= DynamoParams.maxClockNumber;
                } else if(f_to < f_from) {
                    max+= DynamoParams.maxClockNumber;
                    f_to+= DynamoParams.maxClockNumber;
                    min+= DynamoParams.maxClockNumber;
                }
                if(f_from >= min && f_to <= max) {
                    println("Debug: range is contained");
                } else {
                    //filter range
                    if(f_to < max) {
                        f_to = Math.min(min, f_to)
                    }
                    if(f_from > min) {
                        f_to = Math.max(max, f_from)
                    }
                    f_from = f_from % DynamoParams.maxClockNumber;
                    f_to = f_to % DynamoParams.maxClockNumber;
                    range[0] = f_from;
                    range[1] = f_to;
                    result.add(range);
                    //println("Debug: reducing to ["+f_from+"]->["+f_to+"]");
                }
            }
            to = (from - 1 + DynamoParams.maxClockNumber) % DynamoParams.maxClockNumber;
        }
        return result;
    }

    //vráti rozsahy, ktoré by mal obsahovať teraz
    //stav - funkčné
    private static List<Integer> getRequiredRange(List<ServiceInstance> list) {
        ArrayList<Integer> ranges = new ArrayList<>();
        int count = 0;
        for (int i=list.size()-1; i>=0; i--) {
            ServiceInstance instance = list.get(i);
            int instanceKey = Integer.parseInt(instance.payload as String);
            if(instanceKey < DynamoParams.myNumber) {
                ranges.add(instanceKey)
                count++;
                if(count >= DynamoParams.replicas) {
                    break;
                }
            }
        }
        if(count < DynamoParams.replicas && list.size() > count) {
            int i=list.size()-1;
            while(count < Math.min(DynamoParams.replicas, list.size())) {
                int key = Integer.parseInt(list.get(i).payload as String)
                ranges.add(key);
                i--;
                count++;
            }
        }
        return ranges;
    }

    //zistí rozsah, za ktorý bol zodpovedný predtým
    public static int[] getCurrentRange() {
        int min = DynamoParams.myNumber;
        int max = (DynamoParams.myNumber - 1 + DynamoParams.maxClockNumber) % DynamoParams.maxClockNumber;
        int count = 0;
        List<ServiceInstance> all = DynamoParams.lastInstanceList;
        for (int i=all.size()-1; i>=0; i--) {
            ServiceInstance instance = all.get(i);
            int instanceKey = Integer.parseInt(instance.payload as String);
            if(instanceKey < DynamoParams.myNumber) {
                min = instanceKey;
                //println("Debug: new min = "+min);
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
              //  println("Debug: new min = "+min);
                i--;
                count++;
            }
        }
        return [min , max];
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
            println(ex.getLocalizedMessage())
            //ex.printStackTrace()
            return null
        }
    }

    private static List<ServiceInstance> getResponsibleList(int hash) {
        List<ServiceInstance> all = DynamoParams.getLastInstanceList();
        List<ServiceInstance> instances = new ArrayList<>();
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

    private static void downloadRange(Integer[] range, boolean init) {
        println("downloading range "+range[0]+" -> "+range[1])
        int from = range[0]
        int to = range[1]

        def instances
        if(init) {
            instances = Zookeeper.getResponsibleServers(from)
        } else {
            instances = getResponsibleList(from)
        }

        for(ServiceInstance i:instances) {
            println("instance: "+i.address)
            if (!InetAddress.getLocalHost().getHostAddress().equals(i.address)) {
                String url = "http://"+i.address+":"+i.port;
                String path = "/api/v1.0/getRange"
                Map query = new HashMap();
                query.put("from", from);
                query.put("to", to);
                String resp = rest(url, path, query);
                if(!resp) {
                    continue
                };
                println("response: "+resp)
                List parsedList = JSON.parse(resp)
                KeyValue.withTransaction { status ->
                    parsedList.each {
                        JSONObject jsonObject ->
                            println("next object: "+jsonObject)
                            KeyValue kv = new KeyValue(jsonObject)
                            KeyValue.saveImpl(kv.key, kv.value, kv.hash, kv.vectorClock)
                    }
                }
                println("downloaded from server")
            }
        }
    }
}
