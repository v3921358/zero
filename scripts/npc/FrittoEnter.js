var enter = "\r\n";
var seld = -1;

var maps = [];

// 993000000 : 현상금 사냥
// 993000100 : 성벽 지키기
// 993000650 : 스톰윙 출몰지역

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, sel) {
    if (mode == 1) {
        status++;
    } else {
        cm.dispose();
        return;
    }
    if (status == 0) {
        cm.sendNextS("나는 메이플 월드 최고의 현상금 사냥꾼 #r#e프리토#k#n." + enter + "형 #b#e폴로#k#n와 함께 마물들을 퇴치하고 있다.", 1);
    } else if (status == 1) {
        cm.sendSimpleS("이제 막 사냥을 떠나려던 길이였는데, 자네도 나와 함께하지 않겠나?" + enter + "#b#L1#함께 한다." + enter + "#L2#함께하지 않는다.", 1);
    } else if (status == 2) {
        cm.dispose();
        switch (sel) {
            case 1:
	  cm.getPlayer().addKV("poloFritto", cm.getPlayer().getMapId());
                cm.getPlayer().addRadomPotal("poloFritto", "" + cm.getPlayer().getMapId());
                if (cm.getClient().getChannelServer().getMapFactory().getMap(993000200).characterSize() == 0) {
					cm.getClient().getChannelServer().getMapFactory().getMap(993000200).resetFully();
                    maps.push(993000200);
                }
                if (cm.getClient().getChannelServer().getMapFactory().getMap(993000300).characterSize() == 0) {
					cm.getClient().getChannelServer().getMapFactory().getMap(993000300).resetFully();
                    maps.push(993000300);
                }
				if (cm.getClient().getChannelServer().getMapFactory().getMap(993000400).characterSize() == 0) {
					cm.getClient().getChannelServer().getMapFactory().getMap(993000400).resetFully();
                    maps.push(993000400);
                }
				
                if (maps.length == 0) {
                    cm.sendOk("현재 누군가가 진행중이군.");
                    return;
                }
                cm.warp(maps[Packages.server.Randomizer.nextInt(maps.length)], 0);
                break;
            case 2:
                cm.sendOk("그렇다면 어쩔수 없군.");
                    return;
                break;
        }
    }
}