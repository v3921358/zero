/*
블러디퀸 소환
*/
importPackage(Packages.tools.packet);
function act() {
    rm.spawnMonster(8920000, 1);
}

function stateChanged() {
	rm.getReactor().getMap().broadcastMessage(CField.removeMapEffect());
	if (rm.getReactor().getState() == 1) {
		rm.getReactor().getMap().broadcastMessage(CField.startMapEffect("어머, 귀여운 손님들이 찾아왔네. ", 5120099, 3, true));
	} else if (rm.getReactor().getState() == 2) {
		rm.getReactor().getMap().broadcastMessage(CField.startMapEffect("무엄하다! 감히 대전을 함부로 드나들다니! ", 5120100, 3, true));
	} else if (rm.getReactor().getState() == 3) {
		rm.getReactor().getMap().broadcastMessage(CField.startMapEffect("킥킥, 여기가 죽을 자리인 줄도 모르고 왔구나. ", 5120101, 3, true));
	} else if (rm.getReactor().getState() == 4) {
		rm.getReactor().getMap().broadcastMessage(CField.startMapEffect("흑흑, 당신의 죽음을 미리 슬퍼해드리지요. ", 5120102, 5, true));
	}	
}
