package kr.uracle.ums.core.dao.mms;

import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import org.springframework.stereotype.Repository;

@Repository
public class ImoMmsDao implements IMmsDao {
    @Override
    public int inMmsSendMsg(BaseProcessBean baseMmsProcessBean) throws Exception {
        return 0;
    }
}
