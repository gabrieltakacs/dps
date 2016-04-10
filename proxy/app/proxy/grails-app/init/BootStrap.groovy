import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.framework.state.ConnectionState
import org.apache.curator.retry.RetryNTimes
import org.apache.curator.x.discovery.ServiceDiscovery
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder
import org.apache.curator.x.discovery.ServiceInstance
import org.apache.curator.x.discovery.ServiceProvider
import org.apache.curator.x.discovery.details.ServiceCacheListener

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class BootStrap {

    static serviceCache;

    def init = { servletContext ->
        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient("zookeeper:2181", new RetryNTimes(5, 1000))
        curatorFramework.start()
        ServiceDiscovery<String> serviceDiscovery = ServiceDiscoveryBuilder
                .builder(String)
                .basePath("dynamoProxy")
                .client(curatorFramework).build()
        serviceDiscovery.start()
        final ServiceProvider serviceProvider = serviceDiscovery
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
                writeConfig(null);
            }

            void stateChanged(CuratorFramework client, ConnectionState newState) {
                println(client+"  "+newState);
            }
        }
        serviceCache.addListener(listener);
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(new Runnable() {
            void run() {
                writeConfig(serviceProvider);
            }
        }, 5, 5, TimeUnit.SECONDS);

    }
    def destroy = {
        serviceCache.close();
    }

    private synchronized void writeConfig(ServiceProvider provider) {
        try {
            File file = new File("rules.toml");
            String s = " [backends]\n" +
                    "   [backends.backend1]\n";
            int i = 1;
            if(provider == null) {
                for (ServiceInstance ser : serviceCache.getInstances()) {
                    s += "     [backends.backend1.servers.server" + i + "]\n" +
                            "     url = \"http://" + ser.address + ":" + ser.port + "\"\n" +
                            "     weight = 1\n";
                    i++;
                }
            } else {
                for (ServiceInstance ser : provider.allInstances) {
                    s += "     [backends.backend1.servers.server" + i + "]\n" +
                            "     url = \"http://" + ser.address + ":" + ser.port + "\"\n" +
                            "     weight = 1\n";
                    i++;
                }
            }
            s += " [frontends]\n" +
                    "   [frontends.frontend1]\n" +
                    "   backend = \"backend1\"\n" +
                    "   [frontends.frontend1.routes.proxy]\n" +
                    "   rule = \"Host\"\n" +
                    "   value = \"127.0.0.1\"\n";
            s += "   [frontends.frontend2]\n" +
                    "   backend = \"backend1\"\n" +
                    "   [frontends.frontend2.routes.proxy]\n" +
                    "   rule = \"Host\"\n" +
                    "   value = \"192.168.99.100\"\n";
            file.createNewFile();
            file.write(s);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
