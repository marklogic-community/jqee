import com.marklogic.jqee.*;
import com.marklogic.xdmp.*;
import com.marklogic.xdmp.util.*;
import java.io.*;
import java.net.Authenticator;

/**
 * Simple program to run XQueries
 * @author Raffaele Sena
 */
public class xqtor {

  /**
   * parse a string in the form of:
   *
   *  [username:password@]host[:port]
   */
  private static String[] parseHost(String hostParams) {
   
    String username = null;
    String password = "";
    String host     = "localhost";
    String port     = "8002";

    if (hostParams != null) {
//    System.out.println("parsing host: " + hostParams);
      host = hostParams;

      int pos = host.indexOf('@'); // user-params@host-params
      
      if (pos > 0) {
	username = host.substring(0, pos);
	host = host.substring(pos + 1);

	pos = username.indexOf(':'); // user:password

	if (pos > 0) {
	  password = username.substring(pos + 1);
	  username = username.substring(0, pos);
	}
      }

      pos = host.indexOf(':'); // host:port

      if (pos > 0) {
	  port = host.substring(pos + 1);
	  host = host.substring(0, pos);
      }
    }

    String hostInfo[] = { username, password, host, port };
    return hostInfo;
  }

  private static void runQuery(String query, QueryExecuter exec)
  {
    Query q = new Query(query);
    q.setNodesReturnedAs(Query.STRING);
    Object[] answer = exec.execute(q);
    if (answer.length > 0)
      System.out.println(answer[0]);
    else
      System.out.println("<xqtor:ok/>");
  }

  private static String readInput() throws Exception {

    LineNumberReader reader = 
      new LineNumberReader(new InputStreamReader(System.in));
    StringBuffer input =  new StringBuffer();
    String line;
    while ((line = reader.readLine()) != null) {
      input.append(line);
      input.append("\n");
    }

    return input.toString();
  }

  public static void main(String[] args) throws Exception {

    int argi = 0;

    String hostParams = null;
    if (args.length > 0 && args[0].startsWith("--host=")) {
      hostParams = args[0].substring(7);
      argi++;
    }

    String hostInfo[] = parseHost(hostParams);

    if (hostInfo[0] != null) {
      Authenticator.setDefault(
        new XDMPAuthenticator(hostInfo[0], hostInfo[1]));
    }

    QueryExecuter exec = new QueryExecuter(
      new XDMPConnection(hostInfo[2], Integer.parseInt(hostInfo[3])));

    exec.setRetries (0);

    if (argi < args.length) {
      for (; argi < args.length; argi++)
        runQuery(args[argi], exec);
    } else {
      String query = readInput();
      runQuery(query, exec);
    }
  }
}
