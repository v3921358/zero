﻿function act() {
    var em = rm.getEventManager("Chaos_Zakum");
    var eim = rm.getPlayer().getEventInstance();
    if (em == null || eim == null) {
        rm.mapMessage(5, "정상적으로 입장을 하지 않아 리액터가 정상 작동하지 않습니다. 운영자께 문의해주세요.");
        return;
    }
    for (i = 8800103; i < 8800111; i++) {
        mob = em.getMonster(i);
        eim.registerMonster(mob);
        eim.getMapInstance(0).spawnMonsterOnGroundBelow(mob, new java.awt.Point(10, 60));
    }
    mob = em.getMonster(8800102);
    eim.registerMonster(mob);
    eim.getMapInstance(0).spawnMonsterOnGroundBelow(mob, new java.awt.Point(10, 60));
    rm.mapMessage(5, "원석의 힘으로 자쿰이 소환됩니다.");
}