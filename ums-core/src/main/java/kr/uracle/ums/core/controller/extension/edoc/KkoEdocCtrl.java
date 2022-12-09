package kr.uracle.ums.core.controller.extension.edoc;

import com.google.gson.Gson;
import kr.uracle.ums.core.core.AuthIPCheckManager;
import kr.uracle.ums.core.processor.edoc.EdocDbMgr;
import kr.uracle.ums.core.processor.edoc.EdocProcessBean;
import kr.uracle.ums.core.service.UmsSendCommonService;
import kr.uracle.ums.core.util.httppoolclient.HttpPoolClient;
import kr.uracle.ums.core.util.httppoolclient.ResponseBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestMapping(value="/api/extension/edoc")
@Controller
public class KkoEdocCtrl {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    @Autowired(required = true)
    private UmsSendCommonService umsSendCommonService;
    @Autowired(required = true)
    private EdocDbMgr edocDbMgr;
    @Autowired
    private Gson gson;

    @RequestMapping(value = {"/kkoEdocSend.ums"}, produces = "application/json; charset=utf8")
    public @ResponseBody String kkoEdocSend(HttpServletRequest request, HttpServletResponse response, @RequestBody Map<String, Object> requestBodyMap) {
        try {
            //접근이 허용되지 않은 아이피일경우 실패처리함.
            if(!AuthIPCheckManager.getInstance().isAllowIp(request.getRemoteAddr())){
                response.setStatus(401);
                return "{\"error_code\":\"401\", \"error_message\":\"not_allowed_ip\"}";
            }

            String accessToken = request.getHeader("Authorization");
            if(StringUtils.isBlank(accessToken)){
                response.setStatus(401);
                return "{\"error_code\":\"401\", \"error_message\":\"Authorization\"}";
            }

            String contractUuid = request.getHeader("Contract-Uuid");
            if(StringUtils.isBlank(contractUuid)){
                response.setStatus(401);
                return "{\"error_code\":\"401\", \"error_message\":\"Contract-Uuid\"}";
            }
            // HTTP BODY 데이타
            if(!requestBodyMap.containsKey("document")){
                response.setStatus(400);
                return "{\"error_code\":\"400\", \"error_message\":\"invalid_param [document]\"}";
            }

            Map<String,Object> reqDocumentMap = (Map<String,Object>)requestBodyMap.get("document");
            if(!reqDocumentMap.containsKey("requestTranId")){
                response.setStatus(400);
                return "{\"error_code\":\"400\", \"error_message\":\"invalid_param [requestTranId]\"}";
            }
            String requestTranId = reqDocumentMap.get("requestTranId").toString();

            if(!reqDocumentMap.containsKey("hash")){
                response.setStatus(400);
                return "{\"error_code\":\"400\", \"error_message\":\"blank_param [hash]\"}";
            }

            if(!reqDocumentMap.containsKey("receiver")){
                response.setStatus(400);
                return "{\"error_code\":\"400\", \"error_message\":\"blank_param [receiver]\"}";
            }
            Map<String,String> receiverMap = null;
            try{
                receiverMap = gson.fromJson(reqDocumentMap.get("receiver").toString(),Map.class);
                if(!receiverMap.containsKey("ci")){
                    response.setStatus(400);
                    return "{\"error_code\":\"400\", \"error_message\":\"invalid_param [receiver ci error]\"}";
                }
            }catch (Exception e){
                response.setStatus(400);
                return "{\"error_code\":\"400\", \"error_message\":\"invalid_param [receiver json parse error]\"}";
            }

            Map<String, String> httpHeadParam = new HashMap();
            httpHeadParam.put("Content-Type","application/json");
            httpHeadParam.put("Authorization", accessToken.trim());
            httpHeadParam.put("Contract-Uuid", contractUuid);

            Map<String, Object> postParam = new HashMap<String,Object>();
            reqDocumentMap.remove("requestTranId"); // API 정의서의 포함되지 않은 값을 넘길경우 에러처리가 날수 있어 해당 정보는 삭제 후 넘김.
            postParam.put("document",reqDocumentMap);

            int responsTimeout = 20;
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(5000).setSocketTimeout(responsTimeout * 1000).build();
            try {
                String apiUrl = "https://docs-gw.kakaopay.com/v1/documents";
                ResponseBean responseBean = HttpPoolClient.getInstance().sendJsonPost(apiUrl, httpHeadParam, postParam, requestConfig);

                if (responseBean.getStatusCode() == 200 || responseBean.getStatusCode() == 201) {
                    response.setStatus(200);
                    logger.info("받은정보 :" + responseBean.getBody());
                    putEdocDbResult(reqDocumentMap, requestTranId, receiverMap, true, responseBean.getBody(), "200");
                } else {
                    response.setStatus(responseBean.getStatusCode());
                    logger.error("카카오 전자문서발송요청 http response code: " + responseBean.getStatusCode() + " http body:" + responseBean.getBody());
                    putEdocDbResult(reqDocumentMap, requestTranId, receiverMap, false, null, "" + responseBean.getStatusCode());
                }
                return responseBean.getBody();
            }catch (Exception e){
                logger.error(e.getMessage());
                putEdocDbResult(reqDocumentMap, requestTranId, receiverMap, false, null, "401");
                response.setStatus(401);
                Map<String,String> errMsgMap = new HashMap<>();
                errMsgMap.put("rtnMsg",e.getMessage());
                return gson.toJson(errMsgMap);
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
            response.setStatus(500);
            Map<String,String> errMsgMap = new HashMap<>();
            errMsgMap.put("rtnMsg",e.getMessage());
            return gson.toJson(errMsgMap);
        }
    }

    // 카카오 발송요청 응답 데이타 파싱 DB저장처리 큐에 등록
    private void putEdocDbResult(Map<String,Object> reqDocumentMap, String requestTranId,Map<String,String> receiverMap, boolean isSuccess, String respJsonStr, String httpStatusCode){
        EdocProcessBean edocProcessBean = new EdocProcessBean();
        edocProcessBean.setPROVIDER("KAKAO");
        edocProcessBean.setREQUEST_TRANID(requestTranId);
        edocProcessBean.setCI(receiverMap.get("ci"));
        edocProcessBean.setDOCHASH(reqDocumentMap.get("hash").toString());
        edocProcessBean.setSENDMSG(gson.toJson(reqDocumentMap));
        Map<String,String> respMap;
        try {
            respMap = gson.fromJson(respJsonStr,Map.class);
        }catch (Exception e){
            logger.error("카카오 전자문서 성공응답 Json 파싱중 에러 발생 :"+e.getMessage());
            return;
        }

        if(isSuccess){
            // 성공
            if(respMap.containsKey("document_binder_uuid")) {
                edocProcessBean.setDOCID(respMap.get("document_binder_uuid"));
            }else{
                // docId를 카카오에서 넘겨 주지 않았을 경우 방어코드로 doc hash 값 넣음.
                edocProcessBean.setDOCID(reqDocumentMap.get("hash").toString());
            }
        }else{
            // 실패
            edocProcessBean.setDOCID(reqDocumentMap.get("hash").toString());
            edocProcessBean.setRESULTCODE(httpStatusCode);
            if(respMap==null){
                edocProcessBean.setRESULTMSG("카카오 전자문서 HTTT 에러(connect timed out)");
            }else{
                if(respMap.containsKey("error_message")){
                    edocProcessBean.setRESULTMSG(respMap.get("error_message"));
                }else{
                    edocProcessBean.setRESULTMSG("카카오 전자문서 에러메세지 없는 응답에러");
                }
            }
        }
        edocDbMgr.putWork(edocProcessBean);
    }
}
