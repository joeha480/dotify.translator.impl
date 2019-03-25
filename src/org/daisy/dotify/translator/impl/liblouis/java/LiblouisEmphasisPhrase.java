package org.daisy.dotify.translator.impl.liblouis.java;

class LiblouisEmphasisPhrase extends LiblouisEmphasis {
	private int phraseLength = 2;
	private boolean beforeLastWord = false;

	int getPhraseLength() {
		return phraseLength;
	}

	void setPhraseLength(int phraseLength) {
		this.phraseLength = phraseLength;
	}

	boolean placeBeforeLastWord() {
		return beforeLastWord;
	}

	void setBeforeLastWord(boolean beforeLastWord) {
		this.beforeLastWord = beforeLastWord;
	}

}
