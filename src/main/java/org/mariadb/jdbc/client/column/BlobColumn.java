// SPDX-License-Identifier: LGPL-2.1-or-later
// Copyright (c) 2012-2014 Monty Program Ab
// Copyright (c) 2015-2021 MariaDB Corporation Ab

package org.mariadb.jdbc.client.column;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Blob;
import java.sql.SQLDataException;
import java.sql.Types;
import java.util.Calendar;
import java.util.Locale;
import org.mariadb.jdbc.Configuration;
import org.mariadb.jdbc.client.ColumnDecoder;
import org.mariadb.jdbc.client.DataType;
import org.mariadb.jdbc.client.ReadableByteBuf;
import org.mariadb.jdbc.message.server.ColumnDefinitionPacket;
import org.mariadb.jdbc.util.CharsetEncodingLength;

/** Column metadata definition */
public class BlobColumn extends ColumnDefinitionPacket implements ColumnDecoder {

  public BlobColumn(
      ReadableByteBuf buf,
      int charset,
      long length,
      DataType dataType,
      byte decimals,
      int flags,
      int[] stringPos,
      String extTypeName,
      String extTypeFormat) {
    super(buf, charset, length, dataType, decimals, flags, stringPos, extTypeName, extTypeFormat);
  }

  public String defaultClassname(Configuration conf) {
    return isBinary() ? Blob.class.getName() : String.class.getName();
  }

  public int getColumnType(Configuration conf) {
    if (columnLength <= 0 || getDisplaySize() > 16777215) {
      return isBinary() ? Types.LONGVARBINARY : Types.LONGVARCHAR;
    } else {
      if (dataType == DataType.TINYBLOB || dataType == DataType.BLOB) {
        return isBinary() ? Types.VARBINARY : Types.VARCHAR;
      }
      return isBinary() ? Types.LONGVARBINARY : Types.LONGVARCHAR;
    }
  }

  public String getColumnTypeName(Configuration conf) {
    /*
     map to different blob types based on datatype length
     see https://mariadb.com/kb/en/library/data-types/
    */
    if (extTypeFormat != null) {
      return extTypeFormat.toUpperCase(Locale.ROOT);
    }
    if (isBinary()) {
      if (columnLength < 0) {
        return "LONGBLOB";
      } else if (columnLength <= 255) {
        return "TINYBLOB";
      } else if (columnLength <= 65535) {
        return "BLOB";
      } else if (columnLength <= 16777215) {
        return "MEDIUMBLOB";
      } else {
        return "LONGBLOB";
      }
    } else {
      if (columnLength < 0) {
        return "LONGTEXT";
      } else if (getDisplaySize() <= 65532) {
        return "VARCHAR";
      } else if (getDisplaySize() <= 65535) {
        return "TEXT";
      } else if (getDisplaySize() <= 16777215) {
        return "MEDIUMTEXT";
      } else {
        return "LONGTEXT";
      }
    }
  }

  public int getPrecision() {
    if (!isBinary()) {
      Integer maxWidth2 = CharsetEncodingLength.maxCharlen.get(charset);
      if (maxWidth2 != null) return (int) (columnLength / maxWidth2);
    }
    return (int) columnLength;
  }

  @Override
  public Object getDefaultText(final Configuration conf, ReadableByteBuf buf, int length)
      throws SQLDataException {
    if (isBinary()) {
      return buf.readBlob(length);
    }
    return buf.readString(length);
  }

  @Override
  public Object getDefaultBinary(final Configuration conf, ReadableByteBuf buf, int length)
      throws SQLDataException {
    return getDefaultText(conf, buf, length);
  }

  @Override
  public boolean decodeBooleanText(ReadableByteBuf buf, int length) throws SQLDataException {
    if (isBinary()) {
      buf.skip(length);
      throw new SQLDataException(
          String.format("Data type %s cannot be decoded as Boolean", dataType));
    }
    String s = buf.readAscii(length);
    return !"0".equals(s);
  }

  @Override
  public boolean decodeBooleanBinary(ReadableByteBuf buf, int length) throws SQLDataException {
    return decodeBooleanText(buf, length);
  }

  @Override
  public byte decodeByteText(ReadableByteBuf buf, int length) throws SQLDataException {
    long result;
    if (!isBinary()) {
      // TEXT column
      String str2 = buf.readString(length);
      try {
        result = new BigDecimal(str2).setScale(0, RoundingMode.DOWN).longValue();
      } catch (NumberFormatException nfe) {
        throw new SQLDataException(
            String.format("value '%s' (%s) cannot be decoded as Byte", str2, dataType));
      }
      if ((byte) result != result) {
        throw new SQLDataException("byte overflow");
      }

      return (byte) result;
    }
    if (length > 0) {
      byte b = buf.readByte();
      buf.skip(length - 1);
      return b;
    }
    throw new SQLDataException("empty String value cannot be decoded as Byte");
  }

  @Override
  public byte decodeByteBinary(ReadableByteBuf buf, int length) throws SQLDataException {
    return decodeByteText(buf, length);
  }

  @Override
  public String decodeStringText(ReadableByteBuf buf, int length, Calendar cal)
      throws SQLDataException {
    return buf.readString(length);
  }

  @Override
  public String decodeStringBinary(ReadableByteBuf buf, int length, Calendar cal)
      throws SQLDataException {
    return buf.readString(length);
  }

  @Override
  public short decodeShortText(ReadableByteBuf buf, int length) throws SQLDataException {
    if (isBinary()) {
      buf.skip(length);
      throw new SQLDataException(
          String.format("Data type %s cannot be decoded as Short", dataType));
    }
    String str = buf.readString(length);
    try {
      return new BigDecimal(str).setScale(0, RoundingMode.DOWN).shortValueExact();
    } catch (NumberFormatException | ArithmeticException nfe) {
      throw new SQLDataException(String.format("value '%s' cannot be decoded as Short", str));
    }
  }

  @Override
  public short decodeShortBinary(ReadableByteBuf buf, int length) throws SQLDataException {
    return decodeShortText(buf, length);
  }

  @Override
  public int decodeIntText(ReadableByteBuf buf, int length) throws SQLDataException {
    if (isBinary()) {
      buf.skip(length);
      throw new SQLDataException(
          String.format("Data type %s cannot be decoded as Integer", dataType));
    }
    String str = buf.readString(length);
    try {
      return new BigDecimal(str).setScale(0, RoundingMode.DOWN).intValueExact();
    } catch (NumberFormatException | ArithmeticException nfe) {
      throw new SQLDataException(String.format("value '%s' cannot be decoded as Integer", str));
    }
  }

  @Override
  public int decodeIntBinary(ReadableByteBuf buf, int length) throws SQLDataException {
    return decodeIntText(buf, length);
  }

  @Override
  public long decodeLongText(ReadableByteBuf buf, int length) throws SQLDataException {
    if (isBinary()) {
      buf.skip(length);
      throw new SQLDataException(String.format("Data type %s cannot be decoded as Long", dataType));
    }
    String str = buf.readString(length);
    try {
      return new BigInteger(str).longValueExact();
    } catch (NumberFormatException nfe) {
      throw new SQLDataException(String.format("value '%s' cannot be decoded as Long", str));
    }
  }

  @Override
  public long decodeLongBinary(ReadableByteBuf buf, int length) throws SQLDataException {
    if (isBinary()) {
      buf.skip(length);
      throw new SQLDataException(String.format("Data type %s cannot be decoded as Long", dataType));
    }
    String str = buf.readString(length);
    try {
      return new BigDecimal(str).setScale(0, RoundingMode.DOWN).longValueExact();
    } catch (NumberFormatException | ArithmeticException nfe) {
      throw new SQLDataException(String.format("value '%s' cannot be decoded as Long", str));
    }
  }

  @Override
  public float decodeFloatText(ReadableByteBuf buf, int length) throws SQLDataException {
    if (isBinary()) {
      buf.skip(length);
      throw new SQLDataException(
          String.format("Data type %s cannot be decoded as Float", dataType));
    }
    String val = buf.readString(length);
    try {
      return Float.parseFloat(val);
    } catch (NumberFormatException nfe) {
      throw new SQLDataException(String.format("value '%s' cannot be decoded as Float", val));
    }
  }

  @Override
  public float decodeFloatBinary(ReadableByteBuf buf, int length) throws SQLDataException {
    if (isBinary()) {
      buf.skip(length);
      throw new SQLDataException(
          String.format("Data type %s cannot be decoded as Float", dataType));
    }

    String str2 = buf.readString(length);
    try {
      return Float.parseFloat(str2);
    } catch (NumberFormatException nfe) {
      throw new SQLDataException(String.format("value '%s' cannot be decoded as Float", str2));
    }
  }

  @Override
  public double decodeDoubleText(ReadableByteBuf buf, int length) throws SQLDataException {
    if (isBinary()) {
      buf.skip(length);
      throw new SQLDataException(
          String.format("Data type %s cannot be decoded as Double", dataType));
    }
    String str2 = buf.readString(length);
    try {
      return Double.parseDouble(str2);
    } catch (NumberFormatException nfe) {
      throw new SQLDataException(String.format("value '%s' cannot be decoded as Double", str2));
    }
  }

  @Override
  public double decodeDoubleBinary(ReadableByteBuf buf, int length) throws SQLDataException {
    return decodeDoubleText(buf, length);
  }
}
