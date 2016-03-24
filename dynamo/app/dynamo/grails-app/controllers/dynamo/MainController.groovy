package dynamo

import grails.converters.JSON
import org.grails.web.json.JSONObject

class MainController{

    private static int count = 0;
    def index() {

        JSONObject obj = new JSONObject();
        log.info("Index");
        count++;
        obj.put("count", Integer.toString(count));
        render obj as JSON
    }
}