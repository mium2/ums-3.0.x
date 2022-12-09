package kr.uracle.ums.core.service.template;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import com.google.gson.Gson;
import kr.uracle.ums.core.service.UmsSendCommonService;
import kr.uracle.ums.core.util.DateUtil;
import kr.uracle.ums.core.vo.SyncAltTemplVo;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import kr.uracle.ums.core.common.Constants;
import kr.uracle.ums.core.dao.setting.SettingPlusDao;
import kr.uracle.ums.core.dao.template.AlimTemplateDao;
import kr.uracle.ums.core.util.AES256Cipher;
import kr.uracle.ums.core.util.httppoolclient.HttpPoolClient;
import kr.uracle.ums.core.util.httppoolclient.ResponseBean;

@Service
@SuppressWarnings("unchecked")
public class AlmTokTemplService {
	private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	@Autowired(required = true)
	@Qualifier("sqlSessionTemplate")
	protected SqlSessionTemplate sqlSessionTemplate;

	@Autowired(required = true)
	protected MessageSource messageSource;

	@Autowired(required = true)
	protected Gson gson;
	private static final Map<String, String> httpHeadParam = new HashMap<String, String>();
	private static final Map<String, String> bType = new HashMap<String, String>();

	@Value("${LGCNS.API_HOST:https://bizmsg.lgcns.com}")
	private String LGCNS_HOST;
	@Value("${LGCNS.CLINT_COM_ID:}")
	private String CLINT_COM_ID;
	@Value("${LGCNS.PLUS_ID:}")
	private String CACAO_PLUS_ID;
	@Value("${LGCNS.API_KEY:}")
	private String TEMPLET_AUTHKEY;
	@Value("${LGCNS.ALLIMTOLK.TEMPL.SINCE:90}")
	private String TEMPLET_SINCE;

	@Value("${KKO.PLUS_ID:}")
	private String LGCNS_PLUSID;
	
	private Map<String, String> getHttpHeadParam(String encriptStr) throws NoSuchAlgorithmException, UnsupportedEncodingException, GeneralSecurityException {
		logger.debug("encipStr: {} ",encriptStr);
		String authCode = AES256Cipher.getInstance().encrypt(encriptStr, TEMPLET_AUTHKEY);
		httpHeadParam.put("auth", authCode);
		httpHeadParam.put("Content-Type", "application/x-www-form-urlencoded");
		return httpHeadParam;
	}

	public Map<String, Object> newSyncTemplateStatus(HttpServletRequest request) throws Exception {
		String reg_id = null;
		Locale local = Locale.getDefault();
		if (request != null) {
			request.getSession().getAttribute("LOGIN_ID");
			local = request.getLocale();
		}
		if (reg_id == null) reg_id = "UMS_BATCH";


		Map<String, Object> serviceResult = new HashMap<String, Object>();
		serviceResult.put("succMsg", messageSource.getMessage(Constants.SUCCESS, null, local));

		// 플러스 아이디가 여러개일 경우 알림톡 템플릿 구현해야 함.
		Map<String,Object> dbParamMap = new HashMap<String, Object>();
		dbParamMap.put("pageSize",1000);
		dbParamMap.put("pageNum",1);
		List<HashMap<String, Object>> dbPlusidList = new ArrayList<>();
		try {
			dbPlusidList = sqlSessionTemplate.selectList("mybatis.common.selKkoInfo", dbParamMap);
			logger.warn("!!! 매우중요 : DB(T_UMS_CHANNEL_CONFIG)에서 알림톡 플러스아이디 정보 읽어옴.");

			Set<String> usedPlusFriendIDSet = new HashSet<>();
			if(dbPlusidList!=null && dbPlusidList.size()>0) {
				for(int i=0; i<dbPlusidList.size(); i++){
					HashMap<String,Object> dbInfoMap = dbPlusidList.get(i);
					if(dbInfoMap.containsKey("CONFIG")){
						String altConfig = dbInfoMap.get("CONFIG").toString();
						HashMap<String,Object> plusInfoMap = gson.fromJson(altConfig, HashMap.class);
						if(plusInfoMap.containsKey("plusFriendId")){
							try {
								String plusFriendID = plusInfoMap.get("plusFriendId").toString();
//								String serviceNo = CLINT_COM_ID;
//								if(plusInfoMap.containsKey("serviceNo")) {
//									serviceNo = plusInfoMap.get("serviceNo").toString();
//								}
								if(usedPlusFriendIDSet.contains(plusFriendID)){
									// 이미 호출 한 적이 있는 플러스친구아이디는 무시 처리한다.
									continue;
								}
								logger.info("@@@ BATCH PLUSID : {} START", plusFriendID);
								serviceResult = newSyncTemplateStatus2(reg_id, plusFriendID, CLINT_COM_ID, local);
								usedPlusFriendIDSet.add(plusFriendID);
								logger.info("@@@ BATCH PLUSID : {} END", plusFriendID);
							}catch (Exception e){
								serviceResult.put("errMsg", "["+plusInfoMap.get("plusFriendId").toString()+"] 알림톡 템플릿 배치 실패 : "+ e.getMessage());
								logger.error(e.toString());
							}
						}

					}
				}
			}
			usedPlusFriendIDSet.clear();
		}catch (Exception ex){
			// LGCNS_PLUSID
			if(StringUtils.isNotEmpty(LGCNS_PLUSID)){
				String[] plusIdArray = LGCNS_PLUSID.split(",");
				for(int i=0; i<plusIdArray.length; i++){
					dbParamMap.put("PLUS_ID",plusIdArray[0].trim());
				}
			}
			logger.warn("!!! 매우중요 : 설정파일에서 알림톡 플러스아이디 정보 읽어옴.");
		}

		return serviceResult;
	}

	/**
	 * 동기화 처리 로직 다시 만듬.
	 * @param reg_id
	 * @param locale
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> newSyncTemplateStatus2(String reg_id, String kkoPlusId, String clientComId, Locale locale) throws Exception {
		Locale loc = (locale == null)?Locale.getDefault():locale;

		Map<String, Object> resultDataMap = new HashMap<String, Object>();
		resultDataMap.put("insCnt", 0);
		resultDataMap.put("uptCnt", 0);

		if ("".equals(clientComId) || "".equals(TEMPLET_AUTHKEY)) {
			resultDataMap.put("errMsg", String.format("%s(Settgings of LGCNS AllimTolk is EMPTY)", messageSource.getMessage("ERR_500", null, loc)));
			return resultDataMap;
		}
		Map<String, String> httpHeadParam = getHttpHeadParam(clientComId + kkoPlusId);
		httpHeadParam.put("ver","2");
		//  동기화 since 설정  처리 한다.
		int searchSince = Integer.parseInt(TEMPLET_SINCE);
		String reqSince = DateUtil.getDate(-1*searchSince,"")+"000000";
		Map<String, Object> postParam = new HashMap<String,Object>();
		postParam.put("since",reqSince);
		postParam.put("page",1);
		postParam.put("senderKeyType","S");
		ResponseBean responseBean = null;
		boolean hasNextPage = true;
		int upCnt = 0;


		// 알림톡 템플릿 상태 승인 동기화 처리를 한다. 만약 UMS DB에 존재 하지 않는 데이타는 수집하여 LGCNS 목록 요청으로 insert 처리 한다.
		List<SyncAltTemplVo> reqUpKkoBizCodes = new ArrayList<SyncAltTemplVo>();
		List<SyncAltTemplVo> reqInKkoBizCodes = new ArrayList<SyncAltTemplVo>();
		while (hasNextPage) {
			String callUrl = LGCNS_HOST + "/api/newSyncTemplateStatus.ajax";
			logger.debug("## call URL : {} ", callUrl);
			responseBean = HttpPoolClient.getInstance().sendPost(callUrl, httpHeadParam, postParam);
			if (!(responseBean.getStatusCode() == 200 || responseBean.getStatusCode() == 201)) {
				// http프로토콜 respons code 실패.
				resultDataMap.put("errMsg", String.format("%s(%s)%s", messageSource.getMessage("ERR_500", null, loc), responseBean.getStatusCode(), responseBean.getBody()));
				return resultDataMap;
			}
			//LGCNS에서 받은 메세지 형태 : {"message":"메시지포탈에서 처리 중 에러가 발생했습니다","data":{},"code":700}
			Map<String, Object> responseMap = gson.fromJson(responseBean.getBody(), HashMap.class);
			String lgcnsResultCode = responseMap.get("code").toString();
			if (lgcnsResultCode.startsWith("200")) { // code값이 어떨때는 Double 숫자이고 어떨때는 String이기 때문 startWith로 처리.
				Map<String,Object> responseDataMap = (Map<String,Object>)responseMap.get("data");
				List<Map<String, Object>> revSyncList = (List<Map<String, Object>>) responseDataMap.get("list");
				String respSince = responseDataMap.get("since").toString();
				hasNextPage = (Boolean) responseDataMap.get("hasNext");

				// 해당 부분에서 DB 동기화 처리. T_UMS_ALLIMTOK_TEMPL 테이블에 KKOBIZCODE를 조건으로 업데이트 하여 카운트가 0으로 UMS DB가 없는 kkoBizCode는 수직하여 다시
				// LGCNS에 목록조회를 요청하여 인설트 처리 한다.
				for(Map<String,Object> upDataMap : revSyncList){
					SyncAltTemplVo syncAltTemplVo = new SyncAltTemplVo();
					syncAltTemplVo.setKKOBIZCODE(upDataMap.get("kkoBizCode").toString());
					try {
						syncAltTemplVo.setAPPROVAL(upDataMap.get("approval").toString());
						syncAltTemplVo.setTEMPLATECODE(upDataMap.get("templateCode").toString());
						syncAltTemplVo.setKKOTEMPLATECODE(upDataMap.get("templateCode").toString());
						if(upDataMap.containsKey("kkoBtnYn")){
							String kkoBtnYn = upDataMap.get("kkoBtnYn").toString().trim();
							if("Y".equals(kkoBtnYn) && upDataMap.containsKey("buttons")){
								syncAltTemplVo.setBUTTONS(upDataMap.get("buttons").toString());
							}
						}
						if (upDataMap.containsKey("comments")) {
							syncAltTemplVo.setCOMMENTS(upDataMap.get("comments").toString());
						}
						syncAltTemplVo.setSINCE(respSince);
						syncAltTemplVo.setPLUS_ID(kkoPlusId);
						reqUpKkoBizCodes.add(syncAltTemplVo);
						upCnt++;
					}catch (Exception e){
						logger.error("LGCNS 템플릿코드 : "+syncAltTemplVo.getKKOBIZCODE()+" 동기화 처리중 에러 발생하여 동기화 예외처리 됨.");
					}
				}
				postParam.put("page", Integer.parseInt(postParam.get("page").toString()) + 1);
			} else {
				logger.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
				logger.error("[카카오알람톡 템플릿 동기화 배치] 플러스아이디 : {}, 에러코드 : {}, 에러내용 : {}",kkoPlusId, lgcnsResultCode, responseMap.get("message"));
				logger.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
				resultDataMap.put("errMsg", String.format("%s(%s)%s", messageSource.getMessage("ERR_500", null, loc), lgcnsResultCode, responseMap.get("message")));
				return resultDataMap;
			}
		}

		// 동기화 처리를 위해 업데이트 처리
		for(SyncAltTemplVo syncAltTemplVo : reqUpKkoBizCodes){
			int applyRow = sqlSessionTemplate.update("mybatis.template.allimtolk.lgcns.upTemplateReg2", syncAltTemplVo);
			if(applyRow==0){
				// 유라클 UMS DB에 존재하지 않아 insert 처리해야함.
				reqInKkoBizCodes.add(syncAltTemplVo);
			}
		}

		// 동기화 처리에서 UMS DB에 존재하지 않는 데이타는 insert 처리 한다.
		for(SyncAltTemplVo syncAltTemplVo : reqInKkoBizCodes){
			httpHeadParam.clear();
			httpHeadParam = getHttpHeadParam(clientComId);
			httpHeadParam.put("ver","3");

			logger.debug("### httpHeadParam2: {}", gson.toJson(httpHeadParam));

			Map<String, Object> param = new HashMap<String, Object>();
			param.put("yellowId", kkoPlusId);
			param.put("kkoBizCode", syncAltTemplVo.getKKOBIZCODE());
			param.put("senderKeyType", syncAltTemplVo.getSENDERKEYTYPE());
			param.put("templateCode", syncAltTemplVo.getKKOTEMPLATECODE());

			String callUrl = LGCNS_HOST + "/api/retrieveSingleTemplate.ajax";
			logger.debug("## call URL : {} ", callUrl);

			responseBean = HttpPoolClient.getInstance().sendPost(callUrl, httpHeadParam, param);

			if (responseBean.getStatusCode() == 200 || responseBean.getStatusCode() == 201) {
				try {
					//LGCNS에서 받은 메세지 형태 : {"message":"메시지포탈에서 처리 중 에러가 발생했습니다","data":{},"code":700}
					Map<String, Object> responseMap = gson.fromJson(responseBean.getBody(), HashMap.class);
					String lgcnsResultCode = responseMap.get("code").toString();
					if (lgcnsResultCode.startsWith("200")) { // code값이 어떨때는 Double 숫자이고 어떨때는 String이기 때문 startWith로 처리.
						Map<String, Object> lgcnsDataMap = (Map<String, Object>) responseMap.get("data");
						// insert 처리
						if(lgcnsDataMap.containsKey("templateName") && lgcnsDataMap.get("templateName")!=null) {
							syncAltTemplVo.setNOTE(lgcnsDataMap.get("templateName").toString());
						}
						if(lgcnsDataMap.containsKey("templateContents") && lgcnsDataMap.get("templateContents")!=null) {
							syncAltTemplVo.setTEMPLATECONTENTS(lgcnsDataMap.get("templateContents").toString());
						}
						if(lgcnsDataMap.containsKey("templateMessageType") && lgcnsDataMap.get("templateMessageType")!=null) {
							syncAltTemplVo.setTEMPLATETYPE(lgcnsDataMap.get("templateMessageType").toString());
						}
						if(lgcnsDataMap.containsKey("templateImageName") && lgcnsDataMap.get("templateImageName")!=null) {
							syncAltTemplVo.setIMAGE_NAME(lgcnsDataMap.get("templateImageName").toString());
						}
						if(lgcnsDataMap.containsKey("templateImageUrl") && lgcnsDataMap.get("templateImageUrl")!=null) {
							syncAltTemplVo.setIMAGE(lgcnsDataMap.get("templateImageUrl").toString());
						}
						if(lgcnsDataMap.containsKey("templateTitle") && lgcnsDataMap.get("templateTitle")!=null) {
							syncAltTemplVo.setTEMPL_TITLE(lgcnsDataMap.get("templateTitle").toString());
						}
						if(lgcnsDataMap.containsKey("templateSubtitle") && lgcnsDataMap.get("templateSubtitle")!=null) {
							syncAltTemplVo.setTEMPL_ADD_TITLE(lgcnsDataMap.get("templateSubtitle").toString());
						}
						if(lgcnsDataMap.containsKey("categoryCode") && lgcnsDataMap.get("categoryCode")!=null) {
							syncAltTemplVo.setTEMPL_CATEGORYCODE(lgcnsDataMap.get("categoryCode").toString());
						}
						if(lgcnsDataMap.containsKey("buttons") && lgcnsDataMap.get("buttons")!=null) {
							syncAltTemplVo.setBUTTONS(lgcnsDataMap.get("buttons").toString());
						}
						sqlSessionTemplate.insert("mybatis.template.allimtolk.lgcns.insTemplateReg2", syncAltTemplVo);


					} else {
						resultDataMap.put("errMsg", String.format("%s(%s)%s", messageSource.getMessage("ERR_500", null, locale), lgcnsResultCode, responseMap.get("message")));
					}
				}catch (Exception e){
					e.printStackTrace();
					logger.error(e.toString());
				}
			} else {	//http프로토콜 respons code 실패.
				resultDataMap.put("errMsg","[retrieveTemplates.ajax call error] code : "+responseBean.getStatusCode());
				logger.error("[retrieveTemplates.ajax call error] code : {}, errmsg : {} ", responseBean.getStatusCode(),responseBean.getStatusCode());
			}
		}
		resultDataMap.put("insCnt",reqInKkoBizCodes.size());
		resultDataMap.put("uptCnt",upCnt);
		return resultDataMap;
	}

}