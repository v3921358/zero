function enter(pi) {
    var map = pi.getPlayer().getMapId();
    var check = map%10;
    var em = check == 0 ? pi.getEventManager("Normal_Horntail") : pi.getEventManager("Chaos_Horntail");
    var eim = pi.getPlayer().getEventInstance();
    if (em == null || eim == null) {
        pi.getPlayer().dropMessage(5, "���������� ���Դϴ�. ��ڲ� ������ �ּ���.");
    }
    if (map == 240060000 || map == 240060001) {
       // if (eim.getProperty("stage") == "0") {
       //     pi.getPlayer().dropMessage(5, "������ ��Ż�� �۵����� �ʽ��ϴ�.");
       // } else {
            pi.warpParty(map + 100);
	    eim.monterSpawn(pi.getPlayer(), map + 100);
       // }
    } else if (map == 240060100 || map == 240060101) {
        if (eim.getProperty("stage") == "1") {
            pi.getPlayer().dropMessage(5, "������ ��Ż�� �۵����� �ʽ��ϴ�.");
        } else {
            pi.warpParty(map + 100);
	    eim.monterSpawn(pi.getPlayer(), map + 100);
        }
    }
    return false;
}
