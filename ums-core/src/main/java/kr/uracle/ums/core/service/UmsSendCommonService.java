package kr.uracle.ums.core.service;

import au.com.bytecode.opencsv.CSVWriter;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import kr.uracle.ums.codec.redis.config.ErrorManager;
import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.core.common.Constants;
import kr.uracle.ums.core.ehcache.FatigueCacheMgr;
import kr.uracle.ums.core.ehcache.RCSTemplateCache;
import kr.uracle.ums.core.exception.RequestErrException;
import kr.uracle.ums.core.service.bean.FatigueBean;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import kr.uracle.ums.core.service.send.kko.BaseKkoAltSendService;
import kr.uracle.ums.core.service.send.kko.BaseKkoFrtSendService;
import kr.uracle.ums.core.service.send.mms.BaseMmsSendService;
import kr.uracle.ums.core.service.send.naver.BaseNaverSendService;
import kr.uracle.ums.core.service.send.push.PushSendService;
import kr.uracle.ums.core.service.send.rcs.BaseRcsSendService;
import kr.uracle.ums.core.service.send.sms.BaseSmsSendService;
import kr.uracle.ums.core.service.send.wpush.WPushSendService;
import kr.uracle.ums.core.vo.ReqUmsSendVo;
import kr.uracle.ums.core.vo.template.RCSTemplateVo;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Y.B.H(mium2) on 2019. 4. 15..
 */
@Service
@SuppressWarnings("unchecked")
public class UmsSendCommonService {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    @Autowired(required = true)
    protected UmsSendService umsSendService;

    @Autowired(required = true)
    protected PushSendService pushSendService;

    @Autowired(required = true)
    protected WPushSendService wpushSendService;

    @Autowired(required = true)
    private RCSTemplateCache rcsTemplateCache;

    @Autowired(required = true)
    private UmsSendMacroService umsSendMacroService;

    @Autowired(required = true)
    private UmsChannelProviderFactory umsChannelProviderFactory;

    @Autowired(required = true)
    private AllotterManager allotterManager;

    @Autowired(required = true)
    private RedisTemplate redisTemplate;

    @Autowired(required = true)
    private RedisTemplate redisTemplate_vo;

    @Autowired(required = true)
    private FatigueCacheMgr fatigueCacheMgr;

    @Autowired(required=true)
    private MessageSource messageSource;
    @Value("${UMS.TEMPDIR:}")
    private String TEMPDIR;
    @Value("${UMS.NAS.YN:N}")
    private String NAS_YN;
    @Value("${UMS.NAS.SRC}")
    private String NAS_SRC;
    @Value("${UMS.NAS.URL:N}")
    private String NAS_URL;
    @Value(("${CALLBACK.PHONENUM:0000000}"))
    private String CALLBACK_PHONENUM;
    @Value(("${RCS.DONOTCALLNUM}"))
    private String RCS_DONOTCALLNUM;

    @Value("${UMS.PUSH.PRIORITY.USE:N}")
    private String PRIORITY_USE;

    @Autowired(required = true)
    protected Gson gson;
    public static final SimpleDateFormat format3 = new SimpleDateFormat("yyyyMMddhhmmss");
    private Set<String> comonVarsSet = new HashSet<String>();
    private HashSet<SendType> phoneNumKeyChannelSet = new HashSet<>();

    public UmsSendCommonService(){
        comonVarsSet.add("#{이름}");
        comonVarsSet.add("#{아이디}");
        comonVarsSet.add("#{핸드폰번호}");
        phoneNumKeyChannelSet.add(SendType.KKOALT);
        phoneNumKeyChannelSet.add(SendType.KKOFRT);
        phoneNumKeyChannelSet.add(SendType.SMS);
        phoneNumKeyChannelSet.add(SendType.LMS);
        phoneNumKeyChannelSet.add(SendType.MMS);
        phoneNumKeyChannelSet.add(SendType.RCS_SMS);
        phoneNumKeyChannelSet.add(SendType.RCS_LMS);
        phoneNumKeyChannelSet.add(SendType.RCS_MMS);
        phoneNumKeyChannelSet.add(SendType.RCS_FREE);
        phoneNumKeyChannelSet.add(SendType.RCS_CELL);
        phoneNumKeyChannelSet.add(SendType.RCS_DESC);
    }

    /**
     * 푸시 발송정보 UmsSendMsgBean 만들기
     *
     */
    public UmsSendMsgBean makeUmsSendMsgBean(ReqUmsSendVo reqUmsSendVo) throws Exception{
        // 공통항목
        UmsSendMsgBean umsSendMsgBean = new UmsSendMsgBean(reqUmsSendVo.getTRANS_TYPE().toString());
        umsSendMsgBean.setSENDERID(reqUmsSendVo.getSENDERID().trim());
        umsSendMsgBean.setSENDGROUPCODE(reqUmsSendVo.getSENDGROUPCODE().trim());
        umsSendMsgBean.setSTART_SEND_KIND(reqUmsSendVo.getSEND_TYPE().toString());
        umsSendMsgBean.setCALLBACK_NUM(CALLBACK_PHONENUM);
        if(StringUtils.isNotBlank(reqUmsSendVo.getCALLBACK_NUM())) umsSendMsgBean.setCALLBACK_NUM(reqUmsSendVo.getCALLBACK_NUM());
        // 발송 수신자 정보 - 배열 혹은 JsonArray 형 String
        umsSendMsgBean.setCUIDS(reqUmsSendVo.getCUIDS().trim());
        // 타겟팅 정보가 CSV일 경우
        umsSendMsgBean.setCSVFILE(reqUmsSendVo.getCSVFILE());
        // TYPE 셋팅(타겟팅 방법 : AU=>전체UMS회원, AP=>전체푸시유저, OU=>전체조직도유저, MU=>UMS 아이디로 타겟팅, MP=>푸시아이디로 타겟팅, NM=>비회원/핸드폰번호필수, MC=>회원CSV, NC=>비회원CSV)
        umsSendMsgBean.setTARGET_USER_TYPE(reqUmsSendVo.getTARGET_USER_TYPE());
        // 메세지 타입 I:정보성메세지 , A:광고성메세지
        String msgType = reqUmsSendVo.getMSG_TYPE();
        umsSendMsgBean.setMSG_TYPE(msgType);
        umsSendMsgBean.setRESERVEDATE(reqUmsSendVo.getRESERVEDATE().trim());
        umsSendMsgBean.setORG_RESERVEDATE(reqUmsSendVo.getORG_RESERVEDATE());

        // 푸시항목
        umsSendMsgBean.setAPP_ID(reqUmsSendVo.getAPP_ID().trim());
        umsSendMsgBean.setPUSH_TYPE(reqUmsSendVo.getPUSH_TYPE());
        umsSendMsgBean.setSERVICECODE(reqUmsSendVo.getSERVICECODE().trim());
        umsSendMsgBean.setSENDERCODE(reqUmsSendVo.getSENDERCODE().trim());
        umsSendMsgBean.setPUSH_TEMPL_ID(reqUmsSendVo.getPUSH_TEMPL_ID());
        umsSendMsgBean.setTITLE(reqUmsSendVo.getTITLE().trim());
        umsSendMsgBean.setPUSH_MSG(reqUmsSendVo.getPUSH_MSG().trim());

        // EXT 정보는 넘어온 정보를 분석하여 형태를 만들어 넘긴다.
        if(StringUtils.isNotBlank(reqUmsSendVo.getEXT_KIND())) {
            String makeExtStr = makeExtInfo(reqUmsSendVo.getEXT_KIND(), reqUmsSendVo.getEXT(), reqUmsSendVo.getIMGFILES(), reqUmsSendVo.getEXT_LINK(), reqUmsSendVo.getEXT_IMGURL(), reqUmsSendVo.getEXT_MOVURL());
            umsSendMsgBean.setEXT(makeExtStr);
        }
        umsSendMsgBean.setATTACHFILE(reqUmsSendVo.getATTACHFILE());
        umsSendMsgBean.setDB_IN(reqUmsSendVo.getDB_IN());
        umsSendMsgBean.setBADGENO(reqUmsSendVo.getBADGENO());
        umsSendMsgBean.setPUSH_FAIL_SMS_SEND(reqUmsSendVo.getPUSH_FAIL_SMS_SEND().trim());
        umsSendMsgBean.setPUSH_FAIL_WAIT_MIN(reqUmsSendVo.getPUSH_FAIL_WAIT_MIN().trim());
        umsSendMsgBean.setSPLIT_MSG_CNT(reqUmsSendVo.getSPLIT_MSG_CNT().trim());
        umsSendMsgBean.setDELAY_SECOND(reqUmsSendVo.getDELAY_SECOND().trim());
        umsSendMsgBean.setPRIORITY(reqUmsSendVo.getPRIORITY().trim());

        // UMS UI페이지와 SDK에서는 LIMITSECOND, LIMITCNT 파라미터로 속도제한 정보를 보낸다.
        if(!"0".equals(reqUmsSendVo.getLIMITSECOND()) && !"".equals(reqUmsSendVo.getLIMITSECOND()) && !"0".equals(reqUmsSendVo.getLIMITCNT()) && !"".equals(reqUmsSendVo.getLIMITCNT())){
            umsSendMsgBean.setDELAY_SECOND(reqUmsSendVo.getLIMITSECOND());
            umsSendMsgBean.setSPLIT_MSG_CNT(reqUmsSendVo.getLIMITCNT());
        }

        //웹푸시 항목
        umsSendMsgBean.setWPUSH_DOMAIN(reqUmsSendVo.getWPUSH_DOMAIN());
        umsSendMsgBean.setWPUSH_TITLE(reqUmsSendVo.getWPUSH_TITLE()); // [선택]제목이 없을 경우 생략가능
        umsSendMsgBean.setWPUSH_MSG(reqUmsSendVo.getWPUSH_MSG()); // [필수]웹푸시 메세지.
        umsSendMsgBean.setWPUSH_TEMPL_ID(reqUmsSendVo.getWPUSH_TEMPL_ID()); // [필수]웹푸시 템플릿아이디.
        umsSendMsgBean.setWPUSH_ICON(reqUmsSendVo.getWPUSH_ICON());
        umsSendMsgBean.setWPUSH_LINK(reqUmsSendVo.getWPUSH_LINK());
        umsSendMsgBean.setWPUSH_EXT(reqUmsSendVo.getWPUSH_EXT()); // {"IMGURL":"https://uracle.co.kr"}
        umsSendMsgBean.setWPUSH_BADGENO(reqUmsSendVo.getWPUSH_BADGENO()); // [선택]푸시 뱃지정보. 숫자만 가능. 0 : 사용안함. 그 이외의 숫자 뱃지정보

        //SMS 항목
        umsSendMsgBean.setSMS_TITLE(reqUmsSendVo.getSMS_TITLE().trim());
        umsSendMsgBean.setSMS_MSG(reqUmsSendVo.getSMS_MSG().trim());
        umsSendMsgBean.setSMS_TEMPL_ID(reqUmsSendVo.getSMS_TEMPL_ID());

        // MMS 이미지 항목
        umsSendMsgBean.setMMS_IMGURL(reqUmsSendVo.getMMS_IMGURL());
        if("Y".equals(NAS_YN)) { // NAS가 있을 경우 만 첨부 가능.
            MultipartFile[] mmsFiles = reqUmsSendVo.getMMS_IMG_FILES();
            List<String> mmsImgPathList = new ArrayList<String>();
            if (ObjectUtils.isNotEmpty(mmsFiles)) {
                for (MultipartFile mmsFile : mmsFiles) {
                    String imagePath = storeImge(mmsFile, null, "MMS");
                    mmsImgPathList.add(imagePath);
                }
                String mmsImgPathJson = gson.toJson(mmsImgPathList);
                umsSendMsgBean.setMMS_IMGURL(mmsImgPathJson);
            }
        }

        //알림톡 항목
        umsSendMsgBean.setKKO_TITLE(reqUmsSendVo.getKKO_TITLE()); // 2022.12.01 알림톡 강조제목 셋팅
        umsSendMsgBean.setALLIMTOLK_TEMPLCODE(reqUmsSendVo.getALLIMTOLK_TEMPLCODE().trim());
        umsSendMsgBean.setKKOALT_SVCID(reqUmsSendVo.getKKOALT_SVCID().trim());
        umsSendMsgBean.setALLIMTALK_MSG(reqUmsSendVo.getALLIMTALK_MSG());
        umsSendMsgBean.setREPLACE_VARS(reqUmsSendVo.getREPLACE_VARS());

        //친구톡 항목
        umsSendMsgBean.setKKOFRT_SVCID(reqUmsSendVo.getKKOFRT_SVCID().trim());
        umsSendMsgBean.setPLUS_ID(reqUmsSendVo.getPLUS_ID().trim());
        umsSendMsgBean.setFRIENDTOLK_MSG(reqUmsSendVo.getFRIENDTOLK_MSG().trim());
        umsSendMsgBean.setFRT_TEMPL_ID(reqUmsSendVo.getFRT_TEMPL_ID());
        umsSendMsgBean.setKKO_BTNS(reqUmsSendVo.getKKO_BTNS());

        // 알림톡/친구톡 이미지 첨부. 알림톡경우 템플릿등록시 카카오에 등록된 이미지 URL 경로가 셋팅됨.setKKO_IMGFILE_PATH
        if("Y".equals(NAS_YN)) { // NAS가 있을 경우 만 첨부 가능.
            if("0".equals(reqUmsSendVo.getRESERVE_SEQNO()) && reqUmsSendVo.getKKO_IMGFILE()!=null) {
                String absImgSrc = storeImge(reqUmsSendVo.getKKO_IMGFILE(), null,"KKOFRT");
                umsSendMsgBean.setKKO_IMG_PATH(absImgSrc);
                umsSendMsgBean.setKKO_IMG_LINK_URL(reqUmsSendVo.getKKO_IMG_LINK_URL());
            }else{
                // 예약발송은 파일은 NAS에 들어 있는 상태이다.
                umsSendMsgBean.setKKO_IMG_PATH(reqUmsSendVo.getKKO_IMGFILE_PATH());
                umsSendMsgBean.setKKO_IMG_LINK_URL(reqUmsSendVo.getKKO_IMG_LINK_URL());
            }
        }

        // RCS 항목
        umsSendMsgBean.setBRAND_ID(reqUmsSendVo.getBRAND_ID());
        umsSendMsgBean.setRCS_TYPE(reqUmsSendVo.getRCS_TYPE());
        umsSendMsgBean.setRCS_MSGBASE_ID(reqUmsSendVo.getRCS_MSGBASE_ID());
        umsSendMsgBean.setRCS_TITLE(reqUmsSendVo.getRCS_TITLE());
        umsSendMsgBean.setRCS_MSG(reqUmsSendVo.getRCS_MSG());
        umsSendMsgBean.setRCS_MMS_INFO(reqUmsSendVo.getRCS_MMS_INFO());
        umsSendMsgBean.setRCS_TEMPL_ID(reqUmsSendVo.getRCS_TEMPL_ID());
        umsSendMsgBean.setIMG_GROUP_KEY(reqUmsSendVo.getIMG_GROUP_KEY());
        umsSendMsgBean.setEXPIRY_OPTION(reqUmsSendVo.getEXPIRY_OPTION());
        if(StringUtils.isNotBlank(reqUmsSendVo.getCOPY_ALLOWED()))umsSendMsgBean.setCOPY_ALLOWED(reqUmsSendVo.getCOPY_ALLOWED());
        // 수신거부전화번호 설정
        if(StringUtils.isNotBlank(RCS_DONOTCALLNUM))umsSendMsgBean.setFOOTER(RCS_DONOTCALLNUM);
        if(msgType.equalsIgnoreCase("A")) {
            String footer = reqUmsSendVo.getFOOTER();
            if( StringUtils.isNotBlank(footer)) {
                umsSendMsgBean.setFOOTER(footer.trim());
            }
        }

        // RCS 파일 처리 - IMG_PATH로 통일하여 처리
        int RCS_MMS_IMG_CNT = 0;
        MultipartFile[] rcsFiles = reqUmsSendVo.getRCS_IMG_FILES();
        List<String> rcsImgPathList = new ArrayList<String>();
        if(ObjectUtils.isNotEmpty(rcsFiles)) {
            for(MultipartFile rcsFile : rcsFiles) {
                String imagePath = storeImge(rcsFile, null, "RCS");
                rcsImgPathList.add(imagePath);
            }
        }

        if(ObjectUtils.isEmpty(rcsImgPathList) && StringUtils.isNotBlank(reqUmsSendVo.getRCS_IMG_PATH())) {
            rcsImgPathList = gson.fromJson(reqUmsSendVo.getRCS_IMG_PATH(), List.class);
        }

        umsSendMsgBean.setIMG_GROUP_CNT(""+RCS_MMS_IMG_CNT);
        if(ObjectUtils.isNotEmpty(rcsImgPathList)) {
            RCS_MMS_IMG_CNT = rcsImgPathList.size();
            umsSendMsgBean.setRCS_IMG_PATH(rcsImgPathList);
            umsSendMsgBean.setRCS_IMG_PATH_JSON(gson.toJson(rcsImgPathList));
        }

        umsSendMsgBean.setIMG_GROUP_CNT(""+RCS_MMS_IMG_CNT);
        umsSendMsgBean.setRCS_TEMPL_ID(reqUmsSendVo.getRCS_TEMPL_ID());
        umsSendMsgBean.setBRAND_ID(reqUmsSendVo.getBRAND_ID());
        umsSendMsgBean.setRCS_OBJECT(reqUmsSendVo.getRCS_OBJECT());
        umsSendMsgBean.setBTN_OBJECT(reqUmsSendVo.getBTN_OBJECT());
        umsSendMsgBean.setRCS_BTN_TYPE(reqUmsSendVo.getRCS_BTN_TYPE());
        umsSendMsgBean.setRCS_BTN_CNT("0");
        //RCS 버튼 갯수
        if(StringUtils.isNotBlank(reqUmsSendVo.getBTN_OBJECT())) {
            try {
                int count = umsSendMacroService.getRCSButtonCount(reqUmsSendVo.getBTN_OBJECT());
                umsSendMsgBean.setRCS_BTN_CNT(""+count);
            } catch (Exception e) {
                throw new Exception("RCS 버튼 포맷 에러"+e.toString());
            }
        }

        // 네이버톡 항목
        umsSendMsgBean.setNAVER_TEMPL_ID(reqUmsSendVo.getNAVER_TEMPL_ID());
        umsSendMsgBean.setNAVER_MSG(reqUmsSendVo.getNAVER_MSG());
        umsSendMsgBean.setNAVER_PROFILE(reqUmsSendVo.getNAVER_PROFILE());
        umsSendMsgBean.setNAVER_BUTTONS(reqUmsSendVo.getNAVER_BUTTONS());
        umsSendMsgBean.setNAVER_PARTNERKEY(reqUmsSendVo.getNAVER_PARTNERKEY());
        umsSendMsgBean.setNAVER_IMGHASH(reqUmsSendVo.getNAVER_IMGHASH());

        //예약발송번호 처리
        if(!"0".equals(reqUmsSendVo.getRESERVE_SEQNO())){
            umsSendMsgBean.setRESERVE_SEQNO(reqUmsSendVo.getRESERVE_SEQNO());
        }

        // 발송 매크로코드 셋팅
        if("".equals(reqUmsSendVo.getSEND_MACRO_CODE())){
            umsSendMsgBean.setSEND_MACRO_CODE("MACRO_001");
        }else{
            umsSendMsgBean.setSEND_MACRO_CODE(reqUmsSendVo.getSEND_MACRO_CODE().trim());
            umsSendMsgBean.setSEND_MACRO_ORDER(reqUmsSendVo.getSEND_MACRO_ORDER());
        }

        // 고객 거래식별고유키 셋팅.1110
        umsSendMsgBean.setCUST_TRANSGROUPKEY(reqUmsSendVo.getCUST_TRANSGROUPKEY());
        umsSendMsgBean.setCUST_TRANSKEY(reqUmsSendVo.getCUST_TRANSKEY());
        umsSendMsgBean.setMIN_START_TIME(reqUmsSendVo.getMIN_START_TIME());
        umsSendMsgBean.setMAX_END_TIME(reqUmsSendVo.getMAX_END_TIME());
        umsSendMsgBean.setFATIGUE_YN(reqUmsSendVo.getFATIGUE_YN());

        // 사용자정의 파라미터 추가
        umsSendMsgBean.setVAR1(reqUmsSendVo.getVAR1().trim());
        umsSendMsgBean.setVAR2(reqUmsSendVo.getVAR2().trim());
        umsSendMsgBean.setVAR3(reqUmsSendVo.getVAR3().trim());
        umsSendMsgBean.setVAR4(reqUmsSendVo.getVAR4().trim());
        umsSendMsgBean.setVAR5(reqUmsSendVo.getVAR5().trim());
        umsSendMsgBean.setVAR6(reqUmsSendVo.getVAR6().trim());
        umsSendMsgBean.setVAR7(reqUmsSendVo.getVAR7().trim());
        umsSendMsgBean.setVAR8(reqUmsSendVo.getVAR8().trim());
        umsSendMsgBean.setVAR9(reqUmsSendVo.getVAR9().trim());

        // UMS-ADMIN 전용(내부용) 필드
        umsSendMsgBean.setADMIN_MSG_ID(reqUmsSendVo.getADMIN_MSG_ID());
        return umsSendMsgBean;
    }
    /**
     * 알림톡 메세지 체크
     * @param umsSendMsgBean
     * @throws Exception
     */
    public void chkAltMsg(UmsSendMsgBean umsSendMsgBean) throws Exception{
        chkAltMsgCsv(umsSendMsgBean,null);
    }
    /**
     * CSV 알림톡 메세지 체크 ==> 변경 원래는 CSV파일의 개별화 변수만 처리 하였으나 파라미터로 넘어온 REPLACE_VARS를 이용하여 치환변수도 체크하는 로직 추가.
     * @param umsSendMsgBean
     * @param varsIndexMap  // CSV파일 발송일 경우 해더 index값
     * @throws Exception
     */
    public void chkAltMsgCsv(UmsSendMsgBean umsSendMsgBean, Map<String,Integer> varsIndexMap) throws Exception{

        Map<String, List<String>> AllimTolkMsgVarCheckMap = getReplaceVars(umsSendMsgBean.getALLIMTALK_MSG());
        List<String> kkoAltPersonalVars = AllimTolkMsgVarCheckMap.get("personalVars");
        if(varsIndexMap == null){// 알림톡톡 메세지 검증 :  #{아이디} #{이름} 이외의 변수가 존재 할 경우 에러처리 한다.
            if (kkoAltPersonalVars.size() > 0) {
                String replaceVars = umsSendMsgBean.getREPLACE_VARS();
                //개별화 치환변수가 메세지 내용에 존재 할 경우 넘어온 파라미터값의 REPLACE_VARS를 비교 검증한다.
                if( StringUtils.isBlank(replaceVars)) throw new RequestErrException("[ALLIM TOLK] 메세지에 치환변수가 존재하나 치환파라미터가 없습니다.");

                try {
                    Map<String, Object> replaceVarMap = gson.fromJson(replaceVars, Map.class);
                    for (String personVar : kkoAltPersonalVars) {
                        if (replaceVarMap.containsKey(personVar) == false) throw new Exception("[ALLIM TOLK] 메세지 내용의 개별화정보가 치환파라미터에 올바르지 않거나 존재하지 않습니다." + personVar);
                    }
                }catch (JsonSyntaxException e){
                    throw new RequestErrException("[ALLIM TOLK] 치환파라미터가 올바르지 않은 JSON데이타 스트링 입니다. "+e.toString());
                }

            }
        }else {// CSV파일 알림톡톡 메세지 검증 :  #{아이디} #{이름} 이외의 변수가 존재 할 경우 에러처리 한다.
            if (kkoAltPersonalVars.size() > 0) {
                for (String personVar : kkoAltPersonalVars) {
                    if (varsIndexMap.containsKey(personVar) == false) throw new Exception("[CSV ALLIM TOLK] 메세지 내용의 개별화정보가 CSV파일에 존재하지 않습니다." + personVar);
                }
            }
        }
    }

    /**
     * 친구톡 메세지 체크
     * @param umsSendMsgBean
     * @throws Exception
     */
    public void chkFrtMsg(UmsSendMsgBean umsSendMsgBean) throws Exception{
        chkFrtMsgCsv(umsSendMsgBean,null);
    }
    /**
     * CSV 친구톡 메세지 체크
     * @param umsSendMsgBean
     * @param varsIndexMap
     * @throws Exception
     */
    public void chkFrtMsgCsv(UmsSendMsgBean umsSendMsgBean, final Map<String,Integer> varsIndexMap) throws Exception{
        // 친구톡 메세지 검증 :  #{아이디} #{이름} 이외의 변수가 존재 할 경우 검증한다.
        Map<String, List<String>> friendMsgVarCheckMap = getReplaceVars(umsSendMsgBean.getFRIENDTOLK_MSG());
        List<String> kkoFrtPersonalVars = friendMsgVarCheckMap.get("personalVars");

        if(varsIndexMap == null){// 친구톡 메세지 검증 :  #{아이디} #{이름} 이외의 변수가 존재 할 경우 에러처리 한다.
            if (kkoFrtPersonalVars.size() > 0) {
                String replaceVars = umsSendMsgBean.getREPLACE_VARS();
                //개별화 치환변수가 메세지 내용에 존재 할 경우 넘어온 파라미터값의 REPLACE_VARS를 비교 검증한다.
                if( StringUtils.isBlank(replaceVars)) throw new RequestErrException("[친구톡] 메세지에 치환변수가 존재하나 치환파라미터가 없습니다.");

                try {
                    Map<String, Object> replaceVarMap = gson.fromJson(replaceVars, Map.class);
                    for (String personVar : kkoFrtPersonalVars) {
                        if (replaceVarMap.containsKey(personVar) == false) throw new Exception("[친구톡] 메세지 내용의 개별화정보가 치환파라미터에 올바르지 않거나 존재하지 않습니다." + personVar);
                    }
                }catch (JsonSyntaxException e){
                    throw new RequestErrException("[친구톡] 치환파라미터가 올바르지 않은 JSON데이타 스트링 입니다. "+e.toString());
                }

            }
        }else {// CSV파일 알림톡톡 메세지 검증 :  #{아이디} #{이름} 이외의 변수가 존재 할 경우 에러처리 한다.
            if (kkoFrtPersonalVars.size() > 0) {
                for (String personVar : kkoFrtPersonalVars) {
                    if (varsIndexMap.containsKey(personVar) == false) throw new Exception("[CSV 친구톡] 메세지 내용의 개별화정보가 CSV파일에 존재하지 않습니다." + personVar);
                }
            }
        }
    }

    /**
     * 친구톡 메세지 체크
     * @param umsSendMsgBean
     * @throws Exception
     */
    public void chkRCSMsg(UmsSendMsgBean umsSendMsgBean) throws Exception{
        chkRcsMsgCsv(umsSendMsgBean,null);
    }

    /**
     * CSV RCS 메세지 체크
     * @param umsSendMsgBean
     * @param varsIndexMap
     * @throws Exception
     */
    public void chkRcsMsgCsv(UmsSendMsgBean umsSendMsgBean, final Map<String,Integer> varsIndexMap) throws Exception{

        String templtId= umsSendMsgBean.getRCS_TEMPL_ID();
        String rcsMsg = StringUtils.isNotBlank(umsSendMsgBean.getRCS_OBJECT())? umsSendMsgBean.getRCS_OBJECT():umsSendMsgBean.getRCS_MSG();
        try{
            SendType.valueOf(umsSendMsgBean.getRCS_TYPE());
        }catch (IllegalArgumentException e){
            throw new Exception("[RCS] RCS TYPE is Not Correct");
        }
        if( StringUtils.isBlank(umsSendMsgBean.getCALLBACK_NUM()) ) throw new RequestErrException("[RCS] not Exist CALLBACK NUMBER");
        if( StringUtils.isBlank(umsSendMsgBean.getBRAND_ID()) ) throw new RequestErrException("[RCS] not Exist BRAND_ID");
        if( StringUtils.isBlank(umsSendMsgBean.getRCS_MSGBASE_ID()) ) throw new RequestErrException("[RCS] not Exist RCS_MSGBASE_ID");

        if(StringUtils.isNotBlank(templtId)) {
            //내부 템플릿은 등록은 RCS SMS, LMS, FREE만 가능하도록 제한 - 스타일,서술형은 RBC 등록, MMS는 템플릿 미지원, 단 모든템플릿은 공통 테이블에서 관리
            RCSTemplateVo rcsTemplateVo = rcsTemplateCache.getTemplate(templtId);
            if(StringUtils.isBlank(rcsMsg))rcsMsg = rcsTemplateVo.getCONTENT();

        }

        // 메시지 치환변수맵 - 공통치환변수와 커스텀(개별)치환변수로 구성됨
        Map<String, List<String>> msgReplaceVarMap = getReplaceVars(rcsMsg);

        // 매시지 치환변수 맵 중 커스텀(개벌)치환변수 리스트
        List<String> personalVarsList = msgReplaceVarMap.get("personalVars");

        //RCS 메시지 내용 중에 개인화 치환변수가 존재 할 경우, 맵핑 정보 유무 확인
        if(personalVarsList.size()>0) {
            if(ObjectUtils.isEmpty(varsIndexMap)) { //CSV에 치환변수 맵핑정보가 없을 경우
                // 메시지 요청 전문 중 치환변수 정보
                String replaceVars = umsSendMsgBean.getREPLACE_VARS();
                // 요청 전문 중 치환별수 필드 값까지 비어 있다면, 메시지내 개별치환변수를 치환할 정보가 없음으로 발송 요청 실패 처리
                if(StringUtils.isBlank(replaceVars)) throw new RequestErrException("[RCS] 메세지에 치환변수가 존재하나 치환파라미터가 없습니다.");
                try {
                    // 요청 전문 치환변수관련 정보를 맵핑맵으로 변경
                    Map<String, Object> replaceVarMap = gson.fromJson(replaceVars, Map.class);
                    // 메시지 내 개별화 치환변수들 중 치환변수 맵핑맵에 없다면 발송 요청 실패 처리
                    for (String personVar : personalVarsList) {
                        if (replaceVarMap.containsKey(personVar) == false) throw new Exception("[RCS] 메세지 내용의 개별화정보가 치환파라미터에 올바르지 않거나 존재하지 않습니다." + personVar);
                    }
                }catch (JsonSyntaxException e){
                    throw new RequestErrException("[RCS] 치환파라미터가 올바르지 않은 JSON데이타 스트링 입니다. "+e.getMessage());
                }
            }else { //CSV에 치환변수 맵핑정보가 있을 경우
                // 메시지 내 개별화 치환변수들 중 치환변수 맵핑맵에 없다면 발송 요청 실패 처리
                if (personalVarsList.size() > 0) {
                    for (String personVar : personalVarsList) {
                        if (varsIndexMap.containsKey(personVar) == false) throw new Exception("[RCS] 메세지 내용의 개별화정보가 CSV파일에 존재하지 않습니다." + personVar);

                    }
                }
            }
        }

    }


    /**
     * SMS 메세지 체크
     * @param umsSendMsgBean
     * @throws Exception
     */
    public void chkSmsMsg(UmsSendMsgBean umsSendMsgBean) throws Exception{
        chkSmsMsgCsv(umsSendMsgBean,null);
    }
    public void chkSmsMsgCsv(UmsSendMsgBean umsSendMsgBean, final Map<String,Integer> varsIndexMap) throws Exception{
        Map<String, List<String>> smsMsgVarCheckMap = getReplaceVars(umsSendMsgBean.getSMS_MSG());
        List<String> smsPersonalVars = smsMsgVarCheckMap.get("personalVars");
        if(varsIndexMap == null){// 친구톡 메세지 검증 :  #{아이디} #{이름} 이외의 변수가 존재 할 경우 에러처리 한다.
            if (smsPersonalVars.size() > 0) {
                String replaceVars = umsSendMsgBean.getREPLACE_VARS();
                //개별화 치환변수가 메세지 내용에 존재 할 경우 넘어온 파라미터값의 REPLACE_VARS를 비교 검증한다.
                if( StringUtils.isBlank(replaceVars)) throw new RequestErrException("[SMS] 메세지에 치환변수가 존재하나 치환파라미터가 없습니다.");

                try {
                    Map<String, Object> replaceVarMap = gson.fromJson(replaceVars, Map.class);
                    for (String personVar : smsPersonalVars) {
                        if (replaceVarMap.containsKey(personVar) == false) throw new Exception("[SMS] 메세지 내용의 개별화정보가 치환파라미터에 올바르지 않거나 존재하지 않습니다." + personVar);
                    }
                }catch (JsonSyntaxException e){
                    throw new RequestErrException("[SMS] 치환파라미터가 올바르지 않은 JSON데이타 스트링 입니다. "+e.toString());
                }

            }
        }else {// CSV파일 알림톡톡 메세지 검증 :  #{아이디} #{이름} 이외의 변수가 존재 할 경우 에러처리 한다.
            if (smsPersonalVars.size() > 0) {
                for (String personVar : smsPersonalVars) {
                    if (varsIndexMap.containsKey(personVar) == false) throw new Exception("[SMS CSV] 메세지 내용의 개별화정보가 CSV파일에 존재하지 않습니다." + personVar);
                }
            }
        }
        if("".equals(umsSendMsgBean.getCALLBACK_NUM())){
            throw new RequestErrException("[SMS] not Exist CALLBACK NUMBER");
        }
    }


    /**
     * 네이버톡 메세지 체크
     * @param umsSendMsgBean
     * @throws Exception
     */
    public void chkNaverMsg(UmsSendMsgBean umsSendMsgBean) throws Exception{
        chkNaverMsgCsv(umsSendMsgBean,null);
    }
    /**
     * 네이버톡 메세지 체크 ==> 변경 원래는 CSV파일의 개별화 변수만 처리 하였으나 파라미터로 넘어온 REPLACE_VARS를 이용하여 치환변수도 체크하는 로직 추가.
     * @param umsSendMsgBean
     * @param varsIndexMap  // CSV파일 발송일 경우 해더 index값
     * @throws Exception
     */
    public void chkNaverMsgCsv(UmsSendMsgBean umsSendMsgBean, Map<String,Integer> varsIndexMap) throws Exception{

        Map<String, List<String>> naverMsgVarCheckMap = getReplaceVars(umsSendMsgBean.getNAVER_MSG());
        List<String> naverPersonalVars = naverMsgVarCheckMap.get("personalVars"); // 메세지 내용에 존재하는 치화변수
        if(varsIndexMap == null){// 네이버톡 메세지 검증 :  #{아이디} #{이름} 이외의 변수가 존재 할 경우 에러처리 한다.
            if (naverPersonalVars.size() > 0) {
                String replaceVars = umsSendMsgBean.getREPLACE_VARS();
                //개별화 치환변수가 메세지 내용에 존재 할 경우 넘어온 파라미터값의 REPLACE_VARS를 비교 검증한다.
                if( StringUtils.isBlank(replaceVars)) throw new RequestErrException("[네이버톡] 메세지에 치환변수가 존재하나 치환파라미터가 없습니다.");

                try {
                    Map<String, Object> replaceVarMap = gson.fromJson(replaceVars, Map.class);
                    for (String personVar : naverPersonalVars) {
                        if (replaceVarMap.containsKey(personVar) == false) throw new Exception("[네이버톡] 메세지 내용의 개별화정보가 치환파라미터에 올바르지 않거나 존재하지 않습니다." + personVar);
                    }
                }catch (JsonSyntaxException e){
                    throw new RequestErrException("[네이버톡] 치환파라미터가 올바르지 않은 JSON데이타 스트링 입니다. "+e.toString());
                }

            }
        }else {// CSV파일 알림톡톡 메세지 검증 :  #{아이디} #{이름} 이외의 변수가 존재 할 경우 에러처리 한다.
            if (naverPersonalVars.size() > 0) {
                for (String personVar : naverPersonalVars) {
                    if (varsIndexMap.containsKey(personVar) == false) throw new Exception("[CSV 네이버톡] 메세지 내용의 개별화정보가 CSV파일에 존재하지 않습니다." + personVar);
                }
            }
        }

        if(StringUtils.isBlank(umsSendMsgBean.getNAVER_PROFILE())) throw new RequestErrException("[네이버톡] not Exist Naver Profile");
        if(StringUtils.isBlank(umsSendMsgBean.getCALLBACK_NUM())) throw new RequestErrException("[네이버톡] not Exist CALLBACK NUMBER");
    }


    /**
     * 웹푸시/푸시 레디스 푸시가입 사용자 조회 공통
     * @param umsSendMsgBean
     * @return
     * @throws Exception
     */
    public Map<String,Object> chkPushUser(final UmsSendMsgBean umsSendMsgBean) throws Exception{
        try {
            Map<String, Object> returnDataMap = new HashMap<String, Object>();
            Map<String, List<String>> chkUserObj = umsSendMsgBean.getTARGET_USERS(); //{"아이디":["핸드폰번호","이름"],....}
            String[] chkCuidArr = new String[chkUserObj.size()];
            Set<String> cuidSet = chkUserObj.keySet();
            cuidSet.toArray(chkCuidArr);
            // 레디스 푸시/웹푸시 앱아이디 조회키 셋팅
            String REDIS_APPID_KEY = umsSendMsgBean.getAPP_ID();
            if(SendType.WPUSH == SendType.valueOf(umsSendMsgBean.getSTART_SEND_KIND())){
                REDIS_APPID_KEY = umsSendMsgBean.getWPUSH_DOMAIN();
            }

            List<String> redis_pushUsers = redisTemplate.opsForHash().multiGet(REDIS_APPID_KEY + Constants.REDIS_CUID_TABLE, cuidSet);

            Map<String, List<String>> regPushUserObj = new HashMap<String, List<String>>();
            Map<String, List<String>> notRegPushUserObj = new HashMap<String, List<String>>();

            if (redis_pushUsers != null) {
                for (int i = 0; i < chkCuidArr.length; i++) {
                    if (redis_pushUsers.get(i) == null) {
                        //푸시 가입되지 않은 사용자
                        notRegPushUserObj.put(chkCuidArr[i], chkUserObj.get(chkCuidArr[i]));
                    } else {
                        //푸시 가입된 사용자
                        regPushUserObj.put(chkCuidArr[i], chkUserObj.get(chkCuidArr[i]));
                    }
                }
            } else { // 레디스에서 NULL을 줄 경우는 없을 것으로 보이나.. null일 경우 모든 유저를 비푸쉬유저로 등록처리.
                for (int i = 0; i < chkCuidArr.length; i++) {
                    notRegPushUserObj.put(chkCuidArr[i], chkUserObj.get(chkCuidArr[i]));
                }
            }
            returnDataMap.put("regPushUserObj", regPushUserObj);
            returnDataMap.put("notRegPushUserObj", notRegPushUserObj);
            return returnDataMap;
        }catch (Exception e){
            e.printStackTrace();
            throw new Exception(e);
        }
    }

    public Map<String,Object> chkPushUserNotUseRedis(final UmsSendMsgBean umsSendMsgBean) throws Exception{
        try {
            Map<String, Object> returnDataMap = new HashMap<String, Object>();
            Map<String, List<String>> chkUserObj = umsSendMsgBean.getTARGET_USERS(); //{"아이디":["핸드폰번호","이름"],....}
            Map<String, List<String>> notRegPushUserObj = new HashMap<String, List<String>>();
            returnDataMap.put("regPushUserObj", chkUserObj);
            returnDataMap.put("notRegPushUserObj", notRegPushUserObj);
            return returnDataMap;
        }catch (Exception e){
            e.printStackTrace();
            throw new Exception(e);
        }
    }

    public Map<String,List<String>> getReplaceVars(String checkMsg){
        List<String> matchList = new ArrayList<String>();
        List<String> commonVars = new ArrayList<String>();
        List<String> personalVars = new ArrayList<String>();
//        String regexp = "#\\{([가-힣]+)\\}"; // 한글 검사패턴
        String regexp = "#\\{[\\S]+\\}";
        Pattern pattern  =  Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(checkMsg);
        while(matcher.find()){
            matchList.add(matcher.group());
            if(comonVarsSet.contains(matcher.group())){
                commonVars.add(matcher.group());
            }else{
                personalVars.add(matcher.group());
            }

        }
        Map<String,List<String>> replaceVarInfoMap = new HashMap<String, List<String>>();
        replaceVarInfoMap.put("commonVars",commonVars);
        replaceVarInfoMap.put("personalVars",personalVars);
        return replaceVarInfoMap;
    }

    public int inValidMobileNum(Map<String,List<String>> notRegPushUserMap){
        int mobileFailCnt = 0;
        if(notRegPushUserMap!=null){
            Set<String>cuids = notRegPushUserMap.keySet();
            for(String cuid : cuids){
                List<String> userInfos = notRegPushUserMap.get(cuid);
                if(userInfos==null || userInfos.size()==0){
                    mobileFailCnt++;
                }else{
                    try{
                        String mobileNum = userInfos.get(0);
                        if("".equals(mobileNum.trim())){
                            mobileFailCnt++;
                        }
                    }catch (Exception e){
                        mobileFailCnt++;
                    }
                }
            }
        }
        return mobileFailCnt;
    }

    public boolean dateCheck(final String date, final String format){
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
            simpleDateFormat.setLenient(false);
            simpleDateFormat.format(simpleDateFormat.parse(date));
            return true;
        }catch (Exception e){
            return false;
        }
    }

    public String makeCsv(Map<String,List<String>> dataMap) throws Exception{
        TEMPDIR = TEMPDIR.trim();
        String chkLastStr = TEMPDIR.substring(TEMPDIR.length()-1);

        final File tempDir = new File(TEMPDIR);
        if (!tempDir.exists()) {
            boolean makeResult = tempDir.mkdirs();
        }
        UUID csvUUID = UUID.randomUUID();
        String csvFileName = csvUUID.toString();
        String fullFileName = TEMPDIR + csvFileName;
        if(!"/".equals(chkLastStr)){
            fullFileName = TEMPDIR + "/" + csvFileName+".csv";
        }

        CSVWriter cw = new CSVWriter(new FileWriter(fullFileName), ',', '"');

        Set<String> cuidSet = dataMap.keySet();
        try {
            for(String cuid : cuidSet){
                String[] makeCellArr = new String[2];
                cw.writeNext(makeCellArr);
            }
        } finally {
            cw.close();
        }
        return fullFileName;
    }

    public String storeImge (final  MultipartFile IMGFILES, String absPath, String channelName) throws Exception{
        if(StringUtils.isEmpty(absPath))absPath = NAS_SRC;
        if(absPath.endsWith("/") == false) absPath +=File.separator;
        absPath = absPath + channelName + File.separator;
        final File nasDir = new File(absPath);
        if (!nasDir.exists()) {
            boolean makeResult = nasDir.mkdirs();
        }

        // 이미지 파일을 NAS에 업로드
        String fileName = String.format("%s_%s", format3.format(new Date()), IMGFILES.getOriginalFilename());

        String absFileSrc = String.format("%s%s", absPath, fileName);
        try(InputStream is = IMGFILES.getInputStream(); FileOutputStream fos = new FileOutputStream(absFileSrc)){
            int readLength = 0;
            byte[] buffer = new byte[1024];
            while((readLength = is.read(buffer)) > 0) {
                fos.write(buffer, 0, readLength);
            }
            fos.flush();
        }catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
            throw new Exception(e.toString());
        }

        return absFileSrc;
    }

    public String makeExtInfo(final String EXT_KIND, final String EXT, final  MultipartFile IMGFILES, final String EXT_LINK, final String EXT_IMGURL, final String EXT_MOVURL){
        String makeExtStr = "";
        String imagesUrl = EXT_IMGURL;
        // 이미지가 Multipart파일일 경우
        if(IMGFILES!=null){
            final File nasDir = new File(NAS_SRC);
            if (!nasDir.exists()) {
                boolean makeResult = nasDir.mkdirs();
            }

            // 이미지 파일을 NAS에 업로드
            String fileName = String.format("%s_%s", format3.format(new Date()), IMGFILES.getOriginalFilename());
            FileOutputStream fos = null;
            InputStream is = null;
            try {
                is = IMGFILES.getInputStream();
                if("/".equals(NAS_SRC.substring(NAS_SRC.length()-1))){
                    fos = new FileOutputStream(String.format("%s%s", NAS_SRC, fileName));
                }else {
                    fos = new FileOutputStream(String.format("%s/%s", NAS_SRC, fileName));
                }

                int readLength = 0;
                byte[] buffer = new byte[1024];
                while((readLength = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, readLength);
                }
                fos.flush();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                try { fos.close(); } catch (Exception e) { }
            }

            if("/".equals(NAS_URL.substring(NAS_URL.length()-1))){
                imagesUrl = String.format("%s%s", NAS_URL, fileName);
            }else {
                imagesUrl = String.format("%s/%s", NAS_URL, fileName);
            }
        }

        if(EXT_KIND.equals("I")){
            // 이미지 일 경우 처리
            makeExtStr = messageFormat(EXT,"8",EXT_MOVURL,imagesUrl,EXT_LINK);
        }else if(EXT_KIND.equals("M")){
            //동영상일 경우 처리
            makeExtStr = messageFormat(EXT,"7",EXT_MOVURL,imagesUrl,EXT_LINK);
        }else if(EXT_KIND.equals("D")){
            makeExtStr = messageFormat(EXT,"0",EXT_MOVURL,imagesUrl,EXT_LINK);
        }else{
            if(!"".equals(EXT) || !"".equals(EXT_LINK)) {
                makeExtStr = messageFormat(EXT,"6",EXT_MOVURL,imagesUrl,EXT_LINK);
            }
        }
        return makeExtStr;
    }

    private String messageFormat(String ext, String category, String videoUrl, String imageUrl, String imageLink){
        String result = "";
        switch(Integer.parseInt(category)){
            case 6:
                //UMS 기본
                result = category+"|"+ext+ "|" +imageLink;
                break;
            case 7:
                //UMS 동영상
                result = category+"|"+ext+"|"+imageUrl+"|"+videoUrl + "|" +imageLink;
                break;
            case 8:
                //UMS 이미지
                result = category+"|"+ext+"|"+imageUrl+ "|" +imageLink;
                break;
            default:
                result = ext;
                break;
        }

        return result;
    }

    public void processTempMsgUploadedFile(ReqUmsSendVo reqUmsSendVo) {
        // 저장된 메시지 발송 시 사전 업로드 된 파일을 사용하는 경우에 대한 처리
        if((reqUmsSendVo.getCSVFILE() == null || reqUmsSendVo.getCSVFILE().isEmpty())
                && StringUtils.isNotBlank(reqUmsSendVo.getCSV_FILE_PATH())) {
            MultipartFile multipartFile = getTempMsgUploadedFile("CSVFILE", reqUmsSendVo.getCSV_FILE_PATH());
            if(multipartFile != null) {
                reqUmsSendVo.setCSVFILE(multipartFile);
            }
        }
        if((reqUmsSendVo.getKKO_IMGFILE() == null || reqUmsSendVo.getKKO_IMGFILE().isEmpty())
                && StringUtils.isNotBlank(reqUmsSendVo.getKKO_IMGFILE_PATH())) {
            MultipartFile multipartFile = getTempMsgUploadedFile("KKO_IMGFILE", reqUmsSendVo.getKKO_IMGFILE_PATH());
            if(multipartFile != null) {
                reqUmsSendVo.setKKO_IMGFILE(multipartFile);
            }
        }
        if((reqUmsSendVo.getIMGFILES() == null || reqUmsSendVo.getIMGFILES().isEmpty()) && StringUtils.isNotBlank(reqUmsSendVo.getEXT_IMGFILE_PATH())) {
            MultipartFile multipartFile = getTempMsgUploadedFile("IMGFILES", reqUmsSendVo.getEXT_IMGFILE_PATH());
            if(multipartFile != null) {
                reqUmsSendVo.setIMGFILES(multipartFile);
            }
        }
    }

    protected MultipartFile getTempMsgUploadedFile(String fieldName, String filename) {
        DiskFileItemFactory factory = new DiskFileItemFactory();
        FileItem fileItem = factory.createItem(fieldName, "binary/octet-stream", false, filename);
        String filepath = String.format("%s/tempmsg/%s", NAS_SRC, filename);
        File file = new File(filepath);
        if(!file.exists()) {
            return null;
        }

        try (InputStream is = new FileInputStream(file);
             OutputStream os = fileItem.getOutputStream()
        ) {
            IOUtils.copy(is, os);
        } catch (Exception e) {
            return null;
        }

        return new CommonsMultipartFile(fileItem);
    }


    // 대체발송 채널이 핸드폰번호를 이용한 채널여부
    public boolean isPhoneNumNeeds(Set<SendType> sendTypeSet){
        boolean returnValue = false;
        if(sendTypeSet!=null && sendTypeSet.size()>0){
            for(SendType sendType : sendTypeSet){
                if(phoneNumKeyChannelSet.contains(sendType)){
                    return true;
                }
            }
        }
        return returnValue;
    }

    protected boolean setPushFailSendRetryCnt(UmsSendMsgBean umsSendMsgBean, Set<SendType> failRetrySendTypeSet, int failSendCnt){
        boolean isDaecheSend = false;
        if(failRetrySendTypeSet!=null && failRetrySendTypeSet.size()>0){
            isDaecheSend = true;
            for(SendType sendType : failRetrySendTypeSet){
                if(sendType==SendType.PUSH){ // 혹시 매크로 설정을 잘못하여 PUSH>PUSH 이런식일 경우 무한루프에 빠질수 있어 방어코드 넣음.
                    continue;
                }
                switch (sendType){
                    case WPUSH:
                        umsSendMsgBean.setWPUSH_SEND_CNT(failSendCnt);
                        break;
                    case KKOALT:
                        umsSendMsgBean.setALLIMTOLK_CNT(failSendCnt);
                        break;
                    case KKOFRT:
                        umsSendMsgBean.setFRIENDTOLK_CNT(failSendCnt);
                        break;
                    case SMS:
                        umsSendMsgBean.setSMS_CNT(failSendCnt);
                        break;
                    case LMS:
                        umsSendMsgBean.setLMS_CNT(failSendCnt);
                        break;
                    case MMS:
                        umsSendMsgBean.setMMS_CNT(failSendCnt);
                        break;
                    case RCS_SMS:
                        umsSendMsgBean.setRCS_SMS_CNT(failSendCnt);
                        break;
                    case RCS_LMS:
                        umsSendMsgBean.setRCS_LMS_CNT(failSendCnt);
                        break;
                    case RCS_MMS:
                        umsSendMsgBean.setRCS_MMS_CNT(failSendCnt);
                        break;
                    case RCS_FREE:
                        umsSendMsgBean.setRCS_FREE_CNT(failSendCnt);
                        break;
                    case RCS_CELL:
                        umsSendMsgBean.setRCS_CELL_CNT(failSendCnt);
                        break;
                    case RCS_DESC:
                        umsSendMsgBean.setRCS_DESC_CNT(failSendCnt);
                        break;
                    case NAVERT:
                        umsSendMsgBean.setNAVERT_CNT(failSendCnt);
                        break;

                }
            }
        }
        return isDaecheSend;
    }

    protected boolean setWPushFailSendRetryCnt(UmsSendMsgBean umsSendMsgBean, Set<SendType> failRetrySendTypeSet, int failSendCnt){
        boolean isDaecheSend = false;
        if(failRetrySendTypeSet!=null && failRetrySendTypeSet.size()>0){
            isDaecheSend = true;
            for(SendType sendType : failRetrySendTypeSet){
                if(sendType==SendType.WPUSH){ // 혹시 매크로 설정을 잘못하여 PUSH>PUSH 이런식일 경우 무한루프에 빠질수 있어 방어코드 넣음.
                    continue;
                }
                switch (sendType){
                    case PUSH:
                        umsSendMsgBean.setPUSH_SEND_CNT(failSendCnt);
                        break;
                    case KKOALT:
                        umsSendMsgBean.setALLIMTOLK_CNT(failSendCnt);
                        break;
                    case KKOFRT:
                        umsSendMsgBean.setFRIENDTOLK_CNT(failSendCnt);
                        break;
                    case SMS:
                        umsSendMsgBean.setSMS_CNT(failSendCnt);
                        break;
                    case LMS:
                        umsSendMsgBean.setLMS_CNT(failSendCnt);
                        break;
                    case MMS:
                        umsSendMsgBean.setMMS_CNT(failSendCnt);
                        break;
                    case RCS_SMS:
                        umsSendMsgBean.setRCS_SMS_CNT(failSendCnt);
                        break;
                    case RCS_LMS:
                        umsSendMsgBean.setRCS_LMS_CNT(failSendCnt);
                        break;
                    case RCS_MMS:
                        umsSendMsgBean.setRCS_MMS_CNT(failSendCnt);
                        break;
                    case RCS_FREE:
                        umsSendMsgBean.setRCS_FREE_CNT(failSendCnt);
                        break;
                    case RCS_CELL:
                        umsSendMsgBean.setRCS_CELL_CNT(failSendCnt);
                        break;
                    case RCS_DESC:
                        umsSendMsgBean.setRCS_DESC_CNT(failSendCnt);
                        break;
                    case NAVERT:
                        umsSendMsgBean.setNAVERT_CNT(failSendCnt);
                        break;

                }
            }
        }
        return isDaecheSend;
    }
    /**
     * 중요 : 이곳은 대체발송이므로 무조건 한껀씩 넘어오고 TARGET_USERS 정보가 있다. 바로 해당 채널
     * @param umsSendMsgBean
     * @param notRegPushUserMap
     * @param sendType
     * @throws Exception
     */
    protected void sendDaeche(UmsSendMsgBean umsSendMsgBean,Map<String,List<String>> notRegPushUserMap, SendType sendType) throws Exception{
        Map<String,Map<String,String>> cuidVarMap = null;
        if(StringUtils.isNotBlank(umsSendMsgBean.getREPLACE_VARS())){
            cuidVarMap = new HashMap<String, Map<String, String>>();
            Map<String,Object> replaceVarsMap = gson.fromJson(umsSendMsgBean.getREPLACE_VARS(),Map.class);
            Map<String,String> varMap = new HashMap<String,String>();
            for(String varKey : replaceVarsMap.keySet()){
                varMap.put(varKey,replaceVarsMap.get(varKey).toString());
            }
            for(String reqSendCuid : notRegPushUserMap.keySet()){
                cuidVarMap.put(reqSendCuid,varMap);
            }
        }
        switch (sendType){
            case PUSH:
                try {
                    // 푸시/웹푸시 대체발송일 경우 다음 대체발송채널을 구해서 넣어줘야 한다.
                    Set<SendType> nextSendChannels = umsSendMacroService.getNextSendChannelSet(umsSendMsgBean, SendType.PUSH);
                    umsSendMsgBean.setFAIL_RETRY_SENDTYPE(nextSendChannels);
                }catch (Exception e){
                    umsSendMsgBean.setFAIL_RETRY_SENDTYPE(null);
                    logger.info("대체발송 푸시발송시 다음 대체발송 구하는 중 에러 : {}",e.toString());
                }
                pushSendService.pushMapListSend(notRegPushUserMap,umsSendMsgBean, cuidVarMap, true);
                break;

            case WPUSH:
                try {
                    // 푸시/웹푸시 대체발송일 경우 다음 대체발송채널을 구해서 넣어줘야 한다.
                    Set<SendType> nextSendChannels = umsSendMacroService.getNextSendChannelSet(umsSendMsgBean, SendType.WPUSH);
                    umsSendMsgBean.setFAIL_RETRY_SENDTYPE(nextSendChannels);
                }catch (Exception e){
                    umsSendMsgBean.setFAIL_RETRY_SENDTYPE(null);
                    logger.info("대체발송 웹푸시발송시 다음 대체발송 구하는 중 에러 : {}",e.toString());
                }
                wpushSendService.wpushMapListSend(notRegPushUserMap,umsSendMsgBean, cuidVarMap, true);
                break;

            case KKOALT:
                for(Entry<String, List<String>> e : notRegPushUserMap.entrySet()) {
                    String userId = e.getKey();
                    List<String> userInfos = e.getValue();
                    String mobileNum =userInfos.get(0);

                    String provider = allotterManager.getProvider(sendType.toString(), mobileNum);
                    BaseKkoAltSendService sendService = umsChannelProviderFactory.getKkoAltProviderService(provider);
                    sendService.umsKkoAllimTolkSend(Collections.singletonMap(userId, userInfos), umsSendMsgBean, cuidVarMap, true);
                }
                //getKkoAltProviderService().umsKkoAllimTolkSend(notRegPushUserMap, umsSendMsgBean, cuidVarMap, true);
                break;
            case KKOFRT:
                for(Entry<String, List<String>> e : notRegPushUserMap.entrySet()) {
                    String userId = e.getKey();
                    List<String> userInfos = e.getValue();
                    String mobileNum =userInfos.get(0);

                    String provider = allotterManager.getProvider(sendType.toString(), mobileNum);
                    BaseKkoFrtSendService sendService = umsChannelProviderFactory.getKkoFrtProviderService(provider);
                    sendService.umsKkoFriendTolkSend(Collections.singletonMap(userId, userInfos), umsSendMsgBean, cuidVarMap, true);
                }
                //getKkoFrtProviderService().umsKkoFriendTolkSend(notRegPushUserMap,umsSendMsgBean, cuidVarMap, true);
                break;
            case SMS:
                for(Entry<String, List<String>> e : notRegPushUserMap.entrySet()) {
                    String userId = e.getKey();
                    List<String> userInfos = e.getValue();
                    String mobileNum =userInfos.get(0);

                    String provider = allotterManager.getProvider(sendType.toString(), mobileNum);
                    BaseSmsSendService sendService = umsChannelProviderFactory.getSmsProviderService(provider);
                    sendService.umsSmsSend(Collections.singletonMap(userId, userInfos), umsSendMsgBean, cuidVarMap, true);
                }
                //getSmsProviderService().umsSmsSend(notRegPushUserMap, umsSendMsgBean, cuidVarMap, true);
                break;
            case LMS: case MMS:
                for(Entry<String, List<String>> e : notRegPushUserMap.entrySet()) {
                    String userId = e.getKey();
                    List<String> userInfos = e.getValue();
                    String mobileNum =userInfos.get(0);

                    String provider = allotterManager.getProvider(sendType.toString(), mobileNum);
                    BaseMmsSendService sendService = umsChannelProviderFactory.getMmsProviderService(provider);
                    sendService.umsMmsSend(Collections.singletonMap(userId, userInfos), umsSendMsgBean, cuidVarMap, true);
                }
                //getMmsProviderService().umsMmsSend(notRegPushUserMap,umsSendMsgBean, cuidVarMap, true);
                break;
            case RCS_SMS: case RCS_LMS: case RCS_MMS: case RCS_FREE: case RCS_CELL: case RCS_DESC:
                for(Entry<String, List<String>> e : notRegPushUserMap.entrySet()) {
                    String userId = e.getKey();
                    List<String> userInfos = e.getValue();
                    String mobileNum =userInfos.get(0);

                    String provider = allotterManager.getProvider(sendType.toString(), mobileNum);
                    BaseRcsSendService sendService = umsChannelProviderFactory.getRcsProviderService(provider);
                    sendService.umsSend(Collections.singletonMap(userId, userInfos), umsSendMsgBean, cuidVarMap, true);
                }
                //getRCSProviderService().umsSend(notRegPushUserMap, umsSendMsgBean, cuidVarMap, true);
                break;
            case NAVERT:
                for(Entry<String, List<String>> e : notRegPushUserMap.entrySet()) {
                    String userId = e.getKey();
                    List<String> userInfos = e.getValue();
                    String mobileNum =userInfos.get(0);

                    String provider = allotterManager.getProvider(sendType.toString(), mobileNum);
                    BaseNaverSendService sendService = umsChannelProviderFactory.getNaverProviderService(provider);
                    sendService.umsNaverSend(Collections.singletonMap(userId, userInfos), umsSendMsgBean, cuidVarMap, true);
                }
                //getNaverProviderService().umsNaverSend(notRegPushUserMap, umsSendMsgBean, cuidVarMap, true);
                break;
        }
    }


    public void setRspChannelSendCnt(final UmsSendMsgBean umsSendMsgBean, Map<String,Object> returnResultMap){
        if(umsSendMsgBean.getRESERVE_SEQNO().equals("0") == false){
            returnResultMap.put("RESERVE_SEQNO",""+umsSendMsgBean.getRESERVE_SEQNO());
        }else{
            returnResultMap.put("TRANS_TYPE",umsSendMsgBean.getTRANS_TYPE()); // REAL or BATCH
            returnResultMap.put("UMS_SEQNO",""+umsSendMsgBean.getUMS_SEQNO());
        }
        long total =0;
        for(SendType sendType : SendType.values()) {
            int count  = umsSendMsgBean.getChannelSendCount(sendType);
            total += count;
            returnResultMap.put(sendType.toString(), ""+ count);
        }
        returnResultMap.put("SEND_CNT",""+total);
        returnResultMap.put("FAIL_CNT",""+umsSendMsgBean.getFAIL_CNT());
    }

    public boolean chkFatigue(String cuid) {
        int dLimitCnt = 9999999;
        int wLimitCnt = 9999999;
        int mLimitCnt = 9999999;
        boolean isSendAble = true;
        try {
            // 피로도 설정 정보 가져옴.
            Map<String,Double> fartigueMap = fatigueCacheMgr.geFatigue();
            if(fartigueMap.size()==0){
                return true;
            }else{
                if(fartigueMap.containsKey("day")) {
                    if(fartigueMap.get("day")>0) {
                        dLimitCnt = fartigueMap.get("day").intValue();
                    }
                }
                if(fartigueMap.containsKey("week")) {
                    if(fartigueMap.get("week")>0) {
                        wLimitCnt = fartigueMap.get("week").intValue();
                    }
                }
                if(fartigueMap.containsKey("month")) {
                    if(fartigueMap.get("month")>0) {
                        mLimitCnt = fartigueMap.get("month").intValue();
                    }
                }
            }

            Object fatigueObj = redisTemplate_vo.opsForHash().get(Constants.REDIS_FATIGUE, cuid);
            if (fatigueObj != null) {
                String fatigueInfoStr = fatigueObj.toString();
                String[] fatigueInfoStrArr = fatigueInfoStr.split(",");
                String[] dayInfoArr = fatigueInfoStrArr[0].split(":");
                String[] weekInfoArr = fatigueInfoStrArr[1].split(":");
                String[] monthInfoArr = fatigueInfoStrArr[2].split(":");

                FatigueBean fatigueBean = new FatigueBean();
                fatigueBean.setdExpire(Long.parseLong(dayInfoArr[0]));
                fatigueBean.setdSendCnt(Integer.parseInt(dayInfoArr[1]));

                fatigueBean.setwExpire(Long.parseLong(weekInfoArr[0]));
                fatigueBean.setwSendCnt(Integer.parseInt(weekInfoArr[1]));

                fatigueBean.setmExpire(Long.parseLong(monthInfoArr[0]));
                fatigueBean.setmSendCnt(Integer.parseInt(monthInfoArr[1]));

                long nowCurTimeMillis = System.currentTimeMillis();

                // Day 피로도체크
                long dExpireTimeMillis = fatigueBean.getdExpire();
                int dSendCnt = fatigueBean.getdSendCnt();
                if (dExpireTimeMillis < nowCurTimeMillis) {
                    // Day 피로도 만료된 경우 발송카운트 초기화
                    dSendCnt = 0;
                }

                // Week 피로도체크
                long wExpireTimeMillis = fatigueBean.getwExpire();
                int wSendCnt = fatigueBean.getwSendCnt();
                if (wExpireTimeMillis < nowCurTimeMillis) {
                    // Week 피로도 만료된 경우 발송카운트 초기화
                    wSendCnt = 0;
                }

                // Month 피로도체크
                long mExpireTimeMillis = fatigueBean.getmExpire();
                int mSendCnt = fatigueBean.getmSendCnt();
                if (mExpireTimeMillis < nowCurTimeMillis) {
                    // Month 피로도 만료된 경우 발송카운트 초기화
                    mSendCnt = 0;
                }

                if (dLimitCnt <= dSendCnt || wLimitCnt <= wSendCnt || mLimitCnt <= mSendCnt) {
                    // 피로도 누적의 의한 발송제한
                    isSendAble = false;
                }
            }
        }catch (Exception e){
            logger.error("!!![에러] 피로도 정보 조회시 에러 발송 :"+e.toString());
        }
        return isSendAble;
    }

    /**
     * 피로도를 레디스에 저장처리하는 메쏘드
     * @param cuid
     * @param isSendResultFail 발송결과 실패여부
     */
    public synchronized void upFatigue(final String cuid, boolean isSendResultFail){
        try {
            Map<String,Double> fartigueMap = fatigueCacheMgr.geFatigue();
            if(fartigueMap.size()==0) {
                return;
            }
            int fatigueCnt = 1;
            int chkTotalCnt = 0; // 레디스에 만료된 후에 결과실패가 들어왔을시 레디스에 저장하여 리소스 낭비를 방지하기 위해
            if(isSendResultFail){
                fatigueCnt = -1;
            }
            FatigueBean fatigueBean = new FatigueBean();
            // 피로도 사용으로 설정하고 메세지가 정보성이 아닐 경우 피로도 체크
            Object fatigueObj = redisTemplate.opsForHash().get(Constants.REDIS_FATIGUE, cuid);
            if (fatigueObj == null) {
                // 레디스에 저장된 피로도 정보가 없어 피로도를 뺄수 없는 예외경우 처리
                if(isSendResultFail){
                    return;
                }
                long dExpire = System.currentTimeMillis() + 86400000l;   // 60(초)*60(분)*24(시)
                long wExpire = System.currentTimeMillis() + 604800000l;  // 60(초)*60(분)*24(시)*7(일)
                long mExpire = System.currentTimeMillis() + 2592000000l; // 60(초)*60(분)*24(시)*30(일)

                fatigueBean.setdExpire(dExpire);
                fatigueBean.setwExpire(wExpire);
                fatigueBean.setmExpire(mExpire);

                fatigueBean.setdSendCnt(1);
                fatigueBean.setwSendCnt(1);
                fatigueBean.setmSendCnt(1);

                // 레디스에 정보 저장
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(fatigueBean.getdExpire()+":"+fatigueBean.getdSendCnt());
                stringBuilder.append(","+fatigueBean.getwExpire()+":"+fatigueBean.getwSendCnt());
                stringBuilder.append(","+fatigueBean.getmExpire()+":"+fatigueBean.getmSendCnt());
                redisTemplate.opsForHash().put(Constants.REDIS_FATIGUE, cuid, stringBuilder.toString());
            } else {
                // 레디스에 피로도정보가 존재할 경우
                String fatigueInfoStr = fatigueObj.toString();
                String[] fatigueInfoStrArr = fatigueInfoStr.split(",");
                String[] dayInfoArr = fatigueInfoStrArr[0].split(":");
                String[] weekInfoArr = fatigueInfoStrArr[1].split(":");
                String[] monthInfoArr = fatigueInfoStrArr[2].split(":");

                fatigueBean.setdExpire(Long.parseLong(dayInfoArr[0]));
                fatigueBean.setdSendCnt(Integer.parseInt(dayInfoArr[1]));

                fatigueBean.setwExpire(Long.parseLong(weekInfoArr[0]));
                fatigueBean.setwSendCnt(Integer.parseInt(weekInfoArr[1]));

                fatigueBean.setmExpire(Long.parseLong(monthInfoArr[0]));
                fatigueBean.setmSendCnt(Integer.parseInt(monthInfoArr[1]));

                long nowCurTimeMillis = System.currentTimeMillis();
                // Day 피로도체크
                long dExpireTimeMillis = fatigueBean.getdExpire();
                int dSendCnt = fatigueBean.getdSendCnt();
                if (dExpireTimeMillis < nowCurTimeMillis) {
                    // Day 피로도 만료된 경우 초기화
                    dExpireTimeMillis = System.currentTimeMillis() + 86400000l;
                    if(isSendResultFail){
                        dSendCnt = 0;
                    }else{
                        dSendCnt = 1;
                    }
                } else {
                    dSendCnt = dSendCnt + fatigueCnt;
                }
                fatigueBean.setdExpire(dExpireTimeMillis);
                fatigueBean.setdSendCnt(dSendCnt);
                chkTotalCnt = chkTotalCnt+dSendCnt; // Day 피로도 카운트 더함

                // Week 피로도체크
                long wExpireTimeMillis = fatigueBean.getwExpire();
                int wSendCnt = fatigueBean.getwSendCnt();
                if (wExpireTimeMillis < nowCurTimeMillis) {
                    // Week 피로도 만료된 경우 초기화
                    wExpireTimeMillis = System.currentTimeMillis() + 604800000l;
                    if(isSendResultFail){
                        wSendCnt = 0;
                    }else{
                        wSendCnt = 1;
                    }
                } else {
                    wSendCnt = wSendCnt + fatigueCnt;
                }
                fatigueBean.setwExpire(wExpireTimeMillis);
                fatigueBean.setwSendCnt(wSendCnt);
                chkTotalCnt = chkTotalCnt+wSendCnt; // Week 피로도 카운트 더함

                // Month 피로도체크
                long mExpireTimeMillis = fatigueBean.getmExpire();
                int mSendCnt = fatigueBean.getmSendCnt();
                if (mExpireTimeMillis < nowCurTimeMillis) {
                    // Month 피로도 만료된 경우 초기화
                    mExpireTimeMillis = System.currentTimeMillis() + 2592000000l;
                    if(isSendResultFail){
                        mSendCnt = 0;
                    }else{
                        mSendCnt = 1;
                    }
                } else {
                    mSendCnt = mSendCnt + fatigueCnt;
                }
                fatigueBean.setmExpire(mExpireTimeMillis);
                fatigueBean.setmSendCnt(mSendCnt);
                chkTotalCnt = chkTotalCnt+mSendCnt; // Month 피로도 카운트 더함

                if(chkTotalCnt>0) {
                    // 일/주/월 피로도 체크카운트가 한개라도 존재하면 레디스에 저장.
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(fatigueBean.getdExpire()+":"+fatigueBean.getdSendCnt());
                    stringBuilder.append(","+fatigueBean.getwExpire()+":"+fatigueBean.getwSendCnt());
                    stringBuilder.append(","+fatigueBean.getmExpire()+":"+fatigueBean.getmSendCnt());
                    redisTemplate.opsForHash().put(Constants.REDIS_FATIGUE, cuid, stringBuilder.toString());
                }else{
                    redisTemplate.opsForHash().delete(Constants.REDIS_FATIGUE, cuid);
                }
            }

        }catch (Exception e){
            logger.error("!!! 레디스 피로도 정보 업데이트시 에러 밣생 : "+e.toString());

        }
    }

    public String responseJsonString(String resultCode, String requestUri, Object reqParamObj){
        return responseJsonString(resultCode, ErrorManager.getInstance().getMsg(resultCode), new HashMap<String, Object>(), requestUri, reqParamObj);
    }

    public String responseJsonString(String resultCode, String resultMsg, String requestUri, Object reqParamObj){
        return responseJsonString(resultCode, resultMsg, new HashMap<String, Object>(), requestUri, reqParamObj);
    }

    public String responseJsonString(String resultCode, String resultMsg, Map<String,Object> resultBodyMap, String requestUri, Object reqParamObj){
        return responseJsonString(resultCode, resultMsg, new ArrayList<Map<String, Object>>(Arrays.asList(resultBodyMap)), requestUri, reqParamObj);
    }

    public String responseJsonString(String resultCode, String resultMsg, List<Map<String,Object>> resultBodyMapList, String requestUri, Object reqParamObj){
        Map<String,Object> rootMap = new HashMap<String, Object>();
        Map<String,Object> headMap = new HashMap<String, Object>();
        if(resultCode.equals("200")){
            resultCode = "0000";
        }
        headMap.put(Constants.RESULT_CODE, resultCode);
        headMap.put(Constants.RESULT_MSG, resultMsg);

        rootMap.put("HEADER", headMap);
        if(resultBodyMapList != null) {
            rootMap.put("BODY", resultBodyMapList);
        }else{
            rootMap.put("BODY", new ArrayList<Map<String,Object>>());
        }
        // ApplicationContext.xml에 정의 되어 있는 model id jsonReport 호출 하여 json 포멧으로 응답
        String responseJson = gson.toJson(rootMap);
        if(logger.isDebugEnabled()){
            if(reqParamObj != null)logger.debug("[REQ {}]: {}", requestUri, gson.toJson(reqParamObj));
            logger.debug("[RES {}]: {}", requestUri, responseJson);
        } else {
            if(!"0000".equals(resultCode)){
                logger.info("[RES {} fail]: ERRCODE:[{}] ERRMSG:{} reqParam:{}", requestUri, resultCode, resultMsg, responseJson);
            }
        }
        return responseJson;
    }

    public String responseJsonString(String resultCode, String resultMsg, Map<String,Object> resultBodyMap){
        Map<String,Object> rootMap = new HashMap<String, Object>();
        Map<String,Object> headMap = new HashMap<String, Object>();
        if(resultCode.equals("200")){
            resultCode = "0000";
        }
        headMap.put(Constants.RESULT_CODE, resultCode);
        headMap.put(Constants.RESULT_MSG, resultMsg);

        rootMap.put("HEADER", headMap);
        if(resultBodyMap != null) {
            rootMap.put("BODY", resultBodyMap);
        }else{
            rootMap.put("BODY", new HashMap<String,Object>());
        }

        return gson.toJson(rootMap);
    }

    public AllotterManager getAllotterManager() { return allotterManager; }
}

