﻿var status;
importPackage(Packages.server);
importPackage(Packages.client.inventory);
importPackage(Packages.server);
importPackage(Packages.server.items);
one = Math.floor(Math.random() * 5) + 1 // 최소 10 최대 35 , 혼테일
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
		cm.getPlayer().gainDonationPoint(500000);
                cm.gainItem(5068305, 6); // 블랙 베리
                cm.gainItem(2439530, 1); // 기운석
                cm.gainItem(2023287, 1); // 크리데미지
                //cm.gainItem(2046996, 5); // 어메이징큐브
                cm.gainItem(4023026, 1); // 25성 초월
                cm.gainItem(3994718, 3); // 예쁜돌맹이
                cm.gainItem(4001715, 100); // 메소
		cm.gainItem(2630695, -1);
		cm.dispose();
	}
}

