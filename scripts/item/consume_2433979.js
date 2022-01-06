﻿importPackage(java.lang);
importPackage(Packages.server);

var status = -1;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == 1) {
        status++;
    } else {
        cm.dispose();
        return;
    }
    if (status == 0 || status == 1) {
	var rand = Randomizer.rand(50000000, 300000000);
	cm.gainItem(2433979, -1);
	cm.gainMeso(rand);
        cm.dispose();
    } 
}