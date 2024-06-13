import ballerina/http;

listener http:Listener ep0 = new (443, config = {host: "api.example.com"});

service / on ep0 {
    # Returns pet inventories by status
    #
    # + return - successful operation
    resource function get store/inventory() returns inline_response_200 {
    }

    # Returns pet inventories by status
    #
    # + return - successful operation
    resource function get store/inventory02() returns StoreInventory02Response|string {
    }

    # Returns pet inventories by status
    #
    # + return - successful operation
    resource function get store/inventory03() returns StoreInventory03Response {
    }

    # Returns pet inventories by status
    #
    # + return - successful operation
    resource function get store/inventory04() returns StoreInventory04Response {
    }

    # + return - successful operation
    resource function get store/inventory05() returns StoreInventory05ResponseBadRequest {
    }
}
