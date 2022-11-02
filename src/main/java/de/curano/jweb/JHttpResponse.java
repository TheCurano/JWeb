package de.curano.jweb;

import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.util.AsciiString;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.cxf.transport.http.netty.server.servlet.NettyServletOutputStream;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;

public class JHttpResponse implements HttpServletResponse {

    private FullHttpResponse originalResponse;
    private NettyServletOutputStream outputStream;
    private PrintWriter writer;
    private boolean responseCommited;
    private boolean contentSet = false;

    public JHttpResponse(FullHttpResponse response) {
        this.originalResponse = response;
        this.outputStream = new NettyServletOutputStream((HttpContent) response);
        this.writer = new PrintWriter(this.outputStream);
    }

    public FullHttpResponse getOriginalResponse() {
        return originalResponse;
    }

    public void addDateHeader(String name, long date) {
        this.originalResponse.headers().set(name, date);
    }

    public void addHeader(String name, String value) {
        this.originalResponse.headers().set(name, value);
    }

    public void addIntHeader(String name, int value) {
        this.originalResponse.headers().set(name, value);
    }

    public void setResponse(HttpResponseStatus status, String content) {
        this.originalResponse.content().clear();
        this.originalResponse.content().writeBytes(content.getBytes(StandardCharsets.UTF_8));
        this.originalResponse.setStatus(status);
        contentSet = true;
    }

    @Override
    public void addCookie(jakarta.servlet.http.Cookie cookie) {
        DefaultCookie cookie1 = new DefaultCookie(cookie.getName(), cookie.getValue());
        cookie1.setSecure(cookie.getSecure());
        cookie1.setHttpOnly(cookie.isHttpOnly());
        cookie1.setMaxAge(cookie.getMaxAge());
        cookie1.setPath(cookie.getPath());
        cookie1.setDomain(cookie.getDomain());
        // ToDo Comment
        ArrayList<String> cookieHeader = new ArrayList<>(this.originalResponse.headers().getAll("Set-Cookie"));
        cookieHeader.add(ServerCookieEncoder.STRICT.encode(cookie1));
        this.originalResponse.headers().set("Set-Cookie", cookieHeader);
    }

    public void addCookie(Cookie cookie) {
        ArrayList<String> cookieHeader = new ArrayList<>(this.originalResponse.headers().getAll("Set-Cookie"));
        cookieHeader.add(ServerCookieEncoder.STRICT.encode(cookie));
        this.originalResponse.headers().set("Set-Cookie", cookieHeader);
    }

    public boolean containsHeader(String name) {
        return this.originalResponse.headers().contains(name);
    }

    public void sendError(int sc) throws IOException {
        this.originalResponse.setStatus(HttpResponseStatus.valueOf(sc));
    }

    public void sendError(int sc, String msg) throws IOException {
        this.originalResponse.setStatus(new HttpResponseStatus(sc, msg));

    }

    public void sendRedirect(String location) throws IOException {
        setStatus(302);
        setHeader(LOCATION, location);
    }

    public void setDateHeader(String name, long date) {
        this.originalResponse.headers().set(name, date);
    }

    public void setHeader(AsciiString name, String value) {
        this.originalResponse.headers().set(name, value);
    }

    public void setHeader(String name, String value) {
        this.originalResponse.headers().set(name, value);
    }

    public void setIntHeader(String name, int value) {
        this.originalResponse.headers().set(name, value);

    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return this.outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return this.writer;
    }

    public void setStatus(int sc) {
        this.originalResponse.setStatus(HttpResponseStatus.valueOf(sc));
    }

    public void setStatus(int sc, String sm) {
        this.originalResponse.setStatus(new HttpResponseStatus(sc, sm));
    }

    @Override
    public void setContentType(String type) {
        this.originalResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, type);
    }

    @Override
    public void setContentLength(int len) {
        HttpUtil.setContentLength(this.originalResponse, len);
    }

    @Override
    public boolean isCommitted() {
        return this.responseCommited;
    }

    @Override
    public void reset() {
        if (isCommitted()) {
            throw new IllegalStateException("Response already commited!");
        }

        this.originalResponse.headers().clear();
        this.resetBuffer();
    }

    @Override
    public void resetBuffer() {
        if (isCommitted()) {
            throw new IllegalStateException("Response already commited!");
        }
        this.outputStream.resetBuffer();
    }

    @Override
    public void flushBuffer() throws IOException {
        this.getWriter().flush();
        this.responseCommited = true;
    }

    @Override
    public int getBufferSize() {
        return this.outputStream.getBufferSize();
    }

    @Override
    public void setBufferSize(int size) {
        // we using always dynamic buffer for now
    }

    public String encodeRedirectURL(String url) {
        return this.encodeURL(url);
    }

    public String encodeRedirectUrl(String url) {
        return this.encodeURL(url);
    }

    public String encodeURL(String url) {
        try {
            return URLEncoder.encode(url, getCharacterEncoding());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Error encoding url!", e);
        }
    }

    public String encodeUrl(String url) {
        return this.encodeRedirectURL(url);
    }

    @Override
    public int getStatus() {
        return this.originalResponse.status().code();
    }

    @Override
    public String getHeader(String name) {
        return this.originalResponse.headers().get(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return this.originalResponse.headers().getAll(name);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return this.originalResponse.headers().names();
    }

    protected boolean isContentSet() {
        return contentSet;
    }

    @Override
    public String getCharacterEncoding() {
        throw new IllegalStateException(
                "Method 'getCharacterEncoding' not yet implemented!");
    }

    @Override
    public String getContentType() {
        throw new IllegalStateException(
                "Method 'getContentType' not yet implemented!");
    }

    @Override
    public Locale getLocale() {
        throw new IllegalStateException(
                "Method 'getLocale' not yet implemented!");
    }

    @Override
    public void setCharacterEncoding(String charset) {
        throw new IllegalStateException(
                "Method 'setCharacterEncoding' not yet implemented!");
    }

    @Override
    public void setLocale(Locale loc) {
        throw new IllegalStateException(
                "Method 'setLocale' not yet implemented!");
    }

    @Override
    public void setContentLengthLong(long len) {
        throw new IllegalStateException(
                "Method 'setContentLengthLong' not yet implemented!");
    }
}
