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
        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient("localhost:2181", new RetryNTimes(5, 1000))
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

    //ResourceLocator grailsResourceLocator

    def favicon() {
        render "404";
       /* final Resource image = grailsResourceLocator.findResourceForURI('/images/favicon.ico')
        render file: image.inputStream, contentType: 'image/ico'*/
    }

    def index() {

        log.error("ALL: "+serviceProvider.getAllInstances());
        def instance = serviceProvider.getInstance()
        if(instance != null) {
            String address = instance.buildUriSpec()
            String URI = request.getRequestURI().toString();
            //log.error("ADDRESS: "+address);
            //log.error("URI: "+URI);
            log.error(address+URI);
            try {
                String response = (address + URI).toURL().getText()
                render response
            } catch (Exception e) {
                render "CHYBA "+e.getMessage();
            }

        } else {
            log.error("instance is null")
        }
        return response
    }
}
