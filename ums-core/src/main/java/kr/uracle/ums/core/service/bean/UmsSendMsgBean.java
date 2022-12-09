package kr.uracle.ums.core.service.bean;

import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.vo.UmsMessageVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Y.B.H(mium2) on 2019. 4. 1..
 */
public class UmsSendMsgBean extends UmsMessageVo{

	public UmsSendMsgBean(){}
	
    public UmsSendMsgBean(String TRANS_TYPE){ this.TRANS_TYPE = TRANS_TYPE; }
    
    //공통 - 발송유형- 실시간:REAL, 배치성:BATCH
    private String TRANS_TYPE; 
    //발송 타입
    private String START_SEND_KIND="";
    //공통 - CSV 발송 파일
    private MultipartFile CSVFILE;
    
    // TARGET_USERS정보는 TARGET_USER_TYPE정보를 이용하여  다음과 같은 형식으로 만든 정보. {"아이디":["핸드폰번호","이름"]}
    private Map<String,List<String>> TARGET_USERS;
        
    private Map<String, String> REPLACE_VAR_MAP;
    
    // 처리 카운트
    private int PUSH_SEND_CNT = 0, 	WPUSH_SEND_CNT = 0,	ALLIMTOLK_CNT = 0, 	FRIENDTOLK_CNT = 0,	NAVERT_CNT = 0;
    private int SMS_CNT = 0, 		LMS_CNT = 0, 		MMS_CNT = 0; 
    private int RCS_SMS_CNT = 0, 	RCS_LMS_CNT = 0, 	RCS_MMS_CNT = 0, 	RCS_FREE_CNT = 0, RCS_CELL_CNT = 0, RCS_DESC_CNT = 0;    
    private int TOTAL_CNT =0,		SEND_CNT = 0, 		FAIL_CNT = 0;		

    // UMS 내에서 실패 로직처리 위한 정보 셋팅.
    private Set<SendType> FAIL_RETRY_SENDTYPE;  // 첫번째 발송처리실패시 두번째 대체처리발송타입
    private boolean isReplaceMsg = false;  // 치환유무

    /*************************************************************
	* PUSH 채널 정보 영역
	* ***********************************************************/    
    private String PUSH_FAIL_WAIT_MIN = "0";
    private String SPLIT_MSG_CNT =  "0";
    private String DELAY_SECOND = "0";

    /*************************************************************
	* 알림톡 채널 정보 영역
	* ***********************************************************/
    
    /*************************************************************
	* 친구톡 채널 정보 영역
	* ***********************************************************/
    
    /*************************************************************
	* 카카오톡 채널 공통 정보 영역
	* ***********************************************************/ 

    /*************************************************************
	* SMS 채널 정보 영역
	* ***********************************************************/ 

    /*************************************************************
	* RCS 채널 정보 영역
	* ***********************************************************/     
    private List<String> RCS_IMG_PATH = new ArrayList<String>(); // RCS_MMS: NAS 이미지 경로.
    private String RCS_IMG_PATH_JSON = "";
    
    /***************************************************************************************************************************************************************************************************
	****************************************************************************************************************************************************************************************************/     
    public void setTRANS_TYPE(String TRANS_TYPE) { this.TRANS_TYPE = TRANS_TYPE; }
    public String getTRANS_TYPE() { return TRANS_TYPE; }
    
    public String getSTART_SEND_KIND() { return START_SEND_KIND; }
    public void setSTART_SEND_KIND(String START_SEND_KIND) { this.START_SEND_KIND = START_SEND_KIND; }
    
    public Map<String, String> getREPLACE_VAR_MAP() { return REPLACE_VAR_MAP; }
    public void setREPLACE_VAR_MAP(Map<String, String> REPLACE_VAR_MAP) { this.REPLACE_VAR_MAP = REPLACE_VAR_MAP; }
    
    public int getTOTAL_CNT() { return TOTAL_CNT; }
	public void setTOTAL_CNT(int tOTAL_CNT) { TOTAL_CNT = tOTAL_CNT; }

	public int getPUSH_SEND_CNT() { return PUSH_SEND_CNT; }
    public void setPUSH_SEND_CNT(int PUSH_SEND_CNT) { this.PUSH_SEND_CNT = PUSH_SEND_CNT; }

	public int getWPUSH_SEND_CNT() { return WPUSH_SEND_CNT; }
	public void setWPUSH_SEND_CNT(int WPUSH_SEND_CNT) { this.WPUSH_SEND_CNT = WPUSH_SEND_CNT; }

    public int getALLIMTOLK_CNT() { return ALLIMTOLK_CNT; }
    public void setALLIMTOLK_CNT(int ALLIMTOLK_CNT) { this.ALLIMTOLK_CNT = ALLIMTOLK_CNT; }

    public int getFRIENDTOLK_CNT() { return FRIENDTOLK_CNT; }
    public void setFRIENDTOLK_CNT(int FRIENDTOLK_CNT) { this.FRIENDTOLK_CNT = FRIENDTOLK_CNT; }

    public int getSMS_CNT() { return SMS_CNT; }
    public void setSMS_CNT(int SMS_CNT) { this.SMS_CNT = SMS_CNT; }

    public int getLMS_CNT() { return LMS_CNT; }
    public void setLMS_CNT(int LMS_CNT) { this.LMS_CNT = LMS_CNT; }

    public int getMMS_CNT() { return MMS_CNT; }
    public void setMMS_CNT(int MMS_CNT) { this.MMS_CNT = MMS_CNT; }
    
    public int getRCS_SMS_CNT() { return RCS_SMS_CNT; }
    public void setRCS_SMS_CNT(int RCS_SMS_CNT) { this.RCS_SMS_CNT = RCS_SMS_CNT; }

    public int getRCS_LMS_CNT() { return RCS_LMS_CNT; }
    public void setRCS_LMS_CNT(int RCS_LMS_CNT) { this.RCS_LMS_CNT = RCS_LMS_CNT; }

    public int getRCS_MMS_CNT() { return RCS_MMS_CNT; }
    public void setRCS_MMS_CNT(int RCS_MMS_CNT) { this.RCS_MMS_CNT = RCS_MMS_CNT; }

    public int getRCS_FREE_CNT() { return RCS_FREE_CNT;	}
	public void setRCS_FREE_CNT(int rCS_FREE_CNT) { RCS_FREE_CNT = rCS_FREE_CNT; }

	public int getRCS_CELL_CNT() { return RCS_CELL_CNT;	}
	public void setRCS_CELL_CNT(int rCS_CELL_CNT) { RCS_CELL_CNT = rCS_CELL_CNT; }

	public int getRCS_DESC_CNT() { return RCS_DESC_CNT; }
	public void setRCS_DESC_CNT(int rCS_DESC_CNT) { RCS_DESC_CNT = rCS_DESC_CNT; }
    
    public int getNAVERT_CNT() { return NAVERT_CNT; }
    public void setNAVERT_CNT(int NAVERT_CNT) { this.NAVERT_CNT = NAVERT_CNT; }

    public int getSEND_CNT() { return SEND_CNT; }
    public void setSEND_CNT(int sendCnt) { this.SEND_CNT = sendCnt; }
    public void setAddSEND_CNT(int sendCnt){ this.SEND_CNT = this.SEND_CNT + sendCnt; }
    
    public int getFAIL_CNT() { return FAIL_CNT; }
    public void setFAIL_CNT(int FAIL_CNT) { this.FAIL_CNT = FAIL_CNT; }

    public Set<SendType> getFAIL_RETRY_SENDTYPE() { return FAIL_RETRY_SENDTYPE; }
    public void setFAIL_RETRY_SENDTYPE(Set<SendType> FAIL_RETRY_SENDTYPE) { this.FAIL_RETRY_SENDTYPE = FAIL_RETRY_SENDTYPE; }

    public boolean isReplaceMsg() { return isReplaceMsg; }
    public void setReplaceMsg(boolean replaceMsg) { isReplaceMsg = replaceMsg; }
    
    public Map<String, List<String>> getTARGET_USERS() { return TARGET_USERS; }
    public void setTARGET_USERS(Map<String, List<String>> TARGET_USERS) { this.TARGET_USERS = TARGET_USERS; }

    public MultipartFile getCSVFILE() { return CSVFILE; }
    public void setCSVFILE(MultipartFile CSVFILE) { this.CSVFILE = CSVFILE; }

    public String getPUSH_FAIL_WAIT_MIN() { return PUSH_FAIL_WAIT_MIN; }
    public void setPUSH_FAIL_WAIT_MIN(String PUSH_FAIL_WAIT_MIN) { this.PUSH_FAIL_WAIT_MIN = PUSH_FAIL_WAIT_MIN; }

    public String getSPLIT_MSG_CNT() { return SPLIT_MSG_CNT; }
    public void setSPLIT_MSG_CNT(String SPLIT_MSG_CNT) { this.SPLIT_MSG_CNT = SPLIT_MSG_CNT; }

    public String getDELAY_SECOND() { return DELAY_SECOND; }
    public void setDELAY_SECOND(String DELAY_SECOND) { this.DELAY_SECOND = DELAY_SECOND; }

    public List<String> getRCS_IMG_PATH() { return RCS_IMG_PATH; }
    public void setRCS_IMG_PATH(List<String> RCS_IMG_PATH) { this.RCS_IMG_PATH = RCS_IMG_PATH; }
    
    public String getRCS_IMG_PATH_JSON() { return RCS_IMG_PATH_JSON; }
    public void setRCS_IMG_PATH_JSON(String RCS_IMG_PATH_JSON) { this.RCS_IMG_PATH_JSON = RCS_IMG_PATH_JSON; }

    public void setChannelSendCount(SendType sendType, int count) {
    	switch(sendType) {
	    	case SMS:
	    		setSMS_CNT(count);
	    		break;
	    	case LMS:
	    		setLMS_CNT(count);
	    		break;
	    	case MMS:
	    		setMMS_CNT(count);
	    		break;
	    	case PUSH:
	    		setPUSH_SEND_CNT(count);
	    		break;
	    	case KKOALT:
	    		setALLIMTOLK_CNT(count);
	    		break;
	    	case KKOFRT:
	    		setFRIENDTOLK_CNT(count);
	    		break;	    		
	    	case RCS_SMS:
	    		setRCS_SMS_CNT(count);
	    		break;	    		
	    	case RCS_LMS:
	    		setRCS_LMS_CNT(count);
	    		break;
	    	case RCS_MMS:
	    		setRCS_MMS_CNT(count);
	    		break;
	    	case RCS_FREE:
	    		setRCS_FREE_CNT(count);
	    		break;
	    	case RCS_CELL:
	    		setRCS_CELL_CNT(count);
	    		break;
	    	case RCS_DESC:
	    		setRCS_DESC_CNT(count);
	    		break;
	    	case NAVERT:
	    		setNAVERT_CNT(count);
	    		break;
	    	default:
	    		break;
    	}

    }
    
    public int getChannelSendCount(SendType sendType) {
    	int count = 0;
    	switch(sendType) {
	    	case SMS:
	    		count = getSMS_CNT();
	    		break;
	    	case LMS:
	    		count = getLMS_CNT();
	    		break;
	    	case MMS:
	    		count = getMMS_CNT();
	    		break;
	    	case PUSH:
	    		count = getPUSH_SEND_CNT();
	    		break;
	    	case WPUSH:
	    		count = getWPUSH_SEND_CNT();
	    		break;
	    	case KKOALT:
	    		count = getALLIMTOLK_CNT();
	    		break;
	    	case KKOFRT:
	    		count = getFRIENDTOLK_CNT();
	    		break;	    		
	    	case RCS_SMS:
	    		count = getRCS_SMS_CNT();
	    		break;	    		
	    	case RCS_LMS:
	    		count = getRCS_LMS_CNT();
	    		break;
	    	case RCS_MMS:
	    		count = getRCS_MMS_CNT();
	    		break;
	    	case RCS_FREE:
	    		count = getRCS_FREE_CNT();
	    		break;
	    	case RCS_CELL:
	    		count = getRCS_CELL_CNT();
	    		break;
	    	case RCS_DESC:
	    		count = getRCS_DESC_CNT();
	    		break;
	    	case NAVERT:
	    		count = getNAVERT_CNT();
	    		break;
	    	default:
	    		count = 0;
	    		break;
    	}
    	return count;
    }
}
