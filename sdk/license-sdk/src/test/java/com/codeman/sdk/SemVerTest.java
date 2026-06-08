package com.codeman.sdk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SemVerTest {

    @Test
    void compareWorks() {
        assertTrue(SemVer.compare("2.4.0", "2.3.9") > 0);
        assertTrue(SemVer.compare("2.0.0", "2.0.0") == 0);
        assertTrue(SemVer.compare("v2.1.0", "2.2.0") < 0);
    }

    @Test
    void rangeSatisfies() {
        assertTrue(SemVer.satisfies("2.4.0", ">=2.0.0 <3.0.0"));
        assertFalse(SemVer.satisfies("3.1.0", ">=2.0.0 <3.0.0"));
        assertFalse(SemVer.satisfies("1.9.0", ">=2.0.0 <3.0.0"));
        assertTrue(SemVer.satisfies("2.4.0", ""));          // 空区间始终满足
        assertTrue(SemVer.satisfies("2.3.0", ">=2.3.0"));
    }
}
