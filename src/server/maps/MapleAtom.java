package server.maps;

import java.util.ArrayList;
import java.util.List;

public class MapleAtom {

    private boolean byMob, toMob;
    private int dwUserOwner, dwTargetId, nForceAtomType, nSkillId, dwFirstTargetId, nItemId, nForcedTargetX, nForcedTargetY, nArriveDir, nArriveRange, dwSummonObjectId, dwUnknownPoint;
    private List<ForceAtom> forceAtoms = new ArrayList<>();
    private List<Integer> dwTargets = new ArrayList<>();

    public MapleAtom(boolean byMob, int dwTargetId, int type, boolean toMob, int skillId, int nForcedTargetX, int nForcedTargetY) {
        this.byMob = byMob;
        this.dwTargetId = dwTargetId;
        this.nForceAtomType = type;
        this.toMob = toMob;
        this.nSkillId = skillId;
        this.nForcedTargetX = nForcedTargetX;
        this.nForcedTargetY = nForcedTargetY;
    }

    public void addForceAtom(ForceAtom forceAtom) {
        forceAtoms.add(forceAtom);
    }

    public List<ForceAtom> getForceAtoms() {
        return forceAtoms;
    }

    public void setForceAtoms(List<ForceAtom> forceAtoms) {
        this.forceAtoms = forceAtoms;
    }

    public boolean isByMob() {
        return byMob;
    }

    public void setByMob(boolean byMob) {
        this.byMob = byMob;
    }

    public int getDwUserOwner() {
        return dwUserOwner;
    }

    public void setDwUserOwner(int dwUserOwner) {
        this.dwUserOwner = dwUserOwner;
    }

    public int getDwTargetId() {
        return dwTargetId;
    }

    public void setDwTargetId(int dwTargetId) {
        this.dwTargetId = dwTargetId;
    }

    /*
     * type
     * 0 : 데몬 포스
     * 1 : 느와르 카르트
     * 2 : 윌 오브 소드
     * 3 : 쉴드 체이싱, 메기도 플레임, 소울 시커
     * 4 : 여우령, 쉴드 체이싱 / 소울 시커 재생성
     * 5 : 이지스 시스템
     * 6 : 핀 포인트 로켓
     * 7 : 트라이플링 웜
     * 8 : 스톰 브링어
     * 9 : 제로 타임포스
     * 10 : 퀴버 카트리지
     * 11 : 마크 오브 나이트로드
     * 12 : 메소 익소플로젼
     * 13 : 불 여우령
     * 
     * 15 : 쉐도우 배트
     * 16 : 쉐도우 배트 재생성
     * 17 : 오비탈 플레임
     * 
     * 20 : 호밍 미사일
     * 
     * 22 : 텔레키네시스
     * 
     * 24 : 마법 잔해
     * 25 : 소울 시커 엑스퍼트
     * 26 : 소울 시커 엑스퍼트 재생성
     * -- V 매트릭스 이후 --
     * 27 : 가이디드 애로우
     * 28 : 도트 퍼니셔
     * 29 : 에너지 버스트, 조디악 레이
     * 30 : 마이크로 미사일 컨테이너
     * 31 : 잔영의 시
     * 32 : 윌 오브 소드 : 스트라이크
     * 33 : 블랙잭
     * 34 : 아이들 웜
     * 
     * 36 : 크리스탈:자벨린
     * 37 : 크리스탈:오브
     * 38 : 글로리윙:강화 자벨린
     * 
     * 42 : 쉐도우 바이트
     * 43 : 아크 1차 스펠
     * 44 : 아크 2차 스펠
     * 45 : 아크 3차 스펠
     * 46 : 아크 4차 스펠
     * 47 : 아크 2차 온오프 투사체
     * 48 : 아크 3차 칼
     * 49 : 다크로드의 비전서
     * 50 : 보우마스터 불꽃화살
     * 51 : 윈드 윌
     * 52 : 파란 화살 (좀더 날렵함) // 연합용 데이터
     * 53 : 빛나는 미사일 // 연합용 데이터
     * 54 : 파란 화살 (좀더 뚱뚱함) // 연합용 데이터
     * 55 : 와일드 그레네이드 같은 공 // 연합용 데이터
     * 56 : 카디널 디스차지
     * 57 : 마법 화살
     * 58 : 스플릿 미스텔
     * 
     * 60 : 환영 분신부
     * 61 : 권술 : 호접지몽
     * 63 : 마봉 호로부
     */
    public int getnForceAtomType() {
        return nForceAtomType;
    }

    public void setnForceAtomType(int nForceAtomType) {
        this.nForceAtomType = nForceAtomType;
    }

    public boolean isToMob() {
        return toMob;
    }

    public void setToMob(boolean toMob) {
        this.toMob = toMob;
    }

    public List<Integer> getDwTargets() {
        return dwTargets;
    }

    public void setDwTargets(List<Integer> dwTargets) {
        this.dwTargets = dwTargets;
    }

    public int getnSkillId() {
        return nSkillId;
    }

    public void setnSkillId(int nSkillId) {
        this.nSkillId = nSkillId;
    }

    public int getDwFirstTargetId() {
        return dwFirstTargetId;
    }

    public void setDwFirstTargetId(int dwFirstTargetId) {
        this.dwFirstTargetId = dwFirstTargetId;
    }

    public int getnItemId() {
        return nItemId;
    }

    public void setnItemId(int nItemId) {
        this.nItemId = nItemId;
    }

    public int getnForcedTargetX() {
        return nForcedTargetX;
    }

    public void setnForcedTargetX(int nForcedTargetX) {
        this.nForcedTargetX = nForcedTargetX;
    }

    public int getnForcedTargetY() {
        return nForcedTargetY;
    }

    public void setnForcedTargetY(int nForcedTargetY) {
        this.nForcedTargetY = nForcedTargetY;
    }

    public int getnArriveDir() {
        return nArriveDir;
    }

    public void setnArriveDir(int nArriveDir) {
        this.nArriveDir = nArriveDir;
    }

    public int getnArriveRange() {
        return nArriveRange;
    }

    public void setnArriveRange(int nArriveRange) {
        this.nArriveRange = nArriveRange;
    }

    public int getDwSummonObjectId() {
        return dwSummonObjectId;
    }

    public void setDwSummonObjectId(int dwSummonObjectId) {
        this.dwSummonObjectId = dwSummonObjectId;
    }

    public int getDwUnknownPoint() {
        return dwUnknownPoint;
    }

    public void setDwUnknownPoint(int dwUnknownPoint) {
        this.dwUnknownPoint = dwUnknownPoint;
    }
}
