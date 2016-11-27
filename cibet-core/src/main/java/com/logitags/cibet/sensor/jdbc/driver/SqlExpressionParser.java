/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 *******************************************************************************
 */
package com.logitags.cibet.sensor.jdbc.driver;

import net.sf.jsqlparser.expression.AllComparisonExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.InverseExpression;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseAnd;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseOr;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseXor;
import net.sf.jsqlparser.expression.operators.arithmetic.Concat;
import net.sf.jsqlparser.expression.operators.arithmetic.Division;
import net.sf.jsqlparser.expression.operators.arithmetic.Multiplication;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.Matches;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.SubSelect;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SqlExpressionParser implements ExpressionVisitor {

   private Log log = LogFactory.getLog(SqlExpressionParser.class);

   private Object value = null;

   @Override
   public void visit(NullValue arg0) {
      log.debug("sql expression = " + arg0.toString());
      value = null;
   }

   @Override
   public void visit(Function arg0) {
      log.debug("sql function expression = " + arg0.toString());
      value = arg0.toString();
   }

   @Override
   public void visit(InverseExpression arg0) {
      Expression innerExp = arg0.getExpression();
      innerExp.accept(this);
      if (value != null) {
         value = "- " + value.toString();
      }
   }

   @Override
   public void visit(JdbcParameter arg0) {
      log.debug("sql parameter expression = " + arg0.toString());
      value = arg0.toString();
   }

   @Override
   public void visit(DoubleValue arg0) {
      log.debug("sql double value expression = " + arg0.toString());
      value = arg0.getValue();
   }

   @Override
   public void visit(LongValue arg0) {
      log.debug("sql long value expression = " + arg0.toString());
      value = arg0.getValue();
   }

   @Override
   public void visit(DateValue arg0) {
      log.debug("sql Date value expression = " + arg0.getValue());
      value = arg0.getValue();
   }

   @Override
   public void visit(TimeValue arg0) {
      log.debug("sql Time value expression = " + arg0.toString());
      value = arg0.getValue();
   }

   @Override
   public void visit(TimestampValue arg0) {
      log.debug("sql Timestamp value expression = " + arg0.toString());
      value = arg0.getValue();
   }

   @Override
   public void visit(Parenthesis arg0) {
      log.debug("sql Parenthesis value expression = " + arg0.toString());
      value = arg0.toString();
   }

   @Override
   public void visit(StringValue arg0) {
      log.debug("sql String value expression = " + arg0.toString());
      value = arg0.getValue();
   }

   @Override
   public void visit(Addition arg0) {
      log.debug("sql expression = " + arg0.toString());
      value = arg0.toString();
   }

   @Override
   public void visit(Division arg0) {
      log.debug("sql expression = " + arg0.toString());
      value = arg0.toString();
   }

   @Override
   public void visit(Multiplication arg0) {
      log.debug("sql expression = " + arg0.toString());
      value = arg0.toString();
   }

   @Override
   public void visit(Subtraction arg0) {
      log.debug("sql expression = " + arg0.toString());
      value = arg0.toString();
   }

   @Override
   public void visit(AndExpression arg0) {
      log.debug("sql expression = " + arg0.toString());
      value = arg0.getStringExpression();
   }

   @Override
   public void visit(OrExpression arg0) {
      log.debug("sql expression = " + arg0.toString());
      value = arg0.getStringExpression();
   }

   @Override
   public void visit(Between arg0) {
      log.debug("sql expression = " + arg0.toString());
      value = arg0.toString();
   }

   @Override
   public void visit(EqualsTo arg0) {
      log.debug("sql expression = " + arg0.toString());
      value = arg0.getStringExpression();
   }

   @Override
   public void visit(GreaterThan arg0) {
      log.debug("sql expression = " + arg0.toString());
      value = arg0.getStringExpression();
   }

   @Override
   public void visit(GreaterThanEquals arg0) {
      log.debug("sql expression = " + arg0.toString());
      value = arg0.getStringExpression();
   }

   @Override
   public void visit(InExpression arg0) {
      log.debug("sql expression = " + arg0.toString());
      value = arg0.toString();
   }

   @Override
   public void visit(IsNullExpression arg0) {
      log.debug("sql expression = " + arg0.toString());
      value = arg0.toString();
   }

   @Override
   public void visit(LikeExpression arg0) {
      log.debug("sql expression = " + arg0.toString());
      log.debug("sql String expression = " + arg0.getStringExpression());
      value = arg0.getStringExpression();
   }

   @Override
   public void visit(MinorThan arg0) {
      log.debug("sql expression = " + arg0.toString());
      value = arg0.getStringExpression();
   }

   @Override
   public void visit(MinorThanEquals arg0) {
      log.debug("sql expression = " + arg0.toString());
      value = arg0.getStringExpression();
   }

   @Override
   public void visit(NotEqualsTo arg0) {
      log.debug("sql expression = " + arg0.toString());
      value = arg0.getStringExpression();
   }

   @Override
   public void visit(Column arg0) {
      log.debug("sql expression = " + arg0.toString());
      value = arg0.toString();
   }

   @Override
   public void visit(SubSelect arg0) {
      log.debug("sql expression = " + arg0.toString());
      value = arg0.toString();
   }

   @Override
   public void visit(CaseExpression arg0) {
      log.debug("sql expression = " + arg0.toString());
      value = arg0.toString();
   }

   @Override
   public void visit(WhenClause arg0) {
      log.debug("sql expression = " + arg0.toString());
      value = arg0.toString();
   }

   @Override
   public void visit(ExistsExpression arg0) {
      log.debug("sql expression = " + arg0.toString());
      value = arg0.toString();
   }

   @Override
   public void visit(AllComparisonExpression arg0) {
      log.debug("sql expression = " + arg0.GetSubSelect().toString());
      value = arg0.GetSubSelect().toString();
   }

   @Override
   public void visit(AnyComparisonExpression arg0) {
      log.debug("sql expression = " + arg0.GetSubSelect().toString());
      value = arg0.GetSubSelect().toString();
   }

   @Override
   public void visit(Concat arg0) {
      log.debug("sql expression = " + arg0.getStringExpression());
      value = arg0.getStringExpression();
   }

   @Override
   public void visit(Matches arg0) {
      log.debug("sql expression = " + arg0.getStringExpression());
      value = arg0.getStringExpression();
   }

   @Override
   public void visit(BitwiseAnd arg0) {
      log.debug("sql expression = " + arg0.getStringExpression());
      value = arg0.getStringExpression();
   }

   @Override
   public void visit(BitwiseOr arg0) {
      log.debug("sql expression = " + arg0.getStringExpression());
      value = arg0.getStringExpression();
   }

   @Override
   public void visit(BitwiseXor arg0) {
      log.debug("sql expression = " + arg0.getStringExpression());
      value = arg0.getStringExpression();
   }

   /**
    * @return the value
    */
   public Object getValue() {
      return value;
   }

}
