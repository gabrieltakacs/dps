package dynamo

import grails.converters.JSON
import org.apache.curator.ensemble.EnsembleProvider
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.RetryOneTime
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder
import org.apache.curator.x.discovery.ServiceInstance
import org.apache.curator.x.discovery.UriSpec
import org.grails.web.json.JSONObject

class MainController implements EnsembleProvider {

    private static int count = 0;
    def index() {

        JSONObject obj = new JSONObject();
        log.info("Index");

        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient("zookeeper.dev:2181", new RetryOneTime(1000))
        curatorFramework.start()
        ServiceInstance<Void> serviceInstance = ServiceInstance.builder()
                .uriSpec(new UriSpec("{scheme}://{address}:{port}"))
                .address('localhost')
                .port(8080)
                .name("dynamo")
                .build()
        ServiceDiscoveryBuilder.builder(Void)
                .basePath("/traefik")
                .client(curatorFramework)
                .thisInstance(serviceInstance)
                .build()
                .start()

        /*
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
        }*/

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

}