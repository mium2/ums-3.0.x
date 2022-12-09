package kr.uracle.ums.core.dao.sms;

import kr.uracle.ums.codec.redis.vo.BaseProcessBean;

public interface ISmsDao {
    public int inSmsSendMsg(BaseProcessBean baseSmsProcessBean) throws Exception;

    public int inFromSmsToMmsSendMsg(BaseProcessBean baseSmsProcessBean) throws Exception;
}
