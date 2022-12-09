package kr.uracle.ums.core.processor.statistics;

import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.core.processor.bean.StatisticsBean;
import kr.uracle.ums.core.processor.bean.UmsResultBaseBean;
import kr.uracle.ums.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Y.B.H(mium2) on 2019. 4. 3..
 */
@Service
public class StatisticsMgr {
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    // 여기서의 첫번째 맵키는 SENDATE+SENDERID+SENDERTYPE
    private static Map<String,StatisticsBean> sendMsgInfoMap = new ConcurrentHashMap<String, StatisticsBean>();

    /**
     * 일별 발송통계를 넣기 위해. DB부하 최소화
     * @param umsResultBaseBean
     */
    public synchronized void putSentResultMsg(UmsResultBaseBean umsResultBaseBean){
        String sendDay = DateUtil.getSysDay();
        StringBuilder mapKeySB = new StringBuilder(umsResultBaseBean.getTRAN_TYPE().toString()+sendDay+umsResultBaseBean.getSENDERID());
        mapKeySB.append(umsResultBaseBean.getDETAIL_SEND_TYPE());
        mapKeySB.append(umsResultBaseBean.getROOT_CHANNEL_YN());
        // 푸시 발송결과 통계저장처리
        StatisticsBean statisticsBean = new StatisticsBean();
        SendType sendType = SendType.valueOf(umsResultBaseBean.getSEND_TYPE());
        statisticsBean.setROOT_CHANNEL_YN(umsResultBaseBean.getROOT_CHANNEL_YN());
        switch (sendType){
            case PUSH: case WPUSH:
                mapKeySB.append(umsResultBaseBean.getAPP_ID());
                statisticsBean.setOPTIONAL(umsResultBaseBean.getAPP_ID());
                break;
            case KKOALT: case KKOFRT : case NAVERT:
                mapKeySB.append(umsResultBaseBean.getPROVIDER());
                statisticsBean.setOPTIONAL(umsResultBaseBean.getPROVIDER());
                break;
            case SMS: case LMS: case MMS: case RCS_SMS: case RCS_LMS: case RCS_MMS: case RCS_FREE: case RCS_CELL: case RCS_DESC:
                mapKeySB.append(umsResultBaseBean.getPROVIDER());
                statisticsBean.setOPTIONAL(umsResultBaseBean.getPROVIDER());
                break;
        }

        // 일정시간 수집하여  DB에 인설트하기 때문에 DB에 인설트 되기 전까지는 메모리에 정보가 저장되어있다.
        if(sendMsgInfoMap.containsKey(mapKeySB.toString())){
            statisticsBean = sendMsgInfoMap.get(mapKeySB.toString());
        }
        statisticsBean.setTRANS_TYPE(umsResultBaseBean.getTRAN_TYPE());
        statisticsBean.setSENDDATE(sendDay);
        statisticsBean.setSENDERID(umsResultBaseBean.getSENDERID());
        statisticsBean.setSENDERGROUP(umsResultBaseBean.getSENDERGROUPCODE());
        statisticsBean.setSENDTYPE(umsResultBaseBean.getSEND_TYPE());
        statisticsBean.setDETAIL_SENDTYPE(umsResultBaseBean.getDETAIL_SEND_TYPE());

        // 발송결과 처리별 통계저장. 푸시/웹푸시의 경우 http 통신므로 요청과 결과가 동시에 업데이트됨.
        if("SS".equals(umsResultBaseBean.getSEND_RESULT())){
            statisticsBean.setSS_CNT(statisticsBean.getSS_CNT()+1);
        }else if("FS".equals(umsResultBaseBean.getSEND_RESULT())){
            statisticsBean.setFS_CNT(statisticsBean.getFS_CNT()+1);
            statisticsBean.setRS_CNT(statisticsBean.getRS_CNT()+1);

        }else if("FF".equals(umsResultBaseBean.getSEND_RESULT())){
            statisticsBean.setFF_CNT(statisticsBean.getFF_CNT()+1);
            statisticsBean.setRS_CNT(statisticsBean.getRS_CNT()+1);

        }else{
            if(sendType==SendType.PUSH || sendType==SendType.WPUSH){
                // 푸시,웹푸시일 경우 발송요청성공시 발송성공으로 카운트 처리.
                statisticsBean.setRS_CNT(statisticsBean.getRS_CNT()+1);
                statisticsBean.setSS_CNT(statisticsBean.getSS_CNT()+1);
            }else{
                statisticsBean.setRS_CNT(statisticsBean.getRS_CNT()+1);
            }
        }

        sendMsgInfoMap.put(mapKeySB.toString(),statisticsBean);
    }

    public synchronized Map<String,StatisticsBean> getSentResultInfoMap(){
        // 해당날자에 있는 발송자별 보낸 카운트 맵정보를 복사하여 보내 DB에 저장하게 하고 지운다.
        // 지우는 이유는 매번 주기적으로 호출되기 때문 없을 경우 DB실행 안하고 멀티운영서버로 운영될 경우 업데이트시 허수 카운트 증가가 하기 때문
        Map<String,StatisticsBean> cloneMap = new HashMap<String, StatisticsBean>();
        logger.trace("##[clear before] sendMsgInfoMap size:" + sendMsgInfoMap.size());
        Set<Map.Entry<String,StatisticsBean>> sendMsgInfoMapSet = sendMsgInfoMap.entrySet();
        for (Map.Entry<String,StatisticsBean> me : sendMsgInfoMapSet) {
            cloneMap.put(me.getKey(),me.getValue());
        }
        sendMsgInfoMap.clear();
        logger.trace("##[clear after] sendMsgInfoMap size:"+sendMsgInfoMap.size());
        return cloneMap;
    }
}
