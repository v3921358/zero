var status;
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
        as = Randomizer.rand(1, 1);
		cm.gainItem(2437122, 3);
                cm.gainItem(2430026, 4);
                cm.gainItem(4033114, 1000);
                cm.gainItem(4310261, 5000);
                cm.gainItem(4023026, 1);
		cm.gainItem(2439209, -1);
		cm.dispose();
	}
}
