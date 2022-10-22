package de.curano.jweb;

public interface HttpHandler {

    public void onRequest(HttpRequest request);

    public void onException(Throwable cause);

}
