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

import java.util.*;
import java.util.logging.*;
import java.io.*;
import com.marklogic.xdbc.*;
import com.marklogic.xdmp.XDMPConnection;
import org.jdom.*;
import org.jdom.input.*;

/**
 * Class to execute Query instances and return appropriate results.
 * Includes logic to perform logging and query retrying (in case the server
 * is rebooted during the execution for example).
 * @author Jason Hunter, Mark Logic Corporation
 */
public class QueryExecuter {
  private static final int RETRIES = 10;
  private static final int RETRY_WAIT = 30000;  // ms

  private int retries = RETRIES;
  private long retryWait = RETRY_WAIT;

  private XDMPConnection con;
  private Logger log = Logger.getLogger(this.getClass().getName());

  /**
   * Constructs a new instance to perform queries against the supplied
   * connection instance.
   * @param con The connection against which to execute queries.
   */
  public QueryExecuter(XDMPConnection con) {
    this.con = con;
  }

  /**
   * Sets the number of times to retry before giving up.
   * @param retries Number of retries
   */
  public void setRetries (int retries) {
    this.retries = retries;
  }

  /**
   * Set the amount of time to wait between retries.
   # @param retryWait Time to wait, in milliseconds
   */
  public void setRetryWait (long retryWait) {
    this.retryWait = retryWait;
  }

  /**
   * Assigns a java.util.logging.Logger instance to receive log messages.
   * By default log messages to go the com.marklogic.jqee.QueryExecuter logger.
   * @param log The new logger to use.
   */
  public void setLogger(Logger log) {
    this.log = log;
  }

  private Object execute(Query query, ResultHandler handler) throws QueryException {
    int retriesLeft = retries;

    if (retries <= 0) {
      retriesLeft = 1;
    }

    while (retriesLeft > 0) {
      XDBCStatement stmt = null;
      XDBCResultSequence result = null;
      try {
        stmt = con.createStatement();
        log.fine("Executing: " + query);
        result = stmt.executeQuery(query.toString());
        return handler.handle(result);
      }
      // XDBCException means retry; QueryException means don't bother
      catch (XDBCException e) {
        retriesLeft--;
        log.log(Level.WARNING, "XDBC problem: Retries left: " +
                               retriesLeft + ": " + e, e);

        if (retriesLeft <= 0) {
          throw new QueryException ("Giving up on query (" + e + "): " + query);
        }

        sleep (retryWait);
      }
      finally {
        if (stmt != null) {
          try {
            stmt.close();
          }
          catch (Exception e) {
            log.log(Level.WARNING, "Couldn't close XDBC statement: " + e, e);
          }
        }
        if (result != null) {
          try {
            result.close();
          }
          catch (Exception e) {
            log.log(Level.WARNING, "Couldn't close XDBC result", e);
          }
        }
      }
    }
    throw new QueryException("Giving up on query: " + query);
  }

  /**
   * Executes the given query and returns an Object array storing the
   * completely processed results.
   * @param query Query instance to execute
   * @return Object array storing the completely processed results
   * @throws QueryException if there's an unrecoverable problem
   */
  public Object[] execute(final Query query) throws QueryException {
    ResultHandler handler = new ResultHandler() {
      Object handle(XDBCResultSequence result) throws XDBCException {
        ArrayList answers = new ArrayList();
        while (result.hasNext()) {
          result.next();
          switch (result.getItemType()) {
            case XDBCResultSequence.XDBC_Boolean:
              answers.add(result.getBoolean().asBoolean());
              break;
            case XDBCResultSequence.XDBC_Date:
            case XDBCResultSequence.XDBC_DateTime:
            case XDBCResultSequence.XDBC_Time:
              answers.add(result.getDate().asDate());
              break;
            case XDBCResultSequence.XDBC_Double:
            case XDBCResultSequence.XDBC_Float:
              answers.add(result.getDouble().asDouble());
              break;
            case XDBCResultSequence.XDBC_Decimal:
              //answers.add(result.getDecimal().asBigDecimal());
              //break;
            case XDBCResultSequence.XDBC_Integer:
              answers.add(result.getInteger().asInteger());
              break;
            case XDBCResultSequence.XDBC_Node:
              if (query.getNodesReturnedAs() == Query.STRING) {
                answers.add(result.getNode().asString());
              }
              else if (query.getNodesReturnedAs() == Query.DOM) {
                answers.add(result.getNode().asNode());
              }
              else if (query.getNodesReturnedAs() == Query.JDOM) {
                // XXX See bug 551 regarding the use of nextReader()
                //BufferedReader reader = result.nextReader();
                StringReader reader = new StringReader(result.getNode().asString());
                SAXBuilder builder = new SAXBuilder();
                Document doc = null;
                try {
                  doc = builder.build(reader);
                }
                catch (JDOMException e) {
                  // Don't bother retrying
                  throw new QueryException("Problem during JDOM build: " + query);
                }
                catch (IOException e) {
                  // Do bother retrying
                  throw new XDBCException("IO problem during JDOM build: " + query);
                }
                answers.add(doc);
              }
              break;
            case XDBCResultSequence.XDBC_String:
              answers.add(result.get_String());
              break;
            default:
              throw new QueryException("Got unexpected type: " + result.getItemType());
          }
        }
        return answers.toArray();
      }
    };
    return (Object[]) execute(query, handler);
  }

  /**
   * Executes the given query and returns a String array storing the
   * completely processed results.
   * @param query The Query instance to execute
   * @return String array storing the completely processed results
   * @throws QueryException if there's an unrecoverable problem including the
   * case where the result sequence contains a value not mappable to a String
   */
  public String[] executeStrings(Query query) throws QueryException {
    ResultHandler handler = new ResultHandler() {
      Object handle(XDBCResultSequence result) throws XDBCException {
        ArrayList answers = new ArrayList();
        while (result.hasNext()) {
          result.next();
          switch (result.getItemType()) {
            case XDBCResultSequence.XDBC_Node:
              answers.add(result.getNode().asString());
              break;
            case XDBCResultSequence.XDBC_String:
              answers.add(result.get_String());
              break;
            default:
              throw new QueryException("Got non-string type: " + result.getItemType());
          }
        }
        return answers.toArray(new String[0]);
      }
    };
    return (String[]) execute(query, handler);
  }

  /**
   * Executes the given query and returns a single String storing the
   * first returned item as a String.
   *
   * @param query The Query instance to execute
   * @return String storing the first returned item as a String
   * @throws QueryException if there's an unrecoverable problem including the
   * case where the result sequence contains an initial value not mappable to
   * a String
   */
  public String executeString(Query query) throws QueryException {
    String[] answers = executeStrings(query);
    if (answers.length >= 1) {
      return answers[0];
    }
    else throw new QueryException("Got empty value: " + query);
  }

  /**
   * Executes the given query and returns a single int derived from the
   * first returned item.
   *
   * @param query The Query instance to execute
   * @return int storing the first returned item
   * @throws QueryException if there's an unrecoverable problem including the
   * case where the result sequence contains an initial value not of int type
   */
  public int executeInt(Query query) throws QueryException {
    Object[] answers = execute(query);
    if (answers.length >= 1) {
      Object first = answers[0];
      if (first instanceof Integer) {
        return ((Integer)first).intValue();
      }
      else
        throw new QueryException("Got non-int type: " + first);
    }
    else
      throw new QueryException("Got empty answer: " + query);
  }

  /**
   * Executes the given query and returns a single boolean derived from the
   * first returned item.
   *
   * @param query The Query instance to execute
   * @return boolean storing the first returned item
   * @throws QueryException if there's an unrecoverable problem including the
   * case where the result sequence contains an initial value not of boolean
   * type
   */
  public boolean executeBoolean(Query query) throws QueryException {
    Object[] answers = execute(query);
    if (answers.length >= 1) {
      Object first = answers[0];
      if (first instanceof Boolean) {
        return ((Boolean) first).booleanValue();
      }
      else
        throw new QueryException("Got non-boolean type: " + first);
    }
    else
      throw new QueryException("Got empty answer: " + query);
  }

  private void sleep(long ms) {
    try { Thread.sleep(ms); } catch (InterruptedException e) { }
  }

  /**
   * Closed the underlying connection.
   *
   * @throws QueryException if closing the underlying connection threw an
   * exception
   */
  public void close() throws QueryException {
    try {
      con.close();
    }
    catch (XDBCException e) {
      throw new QueryException(e.getMessage());
    }
  }

  abstract class ResultHandler {
    abstract Object handle(XDBCResultSequence result) throws XDBCException;
  }
}
