importPackage(Packages.server);
importPackage(java.lang);

var quest = "소멸의여로";
var 제한레벨 = 200;
var enter = "\r\n";
var seld = -1;
var isitem = false;
var isclear;
var year, month, date2, date, day
var rand;

var daily = [ // 첫번째
	        {'mobid' : 8641000, 'qty' : 300, 't' : 1}, 
                {'mobid' : 8641001, 'qty' : 300, 't' : 1},
                {'mobid' : 8641002, 'qty' : 300, 't' : 1},
                {'mobid' : 8641003, 'qty' : 300, 't' : 1},
                {'mobid' : 8641005, 'qty' : 300, 't' : 1}
]

var daily2  = [ //두번째
	        {'mobid' : 8641000, 'qty' : 300, 't' : 1}, 
                {'mobid' : 8641001, 'qty' : 300, 't' : 1},
                {'mobid' : 8641002, 'qty' : 300, 't' : 1},
                {'mobid' : 8641003, 'qty' : 300, 't' : 1},
                {'mobid' : 8641005, 'qty' : 300, 't' : 1}
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
			cm.sendOk("이 소멸의 여로는 지금까지 겪었던 어떤 공간 보다 위험한 곳 이랍니다.");
			cm.dispose();
			return;
		}
		getData();
		switch(day){
    			case 0:
        			var d = "일";
        			reward = [
			{'itemid' : 1712001, 'qty' : 20},
			{'itemid' : 2437760, 'qty' : 2},
			{'itemid' : 2435719, 'qty' : 20}
        			
				]
        		break;
    			case 1:
        			var d = "월";
        			reward = [
			{'itemid' : 1712001, 'qty' : 10},
			{'itemid' : 2437760, 'qty' : 1},
			{'itemid' : 2435719, 'qty' : 10}
				]
        			break;
    			case 2:
        			var d = "화";
        			reward = [
			{'itemid' : 1712001, 'qty' : 10},
			{'itemid' : 2437760, 'qty' : 1},
			{'itemid' : 2435719, 'qty' : 10}
				]
        		break;
    			case 3:
        			var d = "수";
        			reward = [
			{'itemid' : 1712001, 'qty' : 10},
			{'itemid' : 2437760, 'qty' : 1},
			{'itemid' : 2435719, 'qty' : 10}
				]
        		break;
    			case 4:
        			var d = "목";
        			reward = [
			{'itemid' : 1712001, 'qty' : 10},
			{'itemid' : 2437760, 'qty' : 1},
			{'itemid' : 2435719, 'qty' : 10}
				]
        		break;
    			case 5:
        			var d = "금";
        			reward = [
			{'itemid' : 1712001, 'qty' : 10},
			{'itemid' : 2437760, 'qty' : 1},
			{'itemid' : 2435719, 'qty' : 10}
				]
        		break;
    			case 6:
        			var d = "토";
        			reward = [
			{'itemid' : 1712001, 'qty' : 20},
			{'itemid' : 2437760, 'qty' : 2},
			{'itemid' : 2435719, 'qty' : 20}
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
				var msg = "#b#h 감사합니다.##k님 덕분에 조사가 수월해지겠군요."+enter;
				if (isclear == 5) {
					msg += "노고에 감사드립니다. 자 그에 대한 보상입니다."+enter;
					msg += getReward()+enter;
				}
				msg += "#k앞으로도 잘 부탁드리겠습니다."+enter;

				if (isclear == 2)
					msg += "한 가지 임무를 더 수행하셔야 보상이 지급됩니다. 그럼, 저에게 다시 말을 걸어 임무를 시작해 보도록 해요.";
				else 
					msg += "오늘의 조사는 무사히 끝난 것 같습니다. 내일 뵙도록 하죠.";

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
				var msg = "#b#h 감사합니다.##k님 덕분에 조사가 수월해지겠군요."+enter;
				msg += "노고에 감사드립니다. 자 그에 대한 보상입니다."+enter;
				msg += getReward()+enter;
				msg += "#k앞으로도 잘 부탁드리겠습니다."+enter;
				msg += "한 가지 임무를 수행하셔야 보상이 지급됩니다. 그럼, 저에게 다시 말을 걸어 임무를 시작해 보도록 해요.";
				cm.sendOk(msg);
				cm.dispose();
			}
		}
		switch (isclear) {
			case 1:
				var msg = "이곳을 조사하는 것이 여기 있는 저희 시간의 신관들의 일이죠."+enter;
				msg += "#fUI/UIWindow2.img/UtilDlgEx/list1#\r\n#d#L1#(Lv."+제한레벨+") #b#e[일일 퀘스트]#n#k #d소멸의 여로 조사";

				cm.sendSimple(msg);
			break;
			case 2:
				var msg = "잘 오셨어요. #h #님, 오늘 #h #님에게 부탁드릴 일은 아래와 같습니다."+enter+enter;
				msg += "#fUI/UIWindow2.img/UtilDlgEx/list0#\r\n#r#e[임무 : 조사]#n#k"+enter;
				msg += "처치 대상 : #b#o"+getk("mobid")+"# "+getk("mobq")+"마리#k"+enter+enter;
				msg += "그럼 임무를 완수하고 돌아와 주십시오.";

				cm.sendOk(msg);
				cm.dispose();
			break;
			case 4:
				var msg = "이곳을 조사하는 것이 여기 있는 저희 시간의 신관들의 일이죠."+enter;
				msg += "#fUI/UIWindow2.img/UtilDlgEx/list1#\r\n#d#L1#(Lv."+제한레벨+") #b#e[일일 퀘스트]#n#k #d소멸의 여로 조사";

				cm.sendSimple(msg);
			break;
			case 5:
				var msg = "잘 오셨어요. #h #님, 오늘 #h #님에게 부탁드릴 일은 아래와 같습니다."+enter+enter;
				msg += "#fUI/UIWindow2.img/UtilDlgEx/list0#\r\n#r#e[임무 : 소멸의 여로조사]#n#k"+enter;
				msg += "\r\n목표 전리품 : #b#i"+getk("mobid")+"##z"+getk("mobid")+"# "+getk("mobq")+"개#k"+enter+enter;
				msg += "그럼 임무를 완수하고 돌아와 주십시오.";

				cm.sendOk(msg);
				cm.dispose();
			break;
			case 6:
				var msg = "어서오십시오. #h #."+enter;
				msg += "오늘의 조사는 무사히 끝난 것 같습니다. 내일 뵙도록 하죠."+enter;

				cm.sendOk(msg);
				cm.dispose();
			break;
		}
	} else if (status == 1) {
		if (getk("isclear") == 1 || getk("isclear") == 4) {
			var msg = "잘 오셨어요. #h #님, 오늘 #h #님에게 부탁드릴 일은 아래와 같습니다."+enter+enter;
			msg += isitem ? "#r#e[임무 : 소멸의 여로조사]#n#k"+enter : "#fUI/UIWindow2.img/UtilDlgEx/list0#\r\n#r#e[임무 : 조사]#n#k"+enter;
			msg += isitem ? "\r\n목표 전리품 : #b#i"+getk("mobid")+"##z"+getk("mobid")+"# "+getk("mobq")+"개#k"+enter+enter : "처치 대상 : #b#o"+getk("mobid")+"# "+getk("mobq")+"마리#k"+enter+enter;
			msg += "임무를 수락하시겠습니까?";

			cm.sendYesNo(msg);
		}
	} else if (status == 2) {
		var msg = "잘 오셨어요. #h #님, 오늘 #h #님에게 부탁드릴 일은 아래와 같습니다."+enter+enter;
		msg += isitem ? "#fUI/UIWindow2.img/UtilDlgEx/list0#\r\n#r#e[임무 : 소멸의 여로조사]#n#k"+enter : "#fUI/UIWindow2.img/UtilDlgEx/list0#\r\n#r#e[임무 : 조사]#n#k"+enter;
		msg += isitem ? "\r\n목표 전리품 : #b#i"+getk("mobid")+"##z"+getk("mobid")+"# "+getk("mobq")+"개#k"+enter+enter : "처치 대상 : #b#o"+getk("mobid")+"# "+getk("mobq")+"마리#k"+enter+enter;
		msg += "그럼 임무를 완수하고 돌아와 주십시오.";
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
	//var rand1 = reward[Randomizer.rand(0, reward.length-1)];
	//var rand2 = reward[Randomizer.rand(0, reward.length-1)];
	for (i = 0; i < reward.length; i++) {
		cm.gainItem(reward[i]['itemid'], reward[i]['qty']);
		cm.getPlayer().AddStarDustPoint(20, cm.getPlayer().getTruePosition());
		msg += "#i"+reward[i]['itemid']+"##z"+reward[i]['itemid']+"# "+reward[i]['qty']+"개"+enter;
	}
	//cm.gainItem(rand1['itemid'], rand1['qty']);
	//cm.gainItem(rand2['itemid'], rand2['qty']);
	//msg += "#i"+rand1['itemid']+"##z"+rand1['itemid']+"# "+rand1['qty']+"개"+enter;
	//msg += "#i"+rand2['itemid']+"##z"+rand2['itemid']+"# "+rand2['qty']+"개"+enter;
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
