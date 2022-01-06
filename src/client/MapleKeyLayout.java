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
package client;

import tools.Pair;
import tools.data.MaplePacketLittleEndianWriter;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class MapleKeyLayout implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    private boolean changed = false;
    private Map<Integer, Pair<Byte, Integer>> keymap;

    public MapleKeyLayout() {
        keymap = new HashMap<Integer, Pair<Byte, Integer>>();
    }

    public MapleKeyLayout(Map<Integer, Pair<Byte, Integer>> keys) {
        keymap = keys;
    }

    public final Map<Integer, Pair<Byte, Integer>> Layout() {
        changed = true;
        return keymap;
    }

    public final void unchanged() {
        changed = false;
    }

    public final void writeData(final MaplePacketLittleEndianWriter mplew) {
        Pair<Byte, Integer> binding;
        for (int x = 0; x < 89; x++) {
            binding = keymap.get(Integer.valueOf(x));
            if (binding != null) {
                mplew.write(binding.getLeft());
                mplew.writeInt(binding.getRight());
            } else {
                mplew.write(0);
                mplew.writeInt(0);
            }
        }
    }

    public final void saveKeys(Connection con, final int charid) throws SQLException {
        if (!changed) {
            return;
        }
        PreparedStatement ps = con.prepareStatement("DELETE FROM keymap WHERE characterid = ?");
        ps.setInt(1, charid);
        ps.execute();
        ps.close();
        if (keymap.isEmpty()) {
            return;
        }

//    	Map<Integer, Pair<Byte, Integer>> copyCores = Collections.synchronizedMap(keymap);
        //  	synchronized (copyCores) {
        Iterator<Entry<Integer, Pair<Byte, Integer>>> key = keymap.entrySet().iterator();

        ps = con.prepareStatement("INSERT INTO keymap (`characterid`, `key`, `type`, `action`) VALUES (?, ?, ?, ?)");
        ps.setInt(1, charid);
        while (key.hasNext()) {
            Entry<Integer, Pair<Byte, Integer>> keybinding = key.next();
            ps.setInt(2, keybinding.getKey().intValue());
            ps.setInt(3, keybinding.getValue().getLeft().byteValue());
            ps.setInt(4, keybinding.getValue().getRight().intValue());
            ps.execute();
        }
        ps.close();
    }

//    }
}
