package kr.uracle.ums.core.service;

import au.com.bytecode.opencsv.CSVReader;
import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.core.common.TargetUserKind;
import kr.uracle.ums.core.exception.RequestErrException;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Created by Y.B.H(mium2) on 2019. 4. 15..
 */
@Service
@SuppressWarnings("unchecked")
public class UmsCsvMemberSendService extends UmsCsvBase{
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    @Autowired(required=true)
    private MessageSource messageSource;
    @Autowired(required = true)
    private UmsSendCommonService umsSendCommonService;

    @Autowired
    private UmsCsvSendService umsCsvSendService;

    @Value("${UMS.TEMPDIR:}")
    private String TEMPDIR;

    private Set<String> comonVarsSet = new HashSet<String>();
    public UmsCsvMemberSendService(){
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
    public Map<String,Object> umsPushCsvMemberSend(UmsSendMsgBean umsSendMsgBean) throws Exception{
        
        Map<String,Object> returnResultMap = new HashMap<String,Object>();
        //STEP 1 : CSV파일 점검.
        Map<String,Object> csvFeturnMap = super.confirmMemberCsvFile(umsSendMsgBean.getCSVFILE());
        CSVReader reader = (CSVReader)csvFeturnMap.get("csvReader");
        Map<String,Integer> varsIndexMap =  (Map<String,Integer>)csvFeturnMap.get("varIdxMap");

        if(varsIndexMap.containsKey("#{핸드폰번호}")){
            // 휴대폰정보가 있을 경우 비회원 CSV발송으로 처리 한다.
            return umsCsvSendService.umsPushCsvSend(umsSendMsgBean);
        }

        //STEP 2:[ 메세지 검증]
        super.checkSendMsg(umsSendMsgBean, varsIndexMap);

        // STEP 3: 푸시/웹푸시 가입유저, 미가입 유저 분리.
        List<String[]> csvList = reader.readAll();
        Map<String,Object> pushUserChkMap = chkCsvMemberPushUser(csvList, varsIndexMap, umsSendMsgBean);

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
    public Map<String,Object> umsWPushCsvMemberSend(UmsSendMsgBean umsSendMsgBean) throws Exception{

        Map<String,Object> returnResultMap = new HashMap<String,Object>();
        //STEP 1 : CSV파일 점검.
        Map<String,Object> csvFeturnMap = super.confirmMemberCsvFile(umsSendMsgBean.getCSVFILE());
        CSVReader reader = (CSVReader)csvFeturnMap.get("csvReader");
        Map<String,Integer> varsIndexMap =  (Map<String,Integer>)csvFeturnMap.get("varIdxMap");

        if(varsIndexMap.containsKey("#{핸드폰번호}")){
            // 휴대폰정보가 있을 경우 비회원 CSV발송으로 처리 한다.
            return umsCsvSendService.umsWPushCsvSend(umsSendMsgBean);
        }

        //STEP 2:[ 메세지 검증]
        super.checkSendMsg(umsSendMsgBean, varsIndexMap);

        // STEP 3: 푸시/웹푸시 가입유저, 미가입 유저 분리.
        List<String[]> csvList = reader.readAll();
        Map<String,Object> pushUserChkMap = chkCsvMemberPushUser(csvList, varsIndexMap, umsSendMsgBean);

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
    public Map<String,Object> umsAltCsvMemberSend(UmsSendMsgBean umsSendMsgBean) throws Exception{
        
        //STEP 2 : CSV파일 점검.
        Map<String,Object> csvFeturnMap = confirmMemberCsvFile(umsSendMsgBean.getCSVFILE());
        CSVReader reader = (CSVReader)csvFeturnMap.get("csvReader");
        Map<String,Integer> varsIndexMap =  (Map<String,Integer>)csvFeturnMap.get("varIdxMap");

        if(varsIndexMap.containsKey("#{핸드폰번호}")){
            // 휴대폰정보가 있을 경우 비회원 CSV발송으로 처리 한다.
            return umsCsvSendService.umsAltCsvSend(umsSendMsgBean);
        }

        //STEP 3 : 카톡 알림톡은 카톡에서 승인을 받은 등록된 템플릿만 발송 가능하다.
        if(!"".equals(umsSendMsgBean.getALLIMTOLK_TEMPLCODE().trim())) {
            super.checkSendMsg(umsSendMsgBean, varsIndexMap);
        }else{
            throw new RequestErrException(" ALLIMTOLK TEMPLEATE CODE is NULL");
        }

        // STEP 4: 알림톡 발송유저수 추출.
        List<String[]> csvList = reader.readAll();
        Map<String,Object> makeUserResultMap = makeSendMemberUserMap(csvList,varsIndexMap);
        Map<String, List<String>> sendUserObj = (Map<String, List<String>>)makeUserResultMap.get("sendUserObj");
        Map<String,Map<String,String>> sendCuidVarsMap = (Map<String,Map<String,String>>)makeUserResultMap.get("sendCuidVarsMap");
        List<String> finalFailCuid = (List<String>)makeUserResultMap.get("finalFailCuid");

        // STEP 5 : 알림톡 발송
        return super.umsAltCsvSend(sendUserObj,sendCuidVarsMap,finalFailCuid,umsSendMsgBean);
    }

    /**
     * CSV UMS 친구톡 발송
     * @param umsSendMsgBean
     * @return
     * @throws Exception
     */
    public Map<String,Object> umsFrtCsvMemberSend(UmsSendMsgBean umsSendMsgBean) throws Exception{
    	
        // CSV파일 점검
        Map<String,Object> csvFeturnMap = confirmMemberCsvFile(umsSendMsgBean.getCSVFILE());
        CSVReader reader = (CSVReader)csvFeturnMap.get("csvReader");
        Map<String,Integer> varsIndexMap =  (Map<String,Integer>)csvFeturnMap.get("varIdxMap");

        if(varsIndexMap.containsKey("#{핸드폰번호}")){
            // 휴대폰정보가 있을 경우 비회원 CSV발송으로 처리 한다.
            return umsCsvSendService.umsFrtCsvSend(umsSendMsgBean);
        }

        //STEP 1 : 카톡 친구톡 메세지 검증.
        if(!"".equals(umsSendMsgBean.getFRIENDTOLK_MSG())) {
            // 친구톡 메세지 검증 :  #{아이디} #{이름} 이외의 변수가 존재 할 경우 에러처리 한다.
            super.checkSendMsg(umsSendMsgBean, varsIndexMap);
        }else{
            throw new RequestErrException(" FRIENDTOLK MESSAGE is NULL");
        }

        // STEP 4: 친구톡 발송유저수 추출.
        List<String[]> csvList = reader.readAll();
        Map<String,Object> makeUserResultMap = makeSendMemberUserMap(csvList,varsIndexMap);
        Map<String, List<String>> sendUserObj = (Map<String, List<String>>)makeUserResultMap.get("sendUserObj");
        Map<String,Map<String,String>> sendCuidVarsMap = (Map<String,Map<String,String>>)makeUserResultMap.get("sendCuidVarsMap");
        List<String> finalFailCuid = (List<String>)makeUserResultMap.get("finalFailCuid");

        return super.umsFrtCsvSend(sendUserObj,sendCuidVarsMap,finalFailCuid,umsSendMsgBean);
    }
    
    /**
     * CSV UMS RCS 발송
     * @param umsSendMsgBean
     * @return
     * @throws Exception
     */
    public Map<String,Object> umsRCSCsvMemberSend(UmsSendMsgBean umsSendMsgBean) throws Exception{

        //STEP 1 : CSV파일 점검.
        Map<String,Object> csvFeturnMap = confirmMemberCsvFile(umsSendMsgBean.getCSVFILE());
        CSVReader reader = (CSVReader)csvFeturnMap.get("csvReader");
        Map<String,Integer> varsIndexMap =  (Map<String,Integer>)csvFeturnMap.get("varIdxMap");

        // 휴대폰정보가 있을 경우 비회원 CSV발송으로 처리
        if(ObjectUtils.isNotEmpty(varsIndexMap) && varsIndexMap.containsKey("#{핸드폰번호}")){
            return umsCsvSendService.umsRcsCsvSend(umsSendMsgBean);
        }
        
        //STEP 2 : RCS은 RBC에서 승인을 받은 등록된 템플릿만 발송 가능
        super.checkSendMsg(umsSendMsgBean, varsIndexMap);

        // STEP 3: RCS 발송유저수 추출.
        List<String[]> csvList = reader.readAll();
        // 발송 필요 데이터 맵 - 하위 3개
        Map<String,Object> makeUserResultMap = makeSendMemberUserMap(csvList, varsIndexMap);
        // 1. 수신자별 기본 변수(핸드폰번호, 이름)-리스트 정보
        Map<String, List<String>> sendUserObj = (Map<String, List<String>>)makeUserResultMap.get("sendUserObj");
        // 2. 수신자별 개인화 치환변수매핑 맵 정보
        Map<String,Map<String,String>> sendCuidVarsMap = (Map<String,Map<String,String>>)makeUserResultMap.get("sendCuidVarsMap");
        // 3. 실패처리된 수신자 리스트
        List<String> finalFailCuid = (List<String>)makeUserResultMap.get("finalFailCuid");

        // STEP 4 : RCS 발송
        return super.umsRcsCsvSend(sendUserObj, sendCuidVarsMap, finalFailCuid, umsSendMsgBean);
    }    

    /**
     * CSV UMS SMS 발송
     * @param umsSendMsgBean
     * @param locale
     * @return
     * @throws Exception
     */
    public Map<String,Object> umsSmsCsvMemberSend(final UmsSendMsgBean umsSendMsgBean, Locale locale, SendType sendType) throws Exception{
    	
        //CSV파일 점검.
        Map<String,Object> csvFeturnMap = confirmMemberCsvFile(umsSendMsgBean.getCSVFILE());
        CSVReader reader = (CSVReader)csvFeturnMap.get("csvReader");
        Map<String,Integer> varsIndexMap =  (Map<String,Integer>)csvFeturnMap.get("varIdxMap");

        if(varsIndexMap.containsKey("#{핸드폰번호}")){
            // 휴대폰정보가 있을 경우 비회원 CSV발송으로 처리 한다.
            umsSendMsgBean.setTARGET_USER_TYPE(TargetUserKind.NC.toString());
            return umsCsvSendService.umsSmsCsvSend(umsSendMsgBean, sendType);
        }

        // STEP 1: [SMS 메세지 검증] #{아이디} #{이름} 이외의 변수가 존재 할 경우 에러처리 한다. 개인화 변수가 있으면 우선은 MMS로 발송처리하나.MMS처리 프로세스가 사이즈 체크 후 SMS로 보냄.
        if(!"".equals(umsSendMsgBean.getSMS_MSG())) {
            umsSendCommonService.chkSmsMsgCsv(umsSendMsgBean,varsIndexMap);
        }else{
            throw new RequestErrException(" SMS MESSAGE is NULL");
        }

        // STEP 3: SMS 발송유저수 추출.
        List<String[]> csvList = reader.readAll();
        Map<String,Object> makeUserResultMap = makeSendMemberUserMap(csvList,varsIndexMap);
        Map<String, List<String>> sendUserObj = (Map<String, List<String>>)makeUserResultMap.get("sendUserObj");
        Map<String,Map<String,String>> sendCuidVarsMap = (Map<String,Map<String,String>>)makeUserResultMap.get("sendCuidVarsMap");
        List<String> finalFailCuid = (List<String>)makeUserResultMap.get("finalFailCuid");

        return super.umsSmsCsvSend(sendUserObj,sendCuidVarsMap,finalFailCuid,umsSendMsgBean,sendType);
    }

    /**
     * CSV UMS회원 네이버톡 발송
     * @param umsSendMsgBean
     * @return
     * @throws Exception
     */
    public Map<String,Object> umsNaverCsvMemberSend(UmsSendMsgBean umsSendMsgBean) throws Exception{

        //STEP 2 : CSV파일 점검.
        Map<String,Object> csvFeturnMap = confirmMemberCsvFile(umsSendMsgBean.getCSVFILE());
        CSVReader reader = (CSVReader)csvFeturnMap.get("csvReader");
        Map<String,Integer> varsIndexMap =  (Map<String,Integer>)csvFeturnMap.get("varIdxMap");

        if(varsIndexMap.containsKey("#{핸드폰번호}")){
            // 휴대폰정보가 있을 경우 비회원 CSV발송으로 처리 한다.
            return umsCsvSendService.umsNaverCsvSend(umsSendMsgBean);
        }

        //STEP 3 : 네이버톡은 승인을 받은 등록된 템플릿만 발송 가능하다.
        super.checkSendMsg(umsSendMsgBean, varsIndexMap);

        // STEP 4: 네이버톡 발송유저수 추출.
        List<String[]> csvList = reader.readAll();
        Map<String,Object> makeUserResultMap = makeSendMemberUserMap(csvList,varsIndexMap);
        Map<String, List<String>> sendUserObj = (Map<String, List<String>>)makeUserResultMap.get("sendUserObj");
        Map<String,Map<String,String>> sendCuidVarsMap = (Map<String,Map<String,String>>)makeUserResultMap.get("sendCuidVarsMap");
        List<String> finalFailCuid = (List<String>)makeUserResultMap.get("finalFailCuid");

        // STEP 5 : 네이버톡 CSV발송
        return super.umsNaverCsvSend(sendUserObj,sendCuidVarsMap,finalFailCuid,umsSendMsgBean);
    }
}
