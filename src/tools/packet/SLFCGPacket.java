package tools.packet;

import client.DreamBreakerRank;
import client.MapleBuffStat;
import client.MapleCharacter;
import handling.SendPacketOpcode;
import handling.channel.handler.PlayerInteractionHandler;
import server.Obstacle;
import server.Randomizer;
import server.games.BattleReverse.BattleReverseStone;
import server.games.MultiYutGame.MultiYutPlayer;
import server.games.MultiYutGame.Yut;
import server.games.OneCardGame.OneCard;
import server.games.OneCardGame.OneCardPlayer;
import server.polofritto.MapleRandomPortal;
import tools.HexTool;
import tools.data.MaplePacketLittleEndianWriter;

import java.awt.*;
import java.util.List;
import java.util.Map;

/*
 * @author SLFCG
 */
public class SLFCGPacket {

    /*
     * 게임 값
     * 13 : 빙고
     * 16 : 원카드
     * 18 : 싱가포르
     */
    public static byte[] cancelSoulGauge() {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.CANCEL_BUFF.getValue());
        PacketHelper.writeSingleMask(packet, MapleBuffStat.SoulMP);
        packet.writeInt(0);
        return packet.getPacket();
    }

    /**
     * 테스트용 패킷 전송 함수
     *
     * @param nType 옵코드
     * @param iPacket 패킷 데이터
     */
    public static final byte[] SendPacket(short nType, String iPacket) {
        final MaplePacketLittleEndianWriter w = new MaplePacketLittleEndianWriter();
        w.writeShort(nType);
        w.write(HexTool.getByteArrayFromHexString(iPacket));
        return w.getPacket();
    }

    /**
     * 날씨 추가 패킷
     *
     * @param type 날씨코드 (1: snow, 2: snowstorm)
     */
    public static byte[] WeatherAddPacket(int type) {
        final MaplePacketLittleEndianWriter w = new MaplePacketLittleEndianWriter();
        w.writeShort(SendPacketOpcode.FieldWeather_Add.getValue());
        w.writeInt(type); // 1: snow, 2: snowstorm
        return w.getPacket();
    }

    /**
     * 날씨 삭제 패킷
     *
     * @param type 날씨코드 (1: snow, 2: snowstorm)
     */
    public static byte[] WeatherRemovePacket(int type) {
        final MaplePacketLittleEndianWriter w = new MaplePacketLittleEndianWriter();
        w.writeShort(SendPacketOpcode.FieldWeather_Remove.getValue());
        w.writeInt(type); // 1: snow, 2: snowstorm
        return w.getPacket();
    }

    /**
     * 캐릭터 텔레포트 패킷
     *
     * @param x X좌표
     * @param y Y좌표
     */
    public static byte[] CharReLocationPacket(int x, int y) {
        final MaplePacketLittleEndianWriter w = new MaplePacketLittleEndianWriter();
        w.writeShort(SendPacketOpcode.USER_TELEPORT.getValue());
        w.writeInt(x);
        w.writeInt(y);
        return w.getPacket();
    }

    /**
     * 와글와글 하우스 게임 상태 조작 패킷
     *
     * @param command 게임 State (1: start message, 2: game start, 3: game result)
     */
    public static byte[] BlockGameCommandPacket(int command) {
        final MaplePacketLittleEndianWriter w = new MaplePacketLittleEndianWriter();
        w.writeShort(SendPacketOpcode.BlockGameCommand.getValue());
        w.writeInt(command); // 1: start message, 2: game start, 3: game result
        return w.getPacket();
    }

    /**
     * 와글와글 하우스 게임 컨트롤 패킷
     *
     * @param velocity 블럭 속도
     * @param misplaceallowance 오차 허용치
     */
    public static byte[] BlockGameControlPacket(int velocity, int misplaceallowance) {
        final MaplePacketLittleEndianWriter w = new MaplePacketLittleEndianWriter();
        w.writeShort(SendPacketOpcode.BlockGameControl.getValue());
        w.writeInt(0);
        w.writeInt(0);
        w.writeInt(0);
        w.writeInt(0);
        w.writeInt(1);

        w.writeInt(0); // ?
        w.writeInt(velocity);// velocity(%)
        w.writeInt(misplaceallowance); // missplace allowance
        w.writeInt(0);
        w.writeInt(0);
        w.writeInt(0);
        w.writeInt(0);
        w.writeInt(0);
        return w.getPacket();
    }

    /**
     * 프로즌링크 UI
     *
     * @param count 남은 몬스터 수
     */
    public static byte[] FrozenLinkMobCount(int count) {
        final MaplePacketLittleEndianWriter w = new MaplePacketLittleEndianWriter();
        w.writeShort(SendPacketOpcode.FROZEN_LINK.getValue());
        w.write(1);
        w.writeInt(count);
        w.writeInt(0);
        return w.getPacket();
    }

    /**
     * 메소의자 패킷
     *
     * @param charid 캐릭터 ID
     * @param meso 누적 사용 메소
     * @param chairid 의자 ID
     */
    public static byte[] MesoChairPacket(int charid, int meso, int chairid) {
        final MaplePacketLittleEndianWriter w = new MaplePacketLittleEndianWriter();
        w.writeShort(SendPacketOpcode.UserMesoChairAddMeso.getValue());
        w.writeInt(charid);
        w.writeInt(chairid);
        w.writeLong((long) meso);
        w.writeLong((long) meso);
        return w.getPacket();
    }

    /**
     * 큐브의자 설정값 패킷
     *
     * @param chairs ExQuest 내용
     */
    // TowerChair Structure : Bottom <===> Top
    public static byte[] TowerChairMessage(String chairs) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        packet.write(15);
        packet.writeInt(7266);// QuestID
        packet.writeMapleAsciiString(chairs);
        return packet.getPacket();
    }

    /**
     * 큐브의자 설정 저장완료 패킷
     *
     */
    public static byte[] TowerChairSaveDone() {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.UserTowerChairSettingResult.getValue());
        return packet.getPacket();
    }

    /**
     * OX퀴즈 카운트다운 패킷
     *
     */
    public static byte[] OXQuizCountdown(int time) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.HOxQuizCountEffect.getValue());
        packet.writeInt(time); // 332++
        return packet.getPacket();
    }

    /**
     * OX퀴즈 텍스트 전송 패킷
     *
     * @param texts 텍스트 리스트
     */
    public static byte[] OXQuizPlainText(final List<String> texts) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.HOxQuizEnter.getValue());
        packet.writeInt(texts.size());
        for (String a : texts) {
            packet.writeMapleAsciiString(a);
        }
        return packet.getPacket();
    }

    /**
     * OX퀴즈 질문 패킷
     *
     * @param text 질문
     * @param index 인덱스
     * @param leftquestion 남은 질문 수
     */
    public static byte[] OXQuizQuestion(String text, int index, int leftquestion) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.HOxQuizQuestions.getValue());
        packet.writeInt(index);
        packet.writeInt(leftquestion + 1);
        packet.writeMapleAsciiString(text);
        packet.writeZeroBytes(5);
        return packet.getPacket();
    }

    /**
     * OX퀴즈 정답 해설 패킷
     *
     * @param text 해설
     */
    public static byte[] OXQuizExplain(String text) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.HOxQuizExplan.getValue());
        packet.writeInt(0);
        packet.writeMapleAsciiString(text);
        return packet.getPacket();
    }

    /**
     * OX퀴즈 정답 패킷
     *
     * @param isX X인가
     */
    public static byte[] OXQuizResult(boolean isX) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.HOxQuizResult.getValue());
        packet.writeInt(1);
        packet.writeInt(1);
        packet.writeInt(isX ? 0 : 1);
        return packet.getPacket();
    }

    /**
     * OX퀴즈 텔레포트 패킷
     *
     * @param point 텔레포트 목적 포인트
     */
    public static byte[] OXQuizTelePort(byte point) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.HOxQuizMoveToPortal.getValue());
        packet.write(point);
        return packet.getPacket();
    }

    /**
     * 빙고 UI 패킷
     *
     * @param type UI타임 (3: Open UI 4: Clear Text 5: Clear UpperPanel 6: Close
     * Rank)
     * @param round 현재 라운드
     */
    public static byte[] BingoUI(int type, int round) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.BingoGameState.getValue());
        packet.writeInt(type);// 3: Open UI 4: Clear Text 5: Clear UpperPanel 6: Close Rank
        packet.writeInt(round);// round number
        packet.writeInt(5);// ui amount?
        packet.writeInt(1);
        return packet.getPacket();
    }

    /**
     * 빙고판 설정 패킷
     *
     * @param table 2차원 배열의 빙고 판
     */
    public static byte[] BingoInit(int[][] table) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.EnterBingoGame.getValue());
        packet.writeInt(1);
        packet.writeInt(1);
        packet.writeInt(0);
        packet.writeInt(5);
        packet.writeInt(5);
        packet.writeInt(1);
        packet.write(1);
        packet.writeInt(1);
        packet.writeInt(25);// cell count
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                packet.writeInt(table[x][y]);// cell number
            }
        }
        return packet.getPacket();
    }

    public static byte[] BingoHostNumberReady() {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.HostNumberReady.getValue());
        return packet.getPacket();
    }

    /**
     * 빙고 숫자 호스팅 패킷
     *
     * @param number 숫자
     * @param leftcount 남은 갯수 (5의 배수)
     */
    public static byte[] BingoHostNumber(int type, int number, int leftcount) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.HostNumber.getValue());
        packet.writeInt(type);
        if (type <= 0) {
            packet.writeInt(leftcount); // 확실
        } else {
            packet.writeInt(number);
            packet.writeInt(number);
        }
        return packet.getPacket();
    }

    /**
     * 빙고 숫자 선택 패킷
     *
     * @param number 선택한 숫자
     */
    public static byte[] BingoCheckNumber(int number) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.CheckNumberAck.getValue());
        packet.writeInt(1);
        packet.writeInt(0);
        packet.writeInt(number);
        packet.writeZeroBytes(12);
        return packet.getPacket();
    }

    /**
     * 빙고 숫자 선택 패킷
     *
     * @param index 인덱스
     * @param type 선 타입 (0: 가로 1: 세로 2:대각선 (좌우) 3: 대각선 (우좌))
     */
    public static byte[] BingoDrawLine(int index, int type, int junk) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.CheckNumberAck.getValue());
        packet.writeInt(1);
        packet.writeInt(index);
        packet.writeInt(junk);
        packet.writeInt(0);
        packet.writeInt(1);
        packet.writeInt(type);// 0: 가로 1: 세로 2:대각선 (좌우) 3: 대각선 (우좌)
        return packet.getPacket();
    }

    /**
     * 빙고 랭킹 추가 패킷
     *
     * @param chr 캐릭터
     */
    public static byte[] BingoAddRank(MapleCharacter chr) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.AddBingoRank.getValue());
        packet.writeInt(1);
        packet.writeInt(chr.getId());
        packet.writeMapleAsciiString(chr.getName());
        packet.writeInt(0);
        packet.writeInt(1);// round
        packet.writeInt(0);
        return packet.getPacket();
    }

    /**
     * 사운드 이펙트 재생 패킷
     *
     * @param SE 사운드 경로
     */
    public static byte[] playSE(String SE) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        packet.write(5);
        packet.writeMapleAsciiString(SE);
        packet.writeInt(100); // 볼륨
        return packet.getPacket();
    }

    public static byte[] PoloFrittoEffect(int type, String path) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        switch (type) {
            case 0:
                packet.write(0x0F);
                packet.writeMapleAsciiString(path);
                packet.write(0x07);
                packet.write(0x01);
                break;
            case 1:
                packet.write(0x0E);
                packet.writeMapleAsciiString(path);
                packet.writeInt(0);
                break;
            case 4:
                packet.writeMapleAsciiString(path);
                break;
        }
        return packet.getPacket();
    }

    /**
     * 랜덤포탈 생성 패킷
     *
     * @param type 포탈 타입 (1: meso event 2: polofritto 3: firewolf)
     * @param oid 포탈 오브젝트 ID
     * @param pos 포탈 위치
     * @param charid 표시할 캐릭터 ID
     */
    public static byte[] PoloFrittoPortal(MapleRandomPortal portal) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.BEGIN_RANDOMPORTALPOOL.getValue());
        packet.write(portal.getPortalType()); // 1: meso event 2: polofritto 3: firewolf
        packet.writeInt(portal.getObjectId());
        packet.writePos(portal.getPos());
        packet.writeInt(portal.getMapId());
        packet.writeInt(portal.getCharId());
        return packet.getPacket();
    }

    /**
     * 랜덤포탈 제거 패킷
     *
     * @param oid 포탈 오브젝트 ID
     */
    public static byte[] RemovePoloFrittoPortal(MapleRandomPortal portal) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.RandomPortalRemoved.getValue());
        packet.write(portal.getPortalType());
        packet.writeInt(portal.getObjectId());
        packet.writeInt(portal.getMapId());
        return packet.getPacket();
    }

    public static byte[] milliTimer(int mil) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.CLOCK.getValue());
        packet.write(6);
        packet.writeInt(mil);
        return packet.getPacket();
    }

    public static byte[] setBountyHuntingStage(int stage) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.BOUNTY_HUNTING.getValue());
        packet.writeInt(stage);
        return packet.getPacket();
    }

    public static byte[] setTowerDefenseWave(int wave) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.TOWER_DEFENSE_WAVE.getValue());
        packet.writeInt(wave);
        return packet.getPacket();
    }

    public static byte[] setTowerDefenseLife(int life) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.TOWER_DEFENSE_LIFE.getValue());
        packet.writeInt(life);
        return packet.getPacket();
    }

    public static byte[] courtShipDanceState(int state) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.COURTSHIP_STATE.getValue());
        packet.writeInt(state);
        return packet.getPacket();
    }

    public static byte[] courtShipDanceCommand(List<List<Integer>> list) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.COURTSHIP_COMMAND.getValue());
        packet.writeInt(list.size());
        for (List<Integer> list1 : list) {
            packet.writeInt(list1.size());
            for (int list11 : list1) {
                packet.writeInt(list11); // 0~3 상하좌우 랜덤숫자 입력
            }
        }
        return packet.getPacket();
    }

    public static byte[] createGun() {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.CREATE_GUN.getValue()); // 718
        return packet.getPacket();
    }

    public static byte[] clearGun() {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.CLEAR_GUN.getValue()); // 719
        return packet.getPacket();
    }

    public static byte[] setGun() {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.SET_GUN.getValue()); // 716
        packet.writeMapleAsciiString("shotgun"); // sName
        packet.writeMapleAsciiString("shotgun"); // sCameraMoveType
        packet.writeInt(1); // MobCount
        packet.writeInt(200); // CooldownMS
        packet.writeRect(new Rectangle(-8, -8, 16, 16));
        return packet.getPacket();
    }

    public static byte[] setAmmo(int bullet) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.SET_AMMO.getValue()); // 717
        packet.writeInt(bullet); // maxCount
        return packet.getPacket();
    }

    public static byte[] attackRes() {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.SHOOT_RESULT.getValue()); // 720
        packet.write(1); // discount
        return packet.getPacket();
    }

    public static byte[] deadOnFPSMode(int objectId, int point) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.DEAD_FPS_MODE.getValue());
        packet.writeInt(objectId);
        packet.writeInt(point);
        return packet.getPacket();
    }

    public static byte[] StarDustUI(String path, long Point, long Coin) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.STARDUST_UI.getValue());

        packet.write(1);
        packet.writeInt(100712); // idCode1
        packet.writeInt(0); // idCode2
        packet.writeMapleAsciiString(path);
        packet.writeInt(30000); // 하루 맥스치
        packet.writeInt(0);
        packet.writeInt(0);
        packet.writeInt(0);
        packet.writeInt(Point); // 입장 시 차있는 게이지
        packet.writeInt(Coin); // 총 갯수
        packet.write(false); // Lock
        packet.writeLong(PacketHelper.getTime(PacketHelper.MAX_TIME)); // tEndDate*/
        return packet.getPacket();
    }

    public static byte[] StarDustIncrease(int totalGauge, int add, boolean lock, int total, int pointAdd, Point point) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.STARDUST_INCREASE.getValue());
        packet.writeInt(totalGauge);
        packet.writeInt(add); // 코인 Add
        packet.write(lock);// 락
        packet.writeInt(Math.min(9999, total));// 코인갯수
        packet.writeInt(pointAdd); // 포인트 Add
        packet.writeInt(1); // 이펙트

        //뒤진 몬스터 좌표
        packet.writeInt(point.x);
        packet.writeInt(point.y);

        packet.writeInt(0);
        packet.writeInt(add);// 포인트 알림 갯수
        packet.writeInt(0);
        packet.writeMapleAsciiString("포인트");

        return packet.getPacket();
    }

    public static byte[] SpiritSavedEffect(int SpiritCount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(16); // mode
        mplew.writeMapleAsciiString("Map/Effect3.img/savingSpirit/" + SpiritCount);
        return mplew.getPacket();
    }

    /**
     * 파트너 소환 패킷
     *
     * @param bShow 생성이면 true
     * @param oid 오브젝트 ID
     * @param skillId Skill ID (5000150 : 스피릿 세이비어 정령)
     */
    public static byte[] SpawnPartner(boolean bShow, int oid, int skillId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SPAWN_PARTNER.getValue());
        mplew.write(bShow);
        mplew.writeInt(oid);
        mplew.writeInt(skillId);
        mplew.write(0);
        return mplew.getPacket();
    }

    /**
     * 우측 하단 노란색 메시지창 패킷
     *
     * @param npcid NPC ID
     * @param duraction 지속시간 (밀리초)
     * @param title 타이틀
     * @param msg 내용
     */
    public static byte[] OnYellowDlg(final int npcid, final int duraction, final String title, final String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.YELLOW_DLG.getValue());
        mplew.writeInt(npcid);
        mplew.writeInt(duraction);
        mplew.writeMapleAsciiString(title);
        mplew.writeMapleAsciiString(msg);
        mplew.write(false); // 324++
        return mplew.getPacket();
    }

    public static byte[] DreamBreakerRanking(String name) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        int count = 1;
        packet.writeShort(SendPacketOpcode.DREAM_BREAKER_RANKING.getValue());
        packet.writeInt(0);
        packet.write(43);
        packet.writeZeroBytes(8);
        packet.writeInt(DreamBreakerRank.Rank.containsKey(name) ? DreamBreakerRank.Rank.get(name) : 0);
        packet.writeInt(DreamBreakerRank.Rank.containsKey(name) ? DreamBreakerRank.getRank(name) : 0); // 내랭킹
        packet.writeZeroBytes(16);
        packet.write(43);
        packet.writeInt(DreamBreakerRank.Rank.size() > 100 ? 100 : DreamBreakerRank.Rank.size());
        for (Map.Entry<String, Integer> info : DreamBreakerRank.Rank.entrySet()) {
            if (count == 101) {
                break;
            }
            packet.writeZeroBytes(8);
            packet.writeInt(info.getValue());
            packet.writeInt(count); // 등수
            packet.writeMapleAsciiString(info.getKey());
            packet.write(false);
            count++;
        }
        return packet.getPacket();
    }

    public static byte[] SetDreamBreakerUI(final int stage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DREAM_BREAKER.getValue());
        mplew.writeInt(3);
        mplew.writeInt(500);
        mplew.writeInt(180000);
        mplew.writeInt(stage);
        return mplew.getPacket();
    }

    public static byte[] DreamBreakerGaugePacket(final int Gauge) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DREAM_BREAKER.getValue());
        mplew.writeInt(4);
        mplew.writeInt(Gauge);
        return mplew.getPacket();
    }

    public static byte[] DreamBreakerCountdown(final int stage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DREAM_BREAKER.getValue());
        mplew.writeInt(5);
        mplew.writeInt(stage);
        return mplew.getPacket();
    }

    public static byte[] DreamBreakerDisableTimer(final boolean Disable, final int Time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DREAM_BREAKER.getValue());
        mplew.writeInt(6);
        mplew.write(Disable);
        mplew.writeInt(Time);
        return mplew.getPacket();
    }

    public static byte[] DreamBreakerResult(final int ClearTime) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DREAM_BREAKER.getValue());
        mplew.writeInt(7);
        mplew.writeInt(ClearTime);
        return mplew.getPacket();
    }

    public static byte[] DreamBreakeLockSkill(final int SkillCode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DREAM_BREAKER.getValue());
        mplew.writeInt(8);
        mplew.writeInt(SkillCode); // 0~3
        return mplew.getPacket();
    }

    public static byte[] DreamBreakeSkillRes() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DREAM_BREAKER.getValue());
        mplew.writeInt(9);
        return mplew.getPacket();
    }

    public static byte[] DreamBreakerMsg(String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DETAIL_SHOW_INFO.getValue());
        mplew.writeInt(3); // color
        mplew.writeInt(0x14); // width
        mplew.writeInt(0x14); // heigh
        mplew.writeInt(0);
        mplew.write(false); // 325++
        mplew.writeMapleAsciiString(msg);
        return mplew.getPacket();
    }

    public static byte[] HundredDetectiveGameExplain() {
        String[] msg = {"<어드벤처 암호추리>\n조금만 기다려 주세YO!", "YO! 보물이 가득! 어드벤처 아일랜드!\n" + "보물 상자의 암호를 맞춰YO!",
            "상자의 키패드 클릭 클릭 YO!\n" + "상자의 암호를 맞춰 맞춰 YO!", "숫자도 맞고 위치도 맞으면 ○!\n" + "숫자만 맞으면 △!",
            "암호 입력은 10초에 한 번씩 YO!", "I say ○! U say △! ○! △! ○! △!", "준비되셨나YO!\n" + "이제부터 당신도 암호추리의 대가YO!"};
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.HDetectiveGamePlainText.getValue());
        mplew.writeInt(msg.length); // color
        for (String s : msg) {
            mplew.writeMapleAsciiString(s);
        }
        return mplew.getPacket();
    }

    public static byte[] HundredDetectiveGameReady(final int Stage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.HDetectiveGameSetGame.getValue());
        mplew.writeInt(3);// Final Stage
        mplew.writeInt(Stage);
        mplew.writeInt(3);
        mplew.writeInt(15); // 기회
        mplew.writeInt(10000);
        return mplew.getPacket();
    }

    public static byte[] HundredDetectiveGameControl(final int type, final int Stage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.HDetectiveGameCommand.getValue());
        mplew.writeInt(type); // 3: UI 6: start 4: finish
        mplew.writeInt(Stage);
        return mplew.getPacket();
    }

    public static byte[] HundredDetectiveGameAddRank(final int cid, final String name) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.HDetectiveGameAddRank.getValue());
        mplew.writeInt(cid);
        mplew.writeMapleAsciiString(name);
        return mplew.getPacket();
    }

    public static byte[] HundredDetectiveGameResult(final int input, final int result) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.HDetectiveGameResult.getValue());
        mplew.writeInt(input);
        mplew.writeInt(result);
        return mplew.getPacket();
    }

    public static byte[] HundredDetectiveReEnable(final int attempt) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.HDetectiveGameClear.getValue());
        mplew.writeInt(attempt);
        return mplew.getPacket();
    }

    // 1920 1080이나 1920 1200 해상도에선 제대로 안보임
    public static byte[] ShowWeb(final String URL) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_WEB.getValue());
        mplew.writeMapleAsciiString(URL);
        return mplew.getPacket();
    }

    public static byte[] SendUserClientResolutionRequest() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UserClientResolutionRequest.getValue());
        return mplew.getPacket();
    }

    public static byte[] ChangeVolume(final int Volume, final int FadeTime) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(9);
        mplew.writeInt(Volume);
        mplew.writeInt(FadeTime);
        return mplew.getPacket();
    }

    public static byte[] PlatformerStageInfo(final int Stage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(1);
        mplew.writeInt(34502);
        mplew.write(1);
        mplew.writeMapleAsciiString(String.valueOf(Stage));
        return mplew.getPacket();
    }

    public static byte[] PlatformerTimerInfo() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UserTimerInfo.getValue());
        mplew.write(HexTool
                .getByteArrayFromHexString("02 00 00 00 02 00 00 00 80 96 98 00 00 00 00 00 80 96 98 00 C4 86 00 00"));
        return mplew.getPacket();
    }

    public static byte[] createObstaclePlatformer(Obstacle[] obs) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CREATE_OBSTACLE.getValue());
        mplew.writeInt(0);
        mplew.writeInt(obs.length);
        mplew.write(4);
        mplew.writeInt(15);
        mplew.writeZeroBytes(12);
        mplew.writeInt(1600);
        for (Obstacle ob : obs) {
            mplew.write(true);
            mplew.writeInt(ob.getKey());
            mplew.writeInt(Randomizer.nextInt()); // crc
            mplew.writeInt(ob.getOldPosition().x);
            mplew.writeInt(ob.getOldPosition().y);
            mplew.writeInt(ob.getNewPosition().x);
            mplew.writeInt(ob.getNewPosition().y);
            mplew.writeInt(40);
            mplew.writeZeroBytes(16); // 351 new
            mplew.writeInt(ob.getRange());
            mplew.writeInt(ob.getTrueDamage());
            mplew.writeInt(ob.getDelay());
            mplew.writeInt(ob.getHeight());
            mplew.writeInt(ob.getVperSec());
            mplew.writeInt(ob.getMaxP());
            mplew.writeInt(ob.getLength());
            mplew.writeInt(ob.getAngle());
        }
        return mplew.getPacket();
    }

    public static byte[] ClearObstacles() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CLEAR_OBSTACLE.getValue());
        return mplew.getPacket();
    }

    public static byte[] CameraCtrl(final int nType, int... args) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CameraCtrlMsg.getValue());
        mplew.write(nType);
        switch (nType) {
            case 0x0B:// Loose Control
                mplew.writeInt(args[0]);// Loose Level
                break;
            case 0x0D: // RelMoveCommand
                mplew.write(args[0]); // type
                mplew.writeInt(args[1]); // Delay
                mplew.writeInt(args[2]); // X
                mplew.writeInt(args[3]); // Y
                break;
            case 0x0F: // ScaleCommand
                mplew.write(args[0]);
                mplew.writeInt(args[1]); // Delay
                mplew.writeInt(args[2]); // Y
                break;
        }
        return mplew.getPacket();
    }

    public static byte[] SetIngameDirectionMode(final boolean Enable, final boolean BlackFrame,
            final boolean ForceMouseOver, final boolean ShowUI) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SetInGameDirectionMode.getValue());
        mplew.write(Enable ? 1 : 0);
        mplew.write(BlackFrame ? 1 : 0);
        mplew.write(ForceMouseOver ? 1 : 0);
        mplew.write(ShowUI ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] removeIngameDirectionMode() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SetInGameDirectionMode.getValue());
        mplew.write(0);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static byte[] SetIngameDirectionMode(final boolean Enable, final boolean BlackFrame) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SetInGameDirectionMode.getValue());
        mplew.write(Enable ? 1 : 0);
        mplew.write(BlackFrame ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] SetStandAloneMode(final boolean Enable) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SetStandAloneMode.getValue());
        mplew.write(Enable ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] InGameDirectionEvent(final String str, final int... args) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UserInGameDirectionEvent.getValue());
        mplew.write(args[0]);
        switch (args[0]) {
            case 0x00:
                mplew.writeInt(args[1]); // 333++
                mplew.writeInt(args[2]); // 333++
                break;
            case 0x01:// InGameDirectionEvent_Delay
                mplew.writeInt(args[1]); // Delay
                break;
            case 0x02:// InGameDirectionEvent_EffectPlay
                mplew.writeMapleAsciiString(str);// sEffectUOL
                mplew.writeInt(args[1]); // nCount
                mplew.writeInt(args[2]);// nPartCount
                mplew.writeInt(args[3]);// nRy
                mplew.write(true);
                mplew.writeInt(args[4]);
                mplew.write(args[5]);
                if (args[5] > 0) {
                    mplew.writeInt(args[6]);
                    mplew.write(args[7]);
                    mplew.write(args[8]);
                }
                mplew.write(0);
//                mplew.write(args[9]);
//                if (args[9] > 0) {
//                    mplew.writeMapleAsciiString(str);
//                }
                break;
            case 0x03:// InGameDirectionEvent_ForcedInput
                mplew.writeInt(args[1]);// nForcedInput
                break;
            case 0x04:
                mplew.writeMapleAsciiString(str);// sEffectUOL
                mplew.writeInt(args[1]); // nCount
                mplew.writeInt(args[2]);// nPartCount
                mplew.writeInt(args[3]);// nRy
                break;
            case 0x05:// InGameDirectionEvent_CameraMove
                mplew.write(args[1] > 0 ? 1 : 0);// bBack
                mplew.writeInt(args[2]);// nPixelPerSec
                mplew.write(args[3] > 0 ? 1 : 0);
                if (args[1] > 0 && args[3] > 0) {
                    mplew.writeInt(args[4]);// ptEndPos
                    mplew.writeInt(args[5]);
                }
                break;
            case 0x06:
                mplew.writeInt(args[1]);
                break;
            case 0x07: // InGameDirectionEvent_CameraZoom
                mplew.write(false);
                mplew.writeInt(args[1]); // nTime
                mplew.writeInt(args[2]); // nPartCount
                mplew.writeInt(args[3]); // nTimePos
                mplew.writeInt(args[4]);// ptEndPos
                mplew.writeInt(args[5]); // unk
                break;
            case 0x0A:
                mplew.write(args[1]);
                break;
            case 0x0B:
                mplew.writeInt(args[1]);
                break;
            case 0x0C:
                mplew.writeMapleAsciiString(str);// sEffectUOL
                mplew.write(args[1]); // nCount
                break;
            case 0x0D:
                mplew.writeMapleAsciiString(str);// sEffectUOL
                mplew.write(args[1]); // nCount
                mplew.writeShort(args[2]); // nTimePos
                mplew.writeInt(args[3]);// ptEndPos
                mplew.writeInt(args[4]); // unk
                break;
            case 0x0E:
                mplew.write(args[1]);
                for (int i = 0; i < args[1]; ++i) {
                    mplew.writeInt(args[2]);// ptEndPos
                }
                break;
            case 0x0F:// SetAdditionalEffectVisibleForInGameDirection
                break;
            case 0x10:
                break;
            case 0x11:// InGameDirectionEvent_ForcedMove
                mplew.writeInt(args[1]); // nForcedMoveDir
                mplew.writeInt(args[2]); // ForcedMovePixel
                mplew.write(args[3]);
                break;
            case 0x12:// InGameDirectionEvent_ForcedFlip
                mplew.writeInt(args[1]); // nForcedFlip
                break;
            case 0x13:
                mplew.write(args[1]);
                break;
            case 0x14:
                mplew.write(args[1]);
                break;
            case 0x16:
                mplew.writeInt(args[1]);
                break;
            case 0x17:
                mplew.writeMapleAsciiString(str);// sEffectUOL
                break;
        }
        return mplew.getPacket();
    }

    public static byte[] PlayAmientSound(final String UOL, final int nVolume, final int unk) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PlayAmbientSound.getValue());
        mplew.writeMapleAsciiString(UOL);
        mplew.writeInt(nVolume);
        mplew.writeInt(unk);
        return mplew.getPacket();
    }

    public static byte[] StopAmientSound(final String UOL) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.StopAmbientSound.getValue());
        mplew.writeMapleAsciiString(UOL);
        return mplew.getPacket();
    }

    public static byte[] SetNpcSpecialAction(final int oid, final String uol, final int tDuration, final boolean bLocalAct) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NpcSpecialAction.getValue());
        mplew.writeInt(oid);
        mplew.writeMapleAsciiString(uol);
        mplew.writeInt(tDuration);
        mplew.write(bLocalAct);
        return mplew.getPacket();
    }

    public static byte[] MakeBlind(int... args) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(0x15);
        mplew.write(args[0]);
        mplew.writeShort(args[1]);
        mplew.writeShort(args[2]);
        mplew.writeShort(args[3]);
        mplew.writeShort(args[4]);
        mplew.writeInt(args[5]);
        mplew.writeInt(args[6]);
        return mplew.getPacket();
    }

    public static class OneCardGamePacket {

        public static byte[] CreateUI(MapleCharacter chr, int position, List<MapleCharacter> chrs) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(20);
            mplew.write(16);// GameCode
            mplew.write(chrs.size());
            mplew.write(position);
            for (int a = 0; a < chrs.size(); a++) {
                mplew.write(a);
                PacketHelper.addCharLook(mplew, chrs.get(a), chr.getId() == chrs.get(a).getId(), false);
                mplew.writeMapleAsciiString(chrs.get(a).getName());
                mplew.writeShort(chrs.get(a).getJob());
                mplew.writeInt(0);
            }
            mplew.write(-1);
            // COneCardGameRoomDlg::OnEnterResult
            mplew.write(chrs.size());
            for (int a = 0; a < chrs.size(); a++) {
                mplew.writeInt(chrs.get(a).getId());
            }
            return mplew.getPacket();
        }

        public static byte[] onChangeColorRequest(List<Integer> ableColors) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(2);

            //OneCardChangeColorDlg
            mplew.writeInt(15); // time

            mplew.write(ableColors.size());
            for (int color : ableColors) {
                mplew.write(color);
            }
            return mplew.getPacket();
        }

        public static byte[] onStart(List<OneCardPlayer> players) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(119);
            //MakeSlotScore
            mplew.writeInt(players.size());

            for (int a = 0; a < players.size(); a++) {
                mplew.writeInt(players.get(a).getPlayer().getId());
                mplew.writeInt(0);
                mplew.writeInt(0); // playCount 등급 관련 패킷
                mplew.writeInt(players.get(a).getPosition()); // eSlot
                mplew.writeInt(players.get(a).getCards().size()); // nCardSet
            }
            return mplew.getPacket();
        }

        public static byte[] onPutCardResult(OneCardPlayer player, OneCard card) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(157);
            mplew.writeInt(player == null ? 0 : player.getPlayer().getId());
            mplew.writeInt(card.getObjectId());
            //MakeSlotScore
            mplew.write(card.getColor()); // color
            mplew.write(card.getType()); // type
            mplew.write(false); // Hide..?

            return mplew.getPacket();
        }

        public static byte[] onGetCardResult(OneCardPlayer player, List<OneCard> cards) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(158);
            mplew.writeInt(player.getPlayer().getId());
            mplew.write(cards.size());
            for (OneCard card : cards) {
                mplew.writeInt(card.getObjectId());

                //MakeSlotScore
                mplew.write(card.getColor()); // color
                mplew.write(card.getType()); // type
            }
            return mplew.getPacket();
        }

        public static byte[] onChangeColorResult(boolean bHero, byte color) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(159);
            mplew.write(bHero);
            mplew.write(color);
            return mplew.getPacket();
        }

        public static byte[] onUserPossibleAction(OneCardPlayer player, List<OneCard> cards, boolean bGetCardFromGraves, boolean bClockWiseTurn) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(160);
            mplew.write(15); // time
            mplew.writeInt(player.getPlayer().getId()); // playerId
            mplew.write(bGetCardFromGraves); // bGetCardFromGraves
            mplew.write(bClockWiseTurn); // 시계방향 여부 ㅋㅋ
            mplew.writeInt(cards.size());
            for (OneCard card : cards) {
                mplew.writeInt(card.getObjectId());
            }
            return mplew.getPacket();
        }

        public static byte[] onShowScreenEffect(String str) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(161);
            mplew.writeMapleAsciiString(str);
            return mplew.getPacket();
        }

        public static byte[] onEffectResult(int type, int data, int id, boolean gameOver) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(162);
            mplew.write(type);

            switch (type) {
                case 0: // 카드 덱 섞기
                case 1: // ShowSingleDeck? 효과없음
                    break;
                case 2: // 공격 이펙트
                    mplew.write(data); // 불 갯수
                    break;
                case 3: // 디펜스 이펙트
                case 4: // 디펜스 부수기 이펙트
                    mplew.writeInt(id);
                    break;
                case 5: // 공격 받는 이펙트
                    mplew.writeInt(id);
                    mplew.write(gameOver);
                    break;
            }
            return mplew.getPacket();
        }

        public static byte[] onEmotion(int charid, int eid) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(163);
            mplew.writeInt(charid);
            mplew.writeInt(eid);
            return mplew.getPacket();
        }
    }

    public static byte[] onShowText(String str) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(165);
        mplew.writeMapleAsciiString(str);
        return mplew.getPacket();
    }

    public static final byte[] leaveResult(final byte slot) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Interaction.EXIT.action);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static class MultiYutGamePacket {

        public static byte[] createUI(final MapleCharacter Me, final MapleCharacter Other) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(20);
            mplew.write(17);// GameCode
            mplew.write(2);
            mplew.write(1);
            mplew.write(0);
            for (int a = 0; a < 2; a++) {
                MapleCharacter chr = a == 0 ? Me : Other;
                PacketHelper.addCharLook(mplew, chr, chr != Me, false);
                mplew.writeMapleAsciiString(chr.getName());
                mplew.writeShort(chr.getJob());
                mplew.writeInt(0);
                mplew.write(a == 1 ? -1 : 1);
            }
            // CMultiYutGameDlg::OnEnterResult
            mplew.writeInt(2);
            for (int a = 0; a < 2 /*+ 1*/; a++) {
                MapleCharacter chr = a == 1 ? Me : Other;
                if (chr.getKeyValue(20190205, "win") == -1) {
                    chr.setKeyValue(20190205, "win", "0");
                }
                if (chr.getKeyValue(20190205, "lose") == -1) {
                    chr.setKeyValue(20190205, "lose", "0");
                }
                if (chr.getKeyValue(20190205, "draw") == -1) {
                    chr.setKeyValue(20190205, "draw", "0");
                }
                mplew.writeInt(chr.getId());
                mplew.writeMapleAsciiString(chr.getName());
                mplew.writeInt(chr.getKeyValue(20190205, "win"));// win
                mplew.writeInt(chr.getKeyValue(20190205, "lose"));// lose
                mplew.writeInt(chr.getKeyValue(20190205, "draw"));// draw
                mplew.writeInt(Randomizer.nextInt(9));// 말
                mplew.writeInt(a);
            }
            return mplew.getPacket();
        }

        public static byte[] onInit(MultiYutPlayer mup) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());

            mplew.write(117);
//            mplew.write(myTeam);

            mplew.write(mup.getYuts().size());

            for (Yut yut : mup.getYuts()) {
                mplew.write(yut.getObjectId());

            }

            return mplew.getPacket();
        }
    }

    public static class MonsterPyramidPacket {

        public static byte[] createUI(List<MapleCharacter> chrs) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.MONSTER_PYRAMID.getValue());
            //scoreInfo
            mplew.write(1);

            mplew.writeInt(2);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);

            //playerInfo
            for (int a = 0; a < chrs.size(); ++a) {
                mplew.write(2);
                mplew.writeInt(a);

                mplew.write(true);
                mplew.write(false);
                mplew.write(false);
                mplew.write(false);
                mplew.writeInt(a);
                mplew.writeInt(chrs.get(a).getId()); // chrId
                mplew.writeInt(0);
                mplew.writeInt(2);

                for (int i = 0; i < 6; ++i) {
                    mplew.writeInt(0);
                }
            }

            //setPyramidStackInfo
            for (int i = 0; i < 7; ++i) {
                for (int j = 0; j < 8 - i; ++j) {
                    mplew.write(3);
                    mplew.writeInt(i);
                    mplew.writeInt(j);

                    mplew.writeInt(-1);
                    mplew.writeInt(0);
                    mplew.writeInt(j);
                }
            }

            //unk
            mplew.write(4);
            mplew.writeInt(1);
            mplew.writeInt(3);
            mplew.writeInt(0);

            //end
            mplew.write(0);

            return mplew.getPacket();
        }

        public static byte[] onInit(List<MapleCharacter> chrs) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.MONSTER_PYRAMID.getValue());

            for (int a = 0; a < chrs.size(); ++a) {
                mplew.write(2);
                mplew.writeInt(a);

                mplew.write(true);
                mplew.write(true);
                mplew.write(false);
                mplew.write(false);
                mplew.writeInt(a);
                mplew.writeInt(chrs.get(a).getId()); // chrId
                mplew.writeInt(0);
                mplew.writeInt(2);

                for (int i = 0; i < 6; ++i) {
                    mplew.writeInt(0);
                }
            }

            mplew.write(0);

            return mplew.getPacket();
        }
    }

    public static class MultiOthelloGamePacket {

        public static byte[] createUI(final List<MapleCharacter> chrs, MapleCharacter chr, int position) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(20);
            mplew.write(19);// GameCode
            mplew.write(chrs.size());
            mplew.write(position);
            for (int a = 0; a < chrs.size(); a++) {
                mplew.write(a);
                PacketHelper.addCharLook(mplew, chrs.get(a), chr.getId() == chrs.get(a).getId(), false);
                mplew.writeMapleAsciiString(chrs.get(a).getName());
                mplew.writeShort(chrs.get(a).getJob());
                mplew.writeInt(0);
            }
            mplew.write(-1);

            return mplew.getPacket();
        }

        public static byte[] onInit(List<BattleReverseStone> list, int stoneId) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(96);
            mplew.writeInt(stoneId);// firstStoneID
            mplew.writeInt(1000); // HP
            mplew.writeInt(15); // time
            mplew.writeInt(list.size());
            for (BattleReverseStone stone : list) {
                mplew.writeInt(stone.getStonePosition().x);
                mplew.writeInt(stone.getStonePosition().y);
                mplew.writeInt(stone.getStoneId());
            }
            return mplew.getPacket();
        }

        public static byte[] onBoardUpdate(boolean myTurn, final Point pt, final int StoneId, final int HP, final List<BattleReverseStone> list) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(185);
            mplew.write(myTurn);
            mplew.writeInt(pt.x);
            mplew.writeInt(pt.y);
            mplew.writeInt(StoneId);
            mplew.write(true);
            mplew.writeInt(list.size());
            for (BattleReverseStone stone : list) {
                mplew.writeInt(stone.getStonePosition().x);
                mplew.writeInt(stone.getStonePosition().y);
                mplew.writeInt(stone.getStoneId());
            }
            mplew.writeInt(HP);

            return mplew.getPacket();
        }

        public static byte[] onResult(final int nResult) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(186);
            mplew.writeInt(nResult);//0~1:Lose 2~3:Draw 4:Win

            return mplew.getPacket();
        }
    }

    public static byte[] SetupZodiacInfo() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ZodiacInfo.getValue());
        mplew.writeInt(0);
        mplew.write(1);
        mplew.write(HexTool.getByteArrayFromHexString("A0 96 97 C6 D4 09 00 00"));
        return mplew.getPacket();
    }

    public static byte[] ZodiacRankInfo(int cid, int rank) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ZodiacRankInfo.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(0);
        mplew.writeInt(rank);

        return mplew.getPacket();
    }


    public static byte[] playSound(String SE) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.PLAY_SOUND.getValue());
        packet.writeMapleAsciiString(SE);
        return packet.getPacket();
    }

    public static final byte[] BlackLabel(String msg, int delay, int textspeed, int type, int x, int y, int type1, int type2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_EFFECT.getValue());
        mplew.write(61);
        mplew.writeMapleAsciiString(msg);
        mplew.writeInt(delay);
        mplew.writeInt(textspeed);
        mplew.writeInt(type);
        mplew.writeInt(x);
        mplew.writeInt(y);
        mplew.writeInt(type1);
        mplew.writeInt(type2);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeShort(0);
        mplew.writeLong(0L);
        return mplew.getPacket();
    }
}
