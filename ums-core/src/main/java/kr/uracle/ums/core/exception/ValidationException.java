package kr.uracle.ums.core.exception;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

import kr.uracle.ums.codec.redis.config.ErrorManager;

public class ValidationException extends Exception {
	@Autowired(required=true)
	private static MessageSource messageSource;
	
	public ValidationException(String validationField, Locale locale) {
		super(String.format("%s : %s", "", validationField));
	}
	public String getErrorCode() {
		return ErrorManager.ERR_1001;
	}
}