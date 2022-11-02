package de.curano.jweb;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.ssl.SslContext;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class RawHttpHandler extends SimpleChannelInboundHandler<HttpObject> {

    private JWeb jWeb;
    private SslContext sslCtx;

    protected RawHttpHandler(JWeb jWeb, SslContext sslCtx) {
        this.jWeb = jWeb;
        this.sslCtx = sslCtx;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
        if (msg instanceof HttpRequest req) {
            // boolean keepAlive = HttpUtil.isKeepAlive(req);
            String url = req.uri().split("\\?")[0];
            HashMap<String, String> vars = new HashMap<>();
            if (req.uri().contains("?") && req.uri().contains("=")) {
                if (req.uri().contains("&")) {
                    String[] varsArray = req.uri().split("\\?")[1].split("&");
                    for (String var : varsArray) {
                        String[] varArray = var.split("=");
                        vars.put(varArray[0], varArray[1]);
                    }
                } else {
                    String[] varArray = req.uri().split("\\?")[1].split("=");
                    vars.put(varArray[0], varArray[1]);
                }
            }

            var request = new JHttpRequest(req, url, ctx, vars);
            var response = new JHttpResponse(new DefaultFullHttpResponse(req.protocolVersion(), HttpResponseStatus.NOT_FOUND, Unpooled.copiedBuffer("404 Page not found", StandardCharsets.UTF_8)));

            for (var handler : jWeb.getHandlers()) {
                handler.onRequest(request, response);
            }

            if (!response.isContentSet()) {
                jWeb.getPageNotFound().onRequest(request, response);
            }

            if (request.isClosed()) {
                return;
            }

            // ToDo add "Keep Alive" for any reason
            /*if (keepAlive) {
                if (!req.protocolVersion().isKeepAliveDefault()) {
                    response.headers().set(CONNECTION, KEEP_ALIVE);
                }
            } else {*/
            response.setHeader(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE.toString());
            // }

            ChannelFuture future = ctx.write(response.getOriginalResponse());
            // if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
            // }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        for (var handler : jWeb.getHandlers()) {
            handler.onException(cause);
        }
        ctx.close();
    }

}
