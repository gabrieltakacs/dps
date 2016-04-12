import dynamo.DynamoParams
import dynamo.Zookeeper
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.framework.recipes.locks.InterProcessMutex
import org.apache.curator.retry.RetryNTimes
import org.apache.curator.x.discovery.ServiceDiscovery
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder
import org.apache.curator.x.discovery.ServiceInstance
import org.apache.curator.x.discovery.ServiceProvider
import org.apache.curator.x.discovery.UriSpec

class BootStrap {

    def init = { servletContext ->
        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient("zookeeper:2181", new RetryNTimes(5, 1000))
        curatorFramework.start()
        InterProcessMutex mutex = new InterProcessMutex(curatorFramework, "/lock");
        mutex.acquire()
        setServiceProvider(curatorFramework);
        initDynamo();
        registerInZookeeper(curatorFramework);
        mutex.release();
    }

    def destroy = {
    }

    private static void registerInZookeeper(CuratorFramework curatorFramework) {

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

    private static void setServiceProvider(CuratorFramework curatorFramework) {
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
        Zookeeper.setServiceProvider(serviceProvider);
    }

    private static void initDynamo() {
        ArrayList<Integer> list = new ArrayList<Integer>();
        for(ServiceInstance s:Zookeeper.serviceProvider.allInstances) {
            if (!InetAddress.getLocalHost().getHostAddress().equals(s.address)) {
                println("init dynamo start");
                list.add(Integer.parseInt(s.payload as String));
            }
        }
        int myNumber;
        if(list.isEmpty()) {
            myNumber = 0;
            println("first node - 0");
        } else {
            int most = -1;
            list.sort();
            for(int i=0;i<list.size();i++) {
                int actual = list.get(i);
                int next = list.get((i+1)%list.size());
                int distance = (DynamoParams.maxClockNumber - actual + next) % DynamoParams.maxClockNumber;
                if(most < distance) {
                    most = distance;
                    if(distance == 0) {
                        distance += DynamoParams.maxClockNumber;
                    }
                    myNumber = actual+(distance/2);
                }
            }
        }
        DynamoParams.setMyNumber(myNumber);
    }
}
