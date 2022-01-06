package handling.auction.handler;

import client.MapleCharacter;
import client.MapleCharacterSave;
import client.MapleClient;
import client.inventory.*;
import constants.GameConstants;
import handling.auction.AuctionServer;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.CharacterTransfer;
import handling.world.MapleMessengerCharacter;
import handling.world.PlayerBuffStorage;
import handling.world.World;
import handling.world.World.Find;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.games.BattleReverse;
import server.games.OneCardGame;
import tools.StringUtil;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CField.AuctionPacket;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.InventoryPacket;
import tools.packet.PacketHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class AuctionHandler {

    public static void LeaveAuction(final MapleClient c, final MapleCharacter chr) {
        AuctionServer.getPlayerStorage().deregisterPlayer(chr);
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
            c.setAuction(false);
        }
    }

    public static void EnterAuction(MapleCharacter chr, final MapleClient client) {

        chr.changeRemoval();
        final ChannelServer ch = ChannelServer.getInstance(client.getChannel());
        if (chr.getMessenger() != null) {
            World.Messenger.silentLeaveMessenger(chr.getMessenger().getId(), new MapleMessengerCharacter(chr));
        }
        PlayerBuffStorage.addBuffsToStorage(chr.getId(), chr.getAllBuffs());
        PlayerBuffStorage.addCooldownsToStorage(chr.getId(), chr.getCooldowns());
        World.ChannelChange_Data(new CharacterTransfer(chr), chr.getId(), -20);
        ch.removePlayer(chr);
//		client.setChannel(channel);
        client.setAuction(true);
        client.updateLoginState(MapleClient.CHANGE_CHANNEL, client.getSessionIPAddress());
        final String s = client.getSessionIPAddress();
        LoginServer.addIPAuth(s.substring(s.indexOf('/') + 1, s.length()));
        client.getSession().writeAndFlush(CField.enterAuction(chr));
//		 client.getSession().writeAndFlush(CField.getChannelChange(client, Integer.parseInt(toch.getIP().split(":")[1])));
        new MapleCharacterSave(chr).saveToDB(chr, true, false);
        chr.getMap().removePlayer(chr);
//		 client.setPlayer(null);
//        client.setReceiving(false);

        if (OneCardGame.oneCardMatchingQueue.contains(chr)) {
            OneCardGame.oneCardMatchingQueue.remove(chr);
        }

        if (BattleReverse.BattleReverseMatchingQueue.contains(chr)) {
            BattleReverse.BattleReverseMatchingQueue.remove(chr);
        }

        client.getSession().writeAndFlush(CField.enterAuction(chr));
    }

    public static final void Handle(final LittleEndianAccessor slea, final MapleClient c) {
        int op = slea.readInt();
        Map<Integer, AuctionItem> items = AuctionServer.getItems();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        switch (op) {
            case 0: { // 입장

                CharacterTransfer transfer = AuctionServer.getPlayerStorage().getPendingCharacter(c.getPlayer().getId());
                MapleCharacter chr = MapleCharacter.ReconstructChr(transfer, c, false);

                c.setPlayer(chr);
                c.setAccID(chr.getAccountID());

                if (!c.CheckIPAddress()) { // Remote hack
                    c.getSession().close();
                    return;
                }

                World.isCharacterListConnected(c.getPlayer().getName(), c.loadCharacterNames(c.getWorld()));
                c.updateLoginState(MapleClient.LOGIN_LOGGEDIN, c.getSessionIPAddress());
                AuctionServer.getPlayerStorage().registerPlayer(chr);

                List<AuctionItem> completeItems = new ArrayList<>();
                List<AuctionItem> sellingItems = new ArrayList<>();
                List<AuctionItem> marketPriceItems = new ArrayList<>();
                List<AuctionItem> recentlySellItems = new ArrayList<>();
                List<Integer> wishlist = new ArrayList<>();
                List<AuctionItem> wishItems = new ArrayList<>();

                for (int i = 0; i < 10; i++) {
                    String wish = c.getKeyValue("wish" + i);
                    if (wish != null) {
                        wishlist.add((int) Integer.parseInt(wish));
                    }
                }

                for (Entry<Integer, AuctionItem> itemz : items.entrySet()) {
                    AuctionItem item = itemz.getValue();
                    if (item.getEndDate() < System.currentTimeMillis() || item.getState() >= 2) {
                        if ((item.getState() == 2 && item.getBidUserId() == c.getPlayer().getId()) || ((item.getState() == 3 || item.getState() == 4) && item.getCharacterId() == c.getPlayer().getId())) {
                            completeItems.add(item);
                        }
                    }

                    if (item.getCharacterId() == c.getPlayer().getId() && item.getState() == 0) {
                        sellingItems.add(item);
                    }

                    if (/*(System.currentTimeMillis() - item.getRegisterDate()) <= 60 * 30 * 1000 && */item.getState() == 0 && recentlySellItems.size() < 1000) {
                        //최근 30분 안에 등록한 아이템
                        recentlySellItems.add(item);
                    }

                    if ((item.getState() == 3 || item.getState() == 8) && marketPriceItems.size() < 1000) {
                        marketPriceItems.add(item);
                    }

                    for (int auctionId : wishlist) {
                        if (item.getAuctionId() == auctionId) {
                            wishItems.add(item);
                        }
                    }
                }

                c.getSession().writeAndFlush(AuctionPacket.AuctionCompleteItems(completeItems));
                c.getSession().writeAndFlush(AuctionPacket.AuctionSellingMyItems(sellingItems));
                c.getSession().writeAndFlush(AuctionPacket.AuctionWishlist(wishItems));
                c.getSession().writeAndFlush(AuctionPacket.AuctionOn());
                c.getSession().writeAndFlush(AuctionPacket.AuctionMarketPrice(marketPriceItems));
                c.getSession().writeAndFlush(AuctionPacket.AuctionSearchItems(recentlySellItems));
                break;
            }
            /*        	case 1: { // 퇴장
             LeaveAuction(c, c.getPlayer());
             break;
             }*/
            case 10: { // 아이템 등록
                int nAuctionType = slea.readInt();
                int nItemID = slea.readInt();
                int nNumber = slea.readInt();
                long nPrice = slea.readLong();
                //deleted nDirectPrice
                int nEndHour = slea.readInt();
                byte nTI = slea.readByte();
                int nItemPos = slea.readInt();

                Item source = c.getPlayer().getInventory(nTI).getItem((short) nItemPos);

                if (source == null || source.getItemId() != nItemID || source.getQuantity() < nNumber || nNumber < 0 || nPrice < 0) {
                    System.out.println(c.getPlayer().getName() + " 캐릭터가 경매장에 비정상적인 패킷을 유도함.");
                    c.getSession().close();
                    return;
                } else {
                    if (source.getInventoryId() <= 0) {
                        System.out.println("inventoryId : " + source.getInventoryId());
                        return;
                    }
                    final Item target = source.copy();

                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.getByType(nTI), (short) nItemPos, (short) nNumber, false);

                    target.setQuantity((short) nNumber);

                    if (target.getInventoryId() <= 0) {
                        System.out.println("inventoryId : " + target.getInventoryId());
                        return;
                    }

                    AuctionItem aItem = new AuctionItem();
                    aItem.setAuctionType(nAuctionType);
                    aItem.setItem(target);
                    aItem.setPrice(nPrice);
                    aItem.setDirectPrice(nPrice);
                    aItem.setEndDate(System.currentTimeMillis() + nEndHour * 60 * 60 * 1000);
                    aItem.setRegisterDate(System.currentTimeMillis());
                    aItem.setAccountId(c.getAccID());
                    aItem.setCharacterId(c.getPlayer().getId());
                    aItem.setState(0);
                    aItem.setWorldId(c.getWorld());
                    aItem.setName(c.getPlayer().getName());
                    aItem.setAuctionId(AuctionItemIdentifier.getInstance());
                    items.put(aItem.getAuctionId(), aItem);
                    c.getSession().writeAndFlush(AuctionPacket.AuctionSellItemUpdate(aItem));
                    c.getSession().writeAndFlush(AuctionPacket.AuctionSellItem(aItem));
                }
                break;
            }
            case 11: { // 재등록
                //리시브를 그대로 보냄
                long dwInventoryId = slea.readLong();
                int dwAuctionId = slea.readInt();
                int dwAccountId = slea.readInt();
                int dwCharacterId = slea.readInt();
                int nItemId = slea.readInt();
                int nState = slea.readInt();
                long nPrice = slea.readLong();
                long nBuyTime = slea.readLong();
                int deposit = slea.readInt();
                deposit = slea.readInt();
                int nCount = slea.readInt();
                int nWorldId = slea.readInt();

                if (nCount < 0 || nCount > Short.MAX_VALUE || nPrice < 0) {
                    System.out.println(c.getPlayer().getName() + " 캐릭터가 경매장에 비정상적인 패킷을 유도함.");
                    c.getSession().close();
                    return;
                } else {

                    AuctionItem aItem = items.get(dwAuctionId);
                    if (aItem != null && aItem.getItem() != null && aItem.getHistory() != null) {

                        AuctionHistory history = aItem.getHistory();

                        if (history.getId() != dwInventoryId) {
                            System.out.println("return 1");
                            return;
                        }

                        if (history.getAuctionId() != dwAuctionId) {
                            System.out.println("return 2");
                            return;
                        }

                        if (history.getAccountId() != dwAccountId) {
                            System.out.println("return 3");
                            return;
                        }

                        if (history.getCharacterId() != dwCharacterId) {
                            System.out.println("return 4");
                            return;
                        }

                        if (history.getItemId() != nItemId) {
                            System.out.println("return 5");
                            return;
                        }

                        if (history.getState() != nState) {
                            System.out.println("return 6");
                            return;
                        }

                        if (history.getPrice() != nPrice) {
                            System.out.println("return 7");
                            return;
                        }

                        if (PacketHelper.getTime(history.getBuyTime()) != nBuyTime) {
                            System.out.println("return 8");
                            return;
                        }

                        if (history.getDeposit() != deposit) {
                            System.out.println("return 9");
                            return;
                        }

                        if (history.getQuantity() != nCount) {
                            System.out.println("return 10");
                            return;
                        }

                        if (history.getWorldId() != nWorldId) {
                            System.out.println("return 11");
                            return;
                        }

                        aItem.setEndDate(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
                        aItem.setRegisterDate(System.currentTimeMillis());

                        aItem.setState(9);
                        history.setState(9);
                        Item item = aItem.getItem();
                        c.getSession().writeAndFlush(AuctionPacket.AuctionCompleteItemUpdate(aItem));

                        AuctionItem aItem2 = new AuctionItem();
                        aItem2.setAuctionType(aItem.getAuctionType());
                        aItem2.setItem(item);
                        aItem2.setPrice(aItem.getDirectPrice());
                        aItem2.setSecondPrice(0);
                        aItem2.setDirectPrice(aItem.getDirectPrice());
                        aItem2.setEndDate(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
                        aItem2.setRegisterDate(System.currentTimeMillis());
                        aItem2.setAccountId(c.getAccID());
                        aItem2.setCharacterId(c.getPlayer().getId());
                        aItem2.setState(0);
                        aItem2.setWorldId(c.getWorld());
                        aItem2.setName(c.getPlayer().getName());
                        aItem2.setAuctionId(AuctionItemIdentifier.getInstance());
                        items.put(aItem2.getAuctionId(), aItem2);

                        c.getSession().writeAndFlush(AuctionPacket.AuctionSellItemUpdate(aItem2));
                        c.getSession().writeAndFlush(AuctionPacket.AuctionReSellItem(aItem2));
                    }

                }
                break;
            }
            case 12: { // 판매 중지
                int dwAuctionID = slea.readInt();
                AuctionItem aItem = items.get(dwAuctionID);
                if (aItem != null && aItem.getItem() != null) {

                    if (aItem.getState() != 0) {
                        return;
                    }

                    aItem.setState(4);
                    aItem.setPrice(0);
                    aItem.setSecondPrice(-1);

                    AuctionHistory history = new AuctionHistory();

                    history.setAuctionId(aItem.getAuctionId());
                    history.setAccountId(aItem.getAccountId());
                    history.setCharacterId(aItem.getCharacterId());
                    history.setItemId(aItem.getItem().getItemId());
                    history.setState(aItem.getState());
                    history.setPrice(aItem.getPrice());
                    history.setBuyTime(System.currentTimeMillis());
                    history.setDeposit(aItem.getDeposit());
                    history.setQuantity(aItem.getItem().getQuantity());
                    history.setWorldId(aItem.getWorldId());
                    history.setId(AuctionHistoryIdentifier.getInstance());
                    aItem.setHistory(history);

                    c.getSession().writeAndFlush(AuctionPacket.AuctionSellItemUpdate(aItem));
                    c.getSession().writeAndFlush(AuctionPacket.AuctionCompleteItemUpdate(aItem));
                    c.getSession().writeAndFlush(AuctionPacket.AuctionStopSell(aItem));
                }
                break;
            }
            case 20: // 장비 아이템 구입
            case 21: { // 나머지 구입
                int dwAuctionID = slea.readInt();
                long nPrice = slea.readLong();
                int nCount = 1;
                if (op == 21) {
                    nCount = slea.readInt();
                }

                for (int i = 0; i < 10; i++) {
                    String wish = c.getKeyValue("wish" + i);
                    if (wish != null && wish.equals(String.valueOf(dwAuctionID))) {
                        c.removeKeyValue("wish" + i);
                        break;
                    }
                }

                c.getSession().writeAndFlush(AuctionPacket.AuctionWishlistUpdate(dwAuctionID));

                if (nPrice < 0 || nCount < 0 || nCount > Short.MAX_VALUE) {
                    System.out.println(c.getPlayer().getName() + " 캐릭터가 경매장에 비정상적인 패킷을 유도함.");
                    c.getSession().close();
                    return;
                } else if (c.getPlayer().getMeso() < nPrice) {
                    if (op == 20) {
                        c.getSession().writeAndFlush(AuctionPacket.AuctionBuyEquipResult(0x6A, dwAuctionID));
                    } else {
                        c.getSession().writeAndFlush(AuctionPacket.AuctionBuyItemResult(0x6A, dwAuctionID));
                    }
                } else {
                    AuctionItem item = items.get(dwAuctionID);
                    if (op == 20 && item.getItem() != null) {
                        nCount = item.getItem().getQuantity();
                    }
                    if (item != null && item.getItem() != null && item.getItem().getQuantity() >= nCount) {

                        if (item.getCharacterId() == c.getPlayer().getId() || item.getState() != 0) {
                            return;
                        }

                        //quantity 안곱해줘도 되나?
                        c.getPlayer().gainMeso(-nPrice, false);

                        Item source = item.getItem();

                        final Item target = source.copy();
                        source.setQuantity((short) (source.getQuantity() - nCount));
                        target.setQuantity((short) nCount);

                        if (source.getQuantity() <= 0) {
                            source.setQuantity((short) nCount);
                            //통구매의 경우 올려진 아이템을 메소 회수를 위한 데이터로 변경
                            item.setState(3);
                            item.setBidUserId(c.getPlayer().getId());
                            item.setBidUserName(c.getPlayer().getName());
                            item.setPrice(nPrice);

                            AuctionHistory history = new AuctionHistory();

                            history.setAuctionId(item.getAuctionId());
                            history.setAccountId(item.getAccountId());
                            history.setCharacterId(item.getCharacterId());
                            history.setItemId(item.getItem().getItemId());
                            history.setState(item.getState());
                            history.setPrice(item.getPrice());
                            history.setBuyTime(System.currentTimeMillis());
                            history.setDeposit(item.getDeposit());
                            history.setQuantity(item.getItem().getQuantity());
                            history.setWorldId(item.getWorldId());
                            history.setId(AuctionHistoryIdentifier.getInstance());
                            item.setHistory(history);

                            c.getSession().writeAndFlush(AuctionPacket.AuctionBuyItemUpdate(item, false));
                        } else {

                            c.getSession().writeAndFlush(AuctionPacket.AuctionBuyItemUpdate(item, true));

                            //통구매가 아니면 메소 회수를 위한 데이터를 새로 생성
                            AuctionItem aItem = new AuctionItem();
                            aItem.setAuctionType(item.getAuctionType());
                            aItem.setItem(target);
                            aItem.setPrice(nPrice);
                            aItem.setDirectPrice(nPrice);
                            aItem.setEndDate(item.getEndDate());
                            aItem.setRegisterDate(item.getRegisterDate());
                            aItem.setAccountId(item.getAccountId());
                            aItem.setCharacterId(item.getCharacterId());
                            aItem.setState(3);
                            aItem.setWorldId(item.getWorldId());
                            aItem.setName(item.getName());
                            aItem.setBidUserId(c.getPlayer().getId());
                            aItem.setBidUserName(c.getPlayer().getName());

                            aItem.setAuctionId(AuctionItemIdentifier.getInstance());

                            //history 생성
                            AuctionHistory history = new AuctionHistory();

                            history.setAuctionId(aItem.getAuctionId());
                            history.setAccountId(aItem.getAccountId());
                            history.setCharacterId(aItem.getCharacterId());
                            history.setItemId(aItem.getItem().getItemId());
                            history.setState(aItem.getState());
                            history.setPrice(aItem.getPrice());
                            history.setBuyTime(System.currentTimeMillis());
                            history.setDeposit(aItem.getDeposit());
                            history.setQuantity(aItem.getItem().getQuantity());
                            history.setWorldId(aItem.getWorldId());
                            history.setId(AuctionHistoryIdentifier.getInstance());
                            aItem.setHistory(history);

                            items.put(aItem.getAuctionId(), aItem);
                        }

                        //구매자의 아이템 회수를 위한 데이터
                        AuctionItem aItem = new AuctionItem();
                        aItem.setAuctionType(item.getAuctionType());
                        aItem.setItem(target);
                        aItem.setPrice(nPrice);
                        aItem.setDirectPrice(item.getDirectPrice());
                        aItem.setEndDate(item.getEndDate());
                        aItem.setRegisterDate(item.getRegisterDate());
                        aItem.setAccountId(item.getAccountId());
                        aItem.setCharacterId(item.getCharacterId());
                        aItem.setState(2);
                        aItem.setWorldId(item.getWorldId());
                        aItem.setName(item.getName());
                        aItem.setBidUserId(c.getPlayer().getId());
                        aItem.setBidUserName(c.getPlayer().getName());

                        aItem.setAuctionId(AuctionItemIdentifier.getInstance());

                        //history 생성
                        AuctionHistory history = new AuctionHistory();

                        history.setAuctionId(aItem.getAuctionId());
                        history.setAccountId(aItem.getAccountId());
                        history.setCharacterId(aItem.getCharacterId());
                        history.setItemId(aItem.getItem().getItemId());
                        history.setState(aItem.getState());
                        history.setPrice(aItem.getPrice());
                        history.setBuyTime(System.currentTimeMillis());
                        history.setDeposit(aItem.getDeposit());
                        history.setQuantity(aItem.getItem().getQuantity());
                        history.setWorldId(aItem.getWorldId());
                        history.setId(AuctionHistoryIdentifier.getInstance());
                        aItem.setHistory(history);

                        items.put(aItem.getAuctionId(), aItem);

                        c.getSession().writeAndFlush(AuctionPacket.AuctionCompleteItemUpdate(aItem, target));
                        c.getSession().writeAndFlush(AuctionPacket.AuctionBuyItemResult(0, dwAuctionID));

                        int ch = Find.findAccChannel(item.getAccountId());
                        if (ch >= 0) {
                            MapleClient ac = AuctionServer.getPlayerStorage().getClientById(item.getAccountId());
                            if (ac == null) {
                                ac = ChannelServer.getInstance(ch).getPlayerStorage().getClientById(item.getAccountId());
                            }
                            if (ac != null) {
                                ac.getSession().writeAndFlush(CWvsContext.AlarmAuction(ac.getPlayer(), item));
                            }
                        }
                        break;
                    }
                }
                break;
            }
            case 30: { // 대금 수령
                long dwInventoryId = slea.readLong();
                int dwAuctionId = slea.readInt();
                int dwAccountId = slea.readInt();
                int dwCharacterId = slea.readInt();
                int nItemId = slea.readInt();
                int nState = slea.readInt();
                long nPrice = slea.readLong();
                long nBuyTime = slea.readLong();
                int deposit = slea.readInt();
                deposit = slea.readInt();
                int nCount = slea.readInt();
                int nWorldId = slea.readInt();

                if (nCount < 0 || nCount > Short.MAX_VALUE || nPrice < 0) {
                    System.out.println(c.getPlayer().getName() + " 캐릭터가 경매장에 비정상적인 패킷을 유도함.");
                    c.getSession().close();
                    return;
                } else {
                    AuctionItem item = items.get(dwAuctionId);

                    if (item != null && item.getItem() != null && item.getHistory() != null) {
                        AuctionHistory history = item.getHistory();

                        if (history.getId() != dwInventoryId) {
                            System.out.println("return 1");
                            return;
                        }

                        if (history.getAuctionId() != dwAuctionId) {
                            System.out.println("return 2");
                            return;
                        }

                        if (history.getAccountId() != dwAccountId) {
                            System.out.println("return 3");
                            return;
                        }

                        if (history.getCharacterId() != dwCharacterId) {
                            System.out.println("return 4");
                            return;
                        }

                        if (history.getItemId() != nItemId) {
                            System.out.println("return 5");
                            return;
                        }

                        if (history.getState() != nState) {
                            System.out.println("return 6");
                            return;
                        }

                        if (history.getPrice() != nPrice) {
                            System.out.println("return 7");
                            return;
                        }

                        if (PacketHelper.getTime(history.getBuyTime()) != nBuyTime) {
                            System.out.println("return 8");
                            return;
                        }

                        if (history.getDeposit() != deposit) {
                            System.out.println("return 9");
                            return;
                        }

                        if (history.getQuantity() != nCount) {
                            System.out.println("return 10");
                            return;
                        }

                        if (history.getWorldId() != nWorldId) {
                            System.out.println("return 11");
                            return;
                        }

                        history.setState(8);
                        item.setState(8);
                        c.getPlayer().gainMeso((long) (nPrice * 0.95), false);
                        c.getSession().writeAndFlush(AuctionPacket.AuctionCompleteItemUpdate(item));
                        c.getSession().writeAndFlush(AuctionPacket.AuctionCompleteMesoResult());
                        break;
                    }

                }
                break;
            }
            case 31: { // 물품 반환
                //리시브를 그대로 보냄
                long dwInventoryId = slea.readLong();
                int dwAuctionId = slea.readInt();
                int dwAccountId = slea.readInt();
                int dwCharacterId = slea.readInt();
                int nItemId = slea.readInt();
                int nState = slea.readInt();
                long nPrice = slea.readLong();
                long nBuyTime = slea.readLong();
                int deposit = slea.readInt();
                deposit = slea.readInt();
                int nCount = slea.readInt();
                int nWorldId = slea.readInt();

                if (nCount < 0 || nCount > Short.MAX_VALUE || nPrice < 0) {
                    System.out.println(c.getPlayer().getName() + " 캐릭터가 경매장에 비정상적인 패킷을 유도함.");
                    c.getSession().close();
                    return;
                } else {
                    AuctionItem item = items.get(dwAuctionId);

                    if (item != null && item.getItem() != null && item.getHistory() != null) {
                        Item it = item.getItem().copy();
                        AuctionHistory history = item.getHistory();

                        if (history.getId() != dwInventoryId) {
                            System.out.println("return 1");
                            return;
                        }

                        if (history.getAuctionId() != dwAuctionId) {
                            System.out.println("return 2");
                            return;
                        }

                        if (history.getAccountId() != dwAccountId) {
                            System.out.println("return 3");
                            return;
                        }

                        if (history.getCharacterId() != dwCharacterId) {
                            System.out.println("return 4");
                            return;
                        }

                        if (history.getItemId() != nItemId) {
                            System.out.println("return 5");
                            return;
                        }

                        if (history.getState() != nState) {
                            System.out.println("return 6");
                            return;
                        }

                        if (history.getPrice() != nPrice) {
                            System.out.println("return 7");
                            return;
                        }

                        if (PacketHelper.getTime(history.getBuyTime()) != nBuyTime) {
                            System.out.println("return 8");
                            return;
                        }

                        if (history.getDeposit() != deposit) {
                            System.out.println("return 9");
                            return;
                        }

                        if (history.getQuantity() != nCount) {
                            System.out.println("return 10");
                            return;
                        }

                        if (history.getWorldId() != nWorldId) {
                            System.out.println("return 11");
                            return;
                        }

                        if (c.getPlayer().getId() != dwCharacterId) {
                            if (ItemFlag.KARMA_EQUIP.check(it.getFlag())) {
                                it.setFlag((it.getFlag() - ItemFlag.KARMA_EQUIP.getValue()));
                            } else if (ItemFlag.KARMA_USE.check(it.getFlag())) {
                                it.setFlag((it.getFlag() - ItemFlag.KARMA_USE.getValue()));
                            }
                        }

                        short slot = c.getPlayer().getInventory(GameConstants.getInventoryType(nItemId)).addItem(it);
                        if (slot >= 0) {
                            item.setState(item.getState() + 5);
                            history.setState(history.getState() + 5);
                            it.setGMLog(new StringBuilder().append(StringUtil.getAllCurrentTime()).append("에 ").append("경매장에서 얻은 " + dwCharacterId + "의 아이템.").toString());
                            c.getSession().writeAndFlush(InventoryPacket.addInventorySlot(GameConstants.getInventoryType(nItemId), it));
                            c.getSession().writeAndFlush(AuctionPacket.AuctionCompleteItemUpdate(item));
                            c.getSession().writeAndFlush(AuctionPacket.AuctionCompleteItemResult());
                        }
                        break;
                    }
                }
                break;
            }
            case 40: // 일반 검색
            case 41: { // 시세 검색
                List<AuctionItem> searchItems = new ArrayList<>();

                slea.skip(1); // 0
                int searchType = slea.readInt(); // 방어구, 무기, 소비, 캐시, 기타
                //  slea.skip(4);
                String nameWithSpace = slea.readMapleAsciiString(); // 띄어쓰기 없는 이름
                String nameWithoutSpace = slea.readMapleAsciiString(); // 띄어쓰기 있는 이름

                if (searchType == -1) {
                    for (AuctionItem item : items.values()) {
                        String name = ii.getName(item.getItem().getItemId());
                        if (name != null && (name.contains(nameWithSpace) || name.contains(nameWithoutSpace))) {
                            if ((op == 40 && item.getState() == 0) || (op == 41 && (item.getState() == 3 || item.getState() == 8))) {
                                searchItems.add(item);
                            }
                        }
                    }
                } else {
                    int itemType = slea.readInt(); // 탭
                    int itemSemiType = slea.readInt(); // 부위
                    int lvMin = slea.readInt();
                    int lvMax = slea.readInt();
                    long priceMin = slea.readLong();
                    long priceMax = slea.readLong();
                    int potentialType = slea.readInt();
                    boolean and = slea.readByte() == 1;

                    int optionalSearchCount = slea.readInt();

                    for (int i = 0; i < optionalSearchCount; ++i) {
                        boolean isStarForce = slea.readInt() == 1;
                        int optionType = slea.readInt();
                        int optionValueMin = slea.readInt();
                    }

                    if (searchType <= 1) {
                        for (AuctionItem item : items.values()) {
                            if (item.getItem() != null && item.getItem().getType() == 1) {
                                Equip equip = (Equip) item.getItem();

                                int level = ii.getReqLevel(item.getItem().getItemId());

                                boolean lvLimit = level >= lvMin && level <= lvMax;
                                boolean priceLimit = item.getPrice() >= priceMin && item.getPrice() <= priceMax;
                                boolean potentialLimit = potentialType == -1 || (potentialType == 0 && equip.getState() == 0) || (potentialType > 0 && equip.getState() - 16 == potentialType);
                                boolean typeLimit = typeLimit(searchType, itemType, itemSemiType, equip.getItemId());

                                String name = ii.getName(item.getItem().getItemId());
                                if (typeLimit && lvLimit && priceLimit && potentialLimit && (name.contains(nameWithSpace) || name.contains(nameWithoutSpace) || nameWithoutSpace.isEmpty())) {
                                    if (equipOptionTypes() && ((op == 40 && item.getState() == 0) || (op == 41 && (item.getState() == 3 || item.getState() == 8)))) {
                                        searchItems.add(item);
                                    }
                                }
                            }
                        }
                    } else {
                        for (AuctionItem item : items.values()) {
                            int level = ii.getReqLevel(item.getItem().getItemId());
                            boolean lvLimit = level >= lvMin && level <= lvMax;
                            boolean priceLimit = item.getPrice() >= priceMin && item.getPrice() <= priceMax;
                            boolean typeLimit = typeLimit(searchType, itemType, itemSemiType, item.getItem().getItemId());

                            String name = ii.getName(item.getItem().getItemId());
                            if (typeLimit && lvLimit && priceLimit && (name.contains(nameWithSpace) || name.contains(nameWithoutSpace) || nameWithoutSpace.isEmpty())) {
                                if ((op == 40 && item.getState() == 0) || (op == 41 && (item.getState() == 3 || item.getState() == 8))) {
                                    searchItems.add(item);
                                }
                            }
                        }
                    }
                }

                if (op == 40) {
                    c.getSession().writeAndFlush(AuctionPacket.AuctionSearchItems(searchItems));
                } else {
                    c.getSession().writeAndFlush(AuctionPacket.AuctionMarketPrice(searchItems));
                }

                break;
            }
            case 45: { // 찜 등록
                int dwAuctionId = slea.readInt();

                AuctionItem item = items.get(dwAuctionId);
                if (item != null) {
                    for (int i = 0; i < 10; i++) {
                        if (c.getKeyValue("wish" + i) == null) {
                            c.setKeyValue("wish" + i, String.valueOf(dwAuctionId));
                            c.getSession().writeAndFlush(AuctionPacket.AuctionAddWishlist(item));
                            c.getSession().writeAndFlush(AuctionPacket.AuctionWishlistResult(item));
                            break;
                        }
                    }
                }
                break;
            }
            case 46: { // 찜 목록
                List<Integer> wishlist = new ArrayList<>();
                List<AuctionItem> wishItems = new ArrayList<>();

                for (int i = 0; i < 10; i++) {
                    String wish = c.getKeyValue("wish" + i);
                    if (wish != null) {
                        wishlist.add((int) Integer.parseInt(wish));
                    }
                }

                for (int dwAuctionId : wishlist) {
                    AuctionItem item = items.get(dwAuctionId);
                    if (item != null) {
                        wishItems.add(item);
                    }
                }

                c.getSession().writeAndFlush(AuctionPacket.AuctionWishlist(wishItems));
                break;
            }
            case 47: { // 찜 삭제
                int dwAuctionId = slea.readInt();

                AuctionItem item = items.get(dwAuctionId);
                if (item != null) {
                    for (int i = 0; i < 10; i++) {
                        if (c.getKeyValue("wish" + i).equals(String.valueOf(dwAuctionId))) {
                            c.removeKeyValue("wish" + i);
                            c.getSession().writeAndFlush(AuctionPacket.AuctionWishlistUpdate(dwAuctionId));
                            c.getSession().writeAndFlush(AuctionPacket.AuctionWishlistDeleteResult(dwAuctionId));
                            break;
                        }
                    }
                }
                break;
            }
            case 50: { // 팔고있는 아이템
                List<AuctionItem> sellingItems = new ArrayList<>();

                for (AuctionItem item : items.values()) {
                    if (item.getAccountId() == c.getAccID() && item.getState() == 0) {
                        sellingItems.add(item);
                    }
                }

                c.getSession().writeAndFlush(AuctionPacket.AuctionSellingMyItems(sellingItems));
                break;
            }
            case 51: { // 아이템 구매/판매 현황
                List<AuctionItem> completeItems = new ArrayList<>();

                for (AuctionItem item : items.values()) {
                    if (((item.getState() == 2 || item.getState() == 7) && item.getBidUserId() == c.getPlayer().getId()) || (item.getState() != 7 && item.getState() >= 3 && item.getAccountId() == c.getAccID())) {
                        completeItems.add(item);
                    }
                }

                c.getSession().writeAndFlush(AuctionPacket.AuctionCompleteItems(completeItems));
                break;
            }
        }
    }

    private static boolean equipOptionTypes() {
        // TODO Auto-generated method stub
        return true;
    }

    private static boolean typeLimit(int searchType, int itemType, int itemSemiType, int itemId) {
        switch (searchType) {
            case 0: {
                switch (itemType) {
                    case 0: {
                        return !GameConstants.isWeapon(itemId);
                    }
                    case 1: {
                        switch (itemSemiType) {
                            case 0:
                                return GameConstants.isWeapon(itemId) || GameConstants.isAccessory(itemId);
                            case 1:
                                return itemId / 1000 == 100;
                            case 2:
                                return itemId / 1000 == 104;
                            case 3:
                                return itemId / 1000 == 105;
                            case 4:
                                return itemId / 1000 == 106;
                            case 5:
                                return itemId / 1000 == 107;
                            case 6:
                                return itemId / 1000 == 108;
                            case 7:
                                return itemId / 1000 == 109;
                            case 8:
                                return itemId / 1000 == 110;
                        }
                        break;
                    }
                    case 2: {
                        switch (itemSemiType) {
                            case 0:
                                return !GameConstants.isAccessory(itemId);
                            case 1:
                                return itemId / 1000 == 1012;
                            case 2:
                                return itemId / 1000 == 1022;
                            case 3:
                                return itemId / 1000 == 1032;
                            case 4:
                                return GameConstants.isRing(itemId);
                            case 5:
                                return itemId / 1000 == 1122 || itemId / 1000 == 1123;
                            case 6:
                                return itemId / 1000 == 1132;
                            case 7:
                                return GameConstants.isMedal(itemId);
                            case 8:
                                return itemId / 1000 == 1152;
                            case 9:
                                return itemId / 1000 == 1162;
                            case 10:
                                return itemId / 1000 == 1182;
                        }
                        break;
                    }
                    case 3: {
                        switch (itemSemiType) {
                            case 0:
                                return itemId / 1000 >= 1612 && itemId / 1000 <= 1652;
                            case 1:
                                return itemId / 1000 == 1662;
                            case 2:
                                return itemId / 1000 == 1672;
                            case 3:
                                return itemId / 1000 >= 1942 && itemId / 1000 <= 1972;
                        }
                        break;
                    }
                }
            }
            case 1: {
                switch (itemType) {
                    case 0: {
                        return GameConstants.isWeapon(itemId);
                    }
                    case 1: {
                        switch (itemSemiType) {
                            case 0:
                                return !GameConstants.isTwoHanded(itemId);
                            case 1:
                                return itemId / 1000 == 1212;
                            case 2:
                                return itemId / 1000 == 1222;
                            case 3:
                                return itemId / 1000 == 1232;
                            case 4:
                                return itemId / 1000 == 1242;
                            case 5:
                                return itemId / 1000 == 1302;
                            case 6:
                                return itemId / 1000 == 1312;
                            case 7:
                                return itemId / 1000 == 1322;
                            case 8:
                                return itemId / 1000 == 1332;
                            case 9:
                                return itemId / 1000 == 1342;
                            case 10:
                                return itemId / 1000 == 1362;
                            case 11:
                                return itemId / 1000 == 1372;
                            case 12:
                                return itemId / 1000 == 1262;
                            case 13:
                                return itemId / 1000 == 1272;
                            case 14:
                                return itemId / 1000 == 1282;
                            case 15:
                                return itemId / 1000 == 1292;
                            case 16:
                                return itemId / 1000 == 1213;
                        }
                        break;
                    }
                    case 2: {
                        switch (itemSemiType) {
                            case 0:
                                return GameConstants.isTwoHanded(itemId);
                            case 1:
                                return itemId / 1000 == 1402;
                            case 2:
                                return itemId / 1000 == 1412;
                            case 3:
                                return itemId / 1000 == 1422;
                            case 4:
                                return itemId / 1000 == 1432;
                            case 5:
                                return itemId / 1000 == 1442;
                            case 6:
                                return itemId / 1000 == 1452;
                            case 7:
                                return itemId / 1000 == 1462;
                            case 8:
                                return itemId / 1000 == 1472;
                            case 9:
                                return itemId / 1000 == 1482;
                            case 10:
                                return itemId / 1000 == 1492;
                            case 11:
                                return itemId / 1000 == 1522;
                            case 12:
                                return itemId / 1000 == 1532;
                            case 13:
                                return itemId / 1000 == 1582;
                            case 14:
                                return itemId / 1000 == 1592;
                        }
                        break;
                    }
                    case 3: {
                        switch (itemSemiType) {
                            case 0:
                                return itemId / 1000 == 1352 || itemId / 1000 == 1353;
                            case 1:
                                return itemId / 10 == 135220;
                            case 2:
                                return itemId / 10 == 135221;
                            case 3:
                                return itemId / 10 == 135222;
                            case 4:
                                return itemId / 10 == 135223 || itemId / 10 == 135224 || itemId / 10 == 135225;
                            case 5:
                                return itemId / 10 == 135226;
                            case 6:
                                return itemId / 10 == 135227;
                            case 7:
                                return itemId / 10 == 135228;
                            case 8:
                                return itemId / 10 == 135229;
                            case 9:
                                return itemId / 10 == 135290;
                            case 10:
                                return itemId / 10 == 135291;
                            case 11:
                                return itemId / 10 == 135292;
                            case 12:
                                return itemId / 10 == 135297;
                            case 13:
                                return itemId / 10 == 135293;
                            case 14:
                                return itemId / 10 == 135294;
                            case 15:
                                return itemId / 10 == 135240;
                            case 16:
                                return itemId / 10 == 135201;
                            case 17:
                                return itemId / 10 == 135210;
                            case 18:
                                return itemId / 10 == 135310;
                            case 19:
                                return itemId / 10 == 135295;
                            case 20:
                                return itemId / 10 == 135296;
                            case 21:
                                return itemId / 10 == 135300;
                            case 22:
                                return itemId / 10 == 135270;
                            case 23:
                                return itemId / 10 == 135250;
                            case 24:
                                return itemId / 10 == 135260;
                            case 25:
                                return itemId / 10 == 135320;
                            case 26:
                                return itemId / 10 == 135340;
                            case 27:
                                return itemId / 10 == 135330;
                            case 28:
                                return itemId / 10 == 135350;
                            case 29:
                                return itemId / 10 == 135360;
                            case 30:
                                return itemId / 10 == 135370;
                            case 31:
                                return itemId / 10 == 135380;
                            case 32:
                                return itemId / 10 == 135390;
                        }
                        break;
                    }
                }
                break;
            }
            case 2: {
                return itemId / 1000000 == 2;
            }
            case 3: {
                return MapleItemInformationProvider.getInstance().isCash(itemId);
            }
            case 4: {
                return itemId / 1000000 == 4 || itemId / 1000000 == 3;
            }
        }
        return false;
    }
}
