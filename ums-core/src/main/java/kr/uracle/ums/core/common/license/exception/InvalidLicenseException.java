package kr.uracle.ums.core.common.license.exception;

public class InvalidLicenseException extends Exception {

	private static final long serialVersionUID = -7815987489791638399L;

	public InvalidLicenseException() {
		super();
	}
	
	public InvalidLicenseException(String msg) {
		super(msg);
	}
	
	public InvalidLicenseException(String msg, Throwable e) {
		super(msg, e);
	}
}
