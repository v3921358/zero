var enter = "\r\n";
var seld = -1, seld2 = -1;

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
		var msg = "     #fUI/UIWindow5.img/Disguise/backgrnd3#\r\n";
		msg +="#L1##fs11##fUI/Basic.img/BtCoin/normal/0##fc0xFFFF3300# 소비상점#l      #fc0xFF000099##L5##fUI/UIWindow4.img/pointShop/100711/iconShop# 네오스톤#l      #fc0xFFFF3366##L11##fUI/UIWindow4.img/pointShop/501053/iconShop# 도네이션 (후원)#l\r\n";
		msg +="#L2##fUI/Basic.img/BtCoin/normal/0##fc0xFFFF3300# 기본악세#l      #fc0xFF000099##L6##fUI/UIWindow4.img/pointShop/100712/iconShop# 휴식상점#l      #L12##fUI/UIWindow4.img/pointShop/501053/iconShop##fc0xFFFF3366# 서포터즈 (홍보)#l\r\n";
		msg +="#L3##fUI/Basic.img/BtCoin/normal/0##fc0xFFFF3300# 모루상점#l      #fc0xFF6633FF##L7##fUI/UIWindow4.img/pointShop/500629/iconShop# 유니온샵#l\r\n";
		msg +="#L4##fUI/Basic.img/BtCoin/normal/0##fc0xFFFF3300# 캐시상점#l      #fc0xFF6633FF##L8##fUI/UIWindow4.img/pointShop/100508/iconShop# 출석상점#l      #fc0xFF990033##L22##fUI/UIWindow4.img/pointShop/501215/iconShop# 네오코어#l\r\n";
		msg +="#L9##fUI/Basic.img/BtCoin/normal/0##fc0xFFFF3300# 보조무기#l      #fc0xFF6633FF##L10##fUI/UIWindow4.img/pointShop/17015/iconShop# 제로코인#l      #fc0xFF6633FF##L23##fUI/UIWindow4.img/pointShop/4001886/iconShop# 보스결정판매#l\r\n\r\n";
		msg +="#Cgray##fs11#――――――――――――――――――――――――――――――――――――――――#k";
		//msg +="#L13##fUI/UIWindow4.img/pointShop/4310065/iconShop##fc0xFF0066CC# 파프니르 상점#fc0xFF000000# 이용하기#l\r\n";
		msg +="#L14##fUI/UIWindow4.img/pointShop/4310156/iconShop##fc0xFFCC0000# 앱솔랩스 상점#fc0xFF000000# 이용하기#l        #L18##fUI/UIWindow4.img/pointShop/4310156/iconShop##fc0xFFCC0000# #z4310156##fc0xFF000000# 교환#l\r\n";
		msg +="#L15##fUI/UIWindow4.img/pointShop/4310199/iconShop##fc0xFFCC0000# 앱솔랩스 상점#fc0xFF000000# 이용하기#l        #L19##fUI/UIWindow4.img/pointShop/4310199/iconShop##fc0xFFCC0000# #z4310199##fc0xFF000000# 교환#l\r\n";
		msg +="#L16##fUI/UIWindow4.img/pointShop/4310218/iconShop##fc0xFF000099# 아케인셰이드 상점#fc0xFF000000# 이용하기#l   #L20##fUI/UIWindow4.img/pointShop/4310218/iconShop##fc0xFF000099# #z4310218##fc0xFF000000# 교환#l\r\n";
		msg +="#L17##fUI/UIWindow4.img/pointShop/4310249/iconShop##fc0xFF000099# 아케인셰이드 상점#fc0xFF000000# 이용하기#l   #L21##fUI/UIWindow4.img/pointShop/4310249/iconShop##fc0xFF000099# #z4310249##fc0xFF000000# 교환#l\r\n";
		cm.sendSimple(msg);
	} else if (status == 1) {
		seld = sel;
		switch (sel) {
			case 1:
                                cm.dispose();
                                cm.openShop(1);
			break;
			case 2:
				//var msg = "#fs11#성장에 필요한 장비들이 판매되고 있는 상점들입니다. 원하시는게 있으신가요?"+enter;
				//msg += "#L7##fs11# #b펜살리르#n #k장비"+enter;
				//msg += "#L2##fs11# #b타일런트#n #k장비"+enter;
				//msg += "#L8##fs11# #b파프니르#n #k장비"+enter;
				//msg += "#L3# #b앱솔랩스#n #k장비#l#L10##b스티그마 코인#k#l"+enter;
				//msg += "#L4# #b아케인셰이드#n #k장비"+enter+enter;
                                //msg += "#L9##b 기본 악세서리#n #k상점"+enter;
		     //msg += "#L5##b 보조무기#n #k상점"+enter;
                               // msg += "#L1##b 방패#n #k상점"+enter;
                                //msg += "#L10# #b[판매] #d#e보스결정#n #k판매"+enter;
						cm.dispose();
						cm.openShop(9031015);
					break;
			case 3:
						cm.dispose();
						cm.openShop(9031003);
					break;
			case 4:
				//var msg = "#fs11#성장에 중요한 물품이 판매되고 있는 상점입니다. 원하시는게 있으신가요?"+enter+enter;
                                //msg += "#L3#  #b출석 코인#k상점#l\r\n\r\n";
                               // msg += "#L2#  #b사냥 코인#k상점#l\r\n";
                                //msg += "#L4#  #b유니온 코인 #k상점#l\r\n";
                                //msg += "#L7#  #b네오 스톤 #k상점#l\r\n";
                               // msg += "#L1#  #b네오 젬 (휴식) #k상점#l\r\n";
                                //msg += "#L8#  #b보스 #k상점#l"+enter+enter;
				//msg += "#L5#선택지5"+enter;
				//cm.sendSimple(msg);
						cm.dispose();
						cm.openShop(6); //캐시상점
					break;
			case 5:
				//var msg = "#fs11#서포터를 위한 상점입니다 서포터 관련은 #b#e디스코드#k#n에 자세히   나와있습니다."+enter+enter;
                                //msg += "#L3# #b후원 #k상점\r\n";
                                //msg += "#L6# #b홍보 #k상점\r\n"+enter;
				//cm.sendSimple(msg);
						cm.dispose();
						cm.openShop(9062459);
					break;
			case 6:
						cm.dispose();
						cm.openShop(9001213);
					break;
					case 7:
						cm.dispose();
						cm.openShop(9010107);
					break;
					case 8:
						cm.dispose();
						cm.openShop(18);
					break;
					case 9:
						cm.dispose();
						cm.openShop(2);
					break;
					case 10:
						cm.dispose();
						cm.openShop(1302011);
					break;
					case 11:
						cm.dispose();
						cm.openNpc(9001048);
					break;
					case 12:
						cm.dispose();
						cm.openNpc(3001850);
					break;
					case 13:
						cm.dispose();
						cm.openShop(1302011);
					break;
					case 14:
						cm.dispose();
						cm.openShop(3);
					break;
					case 15:
						cm.dispose();
						cm.openShop(1540894);
					break;
					case 16:
						cm.dispose();
						cm.openShop(4);
					break;
					case 17:
						cm.dispose();
						cm.openShop(5);
					break;
					case 18:
						cm.dispose();
						cm.openNpc(2155009);
					break;
					case 19:
						cm.dispose();
						cm.openNpc(1540893);
					break;
					case 20:
						cm.dispose();
						cm.openNpc(3003105);
					break;
					case 21:
						cm.dispose();
						cm.openNpc(3003536);
					break;
					case 22:
						cm.dispose();
						cm.openShop(20);
					break;
					case 23:
						cm.dispose();
						cm.openShop(9001212);
					break;
		}
	} else if (status == 2) {
		seld2 = sel;
		switch (seld) {
			case 1:
				switch (sel) {
					case 1:
						cm.dispose();
						cm.openShop(1);
					break;
					case 2:
						cm.dispose();
						cm.openNpc(2008);
					break;
					case 3:
						cm.dispose();
						cm.openNpc(2008);
					break;
					case 4:
						cm.dispose();
						cm.openNpc(2008);
					break;
					case 5:
						cm.dispose();
						cm.openNpc(2008);
					break;
					case 6:
						cm.dispose();
						cm.openNpc(2008);
					break;
					case 7:
						cm.dispose();
						cm.openNpc(2008);
					break;
					case 8:
						cm.dispose();
						cm.openNpc(2008);
					break;
					case 9:
						cm.dispose();
						cm.openNpc(2008);
					break;
				}
			break;
			case 2:
				switch (sel) {
					case 1:
						cm.dispose();
						cm.openShop(15);
					break;
					case 2:
						cm.dispose();
						cm.openShop(9);
					break;
					case 3:
						cm.dispose();
						cm.openShop(3);
					break;
					case 4:
						cm.dispose();
						cm.openShop(4);
					break;
					case 6:
						cm.dispose();
						cm.openShop(5);
                                        break;
					case 5:
						cm.dispose();
						cm.openShop(2);
					break;
					case 6:
						cm.dispose();
						cm.openShop(9);
					break;
					case 7:
						cm.dispose();
						cm.openShop(1011000);
					break;
					case 8:
						cm.dispose();
						cm.openShop(1064003);
					break;
					case 9:
						cm.dispose();
						cm.openShop(9031015);
					break;
					case 10:
						cm.dispose();
						cm.openShop(1540894);
					break;
				}
			break;
			case 3:
				switch (sel) {
					case 1:
						cm.dispose();
						cm.openNpc(1530330);
					break;
					case 2:
						cm.dispose();
						cm.openShop(7);
					break;
					case 3:
						cm.dispose();
						cm.openShop(6);
					break;
					case 4:
						cm.dispose();
						cm.openShop(9031003);
					break;
					case 5:
						cm.dispose();
						cm.openShop(1540859);
					break;
				}
			break;
			case 4:
				switch (sel) {
					case 1:
						cm.dispose();
						cm.openShop(9001213);
					break;
					case 2:
						cm.dispose();
						cm.openShop(1302011);
					break;
					case 3:
						cm.dispose();
						cm.openShop(18);
					break;
					case 4:
						cm.dispose();
						cm.openShop(9010107);
					break;
					case 5:
						cm.dispose();
 	                                        cm.openShop(9074100);
                                        break;
					case 6:
						cm.dispose();
 	                                        cm.openNpc(9000423);
					break;
					case 7:
						cm.dispose();
						cm.openShop(9062459);
					break;
					case 8:
						cm.dispose();
						cm.openNpc(9074200);
					break;
				}
			break;
			case 5:
				switch (sel) {
					case 1:
						cm.dispose();
						cm.openShop(8);
					break;
					case 2:
						cm.dispose();
						cm.openShop(1302011);
					break;
					case 3:
						cm.dispose();
						cm.openNpc(9000216);
					break;
					case 4:
						cm.dispose();
						cm.openNpc(1540121);
					break;
					case 5:
						cm.dispose();
 	                                        cm.openShop(9074100);
                                        break;
					case 6:
						cm.dispose();
						cm.openNpc(3001850);
					break;
					case 7:
						cm.dispose();
						cm.openShop(9073002);
					break;
					case 8:
						cm.dispose();
						cm.openNpc(9074200);
					break;
				}
			break;
		}
	} else if (status == 3) {
		if (seld == 4 && seld2 == 3) {
			switch (sel) { 
				case 1:
					cm.dispose();
					cm.openShop(10);
				break;
				case 2:
					cm.dispose();
					cm.openNpc(10);
				break;
				case 3:
					cm.dispose();
					cm.openNpc(10);
				break;
				case 4:
					cm.dispose();
					cm.openNpc(10);
				break;
				case 5:
					cm.dispose();
					cm.openNpc(10);
				break;
			}
		}
	}
}