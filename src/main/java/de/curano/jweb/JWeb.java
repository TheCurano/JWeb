package de.curano.jweb;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import javax.net.ssl.SSLException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JWeb {

    private int httpPort = 8080;
    // private int httpAcceptorThreads = 2;
    // private int httpClientThreads = 2;
    private EventLoopGroup httpAcceptorGroup = null;
    private EventLoopGroup httpClientGroup = null;
    private Channel httpChannel = null;

    private int httpsPort = 8081;
    // private int httpsAcceptorThreads = 2;
    // private int httpsClientThreads = 2;
    private EventLoopGroup httpsAcceptorGroup = null;
    private EventLoopGroup httpsClientGroup = null;
    private Channel httpsChannel = null;
    protected HttpHandler pageNotFound = new HttpHandler() {
        @Override
        public void onRequest(JHttpRequest request, JHttpResponse response) {
            response.setResponse(HttpResponseStatus.NOT_FOUND, "404 Page not found");
        }

        @Override
        public void onException(Throwable cause) {

        }
    };

    protected ArrayList<HttpHandler> handlers = new ArrayList<>();

    public JWeb() {  }

    public void bindHttp(int port) {
        bindHttp(port, 4, 4);
    }

    public void bindHttp(int port, int acceptorThreads, int clientThreads) {
        this.httpPort = port;
        // this.httpAcceptorThreads = acceptorThreads;
        // this.httpClientThreads = clientThreads;

        this.httpAcceptorGroup = new NioEventLoopGroup(acceptorThreads);
        this.httpClientGroup = new NioEventLoopGroup(clientThreads);
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024);
            b.group(this.httpAcceptorGroup, this.httpClientGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new RawHttpInitializer(null, this));
            this.httpChannel = b.bind(port).sync().channel();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void bindHttps(int port, String certPem, String privkeyPem) throws SSLException {
        bindHttps(port, new File(certPem), new File(privkeyPem), 4, 4);
    }

    public void bindHttps(int port, File certPem, File privkeyPem) throws SSLException {
        bindHttps(port, certPem, privkeyPem, 4, 4);
    }

    public void bindHttps(int port, File certPem, File privkeyPem, int acceptorThreads, int clientThreads) throws SSLException {
        this.httpsPort = port;
        // this.httpsAcceptorThreads = acceptorThreads;
        // this.httpsClientThreads = clientThreads;

        this.httpsAcceptorGroup = new NioEventLoopGroup(acceptorThreads);
        this.httpsClientGroup = new NioEventLoopGroup(clientThreads);
        try {
            SslContext sslCtx = SslContextBuilder.forServer(certPem, privkeyPem).build();
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024);
            b.group(this.httpsAcceptorGroup, this.httpsClientGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new RawHttpInitializer(sslCtx, this));
            this.httpsChannel = b.bind(port).sync().channel();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void shutdown() {
        if (this.httpChannel != null && this.httpChannel.isActive()) {
            this.httpChannel.close();
        }
        if (this.httpAcceptorGroup != null) {
            this.httpAcceptorGroup.shutdownGracefully();
        }
        if (this.httpClientGroup != null) {
            this.httpClientGroup.shutdownGracefully();
        }
        if (this.httpsChannel != null && this.httpsChannel.isActive()) {
            this.httpsChannel.close();
        }
        if (this.httpsAcceptorGroup != null) {
            this.httpsAcceptorGroup.shutdownGracefully();
        }
        if (this.httpsClientGroup != null) {
            this.httpsClientGroup.shutdownGracefully();
        }
    }

    public boolean isHttpActive() {
        return this.httpChannel != null && this.httpChannel.isActive();
    }

    public boolean isHttpsActive() {
        return this.httpsChannel != null && this.httpsChannel.isActive();
    }

    public int getHttpPort() {
        return this.httpPort;
    }

    public int getHttpsPort() {
        return this.httpsPort;
    }

    public void addHandler(HttpHandler handler) {
        this.handlers.add(handler);
    }

    public void removeHandler(HttpHandler handler) {
        this.handlers.remove(handler);
    }

    public List<HttpHandler> getHandlers() {
        return (List<HttpHandler>) this.handlers.clone();
    }

    public void setPageNotFound(HttpHandler handler) {
        this.pageNotFound = handler;
    }

    public HttpHandler getPageNotFound() {
        return this.pageNotFound;
    }

}
