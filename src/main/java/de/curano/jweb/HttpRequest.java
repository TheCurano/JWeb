package de.curano.jweb;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.HashMap;

public class HttpRequest {

    private String path;
    private ChannelHandlerContext ctx;
    private HttpObject msg;
    private boolean https;
    private HashMap<String, String> vars;
    private HttpResponseStatus status = HttpResponseStatus.NOT_FOUND;
    private String content = null;
    private boolean closed = false;
    private HttpHeaders headers;

    protected HttpRequest(String path, ChannelHandlerContext ctx, HttpObject msg, boolean https, HashMap<String, String> vars, HttpHeaders headers) {
        this.path = path;
        this.ctx = ctx;
        this.msg = msg;
        this.https = https;
        this.vars = vars;
        this.headers = headers;
    }

    public String getPath() {
        return path;
    }

    public void close() {
        this.closed = true;
        ctx.close();
    }

    protected boolean isClosed() {
        return this.closed;
    }

    public HttpObject getMsg() {
        return msg;
    }

    public boolean isHttps() {
        return https;
    }

    public HashMap<String, String> getVariables() {
        return vars;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public HttpMethod getMethod() {
        return msg.decoderResult().isSuccess() ? ((io.netty.handler.codec.http.HttpRequest) msg).method() : null;
    }

    public void setResponse(HttpResponseStatus status, String content) {
        this.status = status;
        this.content = content;
        if (this.status == HttpResponseStatus.PERMANENT_REDIRECT || this.status == HttpResponseStatus.TEMPORARY_REDIRECT) {
            this.headers.set("Location", this.content);
        }
    }

    // Take care that you have added the http:// or https:// prefix to the url
    public void setRedirectUrl(String url) {
        this.status = HttpResponseStatus.PERMANENT_REDIRECT;
        this.content = "";
        getHeaders().set("Location", url);
    }

    protected HttpResponseStatus getStatus() {
        return status;
    }

    protected String getContent() {
        return content;
    }

}
