package kr.uracle.ums.core.extention.service.division;

import com.google.gson.Gson;
import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DivisionRollManager implements IDivisionRollManager{
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    @Value("${KKOALT.DIVISION.TABLE:}")
    private String KKOALT_TABLE_JSON;
    @Value("${KKOFRT.DIVISION.TABLE:}")
    private String KKOFRT_TABLE_JSON;
    @Value("${SMS.DIVISION.TABLE:}")
    private String SMS_TABLE_JSON;
    @Value("${MMS.DIVISION.TABLE:}")
    private String MMS_TABLE_JSON;
    @Value("${RCS.DIVISION.TABLE:}")
    private String RCS_TABLE_JSON;
    @Value("${NAVER.DIVISION.TABLE:}")
    private String NAVER_TABLE_JSON;

    private Gson gson = new Gson();

    // 키: 알림톡 공급사, Value : 리얼/배치 테이블며정보
    private Map<String, Map<String,String>> KKOALT_TABLE_MAP= null;
    private Map<String, Map<String,String>> KKOFRT_TABLE_MAP= null;
    private Map<String, Map<String,String>> SMS_TABLE_MAP= null;
    private Map<String, Map<String,String>> MMS_TABLE_MAP= null;
    private Map<String, Map<String,String>> RCS_TABLE_MAP= null;
    private Map<String, Map<String,String>> NAVER_TABLE_MAP= null;
    private Map<String, List<String>> smsTablesByProvideRealMap = new HashMap<>();
    private Map<String, List<String>> mmsTablesByProvideRealMap = new HashMap<>();
    private Map<String, List<String>> rcsTablesByProvideRealMap = new HashMap<>();
    private Map<String, List<String>> kkoAltTablesByProvideRealMap = new HashMap<>();
    private Map<String, List<String>> kkoFrtTablesByProvideRealMap = new HashMap<>();
    private Map<String, List<String>> smsTablesByProvideBatchMap = new HashMap<>();
    private Map<String, List<String>> mmsTablesByProvideBatchMap = new HashMap<>();
    private Map<String, List<String>> rcsTablesByProvideBatchMap = new HashMap<>();
    private Map<String, List<String>> kkoAltTablesByProvideBatchMap = new HashMap<>();
    private Map<String, List<String>> kkoFrtTablesByProvideBatchMap = new HashMap<>();
    private boolean isInitLoad = false;
    private Map<String, Integer> smsCallCntByProvider = new HashMap<>();
    private Map<String, Integer> mmsCallCntByProvider = new HashMap<>();
    private Map<String, Integer> rcsCallCntByProvider = new HashMap<>();
    private Map<String, Integer> kkoAltCallCntByProvider = new HashMap<>();

    private void providerTableInitLoad(){
        this.isInitLoad = true;
        if(!KKOALT_TABLE_JSON.equals("")) {
            try {
                KKOALT_TABLE_MAP = gson.fromJson(KKOALT_TABLE_JSON,Map.class);
                if(KKOALT_TABLE_MAP!=null && KKOALT_TABLE_MAP.size()>0){
                    makeTablesByProvideMap("KKOALT", KKOALT_TABLE_MAP);
                }
            } catch (Exception e) {
                logger.error("알림톡 JSON테이블정보 변화시 에러 : " + e.getCause());
            }
        }
        if(!KKOFRT_TABLE_JSON.equals("")) {
            try {
                KKOFRT_TABLE_MAP = gson.fromJson(KKOFRT_TABLE_JSON,Map.class);
                if(KKOFRT_TABLE_MAP!=null && KKOFRT_TABLE_MAP.size()>0){
                    makeTablesByProvideMap("KKOFRT", KKOFRT_TABLE_MAP);
                }
            } catch (Exception e) {
                logger.error("친구톡 JSON테이블정보 변화시 에러 : " + e.getCause());
            }
        }
        if(!SMS_TABLE_JSON.equals("")) {
            try {
                SMS_TABLE_MAP = gson.fromJson(SMS_TABLE_JSON,Map.class);
                if(SMS_TABLE_MAP!=null && SMS_TABLE_MAP.size()>0){
                    makeTablesByProvideMap("SMS", SMS_TABLE_MAP);
                }
            } catch (Exception e) {
                logger.error("SMS JSON테이블정보 변화시 에러 : " + e.getCause());
            }
        }
        if(!MMS_TABLE_JSON.equals("")) {
            try {
                MMS_TABLE_MAP = gson.fromJson(MMS_TABLE_JSON,Map.class);
                if(MMS_TABLE_MAP!=null && MMS_TABLE_MAP.size()>0){
                    makeTablesByProvideMap("MMS", MMS_TABLE_MAP);
                }
            } catch (Exception e) {
                logger.error("MMS JSON테이블정보 변화시 에러 : " + e.getCause());
            }
        }
        if(!RCS_TABLE_JSON.equals("")) {
            try {
                RCS_TABLE_MAP = gson.fromJson(RCS_TABLE_JSON,Map.class);
                if(RCS_TABLE_MAP!=null && RCS_TABLE_MAP.size()>0){
                    makeTablesByProvideMap("RCS", RCS_TABLE_MAP);
                }
            } catch (Exception e) {
                logger.error("RCS JSON테이블정보 변화시 에러 : " + e.getCause());
            }
        }
        if(!NAVER_TABLE_JSON.equals("")) {
            try {
                NAVER_TABLE_MAP = gson.fromJson(NAVER_TABLE_JSON,Map.class);
            } catch (Exception e) {
                logger.error("네이버톡 JSON테이블정보 변화시 에러 : " + e.getCause());
            }
        }
    }
    private void makeTablesByProvideMap(String channel, Map<String, Map<String,String>> tableMapByProvider){
        Set<String> provideSet = tableMapByProvider.keySet();
        for(String provider : provideSet){
            Map<String,String> tableMap = tableMapByProvider.get(provider);
            if(tableMap!=null && tableMap.size()>0) {
                Set<String> tableKeySet = tableMap.keySet();
                List<String> realTables = new ArrayList<>();
                List<String> batchTables = new ArrayList<>();
                for (String tableKey : tableKeySet){
                    String tableName = tableMap.get(tableKey);
                    if(tableKey.startsWith("REAL")){
                        realTables.add(tableName);
                    }else{
                        batchTables.add(tableName);
                    }
                }
                if(channel.equals("SMS")){
                    smsTablesByProvideRealMap.put(provider,realTables);
                    smsTablesByProvideBatchMap.put(provider,batchTables);
                }else if(channel.equals("MMS")){
                    mmsTablesByProvideRealMap.put(provider,realTables);
                    mmsTablesByProvideBatchMap.put(provider,batchTables);
                }else if(channel.equals("RCS")){
                    rcsTablesByProvideRealMap.put(provider,realTables);
                    rcsTablesByProvideBatchMap.put(provider,batchTables);
                }else if(channel.equals("KKOALT")){
                    kkoAltTablesByProvideRealMap.put(provider,realTables);
                    kkoAltTablesByProvideBatchMap.put(provider,batchTables);
                }else if(channel.equals("KKOFRT")){
                    kkoFrtTablesByProvideRealMap.put(provider,realTables);
                    kkoFrtTablesByProvideBatchMap.put(provider,batchTables);
                }
            }
        }
    }

    @Override
    public String getPutTableName(BaseProcessBean baseProcessBean, SendType sendType, String channelProvider) {
        if(!isInitLoad){
            providerTableInitLoad();
        }
        String returnTableName = null;
        switch (sendType){
            case SMS:
                if(SMS_TABLE_MAP==null || SMS_TABLE_MAP.size()==0){
                    return null;
                }
                if(SMS_TABLE_MAP.containsKey(channelProvider)){
                    // 공급사 실시간/배치 테이블정보 맵
                    if(baseProcessBean.getTRANS_TYPE()== TransType.REAL){
                        returnTableName = getTableInfo("SMS", channelProvider, smsTablesByProvideRealMap);
                    }else{
                        returnTableName = getTableInfo("SMS", channelProvider, smsTablesByProvideBatchMap);
                    }
                }
                break;
            case LMS: case MMS:
                if(MMS_TABLE_MAP==null || MMS_TABLE_MAP.size()==0){
                    return null;
                }
                if(MMS_TABLE_MAP.containsKey(channelProvider)){
                    // 공급사 실시간/배치 테이블정보 맵
                    if(baseProcessBean.getTRANS_TYPE()==TransType.REAL){
                        returnTableName = getTableInfo("MMS", channelProvider, mmsTablesByProvideRealMap);
                    }else{
                        returnTableName = getTableInfo("MMS", channelProvider, mmsTablesByProvideBatchMap);
                    }
                }
                break;
            case KKOALT:
                if(KKOALT_TABLE_MAP==null || KKOALT_TABLE_MAP.size()==0){
                    return null;
                }
                if(KKOALT_TABLE_MAP.containsKey(channelProvider)){
                    // 공급사 실시간/배치 테이블정보 맵
                    if(baseProcessBean.getTRANS_TYPE()==TransType.REAL){
                        returnTableName = getTableInfo("KKOALT", channelProvider, kkoAltTablesByProvideRealMap);
                    }else{
                        returnTableName = getTableInfo("KKOALT", channelProvider, kkoAltTablesByProvideBatchMap);
                    }
                }
                break;
            case KKOFRT:
                if(KKOFRT_TABLE_MAP==null || KKOFRT_TABLE_MAP.size()==0){
                    return null;
                }
                if(KKOFRT_TABLE_MAP.containsKey(channelProvider)){
                    // 공급사 실시간/배치 테이블정보 맵
                    if(baseProcessBean.getTRANS_TYPE()==TransType.REAL){
                        returnTableName = getTableInfo("KKOFRT", channelProvider, kkoFrtTablesByProvideRealMap);
                    }else{
                        returnTableName = getTableInfo("KKOFRT", channelProvider, kkoFrtTablesByProvideBatchMap);
                    }
                }
                break;
            case RCS_SMS: case RCS_LMS: case RCS_MMS: case RCS_FREE: case RCS_CELL: case RCS_DESC:
                if(RCS_TABLE_MAP==null || RCS_TABLE_MAP.size()==0){
                    return null;
                }
                if(RCS_TABLE_MAP.containsKey(channelProvider)){
                    // 공급사 실시간/배치 테이블정보 맵
                    if(baseProcessBean.getTRANS_TYPE()==TransType.REAL){
                        returnTableName = getTableInfo("RCS", channelProvider, rcsTablesByProvideRealMap);
                    }else{
                        returnTableName = getTableInfo("RCS", channelProvider, rcsTablesByProvideBatchMap);
                    }
                }
                break;
        }
        return returnTableName;
    }

    private String getTableInfo(String channel, String channelProvider, Map<String, List<String>> tablesByProvideMap ){
        String returnTableName = null;
        if(tablesByProvideMap.containsKey(channelProvider)) {
            List<String> tableList = tablesByProvideMap.get(channelProvider);
            if(tableList.size()==1){
                returnTableName = tableList.get(0);
            }else if(tableList.size()>1){
                returnTableName = getRoundRobinTableInfo(channel, channelProvider, tableList);
            }
        }
        return returnTableName;
    }

    private synchronized String getRoundRobinTableInfo(String channel, String provider, List<String> tableList){
        String returnTableName = null;
        List<String> tableInfos = tableList;
        if("SMS".equals(channel)){
            int smsCallCnt = 0;
            if(smsCallCntByProvider.containsKey(provider)){
                smsCallCnt = smsCallCntByProvider.get(provider);
            }
            int findPos = smsCallCnt%tableInfos.size();
            returnTableName = tableInfos.get(findPos);
            if (smsCallCnt == 1000) {
                smsCallCnt = 0;
            } else {
                smsCallCnt++;
            }
            smsCallCntByProvider.put(provider,smsCallCnt);
        }else if("MMS".equals(channel)){
            int mmsCallCnt = 0;
            if(mmsCallCntByProvider.containsKey(provider)){
                mmsCallCnt = mmsCallCntByProvider.get(provider);
            }

            int findPos = mmsCallCnt%tableInfos.size();
            returnTableName = tableInfos.get(findPos);
            if (mmsCallCnt == 1000) {
                mmsCallCnt = 0;
            } else {
                mmsCallCnt++;
            }
            mmsCallCntByProvider.put(provider,mmsCallCnt);
        }else if("RCS".equals(channel)){
            int rcsCallCnt = 0;
            if(rcsCallCntByProvider.containsKey(provider)){
                rcsCallCnt = rcsCallCntByProvider.get(provider);
            }

            int findPos = rcsCallCnt%tableInfos.size();
            returnTableName = tableInfos.get(findPos);
            if (rcsCallCnt == 1000) {
                rcsCallCnt = 0;
            } else {
                rcsCallCnt++;
            }
            rcsCallCntByProvider.put(provider,rcsCallCnt);
        }else if("KKOALT".equals(channel)){
            int kkoAltCallCnt = 0;
            if(kkoAltCallCntByProvider.containsKey(provider)){
                kkoAltCallCnt = kkoAltCallCntByProvider.get(provider);
            }

            int findPos = kkoAltCallCnt%tableInfos.size();
            returnTableName = tableInfos.get(findPos);
            if (kkoAltCallCnt == 1000) {
                kkoAltCallCnt = 0;
            } else {
                kkoAltCallCnt++;
            }
            kkoAltCallCntByProvider.put(provider,kkoAltCallCnt);
        }
        return returnTableName;
    }
}
