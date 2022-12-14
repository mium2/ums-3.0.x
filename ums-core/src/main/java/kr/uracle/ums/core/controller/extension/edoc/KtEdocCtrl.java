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
            //????????? ???????????? ?????? ?????????????????? ???????????????.
            if(!AuthIPCheckManager.getInstance().isAllowIp(request.getRemoteAddr())){
                response.setStatus(401);
                return "{\"errors\": [{\"errors\":\"not_allowed_ip\" }] }";
            }
            
            String client_id = request.getHeader("client-id");
            String client_tp = request.getHeader("client-tp");
            String accessToken = request.getHeader("Authorization");

            if(StringUtils.isBlank(client_id)){
                response.setStatus(401);
                return "{\"errors\": [{\"errors\":\"client-id ?????? ??? ??????\" }] }";
            }
            if(StringUtils.isBlank(client_tp)){
                response.setStatus(401);
                return "{\"errors\": [{\"errors\":\"client-tp ?????? ??? ??????\" }] }";
            }
            if(StringUtils.isBlank(accessToken)){
                response.setStatus(401);
                return "{\"errors\": [{\"errors\":\"Authorization ?????? ??????\" }] }";
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
            
            // API ???????????? ???????????? ?????? ?????? ???????????? ??????????????? ?????? ?????? ?????? ????????? ?????? ??? ??????.
            requestBodyMap.remove("requestTranId"); 
            
            try {
                ResponseBean responseBean = HttpPoolClient.getInstance().sendJsonPost(API_HOST, httpHeadParam, requestBodyMap, REQUEST_CONFIG);
                int httpStatusCode = responseBean.getStatusCode();
                String responseBody = responseBean.getBody();
                
                response.setStatus(httpStatusCode);
                if (httpStatusCode == 200 || httpStatusCode == 201) {
                    logger.debug("???????????? :" + responseBody);
                    putEdocDbResult(requestBodyMap, requestTranId, responseBody, httpStatusCode);
                } else {
                    logger.error("KT ????????????????????? http response code: " + httpStatusCode + " http body:" + responseBody);
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
            logger.warn("????????? ????????? ???????????? DB ?????? ?????? SKIP");
            return;
        }
        
        String errorCode = ""+httpStatusCode;
        String resultMessage = null;
        if(httpStatusCode == 200 || httpStatusCode == 201){
            try {
                Map<String, Object> respMap = gson.fromJson(responseBody, new TypeToken<Map<String, Object>>(){}.getType());
                errorCode = respMap.get("result_cd")==null?errorCode:respMap.get("result_cd").toString();
                if(errorCode.equals("00")){
                    resultMessage ="?????? ??????";
                }else{
                    resultMessage = respMap.get("errors") ==null?"?????? ??????":respMap.get("errors").toString().substring(0, 1000);
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
                logger.warn("DB ?????? ?????? ??? ?????? ??? ???????????? ?????? ??????, ?????? ????????? :"+data.toString());
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
            // ????????? ?????? ?????? ??? ?????? ?????? ?????? ??? ??????
            if(StringUtils.isBlank(service_cd)){
                logger.warn("?????????????????? ???????????? ?????? ??? ?????? [service_cd],\n?????? ?????? ??????:"+sendResultVo.toString());
                return gson.toJson(responseVo);
            }

            // ?????? ?????? ????????? ?????? ??? ?????? ?????? ?????? ??? ??????
            String req_msg_type_dvcd = sendResultVo.getReq_msg_type_dvcd();
            if(StringUtils.isBlank(service_cd)){
                logger.warn("?????????????????? ???????????? ?????? ??? ?????? [req_msg_type_dvcd],\n?????? ?????? ??????:"+sendResultVo.toString());
                return gson.toJson(responseVo);
            }

            List<KtEdocSendResultDetailVo> datas = sendResultVo.getReqs();
            if(ObjectUtils.isEmpty(datas)){
                logger.warn("?????????????????? ???????????? ?????? ??? ?????? [reqs],\n?????? ?????? ??????:"+sendResultVo.toString());
                return gson.toJson(responseVo);
            }

            for(KtEdocSendResultDetailVo detailVo : datas){
                String transKey = detailVo.getSrc_key();
                if(StringUtils.isBlank(transKey)){
                    logger.warn("?????????????????? ???????????? reqs ????????? ??? ?????? ??? ?????? [src_key],\n?????? ????????? ??????:"+detailVo.toString());
                    continue;
                }
                String errorCode = detailVo.getMms_sndg_rslt_dvcd();
                if(StringUtils.isBlank(errorCode)){
                    logger.warn("?????????????????? ???????????? reqs ????????? ??? ?????? ??? ?????? [mms_sndg_rslt_dvcd],\n?????? ????????? ??????:"+detailVo.toString());
                    continue;
                }
                EdocProcessBean edocProcessBean = new EdocProcessBean();
                edocProcessBean.setQUERY_TYPE("UPDATE");
                edocProcessBean.setPROVIDER("KT");
                edocProcessBean.setDOCID(transKey);
                edocProcessBean.setRESULTCODE(errorCode);
                if(errorCode.equals(SUCCESS_CODE)){
                    edocProcessBean.setRESULTMSG("????????????");
                }else{
                    edocProcessBean.setRESULTMSG("????????????(???????????????)");
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
            msgMap.put("error_msg", "??????/?????? ?????? ?????? ??? ?????? ??????");
            errors.add(msgMap);
            
            responseVo.setErrors(errors);
            return gson.toJson(responseVo);
        }
      
    }

}
