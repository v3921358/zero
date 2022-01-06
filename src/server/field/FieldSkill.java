package server.field;

import server.maps.MapleNodes;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class FieldSkill {

    private int skillId, skillLevel;
    private List<SummonedSequenceInfo> summonedSequenceInfoList = new ArrayList<>();
    private List<FieldFootHold> footHolds = new ArrayList<>();
    private List<LaserInfo> laserInfoList = new ArrayList<>();
    private List<MapleNodes.Environment> envInfo = new ArrayList<>();
    private List<ThunderInfo> thunInfo = new ArrayList<>();

    public FieldSkill(int skillId, int skillLevel) {
        this.skillId = skillId;
        this.skillLevel = skillLevel;
    }

    public int getSkillId() {
        return skillId;
    }

    public int getSkillLevel() {
        return skillLevel;
    }

    public void addSummonedSequenceInfoList(SummonedSequenceInfo info) {
        summonedSequenceInfoList.add(info);
    }

    public List<SummonedSequenceInfo> getSummonedSequenceInfoList() {
        return summonedSequenceInfoList;
    }

    public void setSummonedSequenceInfoList(List<SummonedSequenceInfo> summonedSequenceInfoList) {
        this.summonedSequenceInfoList = summonedSequenceInfoList;
    }

    public List<LaserInfo> getLaserInfoList() {
        return laserInfoList;
    }

    public void setLaserInfoList(List<LaserInfo> infoList) {
        this.laserInfoList = infoList;
    }

    public List<FieldFootHold> getFootHolds() {
        return footHolds;
    }

    public void setFootHolds(List<FieldFootHold> footHolds) {
        this.footHolds = footHolds;
    }

    public List<MapleNodes.Environment> getEnvInfo() {
        return envInfo;
    }

    public void setEnvInfo(List<MapleNodes.Environment> envInfo) {
        this.envInfo = envInfo;
    }

    public List<ThunderInfo> getThunderInfo() {
        return thunInfo;
    }
    public void setThunderInfo(List<ThunderInfo> thunInfo) {
        this.thunInfo = thunInfo;
    }

    public void addThunderInfo(ThunderInfo info) {
        thunInfo.add(info);
    }

    public static class ThunderInfo {
        private Point StartPos, EndPos;
        private int info;
        private int delay;

        public ThunderInfo(Point startPos, Point endPos, int info, int delay) {
            this.StartPos = startPos;
            this.EndPos = endPos;
            this.info = info;
            this.delay = delay;
        }

        public Point getStartPosition() {
            return StartPos;
        }

        public Point getEndPosition() {
            return EndPos;
        }

        public int getInfo() {
            return info;
        }
        public int getDelay() {
            return delay;
        }
    }

    public static class LaserInfo {
        private Point position;
        private int unk1, unk2;

        public LaserInfo(Point position, int unk1, int unk2) {
            this.position = position;
            this.unk1 = unk1;
            this.unk2 = unk2;
        }

        public Point getPosition() {
            return position;
        }

        public int getUnk1() {
            return unk1;
        }

        public int getUnk2() {
            return unk2;
        }
    }

    public static class SummonedSequenceInfo {
        private int id;
        private Point position;

        public SummonedSequenceInfo(int id, Point position) {
            this.id = id;
            this.position = position;
        }

        public int getId() {
            return id;
        }

        public Point getPosition() {
            return position;
        }
    }


    public static class FieldFootHold {

        private Point pos;
        private Rectangle rect;
        private boolean isFacingLeft;

        private short set, unk;
        private int duration, interval, angleMin, angleMax, attackDelay, z;

        public FieldFootHold(int duration, int interval, int angleMin, int angleMax, int attackDelay, int z, short set, short unk, Rectangle rect, Point pos, boolean isFacingLeft) {
            this.duration = duration;
            this.interval = interval;
            this.angleMin = angleMin;
            this.angleMax = angleMax;
            this.attackDelay = attackDelay;
            this.z = z;
            this.set = set;
            this.unk = unk;
            this.rect = rect;
            this.pos = pos;
            this.isFacingLeft = isFacingLeft;
        }

        public int getDuration() {
            return duration;
        }

        public int getInterval() {
            return interval;
        }

        public int getAngleMin() {
            return angleMin;
        }

        public int getAngleMax() {
            return angleMax;
        }

        public int getAttackDelay() {
            return attackDelay;
        }

        public int getZ() {
            return z;
        }

        public short getSet() {
            return set;
        }

        public short getUnk() {
            return unk;
        }

        public boolean isFacingLeft() {
            return isFacingLeft;
        }

        public Point getPos() {
            return pos;
        }

        public Rectangle getRect() {
            return rect;
        }
    }
}