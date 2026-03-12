package com.senspark.game.extension.helper

class AddRequestHandlerHelper(
    private val _adder: (requestId: String, theClass: Class<*>) -> Unit
) {
    private val _requests = mutableMapOf<String, Class<*>>()

    fun addRequestHandler(requestId: String, theClass: Class<*>) {
        if (_requests.containsKey(requestId)) {
            throw Exception("Request handler already exists $requestId")
        }
        _requests[requestId] = theClass
        _adder(requestId, theClass)
    }
    
    fun dispose() {
        _requests.clear()
    }
}