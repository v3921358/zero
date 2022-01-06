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
package handling.login;

import client.MapleClient;
import handling.channel.ChannelServer;
import tools.packet.CWvsContext;
import tools.packet.LoginPacket;

import java.util.Map;

import static handling.login.handler.CharLoginHandler.ServerListRequest;

public class LoginWorker {

    private static long lastUpdate = 0;

    public static void registerClient(final MapleClient c, final String id, String pwd) {
        if (LoginServer.isAdminOnly() && !c.isGm() && !c.isLocalhost()) {
            c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "서버 점검중입니다."));
            c.getSession().writeAndFlush(LoginPacket.getLoginFailed(21));
            return;
        }

        if (System.currentTimeMillis() - lastUpdate > 600000) { // Update once every 10 minutes
            lastUpdate = System.currentTimeMillis();
            final Map<Integer, Integer> load = ChannelServer.getChannelLoad();
            int usersOn = 0;
            if (load == null || load.size() <= 0) { // In an unfortunate event that client logged in before load
                lastUpdate = 0;
                c.getSession().writeAndFlush(LoginPacket.getLoginFailed(7));
                return;
            }
            LoginServer.setLoad(load, usersOn);
            lastUpdate = System.currentTimeMillis();
        }

        if (c.finishLogin() == 0) {
            c.getSession().writeAndFlush(LoginPacket.getAuthSuccessRequest(c, id, pwd));
            ServerListRequest(c, false);
            /*            c.setIdleTask(PingTimer.getInstance().schedule(new Runnable() {

             public void run() {
             c.getSession().close(true);
             System.out.print("?몄뀡?대줈利?8");
             }
             }, 10 * 60 * 10000));*/
        } else {
            c.getSession().writeAndFlush(LoginPacket.getLoginFailed(7));
            return;
        }
    }
}
