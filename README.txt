JQEE presents a high-level interface on top of the lower-level XDBC classes.
While not suitable for every job, the high-level interface makes common tasks
-- such as executing a query and retrieving a sequence of strings -- into two
liners.  Within JQEE you'll find a Query class representing queries and a
QueryExecuter that runs them with its various execute() methods. For example:


XDMPConnection con = new XDMPConnection("localhost", 8004);
QueryExecuter exec = new QueryExecuter(con);

Query q1 = new Query("1+1");
int answer1 = exec.executeInt(q1);    // the value 2

Query q2 = new Query("<foo><bar/></foo>,<quux/>");
q2.setNodesReturnedAs(Query.JDOM);
Object[] answer5 = exec.execute(q2);  // two JDOM nodes

