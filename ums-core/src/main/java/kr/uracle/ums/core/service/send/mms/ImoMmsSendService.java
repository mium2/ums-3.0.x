package kr.uracle.ums.core.service.send.mms;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.vo.mms.ImoMmsProcessBean;
import kr.uracle.ums.core.processor.push.PushEachProcessBean;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import kr.uracle.ums.core.util.httppoolclient.HttpPoolClient;
import kr.uracle.ums.core.util.httppoolclient.ResponseBean;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ImoMmsSendService extends BaseMmsSendService  {
    @Value("${IMO.HTTP.SERVER:}")
    private String imoURL;

    @Value("${IMO.HTTP.TOKEN:}")
    private String imoToken;

    @Override
    public void umsMmsSend(Map<String, List<String>> users, UmsSendMsgBean umsSendMsgBean, Map<String, Map<String, String>> cuidVarMap, boolean isDaeCheSend) {
        if(ObjectUtils.isEmpty(users)){
            logger.warn("IMO MMS 서비스 - users map is empty");
            return;
        }

        SendType sendType = SendType.LMS;
        // 대체 발송 여부
        String ROOT_CHANNEL_YN = isDaeCheSend?"N":"Y";
        // 개인화 유무
        boolean isPersonalMsg = ObjectUtils.isNotEmpty(cuidVarMap);

        int imgCnt = 0;
        String fileName1 = null;
        String fileName2 = null;
        String fileName3 = null;
        String sendUrl = imoURL.endsWith("/")?imoURL+"api/v1/image/upload":imoURL+"/api/v1/image/upload";
        // TODO: 이미지 등록 실패 시 ??????? 어떤 정책을 가지고 가야 할지 결정이 필요 - thread 단에서 sendtype이 MMS인데 이미지 정보가 없으면 실패 처리 일단 하자
        if(StringUtils.isNotBlank(umsSendMsgBean.getMMS_IMGURL())) {
            sendType = SendType.MMS;
            try{
                List<String> mmsImgPaths = gson.fromJson(umsSendMsgBean.getMMS_IMGURL(), new TypeToken<List<String>>(){}.getType());
                if(mmsImgPaths != null){
                    imgCnt = mmsImgPaths.size();
                    for(int i=0; i<imgCnt; i++){
                        String fileName = enrollImage(sendUrl, mmsImgPaths.get(i));
                        if(fileName == null)break;
                        switch (i){
                            case 0:
                                fileName1 = fileName;
                                break;
                            case 1:
                                fileName2 = fileName;
                                break;
                            case 2:
                                fileName3 = fileName;
                                break;
                        }
                    }
                }
            }catch(JsonSyntaxException j){
                logger.error("IMO MMS 서비스 잘못된 포맷의 이미지 정보 - "+umsSendMsgBean.getMMS_IMGURL());
                imgCnt = 1;
                j.printStackTrace();
            }catch(Exception e){
                e.printStackTrace();
            }
        }


        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
        for(String cuid :  users.keySet()){
            //{"아이디":["핸드폰번호","이름"]}
            List<String> userInfos = users.get(cuid);
            ImoMmsProcessBean prcsBean = new ImoMmsProcessBean();
            prcsBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO());
            prcsBean.setTRANS_TYPE(transType);
            prcsBean.setSTART_SEND_TYPE(sendType.toString());
            prcsBean.setUMS_MSG_TYPE(umsSendMsgBean.getMSG_TYPE());
            prcsBean.setROOT_CHANNEL_YN(ROOT_CHANNEL_YN);
            prcsBean.setMIN_START_TIME(umsSendMsgBean.getMIN_START_TIME());
            prcsBean.setMAX_END_TIME(umsSendMsgBean.getMAX_END_TIME());
            prcsBean.setFATIGUE_YN(umsSendMsgBean.getFATIGUE_YN());

            prcsBean.setCUST_TRANSKEY(umsSendMsgBean.getCUST_TRANSKEY());
            prcsBean.setCUST_TRANSGROUPKEY(umsSendMsgBean.getCUST_TRANSGROUPKEY());
            prcsBean.setVAR1(umsSendMsgBean.getVAR1());
            prcsBean.setVAR2(umsSendMsgBean.getVAR2());
            prcsBean.setVAR3(umsSendMsgBean.getVAR3());
            prcsBean.setVAR4(umsSendMsgBean.getVAR4());
            prcsBean.setVAR5(umsSendMsgBean.getVAR5());
            prcsBean.setVAR6(umsSendMsgBean.getVAR6());
            prcsBean.setVAR7(umsSendMsgBean.getVAR7());
            prcsBean.setVAR8(umsSendMsgBean.getVAR8());
            prcsBean.setVAR9(umsSendMsgBean.getVAR9());

            prcsBean.setSENDERID(umsSendMsgBean.getSENDERID());
            prcsBean.setSENDGROUPCODE(umsSendMsgBean.getSENDGROUPCODE());
            prcsBean.setCUID(cuid);
            if(isPersonalMsg) {
                prcsBean.setMSG_VAR_MAP(cuidVarMap.get(cuid));
                prcsBean.setMSG_VARS(gson.toJson(prcsBean.getMSG_VAR_MAP()));
            }

            prcsBean.setTITLE(umsSendMsgBean.getTITLE());
            prcsBean.setMSG_BODY(umsSendMsgBean.getSMS_MSG());
            prcsBean.setCALLBACK_NUM(umsSendMsgBean.getCALLBACK_NUM());

            prcsBean.setHOST_URL(imoURL.endsWith("/")?imoURL+"api/v1/send/create":imoURL+"/api/v1/send/create");
            prcsBean.setIMG_COUNT(imgCnt);
            prcsBean.putHeader("Content-Typ",   "application/json");
            prcsBean.putHeader("Authorization", "Bearer "+imoToken);
            prcsBean.putBody("totalCount", 1);
            prcsBean.putBody("callback",       umsSendMsgBean.getCALLBACK_NUM());
            prcsBean.putBody("title",          umsSendMsgBean.getTITLE());
            if(fileName1 != null)prcsBean.putBody("imageName1", fileName1);
            if(fileName2 != null)prcsBean.putBody("imageName2", fileName2);
            if(fileName3 != null)prcsBean.putBody("imageName3", fileName3);
            if(ObjectUtils.isNotEmpty(userInfos)){
                String mobileNum = userInfos.get(0).trim();
                String cname = "고객";
                if(userInfos.size()>1){
                    cname  = userInfos.get(1);
                }
                prcsBean.setCNAME(cname);
                prcsBean.setMOBILE_NUM(mobileNum);
                prcsBean.putBody("phoneNumbers",   Collections.singletonList(mobileNum));
            }
            mmsWorkerMgrPool.putWork(prcsBean);
        }
    }
    @Override
    public void umsMmsSend(PushEachProcessBean pushEachProcessBean, UmsSendMsgBean umsSendMsgBean) throws Exception {
        if(pushEachProcessBean==null) {
            logger.warn("IMO MMS 서비스 - PushEachProcessBean is null");
            return;
        }

        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
        ImoMmsProcessBean prcsBean = new ImoMmsProcessBean();
        prcsBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO());
        prcsBean.setTRANS_TYPE(transType);
        prcsBean.setSTART_SEND_TYPE(SendType.LMS.toString());
        prcsBean.setUMS_MSG_TYPE(umsSendMsgBean.getMSG_TYPE());
        prcsBean.setROOT_CHANNEL_YN("N");
        prcsBean.setMIN_START_TIME(umsSendMsgBean.getMIN_START_TIME());
        prcsBean.setMAX_END_TIME(umsSendMsgBean.getMAX_END_TIME());
        prcsBean.setFATIGUE_YN(umsSendMsgBean.getFATIGUE_YN());

        prcsBean.setCUST_TRANSKEY(umsSendMsgBean.getCUST_TRANSKEY());
        prcsBean.setCUST_TRANSGROUPKEY(umsSendMsgBean.getCUST_TRANSGROUPKEY());
        prcsBean.setVAR1(umsSendMsgBean.getVAR1());
        prcsBean.setVAR2(umsSendMsgBean.getVAR2());
        prcsBean.setVAR3(umsSendMsgBean.getVAR3());
        prcsBean.setVAR4(umsSendMsgBean.getVAR4());
        prcsBean.setVAR5(umsSendMsgBean.getVAR5());
        prcsBean.setVAR6(umsSendMsgBean.getVAR6());
        prcsBean.setVAR7(umsSendMsgBean.getVAR7());
        prcsBean.setVAR8(umsSendMsgBean.getVAR8());
        prcsBean.setVAR9(umsSendMsgBean.getVAR9());

        prcsBean.setSENDERID(umsSendMsgBean.getSENDERID());
        prcsBean.setSENDGROUPCODE(umsSendMsgBean.getSENDGROUPCODE());
        prcsBean.setCUID(pushEachProcessBean.getCUID());
        prcsBean.setCNAME(pushEachProcessBean.getCNAME());

        prcsBean.setCALLBACK_NUM(umsSendMsgBean.getCALLBACK_NUM());
        prcsBean.setMOBILE_NUM(pushEachProcessBean.getMOBILE_NUM());
        prcsBean.setTITLE(umsSendMsgBean.getSMS_TITLE());
        prcsBean.setMSG_BODY(replaceMsg(umsSendMsgBean.getSMS_MSG(),pushEachProcessBean.getCUID(),pushEachProcessBean.getCNAME(),pushEachProcessBean.getMSG_VAR_MAP()));
    }

    private String enrollImage(String url,  String filePath) throws Exception {
        Map<String, File> fileMap  = new HashMap<String, File>(1);
        File f = new File(filePath);
        if(f.exists() == false){
            logger.error("IMO MMS 서비스 요청 이미지 에러 - 이미지 파일을 찾을 수 없음 : "+filePath);
            return null;
        }
        fileMap.put("image", f);
        Map<String, String> headerMap = new HashMap<String, String>(2);
        headerMap.put("Authorization", "Bearer "+imoToken);
        ResponseBean responseBean = HttpPoolClient.getInstance().sendMultipartPost(url, headerMap, null, fileMap);
        if (responseBean.getStatusCode() == 200 || responseBean.getStatusCode() == 201) {
            Map<String, Object> responseBodyMap = gson.fromJson(responseBean.getBody(), new TypeToken<Map<String, Object>>(){}.getType());
            String rsltCode = responseBodyMap.get("code") == null ? null : responseBodyMap.get("code").toString();
            if(rsltCode != null && rsltCode.equals("200")){
                String data = responseBodyMap.get("data") == null? null: responseBodyMap.get("data").toString();
                Map<String, Object> dataMap = gson.fromJson(data, new TypeToken<Map<String, Object>>(){}.getType());
                if(ObjectUtils.isNotEmpty(dataMap))return dataMap.get("imageName")==null?null:dataMap.get("imageName").toString();
            }
        }else{
            logger.error("IMO MMS 서비스 이미지 등록 서버 에러 - HTTP STATUS CODE : "+responseBean.getStatusCode());
        }
        return null;
    }
}
