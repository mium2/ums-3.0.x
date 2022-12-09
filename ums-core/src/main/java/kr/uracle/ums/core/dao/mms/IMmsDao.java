package kr.uracle.ums.core.dao.mms;

import kr.uracle.ums.codec.redis.vo.BaseProcessBean;

public interface IMmsDao {
    /**
     * MMS 발송테이블 입력
     * @param baseMmsProcessBean
     * @return
     * @throws Exception
     */
    public int inMmsSendMsg(BaseProcessBean baseMmsProcessBean) throws Exception;
}
