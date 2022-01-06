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
        ["Normal_Zakum", count, 280030100],
        ["Chaos_Zakum", count, 280030000]
    ]
    name = ["노말", "카오스"]
    if (mode == -1) {
        cm.dispose();
        return;
    }
    if (mode == -1 || mode == 0) {
        cm.dispose();
        return;
    }
    if (mode == 1) {
        status++;
    }
    if (status == 0) {
        st = cm.getPlayer().getMapId() - 211042400;
        if (st > 1 || st < 0) {
            cm.sendOk("자쿰의 제단이 아닌 곳에 엔피시가 소환되었습니다. 운영자께 문의해 주세요.");
            cm.dispose();
            return;
        }
        if (cm.getPlayer().getParty() == null) {
            cm.sendNext("자네는 파티를 맺지 않고 있군 그래. 파티를 맺어야만 도전할 수 있다네.");
            cm.dispose();
            return;
        }
        talk = "#e<자쿰 : " + name[st] + "모드>#k#n\r\n"
        talk += "자쿰이 부활했다네. 이대로 둔다면 화산 폭발을 일으켜서 엘나스 산맥 전체를 지옥으로 만들어 버릴거야.\r\n"
        talk += "#r(" + name[st] + " 자쿰의 제단은 #e하루에 3회 입장#n할 수 있으며, 입장 기록은 #e매일 자정에 초기화#n 됩니다.)\r\n#k또한 #b보스헌터#k 랭크별로 입장횟수가 추가됩니다.\r\n\r\n";
        talk += "#L0# #b자쿰 입장을 신청한다.(파티원이 동시에 이동됩니다.)#k";
        cm.sendSimple(talk);
    } else if (status == 1) {
        if (cm.getPlayer().getParty() == null) {
            cm.sendNext("자네는 파티를 맺지 않고 있군 그래. 파티를 맺어야만 도전할 수 있다네.");
            cm.dispose();
            return;
	} else if (!cm.allMembersHere()) {
	    cm.sendOk("모든 멤버가 같은 장소에 있어야 합니다.");
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
            talk += "#k#n 님이 오늘 자쿰의 제단에 입장하셔서 들어갈 수 없습니다.";
            cm.sendOk(talk);
            cm.dispose();
            return;
        }
        if (cm.getPlayerCount(setting[st][2]) >= 1) {
            talk = "이미 누군가가 자쿰의 제단에 도전하고 있군. 다른 채널을 이용해 주시게.";
            cm.sendOk(talk);
            cm.dispose();
            return;
        }
        var em = cm.getEventManager(setting[st][0]);
        cm.addBoss(setting[st][0]);
        if (em != null) {
            cm.getEventManager(setting[st][0]).startInstance_Party(setting[st][2] + "", cm.getPlayer());
        }
        cm.dispose();
    }
}