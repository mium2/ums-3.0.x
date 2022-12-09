package kr.uracle.ums.core.processor.rcs;

import com.google.gson.Gson;
import kr.uracle.ums.codec.redis.config.ErrorManager;
import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import kr.uracle.ums.codec.redis.vo.rcs.AmRcsProcessBean;
import kr.uracle.ums.codec.redis.vo.rcs.LguRcsProcessBean;
import kr.uracle.ums.codec.redis.vo.rcs.LotteRcsProcessBean;
import kr.uracle.ums.core.core.tps.TpsManager;
import kr.uracle.ums.core.processor.bean.UmsResultBaseBean;
import kr.uracle.ums.core.util.amsoft.AmProcessor;
import kr.uracle.ums.tcppitcher.codec.messages.AmRcsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Y.B.H(mium2) on 2019. 3. 8..
 */
public class RcsWorkerThread extends Thread {
	private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	private final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("HHmm");
	
	private String ThreadName;
	private final RcsWorkerMgr workerManager;
	private boolean isRun = true;

	private final Gson gson = new Gson();
	private final boolean USE_FATIGUE;

	public RcsWorkerThread(String name, RcsWorkerMgr rcsWorkerMgr) {
		super(name);
		this.ThreadName = getName();
		this.workerManager = rcsWorkerMgr;
		USE_FATIGUE = rcsWorkerMgr.getUSE_FATIGUE().equalsIgnoreCase("Y")?true:false;
	}

	public void run() {
		while (isRun) {
			BaseProcessBean baseBean = null;
			try {
				// Step 1 : 처리일감 가져옴.
				baseBean = workerManager.takeWork();
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

			TpsManager.getInstance().addProcessCnt(TpsManager.TPSSERVERKIND.RCS);
		}
	}

	private void execute(BaseProcessBean baseBean) {
		// STEP 1 : 발송 데이터 추출 및 조립
		UmsResultBaseBean rsltBean = makeResultBean(baseBean, SendType.RCS_FREE);
		final String PROVIDER = baseBean.getPROVIDER();
		boolean isFatigue = false;
		boolean isSend = true;
		String errorMsg = null;
		// UMS <> PROVIDER 간의 조인 키 = SEND_TYPE_SEQCODE <> 마지막 여유 필드
		//LOTTE 조인키는 USER9 	- XML QUERY MAPPING
		//AM 조인키는 MessageId 	- AmKkoAltMessage Assemble MAPPING
		//LGU 조인키는 ETC6 		- XML QUERY MAPPING
		try {
			isFatigue = isFatigue(baseBean);
			if (baseBean instanceof LguRcsProcessBean){
				LguRcsProcessBean prcsBean = (LguRcsProcessBean) baseBean;
				if (prcsBean.getImageRegister()) {
					for (String imagePath : prcsBean.getIMG_PATHS()) {
						prcsBean.setIMG_PATH(imagePath);
						workerManager.getProviderDao(prcsBean.getPROVIDER()).inRcsImg(prcsBean);
					}
					return;
				}

				if(rsltBean.getSEND_TYPE().equals(SendType.RCS_MMS.toString())||rsltBean.getSEND_TYPE().equals(SendType.RCS_CELL.toString())){
					prcsBean.setRCS_OBJECT(baseBean.getMSG_BODY());
					baseBean.setMSG_BODY("");
				}
			}else if (baseBean instanceof LotteRcsProcessBean) {
				LotteRcsProcessBean prcsBean = (LotteRcsProcessBean) baseBean;
				prcsBean.setCMID(baseBean.getTRANSACTION_KEY().substring(3));
			}
			// STEP 2 : 발송 가능여부 체크
			isSend = workerManager.getUmsSendCheckService().chkSend(baseBean, rsltBean, isFatigue);

			//STEP 3 : 발송상세 결과정보 저장
			workerManager.getUmsDao().inUmsSendDetail(rsltBean, baseBean);

			// STEP 4 : 발송처리
			if (isSend) {
				if(baseBean instanceof AmRcsProcessBean){
					AmRcsMessage amKRcsMessage = makeAmRcsMessage((AmRcsProcessBean)baseBean, rsltBean.getSEND_TYPE());
					AmProcessor.getInstance().sendMsg(amKRcsMessage);
				}else{
					workerManager.getProviderDao(PROVIDER).inRcsSend(baseBean);
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

		//STEP 5 : 내부 에러 시 Agent 결과 처리를 위해 통합 테이블 입력
		if(errorMsg != null){
			rsltBean.setERRCODE(ErrorManager.ERR_500);
			rsltBean.setRESULTMSG(errorMsg);
			workerManager.getUmsDao().inUmsLog(rsltBean);
		}

		//STEP 6: 통계 데이터 축적
		if("RS".equals(rsltBean.getSEND_RESULT())){
			workerManager.getSentInfoManager().addSendSucc(baseBean.getTRANS_TYPE(), baseBean.getMASTERTABLE_SEQNO(), SendType.valueOf(rsltBean.getSEND_TYPE()));
			if(isFatigue){
				workerManager.getUmsSendCommonService().upFatigue(baseBean.getCUID(), false);
			}
		}else{
			workerManager.getSentInfoManager().addSendFail(baseBean.getTRANS_TYPE(), baseBean.getMASTERTABLE_SEQNO(), SendType.valueOf(rsltBean.getSEND_TYPE()), true);
		}
	}
	private AmRcsMessage makeAmRcsMessage(AmRcsProcessBean prcsBean, String sendChannel){
		AmRcsMessage amMsg = new AmRcsMessage();
		amMsg.setAckYn("Y");
		amMsg.setTranType(prcsBean.getTRANS_TYPE()== TransType.REAL?"R":"B");
		amMsg.setAdsYn(prcsBean.getUMS_MSG_TYPE().equals("I")?"N":"Y");
		amMsg.setUmsSeqKey(""+prcsBean.getMASTERTABLE_SEQNO());
		amMsg.setMessageId(prcsBean.getTRANSACTION_KEY());
		amMsg.setSendChannel("RCS");
		amMsg.setSenderGroup(prcsBean.getSENDGROUPCODE());
		amMsg.setSenderSys(prcsBean.getSENDERID());
		amMsg.setUserId(prcsBean.getUSER_ID());
		amMsg.setPhoneNum(prcsBean.getMOBILE_NUM());
		amMsg.setCallbackNum(prcsBean.getCALLBACK_NUM());
		amMsg.setTitle(prcsBean.getTITLE());
		amMsg.setMessage(prcsBean.getMSG_BODY());
		if(!"".equals(prcsBean.getIMG_SRCS())){
			try{
				workerManager.getGson().fromJson(prcsBean.getIMG_SRCS(), List.class);
				amMsg.setImsSrcs(prcsBean.getIMG_SRCS());
			}catch (Exception e){
				List<String> imgSrcList = new ArrayList<>();
				imgSrcList.add(prcsBean.getIMG_SRCS());
				amMsg.setImsSrcs(workerManager.getGson().toJson(imgSrcList));
			}
		}
		// AM RCS 고유파라미터
		amMsg.setRcsBrandId(prcsBean.getRCS_BRAND_ID());
		amMsg.setRcsMsgbaseId(prcsBean.getRCS_MSGBASE_ID());
		amMsg.setRcsType(prcsBean.getRCS_TYPE());
		amMsg.setCopyAllow(prcsBean.getCOPY_ALLOW());
		amMsg.setExpiryOption(prcsBean.getEXPIRY_OPTION());
		amMsg.setFooter(prcsBean.getFOOTER());
		amMsg.setBtnObject(prcsBean.getBTN_OBJECT());

		amMsg.setVar1(prcsBean.getVAR1());
		amMsg.setVar2(prcsBean.getVAR2());
		amMsg.setVar3(prcsBean.getVAR3());
		amMsg.setVar4(prcsBean.getVAR4());
		amMsg.setVar5(prcsBean.getVAR5());
		return amMsg;
	}
	
	private boolean isFatigue(BaseProcessBean baseBean) {
		boolean isFatigue = false;
		if ("Y".equals(baseBean.getFATIGUE_YN()) || USE_FATIGUE ) isFatigue = true;
		if(baseBean.getUMS_MSG_TYPE().equalsIgnoreCase("I")) isFatigue = false;
		return isFatigue;
	}

	private UmsResultBaseBean makeResultBean(BaseProcessBean baseBean, SendType sendType) {
		UmsResultBaseBean rsltBean = new UmsResultBaseBean(baseBean.getTRANS_TYPE(), sendType);
		rsltBean.setPROVIDER(baseBean.getPROVIDER());
		rsltBean.setMASTERTABLE_SEQNO(baseBean.getMASTERTABLE_SEQNO());
		rsltBean.setSENDERID(baseBean.getSENDERID());
		rsltBean.setSENDERGROUPCODE(baseBean.getSENDGROUPCODE());
		rsltBean.setROOT_CHANNEL_YN(baseBean.getROOT_CHANNEL_YN());
		rsltBean.setCUID(baseBean.getCUID());
		rsltBean.setCNAME(baseBean.getCNAME());
		rsltBean.setSEND_TYPE(baseBean.getSTART_SEND_TYPE());
		if (!"".equals(baseBean.getMSG_VARS())) {
			rsltBean.setMSG_VARS(baseBean.getMSG_VARS());
		}
		rsltBean.setSEND_TYPE_SEQCODE(baseBean.getTRANSACTION_KEY());
		rsltBean.setCALLBACKNUM(baseBean.getCALLBACK_NUM());
		rsltBean.setMOBILE_NUM(baseBean.getMOBILE_NUM());
		String replaceMsgBody = replaceMsg(baseBean.getMSG_BODY(), baseBean);
		baseBean.setMSG_BODY(replaceMsgBody);
		rsltBean.setSEND_MSG(replaceMsgBody);
		rsltBean.setSEND_TITLE(baseBean.getTITLE());
		rsltBean.setSEND_RESULT("RS");
		rsltBean.setSUCC_STATUS("0");
		rsltBean.setRESULTMSG("발송요청성공");
		return rsltBean;
	}

	private String replaceMsg(String sendMsg, BaseProcessBean baseBean) {
		if (sendMsg.indexOf("#{") > -1) {
			sendMsg = sendMsg.replace("#{이름}", baseBean.getCNAME()).replace("#{아이디}", baseBean.getCUID());
			Map<String, String> personalMap = baseBean.getMSG_VAR_MAP();
			if (personalMap != null && personalMap.size() > 0) {
				// 개인화 메세지 치환처리
				Set<Map.Entry<String, String>> personalMapSet = personalMap.entrySet();
				for (Map.Entry<String, String> personalEntry : personalMapSet) {
					sendMsg = sendMsg.replace(personalEntry.getKey(), personalEntry.getValue());
				}
			}
		}
		return sendMsg;
	}

	public boolean isRun() { return isRun; }
	public void setRun(boolean isRun) { this.isRun = isRun;	}
}
