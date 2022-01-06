
var data;
var day;
var item = -1;
function enter(pi) {
	getData();
	item = 2434745 + day;
	var dateid = "mPark_Date_" + day;
	pi.getClient().setKeyValue(201820, "mc_"+data, pi.getClient().getKeyValue(201820, "mc_"+data) + 1);
	pi.getPlayer().setKeyValue(2, dateid, pi.getPlayer().getKeyValue(2, dateid) + 1);
	pi.getPlayer().setKeyValue(201829, "mc_clear", (pi.getPlayer().getKeyValue(201829, "mc_clear") + 1));
	pi.gainExp(pi.getPlayer().getMparkexp() * 5);
	pi.gainItem(item, 1);
	pi.getPlayer().setMparkexp(0);
	pi.warp(951000000, 0);
	pi.openNpc(9071000, "mPark_Reward");

}

function getData() {
	time = new Date();
	year = time.getFullYear();
	month = time.getMonth() + 1;
	if (month < 10) {
		month = "0"+month;
	}
	date = time.getDate() < 10 ? "0"+time.getDate() : time.getDate();
	data = year+""+month+""+date;
	day = time.getDay();
}
