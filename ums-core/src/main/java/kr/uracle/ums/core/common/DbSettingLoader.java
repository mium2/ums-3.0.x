package kr.uracle.ums.core.common;

import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Y.B.H(mium2) on 2019. 3. 4..
 */
public class DbSettingLoader {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    private static DbSettingLoader instance = null;
    //푸쉬 발송 정책 메모리에 로드
    private Map<String,List<Map<String,Object>>> servicecodeMap = new ConcurrentHashMap<String,List<Map<String,Object>>>();
    private SqlSessionTemplate sqlSessionTemplate;

    private Map<String,String> plusIdMap = new HashMap<>();

    public static DbSettingLoader getInstance(){
        if(instance==null){
            instance = new DbSettingLoader();
        }
        return instance;
    }

    public void init(){
        sqlSessionTemplate = (SqlSessionTemplate) UmsInitListener.wContext.getBean("sqlSessionTemplate");
//        loadServiceCode();
//        settingPlusID();
    }

    public void loadServiceCode(){
        logger.info("#####################################################################");
        logger.info("# UMS Damon  PUSH SERVICE SEND POLICE DB Initializing~~!");
        logger.info("#####################################################################");
        servicecodeMap = new HashMap<String, List<Map<String, Object>>>();

        List<Map<String, Object>> pushServiceCodes = null;
        try {
            pushServiceCodes = sqlSessionTemplate.selectList("mybatis.common.getAllPushService");
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (Map<String, Object> pushServicemap : pushServiceCodes) {
            List<Map<String, Object>> serviceList = null;
            String ServiceCode = pushServicemap.get("SERVICECODE").toString();
            if (servicecodeMap.containsKey(ServiceCode)) {
                serviceList = servicecodeMap.get(ServiceCode);
            } else {
                serviceList = new ArrayList<Map<String, Object>>();
            }
            serviceList.add(pushServicemap);
            servicecodeMap.put(ServiceCode, serviceList);
        }
    }

    private void settingPlusID(){
        Map<String,Object> dbParamMap = new HashMap<>();
        List<HashMap<String,Object>> dbPlusidList = sqlSessionTemplate.selectList("mybatis.common.selKkoInfo",dbParamMap);
        if(dbPlusidList!=null){
            for(HashMap<String,Object> dbMap : dbPlusidList){
                if(dbMap.containsKey("ALLIMTOLK_SVC_ID")){
                    if(dbMap.get("ALLIMTOLK_SVC_ID")!=null) {
                        plusIdMap.put(dbMap.get("ALLIMTOLK_SVC_ID").toString(), dbMap.get("PLUS_ID").toString());
                    }
                }
                if(dbMap.containsKey("FRIENDTOLK_SVC_ID")){
                    if(dbMap.get("FRIENDTOLK_SVC_ID")!=null) {
                        plusIdMap.put(dbMap.get("FRIENDTOLK_SVC_ID").toString(), dbMap.get("PLUS_ID").toString());
                    }
                }
            }
        }
    }

    public String getPlusID(String SVCID){
        String returnPlusID = "";
        if(plusIdMap.containsKey(SVCID)){
            returnPlusID = plusIdMap.get(SVCID);
        }
        return returnPlusID;
    }

    public Map<String, List<Map<String, Object>>> getServicecodeMap() {
        return servicecodeMap;
    }
}
