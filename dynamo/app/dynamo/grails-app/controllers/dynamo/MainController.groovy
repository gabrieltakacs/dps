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
        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient("zookeeper.dev:2181", new RetryNTimes(5, 1000))
        curatorFramework.start()
        ServiceDiscovery<Void> serviceDiscovery = ServiceDiscoveryBuilder
                .builder(Void)
                .basePath("MyProxy")
                .client(curatorFramework).build()
        serviceDiscovery.start()
        serviceProvider = serviceDiscovery
                .serviceProviderBuilder()
                .serviceName("proxy")
                .build()
        serviceProvider.start()
    }

    def index() {
        info();
    }

    def info() {
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



}