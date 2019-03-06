package org.daisy.dotify.translator.impl.liblouis;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.daisy.dotify.api.hyphenator.HyphenatorConfigurationException;
import org.daisy.dotify.api.hyphenator.HyphenatorFactoryMakerService;
import org.daisy.dotify.api.hyphenator.HyphenatorInterface;
import org.daisy.dotify.api.translator.BrailleFilter;
import org.daisy.dotify.api.translator.TextAttribute;
import org.daisy.dotify.api.translator.Translatable;
import org.daisy.dotify.api.translator.TranslationException;
import org.daisy.dotify.api.translator.TranslatorMode;
import org.daisy.dotify.api.translator.TranslatorMode.DotsPerCell;
import org.daisy.dotify.api.translator.TranslatorSpecification;
import org.daisy.dotify.api.translator.TranslatorType;
import org.liblouis.CompilationException;
import org.liblouis.Louis;
import org.liblouis.TranslationResult;
import org.liblouis.Translator;

class LiblouisBrailleFilter implements BrailleFilter {
	private static final int SOFT_HYPHEN = 0x00ad;
	private static final int ZERO_WIDTH_SPACE = 0x200b;
	private static final int LIBLOUIS_NO_BREAKPOINT = 0;
	private static final int LIBLOUIS_SOFT_HYPEN = 1;
	private static final int LIBLOUIS_ZERO_WIDTH_SPACE = 2;
	private static final String UNICODE_TABLE = LiblouisTables.getInstance().getTablePath("unicode.dis").toString();
	private static final Map<String, Integer> MARKERS = makeMarkersMap();
	private static Map<TranslatorSpecification, String> specs;
	private final String loc;
	private final HyphenatorFactoryMakerService hyphenatorFactoryMaker;
	private final Map<String, HyphenatorInterface> hyphenators;
	private final Translator table;

	LiblouisBrailleFilter(TranslatorSpecification ts, HyphenatorFactoryMakerService hyphenatorFactoryMaker) {
		this.loc = ts.getLocale();
		this.hyphenatorFactoryMaker = hyphenatorFactoryMaker;
		this.hyphenators = new HashMap<>();
		// Not sure if it is possible to list the supported locales.
		// Query format:
		//		http://liblouis.org/documentation/liblouis.html#Query-Syntax-1
		//		https://github.com/liblouis/liblouis/wiki/Table-discovery-based-on-table-metadata#standard-metadata-tags
		//		String tbl = Louis.getLibrary().lou_findTable("locale:"+locale);
		buildSpecs();
		String spec = specs.get(ts);
		String tbl = LiblouisTables.getInstance().getTablePath(spec).toString();
		try {
			table = new Translator(UNICODE_TABLE+","+tbl);
		} catch (CompilationException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	static Map<String, Integer> makeMarkersMap() {
		Map<String, Integer> ret = new HashMap<>();
		ret.put("em", 1);
		ret.put("strong", 4);
		return Collections.unmodifiableMap(ret);
	}
	
	static boolean isSupported() {
		try {
			return Louis.getLibrary()!=null;
		} catch (Exception | UnsatisfiedLinkError e) {
			return false;
		}
	}
	
	static synchronized List<TranslatorSpecification> buildSpecs() {
		if (specs == null) {
			specs = new HashMap<>();
			if (isSupported()) {
				//specs.put(new TranslatorSpecification.Builder("", "").eightDot().build(), "");
				//specs.put(new TranslatorSpecification("", ""), "");

				specs.put(new TranslatorSpecification("en", TranslatorMode.withType(TranslatorType.UNCONTRACTED)), "en-us-g1.ctb");
				specs.put(new TranslatorSpecification("en", TranslatorMode.withGrade(1)), "en-us-g1.ctb");

				specs.put(new TranslatorSpecification("en-US", TranslatorMode.withType(TranslatorType.UNCONTRACTED)), "en-us-g1.ctb");
				specs.put(new TranslatorSpecification("en-US", TranslatorMode.withGrade(1)), "en-us-g1.ctb");

				specs.put(new TranslatorSpecification("fi", TranslatorMode.withType(TranslatorType.UNCONTRACTED)), "fi.utb");

				specs.put(new TranslatorSpecification("no", TranslatorMode.withType(TranslatorType.CONTRACTED)), "no.tbl");
				specs.put(new TranslatorSpecification("no", TranslatorMode.withGrade(3)), "no.tbl");

				specs.put(new TranslatorSpecification("de",TranslatorMode.withGrade(0)), "de-g0.utb");
				specs.put(new TranslatorSpecification("de", TranslatorMode.withType(TranslatorType.UNCONTRACTED)), "de-g0.utb");

				specs.put(new TranslatorSpecification("da-dk", TranslatorMode.Builder.withGrade(1.5).dotsPerCell(DotsPerCell.EIGHT).build()), "da-dk-g28l.ctb");
				specs.put(new TranslatorSpecification("da-dk", TranslatorMode.Builder.withType(TranslatorType.CONTRACTED).dotsPerCell(DotsPerCell.EIGHT).build()), "da-dk-g28.ctb");
				specs.put(new TranslatorSpecification("da-dk", TranslatorMode.Builder.withGrade(2).dotsPerCell(DotsPerCell.EIGHT).build()), "da-dk-g28.ctb");
				specs.put(new TranslatorSpecification("da-dk", TranslatorMode.withGrade(1.5)), "da-dk-g26l.ctb");
				specs.put(new TranslatorSpecification("da-dk", TranslatorMode.withType(TranslatorType.CONTRACTED)), "da-dk-g26.ctb");
				specs.put(new TranslatorSpecification("da-dk", TranslatorMode.withGrade(2)), "da-dk-g26.ctb");
				specs.put(new TranslatorSpecification("da-dk", TranslatorMode.Builder.withType(TranslatorType.UNCONTRACTED).dotsPerCell(DotsPerCell.EIGHT).build()), "da-dk-g18.ctb");
				specs.put(new TranslatorSpecification("da-dk", TranslatorMode.Builder.withGrade(1).dotsPerCell(DotsPerCell.EIGHT).build()), "da-dk-g18.ctb");
				specs.put(new TranslatorSpecification("da-dk", TranslatorMode.withType(TranslatorType.UNCONTRACTED)), "da-dk-g16.ctb");
				specs.put(new TranslatorSpecification("da-dk", TranslatorMode.withGrade(1)), "da-dk-g16.ctb");
			}
		}
		return specs.keySet().stream().collect(Collectors.toList());
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
		HyphenatorInterface h = hyphenators.get(locale);
		if (h == null && specification.isHyphenating()) {
			// if we're not hyphenating the language in question, we do not
			// need to add it, nor throw an exception if it cannot be found.
			try {
				h = hyphenatorFactoryMaker.newHyphenator(locale);
			} catch (HyphenatorConfigurationException e) {
				throw new LiblouisBrailleFilterException(e);
			}
			hyphenators.put(locale, h);
		}
		String str = specification.isHyphenating()?h.hyphenate(specification.getText()):specification.getText();
		//translate braille using the same filter, regardless of language
		LiblouisTranslatable louisSpec = toLiblouisSpecification(str, specification, MARKERS);
		try {
			return toBrailleFilterString(table.translate(louisSpec.getText(), louisSpec.getTypeForm(), null, louisSpec.getInterCharAtts()));
		} catch (org.liblouis.TranslationException e) {
			throw new LiblouisBrailleFilterException(e);
		}
	}

	/**
	 * Maps a translatable and the corresponding hyphenated string to a set of data that can be 
	 * used with Liblouis. The hyphenated string is used to set the intercharacter attributes.
	 * The map is used for creating a type form array from the translatable's text attributes.
	 * 
	 * @param hyphStr the hyphenated string
	 * @param spec the translatable
	 * @param map the "type form" map, may be null
	 * @return hyphenation information
	 */
	static LiblouisTranslatable toLiblouisSpecification(String hyphStr, Translatable spec, Map<String, Integer> map) {
		String inputStr = spec.getText();
		if (hyphStr.length() < inputStr.length()) {
			throw new IllegalArgumentException("The hyphenated string cannot be shorter than the input string");
		}
		TextAttribute ta = spec.getAttributes();
		short[] typeForm;
		if (ta==null || map==null) {
			typeForm = new short[(int)spec.getText().codePoints().count()];
		} else {
			typeForm = toTypeForm(spec.getAttributes(), map);
		}
		int[] cpHyph = hyphStr.codePoints().toArray();
		int[] cpInput = inputStr.codePoints().toArray();
		int j=0;
		int flag;
		int[] interCharAttr = new int[cpInput.length-1];
		for (int i=0; i<cpInput.length; i++) {
			flag = LIBLOUIS_NO_BREAKPOINT;
			while (j<cpHyph.length && i<cpInput.length-1 && cpInput[i+1]!=cpHyph[j]) {
				if (cpHyph[j]==SOFT_HYPHEN) {
					flag = LIBLOUIS_SOFT_HYPEN;
				} else if (cpHyph[j]==ZERO_WIDTH_SPACE && flag!=LIBLOUIS_SOFT_HYPEN) {
					flag = LIBLOUIS_ZERO_WIDTH_SPACE;
				} else if (cpInput[i]!=cpHyph[j] && cpInput[i+1]!=cpHyph[j+1]) {
					throw new RuntimeException("'"+hyphStr + ":" + inputStr+"'");
				}
				j++;
			}
			j++;
			if (i<cpInput.length-1) {
				interCharAttr[i] = flag;
			}
		}
		return new LiblouisTranslatable(inputStr, interCharAttr, typeForm);
	}
	
	/**
	 * Converts a text attribute to its "type form" equivalent. Note that type form
	 * values should be a power of two, since they can be superimposed to create
	 * composite type forms. For example: "italic"=>1, "underline"=>2 and "bold"=>4
	 * @param attr the text attribute
	 * @param map the text attribute name to type form value map
	 * @return returns an array with the corresponding values
	 */
	static short[] toTypeForm(TextAttribute attr, Map<String, Integer> map) {
		short[] ret = new short[attr.getWidth()];
		short typeForm = 0;
		if (attr.getDictionaryIdentifier()!=null) {
			Short v = map.get(attr.getDictionaryIdentifier()).shortValue();
			if (v!=null) {
				typeForm = v;
			}
		}
		
		if (attr.hasChildren()) {
			int offset = 0;
			for (TextAttribute t : attr) {
				short[] v = toTypeForm(t, map);
				for (int i=0; i<v.length; i++) {
					ret[i+offset] = (short)(typeForm | v[i]);
				}
				offset += t.getWidth();
			}
		} else {
			for (int i=0; i<ret.length; i++) {
				ret[i] = typeForm;
			}
		}
		return ret;
	}
	
	private static String toBrailleFilterString(TranslationResult res) {
		return toBrailleFilterString(res.getBraille(), res.getInterCharacterAttributes());
	}
	
	/**
	 * Modifies a string from Liblouis into a string that is compatible with {@link BrailleFilter}
	 * by adding hyphenation characters (soft hyphen and zero width space). 
	 * @param str the Liblouis string
	 * @param interCharAttr the inter char attributes.
	 * @return a string
	 */
	static String toBrailleFilterString(String str, int[] interCharAttr) {
		StringBuilder sb = new StringBuilder();
		int[] codePoints = str.codePoints().toArray();
		for (int i=0; i<codePoints.length; i++) {
			sb.appendCodePoint(codePoints[i]);
			if (i<interCharAttr.length) {
				switch (interCharAttr[i]) {
					case LIBLOUIS_NO_BREAKPOINT: break;
					case LIBLOUIS_SOFT_HYPEN: sb.append('\u00ad'); break;
					case LIBLOUIS_ZERO_WIDTH_SPACE: sb.append('\u200b'); break;
					default:
				}
			}
		}
		return sb.toString();
	}

}
