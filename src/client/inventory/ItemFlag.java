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

public enum ItemFlag {

    LOCK(0x01), // 봉인의 자물쇠
    SPIKES(0x02),
    KARMA_USE(0x02), // 장교불 캐시
    USE_BAG(0x04), // 가방 사용
    UNTRADEABLE(0x08), // 교환 불가
    KARMA_EQUIP(0x10), // 장교불 장비
    CHARM_EQUIPED(0x20), // 장착 시 매력 +n 삭제
    ANDROID_ACTIVATED(0x40),
    CRAFTED(0x80),
    PROTECT_SHIELD(0x100), // 프로텍트 쉴드
    LUCKY_PROTECT_SHIELD(0x200), // 럭키 프로텍트 쉴드
    //0x400 unknown
    //0x800 unknown
    TRADEABLE_ONETIME_EQUIP(0x1000), // 계정 내 1회 교환 가능 (쉐어 네임 택)
    SAFETY_SHIELD(0x2000), // 세이프티 쉴드
    RECOVERY_SHIELD(0x4000), // 리커버리 쉴드
    RETURN_SCROLL(0x8000); // 리턴 스크롤
    private final int i;

    private ItemFlag(int i) {
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
