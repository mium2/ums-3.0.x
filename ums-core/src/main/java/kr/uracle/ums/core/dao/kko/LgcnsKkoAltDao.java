package kr.uracle.ums.core.dao.kko;

import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import kr.uracle.ums.codec.redis.vo.kko.LgcnsKkoAltProcessBean;
import kr.uracle.ums.core.extention.service.division.DivisionRollManager;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class LgcnsKkoAltDao implements IKkoAltDao{
    @Autowired(required = true)
    @Qualifier("sqlSessionTemplate")
    private SqlSessionTemplate sqlSessionTemplate;
    @Autowired(required = true)
    private DivisionRollManager divisionRollManager;

    public int inAltSendMsg(BaseProcessBean baseBean) throws Exception{
        LgcnsKkoAltProcessBean prcsBean = (LgcnsKkoAltProcessBean)baseBean;
        String tableName = divisionRollManager.getPutTableName(baseBean, SendType.KKOALT, prcsBean.getPROVIDER());
        // 알림톡 강조타이틀 정보 셋팅
        if(!"".equals(baseBean.getTITLE())){
            prcsBean.setSUBJECT("{\"t\":\""+baseBean.getTITLE()+"\"}");
        }

        if(tableName!=null){
            prcsBean.setTABLE_NAME(tableName);
            return sqlSessionTemplate.insert("mybatis.allimtolk.lgcns.inAltSendMsgDynamic", prcsBean);
        }else {
            return sqlSessionTemplate.insert("mybatis.allimtolk.lgcns.inAltSendMsg", prcsBean);
        }
    }
}
