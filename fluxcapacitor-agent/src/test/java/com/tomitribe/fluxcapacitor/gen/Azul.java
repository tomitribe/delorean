/*
 * Tomitribe Confidential
 *
 * Copyright Tomitribe Corporation. 2017
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.tomitribe.fluxcapacitor.gen;

import java.util.concurrent.atomic.AtomicBoolean;

public interface Azul {

    AtomicBoolean blue = new AtomicBoolean();

    void other(final Purple purple, final Azul blue) throws Red;

}
