/*
 * Copyright 2004 Mark Logic Corporation. All Rights Reserved.
 */
package com.marklogic.jqee;

/**
 * @author Jason Hunter
 */
public class Query {

  public static final int STRING = 0;
  public static final int DOM = 1;
  public static final int JDOM = 2;

  private String query;
  private String module;
  private String moduleNamespace;
  private int nodesAs = STRING;

  public Query(String query) {
    this.query = query;
  }

  public Query(String query, String module, String moduleNamespace) {
    this.query = query;
    this.module = module;
    this.moduleNamespace = moduleNamespace;
  }

  public void setNodesReturnedAs(int type) {
    nodesAs = type;
  }

  public int getNodesReturnedAs() {
    return nodesAs;
  }

  public String toString() {
    if (module == null) {
      return query;
    }
    else {
      StringBuffer buf = new StringBuffer();
      buf.append("import module '");
      buf.append(moduleNamespace);
      buf.append("' at '");
      buf.append(module);
      buf.append("' ");
      buf.append(query);
      return buf.toString();
    }
  }
}
