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
import java.util.UUID;

@RequestMapping(value="/api/extension/edoc")
@Controller
public class KkoEdocBulkCtrl {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    @Autowired(required = true)
    private UmsSendCommonService umsSendCommonService;
    @Autowired(required = true)
    private EdocDbMgr edocDbMgr;
    @Autowired
    private Gson gson;

    @RequestMapping(value = {"/kkoEdocBulkSend.ums"}, produces = "application/json; charset=utf8")
    public @ResponseBody String kkoEdocBulkSend(HttpServletRequest request, HttpServletResponse response, @RequestBody Map<String, Object> requestBodyMap) {
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

            if(!requestBodyMap.containsKey("requestTranId")){
                response.setStatus(401);
                return "{\"error_code\":\"401\", \"error_message\":\"invalid_param [requestTranId]\"}";
            }
            String requestTranId = requestBodyMap.get("requestTranId").toString();

            // HTTP BODY 데이타
            if(!requestBodyMap.containsKey("documents")){
                response.setStatus(400);
                return "{\"error_code\":\"400\", \"error_message\":\"invalid_param [documents]\"}";
            }

            List<Object> reqDocuments = (List<Object>)requestBodyMap.get("documents");

            if(reqDocuments==null || reqDocuments.size()==0){
                response.setStatus(400);
                return "{\"error_code\":\"400\", \"error_message\":\"blank_param [documents]\"}";
            }

            Map<String, String> httpHeadParam = new HashMap();
            httpHeadParam.put("Content-Type","application/json");
            httpHeadParam.put("Authorization", accessToken.trim());
            httpHeadParam.put("Contract-Uuid", contractUuid);

            Map<String, Object> postParam = new HashMap<String,Object>();
            postParam.put("document",reqDocuments);

            int responsTimeout = 40;
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(5000).setSocketTimeout(responsTimeout * 1000).build();
            try {
                String apiUrl = "https://docs-gw.kakaopay.com/v1/documents/bulk";
                ResponseBean responseBean = HttpPoolClient.getInstance().sendJsonPost(apiUrl, httpHeadParam, postParam, requestConfig);

                if (responseBean.getStatusCode() == 200 || responseBean.getStatusCode() == 201) {
                    response.setStatus(200);
                    logger.info("받은정보 :" + responseBean.getBody());
                    putEdocDbResult(reqDocuments, requestTranId, true, responseBean.getBody(), "200");
                } else {
                    response.setStatus(responseBean.getStatusCode());
                    logger.error("네이버 고지서발송요청 http response code: " + responseBean.getStatusCode() + " http body:" + responseBean.getBody());
                    putEdocDbResult(reqDocuments, requestTranId, false, null, "" + responseBean.getStatusCode());
                }
                return responseBean.getBody();
            }catch (Exception e){
                logger.error(e.getMessage());
                putEdocDbResult(reqDocuments, requestTranId, false, null, "401");
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
    private void putEdocDbResult(List<Object> reqDocuments, String requestTranId, boolean isSuccess, String respJsonStr, String httpStatusCode){

        // 카카오에서 받은 개별결과 데이타 저장.
        Map<String,Map<String,String>> respDocMap = new HashMap<>();
        for(int i=0; i<reqDocuments.size(); i++){
            Map<String,Object> reqDocumentMap = (Map<String,Object>)reqDocuments.get(i);
            Map<String,String> reqPropertyMap = null;
            if(reqDocumentMap.containsKey("property")){
                reqPropertyMap = (Map<String,String>)reqDocumentMap.get("property");
            }
            String external_document_uuid = "";
            if(reqPropertyMap!=null && reqPropertyMap.containsKey("external_document_uuid")) {
                external_document_uuid = reqPropertyMap.get("external_document_uuid");
            }else{
                UUID uuid = UUID.randomUUID();
                external_document_uuid = "F_"+uuid.toString();
            }

            EdocProcessBean edocProcessBean = new EdocProcessBean();
            edocProcessBean.setPROVIDER("KAKAO");
            edocProcessBean.setREQUEST_TRANID(requestTranId);
            if(reqDocumentMap.containsKey("hash")){
                edocProcessBean.setDOCHASH(reqDocumentMap.get("hash").toString());
            }
            if(reqDocumentMap.containsKey("receiver")){
                Map<String,String> receiverMap = (Map<String,String>)reqDocumentMap.get("receiver");
                if(receiverMap.containsKey("ci")){
                    edocProcessBean.setCI(receiverMap.get("ci"));
                }
            }
            edocProcessBean.setSENDMSG(gson.toJson(reqDocumentMap));
            edocProcessBean.setDOCID(external_document_uuid);

            Map<String,Object> respMap;

            if(isSuccess){  // http 성공
                try {
                    respMap = gson.fromJson(respJsonStr,Map.class);
                    // 받은 대량 발송 결과 정보를 Map<String,Map<String,String>)으로 담는다.
                    if(respMap.containsKey("documents")){
                        List<Map<String,String>> resDocuments = (List<Map<String,String>>)respMap.get("documents");
                        if(resDocuments!=null){
                            for(Map<String,String> resDocEachMap : resDocuments){
                                respDocMap.put(resDocEachMap.get("external_document_uuid"),resDocEachMap);
                            }
                        }
                    }

                }catch (Exception e){
                    logger.error("카카오 전자문서 성공응답 Json 파싱중 에러 발생 :"+e.getMessage());
                    edocProcessBean.setRESULTCODE("500");
                    edocProcessBean.setRESULTMSG("카카오 전자문서 성공응답 Json 파싱중 에러 발생");
                    edocDbMgr.putWork(edocProcessBean);
                    continue;
                }

                Map<String,String> resultSendMap = respDocMap.get(external_document_uuid);
                if(resultSendMap!=null){
                    if(resultSendMap.containsKey("error_code")){
                        edocProcessBean.setRESULTCODE(resultSendMap.get("error_code"));
                        edocProcessBean.setRESULTMSG(resultSendMap.get("error_message"));
                        edocDbMgr.putWork(edocProcessBean);
                        continue;
                    }else{
                        // 성공일 경우 document_binder_uuid가 존재하면 해당 값으로 DOCID를 저장한다.
                        if(resultSendMap.containsKey("document_binder_uuid")) {
                            edocProcessBean.setDOCID(resultSendMap.get("document_binder_uuid"));
                        }
                    }
                }else{
                    // 응답결과가 없는 경우
                    String errMsg = "카카오 전자문서 응답결과 누락. external_document_uuid:"+external_document_uuid;
                    logger.error(errMsg);
                    edocProcessBean.setRESULTCODE("404");
                    edocProcessBean.setRESULTMSG(errMsg);
                    edocDbMgr.putWork(edocProcessBean);
                    continue;
                }
            }else{
                // http 실패
                edocProcessBean.setRESULTCODE(httpStatusCode);
                edocProcessBean.setRESULTMSG("카카오 전자문서 HTTT 에러(connect timed out)");
            }
            edocDbMgr.putWork(edocProcessBean);
        }
    }
}
