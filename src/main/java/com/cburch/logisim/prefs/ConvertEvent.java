/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.prefs;

import com.cburch.logisim.data.AttributeOption;

/**
 * NOTE: silly members' names are mostly to avoid refactoring of the whole codebase due to
 * Java's Record's getters not using Bean naming convention (so i.e. we get `foo()` instead
 * of `getFoo()`. We may change that in future, but for now it looks silly in this file only.
 */
public record ConvertEvent(AttributeOption getValue) {}
