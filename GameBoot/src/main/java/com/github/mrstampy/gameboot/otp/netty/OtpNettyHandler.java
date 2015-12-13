/*
 *              ______                        ____              __ 
 *             / ____/___ _____ ___  ___     / __ )____  ____  / /_
 *            / / __/ __ `/ __ `__ \/ _ \   / __  / __ \/ __ \/ __/
 *           / /_/ / /_/ / / / / / /  __/  / /_/ / /_/ / /_/ / /_  
 *           \____/\__,_/_/ /_/ /_/\___/  /_____/\____/\____/\__/  
 *                                                 
 *                                 .-'\
 *                              .-'  `/\
 *                           .-'      `/\
 *                           \         `/\
 *                            \         `/\
 *                             \    _-   `/\       _.--.
 *                              \    _-   `/`-..--\     )
 *                               \    _-   `,','  /    ,')
 *                                `-_   -   ` -- ~   ,','
 *                                 `-              ,','
 *                                  \,--.    ____==-~
 *                                   \   \_-~\
 *                                    `_-~_.-'
 *                                     \-~
 *
 *
 * Copyright (C) 2015 Burton Alexander
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 * 
 */
package com.github.mrstampy.gameboot.otp.netty;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.github.mrstampy.gameboot.concurrent.GameBootConcurrentConfiguration;
import com.github.mrstampy.gameboot.exception.GameBootException;
import com.github.mrstampy.gameboot.exception.GameBootRuntimeException;
import com.github.mrstampy.gameboot.metrics.MetricsHelper;
import com.github.mrstampy.gameboot.netty.AbstractGameBootNettyMessageHandler;
import com.github.mrstampy.gameboot.netty.NettyConnectionRegistry;
import com.github.mrstampy.gameboot.otp.KeyRegistry;
import com.github.mrstampy.gameboot.otp.OneTimePad;
import com.github.mrstampy.gameboot.util.GameBootUtils;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.ssl.SslHandler;

/**
 * The Class OtpHandler is intended to provide a transparent means of using the
 * {@link OneTimePad} utility to encrypt outgoing and decrypt incoming messages
 * on unencrypted Netty connections. It is intended that the message is a byte
 * array at the point in which this class is inserted into the pipeline. Inbound
 * messages are later converted to strings, all outbound messages are byte
 * arrays.<br>
 * <br>
 * 
 * This class uses the {@link Channel#remoteAddress()#toString()} as a key to
 * look up the OTP key in the {@link KeyRegistry}. If none exists the message is
 * passed on as is. If an OTP key is returned it is used to encrypt/decrypt the
 * message. <br>
 * <br>
 * 
 * This class registers its channel in the {@link OtpNettyRegistry} as an
 * {@link OtpNettyConnections#getClearChannel()} with the same key as the key
 * registry: {@link Channel#remoteAddress()#toString()}. The encrypted channel
 * (assumed to be Netty, having a {@link SslHandler} in the pipeline) should
 * have the same remote host and can be added using this clear channel key. <br>
 * <br>
 * 
 * Do not instantiate directly as this is a prototype Spring managed bean. Use
 * {@link GameBootUtils#getBean(Class)} to obtain a unique instance when
 * constructing the {@link ChannelPipeline}.
 * 
 * @see NettyConnectionRegistry
 * @see KeyRegistry
 * @see OneTimePad
 * @see OtpNettyConnections
 */
@Component
@Scope("prototype")
public class OtpNettyHandler extends AbstractGameBootNettyMessageHandler {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String OTP_DECRYPT_COUNTER = "Netty OTP Decrypt Counter";

  private static final String OTP_ENCRYPT_COUNTER = "Netty OTP Encrypt Counter";

  @Autowired
  private OneTimePad oneTimePad;

  @Autowired
  private KeyRegistry keyRegistry;

  @Autowired
  private MetricsHelper helper;

  @Autowired
  @Qualifier(GameBootConcurrentConfiguration.GAME_BOOT_EXECUTOR)
  private ExecutorService svc;

  /**
   * Post construct.
   *
   * @throws Exception
   *           the exception
   */
  @PostConstruct
  public void postConstruct() throws Exception {
    super.postConstruct();
    
    if (!helper.containsCounter(OTP_DECRYPT_COUNTER)) {
      helper.counter(OTP_DECRYPT_COUNTER, getClass(), "otp", "decrypt", "counter");
    }

    if (!helper.containsCounter(OTP_ENCRYPT_COUNTER)) {
      helper.counter(OTP_ENCRYPT_COUNTER, getClass(), "otp", "encrypt", "counter");
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * io.netty.channel.ChannelInboundHandlerAdapter#channelInactive(io.netty.
   * channel.ChannelHandlerContext)
   */
  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    super.channelInactive(ctx);
    oneTimePad = null;
    keyRegistry = null;
    helper = null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * io.netty.channel.ChannelInboundHandlerAdapter#channelRead(io.netty.channel.
   * ChannelHandlerContext, java.lang.Object)
   */
  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (!(msg instanceof byte[])) {
      sendError(ctx, "Message must be a byte array");
      return;
    }

    byte[] key = keyRegistry.get(getKey());

    if (key == null) {
      super.channelRead(ctx, msg);
      return;
    }

    helper.incr(OTP_DECRYPT_COUNTER);

    byte[] converted = oneTimePad.convert(key, (byte[]) msg);

    super.channelRead(ctx, converted);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.netty.AbstractGameBootNettyMessageHandler#
   * channelReadImpl(io.netty.channel.ChannelHandlerContext, byte[])
   */
  protected void channelReadImpl(ChannelHandlerContext ctx, byte[] msg) throws Exception {
    svc.execute(() -> {
      try {
        process(ctx, new String(msg));
      } catch (GameBootException | GameBootRuntimeException e) {
        sendError(ctx, e.getMessage());
      } catch (Exception e) {
        log.error("Unexpected exception", e);
        sendError(ctx, "An unexpected error has occurred");
      }
    });
  }

  /*
   * (non-Javadoc)
   * 
   * @see io.netty.channel.ChannelDuplexHandler#write(io.netty.channel.
   * ChannelHandlerContext, java.lang.Object, io.netty.channel.ChannelPromise)
   */
  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    if (!(msg instanceof String)) {
      log.error("Internal error; object is not a string: {}", msg.getClass());
      return;
    }

    byte[] key = keyRegistry.get(getKey());

    if (key == null) {
      ctx.write(((String) msg).getBytes(), promise);
      return;
    }

    helper.incr(OTP_ENCRYPT_COUNTER);

    byte[] converted = oneTimePad.convert(key, ((String) msg).getBytes());

    ctx.write(converted, promise);
  }

}
