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
package jetbrains.jetpad.hybrid;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import jetbrains.jetpad.base.Value;
import jetbrains.jetpad.cell.action.CellActions;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.cell.TextCell;
import jetbrains.jetpad.cell.action.CellAction;
import jetbrains.jetpad.cell.completion.*;
import jetbrains.jetpad.hybrid.parser.Token;
import jetbrains.jetpad.cell.Cell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static jetbrains.jetpad.hybrid.SelectionPosition.*;
import static jetbrains.jetpad.cell.action.CellActions.seq;

class TokenCompletion {
  private HybridSynchronizer<?> mySync;

  TokenCompletion(HybridSynchronizer<?> sync) {
    mySync = sync;
  }

  private HybridPositionSpec<?> positionSpec() {
    return mySync.positionSpec();
  }

  private TokenListEditor<?> tokenListEditor() {
    return mySync.tokenListEditor();
  }

  private TokenOperations<?> tokenOperations() {
    return mySync.tokenOperations();
  }

  CompletionHelper completion(Function<Token, CellAction> handler) {
    return new CompletionHelper(positionSpec().getTokenCompletion(handler).get(CompletionParameters.EMPTY));
  }

  CompletionSupplier placeholderCompletion() {
    return tokenCompletion(new PlaceholderCompletionContext(), new BaseCompleter() {
      @Override
      public CellAction complete(int selectionIndex, Token... tokens) {
        tokenListEditor().tokens.addAll(Arrays.asList(tokens));
        tokenListEditor().updateToPrintedTokens();

        return tokenOperations().selectOnCreation(selectionIndex, LAST);
      }
    });
  }

  CompletionSupplier tokenCompletion(final Cell tokenCell) {
    final int index = mySync.tokenCells().indexOf(tokenCell);
    return tokenCompletion(new TokenCompletionContext(index), new BaseCompleter() {
      @Override
      public CellAction complete(int selectionIndex, Token... tokens) {
        final int caretPosition;
        String oldText = null;
        SelectionPosition position = LAST;
        if (tokenCell instanceof TextCell) {
          TextCell cell = (TextCell) tokenCell;
          caretPosition = cell.caretPosition().get();
          oldText = cell.text().get();
          if (caretPosition == 0) {
            position = FIRST;
          } else if (caretPosition == cell.text().get().length()) {
            position = LAST;
          } else {
            position = null;
          }
        } else {
          caretPosition = -1;
        }


        CompletionController controller = tokenCell.get(Completion.COMPLETION_CONTROLLER);
        final boolean wasCompletionActive = controller != null && controller.isActive();

        tokenListEditor().tokens.remove(index);
        int i = index;
        for (Token t : tokens) {
          tokenListEditor().tokens.add(i++, t);
        }

        tokenListEditor().updateToPrintedTokens();

        final Cell targetCell =  mySync.tokenCells().get(index + selectionIndex);
        if (!(targetCell instanceof TextCell) || !Objects.equal(((TextCell) targetCell).text().get(), oldText)) {
          position = LAST;
        }

        CellAction result;
        if (position == null) {
          result = new CellAction() {
            @Override
            public void execute() {
              targetCell.focus();
              ((TextCell) targetCell).caretPosition().set(caretPosition);
            }
          };
        } else {
          result = mySync.tokenOperations().selectOnCreation(index + selectionIndex, position);
        }

        if (wasCompletionActive) {
          result = CellActions.seq(result, activateCompletion(index + selectionIndex));
        }
        return result;
      }
    });
  }

  CompletionSupplier sideTransform(Cell tokenCell, final int delta) {
    final int index = mySync.tokenCells().indexOf(tokenCell);

    return new CompletionSupplier() {
      @Override
      public List<CompletionItem> get(final CompletionParameters cp) {
        BaseCompleter completer = new BaseCompleter() {
          @Override
          public CellAction complete(int selectionIndex, Token... tokens) {
            int i = index + delta;
            for (Token t : tokens) {
              tokenListEditor().tokens.add(i++, t);
            }
            tokenListEditor().updateToPrintedTokens();
            CellAction result = tokenOperations().selectOnCreation(index + delta + selectionIndex, LAST);
            if (cp.isEndRightTransform()) {
              result = CellActions.seq(result, activateCompletion(index + delta + selectionIndex));
            }
            return result;
          }
        };

        if (cp.isEndRightTransform()) {
          return tokenCompletion(new TokenCompletionContext(index + 1), completer).get(cp);
        }

        return tokenCompletion(new TokenCompletionContext(index + delta), completer).get(cp);
      }
    };
  }

  private CompletionSupplier tokenCompletion(final CompletionContext ctx, final Completer completer) {
    return new CompletionSupplier() {
      @Override
      public List<CompletionItem> get(CompletionParameters cp) {
        List<CompletionItem> result = new ArrayList<CompletionItem>();
        result.addAll(positionSpec().getTokenCompletion(new Function<Token, CellAction>() {
          @Override
          public CellAction apply(Token input) {
            return completer.complete(input);
          }
        }).get(cp));
        if (cp.isMenu()) {
          result.addAll(positionSpec().getAdditionalCompletion(ctx, completer).get(cp));
        }
        return result;
      }
    };
  }

  Token completeToken(String text) {
    final Value<Token> result = new Value<Token>();
    CompletionHelper completion = completion(new Function<Token, CellAction>() {
      @Override
      public CellAction apply(Token token) {
        result.set(token);
        return CellAction.EMPTY;
      }
    });
    List<CompletionItem> matches = completion.matches(text);
    if (matches.size() == 1) {
      matches.get(0).complete(text);
      return result.get();
    }
    return null;
  }

  private CellAction activateCompletion(final int index) {
    return new CellAction() {
      @Override
      public void execute() {
        CompletionController ctrl = mySync.tokenCells().get(index).get(Completion.COMPLETION_CONTROLLER);
        if (ctrl.hasAmbiguousMatches()) {
          ctrl.setActive(true);
        }
      }
    };
  }

  private class PlaceholderCompletionContext implements CompletionContext {
    @Override
    public int targetIndex() {
      return 0;
    }

    @Override
    public List<Token> prefix() {
      return Collections.emptyList();
    }

    @Override
    public List<Cell> views() {
      return Collections.emptyList();
    }

    @Override
    public List<Token> tokens() {
      return Collections.emptyList();
    }

    @Override
    public List<Object> objects() {
      return Collections.emptyList();
    }

    @Override
    public Mapper<?, ?> contextMapper() {
      return mySync.contextMapper();
    }

    @Override
    public Object target() {
      return null;
    }
  }

  private class TokenCompletionContext implements CompletionContext {
    private int myTargetIndex;

    private TokenCompletionContext(int targetIndex) {
      myTargetIndex = targetIndex;
    }

    @Override
    public int targetIndex() {
      return myTargetIndex;
    }

    @Override
    public List<Token> prefix() {
      return Collections.unmodifiableList(tokenListEditor().tokens.subList(0, myTargetIndex));
    }

    @Override
    public List<Cell> views() {
      return Collections.unmodifiableList(mySync.tokenCells().subList(0, myTargetIndex));
    }

    @Override
    public List<Token> tokens() {
      return Collections.unmodifiableList(tokenListEditor().tokens);
    }

    @Override
    public List<Object> objects() {
      return tokenListEditor().objects();
    }

    @Override
    public Mapper<?, ?> contextMapper() {
      return mySync.contextMapper();
    }

    @Override
    public Object target() {
      return mySync.property().get();
    }
  }
}