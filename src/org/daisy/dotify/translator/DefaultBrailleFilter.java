package org.daisy.dotify.translator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.daisy.dotify.api.hyphenator.HyphenatorConfigurationException;
import org.daisy.dotify.api.hyphenator.HyphenatorFactoryMakerService;
import org.daisy.dotify.api.hyphenator.HyphenatorInterface;
import org.daisy.dotify.api.translator.AttributeWithContext;
import org.daisy.dotify.api.translator.BrailleFilter;
import org.daisy.dotify.api.translator.ResolvableText;
import org.daisy.dotify.api.translator.Translatable;
import org.daisy.dotify.api.translator.TranslatableWithContext;
import org.daisy.dotify.api.translator.TranslationException;
import org.daisy.dotify.common.text.StringFilter;

/**
 * Provides a configurable braille filter, in cases where a full implementation
 * is not needed. This implementation first translates the markers, then hyphenates
 * the text, and then sends the result to the supplied string filter.
 * @author Joel Håkansson
 *
 */
public class DefaultBrailleFilter implements BrailleFilter {
	private static final Logger LOGGER = Logger.getLogger(DefaultBrailleFilter.class.getCanonicalName());	
	private static final Set<String> HYPH_STYLES = new HashSet<>(Arrays.asList("em", "strong"));

	private final String loc;
	private final StringFilter filter;
	private final DefaultMarkerProcessor tap;
	private final HyphenatorFactoryMakerService hyphenatorFactoryMaker;
	private final Map<String, HyphenatorInterface> hyphenators;
	
	/**
	 * Creates a new default braille filter with the supplied parameters.
	 * @param filter the braille filter to use
	 * @param locale the locale of the implementation
	 * @param hyphenatorFactoryMaker the hyphenator factory maker
	 */
	public DefaultBrailleFilter(StringFilter filter, String locale, HyphenatorFactoryMakerService hyphenatorFactoryMaker) {
		this(filter, locale, null, hyphenatorFactoryMaker);
	}
	
	/**
	 * Creates a new default braille filter with the supplied parameters.
	 * @param filter the braille filter to use
	 * @param locale the locale of the implementation
	 * @param tap the marker processor
	 * @param hyphenatorFactoryMaker the hyphenator factory maker
	 */
	public DefaultBrailleFilter(StringFilter filter, String locale, DefaultMarkerProcessor tap, HyphenatorFactoryMakerService hyphenatorFactoryMaker) {
		this.loc = locale;
		this.filter = filter;
		this.tap = tap;
		this.hyphenators = new HashMap<>();
		this.hyphenatorFactoryMaker = hyphenatorFactoryMaker;
	}

	@Override
	public String filter(Translatable specification) throws TranslationException {
		if (specification.getText().isEmpty()) {
			return "";
		}
		String locale = specification.getLocale();
		if (locale==null) {
			locale = loc;
		}
		
		String text = specification.getText();
		
		if (!specification.shouldMarkCapitalLetters()) {
			//TODO: toLowerCase may not always do what we want here,
			//it depends on the lower case algorithm and the rules 
			//of the braille for that language
			text = text.toLowerCase(Locale.ROOT);
		}
		
		if (specification.isHyphenating()) {
			HyphenatorInterface h = hyphenators.get(locale);
			if (h == null) {
				try {
					h = hyphenatorFactoryMaker.newHyphenator(locale);
				} catch (HyphenatorConfigurationException e) {
					throw new DefaultBrailleFilterException(e);
				}
				hyphenators.put(locale, h);
			}
			text = h.hyphenate(text);
		}
		
		if (tap != null) {
			text = tap.processAttributes(specification.getAttributes(), text);
		}

		return filter.filter(text);
	}

	@Override
	public String filter(TranslatableWithContext specification) throws TranslationException {
		if (specification.getTextToTranslate().isEmpty()) {
			return "";
		}
		Stream<String> inStream = specification.getTextToTranslate().stream().map(v->v.resolve());
		List<String> texts;
		
		if (tap != null && specification.getAttributes().isPresent()) {
			Stream<String> preceding = specification.getPrecedingText().stream().map(v->v.resolve());
			Stream<String> following = specification.getFollowingText().stream().map(v->v.peek());
			List<String> textsI = Stream.concat(Stream.concat(preceding, inStream), following).collect(Collectors.toList());
			String[] out = tap.processAttributesRetain(specification.getAttributes().get(), textsI);
			int start = specification.getPrecedingText().size();
			int end = start + specification.getTextToTranslate().size();
			texts = Arrays.asList(out).subList(start, end);
		} else {
			texts = inStream.collect(Collectors.toList());
		}
		
		// We've checked that there is at least one text to translate
		Optional<String> l = specification.getTextToTranslate().get(0).getLocale();
		boolean h = specification.getTextToTranslate().get(0).shouldHyphenate();
		boolean m = specification.getTextToTranslate().get(0).shouldMarkCapitalLetters();
		
		int i = 0;
		StringBuilder ret = new StringBuilder();
		StringBuilder toTranslate = new StringBuilder();
		for (ResolvableText t : specification.getTextToTranslate()) {
			if (!t.getLocale().equals(l) || t.shouldHyphenate()!=h || t.shouldMarkCapitalLetters()!=m) {
				//Flush
				Translatable.Builder b = Translatable.text(toTranslate.toString())
						.hyphenate(h)
						.markCapitalLetters(m);
				if (l.isPresent()) {
					b.locale(l.get());
				}
				ret.append(filter(b.build()));
				// Set
				l = t.getLocale();
				h = t.shouldHyphenate();
				m = t.shouldMarkCapitalLetters();
				toTranslate = new StringBuilder();
			}
			toTranslate.append(texts.get(i));
			i++;
		}
		if (toTranslate.length()>0) {
			//Flush
			Translatable.Builder b = Translatable.text(toTranslate.toString())
					.hyphenate(h)
					.markCapitalLetters(m);
			if (l.isPresent()) {
				b.locale(l.get());
			}
			ret.append(filter(b.build()));
		}
		return ret.toString();
	}
	
	private List<String> processCapitalLettersAndHyphenate(TranslatableWithContext specification, List<String> texts) {
		List<String> ret = new ArrayList<>();
		int i = 0;
		int len = texts.size();
		for (ResolvableText v : specification.getTextToTranslate()) {
			String text = texts.get(i);
			if (!v.shouldMarkCapitalLetters()) {
				//TODO: toLowerCase may not always do what we want here,
				//it depends on the lower case algorithm and the rules 
				//of the braille for that language
				text = text.toLowerCase(Locale.ROOT);
			}
			if (v.shouldHyphenate()) {
				String locale = v.getLocale().orElse(loc);
				HyphenatorInterface h = hyphenators.get(locale);
				if (h == null) {
					try {
						h = hyphenatorFactoryMaker.newHyphenator(locale);
					} catch (HyphenatorConfigurationException e) {
						if (LOGGER.isLoggable(Level.WARNING)) {
							LOGGER.log(Level.WARNING, String.format("Failed to create hyphenator for %s", locale), e);
						}
					}
					hyphenators.put(locale, h);
				}
				
				text = h.hyphenate(text);
				// If there is a following segment and this segment ends with a letter
				if (len>i+1 && Character.isLetter(text.charAt(text.length()-1))) {
					ResolvableText next = specification.getTextToTranslate().get(i+1);
					String nextStr = texts.get(i+1);
					if (next.shouldHyphenate()
							&& locale.equals(next.getLocale().orElse(loc)) 
							&& nextStr.length()>0 
							&& Character.isLetter(nextStr.charAt(0))
							) {
						String textN = text + nextStr;
						// Since this segment has already been translated it should not change, therefore we can just remove the substring and see if it starts with a soft hyphen
						if (h.hyphenate(textN).substring(text.length()).startsWith("\u00ad")) {
							text = text + "\u00ad";
						}
					}
				}
			}
			ret.add(text);
			i++;
		}
		return ret;
	}

	private static long[] toLinearForm(AttributeWithContext attr) {
		return toLinearForm(attr, HYPH_STYLES);
	}
	
	static long[] toLinearForm(AttributeWithContext attr, Set<String> filter) {
		return toLinearForm(attr, new HashMap<>(), filter);
	}

	private static long[] toLinearForm(AttributeWithContext attr, Map<String, Long> map, Set<String> filter) {
		long idNumber = 0;
		if (attr.getName().isPresent()) {
			String name = attr.getName().get();
			if (!filter.contains(name)) {
				Long idNum = map.get(name);
				if (idNum!=null) {
					idNumber = idNum.longValue();
				} else {
					// All id's are powers of 2 so that they can be combined without losing information
					// When the map is empty, the new id is 1. The second time it's 2, then 4 etc.
					if (map.size()>62) {
						if (LOGGER.isLoggable(Level.WARNING)) {
							LOGGER.log(Level.WARNING, "Too many styles, ignoring style " + name);
						}
					} else {
						idNumber = 1 << map.size();
						map.put(name, idNumber);
					}
				}
			}
		}

		long[] ret = new long[attr.getWidth()];
		if (attr.hasChildren()) {
			int offset = 0;
			for (AttributeWithContext t : attr) {
				long[] v = toLinearForm(t, map, filter);
				for (int i=0; i<v.length; i++) {
					ret[i+offset] = idNumber | v[i];
				}
				offset += t.getWidth();
			}
		} else {
			Arrays.fill(ret, idNumber);
		}

		return ret;
	}

	private class DefaultBrailleFilterException extends TranslationException {

		/**
		 * 
		 */
		private static final long serialVersionUID = 6102686243949860112L;

		DefaultBrailleFilterException(Throwable cause) {
			super(cause);
		}
		
	}

}
