importPackage(Packages.server);
importPackage(java.lang);

var quest = "헤이븐1";
var 제한레벨 = 190;
var enter = "\r\n";
var seld = -1;
var isitem = false;
var isclear;
var year, month, date2, date, day
var rand;

var daily = [ // 첫번째
	{'mobid' : 8250003, 'qty' : 200, 't' : 1}, // t가 1일땐 몹잡기, 2일땐 템구해오기
	{'mobid' : 8250013, 'qty' : 200, 't' : 1},
	{'mobid' : 8250011, 'qty' : 200, 't' : 1},
                {'mobid' : 8250013, 'qty' : 200, 't' : 1}, 
                {'mobid' : 8250011, 'qty' : 200, 't' : 1}
]

var daily2  = [ //두번째
	{'mobid' : 8250003, 'qty' : 200, 't' : 1}, // t가 1일땐 몹잡기, 2일땐 템구해오기
	{'mobid' : 8250013, 'qty' : 200, 't' : 1},
	{'mobid' : 8250011, 'qty' : 200, 't' : 1},
                {'mobid' : 8250013, 'qty' : 200, 't' : 1}, 
                {'mobid' : 8250011, 'qty' : 200, 't' : 1}
]

var reward;



function start() {
	status = -1;
	action(1, 0, 0);
}

function getk(key) {
	return cm.getPlayer().getKeyValue(201801, date+"_"+quest+"_"+key);
}
function setk(key, value) {
	cm.getPlayer().setKeyValue(201801, date+"_"+quest+"_"+key, value);
}

function action(mode, type, sel) {
	if (mode == 1) {
		status++;
	} else {
		cm.dispose();
		return;
    	}
	if (status == 0) {
		if (cm.getPlayer().getLevel() < 제한레벨) {
			cm.sendOk("핫핫핫. 어린 모험가군, 더 성장하면 내 의뢰를 받아도 되겠군. 핫핫핫.");
			cm.dispose();
			return;
		}
		getData();
		switch(day){
    			case 0:
        			var d = "일";
        			reward = [
			{'itemid' : 4009005, 'qty' : 60},
			{'itemid' : 4001842, 'qty' : 24},
			{'itemid' : 2435719, 'qty' : 10}
        			
				]
        		break;
    			case 1:
        			var d = "월";
        			reward = [
			{'itemid' : 4009005, 'qty' : 30},
			{'itemid' : 4001842, 'qty' : 12},
			{'itemid' : 2435719, 'qty' : 5}
				]
        			break;
    			case 2:
        			var d = "화";
        			reward = [
			{'itemid' : 4009005, 'qty' : 30},
			{'itemid' : 4001842, 'qty' : 12},
			{'itemid' : 2435719, 'qty' : 5}
				]
        		break;
    			case 3:
        			var d = "수";
        			reward = [
			{'itemid' : 4009005, 'qty' : 30},
			{'itemid' : 4001842, 'qty' : 12},
			{'itemid' : 2435719, 'qty' : 5}
				]
        		break;
    			case 4:
        			var d = "목";
        			reward = [
			{'itemid' : 4009005, 'qty' : 30},
			{'itemid' : 4001842, 'qty' : 12},
			{'itemid' : 2435719, 'qty' : 5}
				]
        		break;
    			case 5:
        			var d = "금";
        			reward = [
			{'itemid' : 4009005, 'qty' : 30},
			{'itemid' : 4001842, 'qty' : 12},
			{'itemid' : 2435719, 'qty' : 5}
				]
        		break;
    			case 6:
        			var d = "토";
        			reward = [
			{'itemid' : 4009005, 'qty' : 60},
			{'itemid' : 4001842, 'qty' : 24},
			{'itemid' : 2435719, 'qty' : 10}
				]
        		break;
		}

		rand = daily[Randomizer.rand(0, daily.length-1)];
		isitem = rand['t'] == 2 ? true : false;
		isclear = getk("isclear");
		if (isclear == -1) {
			setk("mobid", isitem ? rand['itemid']+"" : rand['mobid']+"");
			setk("mobq", rand['qty']+"");
			setk("isclear", "1");
			setk("isitem", isitem ? "2" : "1");
			setk("count", "0");
		}
		if (isclear == 3) {
			rand = daily2[Randomizer.rand(0, daily2.length-1)];
			isitem = rand['t'] == 2 ? true : false;
			setk("mobid", isitem ? rand['itemid']+"" : rand['mobid']+"");
			setk("mobq", rand['qty']+"");
			setk("isitem", isitem ? "2" : "1");
			setk("count", "0");
			setk("isclear", "4");
		}
		isclear = getk("isclear");
		isitem = getk("isitem") == 2 ? true : false;

		if (isclear == 2 || isclear == 5) {
			if ((isitem && (cm.haveItem(getk("mobid"), getk("mobq")))) || (!isitem && (getk("count") >= getk("mobq")))) {
				if (isitem) cm.gainItem(getk("mobid"), -getk("mobq"));
				var msg = "#b#h ##k, 자네가 꾸준히 도와줘서 일이 훨씬 수월해졌다네."+enter;
				if (isclear == 5) {
					msg += "여기, 이건 그에 대한 보상일세."+enter;
					msg += getReward()+enter;
				}
				msg += "#k앞으로도 잘 부탁하네."+enter;

				if (isclear == 2)
					msg += "내 의뢰를 하나 더 완수하면 일일 보상을 지급한다네.\r\n다시 한번 말을 걸어 주게.";
				else 
					msg += "단, 오늘은 피곤하니 내일 봅세 하하하.";

				if (isclear == 2)
					setk("isclear", "3");
				else if (isclear == 5)
					setk("isclear", "6");

				cm.sendOk(msg);
				cm.dispose();
			}
		}

		if (isclear == 5) {
			if ((isitem && (cm.haveItem(getk("mobid"), getk("mobq")))) || (!isitem && (getk("count") >= getk("mobq")))) {
				if (isitem) cm.gainItem(getk("mobid"), -getk("mobq"));
				setk("isclear", "6");
				var msg = "#b#h ##k, 자네가 꾸준히 도와줘서 일이 훨씬 수월해졌다네."+enter;
				msg += "여기, 이건 그에 대한 보상일세."+enter;
				msg += getReward()+enter;
				msg += "#k앞으로도 잘 부탁하네."+enter;
				msg += "내 의뢰를 하나 더 완수하면 일일 보상을 지급한다네.\r\n다시 한번 말을 걸어 주게.";
				cm.sendOk(msg);
				cm.dispose();
			}
		}
		switch (isclear) {
			case 1:
			case 4:
				var msg = "어서 오시게나, #h #.#d"+enter;
				msg += "#L1#(Lv."+제한레벨+") 헤이븐 의뢰 : 긴급 지원";

				cm.sendSimple(msg);
			break;
			case 2:
			case 5:
				var msg = "처리해줘야 할 의뢰는 아래와 같네."+enter+enter;
				msg += isitem ? "#r[의뢰 : 전리품 수집]#k"+enter : "#r[의뢰 : 적 로봇 처치]#k"+enter;
				msg += isitem ? "목표 전리품 : #b#i"+getk("mobid")+"##z"+getk("mobid")+"# "+getk("mobq")+"개#k"+enter+enter : "처치 대상 : #b#o"+getk("mobid")+"# "+getk("mobq")+"마리#k"+enter+enter;
				msg += "그럼, 일이 끝난 후에 다시 와주게. 핫핫핫.";

				cm.sendOk(msg);
				cm.dispose();
			break;
			case 6:
				var msg = "어서 오시게나, #h #."+enter;
				msg += "오늘은 더 이상 일거리가 없다네. 핫핫핫."+enter;

				cm.sendOk(msg);
				cm.dispose();
			break;
		}
	} else if (status == 1) {
		if (getk("isclear") == 1 || getk("isclear") == 4) {
			var msg = "처리해줘야 할 의뢰는 아래와 같네."+enter+enter;
			msg += isitem ? "#r[의뢰 : 전리품 수집]#k"+enter : "#r[의뢰 : 적 로봇 처치]#k"+enter;
			msg += isitem ? "목표 전리품 : #b#i"+getk("mobid")+"##z"+getk("mobid")+"# "+getk("mobq")+"개#k"+enter+enter : "처치 대상 : #b#o"+getk("mobid")+"# "+getk("mobq")+"마리#k"+enter+enter;
			msg += "어때? 의뢰를 수락할 텐가?";

			cm.sendYesNo(msg);
		}
	} else if (status == 2) {
		var msg = "처리해줘야 할 의뢰는 아래와 같네."+enter+enter;
		msg += isitem ? "#r[의뢰 : 전리품 수집]#k"+enter : "#r[의뢰 : 적 로봇 처치]#k"+enter;
		msg += isitem ? "목표 전리품 : #b#i"+getk("mobid")+"##z"+getk("mobid")+"# "+getk("mobq")+"개#k"+enter+enter : "처치 대상 : #b#o"+getk("mobid")+"# "+getk("mobq")+"마리#k"+enter+enter;
		msg += "그럼, 일이 끝난 후에 다시 와주게. 핫핫핫.";
		cm.sendOk(msg);
		cm.dispose();
		if (getk("isclear") == 1)
			setk("isclear", "2");
		else if (getk("isclear") == 4)
			setk("isclear", "5");
	}
}

function getReward() {
	var msg = "#b";
	for (i = 0; i < reward.length; i++) {
		cm.gainItem(reward[i]['itemid'], reward[i]['qty']);
		cm.getPlayer().AddStarDustPoint(20, cm.getPlayer().getTruePosition());
		msg += "#i"+reward[i]['itemid']+"##z"+reward[i]['itemid']+"# "+reward[i]['qty']+"개"+enter;
	}
	return msg;
}

function getData() {
	time = new Date();
	year = time.getFullYear();
	month = time.getMonth() + 1;
	if (month < 10) {
		month = "0"+month;
	}
	date2 = time.getDate() < 10 ? "0"+time.getDate() : time.getDate();
	date = Integer.parseInt(year+""+month+""+date2);
	day = time.getDay();
}
