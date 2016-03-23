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
        count++;

        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient("zookeeper.dev:2181", new RetryOneTime(1000))
        curatorFramework.start()
        ServiceInstance<Void> serviceInstance = ServiceInstance.builder()
                .uriSpec(new UriSpec("{scheme}://{address}:{port}"))
                .address('localhost')
                .port(8080)
                .name("dynamo")
                .build()
        ServiceDiscoveryBuilder.builder(Void)
                .basePath("/traefik/backends/backend1/servers")
                .client(curatorFramework)
                .thisInstance(serviceInstance)
                .build()
                .start()



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