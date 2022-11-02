package de.curano.jweb;

public interface HttpHandler {

    public void onRequest(JHttpRequest request, JHttpResponse response);

    public void onException(Throwable cause);

}
