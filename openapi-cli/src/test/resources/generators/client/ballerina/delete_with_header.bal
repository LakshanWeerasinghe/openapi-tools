import ballerina/http;

public isolated client class Client {
    final http:Client clientEp;
    # Gets invoked to initialize the `connector`.
    #
    # + config - The configurations to be used when initializing the `connector`
    # + serviceUrl - URL of the target service
    # + return - An error if connector initialization failed
    public isolated function init(string serviceUrl, ConnectionConfig config =  {}) returns error? {
        http:ClientConfiguration httpClientConfig = {httpVersion: config.httpVersion, timeout: config.timeout, forwarded: config.forwarded, poolConfig: config.poolConfig, compression: config.compression, circuitBreaker: config.circuitBreaker, retryConfig: config.retryConfig, validation: config.validation};
        do {
            if config.http1Settings is ClientHttp1Settings {
                ClientHttp1Settings settings = check config.http1Settings.ensureType(ClientHttp1Settings);
                httpClientConfig.http1Settings = {...settings};
            }
            if config.http2Settings is http:ClientHttp2Settings {
                httpClientConfig.http2Settings = check config.http2Settings.ensureType(http:ClientHttp2Settings);
            }
            if config.cache is http:CacheConfig {
                httpClientConfig.cache = check config.cache.ensureType(http:CacheConfig);
            }
            if config.responseLimits is http:ResponseLimitConfigs {
                httpClientConfig.responseLimits = check config.responseLimits.ensureType(http:ResponseLimitConfigs);
            }
            if config.secureSocket is http:ClientSecureSocket {
                httpClientConfig.secureSocket = check config.secureSocket.ensureType(http:ClientSecureSocket);
            }
            if config.proxy is http:ProxyConfig {
                httpClientConfig.proxy = check config.proxy.ensureType(http:ProxyConfig);
            }
        }
        http:Client httpEp = check new (serviceUrl, httpClientConfig);
        self.clientEp = httpEp;
        return;
    }

    # Delete with header.
    #
    # + headers - Headers to be sent with the request
    # + return - Status OK
    remote isolated function deleteHeader(DeleteHeaderHeaders headers) returns error? {
        string resourcePath = string `/header`;
        map<string|string[]> httpHeaders = getMapForHeaders(headers);
        return self.clientEp->delete(resourcePath, headers = httpHeaders);
    }

    # Delete with header and request body.
    #
    # + headers - Headers to be sent with the request
    # + return - Status OK
    remote isolated function deleteHeaderRequestBody(DeleteHeaderRequestBodyHeaders headers, json payload) returns error? {
        string resourcePath = string `/header-with-request-body`;
        map<string|string[]> httpHeaders = getMapForHeaders(headers);
        http:Request request = new;
        request.setPayload(payload, "application/json");
        return self.clientEp->delete(resourcePath, request, httpHeaders);
    }

    # Delete neither header nor request body.
    #
    # + order_id - Order ID
    # + risk_id - Order Risk ID
    # + headers - Headers to be sent with the request
    # + return - Status OK
    remote isolated function deleteOrderRisk(string order_id, string risk_id, map<string|string[]> headers = {}) returns error? {
        string resourcePath = string `/admin/api/2021-10/orders/${getEncodedUri(order_id)}/risks/${getEncodedUri(risk_id)}.json`;
        return self.clientEp->delete(resourcePath, headers = headers);
    }

    # Delete with request body.
    #
    # + headers - Headers to be sent with the request
    # + return - Status OK
    remote isolated function orderRisk(json payload, map<string|string[]> headers = {}) returns error? {
        string resourcePath = string `/request-body`;
        http:Request request = new;
        request.setPayload(payload, "application/json");
        return self.clientEp->delete(resourcePath, request, headers);
    }
}
