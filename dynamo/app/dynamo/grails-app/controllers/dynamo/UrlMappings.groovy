package dynamo

class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }
        "/about"(controller: "Main", action: "info")
        "/neighbours"(controller: "Main", action: "neighbours")
        "/number"(controller: "Main", action: "getNumber")
        "/"(view:"/dynamo")
        "/default"(view:"/index")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
