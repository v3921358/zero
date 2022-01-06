/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.netty;

import client.MapleCharacterSave;
import client.MapleClient;
import client.SkillFactory;
import client.inventory.MaplePet;
import client.inventory.PetDataFactory;
import connector.ConnectorPanel;
import constants.GameConstants;
import constants.ServerConstants;
import constants.ServerType;
import database.DatabaseConnection;
import handling.RecvPacketOpcode;
import handling.SendPacketOpcode;
import handling.auction.handler.AuctionHandler;
import handling.cashshop.handler.CashShopOperation;
import handling.channel.handler.*;
import handling.farm.handler.FarmHandler;
import handling.login.handler.CharLoginHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import scripting.NPCScriptManager;
import server.enchant.EquipmentEnchant;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleNPC;
import server.maps.MapleMapObject;
import server.maps.MapleMist;
import server.quest.party.MapleNettPyramid;
import tools.*;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CSPacket;
import tools.packet.CWvsContext;
import tools.packet.LoginPacket;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static constants.ServerType.*;
import java.awt.Point;

/**
 *
 * @author csproj
 */
public class MapleNettyHandler extends SimpleChannelInboundHandler<LittleEndianAccessor> {

    private final ServerType serverType;
    private final int channel;
    private final List<String> BlockedIP = new ArrayList<String>();
    private final Map<String, Pair<Long, Byte>> tracker = new ConcurrentHashMap<String, Pair<Long, Byte>>();

    public MapleNettyHandler(ServerType serverType, int channel) {
        this.serverType = serverType;
        this.channel = channel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        // Start of IP checking
        final String address = ctx.channel().remoteAddress().toString().split(":")[0];

        if (BlockedIP.contains(address)) {
            ctx.close();
            return;
        }
        final Pair<Long, Byte> track = tracker.get(address);

        byte count;
        if (track == null) {
            count = 1;
        } else {
            count = track.right;

            final long difference = System.currentTimeMillis() - track.left;
            if (difference < 2000) { // Less than 2 sec
                count++;
            } else if (difference > 20000) { // Over 20 sec
                count = 1;
            }
            if (count >= 10) {
                BlockedIP.add(address);
                tracker.remove(address); // Cleanup
                ctx.close();
                return;
            }
        }
        tracker.put(address, new Pair<Long, Byte>(System.currentTimeMillis(), count));

        boolean check = true;

        if (address.contains("219.250.30.201") || address.contains("1.248.193.246")
                || address.contains("36.39.235.217") || address.contains("103.226.79.71")) {
            check = false;
        }

        if (ServerConstants.ConnectorSetting && check) {
            DefaultTableModel model = (DefaultTableModel) ConnectorPanel.jTable1.getModel();

            boolean enableLogin = false;
            try {
                if (model.getRowCount() >= 1) {
                    for (int i = model.getRowCount() - 1; i >= 0; i--) {
                        if (model.getValueAt(i, 2).toString().startsWith(address)) {
                            enableLogin = true;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!enableLogin) {
                BlockedIP.add(address);
                ctx.close();
                return;
            }
        }

        // IV used to decrypt packets from client.
        switch (serverType) {
            case LOGIN:
                System.out.println("[알림] " + address + " 에서 로그인 서버에 접속했습니다.");
                break;
            case CHANNEL:
                System.out.println("[알림] " + address + " 에서 채널 서버에 접속했습니다.");
                break;
            case CASHSHOP:
                System.out.println("[알림] " + address + " 에서 캐시샵 서버에 접속했습니다.");
                break;
            case BUDDYCHAT:
                System.out.println("[알림] " + address + " 에서 친구 서버에 접속했습니다.");
                break;
            case AUCTION:
                System.out.println("[알림] " + address + " 에서 경매장 서버에 접속했습니다.");
                break;
            default:
        }
        final byte serverRecv[]
                = {
                    //  (byte) 0x22, (byte) 0x3F, (byte) 0x37, (byte) Randomizer.nextInt(255)
                    (byte) 0xFE, (byte) 0x0B, (byte) 0xC4, (byte) 0x5C
                };
        final byte serverSend[]
                = {
                    //   (byte) 0xC9, (byte) 0x3A, (byte) 0x27, (byte) Randomizer.nextInt(255)
                    (byte) 0xD9, (byte) 0x3E, (byte) 0x3E, (byte) 0x50
                };
        final byte ivRecv[] = serverRecv;
        final byte ivSend[] = serverSend;
        final MapleClient client = new MapleClient(
                ctx.channel(),
                new MapleAESOFB(ivSend, (short) (0xFFFF - ServerConstants.MAPLE_VERSION), serverType == CHANNEL || serverType == CASHSHOP || serverType == AUCTION, true), // Sent Cypher
                new MapleAESOFB(ivRecv, ServerConstants.MAPLE_VERSION, serverType == CHANNEL || serverType == CASHSHOP || serverType == AUCTION));
        client.setChannel(channel);

        ctx.writeAndFlush(LoginPacket.initializeConnection(ServerConstants.MAPLE_VERSION, ivSend, ivRecv, !serverType.equals(ServerType.LOGIN)));

        //ctx.writeAndFlush(HexTool.getByteArrayFromHexString("30 00 23 01 05 00 36 35 38 33 30 D9 3E 3E 50 FE 0B C4 5C 01 00 23 01 26 01 00 00 D9 3E 3E 50 FE 0B C4 5C 01 01 00 00 00 01 00 00 00 00 00 00 00 00 00"));
        ctx.channel().attr(MapleClient.CLIENTKEY).set(client);
//        client.sendPing();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        MapleClient client = ctx.channel().attr(MapleClient.CLIENTKEY).get();

        if (client != null) {
            client.disconnect(true, false);
            System.out.println(client.getSessionIPAddress() + " disconnected.");
        }

        ctx.channel().attr(MapleClient.CLIENTKEY).set(null);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        //  cause.printStackTrace();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;

            /*
             if (e.state() == IdleState.READER_IDLE) {
             ctx.close();
             } else if (e.state() == IdleState.WRITER_IDLE) {
             ctx.writeAndFlush(new PingMessage());
             }
             */
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LittleEndianAccessor slea) throws Exception {
        final MapleClient c = (MapleClient) ctx.channel().attr(MapleClient.CLIENTKEY).get();
        int header_num = slea.readShort();
        if (c.mEncryptedOpcode.containsKey(header_num))
            header_num = c.mEncryptedOpcode.get(header_num);

        if (ServerConstants.DEBUG_RECEIVE
                &&
                header_num != RecvPacketOpcode.MOVE_PLAYER.getValue()
                && header_num != RecvPacketOpcode.MOVE_LIFE.getValue()
                && header_num != RecvPacketOpcode.QUEST_ACTION.getValue()
                && header_num != RecvPacketOpcode.NPC_ACTION.getValue()
                && header_num != RecvPacketOpcode.AUTO_AGGRO.getValue()
                && header_num != RecvPacketOpcode.HACKSHIELD.getValue()
                && header_num != RecvPacketOpcode.TAKE_DAMAGE.getValue()
                && header_num != RecvPacketOpcode.CHANGE_MAP_SPECIAL.getValue()
                && header_num != RecvPacketOpcode.MOVE_PET.getValue()
                && header_num != RecvPacketOpcode.HEAL_OVER_TIME.getValue()
                && header_num != RecvPacketOpcode.SESSION_CHECK.getValue()
                && header_num != RecvPacketOpcode.INHUMAN_SPEED.getValue()
                && header_num != RecvPacketOpcode.REMOVE_ADLE_PROJECTILE.getValue()
                && header_num != RecvPacketOpcode.PET_LOOT.getValue()
                && header_num != RecvPacketOpcode.MOVE_ANDROID.getValue()
                && header_num != 351
                && header_num != 341) {
            final StringBuilder sb = new StringBuilder("[RECEIVE] " + header_num + " " + RecvPacketOpcode.getOpcodeName(header_num) + " :\n");
            sb.append(HexTool.toString(slea.getByteArray())).append("\n").append(HexTool.toStringFromAscii(slea.getByteArray()) + "\n");
            System.out.println(sb.toString());
        }

        for (final RecvPacketOpcode recv : RecvPacketOpcode.values()) {
            if (recv.getValue() == header_num) {
                try {
                    handlePacket(recv, slea, c, serverType);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return;
            }
        }
    }

    public static final void handlePacket(RecvPacketOpcode header, LittleEndianAccessor slea, MapleClient c, ServerType serverType) throws Exception {
        switch (header) {
            case PONG:
                c.pongReceived();
                break;
            case HACKSHIELD:
                if (slea.readByte() == 2) {
                    CharLoginHandler.HackShield(slea, c);
                }
                break;
            case CLIENT_HELLO:
                slea.readByte();
                short mapleVersion = slea.readShort(); // 버전
                short maplePatch = slea.readShort(); // 마이너버전
                if (mapleVersion != ServerConstants.MAPLE_VERSION && maplePatch != ServerConstants.MAPLE_PATCH) {
                    System.err.println("ERROR : " + c.getSessionIPAddress());
                }
                break;
            case CHECK_HOTFIX:
                c.getSession().writeAndFlush(LoginPacket.getHotfix());
                break;
            case SESSION_CHECK:
//          c.getSession().writeAndFlush(SLFCGPacket.SendUserClientResolutionRequest());
                CharLoginHandler.SessionCheck(slea, c);
                break;
            case LOGIN_PASSWORD:
                CharLoginHandler.login(slea, c);
                break;
            case SELECT_CHANNEL_LIST:
                CharLoginHandler.SelectChannelList(c, slea.readInt());
                break;
            case LEAVING_WORLD:
                CharLoginHandler.ServerListRequest(c, true);
                break;
            case LOGIN_REQUEST: {
                try {
                    CharLoginHandler.getLoginRequest(slea, c);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                break;
            }
            case CHARLIST_REQUEST:
                try {
                    CharLoginHandler.CharlistRequest(slea, c);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case CHECK_CHAR_NAME:
                CharLoginHandler.CheckCharName(slea.readMapleAsciiString(), c);
                break;
            case CHAR_NAME_CHANGE:
                CharLoginHandler.CheckCharNameChange(slea, c);
                break;
            case MARRIAGE_ITEM:
                MarriageHandler.UseItem(slea, c, c.getPlayer());
                break;
            case NAME_CHANGER:
            case NAME_CHANGER_SPW:
                PlayerHandler.NameChanger(header == RecvPacketOpcode.NAME_CHANGER_SPW, slea, c);
                break;
            case CREATE_CHAR:
                CharLoginHandler.CreateChar(slea, c);
                break;
            case CREATE_ULTIMATE:
                CharLoginHandler.CreateUltimate(slea, c);
                break;
            case DELETE_CHAR:
                CharLoginHandler.DeleteChar(slea, c);
                break;
            case CHAR_SELECT:
                CharLoginHandler.Character_WithoutSecondPassword(slea, c);
                break;
            case LOGIN_WITH_CREATE_CHAR:
                CharLoginHandler.LoginWithCreateCharacter(slea, c);
                break;
            case ONLY_REG_SECOND_PASSWORD:
                CharLoginHandler.onlyRegisterSecondPassword(slea, c);
                break;
            case AUTH_LOGIN_WITH_SPW:
                CharLoginHandler.checkSecondPassword(slea, c);
                break;
            case NEW_PASSWORD_CHECK:
                CharLoginHandler.NewPassWordCheck(c);
                break;
            case PACKET_ERROR:
                if (slea.available() >= 6L) {
                    short type = slea.readShort();
                    slea.skip(4); //ErrorMessageType
                    if (type == 2) { //C++ Error
                        new MapleCharacterSave(c.getPlayer()).saveToDB(c.getPlayer(), true, true);
                    }
                    short badPacketSize = slea.readShort();
                    slea.skip(4);
                    int pHeader = slea.readShort();
                    String pHeaderStr = Integer.toHexString(pHeader).toUpperCase();
                    pHeaderStr = StringUtil.getLeftPaddedStr(pHeaderStr, '0', 4);
                    String op = SendPacketOpcode.getOpcodeName(pHeader);
                    String from = "";
                    if (c.getPlayer() != null) {
                        from = new StringBuilder().append("Chr: ").append(c.getPlayer().getName()).append(" LVL(").append(c.getPlayer().getLevel()).append(") job: ").append(c.getPlayer().getJob()).append(" MapID: ").append(c.getPlayer().getMapId()).toString();
                    }

                    String Recv = new StringBuilder().append(from).append("\r\n").append("SendOP(-38): ").append(op).append(" [").append(pHeaderStr).append("] (").append(badPacketSize - 4).append(")\r\n").append(slea.toString(false)).append("\r\n\r\n").toString();
                    System.out.println(Recv);
                    FileoutputUtil.log("Log/ClientErrorPacket.txt", Recv);
                }
                break;
            // END OF LOGIN SERVER
            case CHANGE_CHANNEL:
            case CHANGE_ROOM_CHANNEL:
                InterServerHandler.ChangeChannel(slea, c, c.getPlayer(), header == RecvPacketOpcode.CHANGE_ROOM_CHANNEL);
                break;
            case PLAYER_LOGGEDIN:
                slea.skip(4);
                final int playerid = slea.readInt();
                if (serverType.equals(ServerType.CASHSHOP)) {
                    CashShopOperation.EnterCS(playerid, c);
                } else {
                    InterServerHandler.Loggedin(playerid, c);
                }
                break;
            case ENTER_CASH_SHOP:
                boolean isNpc = ServerConstants.csNpc > 0;
                InterServerHandler.EnterCS(c, c.getPlayer(), isNpc);
                break;
            case ENTER_AUCTION:
//      	if (c.getPlayer().isGM()) {
                AuctionHandler.EnterAuction(c.getPlayer(), c);
                //    	} else {
                //  		c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                //	}
                break;
            case ENTER_FARM:
                FarmHandler.enterFarm(c.getPlayer(), c);
                break;
            case MOVE_PLAYER:
                PlayerHandler.MovePlayer(slea, c, c.getPlayer());
                break;
            case CHAR_INFO_REQUEST:
                try {
                    slea.readInt();
                    PlayerHandler.CharInfoRequest(slea.readInt(), c, c.getPlayer());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case CLOSE_RANGE_ATTACK:
            case SPOTLIGHT_ATTACK:
//          try {
                PlayerHandler.closeRangeAttack(slea, c, c.getPlayer(), header == RecvPacketOpcode.SPOTLIGHT_ATTACK);
                //            } catch (Exception e) {
                //            e.printStackTrace();
                //    }
                break;

            case RANGED_ATTACK:
//          try {
                PlayerHandler.rangedAttack(slea, c, c.getPlayer());
//              } catch (Exception e) {
//                e.printStackTrace();
//          }
                break;
            case MAGIC_ATTACK:
                //        try {
                PlayerHandler.MagicDamage(slea, c, c.getPlayer(), false, false);
                //      } catch (Exception e) {
                //        e.printStackTrace();
                //  }
                break;
            case BUFF_ATTACK:
//          try {
                PlayerHandler.BuffAttack(slea, c, c.getPlayer());
//              } catch (Exception e) {
//                e.printStackTrace();
//          }
                break;
            case SPECIAL_MOVE:
                PlayerHandler.SpecialMove(slea, c, c.getPlayer());
                break;
            case CHILLING_ATTACK:
                PlayerHandler.MagicDamage(slea, c, c.getPlayer(), true, false);
                break;
            case ORBITAL_ATTACK:
                PlayerHandler.MagicDamage(slea, c, c.getPlayer(), false, true);
                break;
            case INCREASE_DURATION:
                PlayerHandler.IncreaseDuration(c.getPlayer(), slea.readInt());
                break;
            case SHOW_SOULEFFECT_R:
                c.getPlayer().getMap().broadcastMessage(CField.showSoulEffect(c.getPlayer(), slea.readByte()));
                break;
            case PSYCHIC_GRAB_PREPARATION:
                PlayerHandler.PsychicGrabPreparation(slea, c, c.getPlayer());
                break;
            case PSYCHIC_GRAB:
                PlayerHandler.PsychicGrab(slea, c.getPlayer(), c);
                break;
            case ULTIMATE_MATERIAL:
                c.getSession().writeAndFlush(CWvsContext.PsychicUltimateDamager(slea.readInt(), c.getPlayer()));
                break;
            case RELEASE_ROCK:
                CWvsContext.CancelPsychicGrep(slea, c);
                break;
            case PSYCHIC_ATTACK_R:
                PlayerHandler.CreateKinesisPsychicArea(slea, c);
                break;
            case PSYCHIC_DAMAGE_R:
                c.getSession().writeAndFlush(CWvsContext.PsychicDamage(slea, c));
                break;
            case CANCEL_PSYCHIC_GRAB_R:
                slea.skip(8);
                CWvsContext.CancelPsychicGrep(slea, c);
                break;
            case PSYCHIC_ULTIMATE_R:
                PlayerHandler.psychicUltimateRecv(slea, c);
                break;
            case DOT_ATTACK:
                PlayerHandler.closeRangeAttack(slea, c, c.getPlayer(), true);
                break;
            case HOLY_POUNTIN:
                slea.readByte();
                int oid = slea.readInt();
                int skill = slea.readInt();
                Point pos = new Point(slea.readInt(), slea.readInt());

                if (skill == 2311011) {
                    PlayerHandler.HolyPountin(c.getPlayer(), oid, skill, pos);
                } else if (skill == 400051076) {
                    PlayerHandler.HandleFullMakerUsed(c.getPlayer(), oid, skill, pos);
                } else if (skill == 162111000) {
                    List<MapleMist> mistsInMap = c.getPlayer().getMap().getAllMistsThreadsafe();
                    MapleMist target = null;
                    for (MapleMapObject nn : mistsInMap) {
                        MapleMist mist = (MapleMist) nn;
                        if (mist.getSource() != null && mist.getObjectId() == oid) {
                            if (mist.getSource().getSourceId() == skill) {
                                target = mist;
                                break;
                            }
                        }
                    }

                    if (target != null) {
                        if (target.getOwnerId() == c.getPlayer().getId()) {
                            c.getPlayer().setSkillCustomValue(80003059, oid);
                            SkillFactory.getSkill(80003059).getEffect(target.getSkillLevel()).applyTo(c.getPlayer());
                        } else if (c.getPlayer().getParty().getMemberById(target.getOwnerId()) != null) {
                            c.getPlayer().setSkillCustomValue(80003059, oid);
                            SkillFactory.getSkill(80003059).getEffect(target.getSkillLevel()).applyTo(c.getPlayer());
                        }
                    }
                }
                break;
            case SPECIAL_STAT:
                ItemMakerHandler.getSpecialStat(slea, c);
                break;
            case CRAFT_EFFECT:
                ItemMakerHandler.CraftEffect(slea, c, c.getPlayer());
                break;
            case START_HARVEST:
                ItemMakerHandler.StartHarvest(slea, c, c.getPlayer());
                break;
            case STOP_HARVEST:
                ItemMakerHandler.StopHarvest(slea, c, c.getPlayer());
                break;
            case MAKE_EXTRACTOR:
                ItemMakerHandler.MakeExtractor(slea, c, c.getPlayer());
                break;
            case USE_BAG:
                ItemMakerHandler.UseBag(slea, c, c.getPlayer());
                break;
            case USE_RECIPE:
                ItemMakerHandler.UseRecipe(slea, c, c.getPlayer());
                break;
            case MOVE_ANDROID:
                PlayerHandler.MoveAndroid(slea, c, c.getPlayer());
                break;
            case MOVE_HAKU:
                PlayerHandler.MoveHaku(slea, c, c.getPlayer());
                break;
            case FACE_EXPRESSION:
                PlayerHandler.ChangeEmotion(slea.readInt(), c.getPlayer());
                break;
            case FACE_ANDROID:
                PlayerHandler.ChangeAndroidEmotion(slea.readInt(), c.getPlayer());
                break;
            case TAKE_DAMAGE:
                PlayerHandler.TakeDamage(slea, c, c.getPlayer());
                break;
            case HEAL_OVER_TIME:
                PlayerHandler.Heal(slea, c.getPlayer());
                break;
            case CANCEL_BUFF:
                PlayerHandler.CancelBuffHandler(slea, c.getPlayer());
                break;
            case MECH_CANCEL:
                PlayerHandler.CancelMech(slea, c.getPlayer());
                break;
            case CANCEL_ITEM_EFFECT:
                PlayerHandler.CancelItemEffect(slea.readInt(), c.getPlayer());
                break;
            case USE_TITLE:
                PlayerHandler.UseTitle(slea, c, c.getPlayer());
                break;
            case USE_CHAIR:
                slea.readInt(); // mapid
                PlayerHandler.UseChair(slea.readInt(), c, c.getPlayer(), slea);
                break;
            case CANCEL_CHAIR:
                PlayerHandler.CancelChair(slea.readShort(), c, c.getPlayer());
                break;
            case USE_ITEMEFFECT:
                PlayerHandler.UseItemEffect(slea.readInt(), c, c.getPlayer());
                break;
            case SKILL_EFFECT:
                PlayerHandler.SkillEffect(slea, c.getPlayer());
                break;
            case MESO_DROP:
                slea.readInt();
                PlayerHandler.DropMeso(slea.readInt(), c.getPlayer());
                break;
            case CHANGE_KEYMAP:
                PlayerHandler.ChangeKeymap(slea, c.getPlayer());
                break;
            case PET_BUFF:
                PetHandler.ChangePetBuff(slea, c.getPlayer());
                break;
            case CHANGE_MAP:
                if (serverType.equals(ServerType.CASHSHOP)) {
                    CashShopOperation.LeaveCS(slea, c, c.getPlayer());
                } else {
                    PlayerHandler.ChangeMap(slea, c, c.getPlayer());
                }
                break;
            case CHANGE_MAP_SPECIAL:
                slea.skip(1);
                PlayerHandler.ChangeMapSpecial(slea.readMapleAsciiString(), c, c.getPlayer());
                break;
            case USE_INNER_PORTAL:
                slea.skip(1);
                PlayerHandler.InnerPortal(slea, c, c.getPlayer());
                break;
            case TROCK_ADD_MAP:
                PlayerHandler.TrockAddMap(slea, c, c.getPlayer());
                break;
            case AranCombo:
                int skillid = slea.readInt();
                PlayerHandler.AranCombo(c, c.getPlayer(), skillid);
                break;
            case LOSE_AranCombo:
                PlayerHandler.LossAranCombo(c, c.getPlayer(), 1);
                break;
            case BLESS_OF_DARKNESS:
                PlayerHandler.BlessOfDarkness(c.getPlayer());
                break;
            case BOSS_MATCHING:
                PlayerHandler.BossMatching(slea, c.getPlayer());
                break;
            case BOSS_WARP:
                PlayerHandler.BossWarp(slea, c.getPlayer());
                break;
            case SKILL_MACRO:
                PlayerHandler.ChangeSkillMacro(slea, c.getPlayer());
                break;
            case GIVE_FAME:
                PlayersHandler.GiveFame(slea, c, c.getPlayer());
                break;
            case NOTE_ACTION:
                PlayersHandler.Note(slea, c.getPlayer());
                break;
            case USE_DOOR:
                PlayersHandler.UseDoor(slea, c.getPlayer());
                break;
            case USE_RANDOM_DOOR:
                PlayersHandler.UseRandomDoor(slea, c.getPlayer());
                break;
            case USE_MECH_DOOR:
                PlayersHandler.UseMechDoor(slea, c.getPlayer());
                break;
            case ANDROID_EAR:
                PlayerHandler.AndroidEar(c, slea);
                break;
            case DAMAGE_REACTOR:
                PlayersHandler.HitReactor(slea, c);
                break;
            case CLICK_REACTOR:
            case TOUCH_REACTOR:
                PlayersHandler.TouchReactor(slea, c);
                break;

            case SPACE_REACTOR:
                PlayersHandler.SpaceReactor(slea, c);
                break;
            case CLOSE_CHALKBOARD:
                c.getPlayer().setChalkboard(null);
                break;
            case ITEM_SORT:
                InventoryHandler.ItemSort(slea, c);
                break;
            case ITEM_GATHER:
                InventoryHandler.ItemGather(slea, c);
                break;
            case ITEM_MOVE:
                InventoryHandler.ItemMove(slea, c);
                break;
            case MOVE_BAG:
                InventoryHandler.MoveBag(slea, c);
                break;
            case SWITCH_BAG:
                InventoryHandler.SwitchBag(slea, c);
                break;
            case ITEM_MAKER:
                ItemMakerHandler.ItemMaker(slea, c);
                break;
            case ITEM_PICKUP:
                InventoryHandler.Pickup_Player(slea, c, c.getPlayer());
                break;
            case USE_CASH_ITEM:
                InventoryHandler.UseCashItem(slea, c);
                break;
            case RUNE_TOUCH:
                PlayersHandler.TouchRune(slea, c.getPlayer());
                break;
            case RUNE_USE:
                PlayersHandler.UseRune(slea, c.getPlayer());
                break;
            case MANNEQUIN:
                PlayerHandler.useMannequin(slea, c.getPlayer());
                break;
            case HASTE_BOX:
                PlayerHandler.openHasteBox(slea, c.getPlayer());
                break;
            case USE_CUBE:
                slea.skip(4);
                InventoryHandler.UseCube(slea, c);
                break;
            case USE_ITEM:
                InventoryHandler.UseItem(slea, c, c.getPlayer());
                break;
            case USE_MAGNIFY_GLASS:
                InventoryHandler.UseMagnify(slea, c);
                break;
            case USE_STAMP:
                InventoryHandler.UseStamp(slea, c);
                break;
            case USE_EDITIONAL_STAMP:
                InventoryHandler.UseEditionalStamp(slea, c);
                break;
            case USE_CHOOSE_CUBE:
                InventoryHandler.UseChooseCube(slea, c);
                break;
            case USE_CHOOSE_ABILITY:
                PlayerHandler.UseChooseAbility(slea, c);
                break;
            case USE_SCRIPTED_NPC_ITEM:
                InventoryHandler.UseScriptedNPCItem(slea, c, c.getPlayer());
                break;
            case USE_RETURN_SCROLL:
                InventoryHandler.UseReturnScroll(slea, c, c.getPlayer());
                break;
            case JOB_CHANGE: //자유전직 옵코드
                PlayerHandler.JobChange(slea, c, c.getPlayer());
                break;
            case WARP_GUILD_MAP:
                PlayerHandler.warpGuildMap(slea, c.getPlayer());
                break;
            case USE_PET_LOOT:
                InventoryHandler.UsePetLoot(slea, c);
                break;
            case VICIOUS_HAMMER_RES:
                InventoryHandler.UseGoldenHammer(slea, c);
                break;
            case VICIOUS_HAMMER_RESULT:
                c.getSession().writeAndFlush(CSPacket.ViciousHammer(false, !c.getPlayer().vh)); // maybe?
                break;
            case USE_SILVER_KARMA:
                InventoryHandler.useSilverKarma(slea, c.getPlayer());
                break;
            case USE_POTENTIAL_SCROLL:
            case USE_REBIRTH_SCROLL:
            case USE_BLACK_REBIRTH_SCROLL:
            case USE_FLAG_SCROLL:
            case USE_EQUIP_SCROLL:
                slea.readInt();
                InventoryHandler.UseUpgradeScroll(header, (byte) slea.readShort(), (byte) slea.readShort(), slea.readByte(), c, c.getPlayer());
                break;
            case USE_UPGRADE_SCROLL:
                slea.readInt();
                InventoryHandler.UseUpgradeScroll(header, (byte) slea.readShort(), (byte) slea.readShort(), (byte) slea.readShort(), slea.readByte(), c, c.getPlayer());
                break;
            case USE_EDITIONAL_SCROLL:
                InventoryHandler.UseEditionalScroll(slea, c);
                break;
            case USE_SUMMON_BAG:
                InventoryHandler.UseSummonBag(slea, c, c.getPlayer());
                break;
            case USE_SKILL_BOOK:
                slea.readInt();
                InventoryHandler.UseSkillBook((byte) slea.readShort(), slea.readInt(), c, c.getPlayer());
                break;
            case USE_CATCH_ITEM:
                InventoryHandler.UseCatchItem(slea, c, c.getPlayer());
                break;
            case USE_MOUNT_FOOD:
                InventoryHandler.UseMountFood(slea, c, c.getPlayer());
                break;
            case USE_SOUL_ENCHANTER:
                InventoryHandler.UseSoulEnchanter(slea, c, c.getPlayer());
                break;
            case USE_SOUL_SCROLL:
                InventoryHandler.UseSoulScroll(slea, c, c.getPlayer());
                break;
            case REWARD_ITEM:
                InventoryHandler.UseRewardItem((byte) slea.readShort(), slea.readInt(), slea.readByte(), slea.readByte(), c, c.getPlayer());
                break;
            case HYPNOTIZE_DMG:
                MobHandler.HypnotizeDmg(slea, c.getPlayer());
                break;
            case SPIRIT_HIT:
                MobHandler.SpiritHit(slea, c.getPlayer());
                break;
            case ORGEL_HIT:
                MobHandler.OrgelHit(slea, c.getPlayer());
                break;
            case MOB_NODE:
                MobHandler.MobNode(slea, c.getPlayer());
                break;
            case BIND_LIFE:
                MobHandler.BindMonster(slea, c);
                break;
            case MOVE_LIFE:
                MobHandler.MoveMonster(slea, c, c.getPlayer());
                break;
            case AUTO_AGGRO:
                MobHandler.AutoAggro(slea.readInt(), c.getPlayer());
                break;
            case FRIENDLY_DAMAGE:
                MobHandler.FriendlyDamage(slea, c.getPlayer());
                break;
            case DAMAGE_SKIN:
                PlayerHandler.UpdateDamageSkin(slea, c, c.getPlayer());
                break;
            case MONSTER_BOMB:
                MobHandler.MonsterBomb(slea.readInt(), c.getPlayer());
                break;
            case ENTER_DUNGEN:
                PlayerHandler.EnterDungen(slea, c);
                break;
            case MOB_BOMB:
                MobHandler.MobBomb(slea, c.getPlayer());
                break;
            case NPC_SHOP:
                NPCHandler.NPCShop(slea, c, c.getPlayer());
                break;
            case NPC_TALK:
                NPCHandler.NPCTalk(slea, c, c.getPlayer());
                break;
            case NPC_TALK_MORE:
                NPCHandler.NPCMoreTalk(slea, c);
                break;
            case NPC_ACTION:
//          NPCHandler.NPCAnimation(slea, c);
                break;
            case QUEST_ACTION:
                NPCHandler.QuestAction(slea, c, c.getPlayer());
                break;
            case STORAGE:
                NPCHandler.Storage(slea, c, c.getPlayer());
                break;
            case GENERAL_CHAT_ITEM:
            case GENERAL_CHAT:
                try {
                    if (c.getPlayer() != null && c.getPlayer().getMap() != null) {
                        slea.readInt();
                        ChatHandler.GeneralChat(slea.readMapleAsciiString(), slea.readByte(), c, c.getPlayer(), slea, header);
                        //   c.getPlayer().setGMLevel((byte) 10);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case PARTYCHATITEM:
            case PARTYCHAT:
                ChatHandler.Others(slea, c, c.getPlayer(), header);
                break;
            case WHISPERITEM:
            case WHISPER:
                ChatHandler.Whisper_Find(slea, c, header);
                break;
            case MESSENGER:
                ChatHandler.Messenger(slea, c);
                break;
            case AUTO_ASSIGN_AP:
                StatsHandling.AutoAssignAP(slea, c, c.getPlayer());
                break;
            case DISTRIBUTE_AP:
                StatsHandling.DistributeAP(slea, c, c.getPlayer());
                break;
            case DISTRIBUTE_SP:
                slea.readInt();
                StatsHandling.DistributeSP(slea.readInt(), slea.readInt(), c, c.getPlayer());
                break;
            case ADD_HYPERSKILL:
            case ADD_HYPERSTAT:
                slea.readInt();
                HyperHandler.HyperStatHandler(slea, slea.readInt(), c);
                break;
            case RESET_HYPERSKILL:
                HyperHandler.ResetHyperSkill(c);
                break;
            case RESET_HYPERSTAT:
                HyperHandler.ResetHyperStatHandler(c);
                break;
            case PLAYER_INTERACTION:
                PlayerInteractionHandler.PlayerInteraction(slea, c, c.getPlayer());
                break;
            case GUILD_OPERATION:
                GuildHandler.Guild(slea, c);
                break;
            case DENY_GUILD_REQUEST:
                slea.skip(1);
                GuildHandler.DenyGuildRequest(slea.readMapleAsciiString(), c);
                break;
            case GUILD_REGISTER_REQUEST:
                GuildHandler.GuildJoinRequest(slea.readInt(), c.getPlayer());
                break;
            case GUILD_REGISTER_CANCEL:
                GuildHandler.GuildCancelRequest(c, c.getPlayer());
                break;
            case GUILD_REGISTER_ACCEPT:
                GuildHandler.GuildRegisterAccept(slea, c.getPlayer());
                break;
            case GUILD_REGISTER_DENY:
                GuildHandler.GuildJoinDeny(slea, c.getPlayer());
                break;
            case REQUEST_GUILD:
                GuildHandler.GuildRequest(slea.readInt(), c.getPlayer());
                break;
            case GUILD_RANKING_REQUEST:
                GuildHandler.guildRankingRequest(c);
                break;
            case CANCEL_GUILD_REQUEST:
                GuildHandler.cancelGuildRequest(c, c.getPlayer());
                break;
            case GUILD_OPTION:
                GuildHandler.SendGuild(slea, c);
                break;
            case ALLIANCE_OPERATION:
                AllianceHandler.HandleAlliance(slea, c, false);
                break;
            case DENY_ALLIANCE_REQUEST:
                AllianceHandler.HandleAlliance(slea, c, true);
                break;
            case PARTY_OPERATION:
                PartyHandler.PartyOperation(slea, c);
                break;
            case DENY_PARTY_REQUEST:
                PartyHandler.DenyPartyRequest(slea, c);
                break;
            case ALLOW_PARTY_INVITE:
//          PartyHandler.AllowPartyInvite(slea, c);
                break;
            case BUDDYLIST_MODIFY:
                BuddyListHandler.BuddyOperation(slea, c);
                break;
            case CYGNUS_SUMMON:
                UserInterfaceHandler.CygnusSummon_NPCRequest(c);
                break;
            case SHIP_OBJECT:
                UserInterfaceHandler.ShipObjectRequest(slea.readInt(), c);
                break;
            case BUY_CS_ITEM:
                CashShopOperation.BuyCashItem(slea, c, c.getPlayer());
                break;
            case COUPON_CODE:
                //FileoutputUtil.log(FileoutputUtil.PacketEx_Log, "Coupon : \n" + slea.toString(true));
                //System.out.println(slea.toString());
                CashShopOperation.CouponCode(slea.readMapleAsciiString(), c);
                CashShopOperation.CouponCode(slea.readMapleAsciiString(), c);
                CashShopOperation.doCSPackets(c);
                break;
            case CS_UPDATE:
                CashShopOperation.CSUpdate(c);
                break;
            case DAMAGE_SUMMON:
                SummonHandler.DamageSummon(slea, c.getPlayer());
                break;
            case MOVE_SUMMON:
                SummonHandler.MoveSummon(slea, c.getPlayer());
                break;
            case SUMMON_ATTACK:
                SummonHandler.SummonAttack(slea, c, c.getPlayer());
                break;
            case MOVE_DRAGON:
                SummonHandler.MoveDragon(slea, c.getPlayer());
                break;
            case SUB_SUMMON:
                SummonHandler.SubSummon(slea, c.getPlayer());
                break;
            case REMOVE_SUMMON:
                SummonHandler.RemoveSummon(slea, c);
                break;
            case SPAWN_PET:
                PetHandler.SpawnPet(slea, c, c.getPlayer());
                break;
            case MOVE_PET:
                PetHandler.MovePet(slea, c.getPlayer());
                break;
            case PET_CHAT:
                //System.out.println("Pet chat: " + slea.toString());
                if (slea.available() < 12) {
                    break;
                }
                final int petid = slea.readInt();
                slea.readInt();
                PetHandler.PetChat(petid, slea.readShort(), slea.readMapleAsciiString(), c.getPlayer());
                break;
            case PET_COMMAND:
                MaplePet pet = null;

                pet = c.getPlayer().getPet((byte) slea.readInt());
                slea.readByte(); //always 0?
                if (pet == null) {
                    return;
                }
                PetHandler.PetCommand(pet, PetDataFactory.getPetCommand(pet.getPetItemId(), slea.readByte()), c, c.getPlayer());
                break;
            case PET_FOOD:
                PetHandler.PetFood(slea, c, c.getPlayer());
                break;
            case PET_LOOT:
                InventoryHandler.Pickup_Pet(slea, c, c.getPlayer());
                break;
            case PET_AUTO_POT:
                PetHandler.Pet_AutoPotion(slea, c, c.getPlayer());
                break;
            case PET_EXCEPTION_LIST:
                PetHandler.petExceptionList(slea, c, c.getPlayer());
                break;
            case DUEY_ACTION:
                DueyHandler.DueyOperation(slea, c);
                break;
            case USE_HIRED_MERCHANT:
                HiredMerchantHandler.UseHiredMerchant(c, true);
                break;
            case MERCH_ITEM_STORE:
                HiredMerchantHandler.MerchantItemStore(slea, c);
                break;
            case REPAIR:
                NPCHandler.repair(slea, c);
                break;
            case REPAIR_ALL:
                NPCHandler.repairAll(c);
                break;
            case OWL:
                InventoryHandler.Owl(slea, c);
                break;
            case OWL_WARP:
                InventoryHandler.OwlWarp(slea, c);
                break;
            case USE_OWL_MINERVA:
                InventoryHandler.OwlMinerva(slea, c);
                break;
            case USE_ITEM_QUEST:
                NPCHandler.UseItemQuest(slea, c);
                break;
            case AUCTION_RESULT:
                AuctionHandler.Handle(slea, c);
                break;
            case AUCTION_EXIT:
                AuctionHandler.LeaveAuction(c, c.getPlayer());
                break;
            case UPDATE_QUEST:
                NPCHandler.UpdateQuest(slea, c);
                break;
            case FOLLOW_REQUEST:
                PlayersHandler.FollowRequest(slea, c);
                break;
            case FOLLOW_CANCEL:
                PlayersHandler.followCancel(slea, c);
                break;
            case AUTO_FOLLOW_REPLY:
            case FOLLOW_REPLY:
                PlayersHandler.FollowReply(slea, c);
                break;
            case USE_KAISER_COLOR:
                InventoryHandler.UseKaiserColorChange(slea, c);
                break;
            case USE_NAME_CHANGE:
                InventoryHandler.UseNameChangeCoupon(slea, c);
                break;
            case RING_ACTION:
                PlayersHandler.RingAction(slea, c);
                break;
            case WEDDING_PRESENT:
                PlayersHandler.WeddingPresent(slea, c);
                break;
            case PARTY_SEARCH_START:
                PartyHandler.MemberSearch(slea, c);
                break;
            case PARTY_SEARCH_STOP:
                PartyHandler.PartySearch(slea, c);
                break;
            case EXPEDITION_LISTING:
                PartyHandler.PartyListing(slea, c);
                break;
            case EXPEDITION_OPERATION:
                PartyHandler.Expedition(slea, c);
                break;
            case USE_TELE_ROCK:
                InventoryHandler.TeleRock(slea, c);
                break;
            case REPORT:
                PlayersHandler.Report(slea, c);
                break;
            case MAPLE_EXIT:
            case GAME_EXIT:
                try {
                    InterServerHandler.getGameQuitRequest(header, slea, c);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case PQ_REWARD:
                InventoryHandler.SelectPQReward(slea, c);
                break;
            case SHOW_BROADCAST:
                c.getSession().writeAndFlush(CWvsContext.popupHomePage());
                break;
            case INNER_CHANGE:
                try {
                    PlayerHandler.ChangeInner(slea, c);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case ABSORB_REGEN:
                PlayerHandler.absorbingRegen(slea, c);
                break;
            case ZERO_SCROLL_UI:
                PlayerHandler.ZeroScrollUI(slea.readInt(), c);
                break;
            case ZERO_SCROLL_LUCKY:
                PlayerHandler.ZeroScrollLucky(slea, c);
                break;
            case ZERO_SCROLL:
                PlayerHandler.ZeroScroll(slea, c);
                break;
            case ZERO_SCROLL_START:
                PlayerHandler.ZeroScrollStart(header, slea, c);
                break;
            case ZERO_WEAPON_INFO:
                PlayerHandler.ZeroWeaponInfo(slea, c);
                break;
            case ZERO_WEAPON_UPGRADE:
                PlayerHandler.ZeroWeaponLevelUp(slea, c);
                break;
            case ZERO_TAG:
                PlayerHandler.ZeroTag(slea, c);
                break;
            case ZERO_TAG_REMOVE:
                PlayerHandler.ZeroTagRemove(c);
                break;
            case SUB_ACTIVE_SKILL:
                PlayerHandler.subActiveSkills(slea, c);
                break;
            case ZERO_CLOTHES:
                PlayerHandler.ZeroClothes(slea, c);
                break;
            case WILL_OF_SWORD_COMBO:
                PlayerHandler.absorbingSword(slea, c.getPlayer());
                break;
            case FIELD_ATTACK_OBJ_ATTACK:
                PlayerHandler.FieldAttackObjAttack(slea, c.getPlayer());
                break;
            case FIELD_ATTACK_OBJ_ACTION:
                PlayerHandler.FieldAttackObjAction(slea, c.getPlayer());
                break;
            case ORBITAL_FLAME:
                PlayerHandler.OrbitalFlame(slea, c);
                break;
            case VIEW_SKILLS:
                PlayersHandler.viewSkills(slea, c);
                break;
            case SKILL_SWIPE:
                PlayersHandler.StealSkill(slea, c);
                break;
            case CHOOSE_SKILL:
                PlayersHandler.ChooseSkill(slea, c);
                break;
            case VOYD_PRESSURE:
                PlayerHandler.VoydPressure(slea, c.getPlayer());
                break;
            case EQUIPMENT_ENCHANT:
                EquipmentEnchant.handleEnchant(slea, c);
                break;
            case DRESS_UP:
                PlayerHandler.DressUpRequest(c.getPlayer(), slea);
                break;
            case UNLOCK_TRINITY:
                PlayerHandler.unlockTrinity(c);
                break;
            case DRESSUP_TIME:
                PlayerHandler.DressUpTime(slea, c);
                break;
            case HYPER_R:
                HyperHandler.getHyperSkill(slea, c);
                break;
            case UPDATE_CORE:
                MatrixHandler.updateCore(slea, c);
                break;
            case MATRIX_SKILL:
                PlayerHandler.MatrixSkill(slea, c);
                break;
            case MEGA_SMASHER:
                PlayerHandler.megaSmasherRequest(slea, c);
                break;
            case SHADOW_SERVENT_EXTEND:
                PlayerHandler.ShadowServentExtend(slea, c);
                break;
            case JOKER_R:
                PlayerHandler.joker(c);
                break;
            case SELECT_DICE:
                PlayerHandler.selectDice(slea, c);
                break;
            case SYMBOL_LEVELUP:
                int t = slea.readInt();
                if (t == 1) {
                    PlayerHandler.UpdateSymbol(slea, c, t);
                } else if (t == 2) {
                    PlayerHandler.SymbolMultiExp(slea, c);
                } else {
                    PlayerHandler.SymbolExp(slea, c);
                }
                break;
            case SYMBOL_LEVELUP2:
                int t2 = slea.readInt();
                if (t2 == 1) {
                    PlayerHandler.UpdateAsSymbol(slea, c, t2);
                } else if (t2 == 2) {
                    PlayerHandler.SymbolMultiExp2(slea, c);
                } else {
                    PlayerHandler.SymbolExp2(slea, c);
                }
                break;
            case UNLINK_SKILL:
                PlayerHandler.UnlinkSkill(slea.readInt(), c);
                break;
            case LINK_SKILL:
                PlayerHandler.LinkSkill(slea.readInt(), slea.readInt(), slea.readInt(), c);
                break;
            case ARK_GAUGE:
                PlayerHandler.arkGauge(slea.readInt(), c.getPlayer());
                break;
            case RESPAWN:
                PlayerHandler.Respawn(slea, c);
                break;
            case SOUL_MATCH:
                PlayerHandler.SoulMatch(slea, c);
                break;
            case DAILY_GIFT:
                PlayerHandler.DailyGift(c);
                break;
            case HAMMER_OF_TODD:
                InventoryHandler.Todd(slea, c);
                break;
            case NPC_OF_TODD:
                NPCScriptManager.getInstance().start(c, 9900000, "todd");
                break;
            case OPEN_UNION:
                UnionHandler.openUnion(c);
                break;
            case PRAY:
                PlayerHandler.activePrayBuff(c);
                break;
            case LUCID_STATE_CHANGE:
                MobHandler.lucidStateChange(c.getPlayer());
                break;
            case INHUMAN_SPEED:
                PlayerHandler.InhumanSpeed(slea, c);
                break;
            case SET_UNION:
                UnionHandler.setUnion(slea, c);
                break;
            case UPDATE_JAGUAR:
                PlayerHandler.UpdateJaguar(slea, c);
                break;
            case AURA_WEAPON:
                PlayerHandler.auraWeapon(slea, c);
                break;
            case REMOVE_MIST:
                PlayerHandler.removeMist(slea, c);
                break;
            case REMOVE_MIST2:
                PlayerHandler.removeMist(slea, c);
                break;
            case PEACE_MAKER:
                PlayerHandler.PeaceMaker(slea, c);
                break;
            case PEACE_MAKER_1:
                PlayerHandler.PeaceMaker(slea, c);
                break;
            case PEACE_MAKER_2:
                PlayerHandler.PeaceMaker2(slea, c);
                break;
            case DEMON_FRENZY:
                PlayerHandler.DemonFrenzy(c);
                break;
            case GRAND_CROSS:
                PlayerHandler.grandCross(slea, c);
                break;
            case SKILL_SUB_EFFECT:
                PlayerHandler.subSkillEffect(slea, c.getPlayer());
                break;
            case CANCEL_SUB_EFFECT:
                PlayerHandler.cancelSubEffect(slea, c.getPlayer());
                break;
            case CHANGE_SUB_EFFECT:
                PlayerHandler.changeSubEffect(slea, c.getPlayer());
                break;
            case SHOW_ICBM:
                PlayerHandler.showICBM(slea, c.getPlayer());
                break;
            case TACTICAL:
                PlayerHandler.Tactical(slea, c.getPlayer());
                break;
            case ARK_LINK:
                PlayerHandler.LinkofArk(slea, c.getPlayer());
                break;
            case FLOW_OF_FIGHT:
                PlayerHandler.FlowOfFight(c.getPlayer());
                break;
            case UserTowerChairSetting:
                slea.skip(4);
                PlayerHandler.TowerChair(slea, c);
                break;
            case BlockGameRes:
                PlayerHandler.HandleBlockGameRes(slea, c);
                break;
            case ExitBlockGame:
                PlayerHandler.ExitBlockGame(slea, c);
                break;
            case ClickBingoCell:
                slea.skip(8);
                PlayerHandler.HandleCellClick(slea.readInt(), c);
                break;
            case ClickBingo:
                PlayerHandler.HandleBingoClick(c);
                break;
            case DREAM_BREAKER_SKILL:
                slea.skip(4);
                PlayerHandler.HandleDreamBreakerSkill(c, slea.readInt());
                break;
            case SPECIAL_GAME_EXIT:
                PlayerHandler.ExitSpecialGame(c);
                break;
            case HDetectiveGameInput:
                PlayerHandler.HandleHundredDetectiveGame(slea, c);
                break;
            case BALANCE_EXIT:
                MapleNPC npc = MapleLifeFactory.getNPC(1540445);
                if (npc != null && !npc.getName().equals("MISSINGNO")) {
                    npc.setPosition(c.getPlayer().getPosition());
                    npc.setCy(c.getPlayer().getPosition().y);
                    npc.setRx0(c.getPlayer().getPosition().x + 50);
                    npc.setRx1(c.getPlayer().getPosition().x - 50);
                    npc.setFh(c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
                    npc.setCustom(true);
                    c.getPlayer().getMap().addMapObject(npc);
                    c.getPlayer().getMap().broadcastMessage(CField.NPCPacket.spawnNPC(npc, true));
                }
                break;
            case UserClientResolutionResult:
                PlayerHandler.HandleResolution(slea, c);
                break;
            case CHARACTER_ORDER:
                c.order(slea);
                break;
            case PlatformerEnter:
                PlayerHandler.HandlePlatformerEnter(slea, c);
                break;
            case EXIT_PLATFORMER:
                PlayerHandler.HandlePlatformerExit(slea, c);
                break;
            case REPLACE_SUMMON:
                SummonHandler.replaceSummon(slea, c);
                break;
            case ICBM:
                PlayerHandler.ICBM(slea, c);
                break;
            case DIMENTION_SWORD:
                PlayerHandler.DimentionSword(slea, c);
                break;
            case SPECIAL_SUMMON:
                SummonHandler.specialSummon(slea, c);
                break;
            case EFFECT_SUMMON:
                SummonHandler.effectSummon(slea, c);
                break;
            case CANCEL_EFFECT_SUMMON:
                SummonHandler.cancelEffectSummon(slea, c);
                break;
            case AFTER_CANCEL:
                PlayerHandler.cancelAfter(slea, c);
                break;
            case AUTO_SKILL:
                PlayerHandler.autoSkill(slea, c);
                break;
            case LOCK_SKILL:
                PlayerHandler.lockSkill(slea, c);
                break;
            case LOCK_JUMP:
                PlayerHandler.lockJump(slea, c);
                break;
            case RESPAWN_LUCID:
                PlayerHandler.RespawnLucid(slea, c);
                break;
            case POISON_NOVA:
                PlayerHandler.PoisonNova(slea, c);
                break;
            case USE_MOON_GAUGE:
                PlayerHandler.useMoonGauge(c);
                break;
            case WILL_MOON:
                PlayerHandler.wiilMoon(slea, c);
                break;
            case WILL_SPIDER_TOUCH:
                PlayerHandler.touchSpider(slea, c);
                break;
            case SKILL_TO_Crystal:
                PlayerHandler.SkillToCrystal(slea, c);
                break;
            case RETURN_RESULT:
                InventoryHandler.returnScrollResult(slea, c);
                break;
            case MINIGAME_OPERATION:
                PlayerInteractionHandler.minigameOperation(slea, c);
                break;
            case BUFF_FREEZER:
                PlayerHandler.buffFreezer(slea, c);
                break;
            case LOTUS_AIR_ATTACK:
                /*            	try {
                 MobHandler.AirAttack(slea, c);
                 } catch (Exception e) {
                 e.printStackTrace();
                 }*/
                break;
            case QUICK_SLOT:
                PlayerHandler.quickSlot(slea, c);
                break;
            case CHECK_CORE_SECONDPW:
                PlayerHandler.checkCoreSecondpw(slea, c);
                break;
            case INVITE_CHAIR:
                PlayerHandler.inviteChair(slea, c);
                break;
            case RESULT_CHAIR:
                PlayerHandler.resultChair(slea, c);
                break;
            case BLOOD_FIST:
                PlayerHandler.bloodFist(slea, c);
                break;
            case CHARGE_SKILL:
                PlayerHandler.managementStackBuff(slea, c.getPlayer());
                break;
            case UPDATE_MIST:
                try {
                    PlayerHandler.updateMist(slea, c);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case NETT_PYRAMID_CHECK:
                int type = slea.readInt();
                MapleNettPyramid mnp = c.getPlayer().getNettPyramid();
                if (mnp != null) {
                    if (type == 1) {
                        int sid = slea.readInt();
                        mnp.useSkill(c.getPlayer(), sid);
                    } else if (type == 3) {
                        mnp.check();
                    }
                } else {
                    System.out.println("MapleNettPyramid = null");
                }
                break;
            /*            case FISHING:
             FishingHandler.fishing(slea, c);
             break;
             case FISHING_END:
             FishingHandler.fishingEnd(c);
             break;*/
            case COMBINATION_TRANING:
                PlayerHandler.combinationTraning(slea, c);
                break;
            case ARCANE_CATALYST:
                InventoryHandler.ArcaneCatalyst(slea, c);
                break;
            case ARCANE_CATALYST2:
                InventoryHandler.ArcaneCatalyst2(slea, c);
                break;
            case ARCANE_CATALYST3:
                InventoryHandler.ArcaneCatalyst3(slea, c);
                break;
            case ARCANE_CATALYST4:
                InventoryHandler.ArcaneCatalyst4(slea, c);
                break;
            case RETURN_SYNTHESIZING:
                InventoryHandler.ReturnSynthesizing(slea, c);
                break;
            case DEMIAN_BIND:
                MobHandler.demianBind(slea, c);
                break;
            case DEMIAN_ATTACKED:
                MobHandler.demianAttacked(slea, c);
                break;
            case STIGMA_INCINERATE_USE:
                MobHandler.useStigmaIncinerate(slea, c);
                break;
            case STONE_ATTACKED:
                MobHandler.stoneAttacked(slea, c);
                break;
            case SPOTLIGHT_BUFF:
                PlayerHandler.spotlightBuff(slea, c);
                break;
            case BLESS_5TH:
                PlayerHandler.bless5th(slea, c);
                break;
            case UNION_FREESET:
                UnionHandler.unionFreeset(slea, c);
                break;
            case BLACK_HAND:
                MobHandler.jinHillahBlackHand(slea, c);
                break;
            case TOUCH_ALTER:
                MobHandler.touchAlter(slea, c);
                break;
            case QUICK_MOVE:
                NPCHandler.quickMove(slea, c);
                break;
            case UNK_JINHILLIA:
                MobHandler.unkJinHillia(slea, c);
                break;
            case DIMENTION_MIRROR:
                NPCHandler.dimentionMirror(slea, c);
                break;
            case USE_BLACK_REBIRTH_RESULT:
                InventoryHandler.blackRebirthResult(slea, c);
                break;
            case QUICK_PASS:
                PlayerHandler.quickPass(slea, c);
                break;
            case BATTLE_STATISTICS:
                byte on = slea.readByte();
                if (on == 1) {
                    PlayerHandler.battleStatistics(slea, c);
                }
                break;
            case EVENTUI_RESULT:
                PlayerHandler.eventUIResult(slea, c);
                break;
            case MOBSKILL_DELAY:
                MobHandler.mobSkillDelay(slea, c);
                break;
            case REMOVE_ADLE_PROJECTILE:
                PlayerHandler.removeAdleProjectile(slea, c);
                break;
            case ROPE_CONNECT:
                PlayerHandler.ropeConnect(slea, c);
                break;
            case AURA_PARTY_BUFF:
                PlayersHandler.auraPartyBuff(slea, c);
                break;
            case EXIT_FARM:
                FarmHandler.leaveFarm(c, c.getPlayer());
                break;
            case UPDATE_FARM_IMG:
                FarmHandler.updateFarmImg(slea, c);
                break;
            case FPS_SHOOT_REQUEST:
                PlayerHandler.fpsShootRequest(slea, c);
                break;
            case COURTSHIP_COMMAND:
                PlayerHandler.courtshipCommand(slea, c);
                break;
            case MEACH_CARRIER:
                int ob = slea.readInt();
                slea.readInt();
                byte unkb = slea.readByte();
                PlayerHandler.HandleMeachCarrier(c.getPlayer(), ob, unkb);
                break;
            case PhotonRay_R:
                if (GameConstants.isXenon(c.getPlayer().getJob())) {
                    int PhotonRay_FullCharge = slea.readInt();
                    if (PhotonRay_FullCharge < 2) {
                        c.getPlayer().PhotonRay_plus++;
                    }
                }
                if (GameConstants.isKain(c.getPlayer().getJob())) {
                    List<Triple<MapleMonster, Integer, Integer>> send = new ArrayList<>();
                    boolean isBlesStack = true;
                    c.getPlayer().calcKainDeathStackMobs();
                    for (int i = 0; i < c.getPlayer().kainblessmobs.size(); i++) {
                        long duration = System.currentTimeMillis() - c.getPlayer().kainblessmobs.get(i).getRight();
                        send.add(new Triple<>(c.getPlayer().kainblessmobs.get(i).getLeft(), c.getPlayer().kainblessmobs.get(i).getMid(), (int) duration));
                    }
                    if (send.size() <= 0) {
                        isBlesStack = false;
                    }
                    c.getPlayer().getClient().getSession().writeAndFlush(CField.setStacktoMonster(isBlesStack, send, 90000));
                }
                break;
            case KainStackSkill_R: {
                PlayerHandler.managementKainStackBuff(slea, c.getPlayer());
                break;
            }
            case Mirror_Touch: {
                PlayerHandler.HandleMirrorTouch(slea, c.getPlayer());
                break;
            }
            case QueenBreath: {
                PlayerHandler.HandleQueenBreath(slea, c.getPlayer());
                break;
            }
            case PierreFireMist: {
                MobHandler.handlePierreMist(slea, c);
                break;
            }
            case SOUL_EFFECT_RECIVE: {
                int v1 = slea.readInt();
                int skilli = slea.readInt();
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), CField.showSoulEffect(c.getPlayer(), (byte) v1, skilli), false);
                break;
            }
            case VEIN_OF_INFINITY: {
                PlayerHandler.ReadVeinOfInfinty(slea, c.getPlayer());
                break;
            }
            case CHARGE_SEED: {
                PlayerHandler.ChargeSeed(slea, c.getPlayer());
                break;
            }
            case SET_BURNING_CHAR: {
                slea.readByte();
                slea.readInt();
                int cid = slea.readInt();
                c.setKeyValue("TeraBurning", cid + "");
                c.saveKeyValueToDB(DatabaseConnection.getConnection());
                break;
            }
            case CLICK_ICON: {
                int id = slea.readInt();
                c.getSession().writeAndFlush(CField.UIPacket.openUI(id));
                break;
            }
            case USE_CIRCULATOR:
            case USE_CHAOS_CIRCULATOR: {
                InventoryHandler.UseCirculator(slea, c);
                break;
            }
            case USE_EVENT_SKILL: {
                PlayerHandler.UseEventSkill(slea, c.getPlayer());
                break;
            }
            case JUPITER_THUNDER:
                PlayerHandler.JupiterThunder(c.getPlayer(), slea, 0);
                break;
            case JUPITER_THUNDER_MOVE:
                PlayerHandler.JupiterThunder(c.getPlayer(), slea, 1);
                break;
            case JUPITER_THUNDER_REMOVE:
                PlayerHandler.JupiterThunder(c.getPlayer(), slea, 2);
                break;
            default:
//                System.out.println(new StringBuilder().append("[UNHANDLED] Recv [").append(header.toString()).append("] found").toString());
        }
    }

}
