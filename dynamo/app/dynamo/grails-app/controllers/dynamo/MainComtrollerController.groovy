package dynamo

import grails.converters.JSON

class TestController {
    def index() {
        def response = [
                code: "200",
                message: "success"
        ];
        render response as JSON
    }
}