package kr.uracle.ums.core.vo;

import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.vo.UmsMessageVo;
import org.springframework.web.multipart.MultipartFile;

/**
 * Created by Y.B.H(mium2) on 2019. 4. 29..
 * 
 * 
 */
public class ReqUmsSendVo extends UmsMessageVo{
        
    //공통 - 발송유형- 실시간:REAL, 배치성:BATCH
    private TransType TRANS_TYPE = TransType.BATCH;
    private String REQ_TRAN_TYPE = ""; //요청받은 발송유형 형태 : 실시간:RT, 배치성:BT

    //공통 - 발송타입
    private SendType SEND_TYPE;
    //공통 - CSV 발송 파일
    private MultipartFile CSVFILE;
    //공통 - CSV 예약 발송 파일 정보
    private String RESERVE_CSVFILE = "";
    
    //TPS 정보
    private String LIMITSECOND = "0";
    private String LIMITCNT = "0";
    
    private long TEMP_SEQNO = 0;
    
    private String CSV_FILE_PATH ="";
    
    private String EXT_IMGFILE_PATH ="";
    private String EXT_IMGFILE_URL="";

    /*************************************************************
	* PUSH 채널 정보 영역
	* ***********************************************************/
    //EXT 정보 - B: 기본, I:이미지, M: 동영상
    private String EXT_KIND = "";
    private String EXT_LINK = "";
    private String EXT_IMGURL = "";
    private String EXT_MOVURL = "";
    private MultipartFile IMGFILES;
    
    private String PUSH_FAIL_WAIT_MIN = "0";
    private String SPLIT_MSG_CNT = "0";
    private String DELAY_SECOND = "0";

    /*************************************************************
	* 친구톡 채널 정보 영역
	* ***********************************************************/
    private String KKO_IMGFILE_PATH = "";
    private MultipartFile KKO_IMGFILE;

    /*************************************************************
	* 카카오톡 채널 공통 정보 영역
	* ***********************************************************/ 
    private String KKO_IMGFILE_URL="";
    
    /*************************************************************
	* SMS 채널 정보 영역
	* ***********************************************************/
    private String SMS_REJECT_NUM ="";
    private MultipartFile[] MMS_IMG_FILES; // RCS_MMS:이미지 경로. 이미지 File일 경우.
    /*************************************************************
	* RCS 채널 정보 영역
	* ***********************************************************/ 
    // 이미지 경로만 전달 되는 경우, 이미지 경로 정보 - StringArray["","",""]
    private String RCS_IMG_PATH;
    // 이미지가 전달되는 경우, 이미지를 파일시스템에 생성하고 RCS_IMG_PATH에 경로 정보 저장
    private MultipartFile[] RCS_IMG_FILES; // RCS_MMS:이미지 경로. 이미지 File일 경우.

    /***************************************************************************************************************************************************************************************************
	****************************************************************************************************************************************************************************************************/ 
	public TransType getTRANS_TYPE() { return TRANS_TYPE; }
    public void setTRANS_TYPE(TransType TRANS_TYPE) { this.TRANS_TYPE = TRANS_TYPE; }

    public String getREQ_TRAN_TYPE() {
        return REQ_TRAN_TYPE;
    }

    public void setREQ_TRAN_TYPE(String REQ_TRAN_TYPE) {
        this.REQ_TRAN_TYPE = REQ_TRAN_TYPE;
    }

    public SendType getSEND_TYPE() { return SEND_TYPE; }
    public void setSEND_TYPE(SendType SEND_TYPE) { this.SEND_TYPE = SEND_TYPE; }
    
    public MultipartFile getCSVFILE() { return CSVFILE; }
    public void setCSVFILE(MultipartFile CSVFILE) { this.CSVFILE = CSVFILE; }

    public String getRESERVE_CSVFILE() { return RESERVE_CSVFILE; }
    public void setRESERVE_CSVFILE(String RESERVE_CSVFILE) { this.RESERVE_CSVFILE = RESERVE_CSVFILE; }

    public String getEXT_KIND() { return EXT_KIND; }
    public void setEXT_KIND(String EXT_KIND) { this.EXT_KIND = EXT_KIND; }

    public String getEXT_LINK() { return EXT_LINK; }
    public void setEXT_LINK(String EXT_LINK) { this.EXT_LINK = EXT_LINK; }

    public MultipartFile getIMGFILES() { return IMGFILES; }
    public void setIMGFILES(MultipartFile IMGFILES) { this.IMGFILES = IMGFILES; }

    public String getEXT_IMGURL() { return EXT_IMGURL; }
    public void setEXT_IMGURL(String EXT_IMGURL) { this.EXT_IMGURL = EXT_IMGURL; }

    public String getEXT_MOVURL() { return EXT_MOVURL; }
    public void setEXT_MOVURL(String EXT_MOVURL) { this.EXT_MOVURL = EXT_MOVURL; }
    
    public String getPUSH_FAIL_WAIT_MIN() { return PUSH_FAIL_WAIT_MIN; }
    public void setPUSH_FAIL_WAIT_MIN(String PUSH_FAIL_WAIT_MIN) { this.PUSH_FAIL_WAIT_MIN = PUSH_FAIL_WAIT_MIN; }

    public String getSPLIT_MSG_CNT() { return SPLIT_MSG_CNT; }
    public void setSPLIT_MSG_CNT(String SPLIT_MSG_CNT) { this.SPLIT_MSG_CNT = SPLIT_MSG_CNT; }

    public String getDELAY_SECOND() { return DELAY_SECOND; }
    public void setDELAY_SECOND(String DELAY_SECOND) { this.DELAY_SECOND = DELAY_SECOND; }

    public String getKKO_IMGFILE_PATH() { return KKO_IMGFILE_PATH; }
    public void setKKO_IMGFILE_PATH(String KKO_IMGFILE_PATH) { this.KKO_IMGFILE_PATH = KKO_IMGFILE_PATH; }

    public MultipartFile getKKO_IMGFILE() { return KKO_IMGFILE; }
    public void setKKO_IMGFILE(MultipartFile KKO_IMGFILE) { this.KKO_IMGFILE = KKO_IMGFILE; }

    public String getRCS_IMG_PATH() { return RCS_IMG_PATH; }
    public void setRCS_IMG_PATH(String RCS_IMG_PATH) { this.RCS_IMG_PATH = RCS_IMG_PATH; }
    
    public MultipartFile[] getRCS_IMG_FILES() { return RCS_IMG_FILES; }
    public void setRCS_IMG_FILES(MultipartFile[] RCS_IMG_FILES) { this.RCS_IMG_FILES = RCS_IMG_FILES; }

    public String getLIMITSECOND() { return LIMITSECOND; }
    public void setLIMITSECOND(String LIMITSECOND) { this.LIMITSECOND = LIMITSECOND; }

    public String getLIMITCNT() { return LIMITCNT; }
    public void setLIMITCNT(String LIMITCNT) { this.LIMITCNT = LIMITCNT; }

    public long getTEMP_SEQNO() { return TEMP_SEQNO; }
    public void setTEMP_SEQNO(long TEMP_SEQNO) { this.TEMP_SEQNO = TEMP_SEQNO; }

    public String getCSV_FILE_PATH() { return CSV_FILE_PATH; }
    public void setCSV_FILE_PATH(String CSV_FILE_PATH) { this.CSV_FILE_PATH = CSV_FILE_PATH; }

    public String getEXT_IMGFILE_PATH() { return EXT_IMGFILE_PATH; }
    public void setEXT_IMGFILE_PATH(String EXT_IMGFILE_PATH) { this.EXT_IMGFILE_PATH = EXT_IMGFILE_PATH; }

    public String getSMS_REJECT_NUM() { return SMS_REJECT_NUM; }
    public void setSMS_REJECT_NUM(String SMS_REJECT_NUM) { this.SMS_REJECT_NUM = SMS_REJECT_NUM; }

    public MultipartFile[] getMMS_IMG_FILES() {
        return MMS_IMG_FILES;
    }

    public void setMMS_IMG_FILES(MultipartFile[] MMS_IMG_FILES) {
        this.MMS_IMG_FILES = MMS_IMG_FILES;
    }

    public String getKKO_IMGFILE_URL() { return KKO_IMGFILE_URL; }
    public void setKKO_IMGFILE_URL(String KKO_IMGFILE_URL) { this.KKO_IMGFILE_URL = KKO_IMGFILE_URL; }
    
    public String getEXT_IMGFILE_URL() { return EXT_IMGFILE_URL; }
    public void setEXT_IMGFILE_URL(String EXT_IMGFILE_URL) { this.EXT_IMGFILE_URL = EXT_IMGFILE_URL; }



    //    @Override
//    public String toString() {
//        return "{\"ReqUmsSendVo\":"
//                + super.toString()
//                + ", \"TRANS_TYPE\":\"" + TRANS_TYPE + "\""
//                + ", \"SEND_TYPE\":\"" + SEND_TYPE + "\""
//                + ", \"CSVFILE\":" + CSVFILE
//                + ", \"RESERVE_CSVFILE\":\"" + RESERVE_CSVFILE + "\""
//                + ", \"LIMITSECOND\":\"" + LIMITSECOND + "\""
//                + ", \"LIMITCNT\":\"" + LIMITCNT + "\""
//                + ", \"TEMP_SEQNO\":\"" + TEMP_SEQNO + "\""
//                + ", \"CSV_FILE_PATH\":\"" + CSV_FILE_PATH + "\""
//                + ", \"EXT_IMGFILE_PATH\":\"" + EXT_IMGFILE_PATH + "\""
//                + ", \"EXT_IMGFILE_URL\":\"" + EXT_IMGFILE_URL + "\""
//                + ", \"EXT_KIND\":\"" + EXT_KIND + "\""
//                + ", \"EXT_LINK\":\"" + EXT_LINK + "\""
//                + ", \"EXT_IMGURL\":\"" + EXT_IMGURL + "\""
//                + ", \"EXT_MOVURL\":\"" + EXT_MOVURL + "\""
//                + ", \"IMGFILES\":" + IMGFILES
//                + ", \"PUSH_FAIL_WAIT_MIN\":\"" + PUSH_FAIL_WAIT_MIN + "\""
//                + ", \"SPLIT_MSG_CNT\":\"" + SPLIT_MSG_CNT + "\""
//                + ", \"DELAY_SECOND\":\"" + DELAY_SECOND + "\""
//                + ", \"FRIENDTOLK_SVCID\":\"" + FRIENDTOLK_SVCID + "\""
//                + ", \"KKO_IMGFILE_PATH\":\"" + KKO_IMGFILE_PATH + "\""
//                + ", \"KKO_IMGFILE\":" + KKO_IMGFILE
//                + ", \"KKO_IMGFILE_URL\":\"" + KKO_IMGFILE_URL + "\""
//                + ", \"SMS_REJECT_NUM\":\"" + SMS_REJECT_NUM + "\""
//                + ", \"RCS_IMG_PATH\":\"" + RCS_IMG_PATH + "\""
//                + ", \"RCS_IMG_FILES\":" + Arrays.toString(RCS_IMG_FILES)
//                + "}";
//    }
}
