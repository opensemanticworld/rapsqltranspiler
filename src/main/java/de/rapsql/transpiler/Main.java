/* 
   Copyright 2023 Andreas Raeder, https://github.com/raederan

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

// import java.io.IOException;
// import java.nio.file.DirectoryStream;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.nio.file.Paths;
// import java.text.Collator;
// import java.util.Arrays;
// import java.util.Collections;
// import java.util.Comparator;
// import java.util.LinkedHashSet;
// import java.util.List;
// import java.util.Locale;

public class Main 
{
  public static void main( String[] args )
  {
    try {

      ///////////// Pretty Cypher output for RAPSQLBench /////////////
      if (args.length > 0) {
        if (args.length != 2) {
          System.err.println("Usage: java -jar rapsqltranspiler.jar <graph_name> <rdf_path>");
          System.exit(1);
        } else {
          String graph_name = args[0];
          String sparql_path = args[1];
          // String sparql_path = "/usr/local/docker/masterthesis/rapsql/submodules/rapsqltranspiler/src/test/resources/notest/sp2b/queries/a1-q1.sparql";
          // String graph_name = "sp100";
        
          String cypher_q_stmt1 = S2C.rapsql_s2c(graph_name, S2C.get_sparql(sparql_path));
          Helper.pretty_cypher(cypher_q_stmt1);
        }
      }

      //////////////////////////// S2C TESTS ////////////////////////////
      // // // Test 1: S2C
      // String sparql_path1 = "/usr/local/docker/masterthesis/rapsql/submodules/rapsqltranspiler/src/test/resources/ttl-sparql/w3c_test1/queries/query1.sparql";
      // String graph1 = "w3ct1";
      // String cypher1_stmt1 = S2C.rapsql_s2c(graph1, S2C.get_sparql(sparql_path1));
      // System.out.println("Graph: " + graph1 + ", Cypher:\n" + cypher1_stmt1 + "\n");

      // // // Test 2: S2C
      // String sparql_path2 = "/usr/local/docker/masterthesis/rapsql/submodules/rapsqltranspiler/src/test/resources/ttl-sparql/w3c_test2/queries/query1.sparql";
      // String graph2 = "w3ct2";
      // String cypher2_stmt1 = S2C.rapsql_s2c(graph2, S2C.get_sparql(sparql_path2));
      // System.out.println("Graph: " + graph2 + ", Cypher:\n" + cypher2_stmt1 + "\n");

      // // // Test 3: S2C
      // String sparql1_path3 = "/usr/local/docker/masterthesis/rapsql/submodules/rapsqltranspiler/src/test/resources/ttl-sparql/w3c_test3/queries/query1.sparql";
      // String sparql2_path3 = "/usr/local/docker/masterthesis/rapsql/submodules/rapsqltranspiler/src/test/resources/ttl-sparql/w3c_test3/queries/query2.sparql";
      // String sparql3_path3 = "/usr/local/docker/masterthesis/rapsql/submodules/rapsqltranspiler/src/test/resources/ttl-sparql/w3c_test3/queries/query3.sparql";
      // String sparql4_path3 = "/usr/local/docker/masterthesis/rapsql/submodules/rapsqltranspiler/src/test/resources/ttl-sparql/w3c_test3/queries/query4.sparql";
      // String graph3 = "w3ct3";
      // String cypher3_stmt1 = S2C.rapsql_s2c(graph3, S2C.get_sparql(sparql1_path3));
      // String cypher3_stmt2 = S2C.rapsql_s2c(graph3, S2C.get_sparql(sparql2_path3));
      // String cypher3_stmt3 = S2C.rapsql_s2c(graph3, S2C.get_sparql(sparql3_path3));
      // String cypher3_stmt4 = S2C.rapsql_s2c(graph3, S2C.get_sparql(sparql4_path3));
      // System.out.println("Graph: " + graph3 + ", Cypher3_1:\n" + cypher3_stmt1 + "\n");
      // System.out.println("Graph: " + graph3 + ", Cypher3_2:\n" + cypher3_stmt2 + "\n");
      // System.out.println("Graph: " + graph3 + ", Cypher3_3:\n" + cypher3_stmt3 + "\n");
      // System.out.println("Graph: " + graph3 + ", Cypher3_4:\n" + cypher3_stmt4 + "\n");

      // // // Test 4: S2C
      // String sparql_path4 = "/usr/local/docker/masterthesis/rapsql/submodules/rapsqltranspiler/src/test/resources/ttl-sparql/w3c_test4/queries/query1.sparql";
      // String graph4 = "w3ct4";
      // String cypher4_stmt1 = S2C.rapsql_s2c(graph4, S2C.get_sparql(sparql_path4));
      // System.out.println("Graph: " + graph4 + ", Cypher:\n" + cypher4_stmt1 + "\n");

      // // // Test 5: S2C
      // String sparql1_path5 = "/usr/local/docker/masterthesis/rapsql/submodules/rapsqltranspiler/src/test/resources/ttl-sparql/w3c_test5/queries/query1.sparql";
      // String sparql2_path5 = "/usr/local/docker/masterthesis/rapsql/submodules/rapsqltranspiler/src/test/resources/ttl-sparql/w3c_test5/queries/query2.sparql";
      // String sparql3_path5 = "/usr/local/docker/masterthesis/rapsql/submodules/rapsqltranspiler/src/test/resources/ttl-sparql/w3c_test5/queries/query3.sparql";
      // String sparql4_path5 = "/usr/local/docker/masterthesis/rapsql/submodules/rapsqltranspiler/src/test/resources/ttl-sparql/w3c_test5/queries/query4.sparql";
      // String graph5 = "w3ct5";
      // String cypher5_stmt1 = S2C.rapsql_s2c(graph5, S2C.get_sparql(sparql1_path5));
      // String cypher5_stmt2 = S2C.rapsql_s2c(graph5, S2C.get_sparql(sparql2_path5));
      // String cypher5_stmt3 = S2C.rapsql_s2c(graph5, S2C.get_sparql(sparql3_path5));
      // String cypher5_stmt4 = S2C.rapsql_s2c(graph5, S2C.get_sparql(sparql4_path5));
      // System.out.println("Graph: " + graph5 + ", Cypher5_1:\n" + cypher5_stmt1 + "\n");
      // System.out.println("Graph: " + graph5 + ", Cypher5_2:\n" + cypher5_stmt2 + "\n");
      // System.out.println("Graph: " + graph5 + ", Cypher5_3:\n" + cypher5_stmt3 + "\n");
      // System.out.println("Graph: " + graph5 + ", Cypher5_4:\n" + cypher5_stmt4 + "\n");



      //////////////////////////// SP2B TESTS ////////////////////////////
      // // Upload sp100
      // String rdf_path1 = "/usr/local/docker/masterthesis/rapsql/submodules/rapsqltranspiler/src/test/resources/sp2b/sp100/rdf.n3";
      // String graph1 = "sp100";
      // String cypher_stmt1 = R2C.r2c(graph1, R2C.rdf_path(rdf_path1));
      // System.out.println("Graph: " + graph1 + ", Cypher:\n" + cypher_stmt1+ "\n");
      


      // // Test: S2C

      // // String query = "a1-q1";
      // String query = "b2-q2";
      // // String query = "c3-q3a";
      // // String query = "d4-q3b";
      // // String query = "e5-q3c";
      // // String query = "f6-q4";
      // // String query = "g7-q5a";
      // // String query = "h8-q5b";
      // // String query = "i9-q6";    // NOT IMPLEMENTED YET
      // // String query = "j10-q7";   // NOT IMPLEMENTED YET
      // // String query = "k11-q8";
      // // String query = "l12-q9";
      // // String query = "m13-q10";
      // // String query = "n14-q11";
      // // String query = "o15-q12a";
      // // String query = "p16-q12b";
      // // String query = "q17-q12c";
      // // String graph_spcustom = "sp100000";
      // String graph_spcustom = "spcustompart";
      // // String graph_spcustom = "spcustomrdfid";
      // // String graph_spcustom = "sp1m";
      // // String graph_spcustom = "sp100k";
      // // String sparql_path = "/usr/local/docker/masterthesis/rapsql/submodules/rapsqltranspiler/src/test/resources/ttl-sparql/w3c_test5/queries/";
      // String sparql_path = "/usr/local/docker/masterthesis/rapsql/submodules/rapsqltranspiler/src/test/resources/sp2b/spcustom/queries/";
      // sparql_path = sparql_path.concat(query + ".sparql");
      // String cypher_q_stmt1 = S2C.rapsql_s2c(graph_spcustom, S2C.get_sparql(sparql_path));
      // Helper.pretty_cypher(cypher_q_stmt1);
      // // Helper.pretty_cypher(cypher_q_stmt1.replace("*", "count(*)"));
      // // System.out.println("\nGraph: " + graph_spcustom + ", Cypher:\n" + cypher_q_stmt1 + "\n");







      //////////////////////////// LOAD n4j RDF IN THIS VERSION UNSUPPORTED ////////////////////////////

      // String rdf_path = "/usr/local/docker/masterthesis/rapsql/submodules/rapsqltranspiler/src/test/resources/sp2b/spcustom/rdf.n3";
      // String graph = "spcustom";
      // String age_cypher = R2C.r2c(graph, R2C.rdf_path(rdf_path));
      // String n4j_cypher = Helper.age2N4j(age_cypher, false);
      // Helper.pretty_cypher(n4j_cypher);

      // // write n4j cypher to file
      // String file_abspath = "/usr/local/docker/masterthesis/rapsql/submodules/rapsqltranspiler/src/test/resources/str2file/n4j-spcustom-rdf.cyp";
      // Helper.str2file(file_abspath, n4j_cypher);







      //////////////////////////// R2C TESTS ////////////////////////////
      // Test 1: R2C
      // String rdf_path1 = "/usr/local/docker/masterthesis/rapsql/submodules/rapsqltranspiler/src/test/resources/ttl-sparql/w3c_test1/rdf.ttl";
      // String graph1 = "w3ct1";
      // String cypher_stmt1 = R2C.r2c(graph1, R2C.rdf_path(rdf_path1));
      // System.out.println("Graph: " + graph1 + ", Cypher:\n" + cypher_stmt1+ "\n");


      // // Test 2: R2C
      // String rdf_path2 = "/usr/local/docker/masterthesis/rapsql/submodules/rapsqltranspiler/src/test/resources/ttl-sparql/w3c_test2/rdf.ttl";
      // String graph2 = "w3ct2";
      // String cypher_stmt2 = R2C.r2c(graph2, R2C.rdf_path(rdf_path2));
      // System.out.println("Graph: "  + "Graph: " + graph2 + ", Cypher:\n" + cypher_stmt2+ "\n");

      // // Test 3: R2C
      // String rdf_path3 = "/usr/local/docker/masterthesis/rapsql/submodules/rapsqltranspiler/src/test/resources/ttl-sparql/w3c_test3/rdf.ttl";
      // String graph3 = "w3ct3";
      // String cypher_stmt3 = R2C.r2c(graph3, R2C.rdf_path(rdf_path3));
      // System.out.println("Graph: " + graph3 + ", Cypher:\n" + cypher_stmt3+ "\n");

      // // Test 4: R2C
      // String rdf_path4 = "/usr/local/docker/masterthesis/rapsql/submodules/rapsqltranspiler/src/test/resources/ttl-sparql/w3c_test4/rdf.ttl";
      // String graph4 = "w3ct4";
      // String cypher_stmt4 = R2C.r2c(graph4, R2C.rdf_path(rdf_path4));
      // System.out.println("Graph: " + graph4 + ", Cypher:\n" + cypher_stmt4+ "\n");

      // // Test 5: R2C
      // String rdf_path5 = "/usr/local/docker/masterthesis/rapsql/submodules/rapsqltranspiler/src/test/resources/ttl-sparql/w3c_test5/rdf.ttl";
      // String graph5 = "w3ct5";
      // String cypher_stmt5 = R2C.r2c(graph5, R2C.rdf_path(rdf_path5));
      // System.out.println("Graph: " + graph5 + ", Cypher:\n" + cypher_stmt5+ "\n");
      //////////////////////////////////////////////////////////////////////






      // String sparql = RdfGraph.get_sparql("/usr/local/docker/masterthesis/rapsql/submodules/rapsqltranspiler/src/test/resources/ttl-sparql/w3c_test1/queries/query1.sparql");
      // // System.out.println(sparql);
      // String cypher = S2C.rapsql_s2c("sp1", sparql);
      // System.out.println("Cypher:\n" + cypher);
    } catch (Exception e) {
      System.err.println("MAIN ERROR: " + e);
    }
  }

}
