var status = 0;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
    } else {
        if (mode == 0) {
            cm.dispose();
            return;
        }
        if (mode == 1)
            status++;
        else
            status--;
	if (status == 0) {
		var jessica = "#fn나눔고딕 Extrabold##fs13##r            모든 몬스터를 처리 후 소환이 가능합니다.#k\r\n\r\n";
		jessica += "#L0##d보스 강제 킬 처리#k"
		cm.sendSimple(jessica);
	} else if (status == 1) {
	if (selection == 0) {
                cm.killAllMob();
		cm.dispose();
}
}
}
}