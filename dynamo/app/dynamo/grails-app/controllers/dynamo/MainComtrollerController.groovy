package dynamo

import grails.converters.JSON
import org.apache.curator.ensemble.EnsembleProvider
import org.grails.web.json.JSONObject

class MainController implements EnsembleProvider {

    private static int count = 0;
    def index() {

        JSONObject obj = new JSONObject();
        log.info("Index");

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