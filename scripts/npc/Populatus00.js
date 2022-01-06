var status = -1;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
        var count = 1;
    if (cm.getPlayer().getBossTier() >= 1) {
        count += cm.getPlayer().getBossTier();
    }
    setting = [
        ["Normal_Populatus", count, 220080200, 155],
        ["Chaos_Populatus", count, 220080300, 190]
    ]
    name = ["노멀", "하드"]
    if (mode == -1 || mode == 0) {
        cm.dispose();
        return;
    }
    if (mode == 1) {
        if (status == 0 && cm.itemQuantity(4031179) != 0) {
            st = selection;
            status++;
        }
        status++;
    }
    if (status == 0) {
        if (cm.getParty() == null) {
            cm.sendOk("1인 이상 파티를 맺어야만 입장할 수 있습니다.");
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
        말 = "#e<보스: 파풀라투스>#n\r\n"
        말 += "사고뭉치 파풀라투스가 차원을 계속 부수는 것을 막아야 합니다. 도와주시겠어요?\r\n\r\n\r\n"
        말 += "#L0# 노멀 모드 ( 레벨 155 이상 )\r\n"
        말 += "#L1# 카오스 모드 ( 레벨 190 이상 )#l";
        cm.sendSimple(말);
    } else if (status == 1) {
        st = selection;
        if (cm.itemQuantity(4031179) == 0) {
            cm.gainItem(4031179, 1);
        }
        cm.sendNext("차원 균열의 조각이 없으시군요. 파풀라투스를 만나기 위해서 꼭 필요합니다. 제가 갖고 있던 것을 트릴테니, 파풀라투스가 차원을 부수는 것을 꼭 막아주세요!");
    } else if (status == 2) {
        if (cm.getPlayerCount(setting[st][2]) != 0) {
            cm.sendOk("이미 누군가가 파풀라투스에 도전하고 있습니다.\r\n다른채널을 이용 해 주세요.");
            cm.dispose();
            return;
        }
        if (!cm.isBossAvailable(setting[st][0], setting[st][1])) {
            talk = "파티원 중 "
            for (i = 0; i < cm.BossNotAvailableChrList(setting[st][0], setting[st][1]).length; i++) {
                if (i != 0) {
                    talk += ", "
                }
                talk += "#b#e" + cm.BossNotAvailableChrList(setting[st][0], setting[st][1])[i] + ""
            }
            talk += "#k#n님이 오늘 시계탑의 근원에 입장했습니다. 오늘은 더 이상 들어갈 수 없습니다.\r\n\r\n"
            talk += "(" + name[st] + "모드는 #e하루에 " + setting[st][1] + "회 입장#n할 수 있으며, 입장 기록은 #e매일 자정에 초기화#n 됩니다.)";
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
            talk += "#k#n님의 레벨이 부족합니다. 시계탑의 근원에 입장하실 수 없습니다.\r\n\r\n"
            talk += "(" + name[st] + "모드는 #e" + setting[st][3] + " 레벨 이상#n만 입장 가능합니다.)";
            cm.sendOk(talk);
            cm.dispose();
            return;
        } else {
            cm.addBoss(setting[st][0]);
            em = cm.getEventManager(setting[st][0]);
            if (em != null) {
                cm.getEventManager(setting[st][0]).startInstance_Party(setting[st][2] + "", cm.getPlayer());
            }
            cm.dispose();
        }
    }
}