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
        ServiceDiscovery<Void> serviceDiscovery = ServiceDiscoveryBuilder
                .builder(Void)
                .basePath("dynamoProxy")
                .client(curatorFramework).build()
        serviceDiscovery.start()
        serviceProvider = serviceDiscovery
                .serviceProviderBuilder()
                .serviceName("servers")
                .build()
        serviceProvider.start()

        try {
            File file = new File("rules.toml");
            String s = " [backends]\n" +
                    "   [backends.backend1]\n";
            int i = 1;
            for (ServiceInstance ser : serviceProvider.allInstances) {
                s += "     [backends.backend1.servers.server" + i + "]\n" +
                        "     url = \"http://" + ser.address + ":"+ ser.port+"\"\n" +
                        "     weight = 1\n";
                i++;
            }
            s += " [frontends]\n" +
                    "   [frontends.frontend1]\n" +
                    "   backend = \"backend1\"\n" +
                    "   [frontends.frontend1.routes.proxy]\n" +
                    "   rule = \"Host\"\n" +
                    "   value = \"127.0.0.1\"\n";
            file.createNewFile();
            file.write(s);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    def index() {
        Map obj = new LinkedHashMap()
        obj.put("server IP", InetAddress.getLocalHost().getHostAddress());
        int i = 0;
        for (ServiceInstance s : serviceProvider.allInstances) {
            i++;
            obj.put("node " + i + " IP", s.address);
        }
        def json = obj as JSON
        json.prettyPrint = true
        json.render response
    }
}
