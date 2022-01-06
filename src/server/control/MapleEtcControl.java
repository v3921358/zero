package server.control;

import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleCharacterSave;
import client.SkillFactory;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import constants.ServerConstants;
import handling.auction.AuctionServer;
import handling.channel.ChannelServer;
import handling.channel.handler.InventoryHandler;
import handling.world.World;
//import org.json.simple.JSONArray;
//import org.json.simple.JSONObject;
//import org.json.simple.parser.JSONParser;
//import org.json.simple.parser.ParseException;
//import org.jsoup.Jsoup;
import scripting.NPCScriptManager;
import server.MapleInventoryManipulator;
import server.Randomizer;
import server.life.MapleMonsterInformationProvider;
import server.maps.MapleMap;
import tools.CurrentTime;
import tools.Pair;
import tools.packet.CField;
import tools.packet.CWvsContext.BuffPacket;
import tools.packet.CWvsContext.InfoPacket;
import tools.packet.SLFCGPacket;

import java.awt.*;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import tools.Triple;

public class MapleEtcControl implements Runnable {

    public long lastClearDropTime = 0, lastResetTimerTime = 0;
    public int date;
    public long lastCoreTime = 0;

    public MapleEtcControl() {
        lastClearDropTime = System.currentTimeMillis();
        date = CurrentTime.요일();
        System.out.println("[Loading Completed] Start EtcControl");
    }

    @Override
    public void run() {
        long time = System.currentTimeMillis();

//        String strJson = null;
//        try {
//            strJson = Jsoup.connect("https://api.upbit.com/v1/ticker?markets=KRW-BTC").ignoreContentType(true).get().text();
//        } catch (IOException ex) {
////            ex.printStackTrace();
//        }
//
//        if (strJson != null) {
//            JSONParser jsonParser = new JSONParser();
//            JSONArray array = null;
//
//            try {
//                array = (JSONArray) jsonParser.parse(strJson);
//            } catch (ParseException ex) {
////                ex.printStackTrace();
//            }
//
//            if (array.size() > 0) {
//                JSONObject obj = (JSONObject) array.get(0);
//                float tradePrice = 0.0f;
//                if (obj.containsKey("trade_price")) {
//                    try {
//                        tradePrice = Float.parseFloat(obj.get("trade_price").toString());
//                    } catch (NumberFormatException ex) {
////                        ex.printStackTrace();
//                    }
//                    GameConstants.bitcoin = (int) tradePrice;
//                } else {
//                    System.err.println("trade_price is not exist");
//                }
//
//            }
//        }

        if (time - lastClearDropTime >= 1000 * 60 * 60) {
            lastClearDropTime = time;
            MapleMonsterInformationProvider.getInstance().clearDrops();
            AuctionServer.saveItems();
            System.out.println("드롭 데이터를 초기화했습니다.");
        }

        /*        if (time - lastResetTimerTime >= 1000 * 60 * 60 * 6) {
         lastResetTimerTime = time;
         MobTimer.getInstance().stop();
         MobTimer.getInstance().start();
         }*/
        Iterator<ChannelServer> channels = ChannelServer.getAllInstances().iterator();
        while (channels.hasNext()) {
            ChannelServer cs = channels.next();
            Iterator<MapleCharacter> chrs = cs.getPlayerStorage().getAllCharacters().values().iterator();
            while (chrs.hasNext()) {

                MapleCharacter chr = chrs.next();

                if (chr.getLevel() >= 301) {
                    if (chr.getSkillLevel(80000545) > 0) {
                        chr.changeSkillLevel(80000545, (byte) 0, (byte) 0);
                        chr.getStat().recalcLocalStats(chr);
                    }
                }

                if (chr.isAlive()) {
                    List<Integer> prevEffects = chr.getPrevBonusEffect();
                    List<Integer> curEffects = chr.getBonusEffect();

                    for (int i = 0; i < curEffects.size(); i++) {
                        if (prevEffects.get(i) != curEffects.get(i)) {
                            chr.cancelEffectFromBuffStat(MapleBuffStat.IndieDamR, 80002419);
                            chr.cancelEffectFromBuffStat(MapleBuffStat.IndieExp, 80002419);
                            chr.cancelEffectFromBuffStat(MapleBuffStat.DropRate, 80002419);
                            chr.cancelEffectFromBuffStat(MapleBuffStat.MesoUp, 80002419);
                            chr.cancelEffectFromBuffStat(MapleBuffStat.IndieCD, 80002419);
                            chr.cancelEffectFromBuffStat(MapleBuffStat.IndieBDR, 80002419);
                            chr.cancelEffectFromBuffStat(MapleBuffStat.IndieAllStatR, 80002419);
                            chr.cancelEffectFromBuffStat(MapleBuffStat.IndiePmdR, 80002419);
                            SkillFactory.getSkill(80002419).getEffect(1).applyTo(chr);
                            chr.getStat().recalcLocalStats(chr);
                            break;
                        }
                    }
                }

                //if (chr.getMapId() == 261020700) {
                    if(chr.getMapId() == 261020700 || chr.getMapId() == 261010103) {
                    chr.setKeyValue(124, "ppp", String.valueOf(chr.getKeyValue(124, "ppp") + 1000));
                    if (chr.getKeyValue(123, "pp") <= 0) {
                        chr.warp(100000000);
                        chr.dropMessage(5, "피로도가 없어 마을로 돌아갑니다.");
                    } else {
                        if (chr.getKeyValue(124, "ppp") > 60000) {
                            chr.setKeyValue(123, "pp", String.valueOf(chr.getKeyValue(123, "pp") - 2));
                            chr.setKeyValue(124, "ppp", "0");
                            if (chr.getKeyValue(123, "pp") < 0) {
                                chr.setKeyValue(123, "pp", String.valueOf(0));
                            }
                            chr.dropMessage(5, "피로도가 감소합니다. 남은 피로도 : " + chr.getKeyValue(123, "pp"));
                        }
                    }
                }
                Calendar cal = Calendar.getInstance();
                if (cal.get(Calendar.HOUR_OF_DAY) == 0 && cal.get(Calendar.MINUTE) < 1) {
                    chr.setKeyValue(125, "date", String.valueOf(GameConstants.getCurrentDate_NoTime()));
                    int pirodo = 0;
                    switch (chr.getTier()) {
                        case 1: {
                            pirodo = 50;
                            break;
                        }
                        case 2: {
                            pirodo = 70;
                            break;
                        }
                        case 3: {
                            pirodo = 90;
                            break;
                        }
                        case 4: {
                            pirodo = 110;
                            break;
                        }
                        case 5: {
                            pirodo = 130;
                            break;
                        }
                        case 6: {
                            pirodo = 150;
                            break;
                        }
                        case 7: {
                            pirodo = 170;
                            break;
                        }
                        case 8: {
                            pirodo = 200;
                            break;
                        }
                    }
                    chr.setKeyValue(123, "pp", String.valueOf(pirodo));
                    //  chr.dropMessage(5, "자정이지나 피로도가 초기화 되었습니다.");
                }
//                if (time - lastCoreTime >= 1800000) { //900000
//                    lastCoreTime = time;
//                    int ret = 0;
//                    for (ChannelServer csrv : ChannelServer.getAllInstances()) {
//                        int a = csrv.getPlayerStorage().getAllCharacters().size();
//                        ret += a;
//                    }
//                    final int core = ret;
//                    for (ChannelServer csrv : ChannelServer.getAllInstances()) {
//                        for (MapleCharacter chra : csrv.getPlayerStorage().getAllCharacters1()) {
//                            //chr.getClient().getSession().writeAndFlush(CField.EffectPacket.showTextEffect(chr,"#fn나눔고딕 ExtraBold##fs30##fc0xFFA50000#[알림] 제로에 접속하신 걸 환영합니다. "+ret+" 네오 코어를 획득하였습니다.",0,4));
//                            int coreg = core + (int) chra.getKeyValue(501215, "point");
//                            chra.setKeyValue(501215, "point", coreg + "");
//                            int lock = chra.isLockNeoCore() ? 1 : 0;
//                            chra.updateInfoQuest(501215, "point=" + chra.getKeyValue(501215, "point") + ";sum=0;date=" + GameConstants.getCurrentDate_NoTime() + ";;week=0;total=0;today=" + chra.getKeyValue(501215, "today") + ";lock=" + lock + ""); //네오 코어
//                        }
//                    }
//                }

                for (MapleInventory inv : chr.getInventorys()) { // 기간제 아이템 삭제
                    Iterator<Item> items = inv.list().iterator();
                    while (items.hasNext()) {
                        Item item = items.next();
                        if (item.getExpiration() != -1 && (item.getExpiration() <= time)) {
                            if (item.getPosition() < 0) {
                                MapleInventoryManipulator.unequip(chr.getClient(), item.getPosition(), chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot(), MapleInventoryType.EQUIP);
                            }
                            MapleInventoryManipulator.removeFromSlot(chr.getClient(), GameConstants.getInventoryType(item.getItemId()), item.getPosition(), item.getQuantity(), false);
                            chr.getClient().getSession().writeAndFlush(InfoPacket.itemExpired(item.getItemId()));
                        }
                    }
                }

                if (chr.getClient().getChatBlockedTime() > 0) {
                    if (time - chr.getClient().getChatBlockedTime() >= 0) {
                        chr.getClient().setChatBlockedTime(0);
                        chr.dropMessage(5, "채팅 금지 시간이 지나 금지가 해제됩니다.");
                    }
                }
                
                // 매크로
                if (Randomizer.isSuccess2(4) && chr.getMap().getAllMonster().size() > 0 && chr.getChair() == 0 && chr.getEventInstance() == null) {
//                    chr.lastMacroTime = time;
                    NPCScriptManager.getInstance().start(chr.getClient(), 9900003);
                }

                if (time - chr.lastSaveTime >= 60 * 10 * 1000 && chr.choicepotential == null && chr.returnscroll == null && chr.memorialcube == null) {
                    new MapleCharacterSave(chr).saveToDB(chr, false, false);
                    chr.dropMessage(-8, "[알림] " + CurrentTime.시() + "시 " + CurrentTime.분() + "분 캐릭터를 서버에 저장하였습니다.");

                    if (chr.getGuildId() > 0) {
                        World.Guild.gainContribution(chr.getGuildId(), 50, chr.getId());
                        chr.getClient().getSession().writeAndFlush(InfoPacket.getGPContribution(50));
                    }
                }

                if (chr.getChair() != 0 && chr.getMapId() == ServerConstants.WarpMap) {
                    if (time - chr.lastChairPointTime >= 60000) {
                        chr.lastChairPointTime = time;
                        int point = (int) chr.getKeyValue(100712, "point");
                        point += 1;
                        chr.setKeyValue(100712, "point", point + "");
                        chr.updateInfoQuest(100712, "point=" + chr.getKeyValue(100712, "point") + ";start=1;date=" + GameConstants.getCurrentDate_NoTime() + ";today=0;start2=1;lock=1"); //네오 젬
                    }
                }

                /* 무릉 도장 */
                if (chr.getDojoStartTime() > 0) {
                    Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                    statups.put(MapleBuffStat.MobZoneState, new Pair<>(1, 0));
                    chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, null, chr));
                }

                //타이머
                if (chr.getDojoStartTime() > 0) {
                    if (chr.getDojoStopTime() > 0) {
                        chr.setDojoCoolTime(chr.getDojoCoolTime() + 1000); // 1초마다 1초씩 감소해줘야 함..
                        if (time - chr.getDojoStopTime() > 10000) {
                            chr.setDojoStopTime(0);
                            chr.getClient().getSession().writeAndFlush(CField.getDojoClockStop(false, (int) (900 - ((System.currentTimeMillis() - chr.getDojoStartTime()) / 1000))));
                        }
                    } else {
                        if ((time - chr.getDojoStartTime() - chr.getDojoCoolTime()) > (chr.getMapId() / 1000 == 925070 ? 300000 : chr.getMapId() / 1000 == 925071 ? 600000 : 900000)) {
                            MapleMap to = chr.getMap().getReturnMap();
                            chr.changeMap(to, to.getPortal(0));
                            NPCScriptManager.getInstance().start(chr.getClient(), "dojo_exit");
                        }
                    }
                }

                if (chr.getMapId() == 993174800 && chr.getChair() == 3015394) {
                    if (chr.getLastFishingTime() == 0) {
                        chr.setLastFishingTime(time);
                        chr.dropMessage(6, "낚시 시작");
                    }
                    if (time >= chr.getLastFishingTime() + 1000 * 30) {
                        Collections.shuffle(GameConstants.fishingItem);
                        boolean check = false;
                        for (Triple<Integer, Integer, Integer> item : GameConstants.fishingItem) {
                            if (Randomizer.isSuccess(item.right)) {
                                check = true;
                                chr.gainItem(item.left, item.mid);
                                break;
                            }
                        }
                        if (!check) {
                            chr.gainItem(3801268, 1); //아무것도 당첨된게없다면 고정된 아이템 지급
                        }
                        chr.setLastFishingTime(time);
                    }
                }

                if (chr.getKeyValue(12345, "AutoRoot") > 0) {
                    List<MapleMapObject> objs = chr.getMap().getItemsInRange(chr.getPosition(), Double.MAX_VALUE);

                    for (MapleMapObject ob : objs) {
                        MapleMapItem mapitem = (MapleMapItem) ob;
                        if (mapitem.getItem() != null && !MapleInventoryManipulator.checkSpace(chr.getClient(), mapitem.getItemId(), mapitem.getItem().getQuantity(), "")) {
                            continue;
                        }
                        if (!mapitem.isPlayerDrop()) {
                            InventoryHandler.pickupItem(ob, chr.getClient(), chr);
                        }
                    }
                }
            }
        }
    }
}
