package com.gllue.myproxy.transport.core.netty;

import static com.gllue.myproxy.constant.TimeConstants.NANOS_PER_SECOND;

import com.gllue.myproxy.transport.exception.ServerErrorCode;
import com.gllue.myproxy.transport.protocol.packet.MySQLPacket;
import com.gllue.myproxy.transport.protocol.packet.command.CommandPacket;
import com.gllue.myproxy.transport.protocol.packet.generic.ErrPacket;
import com.gllue.myproxy.transport.protocol.payload.MySQLPayload;
import com.gllue.myproxy.transport.protocol.payload.WrappedPayloadPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.prometheus.client.Summary;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MySQLPayloadCodecHandler extends ByteToMessageCodec<MySQLPacket> {
  private static final Summary PROXY_LATENCY_SUMMARY =
      Summary.build()
          .name("proxy_latency_summary")
          .help("Proxy latency summary in seconds")
          .unit("second")
          .register();

  public static final int PAYLOAD_BYTES = 3;

  public static final int SEQUENCE_BYTES = 1;

  public static final int MAX_PAYLOAD_SIZE = (1 << 24) - 1;

  private int sequenceId = 0;

  private CompositeByteBuf cumulation = null;

  private long encodeFirstPayloadTime;

  public boolean isValidHeader(final int readableBytes) {
    return readableBytes >= PAYLOAD_BYTES + SEQUENCE_BYTES;
  }

  @Override
  public void decode(
      final ChannelHandlerContext context, final ByteBuf in, final List<Object> out) {
    final int readableBytes = in.readableBytes();
    if (!isValidHeader(readableBytes)) {
      return;
    }

    in.markReaderIndex();
    final int payloadLength = in.readUnsignedMediumLE();
    final int remainPayloadLength = payloadLength + SEQUENCE_BYTES;
    if (in.readableBytes() < remainPayloadLength) {
      in.resetReaderIndex();
      return;
    }

    var readSeqId = in.readUnsignedByte();
    var payloadByteBuf = in.readRetainedSlice(payloadLength);

    if (readSeqId == 0) {
      sequenceId = 0;
      encodeFirstPayloadTime = System.nanoTime();
    } else {
      validateSequenceId(readSeqId, context);
    }

    if (cumulation == null) {
      if (payloadLength < MAX_PAYLOAD_SIZE) {
        out.add(new MySQLPayload(payloadByteBuf));
      } else {
        cumulation = context.alloc().compositeBuffer();
        cumulation.addComponent(true, payloadByteBuf);
      }
    } else {
      if (payloadByteBuf.isReadable()) {
        cumulation.addComponent(true, payloadByteBuf);
      }

      if (payloadLength < MAX_PAYLOAD_SIZE) {
        out.add(new MySQLPayload(cumulation));
        cumulation = null;
      }
    }

    nextSequenceId();
  }

  private void validateSequenceId(final int readSequenceId, final ChannelHandlerContext context) {
    if (readSequenceId != sequenceId) {
      log.error("Expect sequence id [{}], got [{}]", sequenceId, readSequenceId);
      context.channel().close();
    }
  }

  @Override
  public void encode(
      final ChannelHandlerContext context, final MySQLPacket message, final ByteBuf out) {
    // Fast path for direct transferred query result.
    if (message instanceof WrappedPayloadPacket) {
      writePayload(((WrappedPayloadPacket) message).getPayload(), out);
      return;
    }

    var payload = new MySQLPayload(context.alloc().buffer());
    var payloadBuf = payload.getByteBuf();

    if (message instanceof CommandPacket) {
      sequenceId = 0;
      message.write(payload);
      writePayload(payload, out);
    } else {
      if (encodeFirstPayloadTime > 0) {
        PROXY_LATENCY_SUMMARY.observe((System.nanoTime() - encodeFirstPayloadTime) / NANOS_PER_SECOND);
        encodeFirstPayloadTime = 0;
      }

      payloadBuf.markWriterIndex();
      try {
        message.write(payload);
      } catch (final Exception ex) {
        payloadBuf.resetWriterIndex();
        new ErrPacket(ServerErrorCode.ER_SERVER_ERROR, ex.getMessage()).write(payload);
      } finally {
        writePayload(payload, out);
      }
    }
  }

  private void writePayload(final MySQLPayload payload, final ByteBuf out) {
    var buf = payload.getByteBuf();
    try (payload) {
      int payloadLen;
      do {
        payloadLen = Math.min(MAX_PAYLOAD_SIZE, buf.readableBytes());
        out.writeMediumLE(payloadLen);
        out.writeByte(nextSequenceId());
        out.writeBytes(buf, payloadLen);
      } while (buf.isReadable() || payloadLen == MAX_PAYLOAD_SIZE);
    }
  }

  private int nextSequenceId() {
    sequenceId %= 256;
    return sequenceId++;
  }
}
