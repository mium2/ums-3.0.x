package kr.uracle.ums.core.service.bean;

import java.io.Serializable;

public class FatigueBean implements Serializable {
    private long dExpire = 0l;
    private long wExpire = 0l;
    private long mExpire = 0l;
    private int dSendCnt = 0;
    private int wSendCnt = 0;
    private int mSendCnt = 0;

    public long getdExpire() {
        return dExpire;
    }

    public void setdExpire(long dExpire) {
        this.dExpire = dExpire;
    }

    public long getwExpire() {
        return wExpire;
    }

    public void setwExpire(long wExpire) {
        this.wExpire = wExpire;
    }

    public long getmExpire() {
        return mExpire;
    }

    public void setmExpire(long mExpire) {
        this.mExpire = mExpire;
    }

    public int getdSendCnt() {
        return dSendCnt;
    }

    public void setdSendCnt(int dSendCnt) {
        this.dSendCnt = dSendCnt;
    }

    public int getwSendCnt() {
        return wSendCnt;
    }

    public void setwSendCnt(int wSendCnt) {
        this.wSendCnt = wSendCnt;
    }

    public int getmSendCnt() {
        return mSendCnt;
    }

    public void setmSendCnt(int mSendCnt) {
        this.mSendCnt = mSendCnt;
    }
}
