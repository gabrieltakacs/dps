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
                String address = s.buildUriSpec()
                URL url = (address + "/api/v1.0/clockNumber").toURL();
                try {
                    list.add(Integer.parseInt(url.getText([connectTimeout: 1000, readTimeout: 1000])));
                } catch (Exception e) {
                    println("init dynamo error: "+e.getMessage());
                }
            }
        }
        int myNumber;
        String l = list.toString();
        if(list.isEmpty()) {
            myNumber = 0;
            println("first node - 0");
        } else if(list.size() == 1) {
            myNumber = DynamoParams.maxClockNumber/2;
            println("second node - "+myNumber);
        } else {
            int most = 0;
            list.sort();
            for(int i=0;i<list.size();i++) {
                int next;
                if(i<list.size()-1) next = list.get(i+1) else next = DynamoParams.maxClockNumber;
                if(most < (next - list.get(i))) {
                    most = (next - list.get(i));
                    myNumber = (next+list.get(i))/2;
                }
            }
        }
        //TODO remove later
        l +="    "+Integer.toString(myNumber);
        File file = new File("myLog.txt");
        file.createNewFile();
        file.write(l);
        //
        DynamoParams.setMyNumber(myNumber);
        DynamoParams.setNextNumber(DynamoParams.maxClockNumber);
    }
}
