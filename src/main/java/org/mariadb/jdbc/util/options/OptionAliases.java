// SPDX-License-Identifier: LGPL-2.1-or-later
// Copyright (c) 2012-2014 Monty Program Ab
// Copyright (c) 2015-2021 MariaDB Corporation Ab

package org.mariadb.jdbc.util.options;

import java.util.HashMap;
import java.util.Map;

public final class OptionAliases {

  public static final Map<String, String> OPTIONS_ALIASES;

  static {
    OPTIONS_ALIASES = new HashMap<>();
    OPTIONS_ALIASES.put("enabledSSLCipherSuites", "enabledSslCipherSuites");
    OPTIONS_ALIASES.put("serverRSAPublicKeyFile", "serverRsaPublicKeyFile");
    OPTIONS_ALIASES.put("useSsl", "sslMode");
    OPTIONS_ALIASES.put("useSSL", "sslMode");
  }
}
