/*
 * DownReference.java --
 *
 * tcljava/tests/DownReference.java
 *
 * Copyright (c) 1998 by Moses DeJong
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: DownReference.java,v 1.2.1.1 1999/01/29 20:52:09 mo Exp $
 *
 */


package tests;

import java.util.*;

public class DownReference extends Object {

  public DownReference() {}

  public static Object newInstance() {
    return new DownReference();
  }

  public String getSecret() {
    return "X123";
  }

}

