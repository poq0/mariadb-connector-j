// SPDX-License-Identifier: LGPL-2.1-or-later
// Copyright (c) 2012-2014 Monty Program Ab
// Copyright (c) 2015-2021 MariaDB Corporation Ab

package org.mariadb.jdbc.codec.list;

import java.io.IOException;
import java.sql.SQLDataException;
import java.util.Calendar;
import org.mariadb.jdbc.client.ReadableByteBuf;
import org.mariadb.jdbc.client.context.Context;
import org.mariadb.jdbc.client.socket.PacketWriter;
import org.mariadb.jdbc.codec.Codec;
import org.mariadb.jdbc.codec.DataType;
import org.mariadb.jdbc.message.server.ColumnDefinitionPacket;
import org.mariadb.jdbc.type.*;

public class PointCodec implements Codec<Point> {

  public static final PointCodec INSTANCE = new PointCodec();

  public String className() {
    return Point.class.getName();
  }

  public boolean canDecode(ColumnDefinitionPacket column, Class<?> type) {
    return column.getType() == DataType.GEOMETRY && type.isAssignableFrom(Point.class);
  }

  public boolean canEncode(Object value) {
    return value instanceof Point;
  }

  @Override
  public Point decodeText(
      ReadableByteBuf buf, int length, ColumnDefinitionPacket column, Calendar cal)
      throws SQLDataException {
    return decodeBinary(buf, length, column, cal);
  }

  @Override
  public Point decodeBinary(
      ReadableByteBuf buf, int length, ColumnDefinitionPacket column, Calendar cal)
      throws SQLDataException {
    if (column.getType() == DataType.GEOMETRY) {
      buf.skip(4); // SRID
      Geometry geo = Geometry.getGeometry(buf, length - 4, column);
      if (geo instanceof Point) return (Point) geo;
      throw new SQLDataException(
          String.format("Geometric type %s cannot be decoded as Point", geo.getClass().getName()));
    }
    buf.skip(length);
    throw new SQLDataException(
        String.format("Data type %s cannot be decoded as Point", column.getType()));
  }

  @Override
  public void encodeText(
      PacketWriter encoder, Context context, Object value, Calendar cal, Long maxLength)
      throws IOException {
    encoder.writeBytes(("ST_PointFromText('" + value.toString() + "')").getBytes());
  }

  @Override
  public void encodeBinary(
      PacketWriter encoder, Context context, Object value, Calendar cal, Long maxLength)
      throws IOException {
    Point pt = (Point) value;
    encoder.writeLength(25);
    encoder.writeInt(0); // SRID
    encoder.writeByte(0x01); // LITTLE ENDIAN
    encoder.writeInt(1); // wkbPoint
    encoder.writeDouble(pt.getX());
    encoder.writeDouble(pt.getY());
  }

  public int getBinaryEncodeType() {
    return DataType.BLOB.get();
  }
}
