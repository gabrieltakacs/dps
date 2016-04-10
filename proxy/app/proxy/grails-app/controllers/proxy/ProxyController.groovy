package proxy

import grails.converters.JSON
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.RetryNTimes
import org.apache.curator.x.discovery.ServiceDiscovery
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder
import org.apache.curator.x.discovery.ServiceInstance
import org.apache.curator.x.discovery.ServiceProvider

class ProxyController {

    static ServiceProvider serviceProvider
    static {
        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient("zookeeper:2181", new RetryNTimes(5, 1000))
        curatorFramework.start()
        ServiceDiscovery<String> serviceDiscovery = ServiceDiscoveryBuilder
                .builder(String)
                .basePath("dynamoProxy")
                .client(curatorFramework).build()
        serviceDiscovery.start()
        serviceProvider = serviceDiscovery
                .serviceProviderBuilder()
                .serviceName("servers")
                .build()
        serviceProvider.start()
    }

    def index() {
        Map obj = new LinkedHashMap()
        obj.put("server IP", InetAddress.getLocalHost().getHostAddress());
        int i = 0;
        for (ServiceInstance s : serviceProvider.allInstances) {
            i++;
            obj.put("node " + i + " IP", s.address);
            obj.put("node " + i + " ID", s.payload);
        }
        def json = obj as JSON
        json.prettyPrint = true
        json.render response
    }
}
