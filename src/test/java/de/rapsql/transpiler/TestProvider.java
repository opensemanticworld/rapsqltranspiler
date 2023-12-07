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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.params.provider.Arguments;
import de.rapsql.rdf2pg.maps.generic.GenericMapping;
// import de.rapsql.rdf2pg.writers.RapsqlSplitWriter;
import de.rapsql.rdf2pg.writers.RapsqlSplitWriterMerge;
// import de.rapsql.rdf2pg.writers.RapsqlWriter2;

public class TestProvider {

  // provide test resources of rdf model, rdf-cypher model, sparql queries
  public static List<Arguments> MethodArgProvider(String graph, String test_path, String src_name) throws IOException {
    // provide all folder structured ttl and sparql files
    Helper.display_info('%', 32, "METHOD PROVIDER");
    File f = new File(test_path);
    FileFilter directoryFilter = new FileFilter() {
      public boolean accept(File file) { return file.isDirectory();	}
    };
    File[] files = f.listFiles(directoryFilter);
    // sort model files
    Arrays.sort(files); 
    List<Arguments> method_args = new LinkedList<Arguments>();
    for (File folder_ : files) {
      Helper.display_info('-', 7, "TEST RESOURCES");
      String folder = folder_.getCanonicalPath();
      Path rdf_path = Paths.get(folder, src_name);
            
      // create RDF to Cypher model from ttl
      // RapsqlWriter2 instance_pgwriter = new RapsqlWriter2(); // no edge partitioning
      // RapsqlSplitWriter instance_pgwriter = new RapsqlSplitWriter(); // edge partitioning
      RapsqlSplitWriterMerge instance_pgwriter = new RapsqlSplitWriterMerge(); // edge partitioning merge
      GenericMapping gdm = new GenericMapping();
      Model rdf_model = gdm.runModelMapping4(rdf_path.toString(), instance_pgwriter);
      String rdf_to_cypher = R2C.r2c(graph, instance_pgwriter.getLines());  
      System.out.println(
        rdf_path.getParent().getFileName() + "/" + rdf_path.getFileName()
      );
      // Helper.display_info('-', 7, "RDF MODEL");
      // System.out.println(rdf_model);
      
      // iterate over all sparql files in all query folders
      FileFilter sparqlFilter = new FileFilter() {
        public boolean accept(File file) {
          String extension = "";
          int i = file.getName().lastIndexOf('.');
          if (i > 0) { extension = file.getName().substring(i+1); }
          return file.isFile() && (extension.equals("sparql"));
        }
      };
      File[] query_files = Paths.get(folder, "queries").toFile().listFiles(sparqlFilter);
      Arrays.sort(query_files); // sort .sparql query files
      for(File query_file: query_files) {
        System.out.println(
          query_file.getParentFile().getParentFile().getName() + "/"
          + query_file.getParentFile().getName() + "/"
          + query_file.getName()
        );
        method_args.add(Arguments.of(rdf_model, rdf_to_cypher, query_file));
      }
    }
    Helper.display_info('%', 30, "END METHOD PROVIDER");
    return method_args;
  }

  // create test graph in postgres
  public static void age_create_graph(String db_url, String user, String pass, String graph) throws SQLException {
    Helper.display_info('%', 35, "PARAMETERIZED TEST");
    try ( Connection conn = DriverManager.getConnection(db_url, user, pass) ) {
      Statement stmt = conn.createStatement(); 
      stmt.execute("LOAD 'age';");
      stmt.execute("SET search_path = ag_catalog, \"$user\", public;");
      stmt.execute("SELECT * FROM ag_catalog.create_graph('" + graph  + "');");
      Helper.display_info('-', 7, "SUCCESS CREATE GRAPH " + graph);
    } catch (SQLException e) { e.printStackTrace(); }
  }

  // drop test graph in postgres
  public static void age_drop_graph(String db_url, String user, String pass, String graph) throws SQLException {
    try ( Connection conn = DriverManager.getConnection(db_url, user, pass) ) {
      Statement stmt = conn.createStatement(); 
      stmt.execute("LOAD 'age'");
      stmt.execute("SET search_path = ag_catalog, \"$user\", public;");
      stmt.execute("SELECT * FROM drop_graph('" + graph  + "', true);");
      Helper.display_info('-', 7, "SUCCESS DELETE GRAPH " + graph);
    } catch (SQLException e) { e.printStackTrace(); }    
  }



  // import transformed rdf model to postgres age
  public static void age_import_rdf(
      String db_url, String user, String pass, 
      Model rdf_model, String rdf_to_cypher, File query_file
    ) throws SQLException {
      try ( Connection conn = DriverManager.getConnection(db_url, user, pass) ) {
      Statement stmt = conn.createStatement(); 
      stmt.execute("LOAD 'age';");
      stmt.execute("SET search_path = ag_catalog, \"$user\", public;");
      stmt.execute(rdf_to_cypher); 
      // Helper.display_info('-', 7, "RDF MODEL");
      // System.out.println(rdf_model);
      Helper.display_info('-', 7, 
        "SUCCESS RDF IMPORT " 
        + query_file.getParentFile().getParentFile().getName()
      );
    } catch (SQLException e) { e.printStackTrace(); }
  }

}