/**
 * KOREA Project ================================== KOREA < dookank@nate.com >
 * wild매니저 < up_set@nate.com > ==================================
 */
package handling.channel;

import client.MapleCharacter;
import server.MapleItemInformationProvider;
import server.Randomizer;
import server.Timer;
import tools.packet.CField;

public class FishingHandler {

    public static int Items[] = {4001187, 4001188, 4001189};
    public static short Quentity[] = {1, 1, 1};
    public static int BonusItems[] = {4310229, 4001547, 4001548, 4001549, 4001550};
    public static short BonusQuentity[] = {1, 1, 1, 1, 1};
    public static int FishingMap = 123456789;
    public static int FishingChair = 3010432;

    public static void GainFishing(final MapleCharacter chr) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        int rand = Randomizer.nextInt(Items.length);
        chr.gainItem(Items[rand], Quentity[rand], false, 0, null);
        if (Randomizer.isSuccess(10)) {
            chr.gainItem(BonusItems[rand], BonusQuentity[rand], false, 0, null);
//            chr.getClient().getSession().write(EffectPacket.showWZEffect("Effect/PvPEff.img/GradeUp", 1));
            chr.dropMessage(5, "[낚시 알림] 축하합니다! 보너스로 아이템 " + ii.getName(BonusItems[rand]) + "를 " + BonusQuentity[rand] + "개 얻었습니다!"); //pink
        }
        chr.setKeyValue(2, "fishing", String.valueOf(chr.getKeyValue(2, "fishing") + 1));
        chr.dropMessage(6, "[낚시 알림] 낚시를통해 아이템 " + ii.getName(Items[rand]) + "를 " + Quentity[rand] + "개 얻었습니다!");
    }

    public static void StartFishing(final MapleCharacter chr) {
        chr.setFishing(true);
        chr.getClient().getSession().write(CField.getClock(5));
        Timer.BuffTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                if (chr.getChair() == FishingChair && chr.getMapId() == FishingMap) {
                    GainFishing(chr);
                    StartFishing(chr);
                } else {
                    StopFishing(chr);
                }
            }
        }, 5000);
    }

    public static void StopFishing(final MapleCharacter chr) {
        Timer.BuffTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                chr.setFishing(false);
            }
        }, 1000);
        chr.getClient().getSession().write(CField.getClock(-1));
    }
}
