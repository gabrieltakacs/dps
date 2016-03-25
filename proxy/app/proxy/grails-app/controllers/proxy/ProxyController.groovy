package proxy

import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.RetryNTimes
import org.apache.curator.x.discovery.ServiceDiscovery
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder
import org.apache.curator.x.discovery.ServiceProvider

class ProxyController {

    static ServiceProvider serviceProvider
    static {
        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient("172.17.0.1:2181", new RetryNTimes(5, 1000))
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

    def favicon() {
        render "404";
    }

    def index() {
        def instance = serviceProvider.getInstance()
        if(instance != null) {
            String address = instance.buildUriSpec()
            String URI = request.getRequestURI().toString();
            try {
                String response = (address + URI).toURL().getText()
                render response
            } catch (Exception e) {
                render "ERROR"+e.getMessage();
            }
        } else {
            log.error("instance is null")
            render "ERROR: no instance found!"
        }
    }
}
