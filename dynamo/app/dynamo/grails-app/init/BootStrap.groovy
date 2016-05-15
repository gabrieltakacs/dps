import dynamo.DynamoParams
import dynamo.Replicator
import dynamo.Zookeeper
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.locks.InterProcessMutex
import org.apache.curator.framework.state.ConnectionState
import org.apache.curator.x.discovery.ServiceInstance
import org.apache.curator.x.discovery.details.ServiceCacheListener

class BootStrap {

    def init = { servletContext ->

        CuratorFramework curatorFramework = Zookeeper.initCuratorFramework();
        InterProcessMutex mutex = new InterProcessMutex(curatorFramework, "/lock");
        println("Bootstrap - accquiring lock")
        mutex.acquire()
        println("Bootstrap - lock acquired")
        Zookeeper.initServiceProvider();
        initDynamo();
        Zookeeper.registerServer();
        mutex.release();
        println("Bootstrap - lock released")
        Zookeeper.getServiceCache().addListener(new ServiceCacheListener() {
            @Override
            void cacheChanged() {
                println("ServiceCacheListener - cacheChanged event");
                Replicator.getInstance().process();
            }

            @Override
            void stateChanged(CuratorFramework client, ConnectionState newState) {
                println("ServiceCacheListener - stateChanged event");
                println(client+" - "+newState);
            }
        });
        new Thread() {
            public void run() {
                Replicator.getInstance().process();
            }
        }.start();
    }

    def destroy = {
    }

    private static void initDynamo() {
        println("init dynamo start");
        ArrayList<Integer> list = new ArrayList<Integer>();
        for(ServiceInstance s:Zookeeper.serviceProvider.allInstances) {
            if (!InetAddress.getLocalHost().getHostAddress().equals(s.address)) {
                list.add(Integer.parseInt(s.payload as String));
            }
        }
        println("Bootstrap - peers id: "+list);
        int myNumber = 0;
        if(list.isEmpty()) {
            myNumber = 0;
            println("Bootstrap - server id 0 (first server)");
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
                    myNumber = ((actual + (distance / 2)) as Integer) % DynamoParams.maxClockNumber;
                }
            }
        }
        println("Bootstrap - server id "+myNumber)
        DynamoParams.setMyNumber(myNumber);
    }
}
