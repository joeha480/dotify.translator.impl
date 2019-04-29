package org.daisy.dotify.translator.impl;

import org.daisy.dotify.api.translator.MarkerProcessor;
import org.daisy.dotify.api.translator.MarkerProcessorConfigurationException;
import org.daisy.dotify.api.translator.MarkerProcessorFactory;
import org.daisy.dotify.api.translator.TranslatorType;
import org.daisy.dotify.translator.DefaultMarkerProcessor;

@Deprecated
class PreTranslatedMarkerProcessorFactory implements
		MarkerProcessorFactory {

	@Override
	public MarkerProcessor newMarkerProcessor(String locale, String mode) throws MarkerProcessorConfigurationException {
		if (TranslatorType.PRE_TRANSLATED.toString().equals(mode)) {
			return new DefaultMarkerProcessor.Builder().build();
		}
		throw new DefaultBypassMarkerProcessorConfigurationException("Factory does not support " + locale + "/" + mode);
	}

	private class DefaultBypassMarkerProcessorConfigurationException extends MarkerProcessorConfigurationException {

		/**
		 * 
		 */
		private static final long serialVersionUID = 7831296813639319600L;

		private DefaultBypassMarkerProcessorConfigurationException(String message) {
			super(message);
		}
		
	}

}
