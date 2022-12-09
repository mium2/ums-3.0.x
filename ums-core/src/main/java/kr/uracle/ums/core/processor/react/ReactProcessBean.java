package kr.uracle.ums.core.processor.react;

import java.io.Serializable;

public class ReactProcessBean implements Serializable {
    private long UMS_SEQNO = 0; // UMS 원장고유번호
    private String CUST_TRANSGROUPKEY = "";
    private String CUST_TRANSKEY = ""; // 고객 발송메세지 고유키.
    private long REACT_CNT = 0; // 상세페이지에서 호출. 반응률
    private String BUTTON1_ID; // 버튼1 아이디
    private int BUTTON1_CNT = 0; // 버튼1 호출카운트
    private String BUTTON2_ID; // 버튼2 아이디
    private int BUTTON2_CNT = 0; // 버튼2 호출카운트
    private String BUTTON3_ID; // 버튼3 아이디
    private int BUTTON3_CNT = 0; // 버튼3 호출카운트
    private String BUTTON4_ID; // 버튼4 아이디
    private int BUTTON4_CNT = 0; // 버튼4 호출카운트
    private String BUTTON5_ID; // 버튼5 아이디
    private int BUTTON5_CNT = 0; // 버튼5 호출카운트
    private String BUTTON6_ID; // 버튼5 아이디
    private int BUTTON6_CNT = 0; // 버튼5 호출카운트
    private String BUTTON7_ID; // 버튼7 아이디
    private int BUTTON7_CNT = 0; // 버튼7 호출카운트

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

    public long getREACT_CNT() {
        return REACT_CNT;
    }

    public void setREACT_CNT(long REACT_CNT) {
        this.REACT_CNT = REACT_CNT;
    }

    public String getBUTTON1_ID() {
        return BUTTON1_ID;
    }

    public void setBUTTON1_ID(String BUTTON1_ID) {
        this.BUTTON1_ID = BUTTON1_ID;
    }

    public int getBUTTON1_CNT() {
        return BUTTON1_CNT;
    }

    public void setBUTTON1_CNT(int BUTTON1_CNT) {
        this.BUTTON1_CNT = BUTTON1_CNT;
    }

    public String getBUTTON2_ID() {
        return BUTTON2_ID;
    }

    public void setBUTTON2_ID(String BUTTON2_ID) {
        this.BUTTON2_ID = BUTTON2_ID;
    }

    public int getBUTTON2_CNT() {
        return BUTTON2_CNT;
    }

    public void setBUTTON2_CNT(int BUTTON2_CNT) {
        this.BUTTON2_CNT = BUTTON2_CNT;
    }

    public String getBUTTON3_ID() {
        return BUTTON3_ID;
    }

    public void setBUTTON3_ID(String BUTTON3_ID) {
        this.BUTTON3_ID = BUTTON3_ID;
    }

    public int getBUTTON3_CNT() {
        return BUTTON3_CNT;
    }

    public void setBUTTON3_CNT(int BUTTON3_CNT) {
        this.BUTTON3_CNT = BUTTON3_CNT;
    }

    public String getBUTTON4_ID() {
        return BUTTON4_ID;
    }

    public void setBUTTON4_ID(String BUTTON4_ID) {
        this.BUTTON4_ID = BUTTON4_ID;
    }

    public int getBUTTON4_CNT() {
        return BUTTON4_CNT;
    }

    public void setBUTTON4_CNT(int BUTTON4_CNT) {
        this.BUTTON4_CNT = BUTTON4_CNT;
    }

    public String getBUTTON5_ID() {
        return BUTTON5_ID;
    }

    public void setBUTTON5_ID(String BUTTON5_ID) {
        this.BUTTON5_ID = BUTTON5_ID;
    }

    public int getBUTTON5_CNT() {
        return BUTTON5_CNT;
    }

    public void setBUTTON5_CNT(int BUTTON5_CNT) {
        this.BUTTON5_CNT = BUTTON5_CNT;
    }

    public String getBUTTON6_ID() {
        return BUTTON6_ID;
    }

    public void setBUTTON6_ID(String BUTTON6_ID) {
        this.BUTTON6_ID = BUTTON6_ID;
    }

    public int getBUTTON6_CNT() {
        return BUTTON6_CNT;
    }

    public void setBUTTON6_CNT(int BUTTON6_CNT) {
        this.BUTTON6_CNT = BUTTON6_CNT;
    }

    public String getBUTTON7_ID() {
        return BUTTON7_ID;
    }

    public void setBUTTON7_ID(String BUTTON7_ID) {
        this.BUTTON7_ID = BUTTON7_ID;
    }

    public int getBUTTON7_CNT() {
        return BUTTON7_CNT;
    }

    public void setBUTTON7_CNT(int BUTTON7_CNT) {
        this.BUTTON7_CNT = BUTTON7_CNT;
    }
}
