package kr.uracle.ums.core.core.filter.cors.environment;


import java.util.Properties;


/**
 * System properties environment.
 *
 * @author David Bellem
 */
public class SystemProperties implements Environment {


	@Override
	public String getProperty(final String name) {

		return System.getProperty(name);
	}


	@Override
	public Properties getProperties() {

		return System.getProperties();
	}
}
