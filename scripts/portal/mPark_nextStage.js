/*
 * ǻ��¶��� �ҽ� ��ũ��Ʈ �Դϴ�.
 * 
 * ��Ż��ġ : 
 * ��Ż���� : 
 * 
 * ���� : ��ũ��
 * 
 */

function enter(pi) {
	if (pi.getClient().getChannelServer().getMapFactory().getMap(pi.getPlayer().getMapId()).getNumMonsters() > 0) {
		pi.getPlayer().dropMessage(5, "��� ���͸� óġ�ϼž� ���� ������ �̵��Ͻ� �� �ֽ��ϴ�.");
	} else {
		pi.warp(pi.getPlayer().getMapId() + 100, 0);
		pi.resetMap(pi.getPlayer().getMapId());
	}
}
