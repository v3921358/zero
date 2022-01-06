var ii = Packages.server.MapleItemInformationProvider.getInstance();
var InventoryHandler = Packages.handling.channel.handler.InventoryHandler;
var Randomizer = Packages.server.Randomizer;
var MapleInventoryType = Packages.client.inventory.MapleInventoryType;
var InventoryPacket = Packages.tools.packet.CWvsContext.InventoryPacket;
var GameConstants = Packages.constants.GameConstants;

var CubeStatus = {};
CubeStatus.UNDIFINED = -1;
CubeStatus.RED_CUBE = 0;
CubeStatus.BLACK_CUBE = 1;
CubeStatus.ADDITIONAL_CUBE = 2;
CubeStatus.WHITE_ADDITIONAL_CUBE = 3;

var Potential = {};
Potential.NONE = 0;
Potential.STR_P = 1;
Potential.DEX_P = 2;
Potential.INT_P = 3;
Potential.LUK_P = 4;
Potential.ALL_P = 5;
Potential.IGNORE_DEF = 6; //이거 핸들링 안돼고있음
Potential.WATTACK_P = 7;
Potential.MATTACK_P = 8;
Potential.DAM_P = 9;
Potential.INC_BOSSDAM_P = 10;
Potential.INC_CRIT = 11;
Potential.INC_CRIT_DAM = 12; //MIN MAX 같이 계산할 것
Potential.REDUCE_COOLTIME = 13;
Potential.INC_REWARD_PROP = 14;
Potential.INC_MESO_PROP = 15;
Potential.HP_P = 16;
Potential.MP_P = 17;
Potential.INC_SKILLLV = 18;
Potential.LV_WATK = 19;
Potential.LV_MATK = 20;
Potential.LV_STR = 21;
Potential.LV_DEX = 22;
Potential.LV_INT = 23;
Potential.LV_LUK = 24;
Potential.STR = 25;
Potential.DEX = 26;
Potential.INT = 27;
Potential.LUK = 28;
Potential.ALL = 29;
Potential.WATTACK = 30;
Potential.MATTACK = 31;

var KEEP_LEVEL_PERCENT = 1; //옵이탈
var enter = "\r\n";

var status = -1;
var chat;
var cubeItemList;
var selectedItem;
var newItem;
var renewTargerItem;

var cubeState = CubeStatus.UNDIFINED;

var PotentialStruct = function (type, value, id) {
    this.type = type;
    this.value = value;
    this.id = id;
};

var potentialMap = createPotentialMap();

function start() {
    /*
      if(!cm.getPlayer().isGM())
      {
         cm.dispose()
         cm.sendOk('버그잡는중임 ㅇㅇ.')
      }
      */

    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == 1) {
        status++;
    } else {
        cm.dispose();
        return;
    }

    chat = "#fs11#";

    if (status == 0) {
        chat += "아이템 레벨이 100 이상인 레전드리 잠재능력 아이템에만 사용 가능" + enter;
        chat += "아이템의 잠재능력이 3줄 이상인 아이템만 가능 합니다." + enter;
        chat += "#b<Before>#k 옵션을 선택하시면 현재 옵션을 유지한 채 큐브를 다시 돌릴수있습니다." + enter;
        chat += "#r<After>#k 옵션을 선택하시면 선택하신 옵션으로 아이템의 옵션이 변경됩니다.";

        chat += enter;
        chat += "#L" + CubeStatus.RED_CUBE + "# 레드 큐브" + enter;
        chat += "#L" + CubeStatus.BLACK_CUBE + "# 블랙 큐브" + enter;
        chat += "#L" + CubeStatus.ADDITIONAL_CUBE + "# 에디셔널 큐브" + enter;
        chat += "#L" + CubeStatus.WHITE_ADDITIONAL_CUBE + "# 화이트 에디셔널 큐브" + enter;

        cm.sendSimple(chat);
    } else if (status == 1) {
        var composer;
        cubeState = selection;

        switch (selection) {
            case CubeStatus.RED_CUBE:
            case CubeStatus.BLACK_CUBE:
                composer = function (_item) {
                    var requireLevel = ii.getReqLevel(_item.getItemId());
                    // _item.setState(20);
                    if (_item.getState() == 20 && requireLevel >= 100) {
                        return true;
                    } else {
                        return false;
                    }
                };
                break;

            case CubeStatus.WHITE_ADDITIONAL_CUBE:
            case CubeStatus.ADDITIONAL_CUBE:
                composer = function (_item) {
                    var requireLevel = ii.getReqLevel(_item.getItemId());
                    var level = Math.floor(_item.getPotential4() >= 10000 ? _item.getPotential4() / 10000 : _item.getPotential4() / 100);
                    if (_item.getPotential4() > 0 && level == 4 && requireLevel >= 100) {
                        return true;
                    } else {
                        return false;
                    }
                };
                break;

            default:
                cubeState = CubeStatus.UNDIFINED;
                break;
        }
        cubeItemList = getEquipItemList(composer);
        if (cubeItemList.length > 0) {
            chat += "원하시는 아이템을 선택 해주세요." + enter + enter;
            chat += getItemCatalog(cubeItemList);
        } else {
            chat += "해당되는 아이템을 찾지 못했습니다.";
            cm.dispose();
        }

        cm.sendOk(chat);
    } else if (status == 2) {
        if (cubeState == CubeStatus.UNDIFINED) {
            cm.dispose();
            return;
        }

        if (selection == -1) {
            cm.sendSimple("조건에 맞는 아이템이 없습니다");
            cm.dispose();
        }

        if (selectedItem == null) {
            selectedItem = cubeItemList[selection];
            newItem = selectedItem.copy();
        }

        if (selectedItem != null) {
            if (!checkCost(cubeState, selectedItem.getItemId())) {
                chat += "큐브가 없거나 메소가 부족합니다.";
                cm.dispose();
            } else {
                decreaseCost(cubeState, selectedItem.getItemId());
                renewTargerItem = cubeState == CubeStatus.RED_CUBE || cubeState == CubeStatus.ADDITIONAL_CUBE ? selectedItem : newItem;
                renewPotential(renewTargerItem, cubeState == CubeStatus.WHITE_ADDITIONAL_CUBE || cubeState == CubeStatus.ADDITIONAL_CUBE);

                var newPotentialsData = getPotentialsData(
                    renewTargerItem,
                    cubeState == CubeStatus.WHITE_ADDITIONAL_CUBE || cubeState == CubeStatus.ADDITIONAL_CUBE
                );

                chat += "#b#z" + renewTargerItem.getItemId() + "# #k";
                chat += enter + enter;
                switch (cubeState) {
                    case CubeStatus.RED_CUBE:
                    case CubeStatus.ADDITIONAL_CUBE:
                        cm.getPlayer().forceReAddItem(renewTargerItem, MapleInventoryType.EQUIP);
                        chat += "#r#L0#<큐브결과>#k" + enter + enter;

                        chat += getItemPotential(newPotentialsData, cubeState == CubeStatus.WHITE_ADDITIONAL_CUBE || cubeState == CubeStatus.ADDITIONAL_CUBE);

                        chat += "#k";
                        chat += enter;
                        chat += "#L1#그만 돌리기";
                        break;
                    case CubeStatus.BLACK_CUBE:
                    case CubeStatus.WHITE_ADDITIONAL_CUBE:
                        var originalPotentialData = getPotentialsData(
                            selectedItem,
                            cubeState == CubeStatus.WHITE_ADDITIONAL_CUBE || cubeState == CubeStatus.ADDITIONAL_CUBE
                        );

                        chat +=
                            "#rBefore#k 버튼을 클릭할 시 큐브를 한번 더 돌릴수 있습니다.#l\r\n#rBefore#k 옵션 선택을 원할 시 대화 그만하기를 클릭 해주세요.#l" +
                            enter;

                        chat += "#r#L0#<Before>#k" + enter;

                        chat += getItemPotential(
                            originalPotentialData,
                            cubeState == CubeStatus.WHITE_ADDITIONAL_CUBE || cubeState == CubeStatus.ADDITIONAL_CUBE
                        );

                        chat += "#k";
                        chat += enter;

                        chat += "#r#L1#<After>#k" + enter;
                        chat += getItemPotential(newPotentialsData, cubeState == CubeStatus.WHITE_ADDITIONAL_CUBE || cubeState == CubeStatus.ADDITIONAL_CUBE);
                        chat += "#k";
                        break;
                }
                //chat+= '#L0#다시 돌리기' +enter;
                //chat+= '#L1#적용하기'
            }
        } else {
            chat += "ㄴㄴ";
        }

        cm.sendOk(chat);
    } else if (status == 3) {
        if (selection == 0) {
            status = 1;
            //print(Packages.scripting.NPCScriptManager.getInstance().scriptCount());
            action(1, 0, 0);
            return;
        } else if (selection == 1) {
            switch (cubeState) {
                case CubeStatus.BLACK_CUBE:
                case CubeStatus.WHITE_ADDITIONAL_CUBE:
                    cm.getPlayer().forceReAddItem(renewTargerItem, MapleInventoryType.EQUIP);
                    selectedItem = null;
                    break;
            }
            cm.dispose();
        }
    }
}

function getItemPotential(potentialsData, isAditional) {
    var chat = "";
    for (var i = 0; i < potentialsData.length; i++) {
        var potentialIndex = isAditional ? i + 3 : i;
        chat += potentialIndex + 1 + "번 잠재능력: ";
        chat += getPotentialDescription(potentialsData[i]);

        chat += enter;
    }
    return chat;
}

function getPotentialString(potentialType) {
    var _property = "잡옵션";
    switch (potentialType) {
        case Potential.STR_P:
        case Potential.STR:
            _property = "STR";
            break;
        case Potential.DEX_P:
        case Potential.DEX:
            _property = "DEX";
            break;
        case Potential.INT_P:
        case Potential.INT:
            _property = "INT";
            break;
        case Potential.LUK_P:
        case Potential.LUK:
            _property = "LUK";
            break;
        case Potential.ALL_P:
        case Potential.ALL:
            _property = "올스탯";
            break;
        case Potential.IGNORE_DEF:
            _property = "몬스터 방어율 무시";
            break;
        case Potential.WATTACK_P:
            _property = "공격력";
            break;
        case Potential.MATTACK_P:
            _property = "마력";
            break;
        case Potential.DAM_P:
            _property = "데미지";
            break;
        case Potential.INC_BOSSDAM_P:
            _property = "보스 몬스터 공격 시 데미지";
            break;
        case Potential.INC_CRIT:
            _property = "크리티컬 확률";
            break;
        case Potential.INC_CRIT_DAM:
            _property = "크리티컬 데미지";
            break;
        case Potential.REDUCE_COOLTIME:
            _property = "쿨타임 감소";
            break;
        case Potential.INC_REWARD_PROP:
            _property = "아이템 획득";
            break;
        case Potential.INC_MESO_PROP:
            _property = "메소 획득";
            break;
        case Potential.HP_P:
            _property = "최대 HP";
            break;
        case Potential.MP_P:
            _property = "최대 MP";
            break;
        case Potential.INC_SKILLLV:
            _property = "모든 스킬레벨";
            break;
        case Potential.LV_WATK:
            _property = "10레벨당 공격력";
            break;
        case Potential.LV_MATK:
            _property = "10레벨당 마력";
            break;
        case Potential.LV_STR:
            _property = "10레벨당 STR";
            break;
        case Potential.LV_DEX:
            _property = "10레벨당 DEX";
            break;
        case Potential.LV_INT:
            _property = "10레벨당 INT";
            break;
        case Potential.LV_LUK:
            _property = "10레벨당 LUK";
            break;
    }
    return _property;
}

function getPotentialsData(item, isAditional) {
    var potentialArray = [];
    for (var i = !isAditional ? 1 : 4; i <= (!isAditional ? 3 : 6); i++) {
        var currentPotential = getPotentialData(getPotential(item, i), item.getItemId());
        potentialArray.push(currentPotential);
    }
    return potentialArray;
}

function getPotential(item, index) {
    // var _type = "get";
    // return item[_type + "Potential" + index]();
    switch (index) {
        case 1:
            return item.getPotential1();
        case 2:
            return item.getPotential2();
        case 3:
            return item.getPotential3();
        case 4:
            return item.getPotential4();
        case 5:
            return item.getPotential5();
        case 6:
            return item.getPotential6();
    }
}

function renewPotential(item, isAditional) {
    var level;
    if (!isAditional) {
        level = item.getState() - 16;

        item.setPotential1(InventoryHandler.potential(item.getItemId(), level));
        item.setPotential2(InventoryHandler.potential(item.getItemId(), level == 1 || Randomizer.nextInt(100) < KEEP_LEVEL_PERCENT ? level : level - 1));

        if (item.getPotential3() > 0) {
            item.setPotential3(InventoryHandler.potential(item.getItemId(), level == 1 || Randomizer.nextInt(100) < KEEP_LEVEL_PERCENT ? level : level - 1));
        } else {
            item.setPotential3(0);
        }
    } else {
        level = item.getPotential4() >= 10000 ? item.getPotential4() / 10000 : item.getPotential4() / 100;
        item.setPotential4(InventoryHandler.potential(item.getItemId(), level, true));
        item.setPotential5(InventoryHandler.potential(item.getItemId(), level == 1 || Randomizer.nextInt(100) < KEEP_LEVEL_PERCENT ? level : level - 1, true));

        if (item.getPotential6() > 0) {
            item.setPotential6(
                InventoryHandler.potential(item.getItemId(), level == 1 || Randomizer.nextInt(100) < KEEP_LEVEL_PERCENT ? level : level - 1, true)
            );
        } else {
            item.setPotential6(0);
        }
    }
}

function getItemCatalog(itemList) {
    var _chat = "";
    for (var i = 0; i < itemList.length; i++) {
        var currentItem = itemList[i];
        _chat += "#L" + i + "#";
        _chat += "#v" + currentItem.getItemId() + "#";
        _chat += "#z" + currentItem.getItemId() + "#";
        _chat += enter;
    }
    return _chat;
}

function getEquipItemList(composer) {
    var equipInventory = cm.getPlayer().getInventory(MapleInventoryType.EQUIP);
    var itemList = new Array();
    for (var i = 0; i < equipInventory.getSlotLimit(); i++) {
        var currentItem = equipInventory.getItem(i);
        if (currentItem != null) {
            if (composer(currentItem)) {
                itemList.push(currentItem);
            }
        }
    }
    return itemList;
}

function getPotentialItemId(_cubestate) {
    switch (_cubestate) {
        case CubeStatus.RED_CUBE:
            return 5062009;
        case CubeStatus.BLACK_CUBE:
            return 5062010;
        case CubeStatus.ADDITIONAL_CUBE:
            return 5062500;
        case CubeStatus.WHITE_ADDITIONAL_CUBE:
            return 5062503;
    }
}

function getPotentialItemQuantity(_cubestate) {
    switch (_cubestate) {
        case CubeStatus.RED_CUBE:
            return 1;
        case CubeStatus.BLACK_CUBE:
            return 1;
        case CubeStatus.ADDITIONAL_CUBE:
            return 1;
        case CubeStatus.WHITE_ADDITIONAL_CUBE:
            return 1;
    }
}

function checkCost(_cubestate, itemId) {
    var isQuantityComplete = false;
    var isMesoComplete = false;

    if (cm.haveItem(getPotentialItemId(_cubestate), getPotentialItemQuantity(_cubestate))) {
        isQuantityComplete = true;
    }

    if (cm.getMeso() >= GameConstants.getCubeMeso(itemId)) {
        isMesoComplete = true;
    }

    return isQuantityComplete && isMesoComplete;
}

function decreaseCost(_cubestate, itemId) {
    if (checkCost(_cubestate, itemId)) {
        cm.gainItem(getPotentialItemId(_cubestate), getPotentialItemQuantity(_cubestate) * -1);
        cm.gainMeso(GameConstants.getCubeMeso(itemId) * -1);
        return true;
    }

    return false;
}

function createPotentialMap() {
    var _potentialMap = {};

    _potentialMap[42292] = new PotentialStruct(Potential.ALL_P, 6);

    _potentialMap[20070] = new PotentialStruct(Potential.DAM_P, 6);
    _potentialMap[30070] = new PotentialStruct(Potential.DAM_P, 9);
    _potentialMap[32070] = new PotentialStruct(Potential.DAM_P, 9);
    _potentialMap[40070] = new PotentialStruct(Potential.DAM_P, 12);
    _potentialMap[42070] = new PotentialStruct(Potential.DAM_P, 12);

    _potentialMap[20051] = new PotentialStruct(Potential.WATTACK_P, 6);
    _potentialMap[30051] = new PotentialStruct(Potential.WATTACK_P, 9);
    _potentialMap[40051] = new PotentialStruct(Potential.WATTACK_P, 12);

    _potentialMap[32051] = new PotentialStruct(Potential.WATTACK_P, 9);
    _potentialMap[42051] = new PotentialStruct(Potential.WATTACK_P, 12);

    _potentialMap[20052] = new PotentialStruct(Potential.MATTACK_P, 6);
    _potentialMap[30052] = new PotentialStruct(Potential.MATTACK_P, 9);
    _potentialMap[40052] = new PotentialStruct(Potential.MATTACK_P, 12);

    _potentialMap[32053] = new PotentialStruct(Potential.MATTACK_P, 9);
    _potentialMap[42053] = new PotentialStruct(Potential.MATTACK_P, 12);

    _potentialMap[30601] = new PotentialStruct(Potential.INC_BOSSDAM_P, 20);
    _potentialMap[40601] = new PotentialStruct(Potential.INC_BOSSDAM_P, 30);
    _potentialMap[30602] = new PotentialStruct(Potential.INC_BOSSDAM_P, 30);
    _potentialMap[40602] = new PotentialStruct(Potential.INC_BOSSDAM_P, 35);
    _potentialMap[40603] = new PotentialStruct(Potential.INC_BOSSDAM_P, 40);

    _potentialMap[32601] = new PotentialStruct(Potential.INC_BOSSDAM_P, 12);
    _potentialMap[42602] = new PotentialStruct(Potential.INC_BOSSDAM_P, 18);
    _potentialMap[40086] = new PotentialStruct(Potential.INC_BOSSDAM_P, 5);

    _potentialMap[20055] = new PotentialStruct(Potential.INC_CRIT, 8);
    _potentialMap[30055] = new PotentialStruct(Potential.INC_CRIT, 10);
    _potentialMap[40055] = new PotentialStruct(Potential.INC_CRIT, 12);

    _potentialMap[42058] = new PotentialStruct(Potential.INC_CRIT, 2);
    _potentialMap[32057] = new PotentialStruct(Potential.INC_CRIT, 9);
    _potentialMap[42057] = new PotentialStruct(Potential.INC_CRIT, 12);

    _potentialMap[40056] = new PotentialStruct(Potential.INC_CRIT_DAM, 8);
    _potentialMap[40057] = new PotentialStruct(Potential.INC_CRIT_DAM, 8);

    _potentialMap[40556] = new PotentialStruct(Potential.REDUCE_COOLTIME, 1);
    _potentialMap[42556] = new PotentialStruct(Potential.REDUCE_COOLTIME, 1);
    _potentialMap[40557] = new PotentialStruct(Potential.REDUCE_COOLTIME, 2);

    _potentialMap[40650] = new PotentialStruct(Potential.INC_MESO_PROP, 20);

    _potentialMap[40656] = new PotentialStruct(Potential.INC_REWARD_PROP, 20);

    _potentialMap[40045] = new PotentialStruct(Potential.HP_P, 12);
    _potentialMap[30035] = new PotentialStruct(Potential.HP_P, 9);
    _potentialMap[30045] = new PotentialStruct(Potential.HP_P, 9);

    _potentialMap[40046] = new PotentialStruct(Potential.MP_P, 12);
    _potentialMap[30046] = new PotentialStruct(Potential.MP_P, 9);

    _potentialMap[30106] = new PotentialStruct(Potential.INC_SKILLLV, 1);
    _potentialMap[30107] = new PotentialStruct(Potential.INC_SKILLLV, 3);

    _potentialMap[42095] = new PotentialStruct(Potential.LV_WATK, 1);
    _potentialMap[40091] = new PotentialStruct(Potential.LV_WATK, 1);

    _potentialMap[42096] = new PotentialStruct(Potential.LV_MATK, 1);
    _potentialMap[40092] = new PotentialStruct(Potential.LV_MATK, 1);

    _potentialMap[32091] = new PotentialStruct(Potential.LV_STR, 1);
    _potentialMap[42091] = new PotentialStruct(Potential.LV_STR, 2);

    _potentialMap[32092] = new PotentialStruct(Potential.LV_DEX, 1);
    _potentialMap[42092] = new PotentialStruct(Potential.LV_DEX, 2);

    _potentialMap[32093] = new PotentialStruct(Potential.LV_INT, 1);
    _potentialMap[42093] = new PotentialStruct(Potential.LV_INT, 2);

    _potentialMap[32094] = new PotentialStruct(Potential.LV_LUK, 1);
    _potentialMap[42094] = new PotentialStruct(Potential.LV_LUK, 2);

    //_potentialMap[0] = new PotentialStruct(Potential.NONE, 0);

    return _potentialMap;
}

function getPotentialDescription(potentialData) {
    var chat = "";
    var type = potentialData.type;
    var value = potentialData.value;

    switch (type) {
        case Potential.INC_MESO_PROP:
        case Potential.INC_REWARD_PROP:
        case Potential.REDUCE_COOLTIME:
            chat += "#b";
            break;
    }

    chat += getPotentialString(type);
    chat += "#k";

    //prefix
    switch (type) {
        case Potential.INC_SKILLLV:
        case Potential.LV_WATK:
        case Potential.LV_MATK:
        case Potential.LV_STR:
        case Potential.LV_DEX:
        case Potential.LV_INT:
        case Potential.LV_LUK:
        case Potential.ALL:
        case Potential.STR:
        case Potential.DEX:
        case Potential.INT:
        case Potential.LUK:
        case Potential.WATTACK:
        case Potential.MATTACK:
            chat += " +";
            break;
        default:
    }
    //prefix color
    if (type != Potential.NONE) {
        switch (type) {
            case Potential.STR_P:
            case Potential.DEX_P:
            case Potential.INT_P:
            case Potential.LUK_P:
                if (value >= 12) {
                    chat += "#r";
                }
                break;

            case Potential.ALL_P:
                if (value >= 9) {
                    chat += "#r";
                }
                break;

            case Potential.INC_BOSSDAM_P:
                if (value >= 40) {
                    chat += "#r";
                }
                break;

            case Potential.INC_CRIT_DAM:
                chat += "#r";
                break;

            case Potential.IGNORE_DEF:
                if (value >= 40) {
                    chat += "#r";
                }
        }
        chat += " " + value + "#k";

        //subfix
        switch (type) {
            case Potential.REDUCE_COOLTIME:
                chat += "초";
                break;
            case Potential.INC_SKILLLV:
                break;
            case Potential.LV_WATK:
            case Potential.LV_MATK:
            case Potential.LV_STR:
            case Potential.LV_DEX:
            case Potential.LV_INT:
            case Potential.LV_LUK:
            case Potential.ALL:
            case Potential.STR:
            case Potential.DEX:
            case Potential.INT:
            case Potential.LUK:
            case Potential.WATTACK:
            case Potential.MATTACK:
                break;

            default:
                chat += "%";
        }
    } else {
        if (cm.getPlayer().getGMLevel() >= 10) {
            if (potentialData.id != null) {
                chat += " (" + potentialData.id + ")";
            }
        }
    }
    return chat;
}

function getPotentialData(potential, itemId) {
    var lv = ii.getReqLevel(itemId) / 10 - 1;
    var pi = ii.getPotentialInfo(potential).get(lv);
    if (pi.incSTRr > 0 && pi.incDEXr > 0) {
        return new PotentialStruct(Potential.ALL_P, pi.incSTRr);
    } else if (pi.incSTRr > 0) {
        return new PotentialStruct(Potential.STR_P, pi.incSTRr);
    } else if (pi.incDEXr > 0) {
        return new PotentialStruct(Potential.DEX_P, pi.incDEXr);
    } else if (pi.incINTr > 0) {
        return new PotentialStruct(Potential.INT_P, pi.incINTr);
    } else if (pi.incLUKr > 0) {
        return new PotentialStruct(Potential.LUK_P, pi.incLUKr);
    } else if (pi.ignoreTargetDEF > 0) {
        return new PotentialStruct(Potential.IGNORE_DEF, pi.ignoreTargetDEF);
    } else if (pi.incMHPr > 0) {
        return new PotentialStruct(Potential.HP_P, pi.incMHPr);
    } else if (pi.incMMPr > 0) {
        return new PotentialStruct(Potential.MP_P, pi.incMMPr);
    } else if (pi.incSTR > 0 && pi.incDEX > 0) {
        return new PotentialStruct(Potential.ALL, pi.incSTR);
    } else if (pi.incSTR > 0) {
        return new PotentialStruct(Potential.STR, pi.incSTR);
    } else if (pi.incDEX > 0) {
        return new PotentialStruct(Potential.DEX, pi.incDEX);
    } else if (pi.incINT > 0) {
        return new PotentialStruct(Potential.INT, pi.incINT);
    } else if (pi.incLUK > 0) {
        return new PotentialStruct(Potential.LUK, pi.incLUK);
    } else if (pi.WATTACK > 0) {
        return new PotentialStruct(Potential.WATTACK, pi.incPAD);
    } else if (pi.MATTACK > 0) {
        return new PotentialStruct(Potential.MATTACK, pi.incMAD);
    }

    return getMappedPotentialData(potential);
}

function getMappedPotentialData(potential) {
    var potentialData = potentialMap[potential];
    if (potentialData == null) {
        potentialData = new PotentialStruct(Potential.NONE, 0, potential);
    }
    return potentialData;
}

///////////////////////////////////////////

function print(text) {
    java.lang.System.out.println(text);
}
