package com.amazingshop.personal.userservice;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

class AlwaysFailTest {
    @Test void boom() { fail("boom"); }
}