package kr.uracle.ums.core.dao.kko;

import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import kr.uracle.ums.codec.redis.vo.kko.LotteKkoFrtProcessBean;
import kr.uracle.ums.core.extention.service.division.DivisionRollManager;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class LotteKkoFrtDao implements IKkoFrtDao {

    @Autowired(required = true)
    @Qualifier("sqlSessionTemplate")
    SqlSessionTemplate sqlSessionTemplate;

    @Autowired(required = true)
    private DivisionRollManager divisionRollManager;

    @Override
    public int inFrtSendMsg(BaseProcessBean baseKkoFrtProcessBean) throws Exception {
        LotteKkoFrtProcessBean prcsBean = (LotteKkoFrtProcessBean)baseKkoFrtProcessBean;
        String tableName = divisionRollManager.getPutTableName(baseKkoFrtProcessBean, SendType.KKOFRT, prcsBean.getPROVIDER());
        if(tableName!=null){
            prcsBean.setTABLE_NAME(tableName);
            return sqlSessionTemplate.insert("mybatis.friendtolk.lotte.inFrtSendMsgDynamic", prcsBean);
        }else {
            return sqlSessionTemplate.insert("mybatis.friendtolk.lotte.inFrtSendMsg", prcsBean);
        }
    }
}
