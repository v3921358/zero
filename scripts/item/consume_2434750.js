var status;
var book  = new Array(1182197, 1142919, 4001209, 4001209, 4001209, 4001209, 4001209, 4001210, 4001210, 4001210, 4001211, 4001211, 4001212, 4001716, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029, 4310029);

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
		var text = "강화의 금요일 상자에서는 다음 물품 중 하나가 랜덤으로 드롭됩니다.\r\n";
		for (var i = 0; i < book.length; i++) {
		    text+="#i"+book[i]+":# #z"+book[i]+":##l\r\n";
		}
		text += "상자를 개봉하시겠습니까?";
		cm.sendYesNo(text);
	} else if (status == 1) {
		item = book[Math.floor(Math.random() * book.length)];
		cm.gainItem(item, 1);
		cm.gainItem(2434750, -1);
		if (item == 4310029) {
		cm.gainItem(4310029, 49);
		}
		cm.sendOk("#i" + item + ":##z" + item + ":# 아이템이 수령되었습니다.");
		cm.dispose();
    	}
}
