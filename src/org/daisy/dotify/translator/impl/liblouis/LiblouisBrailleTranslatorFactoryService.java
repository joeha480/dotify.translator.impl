package org.daisy.dotify.translator.impl.liblouis;

import java.util.Collection;
import java.util.List;

import org.daisy.dotify.api.hyphenator.HyphenatorFactoryMaker;
import org.daisy.dotify.api.hyphenator.HyphenatorFactoryMakerService;
import org.daisy.dotify.api.translator.BrailleTranslatorFactory;
import org.daisy.dotify.api.translator.BrailleTranslatorFactoryService;
import org.daisy.dotify.api.translator.TranslatorSpecification;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

@Component
public class LiblouisBrailleTranslatorFactoryService implements BrailleTranslatorFactoryService {
	private final List<TranslatorSpecification> specs;
	private HyphenatorFactoryMakerService hyphenator = null;
	
	public LiblouisBrailleTranslatorFactoryService() {
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
	public Collection<TranslatorSpecification> listSpecifications() {
		return specs;
	}

	@Override
	public BrailleTranslatorFactory newFactory() {
		return new LiblouisBrailleTranslatorFactory(hyphenator);
	}
	
	@Override
	public void setCreatedWithSPI() {
		setHyphenator(HyphenatorFactoryMaker.newInstance());
	}

	/**
	 * Sets the hyphenator factory maker service.
	 * @param hyphenator the hyphenator factory maker service.
	 */
	@Reference(cardinality=ReferenceCardinality.MANDATORY)
	public void setHyphenator(HyphenatorFactoryMakerService hyphenator) {
		this.hyphenator = hyphenator;
	}

	/**
	 * Unsets the hyphenator factory maker service.
	 * @param hyphenator the instance to unset.
	 */
	public void unsetHyphenator(HyphenatorFactoryMakerService hyphenator) {
		this.hyphenator = null;
	}

}
