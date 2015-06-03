/*
 * Copyright (c) 2015, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */

package tonivade.db;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import tonivade.db.redis.RedisToken;

/**
 *
 * @author tomby
 *
 */
public interface ITinyDB {

    /**
     * Metodo llamado cuando se crea un nuevo canal
     *
     * @param channel
     */
    public void channel(SocketChannel channel);

    /**
     * Método llamado cuando la conexión se ha establecido y está activa
     *
     * @param ctx
     */
    public void connected(ChannelHandlerContext ctx);

    /**
     * Metodo llamado cuando se pierde la conexión
     *
     * @param ctx
     */
    public void disconnected(ChannelHandlerContext ctx);

    /**
     * Método llamado cuando se recibe un mensaje
     *
     * @param ctx
     * @param message
     */
    public void receive(ChannelHandlerContext ctx, RedisToken<?> message);

}