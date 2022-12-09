package kr.uracle.ums.core.dao.sms;

import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import org.springframework.stereotype.Repository;

@Repository
public class ImoSmsDao implements ISmsDao{
    @Override
    public int inSmsSendMsg(BaseProcessBean baseSmsProcessBean) throws Exception {
        return 0;
    }
    @Override
    public int inFromSmsToMmsSendMsg(BaseProcessBean baseSmsProcessBean) throws Exception {
        return 0;
    }
}
