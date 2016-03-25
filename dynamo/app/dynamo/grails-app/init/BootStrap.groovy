import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.RetryOneTime
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder
import org.apache.curator.x.discovery.ServiceInstance
import org.apache.curator.x.discovery.UriSpec

class BootStrap {

    def init = { servletContext ->

        registerInZookeeper();
    }
    def destroy = {
    }

    private static void registerInZookeeper() {
        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient("172.17.0.1:2181", new RetryOneTime(1000))
        curatorFramework.start()
        ServiceInstance<Void> serviceInstance = ServiceInstance.builder()
                .uriSpec(new UriSpec("{scheme}://{address}:{port}"))
                .address(InetAddress.getLocalHost().getHostAddress())
                //.port(System.getProperty('server.port').toInteger())//TODO zmeniť
                .port(8080)
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
