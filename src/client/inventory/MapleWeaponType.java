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
package client.inventory;

public enum MapleWeaponType {

    NOT_A_WEAPON(1.43f, 20),
    BOW(1.2f, 15),
    CLAW(1.75f, 15),
    CANE(1.3f, 15), // TODO: Renegades
    DAGGER(1.3f, 20),
    CROSSBOW(1.35f, 15),
    AXE1H(1.2f, 20),
    SWORD1H(1.2f, 20),
    BLUNT1H(1.2f, 20),
    AXE2H(1.32f, 20),
    SWORD2H(1.32f, 20),
    BLUNT2H(1.32f, 20),
    POLE_ARM(1.49f, 20),
    SPEAR(1.49f, 20),
    STAFF(1.0f, 25),
    WAND(1.0f, 25),
    KNUCKLE(1.7f, 20),
    GUN(1.5f, 15),
    DUAL_BOW(2.0f, 15), //beyond op
    MAGIC_ARROW(2.0f, 15),
    KATARA(1.3f, 20),
    BIG_SWORD(1.3f, 15),
    LONG_SWORD(1.3f, 15),
    HANDCANNON(1.5f, 15),
    ENERGYSWORD(1.5f, 15),
    SWORD(1.56f, 20),
    PLANE(1.21f, 15),
    DESPERADO(1.23f, 20),
    GUNTLETREVOLVER(1.7f, 20),
    SOULSHOOTER(1.22f, 15),
    ESPLIMITER(1.2f, 20),
    TEDO(1.57f, 20),
    CHAIN(1.3f, 20),
    MAGICGUNTLET(1.2f, 20),
    ACIENTBOW(1.3f, 20),
    FAN(1.3f, 20),
    TUNER(1.3f, 20);
    private final float damageMultiplier;
    private final int baseMastery;

    private MapleWeaponType(final float maxDamageMultiplier, int baseMastery) {
        this.damageMultiplier = maxDamageMultiplier;
        this.baseMastery = baseMastery;
    }

    public final float getMaxDamageMultiplier() {
        return damageMultiplier;
    }

    public final int getBaseMastery() {
        return baseMastery;
    }
};
