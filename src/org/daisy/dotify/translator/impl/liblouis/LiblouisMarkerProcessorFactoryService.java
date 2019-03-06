package org.daisy.dotify.translator.impl.liblouis;

import java.util.List;

import org.daisy.dotify.api.translator.MarkerProcessorFactory;
import org.daisy.dotify.api.translator.MarkerProcessorFactoryService;
import org.daisy.dotify.api.translator.TranslatorSpecification;

public class LiblouisMarkerProcessorFactoryService implements MarkerProcessorFactoryService {
	private final List<TranslatorSpecification> specs;
	
	public LiblouisMarkerProcessorFactoryService() {
		this.specs = LiblouisBrailleFilter.buildSpecs();
	}

	@Override
	public boolean supportsSpecification(String locale, String mode) {
		TranslatorSpecification target = new TranslatorSpecification(locale, mode);
		for (TranslatorSpecification spec : specs) {
			if (target.equals(spec)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public MarkerProcessorFactory newFactory() {
		return new LiblouisMarkerProcessorFactory();
	}

}
