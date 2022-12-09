package kr.uracle.ums.core.batch.vo;

public class LotteTokenVo {
    private String loginId = "";
    private String accessKey = "";
    private String accessSecret = "";
    private String expireTime = "";
    private String apiFlag = "Y";
    private String authorization = "";
    private long expireTimeStamp = 0;

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getAccessSecret() {
        return accessSecret;
    }

    public void setAccessSecret(String accessSecret) {
        this.accessSecret = accessSecret;
    }

    public String getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(String expireTime) {
        this.expireTime = expireTime;
    }

    public String getApiFlag() {
        return apiFlag;
    }

    public void setApiFlag(String apiFlag) {
        this.apiFlag = apiFlag;
    }

    public String getAuthorization() {
        return authorization;
    }

    public void setAuthorization(String authorization) {
        this.authorization = authorization;
    }

    public long getExpireTimeStamp() {
        return expireTimeStamp;
    }

    public void setExpireTimeStamp(long expireTimeStamp) {
        this.expireTimeStamp = expireTimeStamp;
    }

    @Override
    public String toString() {
        return "LotteTokenVo{" +
                "loginId='" + loginId + '\'' +
                ", accessKey='" + accessKey + '\'' +
                ", accessSecret='" + accessSecret + '\'' +
                ", expireTime='" + expireTime + '\'' +
                ", apiFlag='" + apiFlag + '\'' +
                ", authorization='" + authorization + '\'' +
                ", expireTimeStamp=" + expireTimeStamp +
                '}';
    }
}
