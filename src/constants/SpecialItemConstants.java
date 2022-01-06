/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package constants;

import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import server.MapleItemInformationProvider;
import tools.Pair;
import tools.Triple;

public class SpecialItemConstants {

    public static List<Pair<Integer, Short>> GoldAppleNormalList = new ArrayList<>();
    public static List<Pair<Integer, Short>> GoldAppleSpecialList = new ArrayList<>();

    protected static String toUni(String kor) throws UnsupportedEncodingException {
        return new String(kor.getBytes("KSC5601"), "8859_1");
    }

    public static void LoadGoldAppleItems() {
        try {
            System.out.println("[알림] 골드애플 아이템 목록을 불러옵니다.");
            FileInputStream setting = new FileInputStream("setting/GoldAppleItems.properties");
            Properties setting_ = new Properties();
            setting_.load(setting);
            setting.close();
            final String[] Gsplit = setting_.getProperty(toUni("일반아이템")).split(",");
            final String[] Gsplit2 = setting_.getProperty(toUni("일반아이템개수")).split(",");
            final String[] Gsplit3 = setting_.getProperty(toUni("스페셜아이템")).split(",");
            final String[] Gsplit4 = setting_.getProperty(toUni("스페셜아이템개수")).split(",");
            if (Gsplit != null && Gsplit[0].length() > 0) {
                for (int i = 0; i < Gsplit.length; ++i) {
                    GoldAppleNormalList.add(new Pair(Integer.parseInt(Gsplit[i].replaceAll(" ", "")), Short.parseShort(Gsplit2[i].replaceAll(" ", ""))));
                }
            }
            if (Gsplit != null && Gsplit[0].length() > 0) {
                for (int i = 0; i < Gsplit.length; ++i) {
                    GoldAppleSpecialList.add(new Pair(Integer.parseInt(Gsplit3[i].replaceAll(" ", "")), Short.parseShort(Gsplit4[i].replaceAll(" ", ""))));
                }
            }
            UpdateGoldApple();
        } catch (Exception e) {
            System.err.println("[오류] GoldAppleList를 불러오는데 실패하였습니다.");
            e.printStackTrace();
        }
    }

    public static List<Triple<Integer, Integer, Byte>> GoldAppleList2 = new ArrayList<>();

    public static String getItemName(final int id) {
        return MapleItemInformationProvider.getInstance().getName(id);
    }

    public static final void println(final String text, final int color) {
        System.out.println(text);
    }

    public static final void UpdateGoldApple() {
        for (int i = 1; i < GoldAppleNormalList.size(); i++) {
            String itemcode = getItemName(GoldAppleNormalList.get(i).getLeft());
            int count = GoldAppleNormalList.get(i).getRight();
            println("* 골드애플 일반아이템 : [" + itemcode + "] 개수 : [" + count + "]", 31);
        }
        for (int i = 1; i < GoldAppleSpecialList.size(); i++) {
            String itemcode = getItemName(GoldAppleSpecialList.get(i).getLeft());
            int count = GoldAppleSpecialList.get(i).getRight();
            println("* 골드애플 스페셜아이템 : [" + itemcode + "] 개수 : [" + count + "]", 31);
        }
    }

}
