package common.http.zipkin.trace;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.client.methods.HttpRequestBase;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.web.HttpSpanInjector;
import org.springframework.cloud.sleuth.instrument.web.HttpTraceKeysInjector;
import org.springframework.cloud.sleuth.util.SpanNameUtil;

public class ApacheHttpClientTrace {

	protected static final Log log = LogFactory.getLog(MethodHandles.lookup().lookupClass());

	protected final Tracer tracer;
	protected final HttpSpanInjector spanInjector;
	protected final HttpTraceKeysInjector keysInjector;

	public ApacheHttpClientTrace(Tracer tracer, HttpSpanInjector spanInjector, HttpTraceKeysInjector keysInjector) {
		this.tracer = tracer;
		this.spanInjector = spanInjector;
		this.keysInjector = keysInjector;
	}

	/**
	 * Enriches the request with proper headers and publishes the client sent
	 * event
	 */
	public void publishStartEvent(HttpRequestBase request) {
		URI uri = request.getURI();
		String spanName = getName(uri);
		Span newSpan = this.tracer.createSpan(spanName);
		this.spanInjector.inject(newSpan, new ApacheHttpRequestTextMap(request));
		addRequestTags(request);
		newSpan.logEvent(Span.CLIENT_SEND);
		if (log.isDebugEnabled()) {
			log.debug("Starting new client span [" + newSpan + "]");
		}
	}

	private String getName(URI uri) {
		return SpanNameUtil.shorten(uriScheme(uri) + ":" + uri.getPath());
	}

	private String uriScheme(URI uri) {
		return uri.getScheme() == null ? "http" : uri.getScheme();
	}

	/**
	 * Adds HTTP tags to the client side span
	 */
	public void addRequestTags(HttpRequestBase request) {
		this.keysInjector.addRequestTags(request.getRequestLine().getUri(), request.getURI().getHost(),
				request.getURI().getPath(), request.getRequestLine().getMethod(),
				convetToMap(request.headerIterator()));
	}

	private Map<String, List<String>> convetToMap(HeaderIterator headerIterator) {
		Map<String, List<String>> values = new LinkedHashMap<>();
		List<String> curValues = new ArrayList<>();
		while (headerIterator.hasNext()) {
			Header next = (Header) headerIterator.next();
			curValues.add(next.getValue());
			values.put(next.getName(), curValues);
		}
		return values;
	}

	/**
	 * Close the current span and log the client received event
	 */
	public void finish() {
		if (!isTracing()) {
			return;
		}
		currentSpan().logEvent(Span.CLIENT_RECV);
		this.tracer.close(this.currentSpan());
	}

	protected Span currentSpan() {
		return this.tracer.getCurrentSpan();
	}

	protected boolean isTracing() {
		return this.tracer.isTracing();
	}

}
