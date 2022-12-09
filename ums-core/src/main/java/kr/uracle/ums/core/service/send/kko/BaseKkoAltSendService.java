package kr.uracle.ums.core.service.send.kko;

import com.google.gson.Gson;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.core.dao.ums.UmsDao;
import kr.uracle.ums.core.ehcache.AltTemplateCacheMgr;
import kr.uracle.ums.core.processor.kakao.AllimtalkWorkerMgrPool;
import kr.uracle.ums.core.processor.push.PushEachProcessBean;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unchecked")
public abstract class BaseKkoAltSendService {
    protected Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    @Autowired(required = true)
    protected AllimtalkWorkerMgrPool allimtalkWorkerMgrPool;
    @Autowired(required = true)
    protected LgcnsKkoAltSendService lgcnsKkoAltSendService;
    @Autowired(required = true)
    protected AltTemplateCacheMgr altTemplateCacheMgr;
    @Autowired(required = true)
    protected UmsDao umsDao;
    @Autowired(required = true)
    protected Gson gson;

    /**
     * 푸시발송에서 개별로 발송 실패 대체 알림톡 발송일감 처리 등록.
     * UMS UI 발송: (T_UMS_SEND)원장 저장 테이블.
     * @param pushEachProcessBean
     * @param umsSendMsgBean
     * @return
     */
    public abstract void umsKkoAllimTolkSend(final PushEachProcessBean pushEachProcessBean, final UmsSendMsgBean umsSendMsgBean) throws Exception;

    /**
     * UMS발송에서 등록되는 알림톡 발송일감 처리 등록.
     * UMS UI 발송: (T_UMS_SEND)원장 저장 테이블.
     * @param users
     * @param umsSendMsgBean
     * @param cuidVarMap
     * @return
     */
    public abstract void umsKkoAllimTolkSend(final Map<String,List<String>> users, final UmsSendMsgBean umsSendMsgBean, Map<String,Map<String,String>> cuidVarMap, boolean isDaeCheSend) throws Exception;

    /**
     * UMS발송에서 등록되는 알림톡 발송일감 처리 등록.
     * UMS UI 발송: (T_UMS_SEND)원장 저장 테이블.
     * @param users
     * @param umsSendMsgBean
     * @return
     */
    public void umsKkoAllimTolkSend(final Map<String, List<String>> users, final UmsSendMsgBean umsSendMsgBean, boolean isDaeCheSend) throws Exception{
        //알림톡 발송하는 매니저에 일감등록
        umsKkoAllimTolkSend(users, umsSendMsgBean, null, isDaeCheSend);
    }

    /**
     * 푸시발송에서 4400에러(푸시등록사용자없음) 대체발송 처리. PushWorkerThread에서 UPMC에 http발송을 하였는데 응답코드가 4400이로 호출 된 경우 들어옴.
     * @param pushEachProcessBean
     * @return
     */
    public void umsKkoAllimTolkSend(final PushEachProcessBean pushEachProcessBean) throws Exception{
        // STEP1 : UMS 푸시원장테이블에서 발송정보를 조회해 온다.
        UmsSendMsgBean umsSendMsgBean = null;
        if(TransType.REAL==pushEachProcessBean.getTRANS_TYPE()) {
            umsSendMsgBean = umsDao.selUmsPushSendReal(pushEachProcessBean.getMASTERTABLE_SEQNO());
            umsSendMsgBean.setTRANS_TYPE(TransType.REAL.toString());
        }else{
            umsSendMsgBean = umsDao.selUmsPushSendBatch(pushEachProcessBean.getMASTERTABLE_SEQNO());
            umsSendMsgBean.setTRANS_TYPE(TransType.BATCH.toString());
        }

        //알림톡 발송하는 매니저에 일감등록

        //푸시 개별발송 실패 알림톡 대체발송
        umsSendMsgBean.setREPLACE_VARS(pushEachProcessBean.getALT_REPLACE_VARS());
        umsKkoAllimTolkSend(pushEachProcessBean,umsSendMsgBean);
    }

    public String replaceMsg(String msg ,String cuid, String cname, Map<String,String> msgVarMap){
        if(!"".equals(msg) && msg.indexOf("#{")>-1) {
            try {
                msg = msg.replace("#{아이디}", cuid);
                // 푸시대상자에서 발송일 경우 핸드폰 번호가 없어서 이름이 빈값이다. 푸시에 치환처리 할 수 있도록 빈값일 경우는 치환하지 않는다.
                if (!"".equals(cname)) {
                    msg = msg.replace("#{이름}", cname);
                }

                if (msgVarMap.size() > 0) {
                    // 개인화 메세지 치환처리
                    Set<Map.Entry<String, String>> personalMapSet = msgVarMap.entrySet();
                    for (Map.Entry<String, String> personalEntry : personalMapSet) {
                        msg = msg.replace(personalEntry.getKey(), personalEntry.getValue());
                    }
                }
            }catch (Exception e){
                logger.error(e.toString());
            }

        }
        return msg;
    }
}
