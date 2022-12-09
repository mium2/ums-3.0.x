import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.KomoranResult;
import kr.co.shineware.nlp.komoran.model.Token;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BadWordFilterMain {

    private Set<String> STOP_KWD;

    public static void main(String[] args){
        BadWordFilterMain badWordFilterMain = new BadWordFilterMain();
        badWordFilterMain.setStopKwdFromDB(); // 금칙어 로드
//        badWordFilterMain.action(); // 한국어 금칙어 검사로직 수행
        badWordFilterMain.actionIncludeEng(); // 영어포함 금칙어 검사로직 수행
    }

    public void action() {
        long startTime = System.currentTimeMillis();
        Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);
        String document ="이현진 기자의 보도입니다.\n" +
                "\n" +
                "[리포트]\n" +
                "\n" +
                "울산의 주택 월세가격 상승세가 심상치 않습니다.\n" +
                "\n" +
                "한국부동산원에 따르면 지난달에도 울산의 주택 월세가격은 한달 전보다 0.57%, 일년 전과 비교하면 무려 7.28%나 상승한 것으로 나타났습니다.\n" +
                "\n" +
                "이는 전국 17개 시도 가운데 가장 높은 수치이며 전국 평균 월세가격 상승률인 0.16%보다도 세배 이상 높은 겁니다.\n" +
                "\n" +
                "구·군별로는 동구의 월세가격이 전월 대비 0.82% 올라 가장 높았고 중구 북구 남구 울주군 등의 순이었습니다.\n" +
                "\n" +
                "동구의 경우 최근 조선업 수주 호조세에 따른 단기 거주 목적의 인구 유입이 원인이라는 분석입니다.\n" +
                "\n" +
                "[이영래/부동산리서치 대표 : \"동구 지역에서 인구가 유입되는 수요들은 대부분 원룸이라든지 투룸 수요가 많기 때문에 이들은 대부분 월세 수요라고 볼 수 있을 것같습니다.\"]\n" +
                "\n" +
                "또 공급 부족으로 전세값이 계속 오르자 상당수 세입자들이 전세를 월세로 전환하고 있는 점도 월세가격 상승을 부채질한 것으로 분석됐습니다.\n" +
                "\n" +
                "반면 지난달 울산의 주택가격은 0.02% 하락세로 전환됐습니다.\n" +
                "\n" +
                "울산의 집값이 월간 기준으로 떨어진 건 2년 9개월 만에 처음입니다.\n" +
                "\n" +
                "최근 남구와 중구가 조정대상지역 해제 대상에서 제외되자 실망 매물도 상당수 시장에 나왔다는 분석입니다.\n" +
                "\n" +
                "부동산업계는 고강도 대출 규제와 금리 인상 압박으로 주택 매매시장은 당분간 거래량이 줄고 집값도 조정을 거칠 것으로 내다봤습니다.\n" +
                "\n" +
                "KBS 뉴스 이현진입니다.\n" +
                "\n" +
                "촬영기자:김용삼\n";

        KomoranResult analyzeResultList = komoran.analyze(document);

        List<String> filterNounList = new ArrayList<>();
        for(String noun : analyzeResultList.getNouns()) {
            if(STOP_KWD.contains(noun)) {
                System.out.println("필터된 단어 : "+noun);
                continue;
            }
            filterNounList.add(noun);
        }
        long processTime = System.currentTimeMillis()-startTime;
        System.out.println(filterNounList);
        System.out.println("분석시간 : "+processTime);
    }

    public void actionIncludeEng(){
        long startTime = System.currentTimeMillis();
        Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);
        String document ="이현진 기자의 보도입니다.\n" +
                "\n" +
                "[리포트]\n" +
                "\n" +
                "울산의 주택 월세가격 상승세가 심상치 않습니다.\n" +
                "\n" +
                "한국부동산원에 따르면 지난달에도 울산의 주택 월세가격은 한달 전보다 0.57%, 일년 전과 비교하면 무려 7.28%나 상승한 것으로 나타났습니다.\n" +
                "\n" +
                "이는 전국 17개 시도 가운데 가장 높은 수치이며 전국 평균 월세가격 상승률인 0.16%보다도 세배 이상 높은 겁니다.\n" +
                "\n" +
                "구·군별로는 동구의 월세가격이 전월 대비 0.82% 올라 가장 높았고 중구 북구 남구 울주군 등의 순이었습니다.\n" +
                "\n" +
                "동구의 경우 최근 조선업 수주 호조세에 따른 단기 거주 목적의 인구 유입이 원인이라는 분석입니다.\n" +
                "\n" +
                "[이영래/부동산리서치 대표 : \"동구 지역에서 인구가 유입되는 수요들은 대부분 원룸이라든지 투룸 수요가 많기 때문에 이들은 대부분 월세 수요라고 볼 수 있을 것같습니다.\"]\n" +
                "\n" +
                "또 공급 부족으로 전세값이 계속 오르자 상당수 세입자들이 전세를 월세로 전환하고 있는 점도 월세가격 상승을 부채질한 것으로 분석됐습니다.\n" +
                "\n" +
                "반면 지난달 울산의 주택가격은 0.02% 하락세로 전환됐습니다.\n" +
                "\n" +
                "울산의 집값이 월간 기준으로 떨어진 건 2년 9개월 만에 처음입니다.\n" +
                "\n" +
                "최근 남구와 중구가 조정대상지역 해제 대상에서 제외되자 실망 매물도 상당수 시장에 나왔다는 분석입니다.\n" +
                "\n" +
                "부동산업계는 고강도 대출 규제와 금리 인상 압박으로 주택 매매시장은 당분간 거래량이 줄고 집값도 조정을 거칠 것으로 내다봤습니다.\n" +
                "\n" +
                "KBS 뉴스 이현진입니다.\n" +
                "\n" +
                "촬영기자:김용삼\n";

        KomoranResult analyzeResultList = komoran.analyze(document);
        List<String> filterNounList = new ArrayList<>();
        List<Token> tokenList = analyzeResultList.getTokenList();
        for (Token token : tokenList) {
//            System.out.format("(%2d, %2d) %s/%s\n", token.getBeginIndex(), token.getEndIndex(), token.getMorph(), token.getPos());
            if("NNG".equals(token.getPos()) || "NNP".equals(token.getPos())  || "SL".equals(token.getPos())){
                if(STOP_KWD.contains(token.getMorph())) {
                    System.out.println("필터된 단어 : "+token.getMorph());
                    continue;
                }
                filterNounList.add(token.getMorph());
            }
        }

        long processTime = System.currentTimeMillis()-startTime;
        System.out.println(filterNounList);
        System.out.println("분석시간 : "+processTime);
    }

    public void setStopKwdFromDB() {
        STOP_KWD = new HashSet<>();
        try {
            STOP_KWD.add("개새끼");
            STOP_KWD.add("씨발");
            STOP_KWD.add("fuck");
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("set stop kwds->" + STOP_KWD.size());
    }

    public void setStopKwdFromFile() {
        STOP_KWD = new HashSet<>();
        try {
            BufferedReader inFiles = new BufferedReader(new InputStreamReader(
                    new FileInputStream("stop.dic"), StandardCharsets.UTF_8));

            String line = null;
            while ((line = inFiles.readLine()) != null) {
                if(line.trim().length() > 0) STOP_KWD.add(line.trim());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("set stop kwds->" + STOP_KWD.size());
    }
}
