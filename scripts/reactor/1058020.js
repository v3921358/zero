/*
벨룸 소환
*/
importPackage(java.lang);
importPackage(java.util);

function act() {
var tick = 0;

      var sc = Packages.server.Timer.EtcTimer.getInstance().register(function () {
            if (tick == 1) {
                rm.spawnMonster(8930100, 1);
                rm.getPlayer().getMap().startMapEffect("내 경고를 무시하고 다시 찾아온 것은 네놈이니 더 이상 자비를 베풀지는 않겠다.", 5120103, 5000);
                //rm.getPlayer().dropMessage(6, "cancel");
                sc.cancel(true);
            }
            tick++;
        },
        4000);
}
