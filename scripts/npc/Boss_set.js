var enter = "\r\n";
var seld = -1;

var items = [
   {'itemid' : 1113282, 'qty' : 1, 'allstat' : 0, 'atk' : 0,
   'recipes' : [[1032227, 1], [1122274, 1], [1113089, 1]], 'price' : 20, 'chance' : 50,
   'fail' : [[4001715, 1]]
   },


   {'itemid' : 1012800, 'qty' : 1, 'allstat' : 0, 'atk' : 0,
   'recipes' : [[1012478, 1], [4310237, 300]], 'price' : 20, 'chance' : 40,
   'fail' : [[4001715, 1]]
   },

   {'itemid' : 1022800, 'qty' : 1, 'allstat' : 0, 'atk' : 0,
   'recipes' : [[1022231, 1], [4310237, 300]], 'price' : 20, 'chance' : 40,
   'fail' : [[4001715, 1]]
   },

   {'itemid' : 1113800, 'qty' : 1, 'allstat' : 0, 'atk' : 0,
   'recipes' : [[1113149, 1], [4310237, 300]], 'price' : 20, 'chance' : 40,
   'fail' : [[4001715, 1]]
   },

   {'itemid' : 1032801, 'qty' : 1, 'allstat' : 0, 'atk' : 0,
   'recipes' : [[1032241, 1], [4310237, 300]], 'price' : 20, 'chance' : 40,
   'fail' : [[4001715, 1]]
   },

   {'itemid' : 1032800, 'qty' : 1, 'allstat' : 0, 'atk' : 0,
   'recipes' : [[1032136, 1], [4310237, 300]], 'price' : 20, 'chance' : 40,
   'fail' : [[4001715, 1]]
   },

   {'itemid' : 1122800, 'qty' : 1, 'allstat' : 0, 'atk' : 0,
   'recipes' : [[1122000, 1], [4310237, 300]], 'price' : 20, 'chance' : 40,
   'fail' : [[4001715, 1]]
   },

   {'itemid' : 1122801, 'qty' : 1, 'allstat' : 0, 'atk' : 0,
   'recipes' : [[1122076, 1], [4310237, 300]], 'price' : 20, 'chance' : 40,
   'fail' : [[4001715, 1]]
   },

   {'itemid' : 1132800, 'qty' : 1, 'allstat' : 0, 'atk' : 0,
   'recipes' : [[1132296, 1], [4310237, 300]], 'price' : 20, 'chance' : 40,
   'fail' : [[4001715, 1]]
   },

   {'itemid' : 1122802, 'qty' : 1, 'allstat' : 0, 'atk' : 0,
   'recipes' : [[1122254, 1], [4310237, 300]], 'price' : 20, 'chance' : 40,
   'fail' : [[4001715, 1]]
   },

   {'itemid' : 1122803, 'qty' : 1, 'allstat' : 0, 'atk' : 0,
   'recipes' : [[1122150, 1], [4310237, 300]], 'price' : 20, 'chance' : 40,
   'fail' : [[4001715, 1]]
   },

   {'itemid' : 1132801, 'qty' : 1, 'allstat' : 0, 'atk' : 0,
   'recipes' : [[1132272, 1], [4310237, 300]], 'price' : 20, 'chance' : 40,
   'fail' : [[4001715, 1]]
   },

   {'itemid' : 1152800, 'qty' : 1, 'allstat' : 0, 'atk' : 0,
   'recipes' : [[1152170, 1], [4310237, 300]], 'price' : 20, 'chance' : 40,
   'fail' : [[4001715, 1]]
   },

   {'itemid' : 1162800, 'qty' : 1, 'allstat' : 0, 'atk' : 0,
   'recipes' : [[1162025, 1], [4310237, 300]], 'price' : 20, 'chance' : 40,
   'fail' : [[4001715, 1]]
   },

   {'itemid' : 1162801, 'qty' : 1, 'allstat' : 0, 'atk' : 0,
   'recipes' : [[1162009, 1], [4310237, 300]], 'price' : 20, 'chance' : 40,
   'fail' : [[4001715, 1]]
   },

   {'itemid' : 1182800, 'qty' : 1, 'allstat' : 0, 'atk' : 0,
   'recipes' : [[1182087, 1], [4310237, 300]], 'price' : 20, 'chance' : 40,
   'fail' : [[4001715, 1]]
   },

   {'itemid' : 1113801, 'qty' : 1, 'allstat' : 0, 'atk' : 0,
   'recipes' : [[1113282, 1], [4310237, 300]], 'price' : 20, 'chance' : 40,
   'fail' : [[4001715, 1]]
   },

   {'itemid' : 1022801, 'qty' : 1, 'allstat' : 0, 'atk' : 0,
   'recipes' : [[1022232, 1], [4310237, 300]], 'price' : 20, 'chance' : 40,
   'fail' : [[4001715, 1]]
   },

   {'itemid' : 1022802, 'qty' : 1, 'allstat' : 0, 'atk' : 0,
   'recipes' : [[1022277, 1], [4310237, 300]], 'price' : 20, 'chance' : 40,
   'fail' : [[4001715, 1]]
   },
] // ???????????? ????????? ????????????????????? ???????????????

var item;
var isEquip = false;
var canMake = false;

function start() {
   status = -1;
   action(1, 0, 0);
}
function action(mode, type, sel) {
   if (mode == 1) {
      status++;
   } else {
      cm.dispose();
      return;
       }
   if (status == 0) {
      var msg = "#fs11#???????????? ???????????? ??????????????????."+enter;
      msg += "#fs11#???????????? ???????????? ????????? ???????????? ????????????.#fs12##b"+enter;
      for (i = 0; i < items.length; i++)
         msg += "#fs11##L"+i+"##i"+items[i]['itemid']+"##z"+items[i]['itemid']+"# "+items[i]['qty']+"???"+enter;

      cm.sendSimple(msg);
   } else if (status == 1) {
      seld = sel;
      item = items[sel];
      isEquip = Math.floor(item['itemid'] / 1000000) == 1;

      canMake = checkItems(item);

      var msg = "#fs11#???????????? ???????????? ????????? ????????????.#fs11##b"+enter;
      msg += "#fs11#????????? : #i"+item['itemid']+"##z"+item['itemid']+"# "+item['qty']+"???"+enter;

      if (isEquip) {
         if (item['allstat'] > 0)
            msg += "????????? : +"+item['allstat']+enter;
         if (item['atk'] > 0)
            msg += "?????????, ?????? : +"+item['atk']+enter;
      }

      msg += enter;
      msg += "#fs11##k???????????? ???????????? ???????????? ?????? ??????????????????.#fs11##d"+enter+enter;

      if (item['recipes'].length > 0) {
         for (i = 0; i < item['recipes'].length; i++)
            msg += "#b#i"+item['recipes'][i][0]+"##z"+item['recipes'][i][0]+"# "+item['recipes'][i][1]+"??? #r/ #c"+item['recipes'][i][0]+"#??? ?????? ???#k"+enter;
      }

      if (item['price'] > 0)


      msg += enter+"#fs11##e?????? ?????? ?????? : "+item['chance']+"%#n"+enter+enter;
      msg += "#k?????? ????????? ????????? ?????? ???????????? ???????????????.#fs11##d"+enter+enter;
      if (item['fail'].length > 0) {
         for (i = 0; i < item['fail'].length; i++)
            msg += "#i"+item['fail'][i][0]+"##z"+item['fail'][i][0]+"# "+item['fail'][i][1]+"???"+enter;
      }
      msg +="#fs11#"+enter;
      msg += canMake ? "#b???????????? ???????????? ????????? ?????? ???????????? ?????? ???????????????."+enter+"?????? ?????????????????? '???'??? ???????????????." : "#r???????????? ???????????? ????????? ?????? ???????????? ???????????? ????????????.";

      if (canMake) cm.sendYesNo(msg);
      else {
         cm.sendOk(msg);
         cm.dispose();
      }
      
   } else if (status == 2) {
      canMake = checkItems(item);

      if (!canMake) {
         cm.sendOk("????????? ???????????? ?????? ??? ??? ??????????????????.");
         cm.dispose();
         return;
      }
      payItems(item);
      if (Packages.server.Randomizer.rand(1, 100) <= item['chance']) {
         gainItem(item);
         cm.sendOk("#fs11#??????????????????. ????????? ?????????????????????");
      } else {
         cm.sendOk("#fs11#????????? ??????????????????.");
         gainFail(item);
      }
      cm.dispose();
   }
}

function checkItems(i) {
   recipe = i['recipes'];
   ret = true;

   for (j = 0; j < recipe.length; j++) {
      if (!cm.haveItem(recipe[j][0], recipe[j][1])) {
         //cm.getPlayer().dropMessage(6, "fas");
         ret = false;
         break;
      }
   }
   if (ret) ret = cm.getPlayer().getMeso() >= i['price'];

   return ret;
}

function payItems(i) {
   recipe = i['recipes'];
   for (j = 0; j < recipe.length; j++) {
      if (Math.floor(recipe[j][0] / 1000000) == 1)
         Packages.server.MapleInventoryManipulator.removeById(cm.getClient(), Packages.client.inventory.MapleInventoryType.EQUIP, recipe[j][0], 1, false, false);
      else cm.gainItem(recipe[j][0], -recipe[j][1]);
   }
}

function gainItem(i) {
   ise = Math.floor(i['itemid'] / 1000000) == 1;
   if (ise) {
      vitem = Packages.server.MapleItemInformationProvider.getInstance().getEquipById(i['itemid']);
      if (i['allstat'] > 0) {
         vitem.setStr(i['allstat']);
         vitem.setDex(i['allstat']);
         vitem.setInt(i['allstat']);
         vitem.setLuk(i['allstat']);
      }
      if (i['atk'] > 0) {
         vitem.setWatk(i['atk']);
         vitem.setMatk(i['atk']);
      }
      Packages.server.MapleInventoryManipulator.addFromDrop(cm.getClient(), vitem, false);
   } else {
      cm.gainItem(i['itemid'], i['qty']);
   }
}
function gainFail(i) {
   fail = i['fail'];

   for (j = 0; j < fail.length; j++) {
      cm.gainItem(fail[j][0], fail[j][1]);
   }
}