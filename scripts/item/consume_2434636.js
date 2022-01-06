var status;
importPackage(Packages.server);
importPackage(Packages.client.inventory);

item = [1142910, 2048716, 2048717, 2049700, 2049153, 2049004, 2434290, 4001832];

function start() {
    status = -1;
    action(1, 1, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
        return;
    }
    if (mode == 0) {
        status --;
    }
    if (mode == 1) {
        status++;
    }
    if (status == 0) {
        cm.dispose();
        if (cm.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() > 0) {
            if (cm.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() > 0) {
                if (cm.getPlayer().getInventory(MapleInventoryType.ETC).getNumFreeSlot() > 0) {
                    cm.gainItem(2434636, -1);
                    it = item[Math.floor(Math.random() * item.length)];
                    cm.gainItem(it, it == 4001832 ? 800 : 1);
                    return;
                }
            }
        }
        cm.sendOkS("장비창, 소비창, 기타창을 각각 한 칸씩 비우셔야 합니다.", 4, 9001060);
    }
}
