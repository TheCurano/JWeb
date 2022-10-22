package de.curano.jweb;

interface HttpHandler {

    public void onRequest(HttpRequest request);

    public void onException(Throwable cause);

}
