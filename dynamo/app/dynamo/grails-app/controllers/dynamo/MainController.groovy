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

    private static int count = 0;
    static ServiceProvider serviceProvider
    static {
        //log.info("starting service discovery");
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
        //log.info("service discovery started");
    }

    def index() {
        info();
    }

    def info() {
        log.info("request - server info");
        count++;
        Map obj = new LinkedHashMap()
        obj.put("requestCount", Integer.toString(count));
        obj.put("server IP", InetAddress.getLocalHost().getHostAddress());
        int i=0;
        for(ServiceInstance s:serviceProvider.allInstances) {
            if(!InetAddress.getLocalHost().getHostAddress().equals(s.address)) {
                i++;
                obj.put("neighbour "+i, s.address);
            }
        }
        def json = obj as JSON
        json.prettyPrint = true
        json.render response
    }

    def neighbours() {
        log.info("request - neighbours numbers")

        Map obj = new LinkedHashMap()
        for(ServiceInstance s:serviceProvider.allInstances) {
            if(!InetAddress.getLocalHost().getHostAddress().equals(s.address)) {
                String address = s.buildUriSpec()
                URL url = (address + "number").toURL();
                log.info("sending request to "+url);
                try {
                    obj.put(s.address, url.getText());
                } catch (Exception e) {
                    obj.put(s.address, "unknown");
                    log.error("ERROR",e);
                }
            }
        }
        def json = obj as JSON
        json.prettyPrint = true
        json.render response
    }

    def number() {
        log.info("request - get number")
        log.info("rendering number "+ClockNumber.getNumber())
        render ClockNumber.getNumber().toString();
    }

}