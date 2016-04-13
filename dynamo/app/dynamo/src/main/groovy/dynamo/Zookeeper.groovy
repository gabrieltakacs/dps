package dynamo

import org.apache.curator.x.discovery.ServiceCache
import org.apache.curator.x.discovery.ServiceProvider

class Zookeeper {

    private static ServiceProvider serviceProvider = null;

    private static ServiceCache serviceCache = null;

    private Zookeeper() {
        throw new AssertionError();
    }

    static ServiceProvider getServiceProvider() {
        return serviceProvider
    }

    static void setServiceProvider(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider
    }

    static ServiceCache getServiceCache() {
        return serviceCache
    }

    static void setServiceCache(ServiceCache serviceCache) {
        this.serviceCache = serviceCache
    }
}
