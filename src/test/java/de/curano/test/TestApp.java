package de.curano.test;

import de.curano.jweb.HttpHandler;
import de.curano.jweb.HttpRequest;
import de.curano.jweb.JWeb;
import io.netty.handler.codec.http.HttpResponseStatus;

public class TestApp {

    public static void main(String[] args) {
        // Creating an JWeb Instance
        JWeb jWeb = new JWeb();

        // Binding the HTTP Server to Port 80
        jWeb.bindHttp(80);

        // Adding PageNotFound Handler (you don't have to do this)
        jWeb.setPageNotFound(new HttpHandler() {
            @Override
            public void onRequest(HttpRequest request) {
                request.setResponse(HttpResponseStatus.NOT_FOUND, "Page not found");
            }

            @Override
            public void onException(Throwable throwable) {
                throwable.printStackTrace();
            }
        });

        // Adding a new Handler
        jWeb.addHandler(new HttpHandler() {
            @Override
            public void onRequest(HttpRequest request) {
                // Setting the Response
                if (request.path().equals("/")) {
                    request.setResponse(HttpResponseStatus.ACCEPTED, "Hello World!");
                }
            }

            @Override
            public void onException(Throwable cause) {

            }
        });
    }

}
