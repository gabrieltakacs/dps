package proxy

class UrlMappings {

    static mappings = {
        "/favicon.ico" (controller: "Proxy", action: "favicon")//TODO toto treba inak spraviť
        "/**"(controller: "Proxy", action: "index")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
