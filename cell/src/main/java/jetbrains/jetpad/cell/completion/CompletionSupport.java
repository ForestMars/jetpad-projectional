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
package jetbrains.jetpad.cell.completion;

import com.google.common.base.Strings;
import jetbrains.jetpad.base.Handler;
import jetbrains.jetpad.model.event.CompositeRegistration;
import jetbrains.jetpad.model.event.Registration;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.PropertyBinding;
import jetbrains.jetpad.model.property.PropertyChangeEvent;
import jetbrains.jetpad.model.property.ReadableProperty;
import jetbrains.jetpad.event.Key;
import jetbrains.jetpad.event.KeyEvent;
import jetbrains.jetpad.event.ModifierKey;
import jetbrains.jetpad.cell.*;
import jetbrains.jetpad.base.Value;
import jetbrains.jetpad.cell.action.CellAction;
import jetbrains.jetpad.cell.event.CompletionEvent;
import jetbrains.jetpad.cell.event.FocusEvent;
import jetbrains.jetpad.cell.text.TextEditing;
import jetbrains.jetpad.cell.text.TextEditingTrait;
import jetbrains.jetpad.cell.trait.BaseCellTrait;
import jetbrains.jetpad.cell.trait.CellTrait;
import jetbrains.jetpad.cell.trait.CellTraitPropertySpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompletionSupport {
  public static final CellTraitPropertySpec<Runnable> HIDE_COMPLETION = new CellTraitPropertySpec<Runnable>("hideCompletion");

  public static CellTrait trait() {
    return new BaseCellTrait() {
      @Override
      public Object get(Cell cell, CellTraitPropertySpec<?> spec) {
        if (spec == Completion.COMPLETION_CONTROLLER) {
          return getCompletionHandler(cell);
        }

        return super.get(cell, spec);
      }

      private CompletionController getCompletionHandler(final Cell cell) {
        return new CompletionController() {
          @Override
          public boolean isActive() {
            return cell.frontPopup().get() != null;
          }

          @Override
          public boolean canActivate() {
            CompletionSupplier supplier = cell.get(Completion.COMPLETION);
            return !supplier.get(CompletionParameters.EMPTY).isEmpty();
          }

          @Override
          public void setActive(boolean active) {
            boolean isActive = isActive();
            if (isActive == active) return;
            if (active) {
              List<CompletionItem> items = cell.get(Completion.COMPLETION).get(new BaseCompletionParameters() {
                @Override
                public boolean isMenu() {
                  return true;
                }
              });
              showPopup(cell, cell.frontPopup(), items);
            } else {
              cell.focus();
            }
          }

          @Override
          public boolean hasAmbiguousMatches() {
            //todo implement it. we use this method only in hybrid1 synchronizers so we have mostly text view completion
            return true;
          }
        };
      }

      @Override
      public void onComplete(Cell cell, CompletionEvent event) {
        if (canComplete(cell)) {
          CompletionController handler = getCompletionHandler(cell);
          if (handler.canActivate()) {
            handler.setActive(true);
          }
          event.consume();
        }
        super.onComplete(cell, event);
      }

      private boolean canComplete(Cell cell) {
        Cell current = cell.container().focusedCell.get();
        while (current != cell) {
          Cell parent = current.parent().get();
          if (parent == null) throw new IllegalStateException();
          int index = parent.children().indexOf(current);
          if (index != 0) return false;
          current = parent;
        }
        return true;
      }
    };
  }

  public static void showCompletion(final TextCell textView, List<CompletionItem> items, Registration removeOnClose, final CellContainer.State prevState) {
    if (!textView.focused().get()) {
      throw new IllegalArgumentException();
    }

    final CompletionMenuModel menuModel = new CompletionMenuModel();
    menuModel.items.addAll(items);

    final CompositeRegistration reg = new CompositeRegistration();
    final ReadableProperty<String> prefixText = textView.prefixText();
    reg.add(PropertyBinding.bind(prefixText, menuModel.text));

    final Handler<CompletionItem> completer = new Handler<CompletionItem>() {
      @Override
      public void handle(CompletionItem item) {
        reg.remove();
        prevState.restore();
        item.complete(prefixText.get()).execute();
      }
    };

    reg.add(textView.addTrait(new BaseCellTrait() {
      @Override
      public void onPropertyChanged(Cell cell, CellPropertySpec<?> propery, PropertyChangeEvent<?> event) {
        if (propery == Cell.FOCUSED) {
          PropertyChangeEvent<Boolean> e = (PropertyChangeEvent<Boolean>) event;
          if (!e.getNewValue()) {
            reg.remove();
          }
        }

        super.onPropertyChanged(cell, propery, event);
      }

      @Override
      public void onKeyPressed(Cell cell, KeyEvent event) {
        CompletionItem selectedItem = menuModel.selectedItem.get();
        if (selectedItem == null) return;

        if (event.is(Key.ENTER)) {
          completer.handle(selectedItem);
          event.consume();
          return;
        }

        if (event.is(Key.UP)) {
          menuModel.up();
          event.consume();
          return;
        }

        if (event.is(Key.DOWN)) {
          menuModel.down();
          event.consume();
          return;
        }

        if (event.is(Key.ESCAPE)) {
          reg.remove();
          prevState.restore();
          event.consume();
          return;
        }

        super.onKeyPressed(cell, event);
      }

      @Override
      public Object get(Cell cell, CellTraitPropertySpec<?> spec) {
        if (spec == HIDE_COMPLETION) {
          return new Runnable() {
            @Override
            public void run() {
              reg.remove();
              prevState.restore();
            }
          };
        }

        return super.get(cell, spec);
      }
    }));
    final Cell completionCell = CompletionMenu.createView(menuModel, completer);
    reg.add(new Registration() {
      @Override
      public void remove() {
        completionCell.removeFromParent();
      }
    });
    reg.add(removeOnClose);

    textView.bottomPopup().set(completionCell);
  }

  public static TextCell showPopup(
      Cell cell,
      Property<Cell> targetPopup,
      List<CompletionItem> items) {
    CellContainer container = cell.container();
    final HorizontalCell popup = new HorizontalCell();
    final TextCell textView = new TextCell();

    textView.focusable().set(true);
    final Registration textEditingReg = textView.addTrait(new TextEditingTrait());

    popup.children().add(textView);
    targetPopup.set(popup);
    final CellContainer.State state = container.saveState();
    textView.focus();
    showCompletion(textView, items, new Registration() {
      @Override
      public void remove() {
        popup.removeFromParent();
        textEditingReg.remove();
      }
    }, state);
    return textView;
  }

  public static TextCell showSideTransformPopup(
      final Cell cell,
      final Property<Cell> targetPopup,
      List<CompletionItem> items) {
    final CellContainer container = cell.container();
    final Value<Boolean> completed = new Value<Boolean>(false);
    final Value<Boolean> dismissed = new Value<Boolean>(false);
    final CellContainer.State state = container.saveState();

    final List<CompletionItem> wrappedItems = new ArrayList<CompletionItem>();
    for (CompletionItem i : items) {
      wrappedItems.add(new CompletionItemWrapper(i) {
        @Override
        public CellAction complete(String text) {
          completed.set(true);
          return super.complete(text);
        }
      });
    }

    final HorizontalCell popup = new HorizontalCell();
    final TextCell textView = new TextCell();
    textView.addTrait(new BaseCellTrait() {
      @Override
      public Object get(Cell cell, CellTraitPropertySpec<?> spec) {
        if (spec == Completion.COMPLETION) {
          return new CompletionSupplier() {
            @Override
            public List<CompletionItem> get(CompletionParameters cp) {
              return wrappedItems;
            }
          };
        }

        return super.get(cell, spec);
      }
    });

    final Value<Runnable> dismiss = new Value<Runnable>();
    final CompletionHelper completion = new CompletionHelper(wrappedItems);
    textView.focusable().set(true);
    final Registration textEditingReg = textView.addTrait(new TextEditingTrait() {
      @Override
      public void onPropertyChanged(Cell cell, CellPropertySpec<?> prop, PropertyChangeEvent<?> e) {
        if (prop == TextCell.TEXT) {
          PropertyChangeEvent<String> event = (PropertyChangeEvent<String>) e;
          if (Strings.isNullOrEmpty(event.getNewValue())) {
            dismiss.get().run();
          }
        }

        super.onPropertyChanged(cell, prop, e);
      }

      @Override
      public void onKeyPressed(Cell cell, KeyEvent event) {
        if (event.is(Key.ESCAPE)) {
          dismiss.get().run();
          event.consume();
          return;
        }

        super.onKeyPressed(cell, event);
      }

      @Override
      protected boolean onAfterType(TextCell tv) {
        if (super.onAfterType(tv)) return true;

        if (!textView.isEnd()) return false;

        String text = textView.text().get();
        if (completion.hasSingleMatch(text, cell.get(TextEditing.EAGER_COMPLETION))) {
          completion.matches(text).get(0).complete(text).execute();
          return true;
        }

        String prefix = text.substring(0, text.length() - 1);
        String suffix = text.substring(text.length() - 1);
        if (completion.matches(prefix).size() == 1 && completion.prefixedBy(text).isEmpty()) {
          completion.matches(prefix).get(0).complete(prefix).execute();
          for (int i = 0; i < suffix.length(); i++) {
            container.keyTyped(new KeyEvent(Key.UNKNOWN, suffix.charAt(i), Collections.<ModifierKey>emptySet()));
          }
        }
        return true;
      }
    });

    popup.children().add(textView);

    if (targetPopup.get() != null) throw new IllegalStateException();

    targetPopup.set(popup);
    textView.focus();

    dismiss.set(new Runnable() {
      @Override
      public void run() {
        if (dismissed.get()) return;
        dismissed.set(true);
        popup.removeFromParent();
        textEditingReg.remove();
        if (!completed.get()) {
          state.restore();
        }
      }
    });

    textView.addTrait(new BaseCellTrait() {
      @Override
      public void onFocusLost(Cell cell, FocusEvent event) {
        super.onFocusLost(cell, event);
        dismiss.get().run();
      }
    });

    return textView;
  }
}