package kr.uracle.ums.core.processor.bean;

import kr.uracle.ums.codec.redis.enums.TransType;

/**
 * Created by Y.B.H(mium2) on 2019. 3. 15..
 */
public class SentInfoBean implements Cloneable {
    private TransType TRANS_TYPE;
    private long UMS_SEQNO = 0;    //보낼 메세지원장 고유번호
    private String CUST_TRANSGROUPKEY = ""; // 고객사 그룹거래식별고유번호
    private String CUST_TRANSKEY = ""; // 고객사 거래식별고유번호
    private int REQ_SEND_CNT = 0;     //총발송요청건수
    private int SUCC_CNT = 0;   //발송성공 건수
    private int FAIL_CNT = 0;   //발송실패 건수
    private int FINAL_FAIL_CNT = 0;  //최종 실패 갯수
    private int PUSH_SEND_CNT = 0;
    private int PUSH_FAIL_CNT = 0;
    private int WPUSH_SEND_CNT = 0;
    private int WPUSH_FAIL_CNT = 0;
    private int KKOALT_SEND_CNT = 0;
    private int KKOALT_FAIL_CNT = 0;
    private int KKOFRT_SEND_CNT = 0;
    private int KKOFRT_FAIL_CNT = 0;
    private int SMS_SEND_CNT = 0;
    // SMS
    private int SMS_TOTAL_SEND_CNT = 0;
    private int SMS_TOTAL_FAIL_CNT = 0;
    private int SMS_FAIL_CNT = 0;
    private int LMS_SEND_CNT = 0;
    private int LMS_FAIL_CNT = 0;
    private int MMS_SEND_CNT = 0;
    private int MMS_FAIL_CNT = 0;
    // RCS_SMS, RCS_LMS, RCS_MMS, RCS_FREE, RCS_CELL, RCS_DESC
    private int RCS_TOTAL_SEND_CNT = 0;
    private int RCS_TOTAL_FAIL_CNT = 0;
    private int RCS_SMS_SEND_CNT = 0;
    private int RCS_SMS_FAIL_CNT = 0;
    private int RCS_LMS_SEND_CNT = 0;
    private int RCS_LMS_FAIL_CNT = 0;
    private int RCS_MMS_SEND_CNT = 0;
    private int RCS_MMS_FAIL_CNT = 0;
    private int RCS_FREE_SEND_CNT = 0;
    private int RCS_FREE_FAIL_CNT = 0;
    private int RCS_CELL_SEND_CNT = 0;
    private int RCS_CELL_FAIL_CNT = 0;
    private int RCS_DESC_SEND_CNT = 0;
    private int RCS_DESC_FAIL_CNT = 0;
    private int NAVER_SEND_CNT = 0;
    private int NAVER_FAIL_CNT = 0;

    public SentInfoBean(TransType TRANS_TYPE){
        this.TRANS_TYPE = TRANS_TYPE;
    }

    public TransType getTRANS_TYPE() {
        return TRANS_TYPE;
    }

    public long getUMS_SEQNO() {
        return UMS_SEQNO;
    }

    public void setUMS_SEQNO(long UMS_SEQNO) {
        this.UMS_SEQNO = UMS_SEQNO;
    }

    public String getCUST_TRANSGROUPKEY() {
        return CUST_TRANSGROUPKEY;
    }

    public void setCUST_TRANSGROUPKEY(String CUST_TRANSGROUPKEY) {
        this.CUST_TRANSGROUPKEY = CUST_TRANSGROUPKEY;
    }

    public String getCUST_TRANSKEY() {
        return CUST_TRANSKEY;
    }

    public void setCUST_TRANSKEY(String CUST_TRANSKEY) {
        this.CUST_TRANSKEY = CUST_TRANSKEY;
    }

    public int getREQ_SEND_CNT() {
        return REQ_SEND_CNT;
    }

    public void setREQ_SEND_CNT(int REQ_SEND_CNT) {
        this.REQ_SEND_CNT = REQ_SEND_CNT;
    }

    public int getSUCC_CNT() {
        return SUCC_CNT;
    }

    public void setSUCC_CNT(int SUCC_CNT) {
        this.SUCC_CNT = SUCC_CNT;
    }

    public int getFAIL_CNT() {
        return FAIL_CNT;
    }

    public void setFAIL_CNT(int FAIL_CNT) {
        this.FAIL_CNT = FAIL_CNT;
    }

    public int getFINAL_FAIL_CNT() {
        return FINAL_FAIL_CNT;
    }

    public void setFINAL_FAIL_CNT(int FINAL_FAIL_CNT) {
        this.FINAL_FAIL_CNT = FINAL_FAIL_CNT;
    }

    public void setTRANS_TYPE(TransType TRANS_TYPE) {
        this.TRANS_TYPE = TRANS_TYPE;
    }

    public int getPUSH_SEND_CNT() {
        return PUSH_SEND_CNT;
    }

    public void setPUSH_SEND_CNT(int PUSH_SEND_CNT) {
        this.PUSH_SEND_CNT = PUSH_SEND_CNT;
    }

    public int getPUSH_FAIL_CNT() {
        return PUSH_FAIL_CNT;
    }

    public void setPUSH_FAIL_CNT(int PUSH_FAIL_CNT) {
        this.PUSH_FAIL_CNT = PUSH_FAIL_CNT;
    }

    public int getWPUSH_SEND_CNT() {
        return WPUSH_SEND_CNT;
    }

    public void setWPUSH_SEND_CNT(int WPUSH_SEND_CNT) {
        this.WPUSH_SEND_CNT = WPUSH_SEND_CNT;
    }

    public int getWPUSH_FAIL_CNT() {
        return WPUSH_FAIL_CNT;
    }

    public void setWPUSH_FAIL_CNT(int WPUSH_FAIL_CNT) {
        this.WPUSH_FAIL_CNT = WPUSH_FAIL_CNT;
    }

    public int getKKOALT_SEND_CNT() {
        return KKOALT_SEND_CNT;
    }

    public void setKKOALT_SEND_CNT(int KKOALT_SEND_CNT) {
        this.KKOALT_SEND_CNT = KKOALT_SEND_CNT;
    }

    public int getKKOALT_FAIL_CNT() {
        return KKOALT_FAIL_CNT;
    }

    public void setKKOALT_FAIL_CNT(int KKOALT_FAIL_CNT) {
        this.KKOALT_FAIL_CNT = KKOALT_FAIL_CNT;
    }

    public int getKKOFRT_SEND_CNT() {
        return KKOFRT_SEND_CNT;
    }

    public void setKKOFRT_SEND_CNT(int KKOFRT_SEND_CNT) {
        this.KKOFRT_SEND_CNT = KKOFRT_SEND_CNT;
    }

    public int getKKOFRT_FAIL_CNT() {
        return KKOFRT_FAIL_CNT;
    }

    public void setKKOFRT_FAIL_CNT(int KKOFRT_FAIL_CNT) {
        this.KKOFRT_FAIL_CNT = KKOFRT_FAIL_CNT;
    }

    public int getSMS_TOTAL_SEND_CNT() {
        return SMS_TOTAL_SEND_CNT;
    }

    public void setSMS_TOTAL_SEND_CNT(int SMS_TOTAL_SEND_CNT) {
        this.SMS_TOTAL_SEND_CNT = SMS_TOTAL_SEND_CNT;
    }

    public int getSMS_TOTAL_FAIL_CNT() {
        return SMS_TOTAL_FAIL_CNT;
    }

    public void setSMS_TOTAL_FAIL_CNT(int SMS_TOTAL_FAIL_CNT) {
        this.SMS_TOTAL_FAIL_CNT = SMS_TOTAL_FAIL_CNT;
    }

    public int getSMS_SEND_CNT() {
        return SMS_SEND_CNT;
    }

    public void setSMS_SEND_CNT(int SMS_SEND_CNT) {
        this.SMS_SEND_CNT = SMS_SEND_CNT;
    }

    public int getSMS_FAIL_CNT() {
        return SMS_FAIL_CNT;
    }

    public void setSMS_FAIL_CNT(int SMS_FAIL_CNT) {
        this.SMS_FAIL_CNT = SMS_FAIL_CNT;
    }

    public int getLMS_SEND_CNT() {
        return LMS_SEND_CNT;
    }

    public void setLMS_SEND_CNT(int LMS_SEND_CNT) {
        this.LMS_SEND_CNT = LMS_SEND_CNT;
    }

    public int getLMS_FAIL_CNT() {
        return LMS_FAIL_CNT;
    }

    public void setLMS_FAIL_CNT(int LMS_FAIL_CNT) {
        this.LMS_FAIL_CNT = LMS_FAIL_CNT;
    }

    public int getMMS_SEND_CNT() {
        return MMS_SEND_CNT;
    }

    public void setMMS_SEND_CNT(int MMS_SEND_CNT) {
        this.MMS_SEND_CNT = MMS_SEND_CNT;
    }

    public int getMMS_FAIL_CNT() {
        return MMS_FAIL_CNT;
    }

    public void setMMS_FAIL_CNT(int MMS_FAIL_CNT) {
        this.MMS_FAIL_CNT = MMS_FAIL_CNT;
    }

    public int getRCS_TOTAL_SEND_CNT() {
        return RCS_TOTAL_SEND_CNT;
    }

    public void setRCS_TOTAL_SEND_CNT(int RCS_TOTAL_SEND_CNT) {
        this.RCS_TOTAL_SEND_CNT = RCS_TOTAL_SEND_CNT;
    }

    public int getRCS_TOTAL_FAIL_CNT() {
        return RCS_TOTAL_FAIL_CNT;
    }

    public void setRCS_TOTAL_FAIL_CNT(int RCS_TOTAL_FAIL_CNT) {
        this.RCS_TOTAL_FAIL_CNT = RCS_TOTAL_FAIL_CNT;
    }

    public int getRCS_SMS_SEND_CNT() {
        return RCS_SMS_SEND_CNT;
    }

    public void setRCS_SMS_SEND_CNT(int RCS_SMS_SEND_CNT) {
        this.RCS_SMS_SEND_CNT = RCS_SMS_SEND_CNT;
    }

    public int getRCS_SMS_FAIL_CNT() {
        return RCS_SMS_FAIL_CNT;
    }

    public void setRCS_SMS_FAIL_CNT(int RCS_SMS_FAIL_CNT) {
        this.RCS_SMS_FAIL_CNT = RCS_SMS_FAIL_CNT;
    }

    public int getRCS_LMS_SEND_CNT() {
        return RCS_LMS_SEND_CNT;
    }

    public void setRCS_LMS_SEND_CNT(int RCS_LMS_SEND_CNT) {
        this.RCS_LMS_SEND_CNT = RCS_LMS_SEND_CNT;
    }

    public int getRCS_LMS_FAIL_CNT() {
        return RCS_LMS_FAIL_CNT;
    }

    public void setRCS_LMS_FAIL_CNT(int RCS_LMS_FAIL_CNT) {
        this.RCS_LMS_FAIL_CNT = RCS_LMS_FAIL_CNT;
    }

    public int getRCS_MMS_SEND_CNT() {
        return RCS_MMS_SEND_CNT;
    }

    public void setRCS_MMS_SEND_CNT(int RCS_MMS_SEND_CNT) {
        this.RCS_MMS_SEND_CNT = RCS_MMS_SEND_CNT;
    }

    public int getRCS_MMS_FAIL_CNT() {
        return RCS_MMS_FAIL_CNT;
    }

    public void setRCS_MMS_FAIL_CNT(int RCS_MMS_FAIL_CNT) {
        this.RCS_MMS_FAIL_CNT = RCS_MMS_FAIL_CNT;
    }

    public int getRCS_FREE_SEND_CNT() {
        return RCS_FREE_SEND_CNT;
    }

    public void setRCS_FREE_SEND_CNT(int RCS_FREE_SEND_CNT) {
        this.RCS_FREE_SEND_CNT = RCS_FREE_SEND_CNT;
    }

    public int getRCS_FREE_FAIL_CNT() {
        return RCS_FREE_FAIL_CNT;
    }

    public void setRCS_FREE_FAIL_CNT(int RCS_FREE_FAIL_CNT) {
        this.RCS_FREE_FAIL_CNT = RCS_FREE_FAIL_CNT;
    }

    public int getRCS_CELL_SEND_CNT() {
        return RCS_CELL_SEND_CNT;
    }

    public void setRCS_CELL_SEND_CNT(int RCS_CELL_SEND_CNT) {
        this.RCS_CELL_SEND_CNT = RCS_CELL_SEND_CNT;
    }

    public int getRCS_CELL_FAIL_CNT() {
        return RCS_CELL_FAIL_CNT;
    }

    public void setRCS_CELL_FAIL_CNT(int RCS_CELL_FAIL_CNT) {
        this.RCS_CELL_FAIL_CNT = RCS_CELL_FAIL_CNT;
    }

    public int getRCS_DESC_SEND_CNT() {
        return RCS_DESC_SEND_CNT;
    }

    public void setRCS_DESC_SEND_CNT(int RCS_DESC_SEND_CNT) {
        this.RCS_DESC_SEND_CNT = RCS_DESC_SEND_CNT;
    }

    public int getRCS_DESC_FAIL_CNT() {
        return RCS_DESC_FAIL_CNT;
    }

    public void setRCS_DESC_FAIL_CNT(int RCS_DESC_FAIL_CNT) {
        this.RCS_DESC_FAIL_CNT = RCS_DESC_FAIL_CNT;
    }

    public int getNAVER_SEND_CNT() {
        return NAVER_SEND_CNT;
    }

    public void setNAVER_SEND_CNT(int NAVER_SEND_CNT) {
        this.NAVER_SEND_CNT = NAVER_SEND_CNT;
    }

    public int getNAVER_FAIL_CNT() {
        return NAVER_FAIL_CNT;
    }

    public void setNAVER_FAIL_CNT(int NAVER_FAIL_CNT) {
        this.NAVER_FAIL_CNT = NAVER_FAIL_CNT;
    }


    @Override
    public SentInfoBean clone() throws CloneNotSupportedException {
        SentInfoBean sentInfoBean = (SentInfoBean)super.clone();
        return sentInfoBean;
    }
}
