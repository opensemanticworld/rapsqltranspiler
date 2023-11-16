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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class W3CTest {
  // test parameter
  private static final String DB_URL = "jdbc:postgresql://localhost:5432/rapsql";
  private static final String USER = "postgres";
  private static final String PASS = "postgres";
  private static final String GRAPH_NAME = "junit-test";
  private static final String PATH_NAME = "src/test/resources/ttl-sparql";
  private static final String SRC_NAME = "rdf.ttl";

  // provide test resources of rdf model, rdf-cypher model, sparql queries
  private static List<Arguments> MethodProvider() throws IOException {
    return TestProvider.MethodArgProvider(GRAPH_NAME, PATH_NAME, SRC_NAME);
  }

  // create test graph in postgres
  @BeforeEach 
  public void age_create_graph() throws SQLException {
    TestProvider.age_create_graph(DB_URL, USER, PASS, GRAPH_NAME);
  }

  // drop test graph in postgres
  @AfterEach 
  public void age_drop_graph() throws SQLException {
    TestProvider.age_drop_graph(DB_URL, USER, PASS, GRAPH_NAME);
  }

  // parametrized equality tests of both sparql and transpiled cypher results
  @ParameterizedTest 
  @MethodSource("MethodProvider")
  public void run_test(Model rdf_model, String rdf_to_cypher, File query_file) throws IOException {

    // define result map of sparql and cypher results
    String sparql_query = "";
    String cypher_query = "";
    Set<Map<String, String>> sparql_res_map = new HashSet<Map<String, String>>();
    Set<Map<String, String>> cypher_res_map = new HashSet<Map<String, String>>();

    // import transformed rdf model to postgres age
    try {
      TestProvider.age_import_rdf(DB_URL, USER, PASS, rdf_model, rdf_to_cypher, query_file);
    } catch (SQLException e) {
      e.printStackTrace(); 
      fail("Error: RDF-Cypher-Mapping query in PostgreSQL AGE");
      return; 
    }

    // read, print, and execute sparql query
    try {
      sparql_query = new String(Files.readAllBytes(query_file.toPath()));
      Helper.display_info('-', 20, "SPARQL QUERY " + query_file.getName());
      System.out.println(sparql_query);
          // build-in sparql query execution of jena library
      Query query = QueryFactory.create(sparql_query);
      QueryExecution qe = QueryExecutionFactory.create(query, rdf_model);
      org.apache.jena.query.ResultSet results = qe.execSelect(); 
      // sparql result set mapping 
      while(results.hasNext()) {
        Map<String, String> sparql_res = new HashMap<String, String>();
        QuerySolution row = results.next();
        for(String col: results.getResultVars()) {
          // rm datatype for equality test of different triple store designs
          sparql_res.put(col, Helper.rm_dt(row.get(col).toString()));
        }
        sparql_res_map.add(sparql_res); // add result to sparql result list
      }
    } catch (IOException e) { e.printStackTrace(); }

    // transform, print and execute transpiled cypher query
    try {
      cypher_query = S2C.rapsql_s2c(GRAPH_NAME, sparql_query);
      Helper.display_info('-', 22, "CYPHER TRANSPILED QUERY");
      Helper.pretty_cypher(cypher_query);
      // connect to postgres to perform test on transformed cypher query
      try ( Connection conn = DriverManager.getConnection(DB_URL, USER, PASS) ) {
        Statement stmt = conn.createStatement(); 
        stmt.execute("LOAD 'age'");
        stmt.execute("SET search_path = ag_catalog, \"$user\", public;");             
        ResultSet rs = stmt.executeQuery(cypher_query); // sparql-to-cypher result here
        ResultSetMetaData rsmd = rs.getMetaData();
        int columnsNumber = rsmd.getColumnCount();
        // create map of single cypher statements 
        while (rs.next()) {
          Map<String, String> res = new HashMap<String, String>();
          for (int i = 1; i <= columnsNumber; i++) {
            res.put(rsmd.getColumnName(i), rs.getString(i).replace("\"", ""));
          }
          cypher_res_map.add(res);
        }
      } catch (SQLException e) { 
        fail("CYPHER FAILED WITH EXCEPTION\n"
          + e.getStackTrace()
          + "\nSPARQL QUERY:\n" + sparql_query
          + "\n\nCYPHER QUERY:\n" + cypher_query
        );
      }
    } catch (QueryException e) {
      System.err.println("S2C SKIPPED: " + e.getMessage()); 
      Assumptions.assumeTrue(false, e.getMessage());
      return;
    }

    /*    EQUALITY TEST    */
    // print sparql and cypher result maps for test run
    Helper.display_info('$', 28, "EQUALITY TEST RESULT");
    System.out.println("SPARQL: " + sparql_res_map);
    System.out.println("CYPHER: " + cypher_res_map);
    // testing equality of sparql and cypher result maps
    assertEquals(sparql_res_map, cypher_res_map, String.format(    
      "Equality test failed for %s\nSparql result:\n%s\n\nCypher Result\n%s", 
      query_file.getCanonicalPath(),
      sparql_res_map.toString(),
      cypher_res_map.toString()
    ));

    // System.out.println("-------- SPARQL RESULT --------\n" + sparql_res_map + "\n");
    // System.out.println("-------- CYPHER RESULT --------\n" + cypher_res_map + "\n");
  
  }

}