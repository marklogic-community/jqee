package com.marklogic.jqee;

/*
 * Copyright 2004 Mark Logic Corporation. All Rights Reserved.
 */

/**
 * @author Jason Hunter
 */
public class QueryException extends RuntimeException {

  public QueryException() { }

  public QueryException(String msg) { super(msg); }
}
