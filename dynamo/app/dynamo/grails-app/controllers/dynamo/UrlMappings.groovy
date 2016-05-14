package dynamo

class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }
        "/api/v1.0/post/" {
            controller = 'Dynamo'
            action = [POST: "postData"]
        }
        "/api/v1.0/get/" {
            controller = 'Dynamo'
            action = [GET: "getData"]
        }
        "/api/v1.0/getAll/" {
            controller = 'Dynamo'
            action = [GET: "getAll"]
        }
        "/api/v1.0/getRange/" {
            controller = 'Dynamo'
            action = [GET: "getRange"]
        }
        "/api/v1.0/clear/" {
            controller = 'Dynamo'
            action = [GET: "clear"]
        }
        "/api/v1.0/clockNumber/" {
            controller = 'Main'
            action = [GET: "number"]
        }
        "/api/v1.0/delete/" {
            controller = 'Dynamo'
            action = [DELETE: "deleteData"]
        }
        "/"(controller:"Main", action:"index")
       // "/"(view:"/index")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
