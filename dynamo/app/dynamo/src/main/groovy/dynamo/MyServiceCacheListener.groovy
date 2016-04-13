package dynamo

import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.state.ConnectionState
import org.apache.curator.x.discovery.details.ServiceCacheListener

class MyServiceCacheListener {

    private static ServiceCacheListener listener = new ServiceCacheListener() {
        @Override
        void cacheChanged() {
            println("cacheChanged()");
            println(Zookeeper.serviceProvider.allInstances);
        }

        @Override
        void stateChanged(CuratorFramework client, ConnectionState newState) {
            println("stateChanged");
            println(client+"  "+newState);
        }
    };

    private MyServiceCacheListener() {
        throw new AssertionError();
    }

    public static ServiceCacheListener getInstance() {
        return listener;
    }
}
