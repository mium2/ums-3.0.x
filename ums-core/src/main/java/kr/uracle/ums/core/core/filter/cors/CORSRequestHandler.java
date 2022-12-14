package kr.uracle.ums.core.core.filter.cors;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Handles incoming cross-origin (CORS) requests according to the configured 
 * access policy. Encapsulates the CORS processing logic as specified by the
 * <a href="http://www.w3.org/TR/2013/CR-cors-20130129/">W3C candidate
 * recommendation</a> from 2013-01-29.
 *
 * <p>Note that the actual CORS exception handling (which is outside the CORS
 * specification scope) is left to the invoking class to implement.
 *
 * @author Vladimir Dzhuvinov
 */
public class CORSRequestHandler {


	/**
	 * The CORS filter configuration, detailing the cross-origin access 
	 * policy.
	 */
	private final CORSConfiguration config;


	/**
	 * Pre-computed string of the CORS supported methods.
	 */
	private final String supportedMethods;


	/**
	 * Pre-computed string of the CORS supported headers.
	 */
	private final String supportedHeaders;


	/**
	 * Pre-computed string of the CORS exposed headers.
	 */
	private final String exposedHeaders;


	/**
	 * Creates a new CORS request handler.
	 *
	 * @param config Specifies the cross-origin access policy. Must not be
	 *               {@code null}.
	 */
	public CORSRequestHandler(final CORSConfiguration config) {

		this.config = config;

		// Pre-compute response headers where possible

		// Access-Control-Allow-Methods
		supportedMethods = HeaderUtils.serialize(config.supportedMethods, ", ");

		// Access-Control-Allow-Headers
		if (! config.supportAnyHeader)
			supportedHeaders = HeaderUtils.serialize(config.supportedHeaders, ", ");
		else
			supportedHeaders = null;

		/// Access-Control-Expose-Headers
		exposedHeaders = HeaderUtils.serialize(config.exposedHeaders, ", ");
	}


	/**
	 * Handles a simple or actual CORS request.
	 *
	 * <p>CORS specification: <a href="http://www.w3.org/TR/2013/CR-cors-20130129/#resource-requests">Simple
	 * Cross-Origin Request, Actual Request, and Redirects</a>
	 *
	 * @param request  The HTTP request.
	 * @param response The HTTP response.
	 *
	 * @throws CORSException If the request is invalid or denied.
	 */
	public void handleActualRequest(final HttpServletRequest request,
									final HttpServletResponse response)
		throws CORSException {

		if (CORSRequestType.detect(request) != CORSRequestType.ACTUAL)
			throw CORSException.INVALID_ACTUAL_REQUEST;


		// Check origin against allow list
		Origin requestOrigin = new Origin(request.getHeader(HeaderName.ORIGIN));

		if (! config.isAllowedOrigin(requestOrigin))
			throw CORSException.ORIGIN_DENIED;


		// Check method
		final String method = request.getMethod().toUpperCase();

		if (! config.isSupportedMethod(method))
			throw CORSException.UNSUPPORTED_METHOD;


		// Success, append response headers
		if (config.supportsCredentials) {

			response.addHeader(HeaderName.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");

			// The string "*" cannot be used for a resource that supports credentials.
			response.addHeader(HeaderName.ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin.toString());

			// See https://bitbucket.org/thetransactioncompany/cors-filter/issue/16/
			response.addHeader(HeaderName.VARY, "Origin");

		} else {
			if (config.allowAnyOrigin) {
				response.addHeader(HeaderName.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
			} else {
				response.addHeader(HeaderName.ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin.toString());

				// See https://bitbucket.org/thetransactioncompany/cors-filter/issue/16/
				response.addHeader(HeaderName.VARY, "Origin");
			}
		}

		if (! exposedHeaders.isEmpty())
			response.addHeader(HeaderName.ACCESS_CONTROL_EXPOSE_HEADERS, exposedHeaders);
	}


	/**
	 * Handles a preflight CORS request.
	 *
	 * <p>CORS specification: <a href="http://www.w3.org/TR/2013/CR-cors-20130129/#resource-preflight-requests">Preflight
	 * Request</a>
	 *
	 * @param request  The HTTP request.
	 * @param response The HTTP response.
	 *
	 * @throws CORSException If the request is invalid or denied.
	 */
	public void handlePreflightRequest(final HttpServletRequest request, final HttpServletResponse response)
		throws CORSException {

		if (CORSRequestType.detect(request) != CORSRequestType.PREFLIGHT)
			throw CORSException.INVALID_PREFLIGHT_REQUEST;

		// Check origin against allow list
		Origin requestOrigin = new Origin(request.getHeader(HeaderName.ORIGIN));

		if (! config.isAllowedOrigin(requestOrigin))
			throw CORSException.ORIGIN_DENIED;


		// Parse requested method
		// Note: method checking must be done after header parsing, see CORS spec

		String requestMethodHeader = request.getHeader(HeaderName.ACCESS_CONTROL_REQUEST_METHOD);

		if (requestMethodHeader == null)
			throw CORSException.MISSING_ACCESS_CONTROL_REQUEST_METHOD_HEADER;

		final String requestedMethod = requestMethodHeader.toUpperCase();

		// Parse the requested author (custom) headers
		final String rawRequestHeadersString = request.getHeader(HeaderName.ACCESS_CONTROL_REQUEST_HEADERS);
		final String[] requestHeaderValues = HeaderUtils.parseMultipleHeaderValues(rawRequestHeadersString);

		final String[] requestHeaders = new String[requestHeaderValues.length];

		for (int i=0; i<requestHeaders.length; i++) {

			try {
				requestHeaders[i] = HeaderName.formatCanonical(requestHeaderValues[i]);

			} catch (IllegalArgumentException e) {
				// Invalid header name
				throw CORSException.INVALID_HEADER_VALUE;
			}
		}


		// Now, do method check
		if (! config.isSupportedMethod(requestedMethod))
			throw CORSException.UNSUPPORTED_METHOD;


		// Author request headers check
		if (! config.supportAnyHeader) {

			for (String requestHeader : requestHeaders) {

				if (!config.supportedHeaders.contains(requestHeader))
					throw CORSException.UNSUPPORTED_REQUEST_HEADER;
			}
		}

		// Success, append response headers

		if (config.supportsCredentials) {
			response.addHeader(HeaderName.ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin.toString());
			response.addHeader(HeaderName.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");

			// See https://bitbucket.org/thetransactioncompany/cors-filter/issue/16/
			response.addHeader(HeaderName.VARY, "Origin");
		} else {
			if (config.allowAnyOrigin) {
				response.addHeader(HeaderName.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
			} else {
				response.addHeader(HeaderName.ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin.toString());

				// See https://bitbucket.org/thetransactioncompany/cors-filter/issue/16/
				response.addHeader(HeaderName.VARY, "Origin");
			}
		}

		if (config.maxAge > 0)
			response.addHeader(HeaderName.ACCESS_CONTROL_MAX_AGE, Integer.toString(config.maxAge));

		response.addHeader(HeaderName.ACCESS_CONTROL_ALLOW_METHODS, supportedMethods);


		if (config.supportAnyHeader && rawRequestHeadersString != null) {

			// Echo author headers
			response.addHeader(HeaderName.ACCESS_CONTROL_ALLOW_HEADERS, rawRequestHeadersString);

		} else if (supportedHeaders != null && ! supportedHeaders.isEmpty()) {

			response.addHeader(HeaderName.ACCESS_CONTROL_ALLOW_HEADERS, supportedHeaders);
		}
	}
}