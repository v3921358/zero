/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package handling.channel.handler;

import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.ItemFlag;
import client.inventory.ItemLoader;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.world.World;
import server.MapleDueyActions;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CWvsContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

public class DueyHandler {

    /*
     * 19 = Successful
     * 18 = One-of-a-kind Item is already in Reciever's delivery
     * 17 = The Character is unable to recieve the parcel
     * 15 = Same account
     * 14 = Name does not exist
     */
    public static final void DueyOperation(final LittleEndianAccessor slea, final MapleClient c) {

        final byte operation = slea.readByte();

        switch (operation) {
            case 1: { // Start Duey, 13 digit AS
                final String secondPassword = slea.readMapleAsciiString();
                if (c.CheckSecondPassword(secondPassword)) {
                    slea.skip(4);
                    final int conv = c.getPlayer().getConversation();
                    if (conv == 2) { // Duey
                        List<MapleDueyActions> list1 = new ArrayList<MapleDueyActions>();
                        List<MapleDueyActions> list2 = new ArrayList<MapleDueyActions>();
                        for (MapleDueyActions dp : loadItems((c.getPlayer()))) {
                            if (dp.isExpire()) {
                                list2.add(dp);
                            } else {
                                list1.add(dp);
                            }
                        }
                        c.getSession().writeAndFlush(CField.sendDuey((byte) 10, list1, list2));
                        for (MapleDueyActions dp : list2) {
                            removeItemFromDB(dp.getPackageId(), c.getPlayer().getId());
                        }
                    }
                } else {
                    c.getSession().writeAndFlush(CField.checkFailedDuey());
                }
                break;
            }
            case 3: { // Send Item
                if (c.getPlayer().getConversation() != 2) {
                    return;
                }
                if (!c.getPlayer().isGM()) {
                    c.getPlayer().dropMessage(1, "현재 아이템 및 메소 수령만 가능합니다.");
                    return;
                }
                final byte inventId = slea.readByte();
                final short itemPos = slea.readShort();
                short amount = slea.readShort();
                final int mesos = slea.readInt();
                final String recipient = slea.readMapleAsciiString();
                boolean quickdelivery = slea.readByte() > 0;
                String letter = "";
                int qq = 0;
                if (quickdelivery) {
                    /*                    if (!c.getPlayer().haveItem(5330000, 1) && !c.getPlayer().haveItem(5330001, 1)) {
                     return;
                     }*/
                    letter = slea.readMapleAsciiString();
                    qq = slea.readInt();
                }

                final long finalcost = mesos + GameConstants.getTaxAmount(mesos) + (quickdelivery ? 0 : 5000);

                if (mesos >= 0 && mesos <= 100000000 && c.getPlayer().getMeso() >= finalcost) {
                    final int accid = MapleCharacterUtil.getAccByName(recipient);
                    final int cid = MapleCharacterUtil.getIdByName(recipient);
                    if (accid != -1) {
                        if (accid != c.getAccID()) {

                            boolean recipientOn = false;
                            MapleClient rClient = null;
                            int channel = World.Find.findChannel(recipient);
                            if (channel > -1) {
                                recipientOn = true;
                                ChannelServer rcserv = ChannelServer.getInstance(channel);
                                rClient = rcserv.getPlayerStorage().getCharacterByName(recipient).getClient();
                            }

                            if (inventId > 0) {
                                final MapleInventoryType inv = MapleInventoryType.getByType(inventId);
                                final Item item = c.getPlayer().getInventory(inv).getItem((short) itemPos);
                                if (item == null) {
                                    c.getSession().writeAndFlush(CField.sendDuey((byte) 17, null, null)); // Unsuccessfull
                                    return;
                                }
                                List<MapleDueyActions> dps = loadItems(c.getPlayer());
                                for (MapleDueyActions mda : dps) {
                                    if (mda.getItem() != null) {
                                        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                                        if (ii.isPickupRestricted(mda.getItem().getItemId()) && mda.getItem().getItemId() == item.getItemId()) {
                                            c.getSession().writeAndFlush(CField.sendDuey((byte) 18, null, null)); // 怨좎쑀?꾩씠?쒖쓣 諛쏅뒗?щ엺??媛뽮퀬?덉뒿?덈떎.
                                            return;
                                        }
                                    }
                                }
                                final int flag = item.getFlag();
                                if (ItemFlag.UNTRADEABLE.check(flag) || ItemFlag.LOCK.check(flag)) {
                                    c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                                    return;
                                }
                                if (GameConstants.isThrowingStar(item.getItemId()) || GameConstants.isBullet(item.getItemId())) {
                                    if (item.getQuantity() == 0) {
                                        amount = 0;
                                    }
                                }
                                if (c.getPlayer().getItemQuantity(item.getItemId(), false) >= amount) {
                                    final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                                    if (!ii.isDropRestricted(item.getItemId()) && !ii.isAccountShared(item.getItemId())) {
                                        Item toSend = item.copy();
                                        if (!GameConstants.isThrowingStar(toSend.getItemId()) && !GameConstants.isBullet(toSend.getItemId())) {
                                            toSend.setQuantity(amount);
                                        }
                                        if (addItemToDB(toSend, mesos, c.getPlayer().getName(), cid, recipientOn, letter, qq, quickdelivery)) {
                                            if (GameConstants.isThrowingStar(toSend.getItemId()) || GameConstants.isBullet(toSend.getItemId())) {
                                                MapleInventoryManipulator.removeFromSlot(c, inv, (byte) itemPos, toSend.getQuantity(), true, false);
                                            } else {
                                                MapleInventoryManipulator.removeFromSlot(c, inv, (byte) itemPos, amount, true, false);
                                            }
                                            /*                                            if (quickdelivery) {
                                             if (c.getPlayer().haveItem(5330001, 1)) {
                                             MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, 5330001, 1, false, false);
                                             } else if (c.getPlayer().haveItem(5330000, 1)) {
                                             MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, 5330000, 1, false, false);
                                             }
                                             }
                                             c.getPlayer().gainMeso(-finalcost, false);*/
                                            c.getSession().writeAndFlush(CField.sendDuey((byte) 19, null, null)); // Successfull
                                        } else {
                                            c.getSession().writeAndFlush(CField.sendDuey((byte) 17, null, null)); // Unsuccessful
                                        }
                                    } else {
                                        c.getSession().writeAndFlush(CField.sendDuey((byte) 17, null, null)); // Unsuccessfull
                                    }
                                } else {
                                    c.getSession().writeAndFlush(CField.sendDuey((byte) 17, null, null)); // Unsuccessfull
                                }
                            } else {
                                if (addMesoToDB(mesos, c.getPlayer().getName(), cid, recipientOn, letter, quickdelivery)) {
                                    /*                                    c.getPlayer().gainMeso(-finalcost, false);
                                     if (quickdelivery) {
                                     if (c.getPlayer().haveItem(5330001, 1)) {
                                     MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, 5330001, 1, false, false);
                                     } else if (c.getPlayer().haveItem(5330000, 1)) {
                                     MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, 5330000, 1, false, false);
                                     }
                                     }*/
                                    c.getSession().writeAndFlush(CField.sendDuey((byte) 19, null, null)); // ?깃났?곸쑝濡?諛쒖넚?섏??듬땲??
                                } else {
                                    c.getSession().writeAndFlush(CField.sendDuey((byte) 17, null, null)); // Unsuccessfull
                                }
                            }
                            if (recipientOn && rClient != null) {
                                if (quickdelivery) {
                                    rClient.getSession().writeAndFlush(CField.receiveParcel(c.getPlayer().getName(), quickdelivery));
                                }
                            }
                        } else {
                            c.getSession().writeAndFlush(CField.sendDuey((byte) 15, null, null));
                        }
                    } else {
                        c.getSession().writeAndFlush(CField.sendDuey((byte) 14, null, null));
                    }
                } else {
                    c.getSession().writeAndFlush(CField.sendDuey((byte) 12, null, null));
                }
                break;
            }
            case 5: { // Recieve Package
                if (c.getPlayer().getConversation() != 2) {
                    return;
                }
                final int packageid = slea.readInt();
                //System.out.println("Item attempted : " + packageid);
                final MapleDueyActions dp = loadSingleItem(packageid, c.getPlayer().getId());
                if (dp == null) {
                    return;
                }
                if (dp.isExpire() || !dp.canReceive()) { //packet edit;
                    return;
                }
                if (dp.getItem() != null && !MapleInventoryManipulator.checkSpace(c, dp.getItem().getItemId(), dp.getItem().getQuantity(), dp.getItem().getOwner())) {
                    c.getSession().writeAndFlush(CField.sendDuey((byte) 16, null, null));
                    return;
                } else if (dp.getMesos() < 0 || (dp.getMesos() + c.getPlayer().getMeso()) < 0) {
                    c.getSession().writeAndFlush(CField.sendDuey((byte) 17, null, null));
                    return;
                }
                if (dp.getItem() != null) {
                    if (c.getPlayer().haveItem(dp.getItem().getItemId(), 1, true, true) && MapleItemInformationProvider.getInstance().isPickupRestricted(dp.getItem().getItemId())) {
                        c.getSession().writeAndFlush(CField.sendDuey((byte) 18, null, null));
                        return;
                    }
                }
                removeItemFromDB(packageid, c.getPlayer().getId()); // Remove first
                //System.out.println("Item removed : " + packageid);
                if (dp.getItem() != null && dp.getItem().getQuantity() > 0) {
                    MapleInventoryManipulator.addbyItem(c, dp.getItem(), false);
                }
                if (dp.getMesos() != 0) {
                    c.getPlayer().gainMeso(dp.getMesos(), false);
                }
                c.getSession().writeAndFlush(CField.removeItemFromDuey(false, packageid));
                break;
            }
            case 6: { // Remove package
                if (c.getPlayer().getConversation() != 2) {
                    return;
                }
                final int packageid = slea.readInt();
                removeItemFromDB(packageid, c.getPlayer().getId());
                c.getSession().writeAndFlush(CField.removeItemFromDuey(true, packageid));
                break;
            }
            case 8: { // Close Duey
                c.getPlayer().setConversation(0);
                break;
            }
            default: {
                System.out.println("Unhandled Duey operation : " + slea.toString());
                break;
            }
        }

    }

    private static final boolean addMesoToDB(final int mesos, final String sName, final int recipientID, final boolean isOn, String content, boolean quick) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO dueypackages (RecieverId, SenderName, Mesos, TimeStamp, Checked, Type, `Quick`, content) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, recipientID);
            ps.setString(2, sName);
            ps.setInt(3, mesos);
            ps.setLong(4, System.currentTimeMillis());
            ps.setInt(5, isOn ? 0 : 1);
            ps.setInt(6, 3);
            ps.setInt(7, quick ? 1 : 0);
            ps.setString(8, content);

            ps.executeUpdate();
            ps.close();

            return true;
        } catch (SQLException se) {
            se.printStackTrace();
            return false;
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    public static final boolean addItemToDB(final Item item, final int mesos, final String sName, final int recipientID, final boolean isOn, String content, int qq, boolean Quick) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO dueypackages (RecieverId, SenderName, Mesos, TimeStamp, Checked, Type, content, `Quick`) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS);
            ps.setInt(1, recipientID);
            ps.setString(2, sName);
            ps.setInt(3, mesos);
            ps.setLong(4, System.currentTimeMillis());
            ps.setInt(5, isOn ? 0 : 1);

            ps.setInt(6, item.getType());
            ps.setString(7, content);
            ps.setInt(8, Quick ? 1 : 0);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                ItemLoader.DUEY.saveItems(Collections.singletonList(item), con, rs.getInt(1), GameConstants.getInventoryType(item.getItemId()), true);
            }
            rs.close();
            ps.close();

            return true;
        } catch (SQLException se) {
            se.printStackTrace();
            return false;
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    public static final List<MapleDueyActions> loadItems(final MapleCharacter chr) {
        List<MapleDueyActions> packages = new LinkedList<MapleDueyActions>();
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM dueypackages WHERE RecieverId = ?");
            ps.setInt(1, chr.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                MapleDueyActions dueypack = getItemByPID(rs.getInt("packageid"));
                dueypack.setSender(rs.getString("SenderName"));
                dueypack.setMesos(rs.getInt("Mesos"));
                dueypack.setSentTime(rs.getLong("TimeStamp"));
                dueypack.setContent(rs.getString("content"));
                dueypack.setQuick(rs.getInt("Quick") > 0);
                packages.add(dueypack);
            }
            rs.close();
            ps.close();
            return packages;
        } catch (SQLException se) {
            se.printStackTrace();
            return null;
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    public static final MapleDueyActions loadSingleItem(final int packageid, final int charid) {
        List<MapleDueyActions> packages = new LinkedList<MapleDueyActions>();
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM dueypackages WHERE PackageId = ? and RecieverId = ?");
            ps.setInt(1, packageid);
            ps.setInt(2, charid);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                MapleDueyActions dueypack = getItemByPID(packageid);
                dueypack.setSender(rs.getString("SenderName"));
                dueypack.setMesos(rs.getInt("Mesos"));
                dueypack.setSentTime(rs.getLong("TimeStamp"));
                dueypack.setContent(rs.getString("content"));
                dueypack.setQuick(rs.getInt("Quick") > 0);
                packages.add(dueypack);
                rs.close();
                ps.close();
                return dueypack;
            } else {
                rs.close();
                ps.close();
                return null;
            }
        } catch (SQLException se) {
            return null;
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    public static final void reciveMsg(final MapleClient c, final int recipientId) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE dueypackages SET Checked = 0 where RecieverId = ?");
            ps.setInt(1, recipientId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException se) {
            se.printStackTrace();
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    private static final void removeItemFromDB(final int packageid, final int charid) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM dueypackages WHERE PackageId = ? and RecieverId = ?");
            ps.setInt(1, packageid);
            ps.setInt(2, charid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException se) {
            se.printStackTrace();
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    private static final MapleDueyActions getItemByPID(final int packageid) {
        try {
            Map<Long, Item> iter = ItemLoader.DUEY.loadItems(false, packageid, null);
            if (iter != null && iter.size() > 0) {
                for (Entry<Long, Item> i : iter.entrySet()) {
                    return new MapleDueyActions(packageid, i.getValue());
                }
            }
        } catch (Exception se) {
            se.printStackTrace();
        }
        return new MapleDueyActions(packageid);
    }
}
