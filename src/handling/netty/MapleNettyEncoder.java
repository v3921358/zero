/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.netty;

import client.MapleClient;
import constants.ServerConstants;
import handling.SendPacketOpcode;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import tools.HexTool;
import tools.MapleAESOFB;

import java.util.concurrent.locks.Lock;

/**
 *
 * @author csproj
 */
public class MapleNettyEncoder extends MessageToByteEncoder<byte[]> {

    @Override
    protected void encode(ChannelHandlerContext ctx, byte[] msg, ByteBuf buffer) throws Exception {

        final MapleClient client = ctx.channel().attr(MapleClient.CLIENTKEY).get();

        if (client != null) {
            final Lock mutex = client.getLock();

            mutex.lock();
            try {
                if (ServerConstants.DEBUG_SEND) {
                    int opcode = ((int) (msg[1] & 0xFF) * 0x100) + (int) (msg[0] & 0xFF);
                    if (opcode < SendPacketOpcode.SPAWN_MONSTER.getValue() || opcode > SendPacketOpcode.BLOCK_ATTACK.getValue()) {
                        System.out.println("[SEND] " + opcode + " : " + SendPacketOpcode.getOpcodeName(opcode) + " :\n" + HexTool.toString(msg));
                        System.out.println(HexTool.toStringFromAscii(msg) + "\n");
                    }
                }

                final MapleAESOFB send_crypto = client.getSendCrypto();

                buffer.writeBytes(send_crypto.getPacketHeader(msg.length));
                buffer.writeBytes(send_crypto.crypt(msg));
                ctx.flush();
            } finally {
                mutex.unlock();
            }
        } else { // no client object created yet, send unencrypted (hello)
            buffer.writeBytes(msg);
            //    ctx.F(buffer);
        }

    }
}
