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

import com.marklogic.jqee.*;
import java.util.logging.*;
import com.marklogic.xdmp.XDMPConnection;

/**
 * Simple test program for Query and QueryExecuter.
 */
public class QueryTest {

  public static void main(String[] args) throws Exception {
    XDMPConnection con = new XDMPConnection("localhost", 8004);
    QueryExecuter exec = new QueryExecuter(con);

    // Set up logging across all levels to the console
    Logger log = Logger.getLogger("QueryTest");
    log.setUseParentHandlers(false);
    log.setLevel(Level.FINEST);
    ConsoleHandler console = new ConsoleHandler();
    console.setLevel(Level.FINEST);
    log.addHandler(console);
    exec.setLogger(log);

    Query q1 = new Query("1+1");
    int answer1 = exec.executeInt(q1);
    System.out.println(q1 + ": " + answer1);

    Query q2 = new Query("1+1 = 2");
    boolean answer2 = exec.executeBoolean(q2);
    System.out.println(q2 + ": " + answer2);

    Query q3 = new Query("<foo><bar/></foo>,<quux/>");
    Object[] answer3 = exec.execute(q3);
    System.out.println(q3 + ": " + answer3[0]);

    Query q4 = new Query("<foo><bar/></foo>,<quux/>");
    q4.setNodesReturnedAs(Query.STRING);
    Object[] answer4 = exec.execute(q4);
    System.out.println(q4 + ": " + answer4[0]);

    Query q5 = new Query("<foo><bar/></foo>,<quux/>");
    q5.setNodesReturnedAs(Query.JDOM);
    Object[] answer5 = exec.execute(q5);
    System.out.println(q5 + ": " + answer5[0]);

    Query q6 = new Query("<foo><bar/></foo>,<quux/>");
    q6.setNodesReturnedAs(Query.DOM);
    Object[] answer6 = exec.execute(q6);
    System.out.println(q6 + ": " + answer6[0]);

    Query q7 = new Query("xs:decimal(7*7)");
    Object[] answer7 = exec.execute(q7);
    System.out.println(q7 + ": " + answer7[0].getClass().getName());

    Query q8 = new Query("xs:decimal(7*7)");
    int answer8 = exec.executeInt(q8);
    System.out.println(q8 + ": " + answer8);

    Query q9 = new Query("(for $i in input() return base-uri($i))[1 to 10]");
    String[] answer9 = exec.executeStrings(q9);
    System.out.println(q9 + ": " + answer9.length + " strings");

    Query q10 = new Query("prettyprint('1234567', true())",
         "format-conversions.xqy", "http://www.w3.org/2003/05/xpath-functions");
    String answer10 = exec.executeString(q10);
    System.out.println(q10 + ": " + answer10);;

    Query q11 = new Query("<a/>,<b/>,<c/>,<d/>");
    Object[] answer11 = exec.execute(q11);
    System.out.println(q11 + ": " + answer11.length);
  }
}
