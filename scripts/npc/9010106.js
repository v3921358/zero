var enter = "\r\n";
var seld = -1;

var c = 4310229;

var require = -1;
var reqlevel = -1;

var union, main, sub;
var unionStr = "";
var unionNext = "";
var canAdd = -1;
var nextAdd = -1;

var zz = -1;

function start() {
	status = -1;
	action(1, 0, 0);
}

function getRequire() {
	switch (main) {
		case 1:
			require = sub == 1 ? 120 : sub == 2 ? 140 : sub == 3 ? 150 : sub == 4 ? 160 : sub == 5 ? 170 : -1;
		break;
		case 2:
			require = sub == 1 ? 430 : sub == 2 ? 450 : sub == 3 ? 470 : sub == 4 ? 490 : sub == 5 ? 510 : -1;
		break;
		case 3:
			require = sub == 1 ? 930 : sub == 2 ? 960 : sub == 3 ? 1000 : sub == 4 ? 1030 : sub == 5 ? 1060 : -1;
		break;
		case 4:
			require = sub == 1 ? 2200 : sub == 2 ? 2300 : sub == 3 ? 2350 : sub == 4 ? 2400 : -1;
		break;
	}
}

function getUnionStr() {
	var temp = main == 1 ? "노비스" : main == 2 ? "베테랑" : main == 3 ? "마스터" : main == 4 ? "그랜드 마스터" : "";
	return temp + " 유니온 "+sub+"단계";
}

function getUnionNext() {
	var adv = sub == 5;
	if (!adv) {
		nextAdd = (9 * (main)) + (sub + 1);
		var temp = main == 1 ? "노비스" : main == 2 ? "베테랑" : main == 3 ? "마스터" : main == 4 ? "그랜드 마스터" : "";
		return temp + " 유니온 "+(sub + 1)+"단계";
	} else {
		nextAdd = (9 * (main + 1));
		var temp = (main + 1) == 1 ? "노비스" : (main + 1) == 2 ? "베테랑" : (main + 1) == 3 ? "마스터" : (main + 1) == 4 ? "그랜드 마스터" : "";
		return temp + " 유니온 1단계";
	}
}

function getUnion() {
	union = cm.getPlayer().getKeyValue(18771, "rank");
	main = Math.floor(union / 100);
	sub = union % 10;
	reqlevel = (((main == 1 ? 0 : main == 2 ? 1 : main == 3 ? 2 : 3) * 5) + sub) * 500;
	canAdd = (9 * (main)) + sub;
}

function sex() {
	var adv = sub == 5;
	var newunion = "";
	if (adv) {
		newmain = (main + 1);
		newsub = 1;
		newunion = newmain+"0"+newsub;
	} else {
		newsub = (sub + 1);
		newunion = main+"0"+newsub;
	}
	cm.getPlayer().setKeyValue(18771, "rank", newunion);
}

function action(mode, type, sel) {
	if (mode == 1) {
		status++;
	} else {
		cm.dispose();
		return;
    	}
	if (status == 0) {
		getUnion();
		unionStr = getUnionStr();
		unionNext = getUnionNext();
		getRequire();
		var msg = "이것 참 용잡으러 가기 좋은 날이군요!"+enter;
		msg += "어떤 #b메이플 유니온#k 업무를 도와드릴까요?#b"+enter;
		msg += "#L1#<나의 메이플 유니온 정보를 확인한다.>"+enter;
		msg += "#L2#<메이플 유니온 등급을 올린다.>"+enter;
		cm.sendSimpleS(msg, 4);
	} else if (status == 1) {
		seld = sel;
		switch (sel) {
			case 1:
                                zz = 0;
				for (i = 0; i < cm.getClient().getUnions().size(); i++) {
				    if (cm.getClient().getUnions().get(i).getPosition() != -1) {
					zz++;
				    }
				}
				var msg = "용사님의 #e메이플 유니온#n 정보를 알려드릴까요?"+enter+enter;
				msg += "#e메이플 유니온 등급: #b<"+unionStr+">#k"+enter;
				msg += "#e유니온 레벨: #b<" + cm.getPlayer().getAllUnion() + ">#k#n"+enter;
				msg += "#e보유 유니온 캐릭터: #b<" + cm.getClient().getUnions().size() + ">#k#n"+enter;
				msg += "#e공격대원: #b<" + zz + " / " + canAdd + " 명>#k#n";
				cm.sendNextS(msg, 4);
				cm.dispose();
			break;
			case 2:
				if (main == 4 && sub == 5) {
					cm.sendOk("당신은 더 이상 유니온 등급을 올릴 수 없답니다!");
					cm.dispose();
					return;
				}
				var msg = "#e메이플 유니온 승급#n을 하고 싶으신가요?"+enter;
				msg += "#e현재등급: #b<"+unionStr+">#k"+enter;
				msg += "다음등급: #b<"+unionNext+">#k"+enter;
				msg += "승급 시 투입 가능 공격대원 증가: #b<"+canAdd+"→"+nextAdd+" 명>#k#n"+enter+enter;
				msg += "승급을 위해선 아래 조건을 충족하셔야 해요."+enter+enter;
				msg += "#e<유니온 레벨> #r"+reqlevel+"이상#k"+enter;
				msg += "<지불 코인> #b#z"+c+"# "+require+"개#k#n"+enter+enter;
				msg += "지금 메이플 유니온을 #e승급#k 시켜 드릴까요?";
				cm.sendYesNoS(msg, 4);
			break;
		}
	} else if (status == 2) {
		switch (seld) {
			case 1:
			break;
			case 2:
				if (!cm.haveItem(c, require)) {
					cm.sendOk("코인이 모자라신건 아닌가요?");
					cm.dispose();
					return;
				}
				if (cm.getPlayer().getAllUnion() < reqlevel) {
					cm.sendOk("유니온 레벨이 모자라신건 아닌가요?");
					cm.dispose();
					return;
				}
				var msg = "짝짝짝!"+enter;
				msg += "#e메이플 유니온 등급#n이 올랐어요! 이제 더 많은 공격대원과 함께 더욱 빠르게 성장 하실 수 있어요!"+enter+enter;
				msg += "#e신규등급: #e<"+unionNext+">#k"+enter;
				msg += "투입가능 공격대원: #b"+nextAdd+"#k#n"+enter+enter;
				msg += "그럼 다음 등급까지 쭉쭉~ 성장하세요!";
				cm.gainItem(c, -require);
				sex();
				cm.sendOkS(msg, 4);
				cm.dispose();
			break;
		}
	}
}