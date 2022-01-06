/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.life;

import tools.Triple;

import java.util.ArrayList;
import java.util.List;

public class MobAttack {

    private final int afterAttack;
    private final int fixAttack;
    private final int action;
    private final int cooltime;
    private final int onlyAfterAttack;
    private final int afterAttackCount;
    private final List<Triple<Integer, Integer, Integer>> skills = new ArrayList<>();

    public MobAttack(final int action, final int afterAttack, final int fixAttack, final int onlyAfterAttack, final int cooltime, final int afterAttackCount) {
        this.action = action;
        this.afterAttack = afterAttack;
        this.fixAttack = fixAttack;
        this.onlyAfterAttack = onlyAfterAttack;
        this.cooltime = cooltime;
        this.afterAttackCount = afterAttackCount;
    }

    public List<Triple<Integer, Integer, Integer>> getSkills() {
        return skills;
    }

    public void addSkill(int skillId, int skillLevel, int delay) {
        skills.add(new Triple<>(skillId, skillLevel, delay));
    }

    public int getAction() {
        return action;
    }

    public int getAfterAttack() {
        return afterAttack;
    }

    public boolean isOnlyAfterAttack() {
        return onlyAfterAttack == 1;
    }

    public int getFixAttack() {
        return this.fixAttack;
    }

    public int getCoolTime() {
        return cooltime;
    }

    public int getAfterAttackCount() {
        return afterAttackCount;
    }
}
