package com.marklogic.jqee;

// Copyright (c)2004 Mark Logic Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// The use of the Apache License does not indicate that this project is
// affiliated with the Apache Software Foundation. 

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
