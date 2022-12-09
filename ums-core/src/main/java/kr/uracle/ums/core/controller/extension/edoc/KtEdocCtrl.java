package kr.uracle.ums.core.controller.extension.edoc;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import kr.uracle.ums.core.core.AuthIPCheckManager;
import kr.uracle.ums.core.processor.edoc.EdocDbMgr;
import kr.uracle.ums.core.processor.edoc.EdocProcessBean;
import kr.uracle.ums.core.util.httppoolclient.HttpPoolClient;
import kr.uracle.ums.core.util.httppoolclient.ResponseBean;
import kr.uracle.ums.core.vo.extension.edoc.KtEdocSendResultDetailVo;
import kr.uracle.ums.core.vo.extension.edoc.KtEdocSendResultResponseVo;
import kr.uracle.ums.core.vo.extension.edoc.KtEdocSendResultVo;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.*;

@RequestMapping(value="/api/extension/edoc")
@Controller
public class KtEdocCtrl {
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    
    @Autowired(required = true)
    private EdocDbMgr edocDbMgr;
    
    @Autowired
    private Gson gson;

    @Value("${KT.EDOC.HOST:http://172.16.1.182:10210/ONLWeb/api/message/main/send}")
    private String API_HOST;

    public static Map<String,String> MSG_TYPE_MAP = new HashMap<String, String>();
    static {
        MSG_TYPE_MAP.put("1", "LMS");
        MSG_TYPE_MAP.put("2", "MMS");
        MSG_TYPE_MAP.put("3", "HYBRID_LMS");
        MSG_TYPE_MAP.put("4", "HYBRID_MMS");
        MSG_TYPE_MAP.put("5", "RCS");
        MSG_TYPE_MAP.put("6", "HYBRID_RCS");
    }
    
    public static final String SUCCESS_CODE = "40";
    
    private final RequestConfig REQUEST_CONFIG = RequestConfig.custom().setConnectTimeout(5000).setSocketTimeout(20 * 1000).build();

    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

    @RequestMapping(value = {"/ktEdocSend.ums"}, produces = "application/json; charset=utf8")
    public @ResponseBody String kkoEdocSend(HttpServletRequest request, HttpServletResponse response, @RequestBody Map<String, Object> requestBodyMap) {
        try {
            //접근이 허용되지 않은 아이피일경우 실패처리함.
            if(!AuthIPCheckManager.getInstance().isAllowIp(request.getRemoteAddr())){
                response.setStatus(401);
                return "{\"errors\": [{\"errors\":\"not_allowed_ip\" }] }";
            }
            
            String client_id = request.getHeader("client-id");
            String client_tp = request.getHeader("client-tp");
            String accessToken = request.getHeader("Authorization");

            if(StringUtils.isBlank(client_id)){
                response.setStatus(401);
                return "{\"errors\": [{\"errors\":\"client-id 헤더 값 누락\" }] }";
            }
            if(StringUtils.isBlank(client_tp)){
                response.setStatus(401);
                return "{\"errors\": [{\"errors\":\"client-tp 헤더 값 누락\" }] }";
            }
            if(StringUtils.isBlank(accessToken)){
                response.setStatus(401);
                return "{\"errors\": [{\"errors\":\"Authorization 토큰 누락\" }] }";
            }
            
            if(requestBodyMap.get("requestTranId") == null){
                response.setStatus(400);
                return "{\"errors\": [{\"errors\":\"invalid_param [requestTranId]\" }] }";
            }
            
            String requestTranId = requestBodyMap.get("requestTranId").toString();
            
            Map<String, String> httpHeadParam = new HashMap<String, String>(4);
            httpHeadParam.put("Content-Type","application/json");
            httpHeadParam.put("client-id", client_id.trim());
            httpHeadParam.put("client-tp", client_tp.trim());
            httpHeadParam.put("Authorization", accessToken.trim());
            
            // API 정의서의 포함되지 않은 값을 넘길경우 에러처리가 날수 있어 해당 정보는 삭제 후 넘김.
            requestBodyMap.remove("requestTranId"); 
            
            try {
                ResponseBean responseBean = HttpPoolClient.getInstance().sendJsonPost(API_HOST, httpHeadParam, requestBodyMap, REQUEST_CONFIG);
                int httpStatusCode = responseBean.getStatusCode();
                String responseBody = responseBean.getBody();
                
                response.setStatus(httpStatusCode);
                if (httpStatusCode == 200 || httpStatusCode == 201) {
                    logger.debug("받은정보 :" + responseBody);
                    putEdocDbResult(requestBodyMap, requestTranId, responseBody, httpStatusCode);
                } else {
                    logger.error("KT 고지서발송요청 http response code: " + httpStatusCode + " http body:" + responseBody);
                    putEdocDbResult(requestBodyMap, requestTranId, responseBody, httpStatusCode);
                }
                
                return responseBean.getBody();
            }catch (Exception e){
                logger.error(e.getMessage());
                putEdocDbResult(requestBodyMap, requestTranId, e.getMessage(), 401);
                response.setStatus(401);
                return "{\"errors\": [{\"errors\":\""+e.getMessage()+"\" }] }";
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
            response.setStatus(500);
            return "{\"errors\": [{\"errors\":\""+e.getMessage()+"\" }] }";
        }
    }


    private void putEdocDbResult(Map<String,Object> requestBodyMap, String requestTranId, String responseBody, int httpStatusCode){
        EdocProcessBean edocProcessBean = new EdocProcessBean();
        edocProcessBean.setPROVIDER("KT");
        edocProcessBean.setREQUEST_TRANID(requestTranId);
        
        String msgCode = requestBodyMap.get("m_type")==null?"":requestBodyMap.get("m_type").toString();
        edocProcessBean.setMSGTYPE(MSG_TYPE_MAP.get(msgCode));
        
        // 1:RCS, 2:MMS
        String msgType = requestBodyMap.get("msg_type")==null?"2":requestBodyMap.get("msg_type").toString();
        Object datas = requestBodyMap.get("reqs");
        if(datas == null){
            logger.warn("대상자 정보가 없음으로 DB 이력 저장 SKIP");
            return;
        }
        
        String errorCode = ""+httpStatusCode;
        String resultMessage = null;
        if(httpStatusCode == 200 || httpStatusCode == 201){
            try {
                Map<String, Object> respMap = gson.fromJson(responseBody, new TypeToken<Map<String, Object>>(){}.getType());
                errorCode = respMap.get("result_cd")==null?errorCode:respMap.get("result_cd").toString();
                if(errorCode.equals("00")){
                    resultMessage ="발송 성공";
                }else{
                    resultMessage = respMap.get("errors") ==null?"발송 실패":respMap.get("errors").toString().substring(0, 1000);
                }
            }catch (Exception e){
                errorCode = "401";
                resultMessage = responseBody.substring(0, 1000);
            }
        }else{
            resultMessage = responseBody.substring(0, 1000);
        }
        
        List<Map<String, String>> reqs = (List<Map<String, String>>)datas;
        for(Map<String, String> data : reqs){
            String transKey = data.get("src_key");
            String ci = data.get("Ci");
            String msg = msgType.equals("2")?data.get("mms_dtl_cnts"):data.get("rcs_dtl_cnts");
            if(StringUtils.isBlank(transKey) || StringUtils.isBlank(ci)){
                logger.warn("DB 이력 등록 중 필수 값 누락으로 등록 스킵, 대상 데이터 :"+data.toString());
                continue;
            }
            edocProcessBean.setDOCID(transKey);
            edocProcessBean.setCI(ci);
            edocProcessBean.setSENDMSG(msg);
            edocProcessBean.setRESULTCODE(errorCode);
            edocProcessBean.setRESULTMSG(resultMessage);
            edocDbMgr.putWork(edocProcessBean);
        }
    }

    @RequestMapping(value = {"/report.ums"},produces = "application/json; charset=utf8")
    public @ResponseBody String umsSend(HttpServletRequest request, HttpServletResponse response, @ModelAttribute KtEdocSendResultVo sendResultVo){
        String yyyymmddHHMMSS = DATE_FORMAT.format(new Date());
        KtEdocSendResultResponseVo responseVo = new KtEdocSendResultResponseVo();
        responseVo.setResult_dt(yyyymmddHHMMSS);
        
        try{
            String service_cd = sendResultVo.getService_cd();
            // 서비스 코드 이상 시 정상 응답 전송 후 스킵
            if(StringUtils.isBlank(service_cd)){
                logger.warn("수신결과전송 데이터의 필수 값 누락 [service_cd],\n요청 전문 내용:"+sendResultVo.toString());
                return gson.toJson(responseVo);
            }

            // 발송 요청 메시지 구분 시 정상 응답 전송 후 스킵
            String req_msg_type_dvcd = sendResultVo.getReq_msg_type_dvcd();
            if(StringUtils.isBlank(service_cd)){
                logger.warn("수신결과전송 데이터의 필수 값 누락 [req_msg_type_dvcd],\n요청 전문 내용:"+sendResultVo.toString());
                return gson.toJson(responseVo);
            }

            List<KtEdocSendResultDetailVo> datas = sendResultVo.getReqs();
            if(ObjectUtils.isEmpty(datas)){
                logger.warn("수신결과전송 데이터의 필수 값 누락 [reqs],\n요청 전문 내용:"+sendResultVo.toString());
                return gson.toJson(responseVo);
            }

            for(KtEdocSendResultDetailVo detailVo : datas){
                String transKey = detailVo.getSrc_key();
                if(StringUtils.isBlank(transKey)){
                    logger.warn("수신결과전송 데이터의 reqs 아이템 중 필수 값 누락 [src_key],\n해당 아이템 내용:"+detailVo.toString());
                    continue;
                }
                String errorCode = detailVo.getMms_sndg_rslt_dvcd();
                if(StringUtils.isBlank(errorCode)){
                    logger.warn("수신결과전송 데이터의 reqs 아이템 중 필수 값 누락 [mms_sndg_rslt_dvcd],\n해당 아이템 내용:"+detailVo.toString());
                    continue;
                }
                EdocProcessBean edocProcessBean = new EdocProcessBean();
                edocProcessBean.setQUERY_TYPE("UPDATE");
                edocProcessBean.setPROVIDER("KT");
                edocProcessBean.setDOCID(transKey);
                edocProcessBean.setRESULTCODE(errorCode);
                if(errorCode.equals(SUCCESS_CODE)){
                    edocProcessBean.setRESULTMSG("수신성공");
                }else{
                    edocProcessBean.setRESULTMSG("수신실패(코드표참조)");
                }
                edocDbMgr.putWork(edocProcessBean);
            }

            yyyymmddHHMMSS = DATE_FORMAT.format(new Date());
            responseVo.setResult_dt(yyyymmddHHMMSS);
            return gson.toJson(responseVo);
        }catch (Exception e){
            e.printStackTrace();
            responseVo.setResult_cd("01");
            
            List<Map<String, String>> errors = new ArrayList<Map<String, String>>(1);
            Map<String, String> msgMap = new HashMap<String, String>(1);
            msgMap.put("error_msg", "발송/수신 결과 처리 중 에러 발생");
            errors.add(msgMap);
            
            responseVo.setErrors(errors);
            return gson.toJson(responseVo);
        }
      
    }

}
