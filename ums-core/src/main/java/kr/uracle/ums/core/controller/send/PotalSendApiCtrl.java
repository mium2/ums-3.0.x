package kr.uracle.ums.core.controller.send;

import kr.uracle.ums.codec.redis.config.ErrorManager;
import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.core.common.Constants;
import kr.uracle.ums.core.core.AuthIPCheckManager;
import kr.uracle.ums.core.ehcache.AltTemplateCacheMgr;
import kr.uracle.ums.core.ehcache.CommonTemplateCacheMgr;
import kr.uracle.ums.core.ehcache.NaverTemplateCacheMgr;
import kr.uracle.ums.core.ehcache.RCSTemplateCache;
import kr.uracle.ums.core.exception.LicenseException;
import kr.uracle.ums.core.exception.NotExistUserException;
import kr.uracle.ums.core.exception.RequestErrException;
import kr.uracle.ums.core.exception.ValidationException;
import kr.uracle.ums.core.service.UmsSendCommonService;
import kr.uracle.ums.core.service.UmsSendMacroService;
import kr.uracle.ums.core.vo.ReqUmsSendVo;
import kr.uracle.ums.core.vo.template.AltTemplateBaseVo;
import kr.uracle.ums.core.vo.template.CommonTemplateVo;
import kr.uracle.ums.core.vo.template.NaverTemplateVo;
import kr.uracle.ums.core.vo.template.RCSTemplateVo;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

@RequestMapping(value="/api/send")
@Controller
public class PotalSendApiCtrl {
    protected Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    @Autowired(required = true)
    private PushSend pushSend;
    @Autowired(required = true)
    private WPushSend wpushSend;
    @Autowired(required = true)
    private KkoAltSend kkoAltSend;
    @Autowired(required = true)
    private KkoFrtSend kkoFrtSend;
    @Autowired(required = true)
    private RcsSend rcsSend;
    @Autowired(required = true)
    private SmsSend smsSend;
    @Autowired(required = true)
    private NaverSend naverSend;
    @Autowired(required = true)
    private UmsSendMacroService umsSendMacroService;
    @Autowired(required = true)
    private UmsSendCommonService umsSendCommonService;
    @Autowired(required = true)
    private CommonTemplateCacheMgr commonTemplateCacheMgr;
    @Autowired(required = true)
    private AltTemplateCacheMgr altTemplateCacheMgr;
    @Autowired(required = true)
    private NaverTemplateCacheMgr naverTemplateCacheMgr;
    @Autowired(required = true)
    private RCSTemplateCache rcsTemplateCache;


    @RequestMapping(value = {"/potalSendApi.ums"},produces = "application/json; charset=utf8")
    public @ResponseBody String umsSend(Locale locale, HttpServletRequest request, HttpServletResponse response, @ModelAttribute ReqUmsSendVo reqUmsSendVo){

    	List<Map<String,Object>> responseBodyList = new ArrayList<Map<String,Object>>(5);
    	
        String headerMessage = "";
        String responseBody = "";
        
        try {
        	
        	//접근이 허용되지 않은 아이피일경우 실패처리함.
            if(!AuthIPCheckManager.getInstance().isAllowIp(request.getRemoteAddr())){
                String ERRMSG = ErrorManager.getInstance().getMsg(ErrorManager.ERR_9404);
                logger.error("!! [UMS AUTH CHECK] Unauthorized error]: {}",ERRMSG);
                return umsSendCommonService.responseJsonString(ErrorManager.ERR_9404, ERRMSG, request.getRequestURI(), reqUmsSendVo);
            }

            //STEP1 : 공통 - 필수 파라미터 체크 : 발송자ID, 발송타켓 유형
            if(StringUtils.isBlank(reqUmsSendVo.getSENDERID()) || StringUtils.isBlank(reqUmsSendVo.getTARGET_USER_TYPE()) ||  StringUtils.isBlank(reqUmsSendVo.getREQ_TRAN_TYPE())){
            	headerMessage =  ErrorManager.getInstance().getMsg(ErrorManager.ERR_1001)+", [SENDERID,TARGET_USER_TYPE,REQ_TRAN_TYPE]";
                return umsSendCommonService.responseJsonString(ErrorManager.ERR_1001, headerMessage, request.getRequestURI(), reqUmsSendVo);
            }

            // SendType 타입셋팅. (BATCH or REAL)
            reqUmsSendVo.setTRANS_TYPE(TransType.valueOf(reqUmsSendVo.getREQ_TRAN_TYPE()));
            
            if(!Constants.TARGET_USER_TYPE_SET.contains(reqUmsSendVo.getTARGET_USER_TYPE())){
            	headerMessage = ErrorManager.getInstance().getMsg(ErrorManager.ERR_1001)+", TARGET_USER_TYPE이 올바르지 않습니다. AU:전체UMS회원, AP:전체푸시유저, OU:전체조직도유저, MU:UMS 아이디로 타겟팅, MP:푸시아이디로 타겟팅, NM:비회원/핸드폰번호필수, MC:회원CSV, NC:비회원CSV" ;
                return umsSendCommonService.responseJsonString(ErrorManager.ERR_1001, headerMessage,  request.getRequestURI(), reqUmsSendVo);
            }

            // UMSAgent에서 처리한 예약발송일 경우 저장된 CSV파일을 읽어들여 다시 MultipartFile 파일로 만든다.
            if(StringUtils.isNotBlank(reqUmsSendVo.getRESERVE_CSVFILE())) {
                File file = new File(reqUmsSendVo.getRESERVE_CSVFILE());
                FileItem fileItem = new DiskFileItem("mainFile", "text/plain", false, file.getName(), (int) file.length(), file.getParentFile());
                InputStream input = new FileInputStream(file);
                OutputStream os = fileItem.getOutputStream();
                IOUtils.copy(input, os);
                MultipartFile csvFile = new CommonsMultipartFile(fileItem);
                reqUmsSendVo.setCSVFILE(csvFile);
                // 예약발송시 저장해둔 CSV파일 삭제
                try {
                    FileUtils.forceDelete(file);
                }catch (Exception ex){}
            }
            
            //발송그룹코드가 비어있다면, 발송ID로 지정
            if(StringUtils.isBlank(reqUmsSendVo.getSENDGROUPCODE())){
                reqUmsSendVo.setSENDGROUPCODE(reqUmsSendVo.getSENDERID());
            }

            //STEP2 : 템플릿코드로 만 발송하는 채널이 있을 경우 템플릿코드를 이용한 메세지정보를 셋팅한다.
            chkTemplateCodeSend(reqUmsSendVo);

            //네이버 발송요청시 필수값 체크
            if(!"".equals(reqUmsSendVo.getNAVER_MSG())){
                if(StringUtils.isBlank(reqUmsSendVo.getNAVER_PROFILE())) throw new ValidationException("[네이버 발송 프로필 정보가 없음.]", locale);
                if(StringUtils.isBlank(reqUmsSendVo.getNAVER_PARTNERKEY())) throw new ValidationException("[네이버 발송 파트너키(NAVER_PARTNERKEY)가 없음.]", locale);
            }

            // 매크로설정의 의해 최초발송 채널 정보 가져옴
            if(StringUtils.isBlank(reqUmsSendVo.getSEND_MACRO_CODE()))reqUmsSendVo.setSEND_MACRO_CODE("MACRO_001");
            Set<SendType> sendTypeSet = umsSendMacroService.getFirstSendChannel(reqUmsSendVo);
            // 발송할 채널이 존재하지 않는다는 에러처리
            if(sendTypeSet.size()==0){
            	headerMessage = ErrorManager.getInstance().getMsg(ErrorManager.ERR_1200)+":";
                return umsSendCommonService.responseJsonString(ErrorManager.ERR_1200, headerMessage, "/potalSendApi.ums",reqUmsSendVo);
            }
            
            // 최초발송 타입별 발송처리
            for(SendType sendType : sendTypeSet){
                // 첫번째 보낼 채널(SentType)의 서비스클래스를 할당한다.
            	BaseSend baseSend = getChannelSend(sendType);
                if(baseSend == null)  {
                	headerMessage = ErrorManager.getInstance().getMsg(ErrorManager.ERR_1200)+":";
                	return umsSendCommonService.responseJsonString(ErrorManager.ERR_1200, headerMessage, "/potalSendApi.ums", reqUmsSendVo);
                }
                reqUmsSendVo.setSEND_TYPE(sendType);//발송채널 셋팅
                Map<String, Object>  reponseBodyMap = baseSend.send(reqUmsSendVo, request.getRequestURI(), locale);
                responseBodyList.add(reponseBodyMap);
            }
            headerMessage =  ErrorManager.getInstance().getMsg(ErrorManager.SUCCESS);
            responseBody = umsSendCommonService.responseJsonString(ErrorManager.SUCCESS, headerMessage, responseBodyList, request.getRequestURI(), reqUmsSendVo);

        }catch (LicenseException e){
        	return umsSendCommonService.responseJsonString(ErrorManager.ERR_1100, ErrorManager.getInstance().getMsg(ErrorManager.ERR_1100), request.getRequestURI(), reqUmsSendVo);
        }catch (ValidationException e){
        	return umsSendCommonService.responseJsonString(ErrorManager.ERR_1002, ErrorManager.getInstance().getMsg(ErrorManager.ERR_1002) +"["+e.getMessage()+"]", request.getRequestURI(), reqUmsSendVo);
        }catch (NotExistUserException e){
        	return umsSendCommonService.responseJsonString(ErrorManager.ERR_4400, ErrorManager.getInstance().getMsg(ErrorManager.ERR_4400)+", "+e.getMessage(), request.getRequestURI(), reqUmsSendVo);
        }catch (RequestErrException e){
        	return umsSendCommonService.responseJsonString(ErrorManager.ERR_1002, ErrorManager.getInstance().getMsg(ErrorManager.ERR_1002)+", "+e.getMessage(), request.getRequestURI(), reqUmsSendVo);
        } catch (Exception e) {
            e.printStackTrace();
            return umsSendCommonService.responseJsonString(ErrorManager.ERR_500, ErrorManager.getInstance().getMsg(ErrorManager.ERR_500)+", "+e.getMessage(), request.getRequestURI(), reqUmsSendVo);
        }
        return responseBody;
    }
    
    // BaseSend 채널에따른  인스턴스 초기화
    private BaseSend getChannelSend(SendType sendType) {
    	BaseSend sendService =null;
    	  switch (sendType){
          case PUSH :
          	sendService = pushSend;
              break;
          case WPUSH :
              sendService = wpushSend;
              break;
          case KKOALT:
          	sendService = kkoAltSend;
              break;
          case KKOFRT:
          	sendService = kkoFrtSend;
              break;
          case RCS_SMS: case RCS_LMS: case RCS_MMS: case RCS_FREE: case RCS_CELL: case RCS_DESC:
        	  sendService = rcsSend;        	 
              break;
          case SMS: case LMS: case MMS:
          	sendService = smsSend;
              break;
          case NAVERT:
              sendService = naverSend;
              break;
          default :
        	  break;
      }
    	return sendService;
    }


    private void chkTemplateCodeSend(ReqUmsSendVo reqUmsSendVo){
        //STEP 1 : [푸시] 푸시메세지 없이 PUSH_TEMPL_ID만으로 발송시 체크하여 메세지 셋팅 - 보낸 내용 우선
        if(StringUtils.isBlank(reqUmsSendVo.getPUSH_MSG()) && !StringUtils.isBlank(reqUmsSendVo.getPUSH_TEMPL_ID())){
            CommonTemplateVo commonTemplateVo = commonTemplateCacheMgr.getTemplate(reqUmsSendVo.getPUSH_TEMPL_ID());
            if(commonTemplateVo!=null){
                if (StringUtils.isBlank(reqUmsSendVo.getTITLE()) && StringUtils.isNotBlank(commonTemplateVo.getTITLE())) {
                    reqUmsSendVo.setTITLE(commonTemplateVo.getTITLE());
                }
                if (StringUtils.isBlank(reqUmsSendVo.getPUSH_MSG()) && StringUtils.isNotBlank(commonTemplateVo.getTEMPL_CONT())) {
                    reqUmsSendVo.setPUSH_MSG(commonTemplateVo.getTEMPL_CONT());
                }
            }else{
                logger.warn("푸시 요청한 템플릿코드로 DB 저장된 템플릿이 존재하지 않습니다. "+reqUmsSendVo.getPUSH_TEMPL_ID());
            }
        }

        //STEP 2 : [웹푸시] 웹푸시메세지 없이 WPUSH_TEMPL_ID만으로 발송시 체크하여 메세지 셋팅 - 보낸 내용 우선
        if(StringUtils.isBlank(reqUmsSendVo.getWPUSH_MSG()) && !StringUtils.isBlank(reqUmsSendVo.getWPUSH_TEMPL_ID())){
            CommonTemplateVo commonTemplateVo = commonTemplateCacheMgr.getTemplate(reqUmsSendVo.getWPUSH_TEMPL_ID());
            if(commonTemplateVo!=null){
                if (StringUtils.isBlank(reqUmsSendVo.getWPUSH_TITLE()) && StringUtils.isNotBlank(commonTemplateVo.getTITLE())) {
                    reqUmsSendVo.setWPUSH_TITLE(commonTemplateVo.getTITLE());
                }
                if (StringUtils.isBlank(reqUmsSendVo.getWPUSH_MSG()) && StringUtils.isNotBlank(commonTemplateVo.getTEMPL_CONT())) {
                    reqUmsSendVo.setWPUSH_MSG(commonTemplateVo.getTEMPL_CONT());
                }
            }else{
                logger.warn("웹푸시 요청한 템플릿코드로 DB 저장된 템플릿이 존재하지 않습니다. "+reqUmsSendVo.getWPUSH_TEMPL_ID());
            }
        }

        //STEP 3 : [알림톡] ALLIMTOLK_TEMPLCODE 만 으로 발송시 체크하여 메세지 셋팅 - 보낸 내용 우선
        if(!StringUtils.isBlank(reqUmsSendVo.getALLIMTOLK_TEMPLCODE())) {
            AltTemplateBaseVo altTemplateBaseVo = altTemplateCacheMgr.getTemplate(reqUmsSendVo.getALLIMTOLK_TEMPLCODE());
            if (altTemplateBaseVo != null) {
                if (StringUtils.isBlank(reqUmsSendVo.getALLIMTALK_MSG()) && StringUtils.isNotBlank(altTemplateBaseVo.getTEMPLATECONTENTS())) {
                    reqUmsSendVo.setALLIMTALK_MSG(altTemplateBaseVo.getTEMPLATECONTENTS());
                }
                if (StringUtils.isBlank(reqUmsSendVo.getKKO_BTNS()) && StringUtils.isNotBlank(altTemplateBaseVo.getBUTTONS())) {
                    reqUmsSendVo.setKKO_BTNS(altTemplateBaseVo.getBUTTONS());
                }
                // 알림톡 강조형 제목 셋팅.
//                if (StringUtils.isBlank(reqUmsSendVo.getKKO_TITLE()) && StringUtils.isNotBlank(altTemplateBaseVo.getTITLE())) {
//                    reqUmsSendVo.setKKO_TITLE(altTemplateBaseVo.getTITLE());
//                }
                if (StringUtils.isNotBlank(altTemplateBaseVo.getIMAGE())) {
                    reqUmsSendVo.setKKO_IMGFILE_PATH(altTemplateBaseVo.getIMAGE());
                }
                
            } else {
                logger.warn("알림톡 요청한 템플릿코드로 DB 저장된 템플릿이 존재하지 않습니다. " + reqUmsSendVo.getALLIMTOLK_TEMPLCODE());
            }
        }


        //STEP 4 : [친구톡] 친구톡메세지 없이 FRT_TEMPL_ID만으로 발송시 체크하여 메세지 셋팅
        if(StringUtils.isBlank(reqUmsSendVo.getFRIENDTOLK_MSG()) && !StringUtils.isBlank(reqUmsSendVo.getFRT_TEMPL_ID())){
            CommonTemplateVo commonTemplateVo = commonTemplateCacheMgr.getTemplate(reqUmsSendVo.getFRT_TEMPL_ID());
            if(commonTemplateVo!=null){
                reqUmsSendVo.setFRIENDTOLK_MSG(commonTemplateVo.getTEMPL_CONT());
                if(StringUtils.isNotBlank(reqUmsSendVo.getKKO_BTNS()) && StringUtils.isNotBlank(commonTemplateVo.getBUTTONS())){
                    reqUmsSendVo.setKKO_BTNS(commonTemplateVo.getBUTTONS());
                }
                if(StringUtils.isBlank(reqUmsSendVo.getPLUS_ID()) && StringUtils.isNotBlank(commonTemplateVo.getPLUS_ID())){
                    reqUmsSendVo.setPLUS_ID(commonTemplateVo.getPLUS_ID());
                }
                
                //공통 템플릿 테이블 URL FILE 두개 존재 시 URL을 우선함 멀티파티파일 객체가 있으면 UmsMessageBean 조립 시 우선적으로 사용 함
                if(StringUtils.isBlank(reqUmsSendVo.getKKO_IMGFILE_PATH())){
                    if(StringUtils.isNotBlank(commonTemplateVo.getIMAGE_URL())){
                        reqUmsSendVo.setKKO_IMGFILE_PATH(commonTemplateVo.getIMAGE_URL());
                    }
                }
                if(StringUtils.isBlank(reqUmsSendVo.getKKO_IMGFILE_PATH())){
                    if(StringUtils.isNotBlank(commonTemplateVo.getIMAGE_FILE())){
                        reqUmsSendVo.setKKO_IMGFILE_PATH(commonTemplateVo.getIMAGE_FILE());
                    }
                }
            }else{
                logger.warn("친구톡 요청한 템플릿코드로 DB 저장된 템플릿이 존재하지 않습니다. "+reqUmsSendVo.getFRT_TEMPL_ID());
            }
        }

        //STEP 5 : [SMS] SMS메세지 없이 SMS_TEMPL_ID만으로 발송시 체크하여 메세지 셋팅
        if(StringUtils.isBlank(reqUmsSendVo.getSMS_MSG()) && !StringUtils.isBlank(reqUmsSendVo.getSMS_TEMPL_ID())){
            CommonTemplateVo commonTemplateVo = commonTemplateCacheMgr.getTemplate(reqUmsSendVo.getSMS_TEMPL_ID());
            if(commonTemplateVo!=null){
                reqUmsSendVo.setSMS_TITLE(commonTemplateVo.getTITLE());
                reqUmsSendVo.setSMS_MSG(commonTemplateVo.getTEMPL_CONT());
            }else{
                logger.warn("SMS 요청한 템플릿코드로 DB 저장된 템플릿이 존재하지 않습니다. "+reqUmsSendVo.getSMS_TEMPL_ID());
            }
        }

        // STEP 6 :[RCS] 템플릿ID 여부에 따른 RCS 필드값 값 처리
        String templtId = reqUmsSendVo.getRCS_TEMPL_ID();
        if(StringUtils.isNotBlank(templtId)) {
            RCSTemplateVo rcsTemplateVo = rcsTemplateCache.getTemplate(templtId);
            if(rcsTemplateVo!=null) {
                if (StringUtils.isBlank(reqUmsSendVo.getRCS_TITLE()) && StringUtils.isNotBlank(rcsTemplateVo.getTITLE())){
                    reqUmsSendVo.setRCS_TITLE(rcsTemplateVo.getTITLE());
                }
                
                if (StringUtils.isBlank(reqUmsSendVo.getRCS_MSG()) && StringUtils.isBlank(reqUmsSendVo.getRCS_OBJECT())){
                    if(StringUtils.isNotBlank(rcsTemplateVo.getCONTENT()))reqUmsSendVo.setRCS_MSG(rcsTemplateVo.getCONTENT());
                }
                
                if (StringUtils.isBlank(reqUmsSendVo.getBTN_OBJECT())  && StringUtils.isNotBlank(rcsTemplateVo.getBUTTONS())){
                    reqUmsSendVo.setBTN_OBJECT(rcsTemplateVo.getBUTTONS());
                }
            }else {
                logger.warn("RCS 요청한 템플릿코드로 DB 저장된 템플릿이 존재하지 않습니다. "+templtId);
            }
        }

        //STEP 7 : [네이버톡] NAVER_TEMPL_ID 만 으로 발송시 체크하여 메세지 셋팅
        if(StringUtils.isBlank(reqUmsSendVo.getNAVER_MSG()) && !StringUtils.isBlank(reqUmsSendVo.getNAVER_TEMPL_ID())){
            NaverTemplateVo naverTemplateVo = naverTemplateCacheMgr.getTemplate(reqUmsSendVo.getNAVER_TEMPL_ID());
            if(naverTemplateVo!=null){
                reqUmsSendVo.setNAVER_MSG(naverTemplateVo.getMSG());
                if(StringUtils.isNotBlank(naverTemplateVo.getBUTTONS())){
                    reqUmsSendVo.setNAVER_BUTTONS(naverTemplateVo.getBUTTONS());
                }
                if(StringUtils.isBlank(reqUmsSendVo.getNAVER_PROFILE())){
                    reqUmsSendVo.setNAVER_PROFILE(naverTemplateVo.getNAVER_PROFILE());
                }
                if(StringUtils.isBlank(reqUmsSendVo.getNAVER_PARTNERKEY())){
                    reqUmsSendVo.setNAVER_PARTNERKEY(naverTemplateVo.getPARTNERKEY());
                }
                if(StringUtils.isBlank(reqUmsSendVo.getNAVER_IMGHASH())){
                    reqUmsSendVo.setNAVER_IMGHASH(naverTemplateVo.getIMGHASH());
                }
            }else{
                logger.warn("네이버톡 요청한 템플릿코드로 DB 저장된 템플릿이 존재하지 않습니다. "+reqUmsSendVo.getNAVER_TEMPL_ID());
            }
        }

    }
}
