package org.daisy.dotify.translator;

import static org.junit.Assert.assertArrayEquals;

import java.util.Collections;
import java.util.HashMap;

import org.daisy.dotify.api.translator.AttributeWithContext;
import org.daisy.dotify.api.translator.DefaultAttributeWithContext;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class DefaultBrailleFilterTest {

	@Test
	public void testToLinearForm_01() {
		AttributeWithContext ta = new DefaultAttributeWithContext.Builder()
				.add(5)
				.add(
					new DefaultAttributeWithContext.Builder("italic")
					.add(2)
					.add(new DefaultAttributeWithContext.Builder("bold").build(2))
					.build(4))
				.add(new DefaultAttributeWithContext.Builder("bold").build(5))
				.build(14);

		long[] expecteds = new long[] {0,0,0,0,0,1,1,3,3,2,2,2,2,2};
		long[] actuals = DefaultBrailleFilter.toLinearForm(ta, Collections.emptySet());
		assertArrayEquals(expecteds, actuals);
	}
	
	@Test
	public void testToLinearForm_02() {
		AttributeWithContext ta = new DefaultAttributeWithContext.Builder()
				.add(5)
				.add(new DefaultAttributeWithContext.Builder("em").add(4).build(4))
				.add(5)
				.build(14);

		long[] expecteds = new long[] {0,0,0,0,0,1,1,1,1,0,0,0,0,0};
		long[] actuals = DefaultBrailleFilter.toLinearForm(ta, Collections.emptySet());
		assertArrayEquals(expecteds, actuals);
	}
	
	@Test
	public void testToLinearForm_03() {
		AttributeWithContext ta = new DefaultAttributeWithContext.Builder("strong")
				.add(5)
				.add(new DefaultAttributeWithContext.Builder("em").add(4).build(4))
				.add(5)
				.build(14);

		long[] expecteds = new long[] {1,1,1,1,1,3,3,3,3,1,1,1,1,1};
		long[] actuals = DefaultBrailleFilter.toLinearForm(ta, Collections.emptySet());
		assertArrayEquals(expecteds, actuals);
	}
}
