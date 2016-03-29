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
        log.info("starting service discovery");
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
        log.info("service discovery started");
    }

    def favicon() {
        render "404";
    }

    def index() {
        def instance = serviceProvider.getInstance()
        if(instance != null) {
            String address = instance.buildUriSpec()
            String uri = request.getRequestURI().toString();
            URL url = (address + uri).toURL();
            log.info("Redirecting to "+url);
            try {
                String response = url.getText()
                render response
            } catch (Exception e) {
                log.error("ERROR while redirecting",e)
                render "ERROR: "+e.getMessage();
            }
        } else {
            log.error("ERROR: no instance found")
            render "ERROR: no instance found!"
        }
    }
}
