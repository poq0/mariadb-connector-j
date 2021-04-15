// SPDX-License-Identifier: LGPL-2.1-or-later
// Copyright (c) 2012-2014 Monty Program Ab
// Copyright (c) 2015-2021 MariaDB Corporation Ab

package org.mariadb.jdbc.plugin.authentication;

import java.io.IOException;
import java.sql.SQLException;
import org.mariadb.jdbc.Configuration;
import org.mariadb.jdbc.client.ReadableByteBuf;
import org.mariadb.jdbc.client.context.Context;
import org.mariadb.jdbc.client.socket.PacketReader;
import org.mariadb.jdbc.client.socket.PacketWriter;

public interface AuthenticationPlugin {

  /**
   * Authentication plugin type.
   *
   * @return authentication plugin type. ex: mysql_native_password
   */
  String type();

  /**
   * Authentication plugin active without setting option.
   *
   * @return true if active
   */
  boolean activeByDefault();

  /**
   * Plugin initialization.
   *
   * @param authenticationData authentication data (password/token)
   * @param seed server provided seed
   * @param conf Connection options
   */
  void initialize(String authenticationData, byte[] seed, Configuration conf);

  /**
   * Process plugin authentication.
   *
   * @param encoder out stream
   * @param decoder in stream
   * @param context connection context
   * @return response packet
   * @throws IOException if socket error
   * @throws SQLException if plugin exception
   */
  ReadableByteBuf process(PacketWriter encoder, PacketReader decoder, Context context)
      throws IOException, SQLException;
}
