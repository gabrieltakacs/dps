import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.RetryOneTime
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder
import org.apache.curator.x.discovery.ServiceInstance
import org.apache.curator.x.discovery.UriSpec

class BootStrap {

    private static final String basePath = "/traefik/backends/dynamo/servers.";

    def init = { servletContext ->

        registerInZookeeper();
    }
    def destroy = {
    }

    private static void registerInZookeeper() {
        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient("zookeeper.dev:2181", new RetryOneTime(1000))
        curatorFramework.start()
        ServiceInstance<Void> serviceInstance = ServiceInstance.builder()
                .uriSpec(new UriSpec("{scheme}://{address}:{port}"))
                .address('localhost')
                .port(System.getProperty('server.port').toInteger())//TODO zmeniť
                .name("proxy")
                .build()
        ServiceDiscoveryBuilder.builder(Void)
                .basePath("MyProxy")
                .client(curatorFramework)
                .thisInstance(serviceInstance)
                .build()
                .start()
    }
}
