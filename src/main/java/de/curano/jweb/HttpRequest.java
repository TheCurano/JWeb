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
    private HttpHeaders requestHeaders;
    private HttpHeaders responseHeaders = new DefaultHttpHeaders();

    protected HttpRequest(String path, ChannelHandlerContext ctx, HttpObject httpObject, boolean https, HashMap<String, String> vars, HttpHeaders requestHeaders) {
        this.path = path;
        this.ctx = ctx;
        this.httpObject = httpObject;
        this.https = https;
        this.vars = vars;
        this.requestHeaders = requestHeaders;
    }

    public String path() {
        return path;
    }

    public void close() {
        this.closed = true;
        ctx.close();
    }

    protected boolean isClosed() {
        return this.closed;
    }

    public HttpObject httpObject() {
        return httpObject;
    }

    public boolean isHttps() {
        return https;
    }

    public HashMap<String, String> variables() {
        return vars;
    }

    public HttpHeaders requestHeaders() {
        return requestHeaders;
    }

    public HttpHeaders responseHeaders() {
        return responseHeaders;
    }

    public HttpMethod method() {
        return httpObject.decoderResult().isSuccess() ? ((io.netty.handler.codec.http.HttpRequest) httpObject).method() : null;
    }

    public void setResponse(HttpResponseStatus status, String content) {
        this.status = status;
        this.content = content;
        if (this.status == HttpResponseStatus.PERMANENT_REDIRECT || this.status == HttpResponseStatus.TEMPORARY_REDIRECT) {
            this.responseHeaders().set("Location", this.content);
        }
    }

    protected io.netty.handler.codec.http.HttpRequest getNettyHttpRequest() {
        return (io.netty.handler.codec.http.HttpRequest) httpObject;
    }

    // Take care that you have added the http:// or https:// prefix to the url
    public void setRedirectUrl(String url) {
        this.status = HttpResponseStatus.PERMANENT_REDIRECT;
        this.content = "";
        responseHeaders().set("Location", url);
    }

    protected HttpResponseStatus status() {
        return status;
    }

    protected String content() {
        return content;
    }

    public Set<Cookie> cookies() {
        String cookieString = responseHeaders().get(HttpHeaderNames.COOKIE);
        if (cookieString == null) {
            return Set.of();
        }
        return ServerCookieDecoder.STRICT.decode(cookieString);
    }

    public Cookie getCookie(String name) {
        for (Cookie cookie : cookies()) {
            if (cookie.name().equals(name)) {
                return cookie;
            }
        }
        return null;
    }

    public void setCookies(Cookie... cookies) {
        ArrayList<String> cookieHeader = new ArrayList<>(responseHeaders().getAll("Set-Cookie"));
        cookieHeader.addAll(ServerCookieEncoder.STRICT.encode(cookies));
        responseHeaders().set("Set-Cookie", cookieHeader);
    }

    public void setCookies(Set<Cookie> cookies) {
        ArrayList<String> cookieHeader = new ArrayList<>(responseHeaders().getAll("Set-Cookie"));
        cookieHeader.addAll(ServerCookieEncoder.STRICT.encode(cookies));
        responseHeaders().set("Set-Cookie", cookieHeader);
    }

    public void setCookies(List<Cookie> cookies) {
        ArrayList<String> cookieHeader = new ArrayList<>(responseHeaders().getAll("Set-Cookie"));
        cookieHeader.addAll(ServerCookieEncoder.STRICT.encode(cookies));
        responseHeaders().set("Set-Cookie", cookieHeader);
    }

}
