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
        cm.sendNextS("나는 메이플 월드 최고의 현상금 사냥꾼 #r#e폴로#k#n." + enter + "동생 #b#e프리토#k#n와 함께 마물들을 퇴치하고 있다.", 1);
    } else if (status == 1) {
        cm.sendSimpleS("이제 막 사냥을 떠나려던 길이였는데, 자네도 나와 함께하지 않겠나?" + enter + "#b#L1#함께 한다." + enter + "#L2#함께하지 않는다.", 1);
    } else if (status == 2) {
        cm.dispose();
        switch (sel) {
            case 1:
	  cm.getPlayer().addKV("poloFritto", cm.getPlayer().getMapId());
                cm.getPlayer().addRadomPotal("poloFritto", "" + cm.getPlayer().getMapId());
                /*if (cm.getClient().getChannelServer().getMapFactory().getMap(993000000).characterSize() == 0) {
                    maps.push(993000000);
                }*/
                if (cm.getClient().getChannelServer().getMapFactory().getMap(993000100).characterSize() == 0) {
                    maps.push(993000100);
                }
                if (maps.length == 0) {
                    cm.sendOk("현재 누군가가 진행중이군.");
					cm.dispose();
                    return;
                }
				if(!cm.getPlayer().setPoloMob() && cm.getPlayer().isGM()){
					cm.sendOk("여기는 몬스터가 없어서 나랑 함께 할수 없구만 사냥터에서 보자구.");
					cm.dispose();
                    return;
				}
				if(!cm.getPlayer().setPoloMob()){
					cm.sendOk("음.. 오류가 발생한거 같군 다시 시도해보게");
					cm.dispose();
                    return;
				}
                cm.warp(maps[Packages.server.Randomizer.nextInt(maps.length)], 0);
                break;
            case 2:
                cm.sendOk("그렇다면 어쩔수 없군.");
				cm.dispose();
                break;
        }
    }
}