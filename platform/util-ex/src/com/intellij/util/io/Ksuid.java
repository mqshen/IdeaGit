// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.util.io;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Random;

/**
 * KSUID is for K-Sortable Unique Identifier.
 * Globally unique IDs similar to RFC 4122 UUIDs, but contain a time component, so they can be "roughly" sorted by time of creation.
 * The remainder of the KSUID is randomly generated bytes.
 *
 * See https://github.com/segmentio/ksuid#what-is-a-ksuid
 */
public final class Ksuid {
  // https://github.com/segmentio/ksuid/blob/b65a0ff7071caf0c8770b63babb7ae4a3c31034d/ksuid.go#L19
  private static final int EPOCH = 1400000000;
  private static final int TIMESTAMP_LENGTH = 4;
  private static final int PAYLOAD_LENGTH = 16;
  public static final int MAX_ENCODED_LENGTH = 27;

  public static @NotNull String generate() {
    ByteBuffer byteBuffer = generateCustom(PAYLOAD_LENGTH, DigestUtil.getRandom());
    String uid = new String(Base62.encode(byteBuffer.array()), StandardCharsets.UTF_8);
    return uid.length() > MAX_ENCODED_LENGTH ? uid.substring(0, MAX_ENCODED_LENGTH) : uid;
  }

  public static @NotNull ByteBuffer generateCustom(int payloadLength, @NotNull Random random) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(TIMESTAMP_LENGTH + payloadLength);

    long utc = ZonedDateTime.now(ZoneOffset.UTC).toInstant().toEpochMilli() / 1000;
    int timestamp = (int)(utc - EPOCH);
    byteBuffer.putInt(timestamp);

    byte[] bytes = new byte[payloadLength];
    random.nextBytes(bytes);
    byteBuffer.put(bytes);
    return byteBuffer;
  }
}