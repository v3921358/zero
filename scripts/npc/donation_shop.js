importPackage(Packages.server);
importPackage(Packages.client.inventory);
importPackage(Packages.constants);
importPackage(java.lang);
importPackage(java.io);
importPackage(Packages.packet.creators);
importPackage(Packages.client.items);
importPackage(Packages.server.items);
importPackage(Packages.launch.world);
importPackage(Packages.main.world);
importPackage(Packages.database);
importPackage(java.lang);
importPackage(Packages.server);
importPackage(Packages.handling.world);
importPackage(Packages.tools.packet);

별파 = "#fUI/GuildMark.img/Mark/Pattern/00004001/11#"
별노 = "#fUI/GuildMark.img/Mark/Pattern/00004001/3#"
별흰 = "#fUI/GuildMark.img/Mark/Pattern/00004001/15#"
별갈 = "#fUI/GuildMark.img/Mark/Pattern/00004001/5#"
별빨 = "#fUI/GuildMark.img/Mark/Pattern/00004001/1#"
별검 = "#fUI/GuildMark.img/Mark/Pattern/00004001/16#"
별보 = "#fUI/GuildMark.img/Mark/Pattern/00004001/13#"
별 = "#fUI/FarmUI.img/objectStatus/star/whole#";
S = "#fUI/CashShop.img/CSEffect/today/0#"
데미지 = "22170075"
횟수 = "22140020"
dd = Math.floor(Math.random() * 5) + 3 // 최소 10 최대 35 , 혼테일

importPackage(Packages.constants);

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

	              
                                 var a = "#fs11##fc0xFFFF3366##h0# #fc0xFF000000#님의 도네이션 포인트 : #fc0xFFFF3366#"+cm.getPlayer().getDonationPoint()+" P#k#n\r\n"; 
		 a += "#L1026##i4001760# #d#z4001760##l#k\r\n               #fc0xFF000000#도네이션 포인트#k #e#fc0xFFFF3366#30000 P#k#n\r\n";
		 a += "#L1017##i2630648# #d#z2630648##l#k\r\n               #fc0xFF000000#도네이션 포인트#k #e#fc0xFFFF3366#60000 P#k#n\r\n";
		 a += "#L1002##i5068305# #d#z5068305##l#k\r\n               #fc0xFF000000#도네이션 포인트#k #e#fc0xFFFF3366#4900 P#k#n\r\n";
		 a += "#L1003##i5068305# #d#z5068305# 10개#l#k\r\n               #fc0xFF000000#도네이션 포인트#k #e#fc0xFFFF3366#49,000 P#k#n\r\n";
		 a += "#L1004##i2630127# #d#z2630127##l#k\r\n               #fc0xFF000000#도네이션 포인트#k #e#fc0xFFFF3366#15,000 P#k#n\r\n";
		 a += "#L1012##i2049376# #d#z2049376##l#k\r\n               #fc0xFF000000#도네이션 포인트#k #e#fc0xFFFF3366#9,900 P#k#n\r\n";
		 a += "#L1013##i2048753# #d#z2048753# 30개#l#k\r\n               #fc0xFF000000#도네이션 포인트#k #e#fc0xFFFF3366#9900 P#k#n\r\n";
		 a += "#L1014##i5062005# #d#z5062005# 10개#l#k\r\n               #fc0xFF000000#도네이션 포인트#k #e#fc0xFFFF3366#9900 P#k#n\r\n";
		 a += "#L1015##i5062503# #d#z5062503# 10개#l#k\r\n               #fc0xFF000000#도네이션 포인트#k #e#fc0xFFFF3366#9900 P#k#n\r\n";
		 a += "#L1005##i2046076# #d#z2046076##l#k\r\n               #fc0xFF000000#도네이션 포인트#k #e#fc0xFFFF3366#3000 P#k#n\r\n";
		 a += "#L1006##i2046077# #d#z2046077##l#k\r\n               #fc0xFF000000#도네이션 포인트#k #e#fc0xFFFF3366#3000 P#k#n\r\n";
		 a += "#L1007##i2046150# #d#z2046150##l#k\r\n               #fc0xFF000000#도네이션 포인트#k #e#fc0xFFFF3366#3000 P#k#n\r\n";
		 a += "#L1008##i2046340# #d#z2046340##l#k\r\n               #fc0xFF000000#도네이션 포인트#k #e#fc0xFFFF3366#3000 P#k#n\r\n";
		 a += "#L1009##i2046341# #d#z2046341##l#k\r\n               #fc0xFF000000#도네이션 포인트#k #e#fc0xFFFF3366#3000 P#k#n\r\n";
		 a += "#L1010##i2048047# #d#z2048047##l#k\r\n               #fc0xFF000000#도네이션 포인트#k #e#fc0xFFFF3366#3000 P#k#n\r\n";
		 a += "#L1011##i2048048# #d#z2048048##l#k\r\n               #fc0xFF000000#도네이션 포인트#k #e#fc0xFFFF3366#3000 P#k#n\r\n";
		 a += "#L1019##i2046251# #d#z2046251##l#k\r\n               #fc0xFF000000#도네이션 포인트#k #e#fc0xFFFF3366#3000 P#k#n\r\n";
		 a += "#L1016##i4034803# #d#z4034803##l#k\r\n               #fc0xFF000000#도네이션 포인트#k #e#fc0xFFFF3366#5000 P#k#n\r\n";
		 a += "#L1020##i2632130# #d#z2632130##l#k\r\n               #fc0xFF000000#도네이션 포인트#fc0xFF000000# #e#fc0xFFFF3366#20000 P#k#n\r\n               #r강화비용 별도\r\n";
		 a += "#L1021##i2632131# #d#z2632131##l#k\r\n               #fc0xFF000000#도네이션 포인트#fc0xFF000000# #e#fc0xFFFF3366#20000 P#k#n\r\n               #r강화비용 별도\r\n";
		 a += "#L1022##i2632132# #d#z2632132##l#k\r\n               #fc0xFF000000#도네이션 포인트#fc0xFF000000# #e#fc0xFFFF3366#20000 P#k#n\r\n               #r강화비용 별도\r\n";
		 a += "#L1023##i2632133# #d#z2632133##l#k\r\n               #fc0xFF000000#도네이션 포인트#fc0xFF000000# #e#fc0xFFFF3366#20000 P#k#n\r\n               #r강화비용 별도\r\n";
		 a += "#L1024##i2632134# #d#z2632134##l#k\r\n               #fc0xFF000000#도네이션 포인트#fc0xFF000000# #e#fc0xFFFF3366#20000 P#k#n\r\n               #r강화비용 별도\r\n";
		 a += "#L1025##i2632135# #d#z2632135##l#k\r\n               #fc0xFF000000#도네이션 포인트#fc0xFF000000# #e#fc0xFFFF3366#20000 P#k#n\r\n               #r강화비용 별도\r\n";
		////a += "#L1099##i5062005# #d어메이징 미라클 큐브#k , #b후원 포인트#k #e#r120000 P#k#n\r\n";
		////a += "#L1100##i5062005# #d어메이징 미라클 큐브 10개#k , #b후원 포인트#k #e#r1200000 P#k#n\\n";
		////a += "#L1006##i5068300# #d위습의 원더베리 10개#k , #b후원 포인트#k #e#r90000 P#k#n\r\n";
		////a += "#L1007##i5069100# #d루나 크리스탈#k , #b후원 포인트#k #e#r9000 P#k#n\r\n";
		////a += "#L1009##i5069100# #d루나 크리스탈 5개#k , #b후원 포인트#k #e#r40000 P#k#n\r\n";
		////a += "#L1090##i2435748# #d랜덤 스캐 상자 (미옵션)#k , #b후원 포인트#k #e#r3000 P#k#n\r\n";
		////a += "#L1091##i2435748# #d랜덤 스캐 상자 (미옵션) 5개#k , #b후원 포인트#k #e#r15000 P#k#n\r\n";
		////a += "#L1092##i2437158# #d선택 스캐 상자 (옵션)#k , #b후원 포인트#k #e#r100000 P#k#n\r\n";
		////a += "#L1093##i2437158# #d선택 스캐 상자 (옵션) 5개#k , #b후원 포인트#k #e#r500000 P#k#n\r\n";
		//a += " #L1010##i2439653# #d영환불 패키지#k , #b후원 포인트#k #e#r20000 P#k#n\r\n";
		//a += " #L1022##i2630755# #d강환불 패키지#k , #b후원 포인트#k #e#r12000 P#k#n\r\n";
		//a += " #L1023##i2437157# #d선택 스캐 상자2#k , #b후원 포인트#k #e#r100000 P#k#n\r\n";
		//a += " #L1024##i2437157# #d선택 스캐 상자2 5개#k , #b후원 포인트#k #e#r500000 P#k#n\r\n";
		//a += " #L1025##i2630551# #d블랙 큐브 100개 패키지#k , #b후원 포인트#k #e#r15000 P#k#n\r\n";
		//a += " #L1026##i2049704# #d레전드리 잠재 5개#k , #b후원 포인트#k #e#r20000 P#k#n\r\n";
		//a += "#L1040##i2048753# #d검은 환생의 불꽃 10개#k , #b후원 포인트#k #e#r30000 P#k#n\r\n";
		//a += "#L1011##i2046340# #d[후원] 악공 주문서5개#k , #b후원 포인트#k #e#r25000 P#k#n\r\n";
		//a += "#L1027##i2046341# #d[후원] 악마 주문서5개#k , #b후원 포인트#k #e#r25000 P#k#n\r\n";
		//a += "#L1012##i2046076# #d[후원] 한공 주문서3개#k , #b후원 포인트#k #e#r30000 P#k#n\r\n";
		//a += "#L1013##i2046077# #d[후원] 한마 주문서3개#k , #b후원 포인트#k #e#r30000 P#k#n\r\n";
		//a += "#L1014##i2046150# #d[후원] 두공 주문서3개#k , #b후원 포인트#k #e#r30000 P#k#n\r\n";
		//a += "#L1015##i2048047# #d[후원] 펫장비 공격력3개#k , #b후원 포인트#k #e#r20000 P#k#n\r\n";
		//a += "#L1016##i2048048# #d[후원] 펫장비 마력3개#k , #b후원 포인트#k #e#r20000 P#k#n\r\n";
		////a += "#L1017##i2470007# #d황금망치 100%#k , #b후원 포인트#k #e#r5000 P#k#n\r\n";
		////a += "#L1018##i2450042# #d경험치 2배 쿠폰#k , #b후원 포인트#k #e#r3000 P#k#n\r\n";
		////a += "#L1019##i2450163# #d경험치 3배 쿠폰#k , #b후원 포인트#k #e#r6000 P#k#n\r\n";
		////a += "#L1050##i2450163# #d경험치 3배 쿠폰 10개#k , #b후원 포인트#k #e#r60000 P#k#n\r\n";
		////a += "#L1051##i2450163# #d경험치 3배 쿠폰 50개#k , #b후원 포인트#k #e#r300000 P#k#n\r\n";
		//a += "#L1037##i5121060# #d경험치 뿌리기 3개#k , #b후원 포인트#k #e#r10000 P#k#n\r\n";
		//a += "#L1036##i4034803# #d닉네임 변경권#k , #b후원 포인트#k #e#r30000 P#k#n\r\n";
		////a += "#L1029##i5068302# #d에픽 골드애플 10개#k , #b후원 포인트#k #e#r30000 P#k#n\r\n";
		////a += "#L1030##i5068302# #d에픽 골드애플 50개#k , #b후원 포인트#k #e#r135000 P#k#n\r\n";
//		////a += "#L1031##i5068303# #d유니크 골드애플 10개#k , #b후원 포인트#k #e#r50000 P#k#n\r\n";
//		////a += "#L1032##i5068303# #d유니크 골드애플 50개#k , #b후원 포인트#k #e#r225000 P#k#n\r\n";
		////a += "#L1033##i5068304# #d레전드리 골드애플 10개#k , #b후원 포인트#k #e#r200000 P#k#n\r\n";
		////a += "#L1034##i5068304# #d레전드리 골드애플 50개#k , #b후원 포인트#k #e#r900000 P#k#n\r\n";      
		////a += "#L1035##i2439614# #d후원 패키지#k , #b후원 포인트#k #e#r1500000P#k#n\r\n";
		//a += "#L1042##i4032510# #d다이아 가위#k , #b후원 포인트#k #e#r100000P#k#n\r\n";
		//a += "#L1043##i4032510# #e#d다이아 가위 교환하기#k#n\r\n";
		cm.sendSimple(a);
	        } else if (selection == 3621) {
		cm.dispose();
		cm.openNpc(1012101);

	        } else if (selection == 3623) {
		cm.dispose();
		cm.openNpc(9000268);


	        } else if (selection == 1043) {
		cm.dispose();
		cm.openNpc(1530132);


	        } else if (selection == 1094) {
		cm.dispose();
		cm.openNpc(2002000);


	        } else if (selection == 3624) {
		cm.dispose();
		cm.openNpc(9000269);

	        } else if (selection == 3625) {
		cm.dispose();
		cm.openNpc(9000219);

	        } else if (selection == 3626) {
		cm.dispose();
		cm.openNpc(1530141);

                     } else if (selection == 3622) {
		cm.dispose();
		cm.openNpc(9001010);

		} else if (selection == 1000) {
		if (cm.getPlayer().getDonationPoint() >= 10000) {
		        cm.sendOk("#fs11##b#d홈페이지 고객센터로 양식에 맞춰 작성해주세요.\r\n#d운영진이 수동으로 포인트 차감후 닉네임을 변경해드립니다.\r\n\r\n#b캐릭터 닉네임 : \r\n변경할 닉네임 : \r\n");
		        cm.dispose();
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1001) {
		if (cm.getPlayer().getDonationPoint() >= 30000) {
		        cm.sendOk("#fs11##b#d홈페이지 고객센터로 양식에 맞춰 작성해주세요.\r\n#d운영진이 수동으로 포인트 차감후 직업을 변경해드립니다.\r\n\r\n#b캐릭터 닉네임 : \r\n변경할 직업 : \r\n");
		        cm.dispose();
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}




		} else if (selection == 1002) {
		if (cm.getPlayer().getDonationPoint() >= 4900) {
		    if (cm.canHold(5068305)) {
			cm.getPlayer().gainDonationPoint(-4900);
			cm.gainItem(5068305, 1);
		        cm.sendOk("#b후원 포인트#k 로 #i5068305# #r 1 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r캐시 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1003) {
		if (cm.getPlayer().getDonationPoint() >= 49000) {
		    if (cm.canHold(5068305)) {
			cm.getPlayer().gainDonationPoint(-49000);
			cm.gainItem(5068305, 10);
		        cm.sendOk("#b후원 포인트#k 로 #i5068305# #r 10 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r캐시 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1004) {
		if (cm.getPlayer().getDonationPoint() >= 15000) {
		    if (cm.canHold(2630127)) {
			cm.getPlayer().gainDonationPoint(-15000);
			cm.gainItem(2630127, 1);
		        cm.sendOk("#b후원 포인트#k 로 #i2630127# #r 1 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r캐시 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1099) {
		if (cm.getPlayer().getDonationPoint() >= 120000) {
		    if (cm.canHold(5062005)) {
			cm.getPlayer().gainDonationPoint(-120000);
			cm.gainItem(5062005, 1);
		        cm.sendOk("#b후원 포인트#k 로 #i5062005# #r 1 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1005) {
		if (cm.getPlayer().getDonationPoint() >= 3000) {
		    if (cm.canHold(2046076)) {
			cm.getPlayer().gainDonationPoint(-3000);
			cm.gainItem(2046076, 1);
		        cm.sendOk("#b후원 포인트#k 로 #i2046076# #r 1 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1006) {
		if (cm.getPlayer().getDonationPoint() >= 3000) {
		    if (cm.canHold(2046077)) {
			cm.getPlayer().gainDonationPoint(-3000);
			cm.gainItem(2046077, 1);
		        cm.sendOk("#b후원 포인트#k 로 #i2046077# #r 1 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1007) {
		if (cm.getPlayer().getDonationPoint() >= 3000) {
		    if (cm.canHold(2046150)) {
			cm.getPlayer().gainDonationPoint(-3000);
			cm.gainItem(2046150, 1);
		        cm.sendOk("#b후원 포인트#k 로 #i2046150# #r 1 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1008) {
		if (cm.getPlayer().getDonationPoint() >= 3000) {
		    if (cm.canHold(2046340)) {
			cm.getPlayer().gainDonationPoint(-3000);
			cm.gainItem(2046340, 1);
		        cm.sendOk("#b후원 포인트#k 로 #i2046340# #r 1 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1009) {
		if (cm.getPlayer().getDonationPoint() >= 3000) {
		    if (cm.canHold(2046341)) {
			cm.getPlayer().gainDonationPoint(-3000);
			cm.gainItem(2046341, 1);
		        cm.sendOk("#b후원 포인트#k 로 #i2046341# #r 1 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1010) {
		if (cm.getPlayer().getDonationPoint() >= 3000) {
		    if (cm.canHold(2048047)) {
			cm.getPlayer().gainDonationPoint(-3000);
			cm.gainItem(2048047, 1);
		        cm.sendOk("#b후원 포인트#k 로 #i2048047# 1 개 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1011) {
		if (cm.getPlayer().getDonationPoint() >= 3000) {
		    if (cm.canHold(2048048)) {
			cm.getPlayer().gainDonationPoint(-3000);
			cm.gainItem(2048048, 1);
		        cm.sendOk("#b후원 포인트#k 로 #i2048048# #r 1 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1012) {
		if (cm.getPlayer().getDonationPoint() >= 9900) {
		    if (cm.canHold(2049376)) {
			cm.getPlayer().gainDonationPoint(-9900);
			cm.gainItem(2049376, 1);
		        cm.sendOk("#b후원 포인트#k 로 #i2049376# #r 1 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1013) {
		if (cm.getPlayer().getDonationPoint() >= 9900) {
		    if (cm.canHold(2048753)) {
			cm.getPlayer().gainDonationPoint(-9900);
			cm.gainItem(2048753, 30);
		        cm.sendOk("#b후원 포인트#k 로 #i2048753# #r 30 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}



		} else if (selection == 1014) {
		if (cm.getPlayer().getDonationPoint() >= 9900) {
		    if (cm.canHold(5062005)) {
			cm.getPlayer().gainDonationPoint(-9900);
			cm.gainItem(5062005, 10);
		        cm.sendOk("#b후원 포인트#k 로 #i5062005# #r 10 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r캐시 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1015) {
		if (cm.getPlayer().getDonationPoint() >= 9900) {
		    if (cm.canHold(5062503)) {
			cm.getPlayer().gainDonationPoint(-9900);
			cm.gainItem(5062503, 10);
		        cm.sendOk("#b후원 포인트#k 로 #i5062503# #r 10 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r캐시 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}


		} else if (selection == 1016) {
		if (cm.getPlayer().getDonationPoint() >= 5000) {
		    if (cm.canHold(4034803)) {
			cm.getPlayer().gainDonationPoint(-5000);
			cm.gainItem(4034803, 1);
		        cm.sendOk("#b후원 포인트#k 로 #i4034803# #r 1 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r기타 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1017) {
		if (cm.getPlayer().getDonationPoint() >= 60000) {
		    if (cm.canHold(2630648)) {
			cm.getPlayer().gainDonationPoint(-60000);
			cm.gainItem(2630648, 1);
		        cm.sendOk("#b후원 포인트#k 로 #i2630648# #r 1 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1018) {
		if (cm.getPlayer().getDonationPoint() >= 25000) {
		    if (cm.canHold(4031342)) {
			cm.getPlayer().gainDonationPoint(-25000);
			cm.gainItem(4031342, 1);
		        cm.sendOk("#b후원 포인트#k 로 #i4031342# #r 1 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r기타 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1019) {
		if (cm.getPlayer().getDonationPoint() >= 3000) {
		    if (cm.canHold(2046251)) {
			cm.getPlayer().gainDonationPoint(-3000);
                                    cm.gainItem(2046251, 1);
		        cm.sendOk("#b후원 포인트#k 로 #i2046251# #r 1 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

                } else if (selection == 1050) {
		if (cm.getPlayer().getDonationPoint() >= 60000) {
		    if (cm.canHold(2450163)) {
			cm.getPlayer().gainDonationPoint(-60000);
			cm.gainItem(2450163, 10);
		        cm.sendOk("#b후원 포인트#k 로 #i2450163# #r 10 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

                } else if (selection == 1051) {
		if (cm.getPlayer().getDonationPoint() >= 300000) {
		    if (cm.canHold(2450163)) {
			cm.getPlayer().gainDonationPoint(-300000);
			cm.gainItem(2450163, 50);
		        cm.sendOk("#b후원 포인트#k 로 #i2450163# #r 50 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}


	        } else if (selection == 1020) {
		if (cm.getPlayer().getDonationPoint() >= 20000) {
		    if (cm.canHold(2632130)) {
			cm.getPlayer().gainDonationPoint(-20000);
			cm.gainItem(2632130, 1);
		        cm.sendOk("#b후원 포인트#k 로 #i2632130# #r 1개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}
		} else if (selection == 1021) {
		if (cm.getPlayer().getDonationPoint() >= 20000) {
		    if (cm.canHold(2632131)) {
			cm.getPlayer().gainDonationPoint(-20000);
			cm.gainItem(2632131, 1);
		        cm.sendOk("#b후원 포인트#k 로 #i2632131# #r 1개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1022) {
		if (cm.getPlayer().getDonationPoint() >= 20000) {
		    if (cm.canHold(2632132)) {
			cm.getPlayer().gainDonationPoint(-20000);
			cm.gainItem(2632132, 1);
		        cm.sendOk("#b후원 포인트#k 로 #i2632132# #r 1개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1023) {
		if (cm.getPlayer().getDonationPoint() >= 20000) {
		    if (cm.canHold(2632133)) {
			cm.getPlayer().gainDonationPoint(-20000);
			cm.gainItem(2632133, 1);
		        cm.sendOk("#b후원 포인트#k 로 #i2632133# #r 1개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r장비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1024) {
		if (cm.getPlayer().getDonationPoint() >= 20000) {
		    if (cm.canHold(2632134)) {
			cm.getPlayer().gainDonationPoint(-20000);
			cm.gainItem(2632134, 1);
		        cm.sendOk("#b후원 포인트#k 로 #i2632134# #r 1개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r장비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1025) {
		if (cm.getPlayer().getDonationPoint() >= 20000) {
		    if (cm.canHold(2632135)) {
			cm.getPlayer().gainDonationPoint(-20000);
			cm.gainItem(2632135, 1);
		        cm.sendOk("#b후원 포인트#k 로 #i2632135# #r 1개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r장비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1026) {
		if (cm.getPlayer().getDonationPoint() >= 30000) {
		    if (cm.canHold(4001760)) {
			cm.getPlayer().gainDonationPoint(-30000);
			cm.gainItem(4001760, 1);
		        cm.sendOk("#b후원 포인트#k 로 #i4001760# #r 1개#k를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1027) {
		if (cm.getPlayer().getDonationPoint() >= 25000) {
		    if (cm.canHold(2049341)) {
			cm.getPlayer().gainDonationPoint(-25000);
			cm.gainItem(2046341, 5);
		        cm.sendOk("#b후원 포인트#k 로 #i2046341# #r 1 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

	        } else if (selection == 1028) {
			cm.dispose();
			cm.openNpcCustom(cm.getClient(), 3003273, "cashItemsearch");


		} else if (selection == 1029) {
		if (cm.getPlayer().getDonationPoint() >= 30000) {
		    if (cm.canHold(5068302)) {
			cm.getPlayer().gainDonationPoint(-30000);
			cm.gainItem(5068302, 10);
		        cm.sendOk("#b후원 포인트#k 로 #i5068302# #r 10 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1030) {
		if (cm.getPlayer().getDonationPoint() >= 135000) {
		    if (cm.canHold(5068302)) {
			cm.getPlayer().gainDonationPoint(-135000);
			cm.gainItem(5068302, 50);
		        cm.sendOk("#b후원 포인트#k 로 #i5068302# #r 50 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1031) {
		if (cm.getPlayer().getDonationPoint() >= 50000) {
		    if (cm.canHold(5068303)) {
			cm.getPlayer().gainDonationPoint(-50000);
			cm.gainItem(5068303, 10);
		        cm.sendOk("#b후원 포인트#k 로 #i5068303# #r 10 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1032) {
		if (cm.getPlayer().getDonationPoint() >= 225000) {
		    if (cm.canHold(5068303)) {
			cm.getPlayer().gainDonationPoint(-225000);
			cm.gainItem(5068303, 50);
		        cm.sendOk("#b후원 포인트#k 로 #i5068303# #r 10 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1033) {
		if (cm.getPlayer().getDonationPoint() >= 200000) {
		    if (cm.canHold(5068304)) {
			cm.getPlayer().gainDonationPoint(-200000);
			cm.gainItem(5068304, 10);
		        cm.sendOk("#b후원 포인트#k 로 #i5068304# #r 1 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1034) {
		if (cm.getPlayer().getDonationPoint() >= 900000) {
		    if (cm.canHold(5068304)) {
			cm.getPlayer().gainDonationPoint(-900000);
			cm.gainItem(5068304, 50);
		        cm.sendOk("#b후원 포인트#k 로 #i5068304# #r 1 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1035) {
		if (cm.getPlayer().getDonationPoint() >= 1500000) {
		    if (cm.canHold(5068304)) {
			cm.getPlayer().gainDonationPoint(-1500000);
			cm.gainItem(5068304, 150);
			cm.gainItem(2439614, 1);
			cm.gainItem(2630127, 1);
			cm.gainItem(2432408, 2);
			cm.gainItem(2591659, 2);
			cm.gainItem(1802653, 1);
			cm.gainItem(4031868, 1);
			cm.gainItem(4036531, 1);
			cm.gainItem(2437553, 1);
			cm.gainItem(2431486, 5);
			cm.gainItem(2439653, 10);
			cm.gainItem(2630756, 10);
			cm.gainItem(2630551, 10);
			cm.gainItem(4001716, 15);
		        cm.sendOk("#b후원 포인트#k 로 #i5068304# #r 1 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

} else if (selection == 1037) {
		if (cm.getPlayer().getDonationPoint() >= 10000) {
		    if (cm.canHold(5121060)) {
			cm.getPlayer().gainDonationPoint(-10000);
			cm.gainItem(5121060, 3);
		        cm.sendOk("#b후원 포인트#k 로 #i5121060# #r 3 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1036) {
		if (cm.getPlayer().getDonationPoint() >= 30000) {
		    if (cm.canHold(4034803)) {
			cm.getPlayer().gainDonationPoint(-30000);
			cm.gainItem(4034803, 1);
		        cm.sendOk("#b후원 포인트#k 로 #i4034803# #r 1 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1040) {
		if (cm.getPlayer().getDonationPoint() >= 30000) {
		    if (cm.canHold(2048753)) {
			cm.getPlayer().gainDonationPoint(-30000);
			cm.gainItem(2048753, 10);
		        cm.sendOk("#b후원 포인트#k 로 #i2439975# #r 10 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1042) {
		if (cm.getPlayer().getDonationPoint() >= 100000) {
		    if (cm.canHold(4032510)) {
			cm.getPlayer().gainDonationPoint(-100000);
			cm.gainItem(4032510, 1);
		        cm.sendOk("#b후원 포인트#k 로 #i4032510# #r 1 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1090) {
		if (cm.getPlayer().getDonationPoint() >= 3000) {
		    if (cm.canHold(4021031)) {
			cm.getPlayer().gainDonationPoint(-3000);
			cm.gainItem(2435748, 1);
		        cm.sendOk("#b후원 포인트#k 로 #i2435748# #r 1 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1091) {
		if (cm.getPlayer().getDonationPoint() >= 15000) {
		    if (cm.canHold(2435748)) {
			cm.getPlayer().gainDonationPoint(-15000);
			cm.gainItem(2435748, 5);
		        cm.sendOk("#b후원 포인트#k 로 #i2435748# #r 5 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1092) {
		if (cm.getPlayer().getDonationPoint() >= 100000) {
		    if (cm.canHold(4021031)) {
			cm.getPlayer().gainDonationPoint(-100000);
			cm.gainItem(2437158, 1);
		        cm.sendOk("#b후원 포인트#k 로 #i2437158# #r 1 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1100) {
		if (cm.getPlayer().getDonationPoint() >= 1200000) {
		    if (cm.canHold(5062005)) {
			cm.getPlayer().gainDonationPoint(-1200000);
			cm.gainItem(5062005, 10);
		        cm.sendOk("#b후원 포인트#k 로 #i5062005# #r 10 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}

		} else if (selection == 1093) {
		if (cm.getPlayer().getDonationPoint() >= 500000) {
		    if (cm.canHold(4021031)) {
			cm.getPlayer().gainDonationPoint(-500000);
			cm.gainItem(2437158, 5);
		        cm.sendOk("#b후원 포인트#k 로 #i2437158# #r 5 개#k 를 구입 하셨습니다.");
			cm.dispose();
		    } else {
		        cm.sendOk("#r소비 칸에 빈 공간이 없습니다.#k");
		        cm.dispose();
		    }
		} else {
		    cm.sendOk("#fs11##b후원 포인트#k 가 부족합니다.");
		    cm.dispose();
		}
		

		}
	}
}