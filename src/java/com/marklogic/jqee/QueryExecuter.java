package com.marklogic.jqee;

/*
 * Copyright 2004 Mark Logic Corporation. All Rights Reserved.
 */

import java.util.*;
import java.util.logging.*;
import java.io.*;
import com.marklogic.xdbc.*;
import com.marklogic.xdmp.XDMPConnection;
import org.jdom.*;
import org.jdom.input.*;

/**
 * @author Jason Hunter
 */
public class QueryExecuter {
  private static final int RETRIES = 10;
  private static final int RETRY_WAIT = 30000;  // ms

  private XDMPConnection con;
  private Logger log = Logger.getLogger(this.getClass().getName());

  public QueryExecuter(XDMPConnection con) {
    this.con = con;
  }

  public void setLogger(Logger log) {
    this.log = log;
  }

  private Object execute(Query query, ResultHandler handler) throws QueryException {
    int retriesLeft = RETRIES;
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
        sleep(RETRY_WAIT);
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
                // XXX Using nextReader() is broken due to bug 551
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

  public String executeString(Query query) throws QueryException {
    String[] answers = executeStrings(query);
    if (answers.length >= 1) {
      return answers[0];
    }
    else throw new QueryException("Got empty value: " + query);
  }

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

  private void sleep(int ms) {
    try { Thread.sleep(ms); } catch (InterruptedException e) { }
  }

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
