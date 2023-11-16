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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;


public class FilterParser {
  private boolean isQueryConversionSuccesful = true;
  private String conversionErrors = "";
  public String cypher;
  private HashMap<Integer, String> expr_map;
  private int expr_counter;

  // constructor to apply filter parser
  public FilterParser() {
    this.cypher = new String();
    this.expr_map = new HashMap<Integer, String>();
    this.expr_counter = 0;
  }

  // return where clause and handle exceptions
  public String getWhereClause(ExprList exprList) throws QueryException {
    filterParser(exprList);
    if (this.isQueryConversionSuccesful) return cypher;
    else throw new QueryException(this.conversionErrors);
  }

  // concat cypher string
  public void concatCypher(String _cypher) {
    cypher = cypher.concat(_cypher);
  }

  // coalesce statement for transpilation
  public String coalesceStmt(String var_name, Boolean seperate) {
    String stmt = "";
    stmt = stmt.concat(
      "coalesce("
      + var_name + ".iri, "     // Resource
      + var_name + ".bnid, "    // BlankNode
      + var_name + ".value)"    // Literal
    );
    if(seperate) stmt = stmt.concat(", ");
    // else stmt = stmt.concat(" ");
    return stmt;
  }

  // supported filter operators for transpilation
  public String supportedOperator(String operator) {
    switch (operator) {
      case "||":
        return " OR ";
      case "&&":
        return " AND ";
      case "!":
        return " NOT ";
      case "<":
        return " < ";
      case ">":
        return " > ";
      case "=":
        return " = ";
      case "!=":
        return " <> ";
      case "regex":
        return " =~ ";
      default:
        // not supported exception
        this.isQueryConversionSuccesful = false;
        this.conversionErrors += "\nUnsupported Algebra Filter (Operator): " + operator;
        return operator;
    }
  }

  // supported filter type casts for transpilation
  public String supportedCast(String expr_type, String expr_str) {
    switch (expr_type) {
      case "NodeValueString":
        if (expr_str.contains("\"")) {
          return "toString(\"" + expr_str.replace("\"", "") + "\")";
        } else if (expr_str.contains("\'")) {
          return "toString(\'" + expr_str.replace("\'", "") + "\')";
        } else {
          return "toString(" + expr_str + ")";
        }
      case "NodeValueDecimal":
        return "toFloat(" + expr_str + ")";
      case "NodeValueInteger":
        return "toInteger(" + expr_str + ")";
      case "NodeValueNode":
        // cast to string and remove '<>' from iri
        return "toString(" + expr_str.replaceAll("^<(.*)>$", "'$1'") + ")";
      default:
        // not supported exception
        this.isQueryConversionSuccesful = false;
        this.conversionErrors += "\nUnsupported Algebra Filter (Cast): " + expr_type;
        return expr_str;
    }
  }

  // supported additional regex args for transpilation
  public String supportedRegex3(String reg_str) {
    switch (reg_str) {
      case "i":
        return "(?i)";
      default:
        // not supported exception
        this.isQueryConversionSuccesful = false;
        this.conversionErrors += "\nUnsupported Algebra Filter (Regex3): " + reg_str;       
        return reg_str;
    }
  }

  // retrieve expressions by type
  public String get_typed_expr(
    Expr expr, 
    Boolean expr_has_const, 
    String expr_const_type, 
    String reg3
  ) {
    String typed_expr = "";

    // check if expression is constant 
    if (expr.isConstant()) {
      typed_expr = supportedCast(
        expr.getConstant().getClass().getSimpleName(), 
        reg3.concat(expr.getConstant().toString())
      );
    // check if expression is variable and has no constant neighbor
    } else if (expr.isVariable() && !expr_has_const) {
      typed_expr = coalesceStmt(expr.getVarName(), false);
    // check if expression var must be casted to equalize type with constant
    } else if (expr.isVariable() && expr_has_const) {
      typed_expr = supportedCast(
        expr_const_type,
        coalesceStmt(expr.getVarName(), false)
      );
    } else {
      // nothing to do (e.g. expr is function)
    }
    // deliver type casted and concatenated expression
    return typed_expr;
  }

  // recursive function to parse all listed expressions
  public void parseListExpr(List<Expr> listExpr) {
    List<Expr> expr_list = new ArrayList<Expr>();
    Boolean expr_has_const = false;
    String expr_const_type = "";

    // stop recursion if list is empty
    if (listExpr.get(0).getFunction() != null) {
      // iterate over all expressions in list
      for (Expr expr : listExpr) {
        Integer num_args = expr.getFunction().numArgs();
        String operator = expr.getFunction().getOpName();
        Expr sub_list_1 = expr.getFunction().getArgs().subList(0, 1).get(0);

        // parse expression by number of arguments
        if (num_args < 2) {
          // TODO: NUM_ARGS < 2
          this.isQueryConversionSuccesful = false;
          this.conversionErrors += "\nUnsupported Algebra Filter (Num of Args): " + num_args;
        } else {
          Expr sub_list_2 = expr.getFunction().getArgs().subList(1, 2).get(0);
          Expr sub_list_3 = null;
          String reg3 = "";
          int _expr_cnt = expr_counter;
  
          // check if operator is advanced (e.g. regex) and get operator
          if (operator == null) {
            operator = expr.getFunction().getFunctionSymbol().getSymbol();
            // check if expression has a 3rd argument
            if (num_args == 3) {
              sub_list_3 = expr.getFunction().getArgs().subList(2, 3).get(0);
              reg3 = supportedRegex3(sub_list_3.toString().replace("\"", ""));
              System.out.println("SUB LIST 3: " + sub_list_3);
            }
          }
  
          // check if expression contains constant for var type cast equalization
          if (sub_list_1.isConstant()) {
            expr_has_const = true;
            expr_const_type = sub_list_1.getClass().getSimpleName();
            // System.out.println("DEBUG EXPR HAS CONST: " + expr_has_const);
            // System.out.println("DEBUG EXPR CONST TYPE: " + expr_const_type);
          }
          if (sub_list_2.isConstant()) {
            expr_has_const = true;
            expr_const_type = sub_list_2.getClass().getSimpleName();
            // System.out.println("DEBUG EXPR HAS CONST: " + expr_has_const);
            // System.out.println("DEBUG EXPR CONST TYPE: " + expr_const_type);
          }
  
          // left side of expression
          if (get_typed_expr(sub_list_1, expr_has_const, expr_const_type, reg3).isEmpty()){
            // add expression to map if left side is empty for nested expressions
            expr_counter++;
            expr_map.put(expr_counter, supportedOperator(expr.getFunction().getOpName()));
          } else {
            // concat cypher, if left side of expression is not empty
            concatCypher(
              "(" +
              get_typed_expr(sub_list_1, expr_has_const, expr_const_type, reg3)
            );
          }
  
          // concat operator only if left side (sub_list_1) is not empty
          if (expr_counter == _expr_cnt) {
            concatCypher(supportedOperator(operator));
          }
  
          // right side of expression
          if (!get_typed_expr(sub_list_2, expr_has_const, expr_const_type, reg3).isEmpty()){
            if (expr_counter > 0) {
              concatCypher(
                get_typed_expr(sub_list_2, expr_has_const, expr_const_type, reg3)
                + ")" + expr_map.get(expr_counter)
              );
              expr_counter--;
            } else {
              concatCypher(
                get_typed_expr(sub_list_2, expr_has_const, expr_const_type, reg3)
                + ")"
              );
            }
          } 
  
          // provide sub expressions for recursive function call
          expr_list = expr.getFunction().getArgs();
          parseListExpr(expr_list);
        }
      }
    } 
  }

  // cypher parser for sparql filter expressions
  public void filterParser(ExprList exprList) {
    // convert ExprList to List<Expr>
    List<Expr> list_expr = new ArrayList<Expr>();
    for (Expr expr : exprList){
      list_expr.add(expr);
    }
    // create WHERE clause
    concatCypher("WHERE ");
    // parse all listed expressions recursively
    parseListExpr(list_expr);
    concatCypher(" ");
  }

}
