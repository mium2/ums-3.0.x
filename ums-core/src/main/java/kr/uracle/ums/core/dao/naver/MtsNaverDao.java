package kr.uracle.ums.core.dao.naver;

import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import kr.uracle.ums.codec.redis.vo.naver.MtsNaverProcessBean;
import kr.uracle.ums.core.extention.service.division.DivisionRollManager;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class MtsNaverDao implements INaverDao {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    @Value("${UMS.TEMPDIR:}")
    protected String TEMPDIR;

    @Autowired(required = true)
    @Qualifier("sqlSessionTemplate")
    SqlSessionTemplate sqlSessionTemplate;
    @Autowired(required = true)
    private DivisionRollManager divisionRollManager;
    @Override
    public int inNaverSendMsg(BaseProcessBean baseNaverProcessBean) throws Exception {
        MtsNaverProcessBean mtsNaverProcessBean = (MtsNaverProcessBean)baseNaverProcessBean;
        String tableName = divisionRollManager.getPutTableName(baseNaverProcessBean, SendType.NAVERT, mtsNaverProcessBean.getPROVIDER());
        if(tableName!=null){
            mtsNaverProcessBean.setTABLE_NAME(tableName);
            return sqlSessionTemplate.insert("mybatis.naver.mts.inNaverSendMsgDynamic", mtsNaverProcessBean);
        }else {
            return sqlSessionTemplate.insert("mybatis.naver.mts.inNaverSendMsg", mtsNaverProcessBean);
        }
    }
}
