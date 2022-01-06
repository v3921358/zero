importPackage(java.sql);
importPackage(java.lang);
importPackage(Packages.database);
importPackage(Packages.handling.world);
importPackage(Packages.constants);
importPackage(java.util);
importPackage(java.io);
importPackage(Packages.client.inventory);
importPackage(Packages.client);
importPackage(Packages.server);
importPackage(Packages.tools.packet);

box = 2437760;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1 || mode == 0) {
        cm.dispose();
        return;
    }
    if (mode == 1) {
        status++;
    }
	if (status == 0) {
	  var text = "#fs11#교환하고싶은 #b아케인 심볼#k 을 선택해주세요. 동일한 아이템으로  5개가 지급됩니다.\r\n";
	  text += "#L1##b#i1712001##z1712001#\r\n";
	  text += "#L2##b#i1712002##z1712002#\r\n";
	  text += "#L3##b#i1712003##z1712003#\r\n";
	  text += "#L4##b#i1712004##z1712004#\r\n";
	  text += "#L5##b#i1712005##z1712005#\r\n";
	  text += "#L6##b#i1712006##z1712006#\r\n";
	  cm.sendYesNo(text);
	} else if (selection == 1) {
	  cm.gainItem(1712001,5);
	  cm.gainItem(2437760,-1);
	  cm.sendOk("교환이 완료되었습니다.");
	  cm.dispose();
	} else if (selection == 2) {
	  cm.gainItem(1712002,5);
	  cm.gainItem(2437760,-1);
	  cm.sendOk("교환이 완료되었습니다.");
	  cm.dispose();
	} else if (selection == 3) {
	  cm.gainItem(1712003,5);
	  cm.gainItem(2437760,-1);
	  cm.sendOk("교환이 완료되었습니다.");
	  cm.dispose();
	} else if (selection == 4) {
	  cm.gainItem(1712004,5);
	  cm.gainItem(2437760,-1);
	  cm.sendOk("교환이 완료되었습니다.");
	  cm.dispose();
	} else if (selection == 5) {
	  cm.gainItem(1712005,5);
	  cm.gainItem(2437760,-1);
	  cm.sendOk("교환이 완료되었습니다.");
	  cm.dispose();
	} else if (selection == 6) {
	  cm.gainItem(1712006,5);
	  cm.gainItem(2437760,-1);
	  cm.sendOk("교환이 완료되었습니다.");
	  cm.dispose();
	}
}