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

import java.util.ArrayList;
import de.rapsql.rdf2pg.writers.RapsqlSplitWriter;
import de.rapsql.rdf2pg.maps.generic.GenericMapping;

public class R2C {

  // RDF2PG method integration to read serialized RDF direclty in RAPSQLTranspiler
  public static ArrayList<String> rdf_path(String rdf_path) {
    ArrayList<String> rdf = new ArrayList<String>();
    RapsqlSplitWriter instance_pgwriter = new RapsqlSplitWriter();
    RapsqlSplitWriter schema_pgwriter = new RapsqlSplitWriter();
    GenericMapping gdm = new GenericMapping();
    gdm.run(rdf_path, instance_pgwriter, schema_pgwriter);
    rdf = instance_pgwriter.getLines();
    return rdf;
  }

  // Converts RDF2PG output to AGE Cypher
  public static String r2c(String graph, ArrayList<String> rdf) {
    String cypher_stmt = "SELECT * FROM ag_catalog.cypher('" + graph + "', $$ ";
    // System.out.println("inRDF:\n" + rdf);
    // Iterate over all rdf statements
    for (String stmt : rdf) {
      cypher_stmt = cypher_stmt.concat(stmt + " ");
    }
    cypher_stmt = cypher_stmt.concat("$$) AS (cypher ag_catalog.agtype);");
    return cypher_stmt;
  }



  // // TODO: rdf_str interoperability for JSON-LD instead of file in rdf2pg
  // @Function // auto generated endpoint via _5API.sql
  // public static String rapsql_r2c(String graph_name, String lang_str, String rdf_str) throws QueryException {
  //   Model model = RDFParser
  //                   .create()
  //                   .fromString(rdf_str)
  //                   .lang(RDFLanguages.nameToLang(lang_str))
  //                   .toModel();
  //   // String cypher_str = r2c(graph_name, model);
  //   return cypher_str;
  // }
}

