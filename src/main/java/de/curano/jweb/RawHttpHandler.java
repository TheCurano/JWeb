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

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderValues.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;

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

            var request = new de.curano.jweb.HttpRequest(url, ctx, msg, sslCtx != null, vars, req.headers());

            for (var handler : jWeb.getHandlers()) {
                handler.onRequest(request);
            }

            if (request.isClosed()) {
                return;
            }

            FullHttpResponse response = null;
            if (request.content() != null) {
                response = new DefaultFullHttpResponse(req.protocolVersion(),
                        request.status(),
                        Unpooled.wrappedBuffer(request.content().getBytes(StandardCharsets.UTF_8)));
            } else {
                jWeb.getPageNotFound().onRequest(request);
                response = new DefaultFullHttpResponse(req.protocolVersion(),
                        request.status(),
                        Unpooled.wrappedBuffer(request.content().getBytes(StandardCharsets.UTF_8)));
            }

            response.headers().set(request.responseHeaders());

            // ToDo add "Keep Alive" for any reason
            /*if (keepAlive) {
                if (!req.protocolVersion().isKeepAliveDefault()) {
                    response.headers().set(CONNECTION, KEEP_ALIVE);
                }
            } else {*/
            response.headers().set(CONNECTION, CLOSE);
            // }

            ChannelFuture future = ctx.write(response);
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
