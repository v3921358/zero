
var status;
var select = -1;
var book  = new Array(1212063,1222058,1232057,1242060,1302275,1312153,1322203,1332225,1342082,1362090,1372177,1382208,1402196,1412135,1422140,1432167,1442223,1452205,1462193,1472214,1482168,1492179,1522094,1532098);

function start() {    status = -1;
    action(1, 1, 0);
}

function action(mode, type, selection) {
    if (mode <= 0) {
        cm.dispose();
    	return;
    } else {
        if (mode == 1)
            status++;
        if (status == 0) {
	    var text = "받고 싶은 파프니르 무기를 선택해줘.\r\n\r\n#b";
		for (var i = 0; i < book.length; i++) {
		    text+="#L"+i+"##i"+book[i]+"# #z"+book[i]+"##l\r\n";
		}
		cm.sendSimple(text);
	} else if (status == 1) {
		select = selection;
		cm.sendYesNo("받을 파프니르 무기는 #b#z"+book[select]+"##k 맞아?");
	} else if (status == 2) {
	    if (cm.haveItem(2431938, 1)) {
		if (cm.canHold(1212063)) {
		    cm.sendOk("인벤토리를 확인하세요");
		    cm.gainItem(2431938, -1);
		    cm.gainItem(book[select], 1);
		    cm.dispose();
		} else {
		    cm.sendOk("장비칸에 빈 공간이 없습니다.");
		    cm.dispose();
		}
            } else {
		cm.sendOk("부족합니다.");
		cm.dispose();

}
	}
    }
}