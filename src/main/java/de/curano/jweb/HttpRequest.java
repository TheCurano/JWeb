package de.curano.jweb;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class HttpRequest {

    private String path;
    private ChannelHandlerContext ctx;
    private HttpObject httpObject;
    private boolean https;
    private HashMap<String, String> vars;
    private HttpResponseStatus status = HttpResponseStatus.NOT_FOUND;
    private String content = null;
    private boolean closed = false;
    private HttpHeaders headers;

    protected HttpRequest(String path, ChannelHandlerContext ctx, HttpObject httpObject, boolean https, HashMap<String, String> vars, HttpHeaders headers) {
        this.path = path;
        this.ctx = ctx;
        this.httpObject = httpObject;
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

    public HttpObject getHttpObject() {
        return httpObject;
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
        return httpObject.decoderResult().isSuccess() ? ((io.netty.handler.codec.http.HttpRequest) httpObject).method() : null;
    }

    public void setResponse(HttpResponseStatus status, String content) {
        this.status = status;
        this.content = content;
        if (this.status == HttpResponseStatus.PERMANENT_REDIRECT || this.status == HttpResponseStatus.TEMPORARY_REDIRECT) {
            this.headers.set("Location", this.content);
        }
    }

    protected io.netty.handler.codec.http.HttpRequest getNettyHttpRequest() {
        return (io.netty.handler.codec.http.HttpRequest) httpObject;
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

    public Set<Cookie> getCookies() {
        String cookieString = headers.get(HttpHeaderNames.COOKIE);
        if (cookieString == null) {
            return Set.of();
        }
        return ServerCookieDecoder.STRICT.decode(cookieString);
    }

    public Cookie getCookie(String name) {
        for (Cookie cookie : getCookies()) {
            if (cookie.name().equals(name)) {
                return cookie;
            }
        }
        return null;
    }

    public void setCookies(Cookie... cookie) {;
        headers.set("Set-Cookie", ServerCookieEncoder.STRICT.encode(cookie));
    }

    public void setCookies(Set<Cookie> cookies) {
        headers.set("Set-Cookie", ServerCookieEncoder.STRICT.encode(cookies));
    }

    public void setCookies(List<Cookie> cookies) {
        headers.set("Set-Cookie", ServerCookieEncoder.STRICT.encode(cookies));
    }

}
