/*
*                C O P Y R I G H T  (c) 2022
*                        DEDALUS SPA
*                    All Rights Reserved
*
*
*      THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF
*                        DEDALUS SPA
*     The copyright notice above does not evidence any
*    actual or intended publication of such source code.
*/
package de.in.autoMower.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

/**
 * @author Christopher Schmidt <christopher.schmidt@dedalus.com>
 */
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

		assertEquals(true, result);

	}

	@Test
	void test() {
		fail("Not yet implemented");
	}

}
