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
package server.maps;

public enum SummonMovementType {

    STATIONARY(0),
    FOLLOW(1),
    WALK_STATIONARY(2),
    BIRD_FOLLOW(3),
    CIRCLE_FOLLOW(4),
    CIRCLE_STATIONARY(5),
    ZEROWEAPON(6),
    FLAME_SUMMON(7),
    ShadowServant(8),
    SUMMON_JAGUAR(11),
    BIRD_FOLLOW2(13),
    ShadowServantExtend(14),
    MeachCarrier(17);

    private final int val;

    private SummonMovementType(int val) {
        this.val = val;
    }

    public int getValue() {
        return val;
    }
}
