import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.framework.state.ConnectionState
import org.apache.curator.retry.RetryNTimes
import org.apache.curator.x.discovery.ServiceDiscovery
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder
import org.apache.curator.x.discovery.ServiceInstance
import org.apache.curator.x.discovery.ServiceProvider
import org.apache.curator.x.discovery.details.ServiceCacheListener

class BootStrap {

    static serviceCache;

    def init = { servletContext ->
        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient("zookeeper:2181", new RetryNTimes(5, 1000))
        curatorFramework.start()
        ServiceDiscovery<Void> serviceDiscovery = ServiceDiscoveryBuilder
                .builder(Void)
                .basePath("dynamoProxy")
                .client(curatorFramework).build()
        serviceDiscovery.start()
        ServiceProvider serviceProvider = serviceDiscovery
                .serviceProviderBuilder()
                .serviceName("servers")
                .build()
        serviceProvider.start()
        try {
            serviceCache = serviceDiscovery.serviceCacheBuilder().name("servers").build();
            serviceCache.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ServiceCacheListener listener = new ServiceCacheListener() {

            void cacheChanged() {
                println(serviceCache.getInstances())
                try {
                    File file = new File("rules.toml");
                    String s = " [backends]\n" +
                            "   [backends.backend1]\n";
                    int i = 1;
                    for (ServiceInstance ser : serviceCache.getInstances()) {
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

            void stateChanged(CuratorFramework client, ConnectionState newState) {
                println(client+"  "+newState);
            }
        }
        serviceCache.addListener(listener);
    }
    def destroy = {
        serviceCache.close();
    }
}
