package de.curano.jweb;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.ssl.SslHandler;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.apache.cxf.transport.http.netty.server.servlet.ChannelThreadLocal;
import org.apache.cxf.transport.http.netty.server.servlet.URIParser;
import org.apache.cxf.transport.http.netty.server.util.Utils;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JHttpRequest implements HttpServletRequest {

    private static final String SSL_CIPHER_SUITE_ATTRIBUTE = "jakarta.servlet.request.cipher_suite";
    private static final String SSL_PEER_CERT_CHAIN_ATTRIBUTE = "jakarta.servlet.request.X509Certificate";
    private static final Locale DEFAULT_LOCALE = Locale.getDefault();
    private URIParser uriParser;
    private HttpRequest originalRequest;
    private QueryStringDecoder queryStringDecoder;
    private Map<String, Object> attributes = new ConcurrentHashMap();
    private String characterEncoding;
    private String contextPath;
    private ChannelHandlerContext channelHandlerContext;
    private HashMap<String, String> urlParameters;
    private boolean closed = false;

    public JHttpRequest(HttpRequest request, String contextPath, ChannelHandlerContext ctx, HashMap<String, String> urlParams) {
        this.originalRequest = request;
        this.contextPath = contextPath;
        this.uriParser = new URIParser(contextPath);
        this.uriParser.parse(request.uri());
        this.queryStringDecoder = new QueryStringDecoder(request.uri());
        this.channelHandlerContext = ctx;
        this.urlParameters = urlParams;
        SslHandler sslHandler = (SslHandler) this.channelHandlerContext.pipeline().get(SslHandler.class);
        if (sslHandler != null) {
            SSLSession session = sslHandler.engine().getSession();
            if (session != null) {
                this.attributes.put("jakarta.servlet.request.cipher_suite", session.getCipherSuite());

                try {
                    this.attributes.put("jakarta.servlet.request.X509Certificate", session.getPeerCertificates());
                } catch (SSLPeerUnverifiedException var7) {
                }
            }
        }

    }

    public HashMap<String, String> getURLParams() {
        return this.urlParameters;
    }

    public HttpRequest getOriginalRequest() {
        return this.originalRequest;
    }

    public String getContextPath() {
        return this.contextPath;
    }

    public void close() {
        closed = true;
    }

    public boolean isClosed() {
        return closed;
    }

    public Set<Cookie> getNettyCookie() {
        String cookieString = this.originalRequest.headers().get(HttpHeaderNames.COOKIE);
        if (cookieString != null) {
            return ServerCookieDecoder.STRICT.decode(cookieString);
        } else {
            return Set.of();
        }
    }

    public Cookie getCookie(String name) {
        for (Cookie cookie : this.getNettyCookie()) {
            if (cookie.name().equals(name)) {
                return cookie;
            }
        }
        return null;
    }

    /**
     * Use getNettyCookies() instead
     */
    @Deprecated
    public jakarta.servlet.http.Cookie[] getCookies() {
        String cookieString = this.originalRequest.headers().get(HttpHeaderNames.COOKIE);
        if (cookieString != null) {
            Set<io.netty.handler.codec.http.cookie.Cookie> cookies = ServerCookieDecoder.STRICT.decode(cookieString);
            if (!cookies.isEmpty()) {
                jakarta.servlet.http.Cookie[] cookiesArray = new jakarta.servlet.http.Cookie[cookies.size()];
                int indx = 0;

                for (Iterator var5 = cookies.iterator(); var5.hasNext(); ++indx) {
                    io.netty.handler.codec.http.cookie.Cookie c = (io.netty.handler.codec.http.cookie.Cookie) var5.next();
                    jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(c.name(), c.value());
                    cookie.setDomain(c.domain());
                    cookie.setMaxAge((int) c.maxAge());
                    cookie.setPath(c.path());
                    cookie.setSecure(c.isSecure());
                    cookiesArray[indx] = cookie;
                }

                return cookiesArray;
            }
        }

        return null;
    }

    public long getDateHeader(String name) {
        String longVal = this.getHeader(name);
        return longVal == null ? -1L : Long.parseLong(longVal);
    }

    public String getHeader(String name) {
        return this.originalRequest.headers().get(name);
    }

    public Enumeration getHeaderNames() {
        return Utils.enumeration(this.originalRequest.headers().names());
    }

    public Enumeration getHeaders(String name) {
        return Utils.enumeration(this.originalRequest.headers().getAll(name));
    }

    public int getIntHeader(String name) {
        return this.originalRequest.headers().getInt(name, -1);
    }

    public String getMethod() {
        return this.originalRequest.method().name();
    }

    public String getQueryString() {
        return this.uriParser.getQueryString();
    }

    public String getRequestURI() {
        return this.uriParser.getRequestUri();
    }

    public StringBuffer getRequestURL() {
        StringBuffer url = new StringBuffer();
        String scheme = this.getScheme();
        int port = this.getServerPort();
        String urlPath = this.getRequestURI();
        url.append(scheme);
        url.append("://");
        url.append(this.getServerName());
        if ("http".equalsIgnoreCase(scheme) && port != 80 || "https".equalsIgnoreCase(scheme) && port != 443) {
            url.append(':');
            url.append(this.getServerPort());
        }

        url.append(urlPath);
        return url;
    }

    public int getContentLength() {
        return HttpUtil.getContentLength(this.originalRequest, -1);
    }

    public String getContentType() {
        return this.originalRequest.headers().get(HttpHeaderNames.CONTENT_TYPE);
    }

    public String getCharacterEncoding() {
        if (this.characterEncoding == null) {
            this.characterEncoding = Utils.getCharsetFromContentType(this.getContentType());
        }

        return this.characterEncoding;
    }

    public String getParameter(String name) {
        String[] values = this.getParameterValues(name);
        return values != null ? values[0] : null;
    }

    public Map getParameterMap() {
        return this.queryStringDecoder.parameters();
    }

    public Enumeration getParameterNames() {
        return Utils.enumerationFromKeys(this.queryStringDecoder.parameters());
    }

    public String[] getParameterValues(String name) {
        List<String> values = (List) this.queryStringDecoder.parameters().get(name);
        return values != null && !values.isEmpty() ? (String[]) values.toArray(new String[0]) : null;
    }

    public String getProtocol() {
        return this.originalRequest.protocolVersion().toString();
    }

    public Object getAttribute(String name) {
        return this.attributes != null ? this.attributes.get(name) : null;
    }

    public Enumeration getAttributeNames() {
        return Utils.enumerationFromKeys(this.attributes);
    }

    public void removeAttribute(String name) {
        if (this.attributes != null) {
            this.attributes.remove(name);
        }

    }

    public void setAttribute(String name, Object o) {
        this.attributes.put(name, o);
    }

    public String getRequestedSessionId() {
        return null;
    }

    public HttpSession getSession() {
        return null;
    }

    public HttpSession getSession(boolean create) {
        return null;
    }

    public String getPathInfo() {
        return this.uriParser.getPathInfo();
    }

    public Locale getLocale() {
        String locale = this.originalRequest.headers().get(HttpHeaderNames.ACCEPT_LANGUAGE, DEFAULT_LOCALE.toString());
        return new Locale(locale);
    }

    public String getRemoteAddr() {
        InetSocketAddress addr = (InetSocketAddress) ChannelThreadLocal.get().remoteAddress();
        return addr.getAddress().getHostAddress();
    }

    public String getRemoteHost() {
        InetSocketAddress addr = (InetSocketAddress) ChannelThreadLocal.get().remoteAddress();
        return addr.getHostName();
    }

    public int getRemotePort() {
        InetSocketAddress addr = (InetSocketAddress) ChannelThreadLocal.get().remoteAddress();
        return addr.getPort();
    }

    public String getServerName() {
        InetSocketAddress addr = (InetSocketAddress) ChannelThreadLocal.get().localAddress();
        return addr.getHostName();
    }

    public int getServerPort() {
        InetSocketAddress addr = (InetSocketAddress) ChannelThreadLocal.get().localAddress();
        return addr.getPort();
    }

    public String getServletPath() {
        String servletPath = this.uriParser.getServletPath();
        return "/".equals(servletPath) ? "" : servletPath;
    }

    public String getScheme() {
        return this.isSecure() ? "https" : "http";
    }

    public boolean isSecure() {
        return ChannelThreadLocal.get().pipeline().get(SslHandler.class) != null;
    }

    public String getLocalAddr() {
        InetSocketAddress addr = (InetSocketAddress) ChannelThreadLocal.get().localAddress();
        return addr.getAddress().getHostAddress();
    }

    public String getLocalName() {
        return this.getServerName();
    }

    public int getLocalPort() {
        return this.getServerPort();
    }

    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        this.characterEncoding = env;
    }

    public Enumeration getLocales() {
        Collection<Locale> locales = Utils.parseAcceptLanguageHeader(this.originalRequest.headers().get(HttpHeaderNames.ACCEPT_LANGUAGE));
        if (locales == null || ((Collection) locales).isEmpty()) {
            locales = new ArrayList();
            ((Collection) locales).add(Locale.getDefault());
        }

        return Utils.enumeration((Collection) locales);
    }

    public ServletInputStream getInputStream() throws IOException {
        return null;
    }

    public BufferedReader getReader() throws IOException {
        return null;
    }

    public String getAuthType() {
        return null;
    }

    public String getPathTranslated() {
        return null;
    }

    public String getRemoteUser() {
        return null;
    }

    public Principal getUserPrincipal() {
        return null;
    }

    public boolean isRequestedSessionIdFromCookie() {
        throw new IllegalStateException("Method 'isRequestedSessionIdFromCookie' not yet implemented!");
    }

    public boolean isRequestedSessionIdFromURL() {
        throw new IllegalStateException("Method 'isRequestedSessionIdFromURL' not yet implemented!");
    }

    public boolean isRequestedSessionIdFromUrl() {
        throw new IllegalStateException("Method 'isRequestedSessionIdFromUrl' not yet implemented!");
    }

    public boolean isRequestedSessionIdValid() {
        throw new IllegalStateException("Method 'isRequestedSessionIdValid' not yet implemented!");
    }

    public boolean isUserInRole(String role) {
        throw new IllegalStateException("Method 'isUserInRole' not yet implemented!");
    }

    public String getRealPath(String path) {
        throw new IllegalStateException("Method 'getRealPath' not yet implemented!");
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        throw new IllegalStateException("Method 'getRequestDispatcher' not yet implemented!");
    }

    public long getContentLengthLong() {
        throw new IllegalStateException("Method 'getContentLengthLong' not yet implemented!");
    }

    public ServletContext getServletContext() {
        throw new IllegalStateException("Method 'getServletContext' not yet implemented!");
    }

    public AsyncContext startAsync() throws IllegalStateException {
        throw new IllegalStateException("Method 'startAsync' not yet implemented!");
    }

    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        throw new IllegalStateException("Method 'startAsync' not yet implemented!");
    }

    public boolean isAsyncStarted() {
        throw new IllegalStateException("Method 'isAsyncStarted' not yet implemented!");
    }

    public boolean isAsyncSupported() {
        throw new IllegalStateException("Method 'isAsyncSupported' not yet implemented!");
    }

    public AsyncContext getAsyncContext() {
        throw new IllegalStateException("Method 'getAsyncContext' not yet implemented!");
    }

    public DispatcherType getDispatcherType() {
        throw new IllegalStateException("Method 'getDispatcherType' not yet implemented!");
    }

    public String changeSessionId() {
        throw new IllegalStateException("Method 'changeSessionId' not yet implemented!");
    }

    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        throw new IllegalStateException("Method 'authenticate' not yet implemented!");
    }

    public void login(String username, String password) throws ServletException {
        throw new IllegalStateException("Method 'login' not yet implemented!");
    }

    public void logout() throws ServletException {
        throw new IllegalStateException("Method 'logout' not yet implemented!");
    }

    public Collection<Part> getParts() throws IOException, ServletException {
        throw new IllegalStateException("Method 'getParts' not yet implemented!");
    }

    public Part getPart(String name) throws IOException, ServletException {
        throw new IllegalStateException("Method 'getPart' not yet implemented!");
    }

    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        throw new IllegalStateException("Method 'upgrade' not yet implemented!");
    }

}
