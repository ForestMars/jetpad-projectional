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
package jetbrains.jetpad.cell.text;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import jetbrains.jetpad.cell.*;
import jetbrains.jetpad.cell.action.CellAction;
import jetbrains.jetpad.cell.action.CellActions;
import jetbrains.jetpad.cell.completion.CompletionHelper;
import jetbrains.jetpad.cell.completion.CompletionParameters;
import jetbrains.jetpad.cell.completion.CompletionSupport;
import jetbrains.jetpad.cell.util.CellStateHandler;
import jetbrains.jetpad.cell.completion.Side;
import jetbrains.jetpad.cell.trait.BaseCellTrait;
import jetbrains.jetpad.cell.trait.CellTrait;
import jetbrains.jetpad.cell.trait.CellTraitPropertySpec;
import jetbrains.jetpad.values.Color;

public class TextEditing {
  public static final CellTraitPropertySpec<Boolean> FIRST_ALLOWED = new CellTraitPropertySpec<Boolean>("firstAllowed", true);
  public static final CellTraitPropertySpec<Boolean> LAST_ALLOWED = new CellTraitPropertySpec<Boolean>("firstAllowed", true);
  public static final CellTraitPropertySpec<Boolean> DOT_LIKE_RT = new CellTraitPropertySpec<Boolean>("dotLikeRt", false);

  public static final CellTraitPropertySpec<Function<String, CellAction>> EXPAND_LEFT = new CellTraitPropertySpec<Function<String, CellAction>>("expandLeft", expansionProvider(Side.LEFT));
  public static final CellTraitPropertySpec<Function<String, CellAction>> EXPAND_RIGHT = new CellTraitPropertySpec<Function<String, CellAction>>("expandRight", expansionProvider(Side.RIGHT));
  public static final CellTraitPropertySpec<Supplier<Boolean>> AFTER_TYPE = new CellTraitPropertySpec<Supplier<Boolean>>("afterType");

  public static final CellTraitPropertySpec<Boolean> EAGER_COMPLETION = new CellTraitPropertySpec<Boolean>("eagerCompletion", false);

  private static final TextCellStateHandler TEXT_VIEW_STATE_HANDLER = new TextCellStateHandler(false);
  private static final TextCellStateHandler EDITABLE_TEXT_VIEW_STATE_HANDLER = new TextCellStateHandler(true);

  public static CellTrait textNavigation(final boolean firstAllowed, final boolean lastAllowed) {
    return new BaseCellTrait() {
      @Override
      protected CellTrait[] getBaseTraits(Cell cell) {
        return new CellTrait[] { new TextNavigationTrait() };
      }

      @Override
      public Object get(Cell cell, CellTraitPropertySpec<?> spec) {
        if (spec == FIRST_ALLOWED) {
          return firstAllowed;
        }

        if (spec == LAST_ALLOWED) {
          return lastAllowed;
        }

        if (spec == CellStateHandler.PROPERTY) {
          return TEXT_VIEW_STATE_HANDLER;
        }

        return super.get(cell, spec);
      }

      @Override
      public Object get(Cell cell, CellPropertySpec<?> spec) {
        if (spec == Cell.FOCUSABLE) {
          return true;
        }

        return super.get(cell, spec);
      }
    };
  }

  public static CellTrait textEditing() {
    return new BaseCellTrait() {
      @Override
      protected CellTrait[] getBaseTraits(Cell cell) {
        return new CellTrait[] { new TextEditingTrait() };
      }

      @Override
      public Object get(Cell cell, CellTraitPropertySpec<?> spec) {
        if (spec == CellStateHandler.PROPERTY) {
          return TEXT_VIEW_STATE_HANDLER;
        }

        return super.get(cell, spec);
      }

      @Override
      public Object get(Cell cell, CellPropertySpec<?> spec) {
        if (spec == Cell.FOCUSABLE) {
          return true;
        }

        return super.get(cell, spec);
      }
    };
  }

  public static CellTrait validTextEditing(final Predicate<String> validator, final Color validColor, final boolean selectionAvailable) {
    return new BaseCellTrait() {
      @Override
      protected CellTrait[] getBaseTraits(Cell cell) {
        return new CellTrait[] { validTextEditing(validator) };
      }

      @Override
      public Object get(Cell cell, CellTraitPropertySpec<?> spec) {
        if (spec == ValidTextEditingTrait.VALID_TEXT_COLOR) {
          return validColor;
        }

        if (spec == TextNavigationTrait.SELECTION_AVAILABLE) {
          return selectionAvailable;
        }

        return super.get(cell, spec);
      }
    };
  }

  public static CellTrait validTextEditing(final Predicate<String> validator, final Color validColor) {
    return new BaseCellTrait() {
      @Override
      protected CellTrait[] getBaseTraits(Cell cell) {
        return new CellTrait[] { validTextEditing(validator) };
      }

      @Override
      public Object get(Cell cell, CellTraitPropertySpec<?> spec) {
        if (spec == ValidTextEditingTrait.VALID_TEXT_COLOR) {
          return validColor;
        }

        return super.get(cell, spec);
      }
    };
  }

  public static CellTrait validTextEditing(final Predicate<String> validator) {
    return new BaseCellTrait() {
      @Override
      protected CellTrait[] getBaseTraits(Cell cell) {
        return new CellTrait[] { new ValidTextEditingTrait() };
      }

      @Override
      public Object get(Cell cell, CellTraitPropertySpec<?> spec) {
        if (spec == ValidTextEditingTrait.VALIDATOR) {
          return validator;
        }

        if (spec == CellStateHandler.PROPERTY) {
          return EDITABLE_TEXT_VIEW_STATE_HANDLER;
        }

        return super.get(cell, spec);
      }

      @Override
      public Object get(Cell cell, CellPropertySpec<?> spec) {
        if (spec == Cell.FOCUSABLE) {
          return true;
        }

        return super.get(cell, spec);
      }
    };
  }


  private static Function<Cell, Function<String, CellAction>> expansionProvider(final Side side) {
    return new Function<Cell, Function<String, CellAction>>() {
      @Override
      public Function<String, CellAction> apply(final Cell cell) {
        return new Function<String, CellAction>() {
          @Override
          public CellAction apply(String sideText) {
            CompletionHelper sideCompletion = CompletionHelper.completionFor(cell, CompletionParameters.EMPTY, side.getKey());
            TextCell popupView = CompletionSupport.showSideTransformPopup(cell, side.getPopup(cell), sideCompletion.getItems());
            popupView.text().set(sideText);
            return CellActions.toEnd(popupView);
          }
        };
      }
    };
  }

  private static class TextCellStateHandler implements CellStateHandler<TextCell, TextViewState> {
    private boolean mySaveText;

    private TextCellStateHandler(boolean saveText) {
      mySaveText = saveText;
    }

    @Override
    public TextViewState saveState(TextCell cell) {
      TextViewState result = new TextViewState();
      if (mySaveText) {
        result.text = cell.text().get();
      }
      result.caretPosition = cell.caretPosition().get();
      return result;
    }

    @Override
    public void restoreState(TextCell cell, TextViewState state) {
      if (mySaveText) {
        cell.text().set(state.text);
      }
      cell.caretPosition().set(state.caretPosition);
    }
  }

  private static class TextViewState {
    int caretPosition;
    String text;


    @Override
    public boolean equals(Object o) {
      if (!(o instanceof TextViewState)) return false;

      TextViewState otherState = (TextViewState) o;

      return Objects.equal(text, otherState.text) && caretPosition == otherState.caretPosition;
    }

    @Override
    public int hashCode() {
      return caretPosition * 31  + (text == null ? 0 : text.hashCode());
    }
  }
}