/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.celeborn.common.network.protocol;

import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;

import org.apache.celeborn.common.network.buffer.ManagedBuffer;
import org.apache.celeborn.common.protocol.PbOpenStream;
import org.apache.celeborn.common.protocol.RequestMessageType;

public class RequestMessageV2 extends RequestMessage {
  private int payloadType;
  private byte[] payload;

  public RequestMessageV2(int payloadType, byte[] payload) {
    this.payloadType = payloadType;
    this.payload = payload;
  }

  public RequestMessageV2(ManagedBuffer body, int payloadType, byte[] payload) {
    super(body);
    this.payloadType = payloadType;
    this.payload = payload;
  }

  @Override
  public int encodedLength() {
    return 4 + 4 + payload.length;
  }

  @Override
  public void encode(ByteBuf buf) {
    buf.writeInt(payloadType);
    buf.writeInt(payload.length);
    buf.writeBytes(payload);
  }

  @Override
  public Type type() {
    return Type.REQUEST_MESSAGE_V2;
  }

  public int getPayloadType() {
    return payloadType;
  }

  public byte[] getPayload() {
    return payload;
  }

  public <T> T getPayLoadMessage() {
    switch (this.payloadType) {
      case RequestMessageType.OPEN_STREAM_VALUE:
        try {
          return (T) PbOpenStream.parseFrom(payload);
        } catch (InvalidProtocolBufferException e) {
          throw new RuntimeException(e);
        }
      default:
        return null;
    }
  }

  public static RequestMessageV2 decode(ByteBuf in) {
    int type = in.readInt();
    int bufferLen = in.readInt();
    byte[] tmpBuf = new byte[bufferLen];
    in.readBytes(tmpBuf);
    return new RequestMessageV2(type, tmpBuf);
  }
}
