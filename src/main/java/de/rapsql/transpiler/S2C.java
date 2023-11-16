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

import org.postgresql.pljava.annotation.Function;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryType;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.Algebra;

public class S2C {

  public static String get_sparql(String sparql_path) {
    String sparql_query = "";
    try {
      sparql_query = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(sparql_path)));
    } catch (java.io.IOException e) {
      System.out.println("Error: " + e);
    }
    return sparql_query;
  }

  @Function // auto generated endpoint via _5API.sql
  public static String rapsql_s2c(String graph_name, String sparql_query) throws QueryException {
    // compile sparql query string using jena algebra
    Query sq = QueryFactory.create(sparql_query);
    Op op = Algebra.compile(sq);
    // retrieve query type for transpiler support check
    Enum<QueryType> query_type = sq.queryType();
    // create visitor instance for compiler access
    SparqlAlgebra visitor = new SparqlAlgebra(graph_name, query_type.toString());
    // query transpilation
    op.visit(visitor);        
    // provide transpiled content                            
    return (visitor.getCypher());
  }
}
