importPackage(Packages.client.inventory);
importPackage(Packages.server);
importPackage(java.text);

var status = -1;
isReady = false;
var nf = java.text.NumberFormat.getInstance();
geti = null;
hm = 0;
function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    enhance = ["보스 공격시 데미지 +%", "몬스터 방어율 무시 +%", "데미지 +%", "올스탯 +%"]

    itemid = 4031227;
    qty = 30; //1%당 아이템 개수
    limit = [40, 50, 90, 10];





    if (mode == 1) {
        if (status == 3 && selection != 10) {
            if (selection == 0) {
                hw -= hm;
                hm = 0;
            } else if (selection == 1) {
                hw -= 10;
                hm -= 10;
            } else if (selection == 2) {
                hw -= 1;
                hm -= 1;
            } else if (selection == 3) {
                hw += 1;
                hm += 1;
            } else if (selection == 4) {
                hw += 10;
                hm += 10;
            } else {
                hw += limit[st2] - hw;
                hm = hw - parseInt(gE);
            }
        } else {
            status++;
        }
    } else {
        cm.dispose();
        return;
    }
    if (status == 0) {
        말 = "#fs11##r#i4031227##z4031227##fc0xFF000000#을 이용해\r\n원하시는 장비에 #b특별 추가옵션#fc0xFF000000#을 부여할 수 있습니다.\r\n\r\n";
        말 += "#L0# #b추가옵션 부여하기#k#l";
        cm.sendSimple(말);
    } else if (status == 1) {
        말 = "#fs11##b능력부여를 진행할 아이템을 선택해 주세요.\r\n\r\n";
        for (i = 0; i < cm.getInventory(1).getSlotLimit(); i++) {
            if ((cm.getInventory(1).getItem(i) != null) && !cm.isCash(cm.getInventory(1).getItem(i).getItemId())) {
                말 += "#L" + i + "# #i" + cm.getInventory(1).getItem(i).getItemId() + "# #z" + cm.getInventory(1).getItem(i).getItemId() + "#\r\n"
            }
        }
        cm.sendSimple(말);
    } else if (status == 2) {
        st = selection;
        말 = "#fs11##b선택된 아이템 :#k#n #i" + cm.getInventory(1).getItem(st).getItemId() + "#\r\n\r\n";
        말 += "#fs11##fc0xFF000000#부여하시고 싶은 강화옵션을 선택해 주세요.\r\n\r\n";
        if (isWeapon(cm.getInventory(1).getItem(st).getItemId())) {
        }
        말 += "#fc0xFF6600CC##L3#올스탯 +% #b(최대 10%)#l\r\n"
            말 += "#L0##fc0xFF6600CC#보스 공격시 데미지 +% #b(최대 40%)#l\r\n";
            말 += "#L1##fc0xFF6600CC#몬스터 방어율 무시 +% #b(최대 50%)#l\r\n";
            말 += "#L2##fc0xFF6600CC#데미지 +% #b(최대 90%)#l\r\n";
        cm.sendSimple(말);
    } else if (status == 3) {
        if (!isReady) {
            gI = cm.getInventory(1).getItem(st)
            st2 = selection;
        }
        if (st2 == 0) {
            gE = gI.getBossDamage();
        } else if (st2 == 1) {
            gE = gI.getIgnorePDR();
        } else if (st2 == 2) {
            gE = gI.getTotalDamage();
        } else {
            gE = gI.getAllStat();
        }
        if (!isReady) {
            hw = parseInt(gE) + hm;
            isReady = true;
        }
        말 = "#fs11##b선택된 아이템 :#k#n #i" + cm.getInventory(1).getItem(st).getItemId() + "#\r\n\r\n";
        말 += "#fs11##b특별 추가옵션 :#k#n " + enhance[st2] + "\r\n\r\n";
        말 += "#fs11##d원하는 증가수치를 입력해 주세요.\r\n#r(1% 당 #i4031227# 30개 소모)#k\r\n\r\n";

        말 += "#fs11#수치 변화 : #b" + gE + "%#k → #r" + hw + "% #fc0xFF0BCACC#(+" + hm + "%)#d#n\r\n\r\n"
        if (hm >= 10) {
            말 += "#L1# -10%#l "
        }
        if (hm >= 1) {
            말 += "#L2# -1%#l "
        }
        if (hw + 1 <= limit[st2]) {
            말 += "#L3# +1%#l ";
        }
        if (hw + 10 <= limit[st2]) {
            말 += "#L4# +10%#l ";
        }
        말 += "\r\n"
        말 += "#L10# #fs11##r설정을 완료했습니다."
        cm.sendSimple(말);
    } else if (status == 4) {
        if (cm.itemQuantity(itemid) >= qty * hm) {
            cm.gainItem(itemid, -qty * hm);
          if (st2 == 0) {
                gI.setBossDamage(hw);
            } else if (st2 == 1) {
                gI.setIgnorePDR(hw);
            } else if (st2 == 2) {
                gI.setTotalDamage(hw);
            } else {
                gI.setAllStat(hw);
            }
           
            cm.sendOk("#fs11#능력 부여가 완료되었습니다.");
            cm.getPlayer().forceReAddItem(gI, Packages.client.inventory.MapleInventoryType.EQUIP);
            cm.dispose();
        } else {
            cm.sendOk("#fs11#재료가 부족한거같습니다.");
            cm.dispose();
        }
    }
}


function isWeapon(itemid) {
    if (Math.floor(itemid / 10000) >= 121 && Math.floor(itemid / 10000) <= 158) {
        return true;
    } else {
        return false;
    }
}