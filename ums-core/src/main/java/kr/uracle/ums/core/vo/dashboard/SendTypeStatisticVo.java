package kr.uracle.ums.core.vo.dashboard;

/**
 * Created by Y.B.H(mium2) on 2019. 6. 11..
 */
public class SendTypeStatisticVo {
    private int PUSH_SUCC_CNT = 0;
    private int PUSH_FAIL_CNT = 0;
    private int ALT_SUCC_CNT = 0;
    private int ALT_FAIL_CNT = 0;
    private int FRT_SUCC_CNT = 0;
    private int FRT_FAIL_CNT = 0;
    private int SMS_SUCC_CNT = 0;
    private int SMS_FAIL_CNT = 0;

    public int getPUSH_SUCC_CNT() {
        return PUSH_SUCC_CNT;
    }

    public void setPUSH_SUCC_CNT(int PUSH_SUCC_CNT) {
        this.PUSH_SUCC_CNT = PUSH_SUCC_CNT;
    }

    public int getPUSH_FAIL_CNT() {
        return PUSH_FAIL_CNT;
    }

    public void setPUSH_FAIL_CNT(int PUSH_FAIL_CNT) {
        this.PUSH_FAIL_CNT = PUSH_FAIL_CNT;
    }

    public int getALT_SUCC_CNT() {
        return ALT_SUCC_CNT;
    }

    public void setALT_SUCC_CNT(int ALT_SUCC_CNT) {
        this.ALT_SUCC_CNT = ALT_SUCC_CNT;
    }

    public int getALT_FAIL_CNT() {
        return ALT_FAIL_CNT;
    }

    public void setALT_FAIL_CNT(int ALT_FAIL_CNT) {
        this.ALT_FAIL_CNT = ALT_FAIL_CNT;
    }

    public int getFRT_SUCC_CNT() {
        return FRT_SUCC_CNT;
    }

    public void setFRT_SUCC_CNT(int FRT_SUCC_CNT) {
        this.FRT_SUCC_CNT = FRT_SUCC_CNT;
    }

    public int getFRT_FAIL_CNT() {
        return FRT_FAIL_CNT;
    }

    public void setFRT_FAIL_CNT(int FRT_FAIL_CNT) {
        this.FRT_FAIL_CNT = FRT_FAIL_CNT;
    }

    public int getSMS_SUCC_CNT() {
        return SMS_SUCC_CNT;
    }

    public void setSMS_SUCC_CNT(int SMS_SUCC_CNT) {
        this.SMS_SUCC_CNT = SMS_SUCC_CNT;
    }

    public int getSMS_FAIL_CNT() {
        return SMS_FAIL_CNT;
    }

    public void setSMS_FAIL_CNT(int SMS_FAIL_CNT) {
        this.SMS_FAIL_CNT = SMS_FAIL_CNT;
    }
}
