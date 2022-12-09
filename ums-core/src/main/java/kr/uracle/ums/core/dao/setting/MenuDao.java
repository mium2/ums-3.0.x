package kr.uracle.ums.core.dao.setting;

import kr.uracle.ums.core.dao.BaseDao;
import kr.uracle.ums.core.vo.setting.MenuVo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Created by Y.B.H(mium2) on 2019. 1. 28..
 */
@Repository
public class MenuDao extends BaseDao {
	@Override
	public void afterPropertiesSet() throws Exception {
		namespace = "mybatis.menu";
	}
	public int insertMenu(Map<String, Object> reqMap) throws Exception {
		return sqlSessionTemplate.insert(String.format("%s.insertMenu", namespace), reqMap);
	}
	public int upMenu(Map<String, Object> reqMap) throws Exception {
		return sqlSessionTemplate.update(String.format("%s.updateMenu", namespace), reqMap);
	}
	public List<MenuVo> getMenuList() throws Exception {
		return sqlSessionTemplate.selectList(String.format("%s.selMenuList", namespace));
	}
	public List<MenuVo> selAllMenuList() throws Exception {
		return sqlSessionTemplate.selectList(String.format("%s.selAllMenuList", namespace));
	}
	public List<MenuVo> selLoginMenuList() throws Exception {
		return sqlSessionTemplate.selectList(String.format("%s.selLoginMenuList", namespace));
	}
	public int delMenu(String MENU_ID) throws Exception {
		return sqlSessionTemplate.delete(String.format("%s.delMenu", namespace), MENU_ID);
	}
	public List<MenuVo> sellMenuOrderbyMenuId() throws Exception {
		return sqlSessionTemplate.selectList(String.format("%s.sellMenuOrderbyMenuId", namespace));
	}
	public int upMenuAuthGroup(Map<String, Object> param) {
		return sqlSessionTemplate.update(String.format("%s.upMenuAuthGroup", namespace), param);
	}
}