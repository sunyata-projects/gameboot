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
 *                       http://mrstampy.github.io/gameboot/
 *
 * Copyright (C) 2015, 2016 Burton Alexander
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
package com.github.mrstampy.gameboot.websocket;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import com.github.mrstampy.gameboot.concurrent.GameBootConcurrentConfiguration;
import com.github.mrstampy.gameboot.controller.GameBootMessageController;
import com.github.mrstampy.gameboot.exception.GameBootThrowable;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage.Transport;
import com.github.mrstampy.gameboot.messages.GameBootMessageConverter;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.messages.context.ResponseContext;
import com.github.mrstampy.gameboot.metrics.MetricsHelper;
import com.github.mrstampy.gameboot.processor.connection.AbstractConnectionProcessor;
import com.github.mrstampy.gameboot.processor.connection.ConnectionProcessor;
import com.github.mrstampy.gameboot.systemid.SystemId;
import com.github.mrstampy.gameboot.systemid.SystemIdKey;
import com.github.mrstampy.gameboot.util.registry.AbstractRegistryKey;
import com.github.mrstampy.gameboot.util.registry.RegistryCleaner;

/**
 * Superclass for Netty {@link ConnectionProcessor}s.
 */
public abstract class AbstractWebSocketProcessor extends AbstractConnectionProcessor<WebSocketSession> {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String MESSAGE_COUNTER = "GameBoot Web Socket Message Counter";

  private static final String FAILED_MESSAGE_COUNTER = "GameBoot Web Socket Failed Message Counter";

  @Autowired
  private MetricsHelper helper;

  @Autowired
  private WebSocketSessionRegistry registry;

  @Autowired
  private SystemId generator;

  @Autowired
  private RegistryCleaner cleaner;

  @Autowired
  private GameBootMessageConverter converter;

  /** The system ids. */
  protected Map<String, SystemIdKey> systemIds = new ConcurrentHashMap<>();

  /**
   * Post construct, invoke from {@link PostConstruct}-annotated subclass
   * methods.
   *
   * @throws Exception
   *           the exception
   */
  protected void postConstruct() throws Exception {
    if (!helper.containsCounter(MESSAGE_COUNTER)) {
      helper.counter(MESSAGE_COUNTER, getClass(), "inbound", "messages");
    }

    if (!helper.containsCounter(FAILED_MESSAGE_COUNTER)) {
      helper.counter(FAILED_MESSAGE_COUNTER, getClass(), "failed", "messages");
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.netty.ConnectionProcessor#onConnection(io.
   * netty.channel.WebSocketSession)
   */
  @Override
  public void onConnection(WebSocketSession session) throws Exception {
    setSystemId(session, generator.next());

    setMDC(session);

    addToRegistry(session);
  }

  /**
   * Adds the to registry.
   *
   * @param session
   *          the session
   */
  protected void addToRegistry(WebSocketSession session) {
    SystemIdKey systemId = getSystemId(session);

    if (registry.contains(systemId)) return;

    registry.put(systemId, session);
    registry.putInAll(systemId, session);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.netty.ConnectionProcessor#onDisconnection(io.
   * netty.channel.WebSocketSession)
   */
  @Override
  public void onDisconnection(WebSocketSession session) throws Exception {
    String id = session.getId();

    SystemIdKey systemId = systemIds.remove(id);
    cleaner.cleanup(systemId);

    Set<Entry<AbstractRegistryKey<?>, WebSocketSession>> set = registry.getKeysForValue(session);

    set.forEach(e -> registry.remove(e.getKey()));
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.netty.ConnectionProcessor#onMessage(io.netty.
   * channel.WebSocketSession, java.lang.Object)
   */
  @Override
  public void onMessage(WebSocketSession session, Object msg) throws Exception {
    setMDC(session);

    if (!(msg instanceof WebSocketMessage<?>)) throw new IllegalArgumentException("Must be a WebSocketMessage");

    Object payload = extractPayload(session, msg);

    if (payload instanceof String) {
      onMessageImpl(session, (String) payload);
    } else if (payload instanceof byte[]) {
      onMessageImpl(session, (byte[]) payload);
    }
  }

  /**
   * Extract payload.
   *
   * @param session
   *          the session
   * @param msg
   *          the msg
   * @return the object
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  protected Object extractPayload(WebSocketSession session, Object msg) throws IOException {
    if (msg instanceof BinaryMessage) {
      return ((BinaryMessage) msg).getPayload().array();
    } else if (msg instanceof TextMessage) {
      return ((TextMessage) msg).getPayload();
    }

    log.error("Only strings or byte arrays: {} from {}. Disconnecting", msg.getClass(), session);
    session.close();

    return null;
  }

  /**
   * On message impl, implement processing the message using one of the
   * executors in {@link GameBootConcurrentConfiguration} invoking
   * {@link #process(WebSocketSession, byte[])}.
   *
   * @param session
   *          the session
   * @param msg
   *          the msg
   * @throws Exception
   *           the exception
   */
  protected abstract void onMessageImpl(WebSocketSession session, byte[] msg) throws Exception;

  /**
   * On message impl, implement processing the message using one of the
   * executors in {@link GameBootConcurrentConfiguration} invoking
   * {@link #process(WebSocketSession, String)}.
   *
   * @param session
   *          the session
   * @param msg
   *          the msg
   * @throws Exception
   *           the exception
   */
  protected abstract void onMessageImpl(WebSocketSession session, String msg) throws Exception;

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.processor.connection.ConnectionProcessor#
   * sendError(java.lang.Object,
   * com.github.mrstampy.gameboot.exception.GameBootThrowable)
   */
  public void sendError(WebSocketSession session, GameBootThrowable e) {
    Response r = fail(session, null, e);

    try {
      sendMessage(session, converter.toJsonArray(r), r);
    } catch (Exception e1) {
      log.error("Unexpected exception", e1);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.processor.connection.ConnectionProcessor#
   * sendError(int, java.lang.Object, java.lang.String)
   */
  public void sendError(ResponseContext rc, WebSocketSession session, String message) {
    Response r = fail(rc, null, message);

    try {
      sendMessage(session, converter.toJsonArray(r), r);
    } catch (Exception e) {
      log.error("Unexpected exception", e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.netty.ConnectionProcessor#process(io.netty.
   * channel.WebSocketSession, java.lang.String)
   */
  @Override
  public <AGBM extends AbstractGameBootMessage> Response process(WebSocketSession session, String msg)
      throws Exception {
    setMDC(session);
    helper.incr(MESSAGE_COUNTER);

    Response response = super.process(session, msg);

    if (response != null && ResponseCode.FAILURE == response.getResponseCode()) helper.incr(FAILED_MESSAGE_COUNTER);

    return response;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.netty.ConnectionProcessor#process(io.netty.
   * channel.WebSocketSession, byte[])
   */
  @Override
  public <AGBM extends AbstractGameBootMessage> Response process(WebSocketSession session, byte[] msg)
      throws Exception {
    setMDC(session);
    helper.incr(MESSAGE_COUNTER);

    Response response = super.process(session, msg);

    if (response != null && ResponseCode.FAILURE == response.getResponseCode()) helper.incr(FAILED_MESSAGE_COUNTER);

    return response;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.netty.ConnectionProcessor#process(io.netty.
   * channel.WebSocketSession,
   * com.github.mrstampy.gameboot.controller.GameBootMessageController, AGBM)
   */
  @Override
  public <AGBM extends AbstractGameBootMessage> Response process(WebSocketSession session,
      GameBootMessageController controller, AGBM agbm) throws Exception {
    agbm.setSystemId(getSystemId(session));
    agbm.setTransport(Transport.WEB_SOCKET);
    agbm.setLocal((InetSocketAddress) session.getLocalAddress());
    agbm.setRemote((InetSocketAddress) session.getRemoteAddress());

    Response r = controller.process(agbm);
    processMappingKeys(r, session);
    r.setSystemId(agbm.getSystemId());

    return r;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.netty.ConnectionProcessor#sendMessage(io.netty
   * .channel.WebSocketSession, java.lang.Object,
   * com.github.mrstampy.gameboot.messages.Response)
   */
  @Override
  public void sendMessage(WebSocketSession session, Object msg, Response response) throws Exception {
    sendMessage(session, msg);
  }

  /**
   * Send message, must be a byte array or a string.
   *
   * @param session
   *          the session
   * @param msg
   *          the msg
   * @throws Exception
   *           the exception
   */
  public void sendMessage(WebSocketSession session, Object msg) throws Exception {
    WebSocketMessage<?> toGo = createMessage(session, msg);
    session.sendMessage(toGo);
  }

  /**
   * Creates the message.
   *
   * @param session
   *          the session
   * @param msg
   *          the msg
   * @return the web socket message
   * @throws Exception
   *           the exception
   */
  protected WebSocketMessage<?> createMessage(WebSocketSession session, Object msg) throws Exception {
    if (msg instanceof byte[]) return new BinaryMessage((byte[]) msg);

    if (msg instanceof String) return new TextMessage(((String) msg).getBytes());

    throw new IllegalArgumentException("Can only send strings or byte arrays: " + msg.getClass());
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.processor.connection.ConnectionProcessor#
   * getSystemId(java.lang.Object)
   */
  @Override
  public SystemIdKey getSystemId(WebSocketSession session) {
    return systemIds.get(session.getId());
  }

  /**
   * Sets the system id.
   *
   * @param session
   *          the session
   * @param systemId
   *          the new system id
   */
  public void setSystemId(WebSocketSession session, SystemIdKey systemId) {
    systemIds.put(session.getId(), systemId);
  }

  private void processMappingKeys(Response r, WebSocketSession session) {
    AbstractRegistryKey<?>[] keys = r.getMappingKeys();
    if (keys == null || keys.length == 0) return;

    for (int i = 0; i < keys.length; i++) {
      registry.put(keys[i], session);
    }
  }
}
