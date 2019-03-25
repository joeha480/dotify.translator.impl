package org.daisy.dotify.translator.impl.liblouis.java;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.daisy.dotify.api.translator.MarkerProcessor;
import org.daisy.dotify.common.braille.BrailleNotationConverter;
import org.daisy.dotify.common.text.TextFileReader;
import org.daisy.dotify.translator.DefaultMarkerProcessor;

class LiblouisFileReader {
	private final ResourceResolver rr;
	private final LiblouisBrailleFilter.Builder cr;
	private final Map<String, LiblouisEmphasisManager> emp;
	private final BrailleNotationConverter nc;
	
	private final Logger logger;

	/**
	 * Creates a new empty filter.
	 * @param resolver a resource resolver
	 */
	public LiblouisFileReader(ResourceResolver resolver) {
		this.rr = resolver;
		this.cr = new LiblouisBrailleFilter.Builder();
		this.emp = new HashMap<>();
		this.nc = new BrailleNotationConverter("-");
		this.logger = Logger.getLogger(this.getClass().getCanonicalName());
	}
	
	public LiblouisFileReader() {
		this(new ClassLoaderResourceResolver("resource-files/", Charset.forName("utf-8")));
	}
	
	public void parse(String path) throws IOException {
		parse(rr.resolve(path));
	}
	
	public LiblouisBrailleFilter getFilter() {
		return cr.build();
	}
	
	public MarkerProcessor buildMarkerProcessor() {
		DefaultMarkerProcessor.Builder ret = new DefaultMarkerProcessor.Builder();
		emp.entrySet().forEach(entry->{
			ret.addDictionary(entry.getKey(), entry.getValue().make());
		});
		return ret.build();
	}

	/**
	 * Parses a Liblouis input stream with the specified encoding and
	 * adds its contents to the filter.
	 * @param resource a resource descriptor
	 * @throws IOException if the resource cannot be read
	 */
	public void parse(ResourceDescriptor resource) throws IOException {
		TextFileReader tfr = new TextFileReader.Builder(resource.getInputStream()).
				charset(resource.getEncoding()).
				regex("\\s").build();
		
		TextFileReader.LineData ld;
		while ((ld= tfr.nextLine())!=null) {
			String[] f = ld.getFields();
			if ("uplow".equals(f[0])) {
				addUplow(f[1], f[2]);
			} else if ("punctuation".equals(f[0])) {
				addEntry(f[1], f[2], CharClass.PUNCTUATION);
			} else if ("space".equals(f[0])) {
				addEntry(f[1], f[2], CharClass.SPACE);
			} else if ("sign".equals(f[0])) {
				addEntry(f[1], f[2], CharClass.SIGN);
			} else if ("math".equals(f[0])) {
				addEntry(f[1], f[2], CharClass.MATH);
			} else if ("lowercase".equals(f[0])) {
				addEntry(f[1], f[2], CharClass.LOWERCASE);
			} else if ("uppercase".equals(f[0])) {
				addEntry(f[1], f[2], CharClass.UPPERCASE);
			} else if ("digit".equals(f[0])) {
				addEntry(f[1], f[2], CharClass.DIGIT);
			}
			else if ("include".equals(f[0])) {
				try {
					ResourceDescriptor rd = rr.resolve(f[1]);
					if (rd!=null) {
						parse(rd);
					} else {
						File f2 = new File(f[1]);
						if (f2.isFile()) {
							parse(new ResourceDescriptor(new FileInputStream(f2), resource.getEncoding()));
						} else {
							throw new FileNotFoundException(f[1]);
						}
					}
				} catch (IOException e) {
					logger.warning("Include not found: " + f[1]);
				}
			} 
			else if ("display".equals(f[0]) || "locale".equals(f[0])) {
				//ignore
			} else if ("numsign".equals(f[0])) {
				System.out.println("NUMSIGN");
				cr.numsign(nc.parseBrailleNotation(f[1]));
			} else if ("capsign".equals(f[0])) {
				cr.capsign(nc.parseBrailleNotation(f[1]));
			} else if ("begemph".equals(f[0])) {
				getOrAddEmphasis(f[1]).getNoContext().setPrefix(nc.parseBrailleNotation(f[2]));
			} else if ("endemph".equals(f[0])) {
				getOrAddEmphasis(f[1]).getNoContext().setPostfix(nc.parseBrailleNotation(f[2]));
			} else if ("emphletter".equals(f[0])) {
				getOrAddEmphasis(f[1]).getLetter().setPrefix(nc.parseBrailleNotation(f[2]));
			} else if ("begemphword".equals(f[0])) {
				getOrAddEmphasis(f[1]).getWord().setPrefix(nc.parseBrailleNotation(f[2]));
			} else if ("endemphword".equals(f[0])) {
				getOrAddEmphasis(f[1]).getWord().setPostfix(nc.parseBrailleNotation(f[2]));
			} else if ("begemphphrase".equals(f[0])) {
				getOrAddEmphasis(f[1]).getPhrase().setPrefix(nc.parseBrailleNotation(f[2]));
			} else if ("endemphphrase".equals(f[0])) {
				LiblouisEmphasisManager m = getOrAddEmphasis(f[1]);
				m.getPhrase().setPostfix(nc.parseBrailleNotation(f[3]));
				if ("before".equals(f[2])) {
					m.getPhrase().setBeforeLastWord(true);
				} else if ("after".equals(f[2])) {
					m.getPhrase().setBeforeLastWord(false);
				} else {
					//Ignore
				}
			} else if ("lenemphphrase".equals(f[0])) {
				getOrAddEmphasis(f[1]).getPhrase().setPhraseLength(Integer.parseInt(f[2]));
			}
			else {
				System.out.println("Not implemented: " + ld.getLine());
			}
		}
	}
	
	private LiblouisEmphasisManager getOrAddEmphasis(String label) {
		LiblouisEmphasisManager ret = emp.get(label);
		if (ret==null) {
			ret = new LiblouisEmphasisManager();
			emp.put(label, ret);
		}
		return ret;
	}
	
	public void addEntry(String value, String replacement, CharClass group) {
		int key = StringProcessor.unescape(value).codePointAt(0);
		cr.put(key, nc.parseBrailleNotation(replacement), group);
	}
	
	public void addUplow(String op, String value) {
		op = StringProcessor.unescape(op);
		if (op.length()!=2) {
			Logger.getLogger(this.getClass().getCanonicalName()).info("Uplow op incorrect: " + op);
		} else {
			String[] r = StringProcessor.unescape(value).split(",");
			if (r.length>2 || r.length<1) {
				Logger.getLogger(this.getClass().getCanonicalName()).info("Uplow value incorrect: " + value);
			} else {
				cr.put((int)op.charAt(0), nc.parseBrailleNotation(r[0]), CharClass.UPPERCASE);
				cr.put((int)op.charAt(1), nc.parseBrailleNotation(r[(r.length>1?1:0)]), CharClass.LOWERCASE);
			}
		}
	}

}
