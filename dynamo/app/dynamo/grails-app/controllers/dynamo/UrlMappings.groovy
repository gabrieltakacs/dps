package dynamo

class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }
        "/api/v1.0/post/" {
            controller = 'Main'
            action = [POST: "postData"]
        }
        "/api/v1.0/get/" {
            controller = 'Main'
            action = [GET: "getData"]
        }
        "/api/v1.0/clockNumber/" {
            controller = 'Main'
            action = [GET: "number"]
        }
        "/"(view:"/index")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
