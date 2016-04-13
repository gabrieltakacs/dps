package dynamo

import org.apache.curator.x.discovery.ServiceCache
import org.apache.curator.x.discovery.ServiceInstance
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
