package de.in.autoMower.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MathUtilTest {

	@Test
	void isEqualTrueTest() {
		float a = 4f;
		float b = 4f;
		boolean result = MathUtil.isEqual(a, b);
		assertEquals(true, result);
	}

	@Test
	void isEqualFalseTest() {
		float a = 4f;
		float b = 5f;
		boolean result = MathUtil.isEqual(a, b);
		assertEquals(false, result);
	}
}
