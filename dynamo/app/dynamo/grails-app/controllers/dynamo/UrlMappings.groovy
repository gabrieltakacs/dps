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
        "/api/v1.0/clockNumber/" {
            controller = 'Main'
            action = [GET: "number"]
        }
        "/"(controller:"Main", action:"index")
       // "/"(view:"/index")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
