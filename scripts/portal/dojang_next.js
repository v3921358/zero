function enter(pi) {
    if (pi.haveMonster()) {
	pi.playerMessage("���� ���Ͱ� �����ֽ��ϴ�.");
    } else {
	pi.dojowarp(pi.getPlayer().getMapId() + 100);
    }
}
