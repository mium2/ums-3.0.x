package kr.uracle.ums.core.batch;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

public abstract class BaseBatch implements ApplicationListener<ContextClosedEvent>, InitializingBean {
	public static final List<BaseBatch> batchList = new ArrayList<BaseBatch>();
	
	public abstract void startScheduler();
	public abstract void stopScheduler();
	@Override
	public void afterPropertiesSet() throws Exception {
		batchList.add(this);
	}
	@Override
	public void onApplicationEvent(ContextClosedEvent arg0) {
		stopScheduler();
	}
}