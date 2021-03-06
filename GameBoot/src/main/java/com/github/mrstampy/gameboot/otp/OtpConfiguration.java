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
package com.github.mrstampy.gameboot.otp;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;

import com.github.mrstampy.gameboot.otp.netty.OtpClearNettyHandler;
import com.github.mrstampy.gameboot.otp.netty.OtpClearNettyProcessor;
import com.github.mrstampy.gameboot.otp.websocket.OtpClearWebSocketHandler;
import com.github.mrstampy.gameboot.otp.websocket.OtpClearWebSocketProcessor;

/**
 * The Class OtpConfiguration.
 */
@Configuration
@Profile(OtpConfiguration.OTP_PROFILE)
public class OtpConfiguration {

  /** The Constant OTP_SECURE_RANDOM. */
  public static final String OTP_SECURE_RANDOM = "OTP Secure Random";

  /** The Constant OTP_PROFILE. */
  public static final String OTP_PROFILE = "otp";

  /**
   * Clear netty handler.
   *
   * @return the otp clear netty handler
   */
  @Bean
  @ConditionalOnMissingBean(OtpClearNettyHandler.class)
  @Scope("prototype")
  public OtpClearNettyHandler clearNettyHandler() {
    return new OtpClearNettyHandler();
  }

  /**
   * Clear netty processor.
   *
   * @return the otp clear netty processor
   */
  @Bean
  @ConditionalOnMissingBean(OtpClearNettyProcessor.class)
  @Scope("prototype")
  public OtpClearNettyProcessor clearNettyProcessor() {
    return new OtpClearNettyProcessor();
  }

  /**
   * Clear web socket handler.
   *
   * @return the otp clear web socket handler
   */
  @Bean
  @ConditionalOnMissingBean(OtpClearWebSocketHandler.class)
  public OtpClearWebSocketHandler clearWebSocketHandler() {
    return new OtpClearWebSocketHandler();
  }

  /**
   * Clear web socket processor.
   *
   * @return the otp clear web socket processor
   */
  @Bean
  @ConditionalOnMissingBean(OtpClearWebSocketProcessor.class)
  public OtpClearWebSocketProcessor clearWebSocketProcessor() {
    return new OtpClearWebSocketProcessor();
  }

}
