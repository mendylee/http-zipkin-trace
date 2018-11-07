package common.http.zipkin.trace;

import java.lang.invoke.MethodHandles;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.web.HttpSpanInjector;
import org.springframework.cloud.sleuth.instrument.web.HttpTraceKeysInjector;
import org.springframework.cloud.sleuth.util.SpanNameUtil;

import com.ning.http.client.Request;
import com.ning.http.client.uri.Uri;

public class NingHttpClientTrace {
	protected static final Log log = LogFactory.getLog(MethodHandles.lookup().lookupClass());

	protected final Tracer tracer;
	protected final HttpSpanInjector spanInjector;
	protected final HttpTraceKeysInjector keysInjector;

	public NingHttpClientTrace(Tracer tracer, HttpSpanInjector spanInjector, HttpTraceKeysInjector keysInjector) {
		this.tracer = tracer;
		this.spanInjector = spanInjector;
		this.keysInjector = keysInjector;
	}

	/**
	 * Enriches the request with proper headers and publishes the client sent
	 * event
	 */
	public void publishStartEvent(Request request) {
		Uri uri = request.getUri();
		String spanName = getName(uri);
		Span newSpan = this.tracer.createSpan(spanName);
		this.spanInjector.inject(newSpan, new NingHttpRequestTextMap(request));
		addRequestTags(request);
		newSpan.logEvent(Span.CLIENT_SEND);
		if (log.isDebugEnabled()) {
			log.debug("Starting new client span [" + newSpan + "]");
		}
	}

	private String getName(Uri uri) {
		return SpanNameUtil.shorten(uriScheme(uri) + ":" + uri.getPath());
	}

	private String uriScheme(Uri uri) {
		return uri.getScheme() == null ? "http" : uri.getScheme();
	}

	public void addRequestTags(Request request) {
		this.keysInjector.addRequestTags(request.getUri().toString(), request.getUri().getHost(),
				request.getUri().getPath(), request.getMethod(), request.getHeaders());
	}

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
