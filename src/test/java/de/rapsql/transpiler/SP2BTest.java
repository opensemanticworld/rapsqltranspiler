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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class SP2BTest {
  /////////////////////// TEST CONFIGURATION ///////////////////////
  // test configuration
  private static final Boolean IMPORT_RDF = false;
  private static final Boolean DROP_GRAPH = false;
  private static final Boolean CNT_RESULTS = false;
  private static final Boolean USE_TYPED_SPARQL = false;
  private static final Boolean DISABLE_SPARQL = false;
  private static final Boolean DISABLE_CYPHER = false;

  // db connection setup
  private static final String DB_URL = "jdbc:postgresql://localhost:5432/rapsql";
  private static final String USER = "postgres";
  private static final String PASS = "postgres";
  
  // test parameter setup
  // private static final String GRAPH_NAME = "tesla"; 
  private static final String GRAPH_NAME = "spcustomyars";
  // private static final String GRAPH_NAME = "spcustomrdfid2";
  private static final String PATH_NAME = "src/test/resources/sp2b";
  private static final String SRC_NAME = "rdf.n3";
  ////////////////////////////////////////////////////////////////////

  // provide test resources of rdf model, rdf-cypher model, and sparql queries
  private static List<Arguments> MethodProvider() throws IOException {
    return TestProvider.MethodArgProvider(GRAPH_NAME, PATH_NAME, SRC_NAME);
  }

  // create test graph in postgres before test run, if rdf import is enabled
  @BeforeAll
  public void load_data() throws SQLException {
    System.out.println("\n");
    Helper.display_info('§', 34, "START OF TESTS");
    if (IMPORT_RDF) {
      Arguments arg = null;
      TestProvider.age_create_graph(DB_URL, USER, PASS, GRAPH_NAME);
      try {
        List<Arguments> method_args = MethodProvider();
        arg = method_args.get(0);
      } catch (IOException e) { e.printStackTrace(); }
      Model rdf_model = (Model) arg.get()[0];
      String rdf_to_cypher = (String) arg.get()[1];
      File query_file = (File) arg.get()[2];
      TestProvider.age_import_rdf(DB_URL, USER, PASS, rdf_model, rdf_to_cypher, query_file);
    }
  }
  
  // drop test graph in postgres after test run if drop graph is enabled
  @AfterAll
  public void delete_data() throws SQLException {
    if (DROP_GRAPH) {
      TestProvider.age_drop_graph(DB_URL, USER, PASS, GRAPH_NAME);
    }
    Helper.display_info('§', 35, "END OF TESTS");
    System.out.println("\n");
  }

  // only info and cosmetic separators for log output
  @AfterEach
  public void print_line() {
    Helper.display_info('$', 29, "EQUALITY RESULT END");
  }

  // parametrized equality tests of both sparql and transpiled cypher results
  @ParameterizedTest 
  @MethodSource("MethodProvider")
  public void run_test(Model rdf_model, String rdf_to_cypher, File query_file) throws IOException {

    // define result map of sparql and cypher results
    String sparql_query = "";
    String cypher_query = "";
    long time_before = 0;
    long time_after = 0;
    List<Map<String, String>> sparql_res_list = new ArrayList<Map<String, String>>();
    List<Map<String, String>> cypher_res_list = new ArrayList<Map<String, String>>();

    // SPARQL: read, print, and execute, if sparql is not disabled
    try {
      sparql_query = new String(Files.readAllBytes(query_file.toPath()));
      if (!DISABLE_SPARQL) {
        Helper.display_info('-', 20, "SPARQL QUERY " + query_file.getName());
        System.out.println(sparql_query);
        // build-in sparql query execution of jena library
        time_before = System.currentTimeMillis();
        Query query = QueryFactory.create(sparql_query);
        String query_type = query.queryType().toString();
        // System.out.println("Query type: " + query_type);
        QueryExecution qe = QueryExecutionFactory.create(query, rdf_model);
        
        // support for ASK and SELECT sparql queries
        if (query_type.equals("ASK")) {
          boolean result = qe.execAsk();
          // System.out.println("Result: " + result);
          if (result) {
            sparql_res_list.add(new HashMap<String, String>() {{ put("exists", "t"); }});
          } else {
            sparql_res_list.add(new HashMap<String, String>() {{ put("exists", "f"); }});
          }
          time_after = System.currentTimeMillis();
        }
        if (query_type.equals("SELECT")) {
          org.apache.jena.query.ResultSet results = qe.execSelect(); 
          // sparql result set mapping 
          while(results.hasNext()) {
            Map<String, String> sparql_res = new HashMap<String, String>();
            QuerySolution row = results.next();
            for(String col: results.getResultVars()) {
              // System.out.println("\n\nDEBUG RESULT" + sparql_res + "\n");
              // create sparql result set and cover postgres null values with "null" string
              if (row.get(col) != null) {
                // rm datatype for equality test of different triple store designs
                if (!USE_TYPED_SPARQL) {
                  sparql_res.put(col, row.get(col).toString().split("\\^\\^")[0]);
                } else {
                  sparql_res.put(col, row.get(col).toString());
                }
              } 
              else sparql_res.put(col, "null");
            }
            // add result to sparql result list
            sparql_res_list.add(sparql_res); 
            time_after = System.currentTimeMillis();
          }
        }
      }
      Helper.display_exec_time("SPARQL", time_before, time_after);
    } catch (IOException e) { e.printStackTrace(); }

    // CYPHER: read, print, and execute, if cypher is not disabled
    if (!DISABLE_CYPHER) {
      // transform, print and execute transpiled cypher query
      try {
        cypher_query = S2C.rapsql_s2c(GRAPH_NAME, sparql_query);
        Helper.display_info('-', 22, "CYPHER TRANSPILED QUERY");
        Helper.pretty_cypher(cypher_query);
        // System.out.println(cypher_query);

        // connect to postgres to perform test on transformed cypher query
        try ( Connection conn = DriverManager.getConnection(DB_URL, USER, PASS) ) {
          Statement stmt = conn.createStatement(); 
          stmt.execute("LOAD 'age'");
          stmt.execute("SET search_path = ag_catalog, \"$user\", public;");             
          time_before = System.currentTimeMillis();
          // sparql-to-cypher result here
          ResultSet rs = stmt.executeQuery(cypher_query); 
          ResultSetMetaData rsmd = rs.getMetaData();
          int columnsNumber = rsmd.getColumnCount();
          // create map of single cypher statements 
          while (rs.next()) {
            Map<String, String> res = new HashMap<String, String>();
            for (int i = 1; i <= columnsNumber; i++) {
              if (rs.getString(i) != null) {
                res.put(rsmd.getColumnName(i), rs.getString(i).replace("\"", ""));
              } else res.put(rsmd.getColumnName(i), "null");
            }
            // cypher_res_map.add(res);
            cypher_res_list.add(res);
          }
          time_after = System.currentTimeMillis();
          Helper.display_exec_time("CYPHER", time_before, time_after);

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
    }

    /*    EQUALITY TEST    */
    if (!DISABLE_CYPHER && !DISABLE_SPARQL) {
      // sort result lists, only an applied ORDER BY clause will be checked, not the order of the results
      // caused by equal results, e.g, year(1) = 1950 | year(2) = 1950, the order of these results is not deterministic
      // TODO: Create test data set (no duplicates) for ORDER BY clause, e.g., data year1 = 1950, year2 = 1951, year3 = 1952
      sparql_res_list.sort((m1, m2) -> m1.toString().compareTo(m2.toString()));
      cypher_res_list.sort((m1, m2) -> m1.toString().compareTo(m2.toString()));

      // print sparql and cypher result maps for test run
      Helper.display_info('$', 28, "EQUALITY TEST RESULT");
      if (!CNT_RESULTS) {
        System.out.println("-------- SPARQL RESULT --------\n" + sparql_res_list + "\n");
        System.out.println("-------- CYPHER RESULT --------\n" + cypher_res_list + "\n");      
      } else {
        System.out.println("-------- SPARQL RESULT --------\n" + "cnt=" + sparql_res_list.size() + "\n");
        System.out.println("-------- CYPHER RESULT --------\n" + "cnt=" + cypher_res_list.size() + "\n");
      }
      // testing equality of sparql and cypher result maps
      assertEquals(sparql_res_list, cypher_res_list, String.format(    
        "Equality test failed for %s\nSparql result:\n%s\n\nCypher Result\n%s", 
        query_file.getCanonicalPath(),
        sparql_res_list.toString(),
        cypher_res_list.toString()
      ));
    } else {
      // just print sparql and cypher output if any of them is disabled (response or cnt)
      if (!CNT_RESULTS) {
        // standard output of sparql and cypher result maps
        System.out.println("-------- SPARQL RESULT --------\n" + sparql_res_list + "\n");
        System.out.println("-------- CYPHER RESULT --------\n" + cypher_res_list + "\n");      
      } else {
        // output of sparql and cypher result maps as count of results
        System.out.println("-------- SPARQL RESULT --------\n" + "cnt=" + sparql_res_list.size() + "\n");
        System.out.println("-------- CYPHER RESULT --------\n" + "cnt=" + cypher_res_list.size() + "\n");
      }
    }
  }
}
