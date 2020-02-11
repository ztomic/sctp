/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.mobicents.protocols.sctp.netty;

import io.netty.channel.ChannelHandlerContext;

import java.net.InetSocketAddress;

import org.mobicents.protocols.api.IpChannelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler implementation for the SCTP echo client. It initiates the ping-pong traffic between the echo client and server by
 * sending the first message to the server.
 * 
 * @author <a href="mailto:amit.bhayani@telestax.com">Amit Bhayani</a>
 * 
 */
public class NettySctpClientHandler extends NettySctpChannelInboundHandlerAdapter {

    private final Logger logger = LoggerFactory.getLogger(NettySctpClientHandler.class);

    /**
     * Creates a client-side handler.
     */
    public NettySctpClientHandler(NettyAssociationImpl nettyAssociationImpl) {
        this.association = nettyAssociationImpl;
    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception {
        logger.warn(String.format("ChannelUnregistered event: association=%s", association));
        this.association.setChannelHandler(null);

        this.association.scheduleConnect();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("channelRegistered event: association=%s", this.association));
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("channelActive event: association=%s", this.association));
        }

        this.ctx = ctx;
        this.channel = ctx.channel();
        this.association.setChannelHandler(this);

        String host = null;
        int port = 0;
        InetSocketAddress sockAdd = ((InetSocketAddress) channel.remoteAddress());
        if (sockAdd != null) {
            host = sockAdd.getAddress().getHostAddress();
            port = sockAdd.getPort();
        }

        if (logger.isInfoEnabled()) {
            logger.info(String.format("Association=%s connected to host=%s port=%d", association.getName(), host, port));
        }

        if (association.getIpChannelType() == IpChannelType.TCP) {
            this.association.markAssociationUp(1, 1);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        logger.error("ExceptionCaught for Associtaion: " + this.association.getName() + "\n", cause);
        ctx.close();

//        this.association.scheduleConnect();
    }

    // byte [] m3uaMessage = new byte
    // []{1,0,3,1,0,0,0,24,0,17,0,8,0,0,0,1,0,4,0,8,84,101,115,116};
//    byte[] m3uaMessage = new byte[] { 0x01, 0x00, 0x01, 0x01, 0x00, 0x00, 0x00, (byte) 0x9c, 0x02, 0x00, 0x00, 0x08, 0x00,
//            0x00, 0x00, 0x66, 0x00, 0x06, 0x00, 0x08, 0x00, 0x00, 0x00, 0x65, 0x02, 0x10, 0x00, (byte) 0x81, 0x00, 0x00, 0x00,
//            0x02, 0x00, 0x00, 0x00, 0x01, 0x03, 0x02, 0x00, 0x01, 0x09, 0x01, 0x03, 0x07, 0x0b, 0x04, 0x43, 0x01, 0x00, 0x08,
//            0x04, 0x43, 0x02, 0x00, 0x08, 0x61, 0x62, 0x5f, 0x48, 0x04, 0x00, 0x00, 0x00, 0x01, 0x6b, 0x39, 0x28, 0x37, 0x06,
//            0x07, 0x00, 0x11, (byte) 0x86, 0x05, 0x01, 0x01, 0x01, (byte) 0xa0, 0x2c, 0x60, 0x2a, (byte) 0x80, 0x02, 0x07,
//            (byte) 0x80, (byte) 0xa1, 0x09, 0x06, 0x07, 0x04, 0x00, 0x00, 0x01, 0x00, 0x13, 0x02, (byte) 0xbe, 0x19, 0x28,
//            0x17, 0x06, 0x07, 0x04, 0x00, 0x00, 0x01, 0x01, 0x01, 0x01, (byte) 0xa0, 0x0c, (byte) 0xa0, 0x0a, (byte) 0x80,
//            0x03, (byte) 0x91, 0x22, (byte) 0xf2, (byte) 0x81, 0x03, (byte) 0x91, 0x11, (byte) 0xf1, 0x6c, 0x1c, (byte) 0xa1,
//            0x1a, 0x02, 0x01, 0x01, 0x02, 0x01, 0x3b, 0x30, 0x12, 0x04, 0x01, 0x0f, 0x04, 0x05, (byte) 0xaa, 0x5a, 0x2c, 0x37,
//            0x02, (byte) 0x80, 0x06, (byte) 0x91, (byte) 0x99, 0x06, 0x36, (byte) 0x99, 0x10, 0x00, 0x00, 0x00 };
//    ByteBuf firstMessage = Unpooled.buffer(m3uaMessage.length).writeBytes(m3uaMessage);

}
