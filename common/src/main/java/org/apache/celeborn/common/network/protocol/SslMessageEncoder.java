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

import java.io.InputStream;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.stream.ChunkedStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encoder used by the server side to encode secure (SSL) server-to-client responses. This encoder
 * is stateless so it is safe to be shared by multiple threads.
 * Based on common/network-common/org.apache.spark.network.protocol.SslMessageEncoder
 */
@ChannelHandler.Sharable
public final class SslMessageEncoder extends MessageToMessageEncoder<Message> {

  private static final Logger logger = LoggerFactory.getLogger(SslMessageEncoder.class);
  public static final SslMessageEncoder INSTANCE = new SslMessageEncoder();

  private SslMessageEncoder() {}

  /**
   * Encodes a Message by invoking its encode() method. For non-data messages, we will add one
   * ByteBuf to 'out' containing the total frame length, the message type, and the message itself.
   * In the case of a ChunkFetchSuccess, we will also add the ManagedBuffer corresponding to the
   * data to 'out'.
   */
  @Override
  public void encode(ChannelHandlerContext ctx, Message in, List<Object> out) throws Exception {
    Object body = null;
    int bodyLength = 0;

    // If the message has a body, take it out...
    // For SSL, zero-copy transfer will not work, so we will check if
    // the body is an InputStream, and if so, use an EncryptedMessageWithHeader
    // to wrap the header+body appropriately (for thread safety).
    if (in.body() != null) {
      try {
        bodyLength = (int) in.body().size();
        body = in.body().convertToNettyForSsl();
      } catch (Exception e) {
        in.body().release();
        if (in instanceof ResponseMessage) {
          ResponseMessage resp = (ResponseMessage) in;
          // Re-encode this message as a failure response.
          String error = e.getMessage() != null ? e.getMessage() : "null";
          logger.error(
              String.format("Error processing %s for client %s", in, ctx.channel().remoteAddress()),
              e);
          encode(ctx, resp.createFailureResponse(error), out);
        } else {
          throw e;
        }
        return;
      }
    }

    Message.Type msgType = in.type();
    // message size, message type size, body size, message encoded length
    int headerLength = 4 + msgType.encodedLength() + 4 + in.encodedLength();
    ByteBuf header = ctx.alloc().heapBuffer(headerLength);
    header.writeInt(in.encodedLength());
    msgType.encode(header);
    header.writeInt(bodyLength);
    in.encode(header);
    assert header.writableBytes() == 0;

    if (body != null && bodyLength > 0) {
      if (body instanceof ByteBuf) {
        out.add(Unpooled.wrappedBuffer(header, (ByteBuf) body));
      } else if (body instanceof InputStream || body instanceof ChunkedStream) {
        // For now, assume the InputStream is doing proper chunking.
        out.add(new EncryptedMessageWithHeader(in.body(), header, body, bodyLength));
      } else {
        throw new IllegalArgumentException(
            "Body must be a ByteBuf, ChunkedStream or an InputStream");
      }
    } else {
      out.add(header);
    }
  }
}
