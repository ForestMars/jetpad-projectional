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
package jetbrains.jetpad.cell.dom;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import jetbrains.jetpad.model.event.CompositeRegistration;
import jetbrains.jetpad.model.event.EventHandler;
import jetbrains.jetpad.model.event.Registration;
import jetbrains.jetpad.model.property.PropertyChangeEvent;
import jetbrains.jetpad.cell.TextCell;
import jetbrains.jetpad.projectional.domUtil.DomTextEditor;

class TextCellMapper extends BaseCellMapper<TextCell> {
  private static final int CARET_BLINK_DELAY = 1000;

  private DomTextEditor myTextEditor;
  private long myLastChangeTime = System.currentTimeMillis();
  private boolean myCaretVisible;
  private boolean myContainerFocused;
  private Registration myFocusRegistration;

  TextCellMapper(TextCell source, CellToDomContext ctx) {
    super(source, ctx, DOM.createDiv());
    myContainerFocused = ctx.focused.get();
    myTextEditor = new DomTextEditor(getTarget());
  }

  @Override
  protected boolean managesChildren() {
    return true;
  }

  @Override
  protected void onDetach() {
    super.onDetach();

    if (myFocusRegistration != null) {
      myFocusRegistration.remove();
      myFocusRegistration = null;
    }
  }

  int getCaretOffset(int caret) {
    return myTextEditor.caretOffset(caret);
  }

  int getCaretAt(int x) {
    return myTextEditor.caretPositionAt(x);
  }

  @Override
  protected void refreshProperties() {
    super.refreshProperties();

    if (getSource().focused().get()) {
      if (myFocusRegistration == null) {
        final Timer timer = new Timer() {
          @Override
          public void run() {
            if (System.currentTimeMillis() - myLastChangeTime < CARET_BLINK_DELAY) return;
            myCaretVisible = !myCaretVisible;
            updateCaretVisibility();
          }
        };
        timer.scheduleRepeating(500);
        myContainerFocused = getContext().focused.get();
        myFocusRegistration = new CompositeRegistration(
          getContext().focused.addHandler(new EventHandler<PropertyChangeEvent<Boolean>>() {
            @Override
            public void onEvent(PropertyChangeEvent<Boolean> event) {
              myContainerFocused = event.getNewValue();
              updateCaretVisibility();
            }
          }),
          new Registration() {
            @Override
            public void remove() {
              timer.cancel();
            }
          }
        );
      }
    } else {
      if (myFocusRegistration != null) {
        myFocusRegistration.remove();
        myFocusRegistration = null;
      }
    }

    myLastChangeTime = System.currentTimeMillis();

    myTextEditor.text(getSource().text().get());
    myTextEditor.caretPosition(getSource().caretPosition().get());
    myTextEditor.caretVisible(getSource().caretVisible().get() && getSource().focused().get());
    myTextEditor.textColor(getSource().textColor().get());
    myTextEditor.bold(getSource().bold().get());

    myTextEditor.selectionVisble(getSource().selectionVisible().get() && getSource().focused().get());
    myTextEditor.selectionStart(getSource().selectionStart().get());
  }

  private void updateCaretVisibility() {
    myTextEditor.caretVisible(myContainerFocused && myCaretVisible && getSource().caretVisible().get());
  }

}