package dynamo

import grails.converters.JSON
import org.apache.curator.ensemble.EnsembleProvider
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.RetryNTimes
import org.apache.curator.retry.RetryOneTime
import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.ZooDefs
import org.grails.web.json.JSONObject

class MainController implements EnsembleProvider {

    private static int count = 0;
    def index() {

        JSONObject obj = new JSONObject();


        log.info("Index");
        CuratorFramework client = CuratorFrameworkFactory.newClient("zookeeper.dev:2181", new RetryOneTime(1000))
        client.start();
        try {
            client.blockUntilConnected();
            log.info("connected");
            client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE).forPath("/ABC", "ggg".getBytes());
            obj.put("status", "200");
            obj.put("statusMessage", "success");
        } catch (Exception e) {
            obj.put("status", "400");
            obj.put("statusMessage", e.getMessage());
            log.error("Error while registering server: "+e.getMessage());
        }

        count++;

        obj.put("count", Integer.toString(count));
        render obj as JSON
    }

    @Override
    void start() throws Exception {
        log.info("START");
    }

    @Override
    String getConnectionString() {
        return null
    }

    @Override
    void close() throws IOException {
        log.info("CLOSE");
    }

    private static void registerInZookeeper(int port) {
        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient("localhost:2181", new RetryNTimes(5, 1000))
        curatorFramework.start()
        ServiceInstance<Void> serviceInstance = ServiceInstance.builder()
                .uriSpec(new UriSpec("{scheme}://{address}:{port}"))
                .address('localhost')
                .port(port)
                .name("worker")
                .build()

        ServiceDiscoveryBuilder.builder(Void)
                .basePath("load-balancing-example")
                .client(curatorFramework)
                .thisInstance(serviceInstance)
                .build()
                .start()
    }

}