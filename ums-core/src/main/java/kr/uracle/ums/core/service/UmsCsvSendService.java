package kr.uracle.ums.core.service;

import au.com.bytecode.opencsv.CSVReader;
import com.google.gson.Gson;
import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.core.dao.ums.UmsDao;
import kr.uracle.ums.core.exception.RequestErrException;
import kr.uracle.ums.core.processor.SentInfoManager;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import kr.uracle.ums.core.service.send.push.PushSendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Created by Y.B.H(mium2) on 2019. 4. 1..
 * 해당 클래스는 CSV파일을 통해 UMS에 가입되어 있지 않는 타겟팅 대상자를 처리 하는 클래스이다. 따라서 반드시 핸드폰번호와 이름을 받아야 한다.
 * Target 구조는 JsonObject이다. 예 {"아이디1":["핸드폰번호1","이름1"], "아이디2":["핸드폰번호2","이름2"]...}
 * 또한 CSV파일 1라인 해더값에 #{금액} #{날짜}..등과 같은 개인화 치환 메세지 발송이 가능 하다.
 * 필수 헤더 값 #{아이디} #{핸드폰번호} #{이름}
 */
@Service
@SuppressWarnings("unchecked")
public class UmsCsvSendService extends UmsCsvBase{
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    @Autowired(required=true)
    private MessageSource messageSource;
    @Autowired(required = true)
    private UmsSendCommonService umsSendCommonService;

    @Autowired(required = true)
    private UmsDao umsDao;
    @Autowired(required = true)
    private SentInfoManager sentInfoManager;
    @Autowired (required = true)
    private PushSendService pushSendService;

    @Autowired
    private Gson gson;
    @Value("${UMS.TEMPDIR:}")
    private String TEMPDIR;
    private Set<String> comonVarsSet = new HashSet<String>();
    public UmsCsvSendService(){
        comonVarsSet.add("#{이름}");
        comonVarsSet.add("#{아이디}");
        comonVarsSet.add("#{핸드폰번호}");
    }
    /**
     * UMS CSV푸시 발송
     * @param umsSendMsgBean
     * @return
     * @throws Exception
     */
    public Map<String,Object> umsPushCsvSend(UmsSendMsgBean umsSendMsgBean) throws Exception{
     
        //STEP 1 : CSV파일 점검.
        Map<String,Object> csvFeturnMap = super.confirmCsvFile(umsSendMsgBean.getCSVFILE());
        CSVReader reader = (CSVReader)csvFeturnMap.get("csvReader");
        Map<String,Integer> varsIndexMap =  (Map<String,Integer>)csvFeturnMap.get("varIdxMap");

        //STEP 2:[ 메세지 검증]
        super.checkSendMsg(umsSendMsgBean, varsIndexMap);

        // STEP 7: 푸시가입유저, 미가입 유저 분리.
        List<String[]> csvList = reader.readAll();

        if(!"".equals(umsSendMsgBean.getRESERVEDATE())){
            umsSendMsgBean.setTOTAL_CNT(csvList.size());
            Map<String,Object> returnResultMap = new HashMap<String,Object>();
            //예약날짜 올바른 형식의 날자인지 검증
            if(!umsSendCommonService.dateCheck(umsSendMsgBean.getRESERVEDATE(),"yyyy-MM-dd HH:mm")){
                throw new Exception("The reservation time format(yyyy-MM-dd HH:mm) is incorrect.");
            }
            //CSV 예약발송 구현. 첨부파일이 있으므로 UMS.HOST 정보를 DB에 저장하여 해당 UMS에 발송하도록 처리 하여야한다.
            Map<String,String> csvSaveReturnMap = saveCsv(umsSendMsgBean.getCSVFILE());
            umsDao.inUmsSendReserveMsg(umsSendMsgBean,csvSaveReturnMap);

            // [공통] : 채널별 발송 카운트 셋팅처리. 예약발송 때문 여기서 먼저 한번  셋팅처리.
            umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean,returnResultMap);
            return returnResultMap;
        }

        Map<String,Object> pushUserChkMap = chkCsvPushUser(csvList, varsIndexMap, umsSendMsgBean.getAPP_ID(),false);
        Map<String,List<String>> regPushUserMap = (Map<String,List<String>>)pushUserChkMap.get("regPushUserObj"); //{"아이디":["핸드폰번호","이름"]}
        Map<String,List<String>> notRegPushUserMap = (Map<String,List<String>>)pushUserChkMap.get("notRegPushUserObj"); //{"아이디":["핸드폰번호","이름"]}
        Map<String,Map<String,String>> cuidVarMap = (Map<String,Map<String,String>>)pushUserChkMap.get("pushCuidVarMap"); //CUID를 키로 한 개인화 메세지 정보.
        Map<String,Map<String,String>> notPushCuidVarMap = (Map<String,Map<String,String>>)pushUserChkMap.get("notPushCuidVarMap"); //푸시발송 대상자가 아닌 CUID를 키로 한 개인화 메세지 정보.

        return super.umsPushCsvSend(regPushUserMap, notRegPushUserMap, cuidVarMap, notPushCuidVarMap, umsSendMsgBean);
    }

    /**
     * UMS CSV 웹푸시 발송
     * @param umsSendMsgBean
     * @return
     * @throws Exception
     */
    public Map<String,Object> umsWPushCsvSend(UmsSendMsgBean umsSendMsgBean) throws Exception{

        //STEP 1 : CSV파일 점검.
        Map<String,Object> csvFeturnMap = super.confirmCsvFile(umsSendMsgBean.getCSVFILE());
        CSVReader reader = (CSVReader)csvFeturnMap.get("csvReader");
        Map<String,Integer> varsIndexMap =  (Map<String,Integer>)csvFeturnMap.get("varIdxMap");

        //STEP 2:[ 메세지 검증]
        super.checkSendMsg(umsSendMsgBean, varsIndexMap);

        // STEP 7: 웹푸시가입유저, 미가입 유저 분리.
        List<String[]> csvList = reader.readAll();

        if(!"".equals(umsSendMsgBean.getRESERVEDATE())){
            umsSendMsgBean.setTOTAL_CNT(csvList.size());
            Map<String,Object> returnResultMap = new HashMap<String,Object>();
            //예약날짜 올바른 형식의 날자인지 검증
            if(!umsSendCommonService.dateCheck(umsSendMsgBean.getRESERVEDATE(),"yyyy-MM-dd HH:mm")){
                throw new Exception("The reservation time format(yyyy-MM-dd HH:mm) is incorrect.");
            }
            //CSV 예약발송 구현. 첨부파일이 있으므로 UMS.HOST 정보를 DB에 저장하여 해당 UMS에 발송하도록 처리 하여야한다.
            Map<String,String> csvSaveReturnMap = saveCsv(umsSendMsgBean.getCSVFILE());
            umsDao.inUmsSendReserveMsg(umsSendMsgBean,csvSaveReturnMap);

            // [공통] : 채널별 발송 카운트 셋팅처리. 예약발송 때문 여기서 먼저 한번  셋팅처리.
            umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean,returnResultMap);
            return returnResultMap;
        }

        Map<String,Object> pushUserChkMap = chkCsvPushUser(csvList, varsIndexMap, umsSendMsgBean.getWPUSH_DOMAIN(),false);
        Map<String,List<String>> regPushUserMap = (Map<String,List<String>>)pushUserChkMap.get("regPushUserObj"); //{"아이디":["핸드폰번호","이름"]}
        Map<String,List<String>> notRegPushUserMap = (Map<String,List<String>>)pushUserChkMap.get("notRegPushUserObj"); //{"아이디":["핸드폰번호","이름"]}
        Map<String,Map<String,String>> cuidVarMap = (Map<String,Map<String,String>>)pushUserChkMap.get("pushCuidVarMap"); //CUID를 키로 한 개인화 메세지 정보.
        Map<String,Map<String,String>> notPushCuidVarMap = (Map<String,Map<String,String>>)pushUserChkMap.get("notPushCuidVarMap"); //푸시발송 대상자가 아닌 CUID를 키로 한 개인화 메세지 정보.

        return super.umsWPushCsvSend(regPushUserMap, notRegPushUserMap, cuidVarMap, notPushCuidVarMap, umsSendMsgBean);
    }

    /**
     * CSV UMS 알림톡 발송
     * @param umsSendMsgBean
     * @return
     * @throws Exception
     */
    public Map<String,Object> umsAltCsvSend(UmsSendMsgBean umsSendMsgBean) throws Exception{

        //STEP 2 : CSV파일 점검.
        Map<String,Object> csvFeturnMap = confirmCsvFile(umsSendMsgBean.getCSVFILE());
        CSVReader reader = (CSVReader)csvFeturnMap.get("csvReader");
        Map<String,Integer> varsIndexMap =  (Map<String,Integer>)csvFeturnMap.get("varIdxMap");

        //STEP 3: 카톡 알림톡은 카톡에서 승인을 받은 등록된 템플릿만 발송 가능하다.
        if(!"".equals(umsSendMsgBean.getALLIMTOLK_TEMPLCODE().trim())) {
            super.checkSendMsg(umsSendMsgBean, varsIndexMap);
        }else{
            throw new RequestErrException(" ALLIMTOLK TEMPLEATE CODE is NULL");
        }

        // STEP 4: 알림톡 발송유저수 추출.
        List<String[]> csvList = reader.readAll();

        Map<String,Object> makeUserResultMap = makeSendUserMap(csvList,varsIndexMap);
        Map<String, List<String>> sendUserObj = (Map<String, List<String>>)makeUserResultMap.get("sendUserObj");
        Map<String,Map<String,String>> sendCuidVarsMap = (Map<String,Map<String,String>>)makeUserResultMap.get("sendCuidVarsMap");

        // STEP 5 : 알림톡 발송
        return super.umsAltCsvSend(sendUserObj,sendCuidVarsMap,null,umsSendMsgBean);
    }

    /**
     * CSV UMS 친구톡 발송
     * @param umsSendMsgBean
     * @return
     * @throws Exception
     */
    public Map<String,Object> umsFrtCsvSend(UmsSendMsgBean umsSendMsgBean) throws Exception{

        //CSV파일 점검.
        Map<String,Object> csvFeturnMap = confirmCsvFile(umsSendMsgBean.getCSVFILE());
        CSVReader reader = (CSVReader)csvFeturnMap.get("csvReader");
        Map<String,Integer> varsIndexMap =  (Map<String,Integer>)csvFeturnMap.get("varIdxMap");

        //STEP 1 : 카톡 친구톡 메세지 검증.
        if(!"".equals(umsSendMsgBean.getFRIENDTOLK_MSG())) {
            super.checkSendMsg(umsSendMsgBean, varsIndexMap);
        }else{
            throw new RequestErrException(" FRIENDTOLK MESSAGE is NULL");
        }

        // STEP 2: 친구톡 발송유저수 추출.
        List<String[]> csvList = reader.readAll();
        Map<String,Object> makeUserResultMap = makeSendUserMap(csvList,varsIndexMap);
        Map<String, List<String>> sendUserObj = (Map<String, List<String>>)makeUserResultMap.get("sendUserObj");
        Map<String,Map<String,String>> sendCuidVarsMap = (Map<String,Map<String,String>>)makeUserResultMap.get("sendCuidVarsMap");

        return super.umsFrtCsvSend(sendUserObj,sendCuidVarsMap,null,umsSendMsgBean);
    }
    
    /**
     * CSV UMS RCS 발송
     * @param umsSendMsgBean
     * @return
     * @throws Exception
     */
    public Map<String,Object> umsRcsCsvSend(UmsSendMsgBean umsSendMsgBean) throws Exception{
    	
        //STEP 1 : CSV파일 점검.
        Map<String, Object> csvFeturnMap = confirmCsvFile(umsSendMsgBean.getCSVFILE());
        CSVReader reader = (CSVReader)csvFeturnMap.get("csvReader");
        Map<String,Integer> varsIndexMap =  (Map<String, Integer>)csvFeturnMap.get("varIdxMap");

        //STEP 2 : RCS은 RBC에서 승인을 받은 등록된 템플릿만 발송 가능
        super.checkSendMsg(umsSendMsgBean, varsIndexMap);
        

        // STEP 3: RCS 발송유저수 추출.
        List<String[]> csvList = reader.readAll();
        Map<String,Object> makeUserResultMap = makeSendUserMap(csvList,varsIndexMap);
        Map<String, List<String>> sendUserObj = (Map<String, List<String>>)makeUserResultMap.get("sendUserObj");
        Map<String,Map<String,String>> sendCuidVarsMap = (Map<String,Map<String,String>>)makeUserResultMap.get("sendCuidVarsMap");

        // STEP 4 : RCS 발송
        return super.umsRcsCsvSend(sendUserObj, sendCuidVarsMap, null, umsSendMsgBean);
    }


    /**
     * CSV UMS SMS 발송
     * @param umsSendMsgBean
     * @return
     * @throws Exception
     */
    public Map<String,Object> umsSmsCsvSend(final UmsSendMsgBean umsSendMsgBean, SendType sendType) throws Exception{

        //CSV파일 점검.
        Map<String,Object> csvFeturnMap = confirmCsvFile(umsSendMsgBean.getCSVFILE());
        CSVReader reader = (CSVReader)csvFeturnMap.get("csvReader");
        Map<String,Integer> varsIndexMap =  (Map<String,Integer>)csvFeturnMap.get("varIdxMap");

        // STEP 1: [SMS 메세지 검증] #{아이디} #{이름} 이외의 변수가 존재 할 경우 에러처리 한다. 개인화 변수가 있으면 우선은 MMS로 발송처리하나.MMS처리 프로세스가 사이즈 체크 후 SMS로 보냄.
        if(!"".equals(umsSendMsgBean.getSMS_MSG())) {
            super.checkSendMsg(umsSendMsgBean, varsIndexMap);
        }else{
            throw new RequestErrException(" SMS MESSAGE is NULL");
        }

        // STEP 3: SMS 발송유저수 추출.
        List<String[]> csvList = reader.readAll();
        Map<String,Object> makeUserResultMap = makeSendUserMap(csvList,varsIndexMap);
        Map<String, List<String>> sendUserObj = (Map<String, List<String>>)makeUserResultMap.get("sendUserObj");
        Map<String,Map<String,String>> sendCuidVarsMap = (Map<String,Map<String,String>>)makeUserResultMap.get("sendCuidVarsMap");


        return super.umsSmsCsvSend(sendUserObj,sendCuidVarsMap,null,umsSendMsgBean,sendType);
    }

    /**
     * CSV UMS비회원 네이버톡 발송
     * @param umsSendMsgBean
     * @return
     * @throws Exception
     */
    public Map<String,Object> umsNaverCsvSend(UmsSendMsgBean umsSendMsgBean) throws Exception{

        //STEP 2 : CSV파일 점검.
        Map<String,Object> csvFeturnMap = confirmCsvFile(umsSendMsgBean.getCSVFILE());
        CSVReader reader = (CSVReader)csvFeturnMap.get("csvReader");
        Map<String,Integer> varsIndexMap =  (Map<String,Integer>)csvFeturnMap.get("varIdxMap");

        //STEP 3: 네이버톡은 승인 받은 등록된 템플릿만 발송 가능하다.
        super.checkSendMsg(umsSendMsgBean, varsIndexMap);

        // STEP 4: 네이버톡 CSV파일에서 발송유저수 추출.
        List<String[]> csvList = reader.readAll();
        Map<String,Object> makeUserResultMap = makeSendUserMap(csvList,varsIndexMap);
        Map<String, List<String>> sendUserObj = (Map<String, List<String>>)makeUserResultMap.get("sendUserObj");
        Map<String,Map<String,String>> sendCuidVarsMap = (Map<String,Map<String,String>>)makeUserResultMap.get("sendCuidVarsMap");

        // STEP 5 : 네이버톡 발송
        return super.umsNaverCsvSend(sendUserObj,sendCuidVarsMap,null,umsSendMsgBean);
    }
}
