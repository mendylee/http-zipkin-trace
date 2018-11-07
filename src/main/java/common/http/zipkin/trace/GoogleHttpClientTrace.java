package common.http.zipkin.trace;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.web.HttpSpanInjector;
import org.springframework.cloud.sleuth.instrument.web.HttpTraceKeysInjector;
import org.springframework.cloud.sleuth.util.SpanNameUtil;

import java.lang.invoke.MethodHandles;
import java.util.*;

public class GoogleHttpClientTrace {
	protected static final Log log = LogFactory.getLog(MethodHandles.lookup().lookupClass());

	protected final Tracer tracer;
	protected final HttpSpanInjector spanInjector;
	protected final HttpTraceKeysInjector keysInjector;

	public GoogleHttpClientTrace(Tracer tracer, HttpSpanInjector spanInjector, HttpTraceKeysInjector keysInjector) {
		this.tracer = tracer;
		this.spanInjector = spanInjector;
		this.keysInjector = keysInjector;
	}

	/**
	 * Enriches the request with proper headers and publishes the client sent
	 * event
	 */
	public void publishStartEvent(HttpRequest request) {
		GenericUrl url = request.getUrl();
		String spanName = getName(url);
		Span newSpan = this.tracer.createSpan(spanName);
		this.spanInjector.inject(newSpan, new GoogleHttpRequestTextMap(request));
		addRequestTags(request);
		newSpan.logEvent(Span.CLIENT_SEND);
		if (log.isDebugEnabled()) {
			log.debug("Starting new client span [" + newSpan + "]");
		}
	}

	private String getName(GenericUrl url) {
		return SpanNameUtil.shorten(/* uriScheme(url) + ":" + */ url.toURI().toString());
	}

	private String uriScheme(GenericUrl url) {
		return url.getScheme() == null ? "http" : url.getScheme();
	}

	/**
	 * Adds HTTP tags to the client side span
	 */
	public void addRequestTags(HttpRequest request) {
		this.keysInjector.addRequestTags(request.getUrl().toURI().toString(), request.getUrl().getHost(),
				request.getUrl().getRawPath(), request.getRequestMethod(), convetToMap(request.getHeaders()));
	}

	private Map<String, List<String>> convetToMap(HttpHeaders headers) {
		Iterator<String> keyIter = headers.keySet().iterator();
		Map<String, List<String>> values = new LinkedHashMap<>();
		List<String> curValues = new ArrayList<>();
		while (keyIter.hasNext()) {
			String key = keyIter.next();
			curValues.addAll(headers.getHeaderStringValues(key));
			values.put(key, curValues);
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
