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

// in general, visitor patterns need to define all methods of an interface by design
// therefore, the all the imports are necessary to use this visitor operator 
// even if some methods are not yet supported or not required
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.NodeVisitor;
import org.apache.jena.graph.Node_ANY;
import org.apache.jena.graph.Node_Blank;
import org.apache.jena.graph.Node_Graph;
import org.apache.jena.graph.Node_Literal;
import org.apache.jena.graph.Node_Triple;
import org.apache.jena.graph.Node_URI;
import org.apache.jena.graph.Node_Variable;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.sparql.algebra.OpVisitor;
import org.apache.jena.sparql.algebra.op.OpAssign;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpConditional;
import org.apache.jena.sparql.algebra.op.OpDatasetNames;
import org.apache.jena.sparql.algebra.op.OpDiff;
import org.apache.jena.sparql.algebra.op.OpDisjunction;
import org.apache.jena.sparql.algebra.op.OpDistinct;
import org.apache.jena.sparql.algebra.op.OpExtend;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.algebra.op.OpGraph;
import org.apache.jena.sparql.algebra.op.OpGroup;
import org.apache.jena.sparql.algebra.op.OpJoin;
import org.apache.jena.sparql.algebra.op.OpLabel;
import org.apache.jena.sparql.algebra.op.OpLateral;
import org.apache.jena.sparql.algebra.op.OpLeftJoin;
import org.apache.jena.sparql.algebra.op.OpList;
import org.apache.jena.sparql.algebra.op.OpMinus;
import org.apache.jena.sparql.algebra.op.OpNull;
import org.apache.jena.sparql.algebra.op.OpOrder;
import org.apache.jena.sparql.algebra.op.OpPath;
import org.apache.jena.sparql.algebra.op.OpProcedure;
import org.apache.jena.sparql.algebra.op.OpProject;
import org.apache.jena.sparql.algebra.op.OpPropFunc;
import org.apache.jena.sparql.algebra.op.OpQuad;
import org.apache.jena.sparql.algebra.op.OpQuadBlock;
import org.apache.jena.sparql.algebra.op.OpQuadPattern;
import org.apache.jena.sparql.algebra.op.OpReduced;
import org.apache.jena.sparql.algebra.op.OpSequence;
import org.apache.jena.sparql.algebra.op.OpService;
import org.apache.jena.sparql.algebra.op.OpSlice;
import org.apache.jena.sparql.algebra.op.OpTable;
import org.apache.jena.sparql.algebra.op.OpTopN;
import org.apache.jena.sparql.algebra.op.OpTriple;
import org.apache.jena.sparql.algebra.op.OpUnion;
import org.apache.jena.sparql.core.Var;



public class SparqlAlgebra implements OpVisitor {
  ////////////// Configuration //////////////
  // enable/disable coalesce (gdm dbmodel: true:=yars, false:=rdfid)
  private boolean use_coalesce = true;
  // enable/disable cypher path optimization (cpo)
  private boolean use_cpo = true; 
  // cpo1: left to left
  private boolean l2l_cpo = true;
  // cpo2: left to right 
  private boolean l2r_cpo = false;
  // cpo3: l2l + l2r
  ///////////////////////////////////////////


  ///////////////////////// variables for mappings and supported clause types /////////////////////////
  // (: HINT: use search function and enter variable names in order to quickly find all correlations ;)

  // basic declarations for cypher query
  private String cypher;
  private Map<Var, String> Sparql_to_cypher_variable_map;
  private Map<String, Object> Cypher_to_sparql_variable_map;
  private int blank_node_num = 0;
  private Map<Node_Blank, Var> Sparql_blank_node_to_var_map;
  private String graph_name;
  private String return_clause = "";

  // parameterized support for edge partitioned graphs
  private boolean partitioned = true;

  // support for SPARQL query types (see supportedQueryTypes() for more information)
  private String query_type;

  // support detection of query conversion success and error messages
  private boolean isQueryConversionSuccesful = true;
  private String conversionErrors = "";
  
  // support for Cypher UNION 
  private Boolean has_union_clause = false;
  private String cached_match_clause = ""; 
  private boolean left_bgp_join = false;
  private boolean right_bgp_join = false;

  // support for Cypher ORDER BY 
  private Boolean has_with_clause = false;

  // support for distinct clause
  private boolean distinct = false;

  ///////////////////////////// IMPORTANT HINTS FOR ASK QUERY SUPPORT /////////////////////////////
  // support for special ASK queries without variables, works for all sp2b test cases yet
  // notice: this is experimental and uses the latest variable from the query by default,
  //         it also creates a queryable path, if SPARQL has no variable at all (e.g. SP2B: q12c)
  // TODO: empirical analytics for evidence
  //
  private Var latest_var = null;
  //
  /////////////////////////////////////////////////////////////////////////////////////////////////

  // support for cypher variable path optimization (cpo)
  private List<Pair<Boolean, String>> 
            subject_pairlist = new ArrayList<Pair<Boolean, String>>();
  private List<Pair<Boolean, ArrayList<String>>> 
            predicate_pairlist = new ArrayList<Pair<Boolean, ArrayList<String>>>();
  private List<Pair<Boolean, String>> 
            object_pairlist = new ArrayList<Pair<Boolean, String>>();

  // initialize instance
  public SparqlAlgebra(String _graph_name, String _query_type) {
    this.query_type = _query_type;
    this.graph_name = _graph_name;
    cypher = new String();
    Sparql_blank_node_to_var_map = new HashMap<Node_Blank, Var>();
    Sparql_to_cypher_variable_map = new HashMap<Var, String>();
    Cypher_to_sparql_variable_map = new HashMap<String, Object>();
    if (!supportedQueryTypes(query_type)) {
      this.isQueryConversionSuccesful = false;
      this.conversionErrors += "\nUnsupported SPARQL Query type: " + query_type;
    }
  }

  // IMPORTANT HINT: THIS IS WHERe APACHE AGE SYNTAX OF CYPHER HEAD IS GENERATED
  // ! easy access to change for neo4j support at this point !
  // query type support and cypher query entrypoint 
  public Boolean supportedQueryTypes(String query_type) {
  // CASES: UNKNOWN, SELECT, CONSTRUCT, ASK, DESCRIBE, CONSTRUCT_JSON
  // developer information: https://svn.apache.org/repos/asf/jena/site/trunk/content/documentation/javadoc/arq/org/apache/jena/query/Query.html
    switch (query_type) {
      case "SELECT":
        cypher = cypher + "SELECT * FROM ag_catalog.cypher('" + graph_name + "', $$ ";
        return true;
      case "ASK":
        cypher = cypher + "SELECT EXISTS (SELECT * FROM ag_catalog.cypher('" + graph_name + "', $$ ";
        return true;
      default:
        return false;
    }
  }

  ///////////////////////// START IMPLEMENTATION LOGIC FUNCTIONS FOR VISITOR PATTERN ////////////////////////////////
  //
  // helper function concatCypher for readability of code
  public void concatCypher(String _cypher) {
    cypher = cypher.concat(_cypher);
  }
  
  // create or get variable for blank node
  protected String create_or_get_variable(Node_Blank it) {
    String var_name = "blankvar" + (blank_node_num++);
    Var variable = Var.alloc(var_name + it.getBlankNodeId().toString());
    Sparql_blank_node_to_var_map.put(it, variable);
    String created_var = create_or_get_variable(variable);
    return created_var;
  }

  // create or get variable for variable itself
  protected String create_or_get_variable(Var allocated_var) {
    if (Sparql_to_cypher_variable_map.containsKey(allocated_var)) {
      return Sparql_to_cypher_variable_map.get(allocated_var);
    } 
    else {
      Sparql_to_cypher_variable_map.put(allocated_var, allocated_var.getName());
      Cypher_to_sparql_variable_map.put(allocated_var.getName(), allocated_var);
      return Sparql_to_cypher_variable_map.get(allocated_var);
    }
  }

  // get property key by schema
  // coalesce clause for resource, blank node and literal (!schema dependency)
  // otherwise use rdfid
  // !parameterized for complete visitor algorithm
  public String getSchemaStmt(String var_name, Boolean seperate) {
    String stmt = "";
    if (!use_coalesce) {
      // RDF2PG schema rdfid
      stmt = stmt.concat(var_name + ".rdfid ");
    } else {
      // RDF2PG schema yars
      stmt = stmt.concat(
        "coalesce("
        + var_name + ".iri, "     // Resource
        + var_name + ".bnid, "    // BlankNode
        + var_name + ".value)"    // Literal
      );
    }
    // support of multiple return variables
    if(seperate) stmt = stmt.concat(", ");
    // else stmt = stmt.concat(" ");
    return stmt;
  }

  // get input cypher query, split into two parts, the split grammer indicator is pipe `|`
  // the left part will not be changed, the right part will be changed
  // all MATCH statements in the right part will be changed to OPTIONAL MATCH
  public String getOptionalClause(String _cypher) {
    // define grammer for split using pipe `|`
    String[] cypher_parts = _cypher.split("\\| ", 2);
    // define left and right part
    String cypher_stmt = cypher_parts[0];
    String cypher_stmt_opt = cypher_parts[1];
    // replace all MATCH statements in the 2nd part to OPTIONAL MATCH, 
    // only if OPTIONAL MATCH is not already present (TODO: !multiple OPTIONAL MATCH to be tested)
    cypher_stmt_opt = cypher_stmt_opt.replaceAll("(?<!OPTIONAL) MATCH", ",");
    // cypher_stmt_opt = cypher_stmt_opt.replaceAll("MATCH", ",");
    return cypher_stmt + cypher_stmt_opt;
  }


  // return clause for matching select variables ( ! Cpyher set ! SPARQL get! )
  public String setgetReturnClause(List<Var> vars, Boolean has_with_clause) {
    // build return clause
    String return_clause = "RETURN ";
    // get all variables of SPARQL select statement to set Cypher return clause
    for(Var var: vars) {
      if (has_with_clause) {
        // var only, cause ORDER BY is present including WITH clause
        return_clause = return_clause.concat(Sparql_to_cypher_variable_map.get(var) + ", ");
      } else {
        // coalesce returns first non-null value from list, no OpOrder including WITH clause present
        return_clause = return_clause.concat(getSchemaStmt(Sparql_to_cypher_variable_map.get(var), true));
        // TODO: check if type cast is necessary anymore
      }
    }
    // remove last comma and space, then return clause
    return_clause = return_clause.substring(0, return_clause.length() - 2);
    return return_clause;
  }


  // build MATCH clause
  public void buildMatchClause(String triple_path) {
    String match = "MATCH ";

    // cover OpJoin case for all query types
    switch (query_type) {
      case "SELECT":
        if (!left_bgp_join) {
          // no OpJoin
          cypher = cypher + match + triple_path;
        } else {
          // OpJoin
          cached_match_clause = cached_match_clause + match + triple_path;
        }
        break;
      case "ASK":
        // cover ASK queries that either have a variable or not
        if (!left_bgp_join && latest_var != null) {
          // no OpJoin and variable exists 
          cypher = cypher + "MATCH " + triple_path;
        } else if (!left_bgp_join && latest_var == null) {
          // no OpJoin and no variable exists, use path variable "ask"
          cypher = cypher + "MATCH ask = " + triple_path;
        }
        else if (left_bgp_join && latest_var != null) {
          // OpJoin and variable exists
          cached_match_clause = cached_match_clause + "MATCH " + triple_path;
        } else {
          // OpJoin and no variable exists, use path variable "ask"
          cached_match_clause = cached_match_clause + "MATCH ask = " + triple_path;
        }
        break;
      default:
        break;
    }
  }

  // build end of cypher query
  public void buildEndOfAgeQuery(List<Var> vars) {
      concatCypher(" $$) AS (");
      for(Var var: vars) {
        concatCypher(Sparql_to_cypher_variable_map.get(var) + " ag_catalog.agtype, ");
      }
      cypher = cypher.substring(0, cypher.length() - 2);
      concatCypher("); ");
  }

  // build end of ASK cypher query
  public void buildEndOfAskQuery() {
    Var var = null;
    // ask query either has a variable or not
    if (!Sparql_to_cypher_variable_map.isEmpty()) {
      // get the latest variable
      var = Var.alloc(latest_var);
    } else {
      // create var of ask path 
      var = Var.alloc("ask");
      create_or_get_variable(var);
    }
    // concat specific return clause for ASK queries
    List<Var> ask = List.of(var);
    return_clause = setgetReturnClause(ask, has_with_clause);
    concatCypher(return_clause);
    concatCypher(" LIMIT 1");
    buildEndOfAgeQuery(ask);
    cypher = cypher.substring(0, cypher.length() - 2);
    concatCypher("); ");
  }
  //
  ///////////////////////// END IMPLEMENTATION LOGIC FUNCTIONS FOR VISITOR PATTERN ////////////////////////////////


  /////////////////////////////// !!! CPO EXPERIMENTAL BELOW !!! /////////////////////////////////////////////////
  //
  // TODO: empirical analytics for evidence
  //
  // DESCRIPTION: 
  //  - see: https://neo4j.com/docs/cypher-manual/current/patterns/reference/#graph-patterns-rules-relationship-uniqueness
  //  - left and right belong to subject and object of a bgp's
  //  - overarching cypher variable path optimization rule:     ! ! ! uniqueness of relationship ! ! !
  //  - these algorithms compare vars of all objects or subjects or a combiation of both for possible combinations
  //    to morph multiple MATCH clauses into paths for possible performance boosts
  //  - all implemented functions are called recursively to ensure full coverage of all possible combinations
  //  - only if the uniqueness of relationships is given, the path optimization will be applied
  //  - cpo follows a bunch of helper functions to ensure uniqueness of relationship
  //
  //    definition:
  //    - cpo1: left to left (l2l) path optimization
  //    - cpo2: left to right (l2r) path optimization
  //    - cpo3: left to left then left to right path optimization (in this order)
  //
  // HINTS: 
  //  - code is not cleaned up yet, cause still experimental
  //  - code is inside of SparqlAlgebra.java to ensure full access to all variables and methods
  //  - as soon as the code is stable, it will be moved to a separate class
  //////////////////////////////////////////////////////////////////////////////////////////
  //
  /////////////////////////// START HELPER FUNCTIONS OF CPO ALGORITHMS /////////////////////////////
  // predicate rules
  // no path optimization if both predicates are unique
  public Boolean checkPredicateUniqueness(int left, int right) {
    if (predicate_pairlist.get(left).getRight().equals(predicate_pairlist.get(right).getRight())) {
      return false;
    } else {
      return true;
    }
  }
  // no path optimization if one of the predicates is a variable
  public Boolean checkPredicateVars(int left, int right) {
    if (predicate_pairlist.get(left).getLeft() || predicate_pairlist.get(right).getLeft()) {
      return true;
    } else {
      return false;
    }
  }


  // l2l rules
  // check if both subject variables are true
  public Boolean checkSubjectVars(int left, int right) {
    if (subject_pairlist.get(left).getLeft() && subject_pairlist.get(right).getLeft()) {
      return true;
    } else {
      return false;
    }
  }
  // check if one of the object variables is true
  public Boolean checkOneOfObjectVar(int left, int right) {
    if (object_pairlist.get(left).getLeft() || object_pairlist.get(right).getLeft()) {
      return true;
    } else {
      return false;
    }
  }


  // r2r rules
  // check if both variables are true
  public Boolean checkObjectVars(int left, int right) {
    if (object_pairlist.get(left).getLeft() && object_pairlist.get(right).getLeft()) {
      return true;
    } else {
      return false;
    }
  }
  // check if one of the subject variables is true
  public Boolean checkOneOfSubjectVar(int left, int right) {
    if (subject_pairlist.get(left).getLeft() || subject_pairlist.get(right).getLeft()) {
      return true;
    } else {
      return false;
    }
  }


  // helper function to swap direction of all predicate path elements
  public ArrayList<String> reversePath(ArrayList<String> predicate_path) {
    ArrayList<String> predicate_path_reverse = new ArrayList<String>();
    for (int i = predicate_path.size() - 1; i >= 0; i--) {
      if (predicate_path.get(i).equals("-[")) {
        predicate_path_reverse.add("]-");
      } else if (predicate_path.get(i).equals("]-")) {
        predicate_path_reverse.add("-[");
      } else if (predicate_path.get(i).equals("]->")) {
        predicate_path_reverse.add("<-[");
      } else if (predicate_path.get(i).equals("<-[")) {
        predicate_path_reverse.add("]->");
      } else {
        predicate_path_reverse.add(predicate_path.get(i));
      }
    }
    return predicate_path_reverse;
  }

  // helper function to remove all applied path optimizations by int lists
  public void rmAppliedPathOptimization(List<Integer> rm_list) {
    // set all applied path optimizations 
    // for (int integer : set_list) {
    //   subject_pairlist.set(integer, Pair.of(true, subject_pairlist.get(integer).getRight()));
    //   predicate_pairlist.set(integer, Pair.of(true, predicate_pairlist.get(integer).getRight()));
    //   object_pairlist.set(integer, Pair.of(true, object_pairlist.get(integer).getRight()));
    // }
    // // sort index list from highest to lowest
    rm_list.sort((o1, o2) -> o2.compareTo(o1));
    // System.out.println("Sorted remove index list: " + rm_list.toString());
    // remove all applied path optimizations by each element from index list from highest to lowest
    for (int integer : rm_list) {
      subject_pairlist.remove(integer);
      predicate_pairlist.remove(integer);
      object_pairlist.remove(integer);
    }
  }
  //
  /////////////////////////// END HELPER FUNCTIONS OF CPO ALGORITHMS //////////////////////////////
  //
  //
  //
  //////////////////////// START CYPHER PATH OPTIMIZATION (CPO) ALGORITHMS ////////////////////////
  //
  /////////////////////////////// START R2R CPO ////////////////////////////////////////////
  /////////// R2R NOT YET APPLIED CAUSED BY MISSING TEST CASES ///////////
  // right to right path optimization (same as left to left, object instead of subject)
  public void r2rCypherPath() {
    Map<Integer, String> right_var_matches = new HashMap<Integer, String>();
    Boolean r2r_match = false;
    // print all pairlists
    for (int i = 0; i < object_pairlist.size(); i++) {
      for (int j = i + 1; j < object_pairlist.size(); j++) {
        // object of second will be merged with object of first
        if (
            object_pairlist.get(i).getRight().equals(object_pairlist.get(j).getRight()) 
            && checkObjectVars(i, j) && checkOneOfSubjectVar(i, j)
            && !checkPredicateVars(i, j) && checkPredicateUniqueness(i, j)
          ) {
          System.out.println("i: " + i + " j: " + j);
          r2r_match = true;
          right_var_matches.put(i, object_pairlist.get(i).getRight());
          right_var_matches.put(j, object_pairlist.get(j).getRight());
          // set left var subject to object of second match (i < j)
          object_pairlist.set(i, Pair.of(true, subject_pairlist.get(j).getRight()));
          // get reverse predicate path
          ArrayList<String> second_predicate_path_rev = new ArrayList<String>();
          second_predicate_path_rev = reversePath(predicate_pairlist.get(j).getRight());
          // add matching subject var_name to second predicate path
          second_predicate_path_rev.add("("+ object_pairlist.get(j).getRight() +")");
          // add first predicate path to second predicate path
          second_predicate_path_rev.addAll(predicate_pairlist.get(i).getRight());
          // System.out.println("Second predicate path: " + second_predicate_path_rev.toString());
          // set second predicate path to first predicate path
          predicate_pairlist.set(
            i, 
            Pair.of(predicate_pairlist.get(i).getLeft() 
            && predicate_pairlist.get(j).getLeft(), second_predicate_path_rev)
          );
          // delete second subject, predicate and object pairlist
          subject_pairlist.remove(j);
          predicate_pairlist.remove(j);
          object_pairlist.remove(j);
        }
        if (r2r_match) break;
      }
      if (r2r_match) break;
    }
    if (r2r_match) {
      // recursive call if found possible optimization
      r2rCypherPath();
      // // check l2r path optimization after r2r path optimization
      // l2rCypherPath();
    }
  }
  //
  /////////////////////////////// END R2R CPO //////////////////////////////////////////////
  //
  //
  /////////////////////////////// START L2L CPO ////////////////////////////////////////////
  //
  // left to left path optimization
  public void l2lCypherPath() {
    // left == object, right == subject
    // Map<Integer, String> left_var_matches = new HashMap<Integer, String>();
    Boolean l2l_match = false;
    List<Integer> rm_list = new ArrayList<Integer>();

    // create copy of all pairlists
    List<Pair<Boolean, String>> local_subject_pairlist = new ArrayList<Pair<Boolean, String>>(subject_pairlist);
    List<Pair<Boolean, ArrayList<String>>> local_predicate_pairlist 
                                          = new ArrayList<Pair<Boolean, ArrayList<String>>>(predicate_pairlist);
    // List<Pair<Boolean, String>> local_object_pairlist = new ArrayList<Pair<Boolean, String>>(object_pairlist);
    
    // print subject_pairlist
    // System.out.println("Subject pairlist: " + subject_pairlist.toString());
    // get all duplicates in subject_pairlist
    for (int i = 0; i < subject_pairlist.size(); i++) {
      for (int j = i + 1; j < subject_pairlist.size(); j++) {
        // subject of second will be merged with subject of first
        if (
            subject_pairlist.get(i).getRight().equals(subject_pairlist.get(j).getRight()) 
            && checkSubjectVars(j, i) && checkOneOfObjectVar(i, j)
            && !checkPredicateVars(i, j) && checkPredicateUniqueness(i, j)
          ) {
          // System.out.println("predicate unique: " + checkPredicateUniqueness(i, j));
          // System.out.println("i: " + i + " j: " + j);
          // System.out.println("L2L Path Optimization:");
          // System.out.println("Subject " + i + " var_name match: " + subject_pairlist.get(i).getRight());
          // System.out.println("Subject " + j + " var_name match: " + subject_pairlist.get(j).getRight());

          l2l_match = true;
          // left_var_matches.put(i, subject_pairlist.get(i).getRight());
          // left_var_matches.put(j, subject_pairlist.get(j).getRight());
          // set left var subject to object of second match (i < j)
          local_subject_pairlist.set(i, Pair.of(true, object_pairlist.get(j).getRight()));
          // get reverse predicate path
          ArrayList<String> second_predicate_path_rev = new ArrayList<String>();
          second_predicate_path_rev = reversePath(predicate_pairlist.get(j).getRight());
          // add matching subject var_name to second predicate path
          second_predicate_path_rev.add("("+ subject_pairlist.get(j).getRight() +")");
          // add first predicate path to second predicate path
          second_predicate_path_rev.addAll(predicate_pairlist.get(i).getRight());
          // System.out.println("Second predicate path: " + second_predicate_path_rev.toString());
          // set second predicate path to first predicate path
          local_predicate_pairlist.set(i, Pair.of(predicate_pairlist.get(i).getLeft() && predicate_pairlist.get(j).getLeft(), second_predicate_path_rev));
          // delete second subject, predicate and object pairlist
          // subject_pairlist.remove(j);
          // predicate_pairlist.remove(j);
          // object_pairlist.remove(j);
          rm_list.add(j);
          // System.out.println("Removals: " + rm_list.toString());
        }
        // if (l2l_match) break;
      }
      // if (l2l_match) break;
    }
    
    //////// FOR DEBUGGING ////////
    // System.out.println("RM List: " + rm_list.toString());
    // if (rm_list.stream().distinct().count() == rm_list.size()) {
    //   System.out.println("All elements in rm_list are unique");
    //   idx_uniqueness = true;
    // } else {
    //   System.out.println("All elements in rm_list are not unique");
    //   idx_uniqueness = false;
    // }
    ///////////////////////////////

    if (l2l_match) {
      // check if all elements in rm_list are unique
      if (rm_list.stream().distinct().count() == rm_list.size()) {
        // System.out.println("All elements in rm_list are unique");
        // set local pairlists to global pairlists
        subject_pairlist = local_subject_pairlist;
        predicate_pairlist = local_predicate_pairlist;
        // remove all applied path optimizations by int list
        rmAppliedPathOptimization(rm_list);
        // recursive call if found possible optimization
        l2lCypherPath();
        // System.out.println("-------- CPO L2L APPLIED --------");
      }
    } 
    // // check l2r path optimization after l2l path optimization
    // REMINDER: RECURSIVE CALL of l2r inside of l2l to be tested, still experimental 
    // l2rCypherPath();
  }
  //
  /////////////////////////////// END L2L CPO ////////////////////////////////////////////
  //
  //
  /////////////////////////////// START L2R CPO ////////////////////////////////////////////
  // left to right path optimization
  public void l2rCypherPath() {
    // left == object, right == subject
    Boolean l2r_match = false;
    // print all pairlists
    
    // if subject_pairlist left is true and object_pairlist left is true, then compare all var_names from both lists and save matching ones with their index
    for (int i = 0; i < subject_pairlist.size(); i++) {
      // for every var_name in subject_pairlist compare with every var_name in object_pairlist
      for (int j = 0; j < subject_pairlist.size(); j++) {
      // for from highest index to lowest index to ensure no out of bounds
      // for (int j = subject_pairlist.size() - 1; j >= 0; j--) {
        if (
            subject_pairlist.get(i).getRight().equals(object_pairlist.get(j).getRight()) 
            && !checkPredicateVars(i, j) //&& !checkPredicateUniqueness(i, j)
        ) {
          // System.out.println("L2R Path Optimization:");
          // System.out.println("Subject var_name matches: " + subject_pairlist.get(i).getRight());
          // System.out.println("Object var_name matches: " + object_pairlist.get(j).getRight());

          l2r_match = true;
          // System.out.println("Matching var_name: " + subject_pairlist.get(i).getRight() + " with index: " + i + " and " + object_pairlist.get(j).getRight() + " with index: " + j);
          String left_var = subject_pairlist.get(j).getRight(); 
          ArrayList<String> predicate_path = new ArrayList<String>();
          // build new predicate path
          predicate_path.addAll(predicate_pairlist.get(j).getRight());
          predicate_path.add("("+ object_pairlist.get(j).getRight() +")");
          predicate_path.addAll(predicate_pairlist.get(i).getRight());  
          String right_var = object_pairlist.get(i).getRight();

          // get the smaller and bigger index of i and j to ensure out of bounds 
          int i_min = Math.min(i, j);
          int i_max = Math.max(i, j);

          // replace all pairlists on index of right match and remove left match
          subject_pairlist.set(i_min, Pair.of(subject_pairlist.get(j).getLeft(), left_var));
          predicate_pairlist.set(i_min, Pair.of(predicate_pairlist.get(j).getLeft() && predicate_pairlist.get(i).getLeft(), predicate_path));
          object_pairlist.set(i_min, Pair.of(object_pairlist.get(i).getLeft(), right_var));     
          subject_pairlist.remove(i_max);
          predicate_pairlist.remove(i_max);
          object_pairlist.remove(i_max);
        }
        // break inner loop if found possible optimization
        if (l2r_match) break;
      }
      // break outer loop if found possible optimization
      if (l2r_match) break;
    }
    // recursive call if found possible optimization
    if (l2r_match) {
      l2rCypherPath(); 
      // System.out.println("-------- CPO L2R APPLIED --------");
    }
  }
  //
  /////////////////////////////// END L2R CPO ////////////////////////////////////////////
  //
  //////////////////////// END CYPHER PATH OPTIMIZATION (CPO) ALGORITHMS ////////////////////////
  //
  /////////////////////////////// !!! CPO EXPERIMENTAL ABOVE !!! /////////////////////////////////////////////////



  ///////////////////// START SUPPORTED VISITOR PATTERN OF SPARQL ALGEBRA FOR QUERY INTEROPERABILITY /////////////////////
  //
  // MATCH
  @Override 
  public void visit(OpBGP opBGP) {
    //! ONLY FOR DEBUG VISITOR ! COMMENT OUT FOR STACK INTEGRATION !
    // System.out.println("\nIn opBGP\n" + opBGP.toString()+"\n");
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    // iterate over all triples in BGP
    java.util.Iterator<Triple> it = opBGP.getPattern().iterator();
    while(it.hasNext()) {
      Triple t = it.next();
      // CreateCypher visitor = new CreateCypher(); // BACKUP

      /////////////// BELOW IS NESTED VISITOR OF BGP AND REFERS TO SEMANTIC DATA- AND QUERY-INTEROPERABILTY //////
      //
      // nested visitor pattern for nodes inside BGP (!jena graph)
      NodeVisitor cypherNodeMatcher = new NodeVisitor() {

        // blank node visitor
        @Override
        public String visitBlank(Node_Blank it, BlankNodeId id) {
          // System.out.println("DEBUG BLANK NODE: " + it.toString());
          return create_or_get_variable(it);
        }

        // literal visitor
        @Override
        public String visitLiteral(Node_Literal it, LiteralLabel lit) {
          // current applied semantic interoperability at this point
          return String.format(
            use_coalesce  
              ? "{value:\'%s\', type:\'%s\'}" 
              : "{rdfid:\'%s\', type:\'%s\'}",
            lit.getLexicalForm(),
            lit.getDatatypeURI() 
          );

          ////////////////////////////////////////////////////////////////////////////////////////////
          // BACKUP VERSION OF SINGLE PROPERTY LITERAL -> MUST BE APPLIED AT THIS POINT
          // SEMANTIC INTEROPERABILITY: depends on graph schema: {rdfid:'value^^type'}
          // 
          // return String.format(
          //   lit.getDatatypeURI() == org.apache.jena.datatypes.xsd.XSDDatatype.XSDstring.getURI() ?
          //     "{rdfid:\'%s\'}" : "{rdfid:\'%s^^%s\'}",
          //   lit.getLexicalForm(),
          //   lit.getDatatypeURI()
          // );
          //
          ////////////////////////////////////////////////////////////////////////////////////////////
          //
          ////////////////////////////////////////////////////////////////////////////////////////////
          // TODO: LANGTAG SUPPORT -> MUST BE APPLIED AT THIS POINT
          // $Language tag is not supported yet in rdf2pg
          // $issue: possible (part) solution to add language tag support
          // System.out.println("DEBUG LITERAL: " + it.toString());
          // return 
          //   (lit.language().equals("")) ?
          //     (String.format("{type:\'%s\', value:\'%s\'}",
          //         lit.getDatatypeURI(), lit.getLexicalForm()))
          //   : 
          //     (String.format("{type:\'%s\', value:\'%s\', langtag:\'%s\'}",
          //         lit.getDatatypeURI(), lit.getLexicalForm(), lit.language()));
          ////////////////////////////////////////////////////////////////////////////////////////////
        }

        // uri visitor
        @Override
        public String visitURI(Node_URI it, String uri) {
          // System.out.println("DEBUG URI: " + it.toString());
          return String.format(
            use_coalesce  
              ? "{iri:\'%s\'}" 
              : "{rdfid:\'%s\'}", 
            uri
          );
        }

        // variable visitor
        @Override
        public String visitVariable(Node_Variable it, String name) {
          // System.out.println("DEBUG VARIABLE: " + it.toString());
          return create_or_get_variable(Var.alloc(it));
        }

        // triple visitor (is only for experimental purposes of jena)
        // developer notice: https://svn.apache.org/repos/asf/jena/site/trunk/content/documentation/javadoc/arq/org/apache/jena/sparql/algebra/op/OpTriple.html
        @Override
        public Object visitTriple(Node_Triple it, Triple triple) {
          System.out.println("DEBUG TRIPLE: " + it.toString() + triple.toString());
          return null;
        }

        // graph visitor currently unused
        @Override
        public Object visitGraph(Node_Graph it, Graph graph) { 
          System.out.println("DEBUG GRAPH: " + it.toString() + graph.toString());
          return null;
        }
        
        // any visitor currently unused	
        @Override
        public String visitAny(Node_ANY it) {
          System.out.println("DEBUG ANY: " + it.toString());
          return null;
        } 
      };
      //
      /////////////// ABOVE IS NESTED VISITOR OF BGP AND REFERS TO SEMANTIC DATA- AND QUERY-INTEROPERABILTY //////
      //
      // ! IMPORTANT BRIDGE !
      //
      ///////////// ALL ABOVE AND BELOW IS INSIDE OF BGP TRIPLE ITERATOR TO CREATE MATCH STATEMENTS //////////////
      /////////////      THIS IS AN IMPORTANT BRIDGE BETWEEN RDF2PG-RAPSQL AND RAPSQLTRANSPILER     //////////////
      //
      // ! IMPORTANT BRIDGE !
      //
      /////// BELOW CONTAINS BUSINESS LOGIC OF BGP AND REFERS TO SEMANTIC DATA- AND QUERY-INTEROPERABILTY  ///////
      // get subject, predicate and object of triple
      //
      String s = t.getMatchSubject().visitWith(cypherNodeMatcher).toString();
      String p = t.getMatchPredicate().visitWith(cypherNodeMatcher).toString();
      String o = t.getMatchObject().visitWith(cypherNodeMatcher).toString();

      // get predicate name if it is uri (!partition dependency)
      if (t.getMatchPredicate().isURI() && partitioned) {
        p = ":" + t.getMatchPredicate().getLocalName() + " " + p;
      }
      
      // list of predicates for cpo with different patterns 
      ArrayList<String> p_list = new ArrayList<String>();
      p_list.add("-[");
      p_list.add(p);
      p_list.add("]->");

      // set the latest variable for ask return clause if variable exists
      if (t.getMatchSubject().isVariable()) {
        latest_var = (Var) t.getMatchSubject();
        subject_pairlist.add(Pair.of(true, s));
      } else {
        subject_pairlist.add(Pair.of(false, s));
      }
      if (t.getMatchPredicate().isVariable()) {
        latest_var = (Var) t.getMatchPredicate();
        predicate_pairlist.add(Pair.of(true, p_list));
      } else {
        predicate_pairlist.add(Pair.of(false, p_list));
      }
      if (t.getMatchObject().isVariable()) {
        latest_var = (Var) t.getMatchObject();
        object_pairlist.add(Pair.of(true, o));
      } else {
        object_pairlist.add(Pair.of(false, o));
      }
      
      // build MATCH clause
      if (!use_cpo) {
        buildMatchClause("(" + s + ")-[" + p + "]->(" + o + ") ");
      }
    }
    /////////////// START CONFIGURATION OPTIONS CPO ///////////////
    //
    // use all pairlists for path optimization
    if (use_cpo) {

      // left to left path optimization (l2l cpo)
      if (l2l_cpo) l2lCypherPath();

      // left to right path optimization (l2r cpo)
      if (l2r_cpo) l2rCypherPath();

      // right to right path optimization (no sp2b query case)
      // TODO: test to same as l2l logic when test cases are available
      // r2rCypherPath();


      // build MATCH clause for each list element
      for (int i = 0; i < subject_pairlist.size(); i++) {
        String triple_path = "";
        triple_path = triple_path.concat("("+ subject_pairlist.get(i).getRight() +")");
        for (String predicate: predicate_pairlist.get(i).getRight()) {
          triple_path = triple_path.concat(predicate);
        }
        triple_path = triple_path.concat("("+ object_pairlist.get(i).getRight() +") ");

        // build MATCH clause
        buildMatchClause(triple_path);
      }

      // clear pairlists
      subject_pairlist.clear();
      predicate_pairlist.clear();
      object_pairlist.clear();
    }
    //
    //////////////// END CONFIGURATION OPTIONS CPO ////////////////
  }
  //
  /////// ABOVE CONTAINS BUSINESS LOGIC OF BGP AND REFERS TO SEMANTIC DATA- AND QUERY-INTEROPERABILTY  ///////


  // WHERE
  @Override  
  public void visit(OpFilter opFilter) {
    //! ONLY FOR DEBUG VISITOR ! COMMENT OUT FOR STACK INTEGRATION !
    // System.out.println("\nIn opFilter\n" + opFilter.toString());
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    // visit sub operation
    opFilter.getSubOp().visit(this);  
    // parse filter expressions and create cypher WHERE clause    
    FilterParser filter_parser = new FilterParser(use_coalesce);
    try {
      concatCypher(filter_parser.getWhereClause(opFilter.getExprs()));
    } catch (QueryException e) { 
      this.isQueryConversionSuccesful = false;
      e.printStackTrace();
      this.conversionErrors += "Unsupported Algebra type OpFilter in FilterParser\n";
    }
  }


  // RETURN
  @Override 
  public void visit(OpProject opProject) {
    //! ONLY FOR DEBUG VISITOR ! COMMENT OUT FOR STACK INTEGRATION !
    // System.out.println("\nIn opProject\n" + opProject.toString());
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    
    // visit sub operation
    opProject.getSubOp().visit(this);
    // get vars from select statement
    List<Var> vars = opProject.getVars();
    // build return clause
    return_clause = setgetReturnClause(vars, has_with_clause);
    // concat return clause
    concatCypher(return_clause);
    // build end of agtype cypher query
    buildEndOfAgeQuery(vars);
  }


  // ORDER BY
  @Override 
  public void visit(OpOrder opOrder) {
    //! ONLY FOR DEBUG VISITOR ! COMMENT OUT FOR STACK INTEGRATION !
    // System.out.println("\nIn opOrder BEFORE\n" + opOrder.toString());
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    
    // visit sub operation
    opOrder.getSubOp().visit(this);
    // set detection var for WITH clause, required to return binding variables
    // see: https://age.apache.org/age-manual/master/clauses/order_by.html

    has_with_clause = true;
    // build WITH clause for all variables
    concatCypher(" WITH ");
    for(Var var: Sparql_to_cypher_variable_map.keySet()) {
      concatCypher(
        getSchemaStmt(Sparql_to_cypher_variable_map.get(var), false)
        + "AS " + Sparql_to_cypher_variable_map.get(var) + ", "
      );
    }
    cypher = cypher.substring(0, cypher.length() - 2);
    // build ORDER BY clause for all variables with regard to their conditions 
    concatCypher(" ORDER BY ");
    for(Var var: opOrder.getConditions().get(0).getExpression().getVarsMentioned()) {
      concatCypher(getSchemaStmt(Sparql_to_cypher_variable_map.get(var), true));
    }
    cypher = cypher.substring(0, cypher.length() - 2);
  }


  // OPTIONAL MATCH 
  @Override 
  public void visit(OpLeftJoin opLeftJoin) {
    //! ONLY FOR DEBUG VISITOR ! COMMENT OUT FOR STACK INTEGRATION !
    // System.out.println("\nIn opLeftJoin\n" + opLeftJoin);
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    // visit left basic graph pattern
    opLeftJoin.getLeft().visit(this);
    // cypher concat pipe to detect left and right bgp
    concatCypher("| OPTIONAL ");
    // visit right basic graph pattern
    opLeftJoin.getRight().visit(this);
    // replace all MATCH stmts with OPTIONAL MATCH in right bgp
    // OPTIONAL MATCH needs comma separation for multiple patterns
    cypher = getOptionalClause(cypher);
    // check if FILTER is present by detecting expressions
    if (opLeftJoin.getExprs() != null) {  
      // parse filter expressions and create cypher WHERE clause    
      FilterParser filter_parser = new FilterParser(use_coalesce);
      try {
        concatCypher(filter_parser.getWhereClause(opLeftJoin.getExprs()));
      } catch (QueryException e) { e.printStackTrace(); }
    } 
  }


  // DISTINCT
  @Override 
  public void visit(OpDistinct opDistinct) {
    //! ONLY FOR DEBUG VISITOR ! COMMENT OUT FOR STACK INTEGRATION !
    // System.out.println("\nIn opDistinct\n" + opDistinct.toString());
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    // visit sub operation
    opDistinct.getSubOp().visit(this);
    // DISTINCT is in the RETURN clause of age cypher
    cypher = cypher.replaceAll("RETURN", "RETURN DISTINCT");
    // set distinct detection for UNION clause, 
    //required to duplicate RETURN clause after visitors
    distinct = true;
  }


  // LIMIT and OFFSET
  @Override 
  public void visit(OpSlice opSlice) {
    //! ONLY FOR DEBUG VISITOR ! COMMENT OUT FOR STACK INTEGRATION !
    // System.out.println("\nIn opSlice\n" + opSlice.toString());
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    // visit sub operations
    opSlice.getSubOp().visit(this);
    // check if OFFSET is present -> negative start value
    if (opSlice.getStart() < 0) {
      // OFFSET not present, LIMIT available in age cypher
      cypher = cypher.replace("$$)", "LIMIT " + opSlice.getLength() + " $$)");
    } else {
      // OFFSET present, but not available in age cypher, must be a postgres feature
      // https://www.postgresql.org/docs/current/queries-limit.html
      cypher = cypher.substring(0, cypher.length() - 2);
      concatCypher(" LIMIT " + opSlice.getLength());
      concatCypher(" OFFSET " + opSlice.getStart() + ";");
      // SKIP in cypher could be an alternative
      // see: https://age.apache.org/age-manual/master/clauses/skip.html
    }
  }


  // MATCH duplicates for left and right bgp (!OpJoin necceassary for UNION below)
  @Override
  public void visit(OpJoin opJoin) {
    //! ONLY FOR DEBUG VISITOR ! COMMENT OUT FOR STACK INTEGRATION !
    // System.out.println("\nIn opJoin\n" + opJoin.toString());
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    
    // set detection for OpJoin to cache MATCH clause 
    left_bgp_join = true;
    // used to notice right bgp (unset if further applied)
    right_bgp_join = true;
    // visit left bgp -> cache MATCH clause
    opJoin.getLeft().visit(this);
    // concat cached MATCH clause to the left part
    concatCypher(cached_match_clause); 

    // unset detection for OpJoin to parse by visitors as usual
    left_bgp_join = false;
    // visit right bgp
    opJoin.getRight().visit(this);
  }


  // UNION
  @Override
  public void visit(OpUnion opUnion) {
    //! ONLY FOR DEBUG VISITOR ! COMMENT OUT FOR STACK INTEGRATION !
    // System.out.println("\nIn opUnion\n" + opUnion);
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    // visit left bgp and concat UNION clause
    opUnion.getLeft().visit(this);
    concatCypher("UNION ");
    // set detection to duplicate RETURN clause at parser end
    // see getCypher() below this visit function
    has_union_clause = true;
    // add MATCH duplicate for right bgp
    if (right_bgp_join) {
      concatCypher(cached_match_clause); 
      // unset used cache and detection
      cached_match_clause = "";
      right_bgp_join = false;
    }
    // visit right bgp
    opUnion.getRight().visit(this);
  }
  //
  ////////////////////////////////// END SUPPORTED VISITOR PATTERN OF SPARQL ALGEBRA ///////////////////////////////
  //
  ///////////////////// END SUPPORTED VISITOR PATTERN OF SPARQL ALGEBRA FOR QUERY INTEROPERABILITY /////////////////////



  /////////////////////////// THIS IS THE POINT WHERE TRANSPILED CYPHER WILL BE RETURNED ///////////////////////////////
  //
  // provide cypher query or throw exception for unassigned patterns
  public String getCypher() throws QueryException {
    if(this.isQueryConversionSuccesful) {
      // build return clause for ASK queries
      if (query_type=="ASK") {
        buildEndOfAskQuery();
      }
      // duplicate return clause before UNION if OpJoin + OpUnion is present
      if (has_union_clause) {
        // check if DISTINCT is present
        if (distinct) {
          // DISTINCT is in the RETURN clause of age cypher
          return_clause = return_clause.replaceAll("RETURN", "RETURN DISTINCT");
          // unset distinct detection
          distinct = false;
        }
        cypher = cypher.replace("UNION", return_clause + " UNION");
      }
      // provide final cypher query
      return cypher;
    }
    else throw new QueryException(this.conversionErrors);
  }
  //
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


  //////////////////////// FUTURE WORK BELOW ////////////////////////

  @Override
  public void visit(OpQuadPattern quadPattern){
    this.isQueryConversionSuccesful = false;
    this.conversionErrors += "Unsupported Algebra type OpQuadPattern\n";
  }

  @Override
  public void visit(OpQuadBlock quadBlock) {
    this.isQueryConversionSuccesful = false;
    this.conversionErrors += "Unsupported Algebra type OpQuadBlock\n";
  }

  @Override
  public void visit(OpTriple opTriple) {   
    this.isQueryConversionSuccesful = false;
    this.conversionErrors += "Unsupported Algebra type OpTriple\n";
  }

  @Override
  public void visit(OpQuad opQuad) {
    this.isQueryConversionSuccesful = false;
    this.conversionErrors += "Unsupported Algebra type OpQuad\n";
  }

  @Override
  public void visit(OpPath opPath) {
    this.isQueryConversionSuccesful = false;
    this.conversionErrors += "Unsupported Algebra type OpPath\n";
  }

  @Override
  public void visit(OpTable opTable) {
    this.isQueryConversionSuccesful = false;
    this.conversionErrors += "Unsupported Algebra type OpTable\n";
  }

  @Override
  public void visit(OpNull opNull) {
    this.isQueryConversionSuccesful = false;
    this.conversionErrors += "Unsupported Algebra type OpNull\n";
  }

  @Override
  public void visit(OpProcedure opProc) {
    this.isQueryConversionSuccesful = false;
    this.conversionErrors += "Unsupported Algebra type OpProcedure\n";
  }

  @Override
  public void visit(OpPropFunc opPropFunc) {
    this.isQueryConversionSuccesful = false;
    this.conversionErrors += "Unsupported Algebra type OpPropFunc\n";
  }

  @Override
  public void visit(OpGraph opGraph) {
    this.isQueryConversionSuccesful = false;
    this.conversionErrors += "Unsupported Algebra type OpGraph\n";
  }

  @Override
  public void visit(OpService opService) {
    this.isQueryConversionSuccesful = false;
    this.conversionErrors += "Unsupported Algebra type OpService\n";
  }

  @Override
  public void visit(OpDatasetNames dsNames) {
    this.isQueryConversionSuccesful = false;
    this.conversionErrors += "Unsupported Algebra type OpDatasetNames\n";
  }

  @Override
  public void visit(OpLabel opLabel) {
    this.isQueryConversionSuccesful = false;
    this.conversionErrors += "Unsupported Algebra type OpLabel\n";
  }

  @Override
  public void visit(OpAssign opAssign) {
    this.isQueryConversionSuccesful = false;
    this.conversionErrors += "Unsupported Algebra type OpAssign\n";
  }

  @Override
  public void visit(OpExtend opExtend) {
    this.isQueryConversionSuccesful = false;
    this.conversionErrors += "Unsupported Algebra type OpExtend\n";
  }

  @Override
  public void visit(OpDiff opDiff) {
    this.isQueryConversionSuccesful = false;
    this.conversionErrors += "Unsupported Algebra type OpDiff\n";
  }

  @Override
  public void visit(OpMinus opMinus) {
    this.isQueryConversionSuccesful = false;
    this.conversionErrors += "Unsupported Algebra type OpMinus\n";
  }

  @Override
  public void visit(OpConditional opCondition) {
    this.isQueryConversionSuccesful = false;
    this.conversionErrors += "Unsupported Algebra type OpCondition\n";
  }

  @Override
  public void visit(OpSequence opSequence) {
    this.isQueryConversionSuccesful = false;
    this.conversionErrors += "Unsupported Algebra type OpSequence\n";
  }

  @Override
  public void visit(OpDisjunction opDisjunction) {
    this.isQueryConversionSuccesful = false;
    this.conversionErrors += "Unsupported Algebra type OpDisjunction\n";
  }

  @Override
  public void visit(OpList opList) {
    this.isQueryConversionSuccesful = false;
    this.conversionErrors += "Unsupported Algebra type OpList\n";
  }

  @Override
  public void visit(OpReduced opReduced) {
    this.isQueryConversionSuccesful = false;
    this.conversionErrors += "Unsupported Algebra type OpReduced\n";
  }

  @Override
  public void visit(OpGroup opGroup) {
    this.isQueryConversionSuccesful = false;
    this.conversionErrors += "Unsupported Algebra type OpGroup\n";
  }

  @Override
  public void visit(OpTopN opTop) {
    this.isQueryConversionSuccesful = false;
    this.conversionErrors += "Unsupported Algebra type OpTop\n";
  }
  
  @Override
  public void visit(OpLateral opLateral) {
    this.isQueryConversionSuccesful = false;
    this.conversionErrors += "Unsupported Algebra type OpLateral\n";
  }

}

