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
package server.enchant;

public enum EnchantFlag {

    Watk(0x1),
    Matk(0x2),
    Str(0x4),
    Dex(0x8),
    Int(0x10),
    Luk(0x20),
    Wdef(0x40),
    Mdef(0x80),
    Hp(0x100),
    Mp(0x200),
    Acc(0x400),
    Avoid(0x800),;
    private final int i;

    private EnchantFlag(int i) {
        this.i = i;
    }

    public final int getValue() {
        return i;
    }

    public final boolean check(int flag) {
        if (flag == 0) {
            return false;
        } else {
            return (flag & i) == i;
        }
    }
}
