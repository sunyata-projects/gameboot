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
package com.github.mrstampy.gameboot.exception;

/**
 * Runtime exception generated by GameBoot.
 */
public class GameBootRuntimeException extends RuntimeException {

  private static final long serialVersionUID = -439786992079674082L;

  /**
   * Instantiates a new game boot runtime exception.
   */
  public GameBootRuntimeException() {
    super();
  }

  /**
   * Instantiates a new game boot runtime exception.
   *
   * @param message
   *          the message
   */
  public GameBootRuntimeException(String message) {
    super(message);
  }

  /**
   * Instantiates a new game boot runtime exception.
   *
   * @param cause
   *          the cause
   */
  public GameBootRuntimeException(Throwable cause) {
    super(cause);
  }

  /**
   * Instantiates a new game boot runtime exception.
   *
   * @param message
   *          the message
   * @param cause
   *          the cause
   */
  public GameBootRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Instantiates a new game boot runtime exception.
   *
   * @param message
   *          the message
   * @param cause
   *          the cause
   * @param enableSuppression
   *          the enable suppression
   * @param writableStackTrace
   *          the writable stack trace
   */
  public GameBootRuntimeException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
