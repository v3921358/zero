/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package constants;

import server.MapleItemInformationProvider;
import tools.Pair;
import tools.Triple;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author user
 */
public class CashCalcInsertData {

    public static String parse2 = "오가닉 원더 쿠키(10개)|15.60\n"
            + "고농축 프리미엄 생명의물|14.44\n"
            + "멈뭄미|10.80\n"
            + "먀몸미|10.80\n"
            + "햄미|10.80\n"
            + "식빵이|13.80\n"
            + "마롱이|13.80\n"
            + "펭펭이|3.32\n"
            + "핑핑이|3.32\n"
            + "퐁퐁이|3.32";
    public static String parse = "위대한 매그너스의 소울|0.04503\n"
            + "위대한 핑크빈의 소울|0.18012\n"
            + "위대한 아카이럼의 소울|0.54036\n"
            + "위대한 플레드의 소울|0.36024\n"
            + "카오스 핑크빈 마크|0.00720\n"
            + "도미네이터 펜던트|0.00901\n"
            + "코어 젬스톤 10개 교환권|0.19813\n"
            + "인형의 집 의자|0.45030\n"
            + "들꽃 축제 의자|0.45030\n"
            + "이클립스 버드 라이딩 (영구) 교환권|0.27018\n"
            + "이클립스 버드 라이딩 (90일) 교환권|0.90059\n"
            + "케이카 라이딩 (영구) 교환권|0.27018\n"
            + "케이카 라이딩 (90일) 교환권|0.90059\n"
            + "매지컬 한손무기 공격력 주문서|0.01801\n"
            + "매지컬 한손무기 마력 주문서|0.01801\n"
            + "매지컬 두손무기 공격력 주문서|0.01801\n"
            + "영원한 환생의 불꽃|0.07205\n"
            + "티타늄 하트|0.00901\n"
            + "리튬 하트|0.09006\n"
            + "크리스탈 하트|0.27018\n"
            + "골드 하트|0.27018\n"
            + "기운찬 매그너스의 소울|0.04053\n"
            + "날렵한 매그너스의 소울|0.04053\n"
            + "총명한 매그너스의 소울|0.04053\n"
            + "놀라운 매그너스의 소울|0.04053\n"
            + "화려한 매그너스의 소울|0.04053\n"
            + "강력한 매그너스의 소울|0.04053\n"
            + "빛나는 매그너스의 소울|0.04053\n"
            + "강인한 매그너스의 소울|0.04053\n"
            + "영원히 꺼지지 않는 불꽃 조각|0.90059\n"
            + "스페셜 소울 인챈터|0.45030\n"
            + "마력의 하운드 이어링|0.25217\n"
            + "샤이니 레드 매지션 마이스터 심볼|0.25217\n"
            + "샤이니 레드 시프 마이스터 심볼|0.25217\n"
            + "샤이니 레드 워리어 마이스터 심볼|0.25217\n"
            + "베어스 퍼플 펜던트|0.25217\n"
            + "레드 워리어 마이스터 심볼|0.25217\n"
            + "레드 파이렛 마이스터 심볼|0.25217\n"
            + "하프 이어링|0.25217\n"
            + "레드 시프 마이스터 심볼|0.25217\n"
            + "레드 매지션 마이스터 심볼|0.25217\n"
            + "레드 아처 마이스터 심볼|0.25217\n"
            + "피콕스 퍼플 펜던트|0.25217\n"
            + "아울스 퍼플 펜던트|0.25217\n"
            + "울프스 퍼플 펜던트|0.25217\n"
            + "샤이니 레드 아처 마이스터 심볼|0.25217\n"
            + "샤이니 레드 파이렛 마이스터 심볼|0.25217\n"
            + "강인함의 익스트림 벨트|0.25217\n"
            + "지혜의 익스트림 벨트|0.25217\n"
            + "행운의 익스트림 벨트|0.25217\n"
            + "님블 하운드 이어링|0.25217\n"
            + "날카로운 익스트림 벨트|0.25217\n"
            + "체력의 하운드 이어링|0.25217\n"
            + "하이퍼 하운드 이어링|0.25217\n"
            + "펫장비 공격력 스크롤 100%|0.36024\n"
            + "펫장비 마력 스크롤 100%|0.36024\n"
            + "악세서리 공격력 스크롤 100%|0.36024\n"
            + "악세서리 마력 스크롤 100%|0.36024\n"
            + "에픽 잠재능력 주문서 50%|0.36024\n"
            + "에디셔널 잠재능력 부여 주문서 70%|0.27018\n"
            + "황금 망치 100%|0.54036\n"
            + "기운찬 핑크빈의 소울|0.54036\n"
            + "날렵한 핑크빈의 소울|0.54036\n"
            + "총명한 핑크빈의 소울|0.54036\n"
            + "놀라운 핑크빈의 소울|0.54036\n"
            + "화려한 핑크빈의 소울|0.54036\n"
            + "강력한 핑크빈의 소울|0.54036\n"
            + "빛나는 핑크빈의 소울|0.54036\n"
            + "강인한 핑크빈의 소울|0.54036\n"
            + "기운찬 아카이럼의 소울|0.54036\n"
            + "날렵한 아카이럼의 소울|0.54036\n"
            + "총명한 아카이럼의 소울|0.54036\n"
            + "놀라운 아카이럼의 소울|0.54036\n"
            + "화려한 아카이럼의 소울|0.54036\n"
            + "강력한 아카이럼의 소울|0.54036\n"
            + "빛나는 아카이럼의 소울|0.54036\n"
            + "강인한 아카이럼의 소울|0.54036\n"
            + "기운찬 플레드의 소울|0.54036\n"
            + "날렵한 플레드의 소울|0.54036\n"
            + "총명한 플레드의 소울|0.54036\n"
            + "놀라운 플레드의 소울|0.54036\n"
            + "화려한 플레드의 소울|0.54036\n"
            + "강력한 플레드의 소울|0.54036\n"
            + "빛나는 플레드의 소울|0.54036\n"
            + "강인한 플레드의 소울|0.54036\n"
            + "소울 분해기|3.60237\n"
            + "소울 20칸 가방|1.35089\n"
            + "식물용 20칸 가방|1.35089\n"
            + "광물용 20칸 가방|1.35089\n"
            + "제작물품 20칸 가방|1.35089\n"
            + "레시피 20칸 가방|1.35089\n"
            + "의자 20칸 가방|1.35089\n"
            + "칭호 20칸 명함 지갑|1.35089\n"
            + "주문서 20칸 가방|1.35089\n"
            + "레전드 메이플 리프|1.35089\n"
            + "현명한 피노키오 코|1.35089\n"
            + "얼음결정 페이스페인팅|1.35089\n"
            + "행운의 피노키오 코|1.35089\n"
            + "민첩한 피노키오 코|1.35089\n"
            + "블러드 마스크|1.35089\n"
            + "힘센 피노키오 코|1.35089\n"
            + "금빛 각인의 인장|1.80119\n"
            + "긍정의 혼돈 주문서 30%|1.80119\n"
            + "긍정의 혼돈 주문서 50%|1.35089\n"
            + "놀라운 혼돈의 주문서 30%|1.80119\n"
            + "놀라운 혼돈의 주문서 40%|1.80119\n"
            + "놀라운 혼돈의 주문서 50%|1.80119\n"
            + "놀라운 혼돈의 주문서 60%|1.80119\n"
            + "놀라운 혼돈의 주문서 70%|1.80119\n"
            + "순백의 주문서 10%|1.35089\n"
            + "순백의 주문서 5%|1.80119\n"
            + "은빛 각인의 인장|1.80119\n"
            + "은빛 에디셔널 각인의 인장|1.80119\n"
            + "혼돈의 주문서 70%|1.80119\n"
            + "황금 망치 50%|1.80119\n"
            + "미라클 장갑 공격력 주문서 50%|1.80119\n"
            + "미라클 펫장비 공격력 주문서 50%|1.80119\n"
            + "미라클 악세서리 공격력 주문서 50%|1.80119\n"
            + "미라클 악세서리 마력 주문서 50%|1.80119\n"
            + "미라클 방어구 공격력 주문서 50%|1.80119\n"
            + "미라클 펫장비 마력 주문서 50%|1.80119\n"
            + "미라클 방어구 마력 주문서 50%|1.80119\n"
            + "악세서리 공격력 주문서 70%|1.80119\n"
            + "방어구 공격력 주문서 70%|1.80119\n"
            + "펫장비 공격력 주문서 70%|1.80119\n"
            + "방어구 마력 주문서 70%|1.80119\n"
            + "펫장비 마력 주문서 70%|1.80119\n"
            + "악세서리 마력 주문서 70%|1.80119";

    public static void main(String[] args) {

        String[] strs = parse2.split("\\n");
        for (int i = 0; i < strs.length; i++) {
            List<Triple<String, String, String>> retItems = new ArrayList<>();
            String[] data = strs[i].split("\\|");
            for (Pair<Integer, String> itemPair : MapleItemInformationProvider.getInstance().getAllItems()) {
                if (itemPair.getRight().toLowerCase().equals(data[0])) {
                    retItems.add(new Triple(itemPair.getLeft() + "", itemPair.getRight(), data[1]));
                }
            }
            //  BigDecimal ff = new BigDecimal(data[1]);
            // System.out.println(data[0] + "의 확률은 " + ff +"%");
            // dd = dd.add(ff);
            Scanner sc = new Scanner(System.in);
            List<Triple<String, String, String>> itemcodes = new ArrayList<>();
            if (retItems != null && retItems.size() > 0) {
                if (retItems.size() != 1) {
                    System.out.println("중복리스트가 존재");
                    for (int z = 0; z < retItems.size(); z++) {
                        System.out.println((z + 1) + ". " + retItems.get(z).getLeft() + " - " + retItems.get(z).getMid());
                    }
                    int geti = sc.nextInt();
                    if (geti != -1) {
                        itemcodes.add(retItems.get(geti - 1));
                    }
                } else {
                    itemcodes.add(retItems.get(0));
                }
            }
        }
        BigDecimal dd = new BigDecimal("0.0");
        for (int i = 0; i < strs.length; i++) {
            String[] data = strs[i].split("\\|");
            BigDecimal ff = new BigDecimal(data[1]);
            System.out.println(data[0] + "의 확률은 " + ff + "%");
            dd = dd.add(ff);
        }
        System.out.println("다합친 확률은 " + dd.toString() + "%");
    }
}