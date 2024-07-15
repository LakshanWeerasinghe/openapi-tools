import ballerina/http;

listener http:Listener ep0 = new (80, config = {host: "petstore.openapi.io"});

service /v1 on ep0 {
    # Info for a specific pet
    #
    # + return - returns can be any of following types
    # http:Ok (Expected response to a valid request)
    # http:DefaultStatusCodeResponse (unexpected error)
    resource function get pets(@http:Header string x\-request\-id, @http:Header string[] x\-request\-client) returns http:Ok|ErrorDefault {
    }
}
