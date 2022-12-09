package kr.uracle.ums.core.processor.kakao;

import kr.uracle.ums.codec.redis.config.ErrorManager;
import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import kr.uracle.ums.codec.redis.vo.kko.AmKkoFrtProcessBean;
import kr.uracle.ums.codec.redis.vo.kko.LotteKkoFrtProcessBean;
import kr.uracle.ums.core.core.tps.TpsManager;
import kr.uracle.ums.core.processor.bean.UmsResultBaseBean;
import kr.uracle.ums.core.util.amsoft.AmProcessor;
import kr.uracle.ums.tcppitcher.codec.messages.AmKkoFrtMessage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Y.B.H(mium2) on 2019. 3. 8..
 */
public class FriendtalkWorkerThread extends Thread {
	private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
		
	private String ThreadName;
	private final FriendtalkWorkerMgr workerManager;
	private boolean isRun = true;
	
	private final boolean USE_FATIGUE;

	public FriendtalkWorkerThread(String name, FriendtalkWorkerMgr _friendtalkWorkerMgr) {
		super(name);
		this.ThreadName = getName();
		this.workerManager = _friendtalkWorkerMgr;
		USE_FATIGUE = _friendtalkWorkerMgr.getUSE_FATIGUE().equalsIgnoreCase("Y")?true:false;
	}

	public void run() {
		while (isRun) {
			try {
				BaseProcessBean baseBean = workerManager.takeWork();
				if (baseBean == null) {
					continue;
				}
				execute(baseBean);
			} catch (InterruptedException ex) {
				logger.info("######## [" + ThreadName + "] InterruptedException 발생");
//				break;
			} catch (Exception e) {
				logger.error(e.getMessage());
				e.printStackTrace();
			}

			TpsManager.getInstance().addProcessCnt(TpsManager.TPSSERVERKIND.KKOFRT);

		}
	}

	private void execute(BaseProcessBean baseBean) {
		// STEP 1 : 발송 데이터 추출 및 조립
		UmsResultBaseBean rsltBean = makeResultBean(baseBean);
		final String PROVIDER = baseBean.getPROVIDER();
		boolean isFatigue = false;
		boolean isSend = false;
		String errorMsg = null;

		// UMS <> PROVIDER 간의 조인 키 = SEND_TYPE_SEQCODE <> 마지막 여유 필드
		// LGCNS 조인키는 CUID	- XML QUERY MAPPING
		// LOTTE 조인키는 USER9 	- XML QUERY MAPPING
		// AM 조인키는 MessageId 	- AmKkoAltMessage Assemble MAPPING
		try {
			isFatigue = isFatigue(baseBean);
			if(baseBean instanceof LotteKkoFrtProcessBean) {
				LotteKkoFrtProcessBean prcsBean = (LotteKkoFrtProcessBean)baseBean;
				prcsBean.setCMID(baseBean.getTRANSACTION_KEY().substring(3));
			}

			// STEP 2 : 발송 가능여부 체크
			isSend = workerManager.getUmsSendCheckService().chkSend(baseBean, rsltBean, isFatigue);

			//STEP 3 : 발송상세 결과정보 저장
			workerManager.getUmsDao().inUmsSendDetail(rsltBean, baseBean);

			// STEP 4 : 발송처리
			if(isSend) {
				if(baseBean instanceof AmKkoFrtProcessBean){
					AmKkoFrtMessage amKkoFrtMessage = makeAmKkoFrtMessage((AmKkoFrtProcessBean) baseBean, rsltBean.getSEND_TYPE());
					AmProcessor.getInstance().sendMsg(amKkoFrtMessage);
				}else{
					workerManager.getProviderDao(PROVIDER).inFrtSendMsg(baseBean);
				}
			}
		}catch (RuntimeException re){
			errorMsg = "네트워크 에러";
			re.printStackTrace();
		}catch (SQLException se){
			errorMsg = "DB 에러";
			se.printStackTrace();
		}catch (Exception e) {
			errorMsg = ErrorManager.getInstance().getMsg(ErrorManager.ERR_500);
			e.printStackTrace();
		}

		//STEP 5 : 내부 에러 시 Agent 처리를 위해 통합 로그 테이블에 입력
		if(errorMsg != null){
			rsltBean.setERRCODE(ErrorManager.ERR_500);
			rsltBean.setRESULTMSG(errorMsg);
			workerManager.getUmsDao().inUmsLog(rsltBean);
		}

		//STEP 6 : 통계 데이터 축적
		if("RS".equals(rsltBean.getSEND_RESULT())){
			workerManager.getSentInfoManager().addSendSucc(baseBean.getTRANS_TYPE(), baseBean.getMASTERTABLE_SEQNO(), SendType.KKOFRT);
			if(isFatigue){
				workerManager.getUmsSendCommonService().upFatigue(baseBean.getCUID(), false);
			}
		}else{
			workerManager.getSentInfoManager().addSendFail(baseBean.getTRANS_TYPE(), baseBean.getMASTERTABLE_SEQNO(), SendType.KKOFRT, true);
		}
	}

	private boolean isFatigue(BaseProcessBean baseBean) {
		return "Y".equals(baseBean.getFATIGUE_YN()) || USE_FATIGUE;
	}

	private String replaceMsg(String msgBody, BaseProcessBean baseBean){
		if (msgBody.indexOf("#{") > -1) {
			msgBody = msgBody.replace("#{이름}", baseBean.getCNAME()).replace("#{아이디}", baseBean.getCUID());
			Map<String, String> personalMap = baseBean.getMSG_VAR_MAP();
			if (personalMap != null && personalMap.size() > 0) {
				for (Map.Entry<String, String> personalEntry : personalMap.entrySet()) {
					msgBody = msgBody.replace(personalEntry.getKey(), personalEntry.getValue());
				}
			}
		}
		return msgBody;
	}
	private UmsResultBaseBean makeResultBean(BaseProcessBean baseBean) {
		UmsResultBaseBean rsltBean = new UmsResultBaseBean(baseBean.getTRANS_TYPE(), SendType.KKOFRT);
		rsltBean.setPROVIDER(baseBean.getPROVIDER());
		rsltBean.setMASTERTABLE_SEQNO(baseBean.getMASTERTABLE_SEQNO());
		rsltBean.setSENDERID(baseBean.getSENDERID());
		rsltBean.setSENDERGROUPCODE(baseBean.getSENDGROUPCODE());
		rsltBean.setROOT_CHANNEL_YN(baseBean.getROOT_CHANNEL_YN());
		rsltBean.setCUID(baseBean.getCUID());
		rsltBean.setCNAME(baseBean.getCNAME());
		if (StringUtils.isNotBlank(baseBean.getMSG_VARS())) {
			rsltBean.setMSG_VARS(baseBean.getMSG_VARS());
		}
		rsltBean.setSEND_TYPE_SEQCODE(baseBean.getTRANSACTION_KEY());
		rsltBean.setSVC_ID(baseBean.getSERVICE_ID());
		rsltBean.setCALLBACKNUM(baseBean.getCALLBACK_NUM());
		rsltBean.setMOBILE_NUM(baseBean.getMOBILE_NUM());
		String replaceMsgBody = replaceMsg(baseBean.getMSG_BODY(), baseBean);
		baseBean.setMSG_BODY(replaceMsgBody);
		rsltBean.setSEND_MSG(replaceMsgBody);
		rsltBean.setSEND_TITLE(baseBean.getTITLE());
		rsltBean.setSEND_RESULT("RS");
		rsltBean.setSUCC_STATUS("0");
		rsltBean.setRESULTMSG("발송요청성공");

		rsltBean.setDETAIL_SEND_TYPE(baseBean.getHAS_IMAGE()?"KKOFRT_IMG":"KKOFRT_TXT");
		return rsltBean;
	}

	private AmKkoFrtMessage makeAmKkoFrtMessage(AmKkoFrtProcessBean prcsBean, String sendChannel){

		AmKkoFrtMessage amMsg = new AmKkoFrtMessage();
		amMsg.setAckYn("Y");
		amMsg.setTranType("R");
		amMsg.setTranType(prcsBean.getTRANS_TYPE()== TransType.REAL?"R":"B");
		amMsg.setAdsYn(prcsBean.getUMS_MSG_TYPE().equals("I")?"N":"Y");
		amMsg.setUmsSeqKey(""+prcsBean.getMASTERTABLE_SEQNO());
		amMsg.setMessageId(prcsBean.getTRANSACTION_KEY());
		amMsg.setSendChannel("KKOFRT"); // KKOALT,  KKOFRT , SMS , LMS, MMS, RCS
		amMsg.setSenderGroup(prcsBean.getSENDGROUPCODE());
		amMsg.setSenderSys(prcsBean.getSENDERID());
		amMsg.setUserId(prcsBean.getUSER_ID());
		amMsg.setPhoneNum(prcsBean.getMOBILE_NUM());
		amMsg.setCallbackNum(prcsBean.getCALLBACK_NUM());
		amMsg.setTitle(prcsBean.getTITLE());
		amMsg.setMessage(prcsBean.getMSG_BODY());
		if(StringUtils.isNotBlank(prcsBean.getIMG_SRCS())){
			try{
				workerManager.getGson().fromJson(prcsBean.getIMG_SRCS(), List.class);
				amMsg.setImsSrcs(prcsBean.getIMG_SRCS());
			}catch (Exception e){
				List<String> imgSrcList = new ArrayList<>();
				imgSrcList.add(prcsBean.getIMG_SRCS());
				amMsg.setImsSrcs(workerManager.getGson().toJson(imgSrcList));
			}
		}

		amMsg.setVar1(prcsBean.getVAR1());
		amMsg.setVar2(prcsBean.getVAR2());
		amMsg.setVar3(prcsBean.getVAR3());
		amMsg.setVar4(prcsBean.getVAR4());
		amMsg.setVar5(prcsBean.getVAR5());
		return amMsg;
	}

	public boolean isRun() { return isRun;	}
	public void setRun(boolean isRun) { this.isRun = isRun;	}
}
