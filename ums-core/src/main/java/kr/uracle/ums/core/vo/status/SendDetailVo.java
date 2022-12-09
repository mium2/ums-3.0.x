package kr.uracle.ums.core.vo.status;

/**
 * Created by Y.B.H(mium2) on 2019. 6. 5..
 */
public class SendDetailVo {
    private int TOT_CNT = 0;
    private long DETAIL_SEQNO = 0;
    private long UMS_SEQNO = 0;
    private String CUID = "";
    private String CNAME = "";
    private long PUSH_SEQNO = 0;
    private long KKO_ALT_SEQNO = 0;
    private long KKO_FRT_SEQNO = 0;
    private long SMS_SEQNO = 0;
    private long MMS_SEQNO = 0;
    private String MOBILE_NUM = "";
    private String SEND_KIND = "";
    private String SEND_KIND_TXT = "";
    private String SEND_MSG = "";
    private String MSG_VARS = "";
    private String STATUS = "";
    private String PROCESS_END = "";
    private String RESULTMSG = "";
    private String REGDATE = "";
    private String START_SEND_KIND = "";

    public int getTOT_CNT() {
        return TOT_CNT;
    }

    public void setTOT_CNT(int TOT_CNT) {
        this.TOT_CNT = TOT_CNT;
    }

    public long getDETAIL_SEQNO() {
        return DETAIL_SEQNO;
    }

    public void setDETAIL_SEQNO(long DETAIL_SEQNO) {
        this.DETAIL_SEQNO = DETAIL_SEQNO;
    }

    public long getUMS_SEQNO() {
        return UMS_SEQNO;
    }

    public void setUMS_SEQNO(long UMS_SEQNO) {
        this.UMS_SEQNO = UMS_SEQNO;
    }

    public String getCUID() {
        return CUID;
    }

    public void setCUID(String CUID) {
        this.CUID = CUID;
    }

    public String getCNAME() {
        return CNAME;
    }

    public void setCNAME(String CNAME) {
        this.CNAME = CNAME;
    }

    public long getPUSH_SEQNO() {
        return PUSH_SEQNO;
    }

    public void setPUSH_SEQNO(long PUSH_SEQNO) {
        this.PUSH_SEQNO = PUSH_SEQNO;
    }

    public long getKKO_ALT_SEQNO() {
        return KKO_ALT_SEQNO;
    }

    public void setKKO_ALT_SEQNO(long KKO_ALT_SEQNO) {
        this.KKO_ALT_SEQNO = KKO_ALT_SEQNO;
    }

    public long getKKO_FRT_SEQNO() {
        return KKO_FRT_SEQNO;
    }

    public void setKKO_FRT_SEQNO(long KKO_FRT_SEQNO) {
        this.KKO_FRT_SEQNO = KKO_FRT_SEQNO;
    }

    public long getSMS_SEQNO() {
        return SMS_SEQNO;
    }

    public void setSMS_SEQNO(long SMS_SEQNO) {
        this.SMS_SEQNO = SMS_SEQNO;
    }

    public long getMMS_SEQNO() {
        return MMS_SEQNO;
    }

    public void setMMS_SEQNO(long MMS_SEQNO) {
        this.MMS_SEQNO = MMS_SEQNO;
    }

    public String getMOBILE_NUM() {
        return MOBILE_NUM;
    }

    public void setMOBILE_NUM(String MOBILE_NUM) {
        this.MOBILE_NUM = MOBILE_NUM;
    }

    public String getSEND_KIND() {
        return SEND_KIND;
    }

    public void setSEND_KIND(String SEND_KIND) {
        this.SEND_KIND = SEND_KIND;
    }

    public String getSEND_KIND_TXT() {
        return SEND_KIND_TXT;
    }

    public void setSEND_KIND_TXT(String SEND_KIND_TXT) {
        this.SEND_KIND_TXT = SEND_KIND_TXT;
    }

    public String getSEND_MSG() {
        return SEND_MSG;
    }

    public void setSEND_MSG(String SEND_MSG) {
        this.SEND_MSG = SEND_MSG;
    }

    public String getMSG_VARS() {
        return MSG_VARS;
    }

    public void setMSG_VARS(String MSG_VARS) {
        this.MSG_VARS = MSG_VARS;
    }

    public String getSTATUS() {
        return STATUS;
    }

    public void setSTATUS(String STATUS) {
        this.STATUS = STATUS;
    }

    public String getPROCESS_END() {
        return PROCESS_END;
    }

    public void setPROCESS_END(String PROCESS_END) {
        this.PROCESS_END = PROCESS_END;
    }

    public String getRESULTMSG() {
        return RESULTMSG;
    }

    public void setRESULTMSG(String RESULTMSG) {
        this.RESULTMSG = RESULTMSG;
    }

    public String getREGDATE() {
        return REGDATE;
    }

    public void setREGDATE(String REGDATE) {
        this.REGDATE = REGDATE;
    }

    public String getSTART_SEND_KIND() {
        return START_SEND_KIND;
    }

    public void setSTART_SEND_KIND(String START_SEND_KIND) {
        this.START_SEND_KIND = START_SEND_KIND;
    }
}
