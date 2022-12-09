package kr.uracle.ums.core.processor.bean;


import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;

/**
 * Created by Y.B.H(mium2) on 2019. 3. 15..
 */
public class UmsResultBaseBean {
    private long MASTERTABLE_SEQNO = 0; // UMS 푸시발송원장번호 or UMS 알림톡/친구톡 발송원장번호 or SMS/MMS 발송원장번호
    private TransType TRAN_TYPE;
    private String ROOT_CHANNEL_YN = "Y"; // 발송요청 첫번째 채널일 경우:Y, 대체발송 채널일 경우:N
    private String PROVIDER; // 채널 공급(중계)사
    private String SENDERID = ""; // 발송자아이디
    private String SENDERGROUPCODE = "";  // 조직코드
    private String SEND_TYPE = "";
    
    private String DETAIL_SEND_TYPE = "";
    
    private String SEND_TYPE_SEQCODE = ""; //발송채널 별 발송고유코드
    private String CUID = "";
    private String CNAME = "";
    private String MOBILE_NUM = "";
    private String SEND_TITLE = "";
    private String SEND_MSG = "";
    private String MSG_VARS = "";
    private String APP_ID = "";
    private String SVC_ID = "";
    private String CALLBACKNUM = "";
    private String SEND_RESULT = "RS"; // RS:요청, SS:성공, FS:대체발송실패, FF:최종실패
    private String SUCC_STATUS = "0"; // 0:발송요청, 1:발송성공, 2:수신확인, 3: 읽음확인
    private String PROCESS_END = "N";
    private String ERRCODE = "0000";
    private String RESULTMSG = "SUCCESS";
    
    

    public UmsResultBaseBean(TransType TRANS_TYPE, SendType sendType){
        this.TRAN_TYPE = TRANS_TYPE;
        this.SEND_TYPE=sendType.toString();
        this.DETAIL_SEND_TYPE = sendType.toString();
    }

    public TransType getTRAN_TYPE() { return TRAN_TYPE; }

    public String getROOT_CHANNEL_YN() { return ROOT_CHANNEL_YN; }
    public void setROOT_CHANNEL_YN(String ROOT_CHANNEL_YN) { this.ROOT_CHANNEL_YN = ROOT_CHANNEL_YN; }
    
    public String getPROVIDER() { return PROVIDER;	}
	public void setPROVIDER(String pROVIDER) { PROVIDER = pROVIDER;	}

	public long getMASTERTABLE_SEQNO() { return MASTERTABLE_SEQNO; }
    public void setMASTERTABLE_SEQNO(long MASTERTABLE_SEQNO) { this.MASTERTABLE_SEQNO = MASTERTABLE_SEQNO; }

    public String getSENDERID() { return SENDERID; }
    public void setSENDERID(String SENDERID) { this.SENDERID = SENDERID; }

    public String getSENDERGROUPCODE() { return SENDERGROUPCODE; }
    public void setSENDERGROUPCODE(String SENDERGROUPCODE) { this.SENDERGROUPCODE = SENDERGROUPCODE; }

    public String getSEND_TYPE() { return SEND_TYPE; }
    public void setSEND_TYPE(String SEND_TYPE) { this.SEND_TYPE = SEND_TYPE; }

    public String getSEND_TYPE_SEQCODE() { return SEND_TYPE_SEQCODE; }
    public void setSEND_TYPE_SEQCODE(String SEND_TYPE_SEQCODE) { this.SEND_TYPE_SEQCODE = SEND_TYPE_SEQCODE; }

    public String getCUID() { return CUID; }
    public void setCUID(String CUID) { this.CUID = CUID; }

    public String getCNAME() { return CNAME; }
    public void setCNAME(String CNAME) { this.CNAME = CNAME; }

    public String getMOBILE_NUM() { return MOBILE_NUM; }
    public void setMOBILE_NUM(String MOBILE_NUM) { this.MOBILE_NUM = MOBILE_NUM; }

    public String getSEND_TITLE() {
        return SEND_TITLE;
    }

    public void setSEND_TITLE(String SEND_TITLE) {
        this.SEND_TITLE = SEND_TITLE;
    }

    public String getSEND_MSG() { return SEND_MSG; }
    public void setSEND_MSG(String SEND_MSG) { this.SEND_MSG = SEND_MSG; }

    public String getMSG_VARS() { return MSG_VARS; }
    public void setMSG_VARS(String MSG_VARS) { this.MSG_VARS = MSG_VARS; }

    public String getAPP_ID() { return APP_ID; }
    public void setAPP_ID(String APP_ID) { this.APP_ID = APP_ID; }

    public String getSVC_ID() { return SVC_ID; }
    public void setSVC_ID(String SVC_ID) { this.SVC_ID = SVC_ID; }

    public String getCALLBACKNUM() { return CALLBACKNUM; }
    public void setCALLBACKNUM(String CALLBACKNUM) { this.CALLBACKNUM = CALLBACKNUM; }

    public String getSEND_RESULT() { return SEND_RESULT; }
    public void setSEND_RESULT(String SEND_RESULT) { this.SEND_RESULT = SEND_RESULT; }

    public String getSUCC_STATUS() { return SUCC_STATUS; }
    public void setSUCC_STATUS(String SUCC_STATUS) { this.SUCC_STATUS = SUCC_STATUS; }
    
    public String getPROCESS_END() { return PROCESS_END; }
    public void setPROCESS_END(String PROCESS_END) { this.PROCESS_END = PROCESS_END; }
    
    public String getERRCODE() { return ERRCODE; }
    public void setERRCODE(String ERRCODE) { this.ERRCODE = ERRCODE; }

    public String getRESULTMSG() { return RESULTMSG; }
    public void setRESULTMSG(String RESULTMSG) { this.RESULTMSG = RESULTMSG;}

    public String getDETAIL_SEND_TYPE() {return DETAIL_SEND_TYPE;}
    public void setDETAIL_SEND_TYPE(String DETAIL_SEND_TYPE) {this.DETAIL_SEND_TYPE = DETAIL_SEND_TYPE;}
}

