package de.curano.jweb;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.handler.ssl.SslContext;

public class RawHttpInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;
    private final JWeb jWeb;

    protected RawHttpInitializer(SslContext sslCtx, JWeb jWeb) {
        this.sslCtx = sslCtx;
        this.jWeb = jWeb;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        if (sslCtx != null) {
            p.addLast(sslCtx.newHandler(ch.alloc()));
        }
        p.addLast(new HttpServerCodec());
        p.addLast(new HttpServerExpectContinueHandler());
        p.addLast(new RawHttpHandler(jWeb, sslCtx));
    }

}
