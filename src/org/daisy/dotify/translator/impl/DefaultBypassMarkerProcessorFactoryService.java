package org.daisy.dotify.translator.impl;

import org.daisy.dotify.api.translator.MarkerProcessorFactory;
import org.daisy.dotify.api.translator.MarkerProcessorFactoryService;
import org.daisy.dotify.api.translator.TranslatorType;
import org.osgi.service.component.annotations.Component;

/**
 * Provides a pass through marker processor factory service.
 * @author Joel Håkansson
 * @deprecated without replacement
 */
@Component
@Deprecated
public class DefaultBypassMarkerProcessorFactoryService implements
		MarkerProcessorFactoryService {

	@Override
	public boolean supportsSpecification(String locale, String mode) {
		return mode.equals(TranslatorType.BYPASS.toString());
	}

	@Override
	public MarkerProcessorFactory newFactory() {
		return new DefaultBypassMarkerProcessorFactory();
	}

	@Override
	public void setCreatedWithSPI() {
	}

}
