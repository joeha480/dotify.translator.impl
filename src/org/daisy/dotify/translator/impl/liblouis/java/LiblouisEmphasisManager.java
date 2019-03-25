package org.daisy.dotify.translator.impl.liblouis.java;

import org.daisy.dotify.translator.MarkerDictionary;
import org.daisy.dotify.translator.RegexMarkerDictionary;

class LiblouisEmphasisManager {
	private final LiblouisEmphasis noContext = new LiblouisEmphasis();
	private final LiblouisEmphasis letter = new LiblouisEmphasis();
	private final LiblouisEmphasis word = new LiblouisEmphasis();
	private final LiblouisEmphasisPhrase phrase = new LiblouisEmphasisPhrase();

	LiblouisEmphasis getNoContext() {
		return noContext;
	}

	LiblouisEmphasis getLetter() {
		return letter;
	}

	LiblouisEmphasis getWord() {
		return word;
	}
	
	LiblouisEmphasisPhrase getPhrase() {
		return phrase;
	}
	
	MarkerDictionary make() {
		RegexMarkerDictionary def = new RegexMarkerDictionary.Builder()
				.build();
		return def;
	}

}
