function enter(pi) {
    if (pi.haveMonster()) {
	pi.playerMessage("���� ���Ͱ� �����ֽ��ϴ�.");
    } else {
	if (pi.getPlayer().getMapId() == 925076300) {
		pi.warp(925020003, 1);
		pi.openNpcCustom(pi.getClient(), 2091011, "dojo_exit");
	} else {
		pi.dojowarp(pi.getPlayer().getMapId() + 100);
		pi.openNpcCustom(pi.getClient(), 2091011, "dojang_up");
	}
    }
}
