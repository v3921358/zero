package client;

import constants.GameConstants;
import handling.Buffstat;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import server.Randomizer;
import tools.Pair;

public enum MapleBuffStat implements Serializable, Buffstat {

    // 1.2.343 -> 1.2.350 +0
    IndiePad(0),
    IndieMad(1),
    IndiePdd(2),
    IndieHp(3),
    IndieHpR(4), // ok
    IndieMp(5),
    IndieMpR(6), // ok
    IndieAcc(7), // dummy
    IndieEva(8), // dummy
    IndieJump(9),
    IndieSpeed(10),
    IndieAllStat(11), // ok
    IndieAllStatR(12), // ok
    IndieDodgeCriticalTime(13),
    IndieExp(14),
    IndieBooster(15),
    IndieFixedDamageR(16),
    PyramidStunBuff(17),
    PyramidFrozenBuff(18),
    PyramidFireBuff(19),
    PyramidBonusDamageBuff(20), // ok
    IndieRelaxEXP(21),
    IndieStr(22), // ok
    IndieDex(23), // ok
    IndieInt(24), // ok
    IndieLuk(25), // ok
    IndieDamR(26),
    IndieScriptBuff(27),
    IndieMaxDamageR(28),
    IndieAsrR(29),
    IndieTerR(30),
    IndieCr(31),
    IndiePddR(32), // no ??
    IndieCD(33),
    IndieBDR(34),
    IndieStatR(35),
    IndieStance(36), // ok
    IndieIgnoreMobPdpR(37),
    IndieEmpty(38),
    IndiePadR(39), // ok
    IndieMadR(40), // ok
    IndieEvaR(41),
    IndieDrainHP(42),
    IndiePmdR(43),
    IndieForceJump(44),
    IndieForceSpeed(45),
    IndieDamageReduce(46),
    IndieSummon(47),
    IndieReduceCooltime(48), // ok
    IndieNotDamaged(49), // ok
    IndieJointAttack(50), // 1.2.287
    IndieKeyDownMoving(51),
    IndieUnkIllium(52),
    IndieEvasion(53),
    IndieShotDamage(54),
    IndieSuperStance(55), // 1.2.329
    IndieGrandCross(56),
    IndieBarrierDischarge(57), // 1.2.329
    Indie_STAT_COUNT(58),
    Indie_DamageReflection(59), //히어로 뎀반사
    Unk60(60),
    Unk61(61),
    IndieFloating(62), // 1.2.329
    IndieWickening(63), //  334 맞추는중
    IndieWickening1(64), //  324
    IndieWickening2(65), //  324
    IndieBlockSkill(66),
    IndieDarknessAura(67),
    IndieNormalMobDamage(70),
    Pad(73), //342 +1 시작
    Pdd(74),
    Mad(75),
    Acc(76),
    Eva(77),
    Craft(78), //1.2.1029
    Speed(79),
    Jump(80),
    MagicGaurd(81),
    DarkSight(82),
    Booster(83), //335 ?테스트ok79
    PowerGaurd(84),
    MaxHP(85),
    MaxMP(86),
    Invincible(87),
    SoulArrow(88),
    Stun(89), // 320 IDA
    Poison(90), // 320 IDA
    Seal(91), // 320 IDA
    Darkness(92), // 320 IDA
    ComboCounter(93), // 320 IDA
    WeaponCharge(94), // 320 IDA
    BlessedHammer(95), // 320 IDA
    BlessedHammer2(96), // 1.2.350 IDA
    SnowCharge(97),
    HolySymbol(98), // 320 IDA
    MesoUp(99),
    ShadowPartner(100), // 320 IDA
    PickPocket(101),
    MesoGuard(102),
    THAW(103),
    Weakness(104), // 320 IDA
    Curse(105), // 320 IDA
    Slow(106), // 320 IDA
    Morph(107), // 320 IDA
    Recovery(108),
    BasicStatUp(109), //334 ok  시작
    Stance(110),
    SharpEyes(111), // IDA
    ManaReflection(112),
    Attract(113), // 320 IDA
    NoBulletConsume(114), // 320 IDA
    Infinity(115),
    AdvancedBless(116), // 320 IDA
    ILLUSION(117), //1.2.1029
    Blind(118), //de : 0x20000000, 13
    Concentration(119), //1.2.1029
    BanMap(120), // 320 IDA
    MaxLevelBuff(121),
    MesoUpByItem(122), //1.2.1029 ++
    WealthOfUnion(123), //1.2.284
    RuneOfGreed(124),
    Ghost(125), // 320 IDA
    Barrier(126), // 320 IDA
    ReverseInput(127, 68), // 320 IDA
    ItemUpByItem(128), //1.2.1029 ++
    RespectPImmune(129), // 320 IDA
    RespectMImmune(130), // 320 IDA
    DefenseAtt(131), // 320 IDA
    DefenseState(132), // 320 IDA
    DojangBerserk(133), // 320 IDA
    DojangInvincible(134), // 320 IDA
    DojangShield(135), // 320 IDA
    SoulMasterFinal(136), //1.2.1029 ++
    WindBreakerFinal(137), // 320 IDA
    ElementalReset(138),
    HideAttack(139), // 320 IDA
    EVENT_RATE(140), //1.2.1029 ++
    AranCombo(141),
    AuraRecovery(142),
    Unk136(143),
    BodyPressure(144),
    RepeatEffect(145), // 320 IDA
    ExpBuffRate(146), // 342 sniffer 확실
    StopPortion(147), // 320 IDA
    StopMotion(148), // 320 IDA
    Fear(149), // 320 IDA
    HiddenPieceOn(150), // 320 IDA
    MagicShield(151), // 320 IDA
    ICE_SKILL(152),
    SoulStone(153),
    Flying(154), // IDA
    Frozen(155, 73),
    AssistCharge(156),
    Enrage(157),
    DrawBack(158), // 320 IDA
    NotDamaged(159), // 320 IDA
    FinalCut(160), // 320 IDA
    HowlingParty(161),
    BeastFormDamage(162),
    Dance(163), // 320 IDA
    EnhancedMaxHp(164),
    EnhancedMaxMp(165),
    EnhancedPad(166),
    EnhancedMad(167),
    EnhancedPdd(168),
    PerfectArmor(169),
    UnkBuffStat2(170),
    IncreaseJabelinDam(171),
    HowlingCritical(168),
    HowlingMaxMp(169),
    HowlingDefence(170),
    HowlingEvasion(171),
    PinkbeanMinibeenMove(172), //1.2.1029
    Sneak(173), // IDA
    Mechanic(174), // 320 IDA
    BeastFormMaxHP(175), // de : 11 line
    DiceRoll(176), // IDA
    BlessingArmor(177), // IDA
    DamR(178),
    TeleportMastery(179),
    CombatOrders(180),
    Beholder(181), // 320 IDA
    DispelItemOption(182),
    Inflation(183), // 320 IDA
    OnixDivineProtection(184),
    Web(185), // 320 IDA
    Bless(186),
    TimeBomb(187), // 320 IDA
    DisOrder(188), // 320 IDA
    Thread(189), // 320 IDA
    Team(190), // 320 IDA
    Explosion(191),
    BUFFLIMIT(192),
    STR(193),
    INT(194),
    DEX(195),
    LUK(196),
    DISPEL_BY_FIELD(197),
    DarkTornado(198),
    PVPDamage(199),
    PVP_SCORE_BONUS(200),
    PVP_INVINCIBLE(201),
    PvPRaceEffect(202),
    WeaknessMdamage(203),
    Frozen2(204, 73),
    PVP_DAMAGE_SKILL(205),
    AmplifyDamage(206),
    Shock(207), // 1.2.350 IDA
    InfinityForce(208),
    IncMaxHP(209),
    IncMaxMP(210),
    HolyMagicShell(211), // 342 sniffer 211 확실
    KeyDownTimeIgnore(212),
    ArcaneAim(213),
    MasterMagicOn(214),
    Asr(215),
    Ter(216),
    DamAbsorbShield(217), // 320 IDA
    DevilishPower(218), // 342 확실
    Roulette(219),
    SpiritLink(220), // 320 IDA
    AsrRByItem(221),
    Event(222), // 320 IDA
    CriticalIncrease(223),
    DropItemRate(224),
    DropRate(225),
    ItemInvincible(226),
    Awake(227),
    ItemCritical(228),
    ItemEvade(229),
    Event2(230), // 320 IDA
    DrainHp(231), // IDA
    IncDefenseR(232),
    IncTerR(233),
    IncAsrR(234),
    DeathMark(235), // 320 IDA
    Infiltrate(236),
    Lapidification(237), // 320 IDA
    VenomSnake(238), // 320 IDA
    CarnivalAttack(239), // 310 IDA
    CarnivalDefence(240),
    CarnivalExp(241),
    SlowAttack(242),
    PyramidEffect(243), // 320 IDA
    KillingPoint(244), // 320 IDA
    Unk248(245),
    KeyDownMoving(246), // 320 IDA
    IgnoreTargetDEF(247), // 320 IDA
    ReviveOnce(248),
    Invisible(249), // 320 IDA
    EnrageCr(250),
    EnrageCrDamMin(251),
    Judgement(252), // 1.2.350 IDA
    DojangLuckyBonus(253),
    PainMark(254), // 320 IDA
    Magnet(255), // 320 IDA
    MagnetArea(256), // 320 IDA
    GuidedArrow(257), // 1.2.350 IDA
    UnkBuffStat4(258), // 1.2.350 IDA
    BlessMark(259), // 1.2.350 IDA
    BonusAttack(260), // 1.2.350 IDA
    UnkBuffStat5(261), // 1.2.350 IDA
    FlowOfFight(262),
    GrandCrossSize(263), // 1.2.350 IDA
    LuckOfUnion(264),
    UNK265(265),

    // 1.2.343 -> 1.2.350 +2
    VampDeath(267), // 320 IDA
    BlessingArmorIncPad(268), // IDA
    KeyDownAreaMoving(269), // 1.2.350 IDA
    Larkness(270), // 1.2.350 IDA
    StackBuff(271), // 1.2.350 IDA
    BlessOfDarkness(272), // 1.2.350 IDA
    AntiMagicShell(273),
    LifeTidal(274),
    HitCriDamR(275),
    SmashStack(276),
    RoburstArmor(277),
    ReshuffleSwitch(278),
    SpecialAction(279),
    VampDeathSummon(280),
    StopForceAtominfo(281),
    SoulGazeCriDamR(282),
    Affinity(283),
    PowerTransferGauge(284),
    AffinitySlug(285),
    Trinity(286),
    INCMAXDAMAGE(287),
    BossShield(288),
    MobZoneState(289),
    GiveMeHeal(290),
    TouchMe(291),
    Contagion(292),
    ComboUnlimited(293),
    SoulExalt(294),
    IgnorePCounter(295),
    IgnoreAllCounter(296),
    IgnorePImmune(297),
    IgnoreAllImmune(298),
    UnkBuffStat6(299),
    FireAura(300),
    VengeanceOfAngel(301),
    HeavensDoor(302),
    Preparation(303),
    BullsEye(304),
    IncEffectHPPotion(305),
    IncEffectMPPotion(306),
    BleedingToxin(307),
    IgnoreMobDamR(308),
    Asura(309),
    MegaSmasher(310),
    FlipTheCoin(311),
    UnityOfPower(312),
    Stimulate(313),
    ReturnTeleport(314),
    DropRIncrease(315),
    IgnoreMobPdpR(316),
    BdR(317),
    CapDebuff(318), // 1.2.350 IDA
    Exceed(319),
    DiabloicRecovery(320),
    FinalAttackProp(321),
    Unk319(322),
    OverloadCount(323), // 1.2.350 IDA
    Buckshot(324),
    FireBomb(325), // 1.2.350 IDA
    HalfstatByDebuff(326), // 1.2.350 IDA
    SurplusSupply(327), // 1.2.350 IDA
    SetBaseDamage(328),
    EvaR(329),
    NewFlying(330), // 1.2.350 IDA
    AmaranthGenerator(331), // 1.2.350 IDA
    OnCapsule(332),
    CygnusElementSkill(333), // 1.2.350 IDA
    StrikerHyperElectric(334), // 1.2.350 IDA
    EventPointAbsorb(335), // 1.2.350 IDA
    EventAssemble(336), // 1.2.350 IDA
    StormBringer(337),
    AddAvoid(338),
    AddAcc(339),
    Albatross(340), // 1.2.350 IDA
    Translucence(341), // 1.2.350 IDA
    PoseType(342), // 1.2.350 IDA
    LightOfSpirit(343),
    ElementSoul(344),
    GlimmeringTime(345),
    SolunaTime(346),
    WindWalk(347),
    SoulMP(348),
    FullSoulMP(349),
    SoulSkillDamageUp(350),
    ElementalCharge(351),
    Listonation(352),
    CrossOverChain(353),
    ChargeBuff(354),
    Reincarnation(355),
    ChillingStep(356),
    DotBasedBuff(357),
    BlessingAnsanble(358),
    ComboCostInc(359),

    // 1.2.343 -> 1.2.350 +3
    ExtremeArchery(361),
    NaviFlying(362), // 1.2.350 IDA
    QuiverCatridge(363),
    AdvancedQuiver(364),
    UserControlMob(365),
    ImmuneBarrier(366),
    ArmorPiercing(367),
    CriticalGrowing(368),
    CardinalMark(369),
    QuickDraw(370),
    BowMasterConcentration(371),
    TimeFastABuff(372),
    TimeFastBBuff(373),
    GatherDropR(374),
    AimBox2D(375),
    TrueSniping(376), // 1.2.350 IDA
    DebuffTolerance(377),
    Unk376(378),
    DotHealHPPerSecond(379),
    DotHealMPPerSecond(380),
    SpiritGuard(381), // 1.2.350 IDA
    PreReviveOnce(382),
    SetBaseDamageByBuff(383),
    LimitMP(384),
    ReflectDamR(385),
    ComboTempest(386), // 1.2.350 IDA
    MHPCutR(387),
    MMPCutR(388),
    SelfWeakness(389),
    ElementDarkness(390),
    FlareTrick(391),
    Ember(392),
    Dominion(393),
    SiphonVitality(394),
    DarknessAscension(395),
    BossWaitingLinesBuff(396),
    DamageReduce(397),
    ShadowServant(398),
    ShadowIllusion(399),
    KnockBack(400),
    IgnisRore(401),
    ComplusionSlant(402), // 1.2.350 IDA
    JaguarSummoned(403), // 1.2.350 IDA
    JaguarCount(404),
    SSFShootingAttack(405),
    DEVIL_CRY(406),
    ShieldAttack(407),
    DarkLighting(408), // 1.2.350 IDA
    AttackCountX(409), // 1.2.350 IDA
    BMageDeath(410),
    BombTime(411), // 1.2.350 IDA
    NoDebuff(412),
    BattlePvP_Mike_Shield(413),
    BattlePvP_Mike_Bugle(414),
    AegisSystem(415),
    SoulSeekerExpert(416),
    HiddenPossession(417),
    ShadowBatt(418),
    MarkofNightLord(419),
    WizardIgnite(420),
    FireBarrier(421), // 1.2.350 IDA
    CHANGE_FOXMAN(422),
    HolyUnity(423), // 1.2.350 IDA
    DemonFrenzy(424),
    ShadowSpear(425),
    UnkBuffStat9(426), // 1.2.350 IDA
    Ellision(427),
    QuiverFullBurst(428),
    LuminousPerfusion(429),
    WildGrenadier(430),
    GrandCross(431),
    Unk432(432),
    BattlePvP_Helena_Mark(433),
    BattlePvP_Helena_WindSpirit(434),
    BattlePvP_LangE_Protection(435),
    BattlePvP_LeeMalNyun_ScaleUp(436),
    BattlePvP_Revive(437),
    PinkbeanAttackBuff(438),
    PinkbeanRelax(439),
    PinkbeanRollingGrade(440),
    PinkbeanYoYoStack(441),
    UnkBuffStat10(442),
    RandAreaAttack(443),
    NextAttackEnhance(444),
    BeyondNextAttackProb(445),
    NautilusFinalAttack(446),
    ViperTimeLeap(447),
    RoyalGuardState(448),
    RoyalGuardPrepare(449),
    MichaelSoulLink(450), // 1.2.350 IDA
    Unk451(451),
    TryflingWarm(452),
    AddRange(453),
    KinesisPsychicPoint(454),
    KinesisPsychicOver(455),
    KinesisPsychicShield(456),
    KinesisIncMastery(457),
    KinesisPsychicEnergeShield(458), // 1.2.350 IDA
    BladeStance(459), // 1.2.350 IDA
    DebuffActiveHp(460),
    DebuffIncHp(461),
    MortalBlow(462),
    SoulResonance(463),
    Fever(464), // 1.2.350 IDA
    SikSin(465),
    TeleportMasteryRange(466),
    FixCooltime(467),
    IncMobRateDummy(468),
    AdrenalinBoost(469), // 1.2.350 IDA
    AranSmashSwing(470),
    AranDrain(471),
    AranBoostEndHunt(472),
    HiddenHyperLinkMaximization(473),
    RWCylinder(474),
    RWCombination(475),
    UnkBuffStat12(476), // 1.2.350 IDA
    RwMagnumBlow(477), // 1.2.350 IDA
    RwBarrier(478), // 1.2.350 IDA
    RWBarrierHeal(479),
    RWMaximizeCannon(480),
    RWOverHeat(481),
    UsingScouter(482),
    RWMovingEvar(483),
    Stigma(484), // 1.2.350 IDA
    InstallMaha(485), // 1.2.350 IDA
    CooldownHeavensDoor(486),
    CooldownRune(487),
    PinPointRocket(488),
    UnkBuffStat13(489),
    Transform(490), // 1.2.350 IDA
    EnergyBurst(491), // 1.2.350 IDA
    Striker1st(492), // 1.2.350 IDA
    BulletParty(493), // 1.2.350 IDA
    SelectDice(494), // 1.2.350 IDA
    Pray(495), // 1.2.350 IDA
    ChainArtsFury(496),
    DamageDecreaseWithHP(497),
    Unk498(498),
    AuraWeapon(499),
    OverloadMana(500), // 1.2.350 IDA
    RhoAias(501), // 1.2.350 IDA
    PsychicTornado(502), // 1.2.350 IDA
    SpreadThrow(503),
    HowlingGale(504),
    VMatrixStackBuff(505),
    ShadowAssult(506),
    MultipleOption(507),
    Unk508(508),
    BlitzShield(509),
    SplitArrow(510),
    FreudsProtection(511), // 1.2.350 IDA
    Overload(512), // 1.2.350 IDA
    Spotlight(513), // 1.2.350 IDA
    UnkBuffStat16(514), // 1.2.350 IDA
    WeaponVariety(515),
    GloryWing(516), // 1.2.350 IDA
    ShadowerDebuff(517),
    OverDrive(518), // 1.2.350 IDA
    Etherealform(519), // 1.2.350 IDA
    ReadyToDie(520), // 1.2.350 IDA
    CriticalReinForce(521), // 1.2.350 IDA
    CurseOfCreation(522), // 1.2.350 IDA
    CurseOfDestruction(523), // 1.2.350 IDA
    BlackMageDebuff(524), // 1.2.350 IDA
    BodyOfSteal(525), // 1.2.350 IDA
    UnkBuffStat18(526), // 1.2.350 IDA
    UnkBuffStat19(527), // 1.2.350 IDA
    HarmonyLink(528), // 1.2.350 IDA
    FastCharge(529), // 1.2.350 IDA
    UnkBuffStat20(530),
    CrystalBattery(531),
    Deus(532),
    CrystalChargeMax(533),
    UnkBuffStat22(534),
    Unk536(535),
    Unk537(536),
    Unk538(537),
    SpectorGauge(538),
    SpectorTransForm(539), // 1.2.350 IDA
    PlainBuff(540),
    ScarletBuff(541),
    GustBuff(542),
    AbyssBuff(543),
    ComingDeath(544), // 1.2.350 IDA
    FightJazz(545),
    ChargeSpellAmplification(546),
    InfinitySpell(547),
    MagicCircuitFullDrive(548),
    LinkOfArk(549),
    MemoryOfSource(550),
    UnkBuffStat26(551),
    WillPoison(552), // 1.2.350 IDA
    Unk556(553),
    UnkBuffStat28(554),
    CooltimeHolyMagicShell(555),
    Striker3rd(556),
    ComboInstict(557),
    WindWall(558),
    UnkBuffStat29(559),
    SwordOfSoulLight(560),
    MarkOfPhantomStack(561),
    MarkOfPhantomDebuff(562),
    Unk565(563),
    Unk566(564),
    Unk567(565),
    Unk568(566),
    Unk569(567), // 눈내리는 버프스탯
    EventSpecialSkill(568), // 코인 모으기용 버프스탯인듯, 네오 머쉬룸 와칭도 동일
    PmdReduce(569),
    ForbidOpPotion(570),
    ForbidEquipChange(571),
    YalBuff(572), // 1.2.350 IDA
    IonBuff(573), // 1.2.350 IDA
    Unk576(574),
    UnkBuffStat36(575),
    Unk578(576),
    Protective(577), // 1.2.350 IDA
    UnkBuffStat38(578), // 1.2.350 IDA
    AncientGuidance(579),
    UnkBuffStat39(580),
    UnkBuffStat40(581), // 1.2.350 IDA
    UnkBuffStat41(582), // 1.2.350 IDA
    UnkBuffStat42(583), // 1.2.350 IDA
    UnkBuffStat43(584),
    New1124_1(585),
    New1124_2(586),

    // 1.2.350 -> 1.2.1124 + 2
    UnkBuffStat44(587), // 1.2.1124 IDA
    Bless5th(588), // ?
    UnkBuffStat45(589), // 1.2.350 IDA
    UnkBuffStat46(590),
    UnkBuffStat47(591),
    UnkBuffStat48(592),
    UnkBuffStat49(593),
    HoyoungThirdProperty(594),
    PapyrusOfLuck(595),
    UnkBuffStat50(596), // 1.2.350 IDA
    TidalForce(597),
    Alterego(598),
    AltergoReinforce(599), // 1.2.350 IDA
    ButterflyDream(600),
    Sungi(601),
    UnkBuffStat51(602),
    EmpiricalKnowledge(603),
    UnkBuffStat52(604),
    UnkBuffStat53(605), // 1.2.350 IDA
    Graffiti(606), // 1.2.350 IDA
    DreamDowon(607),
    AdelGauge(609),
    Creation(610),
    Dike(611),
    Wonder(612),
    Restore(613),
    Novility(614), // 1.2.350 IDA
    AdelResonance(615),
    RuneOfPure(616),
    DuskDarkness(617), // 1.2.350 IDA
    YellowAura(619), // 1.2.350 IDA
    DrainAura(620), // 1.2.350 IDA
    BlueAura(621), // 1.2.350 IDA
    DarkAura(622), // 1.2.350 IDA
    DebuffAura(623), // 1.2.350 IDA
    UnionAura(624), // 1.2.350 IDA
    IceAura(625), // 1.2.350 IDA
    KnightsAura(626), // 1.2.350 IDA
    UnkBuffStat622(627), // 1.2.350 IDA
    ZeroAuraStr(628), // 1.2.350 IDA
    ZeroAuraSpd(629), // 1.2.350 IDA
    UnkBuffStat57(630), // 1.2.350 IDA
    Revenant(631),
    RevenantDamage(632),
    UNK633(633),
    UNK634(634), // 1.2.350 IDA
    PhotonRay(635), // 1.2.350 IDA
    UNK636(636), // 1.2.350 IDA
    RoyalKnights(637),
    RepeatinCartrige(640),
    ThrowBlasting(642),
    SageElementalClone(643),
    DarknessAura(645), // 1.2.350 IDA
    UNK646(646),
    UNK647(647),
    UnkBuffStat60(648), // 1.2.350 IDA
    RelicUnbound(649),
    unk647(651),
    Malice(653),
    Possession(654),
    DeathBlessing(655),
    ThanatosDescent(656), // 1.2.350 IDA
    RemainInsence(657),
    GripOfAgony(658),
    DragonPang(659),
    SerenDebuffs(660),
    SerenDebuff(661),
    SerenDebuffUnk(662),
    KainLink(663),

    UNK668(670), // 1.2.350 IDA
    UNK671(671),
    UNK672(672), // 1.2.350 IDA

    ReadVeinOfInfinity(677),
    SeedOfMountain(678),
    GuardOfMountain(679),
    AbsorptionRiver(680),
    AbsorptionWind(681),
    AbsorptionSun(682),
    FreeVein(683),

    FriendOfNature(686),
    TraceOfVein(687),

    // 1.2.350 -> 1.2.1124 +5
    EnergyCharged(688),
    DashJump(689),
    DashSpeed(690),
    RideVehicle(691),
    PartyBooster(692),
    GuidedBullet(693),
    Undead(694, 1, 133),
    RideVehicleExpire(695),
    RelikGauge(696),
    Grave(697),
    CountPlus1(698);

    private static final long serialVersionUID = 0L;
    private int buffstat;
    private int first;
    private boolean stacked = false;
    private int disease;
    private int flag;
    // [8] [7] [6] [5] [4] [3] [2] [1]
    // [0] [1] [2] [3] [4] [5] [6] [7]

    private MapleBuffStat(int flag) {
        this.buffstat = (1 << (31 - (flag % 32)));
        this.setFirst(GameConstants.MAX_BUFFSTAT - (byte) Math.floor(flag / 32));
        this.setStacked(name().startsWith("Indie") || name().startsWith("Pyramid"));
        this.setFlag(flag);
    }

    private MapleBuffStat(int flag, int disease) {
        this.buffstat = (1 << (31 - (flag % 32)));
        this.setFirst(GameConstants.MAX_BUFFSTAT - (byte) Math.floor(flag / 32));
        this.setStacked(name().startsWith("Indie") || name().startsWith("Pyramid"));
        this.setFlag(flag);
        this.disease = disease;
    }

    private MapleBuffStat(int buffstat, int first, boolean stacked) {
        this.buffstat = buffstat;
        this.setFirst(first);
        this.setStacked(stacked);
    }

    private MapleBuffStat(int buffstat, int first, int disease) {
        this.buffstat = buffstat;
        this.setFirst(first);
        this.disease = disease;
    }

    public final int getPosition() {
        return getFirst();//getPosition(stacked);
    }

    public final int getPosition(boolean stacked) {
        if (!stacked) {
            return getFirst();
        }
        switch (getFirst()) {
            case 16:
                return 0;
            case 15:
                return 1;
            case 14:
                return 2;
            case 13:
                return 3;
            case 12:
                return 4;
            case 11:
                return 5;
            case 10:
                return 6;
            case 9:
                return 7;
            case 8:
                return 8;
            case 7:
                return 9;
            case 6:
                return 10;
            case 5:
                return 11;
            case 4:
                return 12;
            case 3:
                return 13;
            case 2:
                return 14;
            case 1:
                return 15;
            case 0:
                return 16;
        }
        return 0; // none
    }

    public final int getValue() {
        return getBuffstat();
    }

    public final boolean canStack() {
        return isStacked();
    }

    public int getDisease() {
        return disease;
    }

    public static final MapleBuffStat getByFlag(final int flag) {
        for (MapleBuffStat d : MapleBuffStat.values()) {
            if (d.getFlag() == flag) {
                return d;
            }
        }
        return null;
    }

    public static final MapleBuffStat getBySkill(final int skill) {
        for (MapleBuffStat d : MapleBuffStat.values()) {
            if (d.getDisease() == skill) {
                return d;
            }
        }
        return null;
    }

    public static final List<MapleBuffStat> getUnkBuffStats() {
        List<MapleBuffStat> stats = new ArrayList<>();
        for (MapleBuffStat d : MapleBuffStat.values()) {
            if (d.name().startsWith("UnkBuff")) {
                stats.add(d);
            }
        }
        return stats;
    }

    public static final MapleBuffStat getRandom() {
        while (true) {
            for (MapleBuffStat dis : MapleBuffStat.values()) {
                if (Randomizer.nextInt(MapleBuffStat.values().length) == 0) {
                    return dis;
                }
            }
        }
    }

    public static boolean isEncode4Byte(Map<MapleBuffStat, Pair<Integer, Integer>> statups) {
        MapleBuffStat[] stats
                = {
                CarnivalDefence,
                SpiritLink,
                DojangLuckyBonus,
                SoulGazeCriDamR,
                PowerTransferGauge,
                ReturnTeleport,
                ShadowPartner,
                SetBaseDamage,
                QuiverCatridge,
                ImmuneBarrier,
                NaviFlying,
                Dance,
                AranSmashSwing,
                DotHealHPPerSecond,
                SetBaseDamageByBuff,
                MagnetArea,
                MegaSmasher,
                RwBarrier,
                VampDeath,
                RideVehicle,
                RideVehicleExpire,
                Protective,
                BlitzShield,
                UnkBuffStat2,
                UnkBuffStat22,};
        for (MapleBuffStat stat : stats) {
            if (statups.containsKey(stat)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSpecialBuff() {
        switch (this) {
            case EnergyCharged:
            case DashSpeed:
            case DashJump:
            case RideVehicle:
            case PartyBooster:
            case GuidedBullet:
            case Undead:
            case RideVehicleExpire:
            case RelikGauge:
            case Grave:
                return true;
            default:
                return false;
        }
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public boolean isItemEffect() {
        switch (this) {
            case DropItemRate:
            case ItemUpByItem:
            case MesoUpByItem:
            case ExpBuffRate:
            case WealthOfUnion:
            case LuckOfUnion:
                return true;
            default:
                return false;
        }
    }

    public boolean SpectorEffect() {
        switch (this) {
            case SpectorGauge:
            case SpectorTransForm:
            case PlainBuff:
            case ScarletBuff:
            case GustBuff:
            case AbyssBuff:
                return true;
            default:
                return false;
        }
    }

    public int getBuffstat() {
        return buffstat;
    }

    public void setBuffstat(int buffstat) {
        this.buffstat = buffstat;
    }

    public int getFirst() {
        return first;
    }

    public void setFirst(int first) {
        this.first = first;
    }

    public boolean isStacked() {
        return stacked;
    }

    public void setStacked(boolean stacked) {
        this.stacked = stacked;
    }
}
