var status = -1;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    setting = [
        ["Normal_Lucid", 3, 450004100, 230],
        ["Hard_Lucid", 3, 450004400, 235],
        ["Normal_Lucid", 5, 450004100, 230],
        ["Hard_Lucid", 5, 450004400, 235],
        ["Extreme_Lucid", 2, 450004400, 230]
    ]
    name = ["노멀", "하드", "노멀 연습", "하드 연습"]
    if (mode == -1 || mode == 0) {
        cm.dispose();
        return;
    }
    if (mode == 1) {
        status++;
    }

    if (status == 0) {
        talk = "#e<보스: 루시드>#n\r\n"
        talk+= "루시드를 막지 못한다면, 무서운 일이 일어날 것입니다.\r\n\r\n"
//        if (cm.getPlayer().isGM()) {
            //talk+= "#L4# #b<보스: 루시드(익스트림)> 입장을 신청한다.#l\r\n"
  //      }
        talk+= "#L0# #b<보스: 루시드(노멀)> 입장을 신청한다.(레벨 230이상)#l\r\n"
        talk+= "#L1# #b<보스: 루시드(하드)> 입장을 신청한다.(레벨 235이상)#l\r\n"
        talk+= "#L2# #b<보스: 루시드(노멀 연습)> 입장을 신청한다.(레벨 230이상)#l\r\n"
        talk+= "#L3# #b<보스: 루시드(하드 연습)> 입장을 신청한다.(레벨 235이상)#l\r\n"
        cm.sendSimple(talk);
    } else if (status == 1) {
        st = selection;
        if (cm.getParty() == null) {
            cm.sendOk("파티를 맺어야만 입장할 수 있습니다.")
            cm.dispose();
            return;
        } else if (cm.getPlayerCount(setting[st][2]) >= 1 || cm.getPlayerCount(Number(setting[st][2]) + 50) || cm.getPlayerCount(Number(setting[st][2]) + 100) || cm.getPlayerCount(Number(setting[st][2]) + 150) || cm.getPlayerCount(Number(setting[st][2]) + 200) >= 1) {
            cm.sendOk("이미 누군가가 루시드에 도전하고 있습니다.\r\n다른채널을 이용 해 주세요.");
            cm.dispose();
            return;
        } else if (!cm.isLeader()) {
            cm.sendOk("파티장만이 입장을 신청할 수 있습니다.");
            cm.dispose();
            return;
	} else if (!cm.allMembersHere()) {
	    cm.sendOk("모든 멤버가 같은 장소에 있어야 합니다.");
	    cm.dispose();
            return;
        }
        if (!cm.isBossAvailable(setting[st][0], setting[st][1])) {
            talk = "파티원 중 "
            for (i = 0; i < cm.BossNotAvailableChrList(setting[st][0],  setting[st][1]).length; i++) {
                if (i != 0) {
                    talk += ", "
                }
                talk += "#b#e" + cm.BossNotAvailableChrList(setting[st][0], setting[st][1])[i] + ""
            }
            talk += "#k#n님이 오늘 입장했습니다. 루시드 " + name[st] + "모드는 하루에 " + setting[st][1] + "번만 도전하실 수 있습니다.";
            cm.sendOk(talk);
            cm.dispose();
            return;
        } else if (!cm.isLevelAvailable(setting[st][3])) {
            talk = "파티원 중 "
            for (i = 0; i < cm.LevelNotAvailableChrList(setting[st][3]).length; i++) {
                if (i != 0) {
                    talk += ", "
                }
                talk += "#b#e" + cm.LevelNotAvailableChrList(setting[st][3])[i] + ""
            }
            talk += "#k#n님의 레벨이 부족합니다. 루시드 " + name[st] + "모드는 " + setting[st][3] + " 레벨 이상만 입장 가능합니다.";
            cm.sendOk(talk);
            cm.dispose();
            return;
        } else {
            if (st <= 1) {
                cm.addBoss(setting[st][0]);
                em = cm.getEventManager(setting[st][0]);
                if (em != null) {
                    cm.getEventManager(setting[st][0]).startInstance_Party(setting[st][2] + "", cm.getPlayer());
                }
                cm.dispose();
            } else if (st <= 3) {
                cm.sendYesNoS("연습 모드에 입장을 선택하셨습니다. 연습 모드에서는 #b#e경험치와 보상을 받을 수 없으며#n#k 보스 몬스터의 종류와 상관없이 #b#e하루 5회#k#n만 이용할 수 있습니다. 입장하시겠습니까?", 4, 2007);
            } else {
                cm.sendYesNoS("익스트림 모드에 입장을 선택하셨습니다. 익스트림 모드에서는 데미지가 70% 감소하지만, #b#e보다 강력한 장비 보상이 추가옵션과 함께 드롭됩니다.#n#k 해당 장비의 추가옵션은 #b#e이노센트 주문서 이용 시 함께 사라지며, 이는 복구가 불가능합니다.#k#n 입장하시겠습니까?", 4, 2007);
            }
        }
    } else if (status == 2) {
        cm.dispose();
        if (st <= 3) {
            cm.addBossPractice(setting[st][0]);
            em = cm.getEventManager(setting[st][0]);
            if (em != null) {
                cm.getEventManager(setting[st][0]).startInstance_Party(setting[st][2] + "", cm.getPlayer());
            }
        } else {
//            if (cm.getPlayer().isGM()) {
                cm.addBoss(setting[st][0]);
                em = cm.getEventManager(setting[st][0]);
                if (em != null) {
                    cm.getEventManager(setting[st][0]).startInstance_Party(setting[st][2] + "", cm.getPlayer());
                }
//            }
        }
    }
}