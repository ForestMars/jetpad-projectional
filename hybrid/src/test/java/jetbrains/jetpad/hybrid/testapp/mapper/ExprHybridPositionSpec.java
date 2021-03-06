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
package jetbrains.jetpad.hybrid.testapp.mapper;

import com.google.common.base.Function;
import jetbrains.jetpad.cell.action.CellAction;
import jetbrains.jetpad.cell.completion.*;
import jetbrains.jetpad.cell.util.Validators;
import jetbrains.jetpad.hybrid.Completer;
import jetbrains.jetpad.hybrid.CompletionContext;
import jetbrains.jetpad.hybrid.HybridPositionSpec;
import jetbrains.jetpad.hybrid.parser.*;
import jetbrains.jetpad.hybrid.parser.prettyprint.PrettyPrinter;
import jetbrains.jetpad.hybrid.parser.prettyprint.PrettyPrinterContext;
import jetbrains.jetpad.hybrid.testapp.model.*;

import java.util.ArrayList;
import java.util.List;

public class ExprHybridPositionSpec implements HybridPositionSpec<Expr> {
  @Override
  public Parser<Expr> getParser() {
    return new Parser<Expr>() {
      @Override
      public Expr parse(ParsingContext ctx) {
        Expr result = parseExpr(ctx);
        if (ctx.current() != null) return null;
        return result;
      }

      private Expr parseExpr(ParsingContext ctx) {
        return parsePlus(ctx);
      }

      private Expr parsePlus(ParsingContext ctx) {
        Expr result = parseMul(ctx);
        if (result == null) return result;
        while (ctx.current() == Tokens.PLUS) {
          ParsingContext.State startState = ctx.saveState();
          ctx.advance();

          Expr otherFactor = parseMul(ctx);
          if (otherFactor == null) {
            startState.restore();
            return result;
          }

          BinExpr binExpr = new PlusExpr();
          binExpr.left.set(result);
          binExpr.right.set(otherFactor);

          result = binExpr;
        }
        return result;
      }

      private Expr parseMul(ParsingContext ctx) {
        Expr result = parsePostfix(ctx);
        if (result == null) return result;
        while (ctx.current() == Tokens.MUL) {
          ParsingContext.State startState = ctx.saveState();
          ctx.advance();

          Expr otherFactor = parsePostfix(ctx);
          if (otherFactor == null) {
            startState.restore();
            return result;
          }


          BinExpr binExpr = new MulExpr();
          binExpr.left.set(result);
          binExpr.right.set(otherFactor);

          result = binExpr;
        }
        return result;
      }

      private Expr parsePostfix(ParsingContext ctx) {
        Expr result = parsePrimary(ctx);
        if (result == null) return result;
        while (ctx.current() == Tokens.INCREMENT) {
          ctx.advance();
          PostfixIncrementExpr inc = new PostfixIncrementExpr();
          inc.expr.set(result);
          result = inc;
        }
        return result;
      }

      private Expr parsePrimary(ParsingContext ctx) {
        ParsingContext.State state = ctx.saveState();
        Token current = ctx.current();

        if (current == Tokens.ID) {
          ctx.advance();
          return new IdExpr();
        }

        if (Tokens.isLp(current)) {
          ctx.advance();

          Expr expr = parseExpr(ctx);
          if (expr == null) {
            state.restore();
            return null;
          }

          current = ctx.current();
          if (current == Tokens.RP) {
            ctx.advance();

            ParenExpr parens = new ParenExpr();
            parens.expr.set(expr);
            return parens;
          }

          state.restore();
          return null;
        }

        if (current instanceof IntValueToken) {
          ctx.advance();
          IntValueToken token = (IntValueToken) current;
          NumberExpr result = new NumberExpr();
          result.value.set(token.getValue());
          return result;
        }

        if (current instanceof IdentifierToken) {
          ctx.advance();
          IdentifierToken token = (IdentifierToken) current;

          if (Tokens.isLp(ctx.current())) {
            ctx.advance();
            if (ctx.current() == Tokens.RP) {
              ctx.advance();
              CallExpr call = new CallExpr();
              call.name.set(token.getName());
              return call;
            } else {
              state.restore();
              return null;
            }
          } else {
            VarExpr result = new VarExpr();
            result.name.set(token.getName());
            return result;
          }
        }

        if (current instanceof ValueToken && ((ValueToken) current).value() instanceof ValueExpr) {
          ctx.advance();
          return new ValueExpr();
        }

        if (current instanceof ValueToken && ((ValueToken) current).value() instanceof PosValueExpr) {
          ctx.advance();
          return new PosValueExpr();
        }

        if (current instanceof ValueToken && ((ValueToken) current).value() instanceof ComplexValueExpr) {
          ctx.advance();
          return new ComplexValueExpr();
        }

        return null;
      }
    };
  }

  @Override
  public PrettyPrinter<Expr> getPrettyPrinter() {
    return new PrettyPrinter<Expr>() {
      @Override
      public void print(final Expr value, final PrettyPrinterContext<Expr> ctx) {
        if (value instanceof BinExpr) {
          BinExpr expr = (BinExpr) value;
          ctx.append(expr.left);
          if (expr instanceof PlusExpr) {
            ctx.append(Tokens.PLUS);
          } else if (expr instanceof MulExpr) {
            ctx.append(Tokens.MUL);
          }
          ctx.append(expr.right);
          return;
        }

        if (value instanceof ParenExpr) {
          ParenExpr paren = (ParenExpr) value;
          ctx.append(Tokens.LP);
          ctx.append(paren.expr);
          ctx.append(Tokens.RP);
          return;
        }

        if (value instanceof PostfixIncrementExpr) {
          PostfixIncrementExpr incr = (PostfixIncrementExpr) value;
          ctx.append(incr.expr);
          ctx.append(Tokens.INCREMENT);
          return;
        }

        if (value instanceof CallExpr) {
          CallExpr callExpr = (CallExpr) value;
          ctx.appendId(callExpr.name);
          ctx.append(Tokens.LP_CALL);
          ctx.append(Tokens.RP);
          return;
        }

        if (value instanceof VarExpr) {
          VarExpr varExpr = (VarExpr) value;
          ctx.appendId(varExpr.name);
          return;
        }

        if (value instanceof IdExpr) {
          ctx.append(Tokens.ID);
          return;
        }

        if (value instanceof NumberExpr) {
          NumberExpr num = (NumberExpr) value;
          ctx.appendInt(num.value);
          return;
        }

        if (value instanceof ValueExpr) {
          ctx.append(new ValueToken(value));
          return;
        }

        if (value instanceof ComplexValueExpr) {
          ctx.append(new ValueToken(value));
          return;
        }

        throw new IllegalStateException();
      }
    };
  }

  @Override
  public CompletionSupplier getTokenCompletion(final Function<Token, CellAction> tokenHandler) {
    return new CompletionSupplier() {
      @Override
      public List<CompletionItem> get(CompletionParameters cp) {
        class SimpleTokenCompletionItem extends SimpleCompletionItem {
          private Token myToken;

          SimpleTokenCompletionItem(Token token) {
            super(token.toString());
            myToken = token;
          }

          @Override
          public CellAction complete(String text) {
            return tokenHandler.apply(myToken);
          }
        }

        List<CompletionItem> result = new ArrayList<CompletionItem>();
        result.add(new SimpleTokenCompletionItem(Tokens.ID));
        result.add(new SimpleTokenCompletionItem(Tokens.PLUS));
        result.add(new SimpleTokenCompletionItem(Tokens.INCREMENT));
        result.add(new SimpleTokenCompletionItem(Tokens.MUL));
        result.add(new SimpleTokenCompletionItem(Tokens.LP));
        result.add(new SimpleTokenCompletionItem(Tokens.RP));
        result.add(new SimpleTokenCompletionItem(Tokens.DOT));
        result.add(new SimpleCompletionItem("value") {
          @Override
          public CellAction complete(String text) {
            return tokenHandler.apply(new ValueToken(new ValueExpr()));
          }
        });
        result.add(new SimpleCompletionItem("aaaa") {
          @Override
          public CellAction complete(String text) {
            return tokenHandler.apply(new ValueToken(new ComplexValueExpr()));
          }
        });

        result.add(new SimpleCompletionItem("posValue") {
          @Override
          public CellAction complete(String text) {
            return tokenHandler.apply(new ValueToken(new PosValueExpr()));
          }
        });

        result.add(new BaseCompletionItem() {
          @Override
          public String visibleText(String text) {
            return "number";
          }

          @Override
          public boolean isStrictMatchPrefix(String text) {
            if ("".equals(text)) return true;
            return isMatch(text);
          }

          @Override
          public boolean isMatch(String text) {
            return Validators.integer().apply(text);
          }

          @Override
          public CellAction complete(String text) {
            int value;
            if (text == null || text.isEmpty()) {
              value = 0;
            } else {
              value = Integer.parseInt(text);
            }
            return tokenHandler.apply(new IntValueToken(value));
          }
        });

        result.add(new BaseCompletionItem() {
          @Override
          public String visibleText(String text) {
            return "identifier";
          }

          @Override
          public boolean isStrictMatchPrefix(String text) {
            if ("".equals(text)) return true;
            return isMatch(text);
          }

          @Override
          public boolean isMatch(String text) {
            return Validators.identifier().apply(text);
          }

          @Override
          public CellAction complete(String text) {
            return tokenHandler.apply(new IdentifierToken(text));
          }

          @Override
          public boolean isLowPriority() {
            return true;
          }
        });

        return result;
      }
    };
  }

  @Override
  public CompletionSupplier getAdditionalCompletion(CompletionContext ctx, Completer complerer) {
    return CompletionSupplier.EMPTY;
  }
}