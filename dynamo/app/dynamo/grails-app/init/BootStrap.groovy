import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.RetryNTimes
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
        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient("zookeeper.dev:2181", new RetryNTimes(5, 1000))
        curatorFramework.start()
        ServiceInstance<Void> serviceInstance = ServiceInstance.builder()
                .uriSpec(new UriSpec("{scheme}://{address}:{port}"))
                .address(InetAddress.getLocalHost().getHostAddress())
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
