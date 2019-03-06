package org.daisy.dotify.translator.impl.liblouis;

import org.daisy.dotify.api.translator.MarkerProcessor;
import org.daisy.dotify.api.translator.MarkerProcessorConfigurationException;
import org.daisy.dotify.api.translator.MarkerProcessorFactory;
import org.daisy.dotify.translator.DefaultMarkerProcessor;

public class LiblouisMarkerProcessorFactory implements MarkerProcessorFactory {

	@Override
	public MarkerProcessor newMarkerProcessor(String locale, String mode) throws MarkerProcessorConfigurationException {
		return new DefaultMarkerProcessor.Builder().build();
	}

}
