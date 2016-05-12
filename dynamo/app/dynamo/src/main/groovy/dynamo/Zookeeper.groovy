package dynamo

import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.RetryNTimes
import org.apache.curator.x.discovery.ServiceCache
import org.apache.curator.x.discovery.ServiceDiscovery
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder
import org.apache.curator.x.discovery.ServiceInstance
import org.apache.curator.x.discovery.ServiceProvider
import org.apache.curator.x.discovery.UriSpec

class Zookeeper {

    private static CuratorFramework curatorFramework
    private static ServiceProvider serviceProvider = null;
    private static ServiceCache serviceCache = null;

    private Zookeeper() {
        throw new AssertionError();
    }

    static ServiceProvider getServiceProvider() {
        return serviceProvider
    }

    public static CuratorFramework initCuratorFramework() {
        curatorFramework = CuratorFrameworkFactory.newClient("zookeeper:2181", new RetryNTimes(5, 1000))
        curatorFramework.start()
        return this.curatorFramework;
    }

    public static CuratorFramework getCuratorFramework() {
        return curatorFramework;
    }

    public static ServiceProvider initServiceProvider() {
        ServiceDiscovery<String> serviceDiscovery = ServiceDiscoveryBuilder
                .builder(String)
                .basePath("dynamoProxy")
                .client(curatorFramework).build()
        serviceDiscovery.start()
        ServiceProvider serviceProvider = serviceDiscovery
                .serviceProviderBuilder()
                .serviceName("servers")
                .build()
        serviceProvider.start();
        ServiceCache serviceCache = serviceDiscovery.serviceCacheBuilder().name("servers").build();
        serviceCache.start();
        this.serviceCache = serviceCache;
        this.serviceProvider = serviceProvider;
        return serviceProvider;
    }

    public static void registerServer() {
        ServiceInstance<String> serviceInstance = ServiceInstance.builder()
                .uriSpec(new UriSpec("{scheme}://{address}:{port}"))
                .address(InetAddress.getLocalHost().getHostAddress())
                .port(8080)
                .payload(Integer.toString(DynamoParams.myNumber))
                .name("servers")
                .build()
        ServiceDiscoveryBuilder.builder(String)
                .basePath("dynamoProxy")
                .client(curatorFramework)
                .thisInstance(serviceInstance)
                .build()
                .start()
    }

    static ServiceCache getServiceCache() {
        return serviceCache
    }

    public static final List<ServiceInstance> getSortedServices() {
        List<ServiceInstance> instances = new ArrayList<ServiceInstance>(serviceProvider.allInstances);
        instances.sort(serviceInstanceComparator);
        return instances;
    }

    public static final Comparator<ServiceInstance> serviceInstanceComparator = new Comparator<ServiceInstance>() {
        @Override
        int compare(ServiceInstance o1, ServiceInstance o2) {
            return Integer.compare(Integer.parseInt(o1.payload as String), Integer.parseInt(o2.payload as String))
        }
    }

    public static List<ServiceInstance> getResponsibleServers(int hash) {
        List<ServiceInstance> instances = new ArrayList<>();
        List<ServiceInstance> all = getSortedServices();
        for (int i=0; i<all.size(); i++) {
            ServiceInstance instance = all.get(i);
            int instanceKey = Integer.parseInt(instance.payload as String);
            if(instanceKey >= hash) {
                instances.add(instance);
                if(instances.size() >= DynamoParams.replicas) {
                    break;
                }
            }
        }
        if(instances.size() < DynamoParams.replicas && all.size() > instances.size()) {
            int i=0;
            while(instances.size() < Math.min(DynamoParams.replicas, all.size())) {
                instances.add(all.get(i));
                i++;
            }
        }
        return instances;
    }
}
