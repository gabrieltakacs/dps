package dynamo

import org.apache.curator.x.discovery.ServiceProvider

class Zookeeper {

    private static ServiceProvider serviceProvider = null;

    private Zookeeper() {
        throw new AssertionError();
    }

    static ServiceProvider getServiceProvider() {
        return serviceProvider
    }

    static void setServiceProvider(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider
    }
}
