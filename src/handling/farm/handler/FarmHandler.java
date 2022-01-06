package handling.farm.handler;

import client.MapleCharacter;
import client.MapleCharacterSave;
import client.MapleClient;
import handling.channel.ChannelServer;
import handling.farm.FarmServer;
import handling.login.LoginServer;
import handling.world.CharacterTransfer;
import handling.world.PlayerBuffStorage;
import handling.world.World;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CField.FarmPacket;

public class FarmHandler {

    public static void leaveFarm(final MapleClient c, final MapleCharacter chr) {
        FarmServer.getPlayerStorage().deregisterPlayer(chr);
        c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, c.getSessionIPAddress());

        try {

            World.ChannelChange_Data(new CharacterTransfer(chr), chr.getId(), c.getChannel());
            c.getSession().writeAndFlush(CField.getChannelChange(c, Integer.parseInt(ChannelServer.getInstance(c.getChannel()).getIP().split(":")[1])));
        } finally {
            final String s = c.getSessionIPAddress();
            LoginServer.addIPAuth(s.substring(s.indexOf('/') + 1, s.length()));
            new MapleCharacterSave(chr).saveToDB(chr, true, false);
            c.setPlayer(null);
//	            c.setReceiving(false);
            c.setFarm(false);
        }
    }

    public static void enterFarm(MapleCharacter chr, final MapleClient c) {

        final ChannelServer ch = ChannelServer.getInstance(c.getChannel());
        chr.changeRemoval();

        PlayerBuffStorage.addBuffsToStorage(chr.getId(), chr.getAllBuffs());
        PlayerBuffStorage.addCooldownsToStorage(chr.getId(), chr.getCooldowns());
        ch.removePlayer(chr);

        World.isCharacterListConnected(c.getPlayer().getName(), c.loadCharacterNames(c.getWorld()));
        FarmServer.getPlayerStorage().registerPlayer(chr);
        new MapleCharacterSave(chr).saveToDB(chr, true, false);
        chr.getMap().removePlayer(chr);

        c.getSession().writeAndFlush(FarmPacket.onEnterFarm(chr));
        c.getSession().writeAndFlush(FarmPacket.onSetFarmUser(chr));
        c.getSession().writeAndFlush(FarmPacket.onFarmSetInGameInfo(chr));
        c.getSession().writeAndFlush(FarmPacket.onFarmRequestSetInGameInfo(chr));

        if (c.getFarmImg() != null) {
            c.getSession().writeAndFlush(FarmPacket.onFarmImgUpdate(c, c.getFarmImg().length, c.getFarmImg()));
        }

        c.getSession().writeAndFlush(FarmPacket.onFarmNotice("지금은 이미지 수정만 가능합니다."));
        c.setFarm(true);
    }

    public static void updateFarmImg(LittleEndianAccessor slea, MapleClient c) {
        int length = slea.readInt();
        byte[] img = slea.read(length);
        c.setFarmImg(img);
        c.getSession().writeAndFlush(FarmPacket.onFarmImgUpdate(c, length, img));
    }
}
