/*
 * Copyright 2012-2013 JetBrains s.r.o
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.jetpad.grammar.slr;

import com.google.common.collect.Range;
import jetbrains.jetpad.grammar.*;
import jetbrains.jetpad.grammar.lr.LRParser;
import jetbrains.jetpad.grammar.lr.LRTable;
import jetbrains.jetpad.grammar.lr.Lexeme;
import org.junit.Test;

import static jetbrains.jetpad.grammar.GrammarTestUtil.asTokens;
import static org.junit.Assert.*;

public class SLRParserGenerationTest {
  @Test(expected = IllegalArgumentException.class)
  public void emptyGrammarInvalid() {
    Grammar g = new Grammar();
    new SLRTableGenerator(g).generateTable();
  }

  @Test(expected = IllegalArgumentException.class)
  public void grammarWithTerminalStartRuleInvalid() {
    Grammar g = new Grammar();
    g.newRule(g.getStart(), g.newTerminal("id"));
    new SLRTableGenerator(g).generateTable();
  }

  @Test(expected = IllegalArgumentException.class)
  public void grammarWithTooManyRulesInvalid() {
    Grammar g = new Grammar();
    g.newRule(g.getStart(), g.newNonTerminal("x"));
    g.newRule(g.getStart(), g.newNonTerminal("y"));
    new SLRTableGenerator(g).generateTable();
  }

  @Test
  public void parserGenerationAndParsing() {
    Grammar g = new Grammar();

    NonTerminal start = g.getStart();
    NonTerminal expr = g.newNonTerminal("E");
    NonTerminal term = g.newNonTerminal("T");
    NonTerminal fact = g.newNonTerminal("F");

    Terminal id = g.newTerminal("id");
    Terminal plus = g.newTerminal("+");
    Terminal star = g.newTerminal("*");
    Terminal lp = g.newTerminal("(");
    Terminal rp = g.newTerminal(")");

    g.newRule(start, expr);
    g.newRule(expr, expr, plus, term);
    g.newRule(expr, term);
    g.newRule(term, term, star, fact);
    g.newRule(term, fact);
    g.newRule(fact, id);
    g.newRule(fact, lp, expr, rp);

    SLRTableGenerator gen = new SLRTableGenerator(g);

    LRTable table = gen.generateTable();

    LRParser parser = new LRParser(table);

    assertTrue(parser.parse(id));
    assertFalse(parser.parse(id, plus));
    assertTrue(parser.parse(id, plus, id));
    assertTrue(parser.parse(lp, id, rp));
  }

  @Test(expected = IllegalStateException.class)
  public void ambiguityDetection() {
    Grammar g = new Grammar();
    NonTerminal start = g.getStart();
    NonTerminal expr = g.newNonTerminal("E");
    Symbol plus = g.newTerminal("+");
    Symbol id = g.newTerminal("id");

    g.newRule(start, expr);
    g.newRule(expr, plus, expr);
    g.newRule(expr, expr, plus, expr);
    g.newRule(expr, id);

    new SLRTableGenerator(g).generateTable();
  }

  @Test
  public void ambiguityResolutionWithAssocPropertyToLeftAssoc() {
    SimplePrecedenceGrammar g = new SimplePrecedenceGrammar();
    g.plusRule.setAssociativity(Associativity.LEFT).setPriority(0);

    LRParser parser = new LRParser(new SLRTableGenerator(g.grammar).generateTable());
    Object parse = parser.parse(asTokens(g.id, g.plus, g.id, g.plus, g.id));

    assertEquals("((id + id) + id)", parse.toString());
  }

  @Test
  public void ambiguityResolutionWithAssocPropertyToRightAssoc() {
    SimplePrecedenceGrammar g = new SimplePrecedenceGrammar();
    g.plusRule.setAssociativity(Associativity.RIGHT).setPriority(0);

    LRParser parser = new LRParser(new SLRTableGenerator(g.grammar).generateTable());
    Object parse = parser.parse(asTokens(g.id, g.plus, g.id, g.plus, g.id));

    assertEquals("(id + (id + id))", parse.toString());
  }

  @Test
  public void ambiguityResolutionWithPriority() {
    SimplePrecedenceGrammarWithDifferentPriorities g = new SimplePrecedenceGrammarWithDifferentPriorities();

    g.plusRule.setAssociativity(Associativity.LEFT);
    g.plusRule.setPriority(0);

    g.mulRule.setAssociativity(Associativity.LEFT);
    g. mulRule.setPriority(1);

    LRTable table = new SLRTableGenerator(g.grammar).generateTable();
    LRParser parser = new LRParser(table);

    Object parse = parser.parse(asTokens(g.id, g.plus, g.id, g.mul, g.id));

    assertEquals("(id + (id * id))", parse.toString());
  }

  @Test
  public void positionsDuringParsing() {
    SimplePrecedenceGrammar g = new SimplePrecedenceGrammar();
    g.plusRule.setAssociativity(Associativity.LEFT);
    g.plusRule.setPriority(0);

    LRTable table = new SLRTableGenerator(g.grammar).generateTable();

    LRParser parser = new LRParser(table);
    BinExpr parse = (BinExpr) parser.parse(asTokens(g.id, g.plus, g.id));

    assertEquals(Range.closed(0, 3), parse.getRange());
    assertEquals(Range.closed(0, 1), parse.left.getRange());
    assertEquals(Range.closed(2, 3), parse.right.getRange());
  }

  private class SimplePrecedenceGrammar {
    final Grammar grammar = new Grammar();

    final NonTerminal start = grammar.getStart();
    final NonTerminal expr = grammar.newNonTerminal("E");
    final Terminal plus = grammar.newTerminal("+");
    final Terminal id = grammar.newTerminal("id");

    final Rule startRule = grammar.newRule(start, expr);
    final Rule idRule = grammar.newRule(expr, id);
    final Rule plusRule = grammar.newRule(expr, expr, plus, expr);

    {
      startRule.setHandler(new RuleHandler() {
        @Override
        public Object handle(RuleContext ctx) {
          return ctx.get(0);
        }
      });

      idRule.setHandler(new RuleHandler() {
        @Override
        public Object handle(RuleContext ctx) {
          return new IdExpr(ctx.getRange());
        }
      });

      plusRule.setHandler(new BinOpHandler());
    }

    class BinOpHandler implements RuleHandler {
      @Override
      public Object handle(RuleContext ctx) {
        Expr left = (Expr) ctx.get(0);
        Lexeme sign = (Lexeme) ctx.get(1);
        Expr right = (Expr) ctx.get(2);
        return new BinExpr(left, right, sign.getTerminal(), ctx.getRange());
      }
    }
  }

  private class SimplePrecedenceGrammarWithDifferentPriorities extends SimplePrecedenceGrammar {
    final Terminal mul = grammar.newTerminal("*");

    final Rule mulRule = grammar.newRule(expr, expr, mul, expr);

    {
      mulRule.setHandler(new BinOpHandler());
    }
  }

  private abstract class Expr {
    private Range<Integer> myRange;

    protected Expr(Range<Integer> range) {
      myRange = range;
    }

    Range<Integer> getRange() {
      return myRange;
    }
  }

  private class IdExpr extends Expr {
    private IdExpr(Range<Integer> range) {
      super(range);
    }

    @Override
    public String toString() {
      return "id";
    }
  }

  private class BinExpr extends Expr {
    final Expr left;
    final Expr right;
    final Terminal symbol;

    private BinExpr(Expr left, Expr right, Terminal symbol, Range<Integer> range) {
      super(range);
      this.left = left;
      this.right = right;
      this.symbol = symbol;
    }

    @Override
    public String toString() {
      return "(" + left + " " + symbol + " " + right + ")";
    }
  }
}