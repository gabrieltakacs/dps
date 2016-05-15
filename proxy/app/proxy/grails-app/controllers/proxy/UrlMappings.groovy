package proxy

class UrlMappings {

    static mappings = {
        "/**"(controller: "Proxy", action: "index")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
