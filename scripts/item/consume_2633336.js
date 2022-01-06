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

box = 2633336;

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
	  var text = "#fs11#교환하고싶은 #b어센틱 심볼#k 을 선택해주세요. 동일한 아이템으로  5개가 지급됩니다.\r\n";
	  text += "#L1##b#i1713000##z1713000#\r\n";
	  text += "#L2##b#i1713001##z1713001#\r\n";
	  cm.sendYesNo(text);
	} else if (selection == 1) {
	  cm.gainItem(1713000,5);
	  cm.gainItem(2633336,-1);
	  cm.sendOk("교환이 완료되었습니다.");
	  cm.dispose();
	} else if (selection == 2) {
	  cm.gainItem(1713001,5);
	  cm.gainItem(2633336,-1);
	  cm.sendOk("교환이 완료되었습니다.");
	  cm.dispose();
	}
}