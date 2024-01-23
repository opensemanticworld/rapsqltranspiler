/* 
   Copyright 2023 Andreas Räder, https://github.com/raederan

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package de.rapsql.transpiler;
import org.apache.commons.lang3.StringUtils;

public class Helper {

  // remove datatype from sparql responses for different triple store design tests
  public static String rm_dt(String s) {
    return s.replaceAll("\\^\\^.*", "");
  }

  // just cosmetics for information about the test
  public static void display_info(Character c, Integer i, String s) {
    System.out.println( 
      "\n" + StringUtils.repeat(c, i) + " " 
      + s + " " + StringUtils.repeat(c, i)
    );
  }

  // pretty output of cypher query string
  public static void pretty_cypher(String cypher_str) {
    // Definition of keywords to be detected
    String[] keywords = {
      "OPTIONAL", "CREATE", "MERGE", "ORDER BY", "WHERE", "OFFSET",
      "WITH", "RETURN", "UNIQUE", "JOIN", "ON", "AS", "LIMIT", "UNION"
    };
    // Replace the keywords with new line before each keyword
    for (String keyword : keywords) {
      cypher_str = cypher_str.replaceAll("\\b" + keyword + "\\b", "\n" + keyword);
    }
    // Replace all "MATCH" with new line if "OPTIONAL" stands not before
    cypher_str = cypher_str.replaceAll("(?<!OPTIONAL) MATCH", "\nMATCH");

    System.out.println(cypher_str);
  }

  // calculate and display execution time
  public static void display_exec_time(String info, long time_before, long time_after) {
    long exec_time = time_after - time_before;
    display_info('#', 7, info + " EXEC TIME (ms)");
    System.out.println(exec_time);
    // TimeUnit.MILLISECONDS.toSeconds(exec_time);
  }

  // convert age cypher to neo4j cypher
  public static String age2N4j(String age_query, Boolean sparql) {
    if (!sparql) {
      int startIndex = age_query.indexOf("$$") + 2;
      int endIndex = age_query.lastIndexOf("$$");
      String n4j_query = age_query.substring(startIndex, endIndex);
      return n4j_query;
    } else {
      int start_index = age_query.indexOf("$$") + 2;
      int split_index = age_query.lastIndexOf(" $$)");
      int end_index = age_query.indexOf(";");
      String n4j_query1 = age_query.substring(start_index, split_index);
      String n4j_query2 = age_query.substring(split_index + 4, end_index);
      // set n4j_query2 alias to everything in between "(" and ")" using regex
      n4j_query2 = n4j_query2.replaceAll("\\((.*)\\)", "$1");

      String n4j_query = n4j_query1.concat(n4j_query2).replaceAll(" ag_catalog.agtype", "");
      // System.out.println("N4J Cypher:\n" + n4j_query + "\n");
      return n4j_query;
    }
  }

  // write String to file by given path and filename
  public static void str2file(String file_abspath, String content) {
    try {
      java.io.FileWriter fw = new java.io.FileWriter(file_abspath);
      fw.write(content);
      fw.close();
    } catch (Exception e) {
      System.out.println(e);
    }
  }

}
