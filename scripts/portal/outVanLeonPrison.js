function enter(pi) {
   
   if (!pi.haveItem(4032860, 1)) {
      pi.warp(211070100);
   } else {
      pi.gainItem(4032860, -1);
      pi.warp(211070104);
   }

   return true;
}
